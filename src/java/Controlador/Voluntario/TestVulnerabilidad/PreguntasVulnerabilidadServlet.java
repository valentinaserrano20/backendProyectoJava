package Controlador.Voluntario.TestVulnerabilidad;

import Modelo.Servicios.Voluntario.VulnerabilidadServicio;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.json.JSONObject;

// Qué hace: Servlet encargado de mapear las peticiones HTTP GET sobre el catálogo de preguntas de vulnerabilidad para calificar el test.
// Por qué existe: Actúa como el controlador que provee el catálogo estático o dinámico de preguntas (infraestructura, ubicación, capacitación) para que el voluntario responda el test de vulnerabilidad.
// Qué pasaría si no estuviera: No se podrían cargar ni mostrar las preguntas del test en el formulario del frontend, impidiendo calificar la vulnerabilidad general del plan.
// @WebServlet("/api/vulnerableQuestions/*")
public class PreguntasVulnerabilidadServlet extends HttpServlet {
    // Qué hace: Instancia el servicio de lógica de negocios para la vulnerabilidad y sus preguntas.
    // Por qué existe: Separa la gestión del protocolo HTTP de la consulta de base de datos de preguntas.
    // Qué pasaría si no estuviera: El servlet debería realizar consultas directas SQL mediante JDBC.
    // Flujo: De aquí pasamos a VulnerabilidadServicio.
    private final VulnerabilidadServicio servicio = new VulnerabilidadServicio();

    // Qué hace: Atiende llamadas HTTP GET para listar preguntas de vulnerabilidad, ya sea en lote completo o paginadas.
    // Por qué existe: Proporciona las preguntas que estructuran el test de vulnerabilidad del plan de emergencia familiar en la interfaz SPA.
    // Qué pasaría si no estuviera: No se podrían renderizar las preguntas dinámicamente en el formulario web del test.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Qué hace: Verifica que exista una sesión de servidor activa para el usuario.
        // Por qué existe: Impide que usuarios anónimos o no autorizados descarguen el listado de preguntas del test.
        // Qué pasaría si no estuviera: Cualquiera podría acceder al catálogo de preguntas de auditoría sin identificarse.
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject()
                .put("success", false)
                .put("message", "Acceso denegado. Inicie sesión.")
                .toString());
            return;
        }
        
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            // Qué hace: Obtiene la totalidad de preguntas de vulnerabilidad disponibles en la base de datos.
            // y luego de esto pasamos a VulnerabilidadServicio.obtenerPreguntas, que realiza un SELECT * de las preguntas.
            String json = servicio.obtenerPreguntas();
            response.getWriter().write(json);
        } else if (pathInfo.equals("/paginate")) {
            int page = 1;
            String pageStr = request.getParameter("page");
            if (pageStr != null) {
                try {
                    page = Integer.parseInt(pageStr);
                } catch (NumberFormatException e) {
                    page = 1;
                }
            }
            // Qué hace: Obtiene las preguntas de vulnerabilidad de forma paginada para realizar la carga dosificada en el frontend.
            // y luego de esto pasamos a VulnerabilidadServicio.obtenerPreguntasPaginadas, que consulta con cláusulas LIMIT y OFFSET.
            String json = servicio.obtenerPreguntasPaginadas(page, 3);
            response.getWriter().write(json);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write(new JSONObject()
                .put("success", false)
                .put("message", "Recurso no encontrado.")
                .toString());
        }
    }
}
