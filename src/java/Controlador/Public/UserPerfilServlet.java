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

// Qué hace: Servlet encargado de gestionar las operaciones de lectura y actualización del perfil del usuario logueado (teléfono, correo, contraseña).
// Por qué existe: Expone endpoints específicos de perfil bajo la ruta '/api/users/profile/*' para que el voluntario o supervisor administre sus propios datos de acceso.
// Qué problema resuelve: Protege y aísla la edición de información sensible del perfil de usuario mediante autenticación obligatoria y validación de contraseña.
@WebServlet(urlPatterns = {"/api/users/profile", "/api/users/profile/phone", "/api/users/profile/email", "/api/users/profile/password"})
public class UserPerfilServlet extends HttpServlet {

    // Qué hace: Instancia el servicio de autenticación y gestión de usuarios.
    // Por qué existe: Contiene las reglas de negocio y algoritmos de cifrado de contraseñas.
    // Qué pasaría si no estuviera: Tendríamos que cifrar contraseñas y realizar actualizaciones SQL directo en el controlador.
    // Flujo: De aquí pasamos a AuthServicio.
    private final AuthServicio authServicio = new AuthServicio();

    // Qué hace: Atiende peticiones GET para obtener la ficha de perfil completa del usuario en sesión.
    // Por qué existe: Permite a la interfaz web de la SPA renderizar la pantalla "Mi Perfil" con los datos del usuario logueado.
    // Qué problema resuelve: Mapea los datos internos del usuario a claves legibles para el frontend de forma segura.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            // Qué hace: Recupera la sesión HTTP actual.
            // Por qué existe: Verifica la autenticación antes de retornar información del perfil.
            // Qué problema resuelve: Deniega el acceso 401 si un cliente no autenticado intenta consultar el perfil.
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("user_id") == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print(new JSONObject().put("success", false).put("message", "No autenticado").toString());
                return;
            }

            int userId = (int) session.getAttribute("user_id");
            
            // Qué hace: Llama al servicio de autenticación para obtener el JSON del perfil.
            // y luego de esto pasamos a AuthServicio.obtenerPerfilDetalladoMapeado, el cual lee de base de datos.
            JSONObject dataPerfil = authServicio.obtenerPerfilDetalladoMapeado(userId);

            JSONObject respuesta = new JSONObject().put("success", true).put("data", dataPerfil);
            response.setStatus(HttpServletResponse.SC_OK);
            out.print(respuesta.toString());

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(new JSONObject().put("success", false).put("message", e.getMessage()).toString());
        }
    }

    // Qué hace: Procesa peticiones PUT para modificar el teléfono, correo o contraseña de la cuenta del usuario.
    // Por qué existe: Permite realizar actualizaciones parciales y seguras del perfil validando la contraseña actual.
    // Qué problema resuelve: Evita cambios de correo o teléfono no autorizados mediante la validación previa de la contraseña del usuario.
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

            // Qué hace: Bifurca el flujo según el endpoint exacto invocado.
            // Por qué existe: Permite reutilizar el método PUT del servlet para actualizar diferentes aspectos del perfil.
            // Qué problema resuelve: Centraliza la actualización de campos de perfil individuales.
            switch (path) {
                case "/api/users/profile/phone":
                    // Qué hace: Modifica el celular del usuario validando la clave de seguridad.
                    // y luego de esto pasamos a AuthServicio.modificarTelefonoPerfil.
                    authServicio.modificarTelefonoPerfil(userId, body.getString("phone"), body.getString("password"));
                    respuesta.put("message", "Número de teléfono actualizado con éxito.");
                    break;

                case "/api/users/profile/email":
                    // Qué hace: Modifica el correo electrónico de la cuenta del usuario.
                    // y luego de esto pasamos a AuthServicio.modificarEmailPerfil.
                    authServicio.modificarEmailPerfil(userId, body.getString("email"), body.getString("password"));
                    respuesta.put("message", "Correo electrónico actualizado con éxito.");
                    break;

                case "/api/users/profile/password":
                    // Qué hace: Encripta y actualiza la contraseña en base de datos tras verificar la clave anterior.
                    // y luego de esto pasamos a AuthServicio.modificarContrasenaPerfil.
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