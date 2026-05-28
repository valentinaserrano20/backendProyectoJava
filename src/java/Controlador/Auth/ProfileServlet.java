package Controlador.Auth;

// =========================================================
// IMPORTACIONES OBLIGATORIAS
// =========================================================
import Modelo.Servicios.Auth.AuthServicio;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import org.json.JSONObject;

/**
 * Servlet: ProfileServlet -> Intercepta la ruta /api/users/profile
 * Capa: Controlador.Auth
 * Responsabilidad: 
 * - Proteger rutas privadas verificando la existencia de cookies de sesión HTTP.
 * - Coordinar con la capa de servicio para responder payloads JSON estandarizados.
 */
@WebServlet("/api/users/profile")
public class ProfileServlet extends HttpServlet {

    /**
     * Procesa peticiones HTTP GET enviadas desde el controlador javascript del SPA.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Estándar del contrato: Salida explícita en formato JSON con codificación UTF-8
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        try {
            // REGLA DE SEGURIDAD 4: Obtener sesión existente. NO crear una nueva (false).
            HttpSession session = request.getSession(false);
            
            if (session == null || session.getAttribute("user_id") == null) {
                // Código HTTP 401 Unauthorized para detonar la redirección en el Front
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); 
                out.print(new JSONObject()
                        .put("success", false)
                        .put("message", "Acceso denegado. Sesión inválida o expirada.")
                        .toString());
                return;
            }
            
            // Extracción segura del ID enlazado tras el login
            int userId = (int) session.getAttribute("user_id");
            
            // Instanciar y coordinar de forma aislada con la capa de negocio
            AuthServicio authServicio = new AuthServicio();
            JSONObject datosMapeados = authServicio.obtenerPerfilMapeado(userId);
            
            if (datosMapeados == null) {
                // Código HTTP 404 Not Found si el ID quedó huérfano en la BD
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print(new JSONObject()
                        .put("success", false)
                        .put("message", "El usuario actual no pudo ser localizado en el sistema.")
                        .toString());
                return;
            }
            
            // ESTÁNDAR DE RESPUESTA 1: Envío exitoso encapsulado en la llave "data"
            JSONObject respuestaExitosa = new JSONObject();
            respuestaExitosa.put("success", true);
            respuestaExitosa.put("message", "Perfil del voluntario recuperado con éxito");
            respuestaExitosa.put("data", datosMapeados);
            
            response.setStatus(HttpServletResponse.SC_OK); // 200 OK
            out.print(respuestaExitosa.toString());
            
        } catch (Exception e) {
            // Captura integral contra caídas imprevistas de BD o NullPointers
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500
            out.print(new JSONObject()
                    .put("success", false)
                    .put("message", "Error interno de infraestructura Java: " + e.getMessage())
                    .toString());
        }
    }
}