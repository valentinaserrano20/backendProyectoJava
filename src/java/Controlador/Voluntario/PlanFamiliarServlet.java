package Controlador.Voluntario;

import Modelo.DTO.RegistroPlanDTO;
import Modelo.DTO.ActualizarIdentificacionDTO;
import Modelo.Servicios.Voluntario.PlanFamiliarServicio;
import Modelo.Servicios.Voluntario.VulnerabilidadServicio;
import Modelo.DAO.NotificacionDAO;
import Modelo.DTO.NotificacionDTO;
import Modelo.DAO.PlanFamiliarDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse; 
import jakarta.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import org.json.JSONObject;

/**
 * Qué hace: Registra e inicializa el Servlet mapeado a la subruta "/api/familyPlans/*" para gestionar planes familiares.
 * Por qué existe: Actúa como el controlador de red principal para crear, actualizar parcial (PATCH) y consultar la información del censo familiar.
 * Qué pasaría si no estuviera: El cliente frontend no tendría un endpoint de red unificado para administrar la información de los planes familiares de emergencia.
 */
@WebServlet("/api/familyPlans/*")
public class PlanFamiliarServlet extends HttpServlet {

    // Qué hace: Instancia la clase de lógica de negocios para los planes familiares.
    // Por qué existe: Delega la lógica de guardado, lectura y validación de la propiedad de los planes familiares.
    // Qué pasaría si no estuviera: Se tendría que escribir SQL y lógica de negocio directamente en el servlet.
    // Flujo: De aquí pasamos a PlanFamiliarServicio para orquestar la manipulación de base de datos.
    private final PlanFamiliarServicio planServicio = new PlanFamiliarServicio();

    // Qué hace: Instancia la clase de lógica de negocios de vulnerabilidad.
    // Por qué existe: Delega el cambio de estados de planes y auditoría de seguimiento.
    // Qué pasaría si no estuviera: No podríamos transicionar el plan de estado o guardar las bitácoras de rechazo/aprobación.
    // Flujo: De aquí pasamos a VulnerabilidadServicio.
    private final VulnerabilidadServicio vulServicio = new VulnerabilidadServicio();

