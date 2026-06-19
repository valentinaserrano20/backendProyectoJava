package Controlador.Voluntario.TestVulnerabilidad;

import Modelo.DTO.RespuestaTestDTO;
import Modelo.Servicios.Voluntario.VulnerabilidadServicio;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

// Qué hace: Servlet encargado de mapear las peticiones HTTP (GET, POST) sobre el test de vulnerabilidad.
// Por qué existe: Actúa como el controlador para recuperar las respuestas de una familia y procesar/calificar el lote de respuestas del test de vulnerabilidad.
// Qué pasaría si no estuviera: Las respuestas al test de vulnerabilidad no se guardarían ni calificarían en la base de datos, impidiendo determinar si la vulnerabilidad es alta, media o baja.
@WebServlet("/api/vulnerableTest")
public class TestVulnerabilidadServlet extends HttpServlet {
    // Qué hace: Instancia el servicio de lógica de negocios de vulnerabilidad.
    // Por qué existe: Mantiene aislada la lógica de base de datos y de cálculo del servlet de red.
    // Qué pasaría si no estuviera: Sería necesario procesar JDBC y lógica de cálculo del promedio de vulnerabilidad directamente en los métodos de este servlet.
    // Flujo: De aquí pasamos a VulnerabilidadServicio.
    private final VulnerabilidadServicio servicio = new VulnerabilidadServicio();

    // Qué hace: Atiende peticiones HTTP GET para la precarga de respuestas de la familia.
    // Por qué existe: Permite que el frontend consulte si la familia ya tiene respuestas previas guardadas y las dibuje en el formulario.
    // Qué pasaría si no estuviera: Al recargar o reabrir el formulario, el usuario vería el test vacío y no sabría qué respondió previamente.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Qué hace: Comprueba la existencia de una sesión de usuario activa.
        // Por qué existe: Protege los datos e informes de seguridad habitacional de accesos externos no autenticados.
        // Qué pasaría si no estuviera: Cualquier persona podría auditar las respuestas del test de cualquier familia sin credenciales.
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            return;
        }

        String planIdStr = request.getParameter("family_plan_id");
        if (planIdStr == null || planIdStr.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Falta el parámetro family_plan_id.").toString());
            return;
        }

        try {
            int planId = Integer.parseInt(planIdStr);
            
            // Qué hace: Obtiene las respuestas al test vinculadas al plan familiar.
            // y luego de esto pasamos a VulnerabilidadServicio.obtenerRespuestasPlan, el cual hace el SELECT relacional en MySQL.
            String json = servicio.obtenerRespuestasPlan(planId);
            response.getWriter().write(json);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "family_plan_id debe ser un entero válido.").toString());
        }
    }

    // Qué hace: Atiende peticiones HTTP POST para guardar el lote de respuestas y calificar la vulnerabilidad.
    // Por qué existe: Recibe las respuestas del formulario del test de vulnerabilidad y calcula el promedio ponderado para asignarle el estado al plan familiar.
    // Qué pasaría si no estuviera: No podríamos guardar las respuestas del voluntario ni actualizar el semáforo de riesgo de la familia.
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Qué hace: Valida la sesión activa.
        // Por qué existe: Evita que usuarios anónimos envíen respuestas ficticias alterando la estadística de vulnerabilidad.
        // Qué pasaría si no estuviera: El sistema estaría expuesto a inserciones de tests aleatorios por bots externos.
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            return;
        }

        StringBuilder buffer = new StringBuilder();
        String linea;
        try (BufferedReader reader = request.getReader()) {
            while ((linea = reader.readLine()) != null) {
                buffer.append(linea);
            }
        }

        try {
            JSONObject json = new JSONObject(buffer.toString());

            int planId = json.getInt("family_plan_id");
            JSONArray answersArray = json.getJSONArray("answers");

            List<RespuestaTestDTO> respuestas = new ArrayList<>();
            for (int i = 0; i < answersArray.length(); i++) {
                JSONObject item = answersArray.getJSONObject(i);
                RespuestaTestDTO dto = new RespuestaTestDTO(
                    item.getInt("vulnerable_question_id"),
                    item.getBoolean("answer")
                );
                respuestas.add(dto);
            }

            // Qué hace: Procesa la lista de respuestas y recalcula el puntaje de vulnerabilidad del plan.
            // y luego de esto pasamos a VulnerabilidadServicio.procesarGuardadoLote, que guarda en BD y actualiza el plan familiar.
            String respuestaJson = servicio.procesarGuardadoLote(planId, respuestas);
            response.getWriter().write(respuestaJson);

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "JSON mal formado o faltan campos obligatorios.").toString());
        }
    }
}
