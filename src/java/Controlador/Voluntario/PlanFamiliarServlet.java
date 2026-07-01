package Controlador.Voluntario;

/*
 * Qué hace (la acción): Importa los DTOs de registro e identificación de planes familiares, servicios de negocio, utilidades de JSON, sesión y APIs estándares de servlets de Jakarta.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.DTO.RegistroPlanDTO: DTO para el registro inicial básico de un plan familiar de emergencia.
 *   - Modelo.DTO.ActualizarIdentificacionDTO: DTO que transporta los datos detallados de dirección, calidad de vivienda, sector y teléfono de contacto.
 *   - Modelo.Servicios.Voluntario.PlanFamiliarServicio / VulnerabilidadServicio: Capa de servicios para la administración física, accesos y transiciones de estado de planes.
 * Para qué se usa (el propósito): Proveer al servlet de los DTOs, utilidades y clases de negocio para la gestión integral del censo familiar.
 * Por qué es importante (el impacto o problema que resuelve): Sin estas importaciones, no se podría procesar la información del censo de la vivienda familiar ni realizar transiciones de estado lógicas.
 */
import Modelo.DTO.RegistroPlanDTO;
import Modelo.DTO.ActualizarIdentificacionDTO;
import Modelo.Servicios.Voluntario.PlanFamiliarServicio;
import Modelo.Servicios.Voluntario.VulnerabilidadServicio;
import Modelo.Utilidades.JSONUtil;
import Modelo.Utilidades.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse; 
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.json.JSONObject;

/*
 * Qué hace (la acción): Asocia el servlet PlanFamiliarServlet con el endpoint "/api/familyPlans/*" a través de @WebServlet.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - @WebServlet("/api/familyPlans/*"): Comodín que intercepta la gestión general, actualizaciones, verificaciones de acceso y fichas técnicas del plan familiar.
 * Para qué se usa (el propósito): Servir como el controlador web centralizado para el ciclo de vida del Plan Familiar de Emergencia del voluntario.
 * Por qué es importante (el impacto o problema que resuelve): Unifica las responsabilidades CRUD y de control de estado del censo familiar en un solo punto, separando la lógica del servlet de la persistencia JDBC.
 */
@WebServlet("/api/familyPlans/*")
public class PlanFamiliarServlet extends HttpServlet {

    /*
     * Qué hace (la acción): Instancia de manera privada y constante las variables de servicio del plan y de vulnerabilidad.
     * Qué significa (conceptos, métodos, tipos involucrados): Instancias de PlanFamiliarServicio y VulnerabilidadServicio.
     * Para qué se usa (el propósito): Ejecutar los procesos de lógica de negocio.
     */
    private final PlanFamiliarServicio planServicio = new PlanFamiliarServicio();
    private final VulnerabilidadServicio vulServicio = new VulnerabilidadServicio();

    /*
     * Qué hace (la acción): Sobrescribe el método service para canalizar peticiones del verbo HTTP PATCH hacia el método doPatch.
     * Qué significa (conceptos, métodos, tipos involucrados): doPatch redirigido manualmente para soportar actualizaciones parciales.
     * Para qué se usa (el propósito): Habilitar PATCH en el contenedor Tomcat.
     */
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

