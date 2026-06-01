package Controlador.Voluntario;

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

// Controlador Servlet renombrado a español TestVulnerabilidadServlet
// Mapeado al endpoint "/api/vulnerableTest" para mantener compatibilidad con las llamadas del frontend
@WebServlet("/api/vulnerableTest")
public class TestVulnerabilidadServlet extends HttpServlet {
    // Instancia el servicio de vulnerabilidades
    private final VulnerabilidadServicio servicio = new VulnerabilidadServicio();

    // Intercepta peticiones HTTP GET para la precarga de respuestas de la familia
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Define el tipo de respuesta a JSON
        response.setContentType("application/json");
        // Establece la codificación UTF-8
        response.setCharacterEncoding("UTF-8");

        // Intenta recuperar la sesión del usuario
        HttpSession session = request.getSession(false);
        // Valida la existencia y validez de la sesión
        if (session == null || session.getAttribute("usuarioId") == null) {
            // Establece HTTP 401 si no está autorizado
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            // Escribe el JSON de rechazo
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            // Detiene la ejecución
            return;
        }

        // Recupera el parámetro family_plan_id de la URL
        String planIdStr = request.getParameter("family_plan_id");
        // Valida que el parámetro no esté vacío
        if (planIdStr == null || planIdStr.trim().isEmpty()) {
            // Establece HTTP 400 (Bad Request)
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            // Escribe el mensaje de error
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Falta el parámetro family_plan_id.").toString());
            // Detiene la ejecución
            return;
        }

        try {
            // Intenta parsear el ID del plan a entero
            int planId = Integer.parseInt(planIdStr);
            // Llama al servicio para obtener las respuestas guardadas de este plan
            String json = servicio.obtenerRespuestasPlan(planId);
            // Envía las respuestas en JSON al cliente
            response.getWriter().write(json);
        } catch (NumberFormatException e) {
            // Si el ID del plan no es un entero válido, establece HTTP 400
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            // Envía la respuesta de error
            response.getWriter().write(new JSONObject().put("success", false).put("message", "family_plan_id debe ser un entero válido.").toString());
        }
    }

    // Intercepta peticiones HTTP POST para guardar el lote de respuestas y calificar la vulnerabilidad
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Configura el tipo de contenido a JSON
        response.setContentType("application/json");
        // Establece la codificación de caracteres
        response.setCharacterEncoding("UTF-8");

        // Recupera la sesión activa
        HttpSession session = request.getSession(false);
        // Valida que el usuario esté debidamente autenticado en el servidor
        if (session == null || session.getAttribute("usuarioId") == null) {
            // Establece HTTP 401 si no está autorizado
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            // Responde al cliente informando la falta de sesión
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            // Cancela el procesamiento
            return;
        }

        // Inicializa un buffer de cadenas para leer el flujo de entrada
        StringBuilder buffer = new StringBuilder();
        String linea;
        // Abre el lector de flujo del cuerpo de la petición HTTP
        try (BufferedReader reader = request.getReader()) {
            // Lee línea por línea el JSON enviado por el cliente
            while ((linea = reader.readLine()) != null) {
                // Acumula la línea en el buffer
                buffer.append(linea);
            }
        }

        try {
            // Instancia el objeto JSON a partir de la cadena leída del stream
            JSONObject json = new JSONObject(buffer.toString());
            
            // Extrae el ID del plan familiar en progreso
            int planId = json.getInt("family_plan_id");
            // Extrae el arreglo JSON que contiene las respuestas (answers)
            JSONArray answersArray = json.getJSONArray("answers");
            
            // Inicializa la lista de respuestas DTO
            List<RespuestaTestDTO> respuestas = new ArrayList<>();
            // Itera sobre los elementos del arreglo JSON de respuestas
            for (int i = 0; i < answersArray.length(); i++) {
                // Obtiene el objeto JSON de la respuesta actual
                JSONObject item = answersArray.getJSONObject(i);
                // Instancia el DTO leyendo el ID de pregunta y el valor booleano
                RespuestaTestDTO dto = new RespuestaTestDTO(
                    item.getInt("vulnerable_question_id"),
                    item.getBoolean("answer")
                );
                // Agrega el DTO a la lista del lote
                respuestas.add(dto);
            }

            // Delega el guardado y calificación en lote al servicio
            String respuestaJson = servicio.procesarGuardadoLote(planId, respuestas);
            // Envía la respuesta en String JSON al frontend
            response.getWriter().write(respuestaJson);

        } catch (Exception e) {
            // Si el parsing falla o faltan campos obligatorios, establece HTTP 400
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            // Informa del error de formato JSON
            response.getWriter().write(new JSONObject().put("success", false).put("message", "JSON mal formado o faltan campos obligatorios.").toString());
        }
    }
}
