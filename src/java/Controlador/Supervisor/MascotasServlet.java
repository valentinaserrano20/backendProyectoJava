package Controlador.Supervisor;

import Modelo.Servicios.Supervisor.SupervisorServicio;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.json.JSONObject;

// Qué hace: Servlet exclusivo del supervisor que expone el endpoint /api/mascotas/* para obtener la lista global de mascotas registradas.
// Por qué existe: Separa la ruta del supervisor de la ruta /api/pets/* del voluntario, evitando mezcla de roles.
// Qué problema resuelve: Permite al frontend del supervisor consultar todas las mascotas del sistema de forma independiente a la gestión CRUD del voluntario.
@WebServlet("/api/mascotas/*")
public class MascotasServlet extends HttpServlet {

    // Instancia del servicio del supervisor que contiene la lógica de consulta global
    private final SupervisorServicio servicio = new SupervisorServicio();

    // Qué hace: Atiende peticiones GET para retornar el listado global de mascotas con sus planes familiares asociados.
    // Por qué existe: El RevisionPlanController.js del supervisor consume api.get('mascotas/') para filtrar las mascotas de un plan específico.
    // Qué problema resuelve: Devuelve la estructura JSON {success, data} compatible con el helper api.get() del frontend.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Establece el tipo de contenido de la respuesta como JSON con codificación UTF-8
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Verifica que exista una sesión activa con un usuario autenticado
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioId") == null) {
            // Si no hay sesión válida, responde con estado 401 (No autorizado)
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject()
                    .put("success", false)
                    .put("message", "Acceso denegado. Inicie sesión.")
                    .toString());
            return;
        }

        try {
            // Delega la consulta global al servicio del supervisor
            String resJson = servicio.obtenerTodasMascotas();
            // Escribe la respuesta JSON en el cuerpo de la respuesta HTTP
            response.getWriter().write(resJson);
        } catch (Exception e) {
            // Si ocurre un error inesperado, responde con estado 500
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(new JSONObject()
                    .put("success", false)
                    .put("message", "Error interno del servidor: " + e.getMessage())
                    .toString());
        }
    }
}