    /*
     * Qué hace (la acción): Sobrescribe el método doGet para listar planes familiares asignados al usuario logueado en la sesión de manera paginada, verificar los accesos al plan, corroborar si tiene integrantes ingresados o retornar su ficha técnica unificada completa.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - check-access: Valida si el voluntario tiene autorización para editar el plan (ej: si pertenece a su cuenta o si posee rol de supervisor).
     *   - has-members: Verifica si el plan cuenta con por lo menos un miembro registrado en su censo familiar.
     *   - planServicio.obtenerPlanDetallado(planId): Consulta y consolida en un único JSON la ficha técnica completa del plan de emergencia familiar.
     * Para qué se usa (el propósito): Proveer los datos de consulta para los dashboards y ventanas de confirmación en la UI del voluntario y supervisor.
     * Por qué es importante (el impacto o problema que resuelve): Permite validar permisos de seguridad y estados de avance antes de permitir la edición, previniendo visualizaciones cruzadas no autorizadas de planes familiares.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        String pathInfo = request.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            try {
                Integer usuarioId = SessionUtil.getUsuarioId(session);
                if (usuarioId == null) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Sesión inválida.").toString());
                    return;
                }
                
                String pageParam = request.getParameter("page");
                int page = 1;
                if (pageParam != null && !pageParam.trim().isEmpty()) {
                    page = Integer.parseInt(pageParam);
                }
                
                String jsonRespuesta = planServicio.listarPlanesPaginado(usuarioId, page);
                response.getWriter().write(jsonRespuesta);
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al procesar listado de planes: " + e.getMessage()).toString());
            }
            return;
        }

        String[] partes = pathInfo.split("/");
        
        try {
            if (partes[1].equals("check-access")) {
                if (partes.length < 3) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de plan no provisto para verificación.").toString());
                    return;
                }
                int planId = Integer.parseInt(partes[2]);
                Integer usuarioId = SessionUtil.getUsuarioId(session);
                if (usuarioId == null) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Sesión inválida.").toString());
                    return;
                }
                
                String jsonRespuesta = planServicio.verificarAccesoAPlan(planId, usuarioId);
                response.getWriter().write(jsonRespuesta);
                
            } else if (partes[1].equals("has-members")) {
                if (partes.length < 3) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de plan no provisto para validar integrantes.").toString());
                    return;
                }
                int planId = Integer.parseInt(partes[2]);
                
                String jsonRespuesta = planServicio.verificarTieneIntegrantes(planId);
                response.getWriter().write(jsonRespuesta);
                
            } else {
                int planId = Integer.parseInt(partes[1]);
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

    /*
     * Qué hace (la acción): Sobrescribe el método doPost para inicializar y registrar un nuevo plan familiar para la seccional y zona correspondiente en la base de datos SQL.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - RegistroPlanDTO: DTO que modela los campos requeridos para abrir un plan.
     *   - planServicio.registrarNuevoPlan(dto): Valida y escribe la nueva fila en base de datos.
     * Para qué se usa (el propósito): Crear el plan familiar base que posteriormente será rellenado con integrantes, croquis, riesgos y maletines.
     * Por qué es importante (el impacto o problema que resuelve): Asocia de forma limpia la cuenta del voluntario creador al plan de emergencia familiar, garantizando consistencia.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        try {
            HttpSession session = request.getSession(false);
            JSONObject json = JSONUtil.leerJson(request);
            
            RegistroPlanDTO dto = new RegistroPlanDTO();
            dto.setLastNames(json.optString("last_names"));
            dto.setZoneId(json.optInt("zone_id"));
            dto.setOrganizacionId(json.optInt("city_id"));
            
            Integer usuarioId = SessionUtil.getUsuarioId(session);
            if (usuarioId == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Sesión inválida.").toString());
                return;
            }
            dto.setUserId(usuarioId);

            String jsonRespuesta = planServicio.registrarNuevoPlan(dto);
            response.getWriter().write(jsonRespuesta);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "JSON mal formado o inválido: " + e.getMessage()).toString());
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doPatch para realizar modificaciones parciales, como actualizar el estado del plan (Enviar a revisión, Aprobar, Rechazar con comentarios) o rellenar detalladamente la identificación del hogar (dirección, calidad de la vivienda, sector y teléfono).
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - accion.equals("change-status"): Ejecuta el cambio de estado del plan de emergencia familiar llamando a vulServicio.cambiarEstadoPlan.
     *   - accion.equals("identify"): Guarda los datos de ubicación e infraestructura de la vivienda llamando a planServicio.guardarIdentificacion.
     * Para qué se usa (el propósito): Actualizar secciones específicas del censo familiar a medida que el voluntario avanza en el formulario o el supervisor evalúa.
     * Por qué es importante (el impacto o problema que resuelve): Posibilita guardar de forma parcial el formulario y enrutar las transiciones de estado del plan a revisión, aprobados o rechazados con comentarios de retroalimentación para el voluntario.
     */
    protected void doPatch(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
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

            if (partes[1].equals("status")) {
                planId = Integer.parseInt(partes[2]);
                accion = "change-status";
            } else {
                planId = Integer.parseInt(partes[1]);
                accion = partes[2];
            }

            // Escenario 1: Cambiar el estado del plan (Enviar, Aprobar, Rechazar)
            if (accion.equals("change-status")) {
                JSONObject json = JSONUtil.leerJson(request);
                int statusPlanId = json.getInt("status_plan_id");
                String comentary = json.has("comentary") && !json.isNull("comentary") ? json.getString("comentary") : null;
                Integer usuarioId = SessionUtil.getUsuarioId(session);
                if (usuarioId == null) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Sesión inválida.").toString());
                    return;
                }

                String resJson = vulServicio.cambiarEstadoPlan(planId, statusPlanId, comentary, usuarioId);
                response.getWriter().write(resJson);
            } 
            // Escenario 2: Guardar los datos de identificación detallados de la vivienda (PATCH de identificación)
            else if (accion.equals("identify")) {
                JSONObject json = JSONUtil.leerJson(request);
                
                ActualizarIdentificacionDTO dto = new ActualizarIdentificacionDTO();
                dto.setLastNames(json.getString("last_names"));
                dto.setAddress(json.getString("address"));
                dto.setHousingQualityId(json.getInt("housing_quality_id"));
                dto.setSectorId(json.getInt("sector_id"));
                dto.setSectorName(json.getString("sector_name"));
                dto.setLandlinePhone(json.optString("landline_phone"));
                dto.setZoneId(json.optInt("zone_id", 0));

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