package Controlador.Auth;

// =========================================================
// IMPORTACIONES OBLIGATORIAS
// =========================================================
import Modelo.Servicios.Auth.AuthServicio;
import Modelo.Utilidades.JSONUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import org.json.JSONObject;

/**
 * Servlet: ForgotPasswordServlet
 * Capa: Controlador.Auth
 * Mapeo: Intercepta las tres interacciones JSON del enrutador de recuperación.
 */
@WebServlet(urlPatterns = {"/api/forgotPassword", "/api/verifyCode", "/api/changePassword"})
public class ForgotPasswordServlet extends HttpServlet {

    private final AuthServicio authServicio = new AuthServicio();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Formato contractual estandarizado
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        // Obtener cuál de las rutas mapeadas invocó el fetch
        String path = request.getServletPath();

        try {
            // Leer el body JSON unificado
            JSONObject body = JSONUtil.leerJson(request);
            JSONObject respuestaJson = new JSONObject();

            switch (path) {
                case "/api/forgotPassword":
                    String email = body.getString("email");
                    authServicio.procesarSolicitudRecuperacion(email);
                    
                    respuestaJson.put("success", true);
                    respuestaJson.put("message", "Código de recuperación enviado. Por favor revise su bandeja de correo.");
                    response.setStatus(HttpServletResponse.SC_OK); // 200 OK
                    break;

                case "/api/verifyCode":
                    String token = body.getString("token"); // Lee los 6 dígitos unificados del front
                    boolean valido = authServicio.verificarTokenValido(token);
                    
                    if (valido) {
                        respuestaJson.put("success", true);
                        respuestaJson.put("message", "Código verificado con éxito.");
                        response.setStatus(HttpServletResponse.SC_OK); // 200 OK
                    } else {
                        respuestaJson.put("success", false);
                        respuestaJson.put("message", "El código ingresado es incorrecto o ya expiró.");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400 Bad Request
                    }
                    break;

                case "/api/changePassword":
                    String tokenFinal = body.getString("token");
                    String nuevaClave = body.getString("password");
                    
                    authServicio.restablecerContrasenaFinal(tokenFinal, nuevaClave);
                    
                    respuestaJson.put("success", true);
                    respuestaJson.put("message", "Su contraseña ha sido actualizada con éxito. Ya puede iniciar sesión.");
                    response.setStatus(HttpServletResponse.SC_OK); // 200 OK
                    break;

                default:
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND); // 404
                    respuestaJson.put("success", false).put("message", "Ruta de recuperación inexistente.");
                    break;
            }

            // Despachar la respuesta al cliente
            out.print(respuestaJson.toString());

        } catch (Exception e) {
            // Control estricto de errores contractuales
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
            out.print(new JSONObject()
                    .put("success", false)
                    .put("message", e.getMessage())
                    .toString());
        }
    }
}