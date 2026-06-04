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

// Controlador Servlet renombrado a español PreguntasVulnerabilidadServlet
// Mapeado al endpoint "/api/vulnerableQuestions/*"
// @WebServlet("/api/vulnerableQuestions/*")
public class PreguntasVulnerabilidadServlet extends HttpServlet {
    private final VulnerabilidadServicio servicio = new VulnerabilidadServicio();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
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
