package Controlador.Voluntario;

import Modelo.DTO.ActionPlanDTO;
import Modelo.Servicios.Voluntario.ActionPlanServicio;
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
 * Qué hace: Servlet controlador mapeado a /api/actionPlans/* que procesa todas las solicitudes HTTP del plan de acción familiar general.
 * Por qué existe: Actúa como punto de entrada de la API para las operaciones del módulo de Plan de Acción en la interfaz SPA del voluntario.
 * Qué pasaría si no estuviera: El frontend no podría consultar, inicializar ni actualizar los planes de acción familiares de emergencia de los ciudadanos.
 */
@WebServlet("/api/actionPlans/*")
public class ActionPlansServlet extends HttpServlet {

    // Qué hace: Crea una instancia del servicio de lógica de negocios para los planes de acción.
    // Por qué existe: Delega la verificación, creación y actualización de los planes de acción de los planes familiares.
    // Qué pasaría si no estuviera: Sería necesario programar lógica SQL redundante dentro de los métodos del controlador.
    // Flujo: De aquí pasamos a ActionPlanServicio.
    private final ActionPlanServicio servicio = new ActionPlanServicio();

    // Qué hace: Atiende peticiones HTTP GET para verificar si el plan de acción existe o para obtener los datos superiores de coordinador/riesgo.
    // Por qué existe: Permite consultar y renderizar la información base en los selects de la pantalla del voluntario.
    // Qué pasaría si no estuviera: La interfaz del voluntario no podría precargar los datos de los planes de acción registrados.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Qué hace: Comprueba que el usuario tenga una sesión activa y válida en el servidor.
        // Por qué existe: Restringe el acceso a los planes de acción exclusivamente a los voluntarios que han iniciado sesión.
        // Qué pasaría si no estuviera: Cualquiera podría acceder a los endpoints /api/actionPlans/* y modificar planes familiares de forma anónima.
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            return;
        }

        // Qué hace: Obtiene los segmentos adicionales en la URL.
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Ruta no especificada.").toString());
            return;
        }

        String[] parts = pathInfo.split("/");
        
        try {
            // Caso 1: actionPlans/familyPlan/boolean/{planId}
            if (parts[1].equals("familyPlan") && parts.length > 2 && parts[2].equals("boolean")) {
                int planId = Integer.parseInt(parts[3]);
                
                // Qué hace: Verifica si ya existe un plan de acción creado para el plan familiar.
                // y luego de esto pasamos a ActionPlanServicio.verificarExistePlan, que consulta la existencia en base de datos.
                String jsonRes = servicio.verificarExistePlan(planId);
                response.getWriter().write(jsonRes);
            } 
            // Caso 2: actionPlans/familyPlan/{planId}
            else if (parts[1].equals("familyPlan") && parts.length == 3) {
                int planId = Integer.parseInt(parts[2]);
                
                // Qué hace: Recupera el plan de acción familiar correspondiente desde el servicio.
                // y luego de esto pasamos a ActionPlanServicio.obtenerPlan, que obtiene el registro base del plan de acción.
                String jsonRes = servicio.obtenerPlan(planId);
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

    // Qué hace: Recibe peticiones HTTP POST para insertar las tareas por defecto iniciales del plan de acción.
    // Por qué existe: Permite crear por primera vez el plan de acción familiar general desde el frontend.
    // Qué pasaría si no estuviera: No podríamos inicializar la lista de tareas obligatorias del censo de emergencia.
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

        // Qué hace: Lee el cuerpo JSON enviado por la red.
        StringBuilder buffer = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
        }

        try {
            JSONObject json = new JSONObject(buffer.toString());
            ActionPlanDTO dto = new ActionPlanDTO();
            dto.setMemberId(json.getInt("member_id"));
            dto.setRiskFactorId(json.getInt("risk_factor_id"));
            dto.setFamilyPlanId(json.getInt("family_plan_id")); 

            // Qué hace: Ejecuta la creación del plan de acción familiar y sus tareas asociadas.
            // y luego de esto pasamos a ActionPlanServicio.crearPlan, el cual inserta las filas correspondientes en la BD.
            String jsonRes = servicio.crearPlan(dto);
            response.getWriter().write(jsonRes);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al registrar plan de acción: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Procesa solicitudes HTTP PUT para actualizar el coordinador y factor de riesgo para todas las tareas asociadas.
    // Por qué existe: Atiende la petición de guardado al modificar los selectores del formulario de edición.
    // Qué pasaría si no estuviera: No podríamos reasignar coordinadores familiares o factores de riesgo generales a los planes de acción.
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) 
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
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID del plan de acción no provisto.").toString());
            return;
        }

        String[] parts = pathInfo.split("/");
        
        try {
            int planId = Integer.parseInt(parts[1]); 
            
            StringBuilder buffer = new StringBuilder();
            String line;
            try (BufferedReader reader = request.getReader()) {
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }
            }

            JSONObject json = new JSONObject(buffer.toString());
            ActionPlanDTO dto = new ActionPlanDTO();
            dto.setMemberId(json.getInt("member_id"));
            dto.setRiskFactorId(json.getInt("risk_factor_id"));

            // Qué hace: Llama al servicio para actualizar los datos base de coordinador y riesgos en el plan de acción.
            // y luego de esto pasamos a ActionPlanServicio.actualizarPlan, que ejecuta el UPDATE SQL correspondiente en la base de datos.
            String jsonRes = servicio.actualizarPlan(planId, dto);
            response.getWriter().write(jsonRes);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de plan de acción debe ser numérico.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al actualizar plan de acción: " + e.getMessage()).toString());
        }
    }
}
