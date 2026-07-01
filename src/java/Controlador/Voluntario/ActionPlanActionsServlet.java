package Controlador.Voluntario;

/*
 * Qué hace (la acción): Importa el DTO de acciones de plan, el servicio de gestión de acciones, las utilidades de JSON y respuestas web, y las clases de Jakarta Servlet.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.DTO.ActionPlanActionDTO: Contenedor que modela la información de una micro-acción (Antes, Durante, Después) del Plan de Acción.
 *   - Modelo.Servicios.Voluntario.ActionPlanActionServicio: Clase que implementa la lógica de negocio para crear, actualizar, listar y borrar tareas.
 * Para qué se usa (el propósito): Proveer al servlet las dependencias de transferencia de datos y negocio para administrar las acciones del plan.
 * Por qué es important (el impacto o problema que resuelve): Sin estas importaciones, el controlador no podría coordinar las peticiones HTTP con la lógica de negocio que persiste los planes familiares.
 */
import Modelo.DTO.ActionPlanActionDTO;
import Modelo.Servicios.Voluntario.ActionPlanActionServicio;
import Modelo.Utilidades.JSONUtil;
import Modelo.Utilidades.ResponseUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.json.JSONObject;

/*
 * Qué hace (la acción): Asocia el servlet ActionPlanActionsServlet con el patrón de ruta "/api/actionPlanActions/*" a través de la anotación @WebServlet.
 * Qué significa (conceptos, métodos, tipos involucrados): @WebServlet es el decorador de Jakarta que registra el mapeo de red del servlet en Tomcat.
 * Para qué se usa (el propósito): Servir como el endpoint central de la API para administrar las micro-acciones (tareas) específicas de los planes de contingencia familiar.
 * Por qué es importante (el impacto o problema que resuelve): Permite interceptar llamadas RESTful de origen cruzado para gestionar individualmente o en masa las tareas asignadas a cada miembro del hogar ante desastres.
 */
@WebServlet("/api/actionPlanActions/*")
public class ActionPlanActionsServlet extends HttpServlet {

    /*
     * Qué hace (la acción): Instancia de manera privada y constante la variable servicio de tipo ActionPlanActionServicio.
     * Qué significa (conceptos, métodos, tipos involucrados): Instancia de la clase de servicios de negocio para tareas de planes familiares.
     * Para qué se usa (el propósito): Invocar los métodos del CRUD de acciones.
     */
    private final ActionPlanActionServicio servicio = new ActionPlanActionServicio();

