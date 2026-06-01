package Controlador.Voluntario;

import Modelo.Servicios.Voluntario.VulnerabilidadServicio;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.json.JSONObject;

// Controlador Servlet renombrado a español PreguntasVulnerabilidadServlet
// Mapeado al endpoint "/api/vulnerableQuestions/*" para mantener compatibilidad con las llamadas del frontend
@WebServlet("/api/vulnerableQuestions/*")
public class PreguntasVulnerabilidadServlet extends HttpServlet {
    // Instancia el servicio de vulnerabilidades para delegar la lógica de negocio
    private final VulnerabilidadServicio servicio = new VulnerabilidadServicio();

    // Intercepta peticiones HTTP GET
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Define el tipo de contenido como JSON
        response.setContentType("application/json");
        // Configura la codificación a UTF-8 para soportar tildes y caracteres especiales
        response.setCharacterEncoding("UTF-8");

        // Intenta recuperar la sesión actual sin crear una nueva
        HttpSession session = request.getSession(false);
        // Valida si la sesión ha expirado o no posee un ID de usuario válido
        if (session == null || session.getAttribute("usuarioId") == null) {
            // Establece el código de estado HTTP a 401 (No Autorizado)
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            // Escribe un JSON informando el rechazo de acceso por seguridad
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            // Cancela el procesamiento del servlet
            return;
        }

        // Obtiene la ruta relativa solicitada después del mapeo base (ej. /paginate)
        String pathInfo = request.getPathInfo();
        
        // Si no se especifica ninguna ruta adicional o es la raíz
        if (pathInfo == null || pathInfo.equals("/")) {
            // Solicita todas las preguntas activas y las almacena en una cadena JSON
            String json = servicio.obtenerPreguntas();
            // Envía la respuesta JSON al cliente
            response.getWriter().write(json);
        } 
        // Si el cliente solicita paginación de preguntas
        else if (pathInfo.equals("/paginate")) {
            // Inicializa la variable de página en 1 por defecto
            int page = 1;
            // Lee el parámetro de consulta "page"
            String pageStr = request.getParameter("page");
            // Si el parámetro está presente
            if (pageStr != null) {
                try {
                    // Intenta convertir el parámetro de página a un entero válido
                    page = Integer.parseInt(pageStr);
                } catch (NumberFormatException e) {
                    // Si falla la conversión, revierte al valor por defecto (página 1)
                    page = 1;
                }
            }
            // Solicita las preguntas correspondientes a la página con perPage = 3
            String json = servicio.obtenerPreguntasPaginadas(page, 3);
            // Envía el JSON paginado al cliente
            response.getWriter().write(json);
        } 
        // Si la ruta solicitada no coincide con ninguna acción soportada
        else {
            // Establece el código de estado HTTP a 404 (No Encontrado)
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            // Escribe la respuesta de error en formato JSON
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Recurso no encontrado.").toString());
        }
    }
}
