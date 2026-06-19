package Controlador.Voluntario;

import Modelo.DTO.ActionPlanActionDTO;
import Modelo.Servicios.Voluntario.ActionPlanActionServicio;
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
 * Qué hace: Servlet controlador mapeado a /api/actionPlanActions/* que procesa todas las solicitudes HTTP del CRUD de micro-acciones (Antes, Durante, Después).
 * Por qué existe: Actúa como punto de entrada de la API para las operaciones del módulo de tareas del Plan de Acción en la interfaz SPA del voluntario.
 * Qué pasaría si no estuviera: Los voluntarios no tendrían forma de añadir, ver, editar o eliminar las micro-acciones asociadas a los planes de emergencia familiar.
 */
@WebServlet("/api/actionPlanActions/*")
public class ActionPlanActionsServlet extends HttpServlet {

    // Qué hace: Instancia el servicio de lógica de negocio para las tareas individuales de los planes de acción.
    // Por qué existe: Delega la persistencia y las reglas de negocio al servicio de micro-acciones.
    // Qué pasaría si no estuviera: Habría que programar consultas JDBC y mapeos JSON directamente dentro de este controlador web.
    // Flujo: De aquí pasamos a ActionPlanActionServicio.
    private final ActionPlanActionServicio servicio = new ActionPlanActionServicio();

    // Qué hace: Captura las peticiones HTTP e enruta las de tipo PATCH hacia el método doPatch.
    // Por qué existe: Los servlets tradicionales de Java no tienen un método doPatch heredable por defecto.
    // Qué pasaría si no estuviera: Peticiones de tipo PATCH del frontend para actualizar campos específicos arrojarían un error HTTP 405 (Method Not Allowed).
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

    // Qué hace: Atiende peticiones HTTP GET para listar acciones de un plan o consultar los detalles de una acción puntual.
    // Por qué existe: Permite renderizar y refrescar la lista de tareas en las fases de la SPA.
    // Qué pasaría si no estuviera: No se podrían cargar ni pintar las tareas registradas en el frontend.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Qué hace: Valida la existencia de una sesión de voluntario activa.
        // Por qué existe: Impide que usuarios no autenticados accedan a la lista de tareas de seguridad de la vivienda.
        // Qué pasaría si no estuviera: Cualquiera en internet podría espiar el plan de acción familiar y sus responsables asociados.
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            return;
        }

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Ruta no especificada.").toString());
            return;
        }

        String[] parts = pathInfo.split("/");
        
        try {
            // Caso 1: actionPlanActions/actionPlan/{planId}
            if (parts[1].equals("actionPlan") && parts.length > 2) {
                int planId = Integer.parseInt(parts[2]);
                
                // Qué hace: Listado de acciones ligadas al plan de acción familiar.
                // y luego de esto pasamos a ActionPlanActionServicio.listarPorPlan, el cual obtiene el listado desde la BD en formato JSON.
                String jsonRes = servicio.listarPorPlan(planId);
                response.getWriter().write(jsonRes);
            } 
            // Caso 2: actionPlanActions/{id}
            else if (parts.length == 2) {
                int id = Integer.parseInt(parts[1]);
                
                // Qué hace: Recupera una acción puntual mediante su identificador primario.
                // y luego de esto pasamos a ActionPlanActionServicio.obtenerPorId, el cual consulta la micro-acción en base de datos.
                String jsonRes = servicio.obtenerPorId(id);
                response.getWriter().write(jsonRes);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(new JSONObject().put("success", false).put("message", "Ruta de consulta no válida.").toString());
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "El identificador de ruta debe ser numérico.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al procesar consulta: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Recibe peticiones HTTP POST para agregar una nueva micro-acción o tarea.
    // Por qué existe: Atiende la creación de nuevas tareas desde los cuadros modales interactivos.
    // Qué pasaría si no estuviera: Los voluntarios no tendrían forma de añadir micro-acciones dinámicas a su plan.
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

        StringBuilder buffer = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
        }

        try {
            JSONObject json = new JSONObject(buffer.toString());
            ActionPlanActionDTO dto = new ActionPlanActionDTO();
            dto.setDescription(json.getString("description"));
            dto.setActionTypeId(json.getInt("action_type_id"));
            dto.setActionPlanId(json.getInt("action_plan_id"));
            
            String memberIdStr = json.optString("member_id", "");
            if (!memberIdStr.isEmpty()) {
                dto.setMemberId(Integer.parseInt(memberIdStr));
            }

            // Qué hace: Crea una nueva micro-acción en la BD.
            // y luego de esto pasamos a ActionPlanActionServicio.crear, el cual realiza la validación e inserción en la BD.
            String jsonRes = servicio.crear(dto);
            response.getWriter().write(jsonRes);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al registrar la acción: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Procesa solicitudes HTTP PATCH para actualizar la descripción y responsable de una acción existente.
    // Por qué existe: Atiende el guardado tras modificar los campos en los cuadros modales de edición.
    // Qué pasaría si no estuviera: No podríamos actualizar el texto ni el responsable familiar de las tareas del plan de acción.
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
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de acción no provisto.").toString());
            return;
        }

        String[] parts = pathInfo.split("/");
        
        try {
            int id = Integer.parseInt(parts[1]);
            
            StringBuilder buffer = new StringBuilder();
            String line;
            try (BufferedReader reader = request.getReader()) {
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }
            }

            JSONObject json = new JSONObject(buffer.toString());
            ActionPlanActionDTO dto = new ActionPlanActionDTO();
            dto.setDescription(json.getString("description"));
            
            String memberIdStr = json.optString("member_id", "");
            if (!memberIdStr.isEmpty()) {
                dto.setMemberId(Integer.parseInt(memberIdStr));
            }

            // Qué hace: Actualiza la micro-acción identificada.
            // y luego de esto pasamos a ActionPlanActionServicio.actualizar, el cual aplica los cambios en MySQL.
            String jsonRes = servicio.actualizar(id, dto);
            response.getWriter().write(jsonRes);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de acción debe ser numérico.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al actualizar la acción: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Procesa solicitudes HTTP DELETE para eliminar una micro-acción específica por su ID.
    // Por qué existe: Habilita la eliminación física de micro-acciones desde el modal SweetAlert.
    // Qué pasaría si no estuviera: Las micro-acciones creadas por error quedarían registradas para siempre.
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
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
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de acción no provisto.").toString());
            return;
        }

        String[] parts = pathInfo.split("/");
        
        try {
            int id = Integer.parseInt(parts[1]);
            
            // Qué hace: Elimina físicamente la tarea por su ID primario.
            // y luego de esto pasamos a ActionPlanActionServicio.eliminar, el cual ejecuta la remoción en base de datos.
            String jsonRes = servicio.eliminar(id);
            response.getWriter().write(jsonRes);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de acción debe ser numérico.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al eliminar la acción: " + e.getMessage()).toString());
        }
    }
}