    /*
     * Qué hace (la acción): Sobrescribe el método service para desviar las peticiones que utilizan el verbo HTTP PATCH hacia el método doPatch.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - service: Método del ciclo de vida del servlet que se ejecuta antes de delegar a doGet, doPost, etc.
     *   - PATCH: Método HTTP utilizado para la actualización parcial de recursos.
     * Para qué se usa (el propósito): Habilitar el soporte del método PATCH en la API de servlets de Jakarta.
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
     * Qué hace (la acción): Sobrescribe el método doGet para procesar consultas GET, permitiendo listar las acciones de un plan familiar específico o consultar los detalles de una sola acción a partir de su ID.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - parts[1].equals("actionPlan"): Detecta si la ruta solicita el listado de tareas vinculadas a un plan (ej: "/actionPlan/5").
     *   - parts.length == 2: Detecta si se solicita la información detallada de una única acción por su ID.
     * Para qué se usa (el propósito): Alimentar la tabla de tareas y la ventana de edición en la interfaz del voluntario.
     * Por qué es importante (el impacto o problema que resuelve): Garantiza que el voluntario obtenga la lista exacta de tareas del plan familiar o el detalle de una tarea específica con control de errores de formato numérico.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Ruta no especificada."));
            return;
        }

        String[] parts = pathInfo.split("/");
        
        try {
            // Caso 1: actionPlanActions/actionPlan/{planId}
            if (parts[1].equals("actionPlan") && parts.length > 2) {
                int planId = Integer.parseInt(parts[2]);
                String jsonRes = servicio.listarPorPlan(planId);
                response.getWriter().write(jsonRes);
            } 
            // Caso 2: actionPlanActions/{id}
            else if (parts.length == 2) {
                int id = Integer.parseInt(parts[1]);
                String jsonRes = servicio.obtenerPorId(id);
                response.getWriter().write(jsonRes);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(ResponseUtil.error("Ruta de consulta no válida."));
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("El identificador de ruta debe ser numérico."));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al procesar consulta: " + e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doPost para agregar una nueva tarea o acción de contingencia familiar.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - JSONUtil.leerJson(request): Parsea la petición asíncrona a JSONObject.
     *   - memberIdStr: ID del integrante de la familia asignado como responsable de la tarea (campo opcional).
     * Para qué se usa (el propósito): Crear y persistir una nueva acción (ej. "Cerrar llaves del gas") asignándola a una fase y plan familiar.
     * Por qué es importante (el impacto o problema que resuelve): Permite registrar tareas y asignar un responsable familiar (si aplica), previniendo fallos en JSON mal estructurados.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        try {
            JSONObject json = JSONUtil.leerJson(request);
            ActionPlanActionDTO dto = new ActionPlanActionDTO();
            dto.setDescription(json.getString("description"));
            dto.setActionTypeId(json.getInt("action_type_id"));
            dto.setActionPlanId(json.getInt("action_plan_id"));
            
            String memberIdStr = json.optString("member_id", "");
            if (!memberIdStr.isEmpty()) {
                dto.setMemberId(Integer.parseInt(memberIdStr));
            }

            String jsonRes = servicio.crear(dto);
            response.getWriter().write(jsonRes);
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("JSON mal formado: " + e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al registrar la acción: " + e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Define la lógica doPatch para actualizar la descripción o cambiar el responsable asignado a una tarea de contingencia familiar.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - doPatch: Procesador de cambios parciales.
     *   - servicio.actualizar(id, dto): Actualiza en la base de datos la fila del registro seleccionado.
     * Para qué se usa (el propósito): Modificar una tarea ya creada en la interfaz sin necesidad de rellenar de nuevo el tipo de acción o plan familiar al que pertenece.
     * Por qué es importante (el impacto o problema que resuelve): Permite modificar de forma ágil y asíncrona la descripción y responsable de la tarea de evacuación.
     */
    protected void doPatch(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID de acción no provisto."));
            return;
        }

        String[] parts = pathInfo.split("/");
        
        try {
            int id = Integer.parseInt(parts[1]);
            JSONObject json = JSONUtil.leerJson(request);
            ActionPlanActionDTO dto = new ActionPlanActionDTO();
            dto.setDescription(json.getString("description"));
            
            String memberIdStr = json.optString("member_id", "");
            if (!memberIdStr.isEmpty()) {
                dto.setMemberId(Integer.parseInt(memberIdStr));
            }

            String jsonRes = servicio.actualizar(id, dto);
            response.getWriter().write(jsonRes);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID de acción debe ser numérico."));
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("JSON mal formado: " + e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al actualizar la acción: " + e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doDelete para eliminar físicamente una tarea o acción de contingencia familiar por su ID.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - servicio.eliminar(id): Elimina de la tabla SQL la fila correspondiente.
     * Para qué se usa (el propósito): Permitir que el voluntario descarte tareas innecesarias de su plan familiar de emergencia.
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID de acción no provisto."));
            return;
        }

        String[] parts = pathInfo.split("/");
        
        try {
            int id = Integer.parseInt(parts[1]);
            String jsonRes = servicio.eliminar(id);
            response.getWriter().write(jsonRes);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID de acción debe ser numérico."));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al eliminar la acción: " + e.getMessage()));
        }
    }
}