    // Qué hace: Intercepta todas las peticiones entrantes para desviar los verbos PATCH.
    // Por qué existe: Java Servlet estándar no soporta directamente doPatch en la jerarquía tradicional sin este filtro de enrutamiento manual.
    // Qué pasaría si no estuviera: Las peticiones de tipo PATCH hechas por el frontend fallarían con código HTTP 405 (Method Not Allowed).
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        String method = req.getMethod();
        if (method.equalsIgnoreCase("PATCH")) {
            doPatch(req, resp);
        } else {
            super.service(req, resp);
        }
    }

    // Qué hace: Atiende llamadas HTTP GET para listar planes familiares, verificar accesos, integrantes o retornar la ficha técnica completa.
    // Por qué existe: Expone la información necesaria para pintar las tablas o formularios en la UI.
    // Qué pasaría si no estuviera: El voluntario no podría ver el listado de sus planes creados ni recuperar la información guardada previamente.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Qué hace: Configura la respuesta a formato JSON estructurado.
        response.setContentType("application/json");
        // Qué hace: Asegura la codificación UTF-8 para admitir eñes y acentos.
        response.setCharacterEncoding("UTF-8");

        // Qué hace: Recupera la sesión activa sin crear una nueva.
        // Por qué existe: Bloquea accesos anónimos a los datos sensibles de los planes y viviendas de los ciudadanos.
        // Qué pasaría si no estuviera: Cualquier persona podría descargar censos familiares de la base de datos sin estar autenticado.
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            return;
        }

        // Qué hace: Captura el segmento adicional de la ruta URL.
        // Por qué existe: Permite diferenciar si se pide la lista paginada (/), verificar acceso (/check-access/{id}) o miembros (/has-members/{id}).
        // Qué pasaría si no estuviera: No podríamos mapear dinámicamente diferentes consultas GET bajo la misma ruta raíz.
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            try {
                // Qué hace: Obtiene el ID del usuario en sesión.
                int usuarioId = (int) session.getAttribute("usuarioId");
                
                // Qué hace: Lee el parámetro de paginación.
                String pageParam = request.getParameter("page");
                int page = 1;
                if (pageParam != null && !pageParam.trim().isEmpty()) {
                    page = Integer.parseInt(pageParam);
                }
                
                // Qué hace: Invocación delegada para traer el listado JSON.
                // y luego de esto pasamos a PlanFamiliarServicio.listarPlanesPaginado, el cual realiza las consultas SQL paginadas en la base de datos.
                String jsonRespuesta = planServicio.listarPlanesPaginado(usuarioId, page);
                response.getWriter().write(jsonRespuesta);
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al procesar listado de planes: " + e.getMessage()).toString());
            }
            return;
        }

        // Qué hace: Divide la ruta para procesar sub-recursos REST.
        String[] partes = pathInfo.split("/");
        
        try {
            if (partes[1].equals("check-access")) {
                if (partes.length < 3) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de plan no provisto para verificación.").toString());
                    return;
                }
                int planId = Integer.parseInt(partes[2]);
                int usuarioId = (int) session.getAttribute("usuarioId");
                
                // Qué hace: Solicita verificar si este voluntario es dueño del plan familiar solicitado.
                // y luego de esto pasamos a PlanFamiliarServicio.verificarAccesoAPlan, el cual valida los permisos de pertenencia en base de datos.
                String jsonRespuesta = planServicio.verificarAccesoAPlan(planId, usuarioId);
                response.getWriter().write(jsonRespuesta);
                
            } else if (partes[1].equals("has-members")) {
                if (partes.length < 3) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de plan no provisto para validar integrantes.").toString());
                    return;
                }
                int planId = Integer.parseInt(partes[2]);
                
                // Qué hace: Consulta si el plan ya cuenta con integrantes familiares asociados.
                // y luego de esto pasamos a PlanFamiliarServicio.verificarTieneIntegrantes, que cuenta los integrantes activos en la base de datos.
                String jsonRespuesta = planServicio.verificarTieneIntegrantes(planId);
                response.getWriter().write(jsonRespuesta);
                
            } else {
                // Qué hace: Parsea el ID primario directo de la ruta (ej. /api/familyPlans/12).
                int planId = Integer.parseInt(partes[1]);
                
                // Qué hace: Retorna la ficha técnica de la vivienda y miembros en un JSON unificado.
                // y luego de esto pasamos a PlanFamiliarServicio.obtenerPlanDetallado, el cual recupera toda la ficha técnica desde la BD.
                String json = planServicio.obtenerPlanDetallado(planId);
                response.getWriter().write(json);
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID del plan debe ser numérico.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al procesar la petición GET: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Atiende peticiones HTTP POST para inicializar el censo familiar en el Paso 1 (apellidos, zona, municipio).
    // Por qué existe: Crea de forma inicial el registro físico en la tabla de base de datos relacional.
    // Qué pasaría si no estuviera: No se podría dar de alta un plan familiar en el sistema.
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            return;
        }

        // Qué hace: Lee el cuerpo JSON enviado por el cliente asíncrono.
        // Por qué existe: Permite capturar las variables estructuradas enviadas en el payload.
        // Qué pasaría si no estuviera: No podríamos obtener los datos del formulario de registro enviados desde la SPA.
        StringBuilder buffer = new StringBuilder();
        String linea;
        try (BufferedReader reader = request.getReader()) {
            while ((linea = reader.readLine()) != null) {
                buffer.append(linea);
            }
        }

        try {
            JSONObject json = new JSONObject(buffer.toString());
            
            // Qué hace: Crea el DTO para el registro.
            RegistroPlanDTO dto = new RegistroPlanDTO();
            dto.setLastNames(json.optString("last_names"));
            dto.setZoneId(json.optInt("zone_id"));
            dto.setOrganizacionId(json.optInt("city_id"));
            dto.setUserId((int) session.getAttribute("usuarioId"));

            // Qué hace: Llama al servicio para registrar el nuevo plan.
            // y luego de esto pasamos a PlanFamiliarServicio.registrarNuevoPlan, el cual inserta el registro en MySQL y retorna el ID generado.
            String jsonRespuesta = planServicio.registrarNuevoPlan(dto);
            response.getWriter().write(jsonRespuesta);

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "JSON mal formado.").toString());
        }
    }

    // Qué hace: Procesa peticiones HTTP PATCH para transicionar de estado el plan o para guardar el censo de identificación.
    // Por qué existe: Permite actualizar propiedades parciales del plan de emergencia sin necesidad de enviar todo el objeto completo.
    // Qué pasaría si no estuviera: Tendríamos que hacer peticiones PUT masivas consumiendo más ancho de banda y arriesgando sobreescrituras accidentales.
    protected void doPatch(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            return;
        }

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acción no especificada.").toString());
            return;
        }

        String[] partes = pathInfo.split("/");
        if (partes.length < 3) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "URL mal estructurada.").toString());
            return;
        }

        try {
            int planId;
            String accion;

            // Qué hace: Evalúa la estructura del pathInfo para clasificar peticiones de estado de forma segura.
            if (partes[1].equals("status")) {
                planId = Integer.parseInt(partes[2]);
                accion = "change-status";
            } else {
                planId = Integer.parseInt(partes[1]);
                accion = partes[2];
            }

            // Escenario 1: Cambiar el estado del plan (Enviar, Aprobar, Rechazar)
            if (accion.equals("change-status")) {
                StringBuilder buffer = new StringBuilder();
                String linea;
                try (BufferedReader reader = request.getReader()) {
                    while ((linea = reader.readLine()) != null) {
                        buffer.append(linea);
                    }
                }
                
                JSONObject json = new JSONObject(buffer.toString());
                int statusPlanId = json.getInt("status_plan_id");
                String comentary = json.has("comentary") && !json.isNull("comentary") ? json.getString("comentary") : null;
                int usuarioId = (int) session.getAttribute("usuarioId");

                // Qué hace: Modifica el estado del plan familiar delegando al servicio.
                // y luego de esto pasamos a VulnerabilidadServicio.cambiarEstadoPlan, el cual actualiza el estado y escribe la bitácora en la BD.
                String resJson = vulServicio.cambiarEstadoPlan(planId, statusPlanId, comentary, usuarioId);
                
                // =========================================
                // CREAR NOTIFICACIÓN PARA EL VOLUNTARIO
                // =========================================
                try {
                    // y luego de esto instanciamos PlanFamiliarDAO y NotificacionDAO para interactuar con la persistencia en MySQL.
                    PlanFamiliarDAO planDAO = new PlanFamiliarDAO();
                    NotificacionDAO notificacionDAO = new NotificacionDAO();
                    
                    String sql = "SELECT voluntario_id FROM planes_familiares WHERE id = ?";
                    
                    // Qué hace: Consulta directa para averiguar qué voluntario es el destinatario de la alerta.
                    try (java.sql.Connection con = Modelo.Config.Conexion.obtener();
                         java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
                        
                        ps.setInt(1, planId);
                        
                        try (java.sql.ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                int voluntarioId = rs.getInt("voluntario_id");
                                
                                // Qué hace: Valida si el plan transiciona a Aprobado (6) o Rechazado (7) para alertar al voluntario.
                                if (statusPlanId == 6 || statusPlanId == 7) {
                                    NotificacionDTO notificacion = new NotificacionDTO();
                                    notificacion.setUsuarioId(voluntarioId);
                                    notificacion.setTitulo(statusPlanId == 6 ? "Plan Aprobado" : "Plan Rechazado");
                                    notificacion.setMensaje(statusPlanId == 6 
                                        ? "Tu plan familiar ha sido aprobado exitosamente" 
                                        : "Tu plan familiar ha sido rechazado. " + (comentary != null ? comentary : ""));
                                    notificacion.setTipo("plan_estado");
                                    notificacion.setLeida(false);
                                    notificacion.setEnlace("#/voluntario/plan_familiar");
                                    notificacion.setEntidadId(planId);
                                    
                                    // Qué hace: Persiste de forma física la notificación del voluntario.
                                    notificacionDAO.crear(notificacion);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error al crear notificación: " + e.getMessage());
                }
                
                response.getWriter().write(resJson);
            } 
            // Escenario 2: Guardar los datos de identificación detallados de la vivienda (PATCH de identificación)
            else if (accion.equals("identify")) {
                StringBuilder buffer = new StringBuilder();
                String linea;
                try (BufferedReader reader = request.getReader()) {
                    while ((linea = reader.readLine()) != null) {
                        buffer.append(linea);
                    }
                }

                JSONObject json = new JSONObject(buffer.toString());
                
                ActualizarIdentificacionDTO dto = new ActualizarIdentificacionDTO();
                dto.setLastNames(json.getString("last_names"));
                dto.setAddress(json.getString("address"));
                dto.setHousingQualityId(json.getInt("housing_quality_id"));
                dto.setSectorId(json.getInt("sector_id"));
                dto.setSectorName(json.getString("sector_name"));
                dto.setLandlinePhone(json.optString("landline_phone"));
                dto.setZoneId(json.optInt("zone_id", 0));

                // Qué hace: Actualiza la información de identificación de la vivienda en la BD.
                // y luego de esto pasamos a PlanFamiliarServicio.guardarIdentificacion, que ejecuta el UPDATE SQL en MySQL.
                String resJson = planServicio.guardarIdentificacion(planId, dto);
                response.getWriter().write(resJson);
            }
            else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write(new JSONObject().put("success", false).put("message", "Acción no reconocida.").toString());
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID del plan debe ser un número entero.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al procesar la petición PATCH: " + e.getMessage()).toString());
        }
    }
}