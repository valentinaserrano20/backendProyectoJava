package Controlador.Voluntario;

import Modelo.DTO.RegistroPlanDTO;
import Modelo.DTO.ActualizarIdentificacionDTO;
import Modelo.Servicios.Voluntario.PlanFamiliarServicio;
import Modelo.Servicios.Voluntario.VulnerabilidadServicio;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse; 
import jakarta.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import org.json.JSONObject;

// Servlet mapeado a /api/familyPlans/* para gestionar el registro inicial y actualizaciones de planes familiares
@WebServlet("/api/familyPlans/*")
public class PlanFamiliarServlet extends HttpServlet {
    // Instancia el servicio de registro y consulta de planes familiares
    private final PlanFamiliarServicio planServicio = new PlanFamiliarServicio();
    // Instancia el servicio de vulnerabilidades para el cambio de estado de planes
    private final VulnerabilidadServicio vulServicio = new VulnerabilidadServicio();

    // Sobrescribe service para capturar peticiones PATCH y redirigirlas a doPatch
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        // Obtiene el verbo HTTP de la solicitud
        String method = req.getMethod();
        // Si es una petición PATCH, delega a nuestro manejador personalizado
        if (method.equalsIgnoreCase("PATCH")) {
            doPatch(req, resp);
        } else {
            // Si es otro método (GET/POST), continúa el ciclo de vida estándar
            super.service(req, resp);
        }
    }

    // Intercepta peticiones HTTP GET para consultar el detalle de un plan familiar específico o acciones de validación
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Define el tipo de contenido a JSON
        response.setContentType("application/json");
        // Configura la codificación
        response.setCharacterEncoding("UTF-8");

        // Valida la sesión activa de usuario
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioId") == null) {
            // Retorna HTTP 401 si no está autenticado
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            return;
        }

        // Obtiene la parte de la URL con el ID del plan o la acción solicitada (ej: /check-access/11 o /12)
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            // Sirve para: Resolver el listado paginado de planes cuando no hay un pathInfo específico
            // Qué hace: Obtiene el ID del voluntario de la sesión, extrae la página de los parámetros de consulta y llama al servicio
            // Por qué es importante: El frontend llama a /api/familyPlans pasándole el parámetro de página para pintar el listado principal
            try {
                // Recupera el ID del voluntario en sesión
                int usuarioId = (int) session.getAttribute("usuarioId");
                // Recupera el parámetro 'page' de la URL
                String pageParam = request.getParameter("page");
                // Inicializa la página por defecto en 1
                int page = 1;
                // Si el parámetro existe y no está vacío
                if (pageParam != null && !pageParam.trim().isEmpty()) {
                    // Parsea la página recibida
                    page = Integer.parseInt(pageParam);
                }
                // Llama al servicio para obtener los planes de forma paginada
                String jsonRespuesta = planServicio.listarPlanesPaginado(usuarioId, page);
                // Escribe la respuesta JSON en el body
                response.getWriter().write(jsonRespuesta);
            } catch (Exception e) {
                // Si ocurre un error inesperado, responde con HTTP 500
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al procesar listado de planes: " + e.getMessage()).toString());
            }
            return;
        }

        // Divide las partes de la ruta de la URL por diagonales
        String[] partes = pathInfo.split("/");
        
        // Sirve para: Enrutar dinámicamente las solicitudes GET de consulta y validación del plan familiar
        // Qué hace: Evalúa si el primer segmento solicita 'check-access' o 'has-members' para procesarlas de manera especializada; de lo contrario, asume que es el ID del plan familiar directo
        // Por qué es importante: Evita errores de parseo numérico cuando el frontend consulta sub-recursos bajo /api/familyPlans
        try {
            if (partes[1].equals("check-access")) {
                // Sirve para: Validar si la petición de acceso trae un ID de plan
                // Qué hace: Comprueba la longitud de los segmentos y devuelve HTTP 400 si no está el ID del plan
                // Por qué es importante: Previene fallas por índices fuera de rango al acceder a partes[2]
                if (partes.length < 3) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de plan no provisto para verificación.").toString());
                    return;
                }
                // Parsea el ID del plan desde el segundo segmento
                int planId = Integer.parseInt(partes[2]);
                // Obtiene el ID del usuario en sesión
                int usuarioId = (int) session.getAttribute("usuarioId");
                // Llama al servicio para validar los permisos
                String jsonRespuesta = planServicio.verificarAccesoAPlan(planId, usuarioId);
                // Retorna la respuesta de acceso en formato JSON
                response.getWriter().write(jsonRespuesta);
                
            } else if (partes[1].equals("has-members")) {
                // Sirve para: Validar si la petición de conteo de integrantes trae un ID de plan
                // Qué hace: Comprueba la longitud de los segmentos y devuelve HTTP 400 si falta el ID
                // Por qué es importante: Garantiza que se envíe el ID de plan necesario para la consulta en base de datos
                if (partes.length < 3) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de plan no provisto para validar integrantes.").toString());
                    return;
                }
                // Parsea el ID del plan del segundo segmento
                int planId = Integer.parseInt(partes[2]);
                // Llama al servicio para validar si tiene integrantes
                String jsonRespuesta = planServicio.verificarTieneIntegrantes(planId);
                // Retorna la respuesta de integrantes en formato JSON
                response.getWriter().write(jsonRespuesta);
                
            } else {
                // Intenta extraer el ID del plan familiar desde la primera posición
                int planId = Integer.parseInt(partes[1]);
                // Invoca al servicio para obtener el JSON del plan detallado
                String json = planServicio.obtenerPlanDetallado(planId);
                // Envía la respuesta al frontend
                response.getWriter().write(json);
            }
        } catch (NumberFormatException e) {
            // Si el ID del plan no es numérico, retorna HTTP 400
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID del plan debe ser numérico.").toString());
        } catch (Exception e) {
            // Captura cualquier otro fallo general
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al procesar la petición GET: " + e.getMessage()).toString());
        }
    }

    // Intercepta peticiones HTTP POST para crear e inicializar un nuevo plan familiar (Paso 1)
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Define el tipo de contenido a JSON
        response.setContentType("application/json");
        // Configura la codificación
        response.setCharacterEncoding("UTF-8");

        // Obtiene la sesión actual
        HttpSession session = request.getSession(false);
        // Valida la sesión
        if (session == null || session.getAttribute("usuarioId") == null) {
            // Establece HTTP 401 si no está autorizado
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            return;
        }

        // Inicializa un buffer de cadenas para leer el stream
        StringBuilder buffer = new StringBuilder();
        String linea;
        // Abre el lector para procesar el cuerpo de la petición
        try (BufferedReader reader = request.getReader()) {
            while ((linea = reader.readLine()) != null) {
                // Acumula la línea
                buffer.append(linea);
            }
        }

        try {
            // Parsea la cadena leída a objeto JSON
            JSONObject json = new JSONObject(buffer.toString());
            
            // Instancia el DTO para el registro inicial del plan familiar
            RegistroPlanDTO dto = new RegistroPlanDTO();
            // Asigna los apellidos familiares
            dto.setLastNames(json.optString("last_names"));
            // Asigna el ID de la zona
            dto.setZoneId(json.optInt("zone_id"));
            // Asigna el ID de la organización
            dto.setOrganizacionId(json.optInt("city_id"));
            // Inyecta de forma segura el ID de usuario desde la sesión
            dto.setUserId((int) session.getAttribute("usuarioId"));

            // Invoca al servicio para registrar el plan familiar e inicializarlo
            String jsonRespuesta = planServicio.registrarNuevoPlan(dto);
            // Envía el JSON al cliente
            response.getWriter().write(jsonRespuesta);

        } catch (Exception e) {
            // Establece HTTP 400 si el JSON está mal formado o hay errores inesperados
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "JSON mal formado.").toString());
        }
    }

    // Manejador personalizado para procesar peticiones HTTP PATCH (ej. cambiar estado o guardar identificación)
    protected void doPatch(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Define el tipo de contenido a JSON
        response.setContentType("application/json");
        // Configura la codificación
        response.setCharacterEncoding("UTF-8");

        // Obtiene la sesión activa
        HttpSession session = request.getSession(false);
        // Valida la sesión
        if (session == null || session.getAttribute("usuarioId") == null) {
            // Establece HTTP 401 si no está autorizado
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            return;
        }

        // Obtiene la ruta relativa solicitada (ej. /12/identify)
        String pathInfo = request.getPathInfo();
        // Valida que la ruta no esté vacía
        if (pathInfo == null || pathInfo.equals("/")) {
            // Establece HTTP 400
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acción no especificada.").toString());
            return;
        }

        // Divide la ruta por diagonales para extraer el ID y la acción
        String[] partes = pathInfo.split("/");
        // Si no cumple con la estructura esperada /id/accion
        if (partes.length < 3) {
            // Establece HTTP 400
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "URL mal estructurada.").toString());
            return;
        }

        try {
            // Obtiene el ID del plan familiar desde la ruta de la URL
            int planId = Integer.parseInt(partes[1]);
            // Obtiene el nombre de la acción (ej. identify / change-status)
            String accion = partes[2];

            // Escenario 1: Cambiar el estado del plan
            if (accion.equals("change-status")) {
                // Lee el cuerpo de la petición HTTP
                StringBuilder buffer = new StringBuilder();
                String linea;
                try (BufferedReader reader = request.getReader()) {
                    while ((linea = reader.readLine()) != null) {
                        buffer.append(linea);
                    }
                }
                
                // Parsea el cuerpo a objeto JSON
                JSONObject json = new JSONObject(buffer.toString());
                // Obtiene el ID del nuevo estado solicitado
                int statusPlanId = json.getInt("status_plan_id");
                
                // Obtiene el comentario opcional si existe en el JSON
                String comentary = json.has("comentary") && !json.isNull("comentary") ? json.getString("comentary") : null;
                // Obtiene de forma segura el ID del usuario gestor activo desde la sesión
                int usuarioId = (int) session.getAttribute("usuarioId");

                // Llama al servicio para actualizar el estado del plan familiar e insertar en el seguimiento
                String resJson = vulServicio.cambiarEstadoPlan(planId, statusPlanId, comentary, usuarioId);
                // Envía el JSON de confirmación al cliente
                response.getWriter().write(resJson);
            } 
            // Escenario 2: Guardar los datos de identificación detallados de la vivienda (PATCH de identificación)
            // Sirve para: Interceptar la petición de actualización de datos de la vivienda y del sector
            // Qué hace: Lee el stream del JSON body, parsea dirección, sector, barrio, zona, teléfono y calidad, y llama al servicio
            // Por qué es importante: Permite persistir los cambios hechos en el formulario del frontend de forma estructurada
            else if (accion.equals("identify")) {
                // Lee el cuerpo de la petición HTTP
                StringBuilder buffer = new StringBuilder();
                String linea;
                try (BufferedReader reader = request.getReader()) {
                    while ((linea = reader.readLine()) != null) {
                        buffer.append(linea);
                    }
                }

                // Parsea el JSON del cuerpo
                JSONObject json = new JSONObject(buffer.toString());
                
                // Instancia el DTO para capturar los datos de la vivienda
                ActualizarIdentificacionDTO dto = new ActualizarIdentificacionDTO();
                // Asigna los apellidos familiares
                dto.setLastNames(json.getString("last_names"));
                // Asigna la dirección
                dto.setAddress(json.getString("address"));
                // Asigna el identificador del sector
                dto.setSectorId(json.getInt("sector_id"));
                // Asigna el nombre de barrio/sector
                dto.setSectorName(json.getString("sector_name"));
                // Asigna el teléfono fijo
                dto.setLandlinePhone(json.optString("landline_phone"));
                // Asigna el identificador de calidad de vivienda
                dto.setHousingQualityId(json.getInt("housing_quality_id"));
                // Asigna el identificador del tipo de zona (es opcional en el cuerpo del JSON)
                dto.setZoneId(json.optInt("zone_id", 0));

                // Invoca al servicio para actualizar los datos detallados de la vivienda
                String resJson = planServicio.guardarIdentificacion(planId, dto);
                // Envía la respuesta JSON al cliente
                response.getWriter().write(resJson);
            }
            // Si la acción solicitada no está implementada
            else {
                // Establece HTTP 404 (No Encontrado)
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write(new JSONObject().put("success", false).put("message", "Acción no reconocida.").toString());
            }
        } catch (NumberFormatException e) {
            // Establece HTTP 400 si el ID del plan en la URL no es numérico
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID del plan debe ser un número entero.").toString());
        } catch (Exception e) {
            // Captura cualquier otro fallo general y establece HTTP 400
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al procesar la petición PATCH: " + e.getMessage()).toString());
        }
    }
}