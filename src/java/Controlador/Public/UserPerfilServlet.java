package Controlador.Public;

import Modelo.Servicios.Auth.AuthServicio;
import Modelo.Utilidades.JSONUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import org.json.JSONObject;

@WebServlet(urlPatterns = {"/api/users/profile", "/api/users/profile/phone", "/api/users/profile/email", "/api/users/profile/password"})
public class UserPerfilServlet extends HttpServlet {

    private final AuthServicio authServicio = new AuthServicio();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("user_id") == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print(new JSONObject().put("success", false).put("message", "No autenticado").toString());
                return;
            }

            int userId = (int) session.getAttribute("user_id");
            JSONObject dataPerfil = authServicio.obtenerPerfilDetalladoMapeado(userId);

            JSONObject respuesta = new JSONObject().put("success", true).put("data", dataPerfil);
            response.setStatus(HttpServletResponse.SC_OK);
            out.print(respuesta.toString());

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(new JSONObject().put("success", false).put("message", e.getMessage()).toString());
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        String path = request.getServletPath();

        try {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("user_id") == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print(new JSONObject().put("success", false).put("message", "No autenticado").toString());
                return;
            }

            int userId = (int) session.getAttribute("user_id");
            JSONObject body = JSONUtil.leerJson(request);
            JSONObject respuesta = new JSONObject().put("success", true);

            switch (path) {
                case "/api/users/profile/phone":
                    authServicio.modificarTelefonoPerfil(userId, body.getString("phone"), body.getString("password"));
                    respuesta.put("message", "Número de teléfono actualizado con éxito.");
                    break;

                case "/api/users/profile/email":
                    authServicio.modificarEmailPerfil(userId, body.getString("email"), body.getString("password"));
                    respuesta.put("message", "Correo electrónico actualizado con éxito.");
                    break;

                case "/api/users/profile/password":
                    authServicio.modificarContrasenaPerfil(userId, body.getString("password_actual"), body.getString("password_nueva"));
                    respuesta.put("message", "Su contraseña ha sido modificada con éxito.");
                    break;

                default:
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(new JSONObject().put("success", false).put("message", "Acción no soportada").toString());
                    return;
            }

            response.setStatus(HttpServletResponse.SC_OK);
            out.print(respuesta.toString());

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(new JSONObject().put("success", false).put("message", e.getMessage()).toString());
        }
    }
}