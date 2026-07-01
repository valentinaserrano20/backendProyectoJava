package Controlador.Public;

/*
 * Qué hace (la acción): Importa la capa de servicios de autenticación y las utilidades para manipular peticiones JSON, sesiones y respuestas web uniformes.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.Servicios.Auth.AuthServicio: Servicio que contiene la lógica de negocio para obtener y modificar perfiles.
 *   - Modelo.Utilidades.SessionUtil: Clase de utilidad para validar la identidad del usuario a través de los datos de su sesión HTTP.
 *   - Modelo.Utilidades.ResponseUtil: Genera la envoltura JSON estándar para las respuestas de la API.
 * Para qué se usa (el propósito): Proveer al servlet de las herramientas lógicas necesarias para leer y actualizar los datos confidenciales de la cuenta del usuario.
 * Por qué es importante (el impacto o problema que resuelve): Sin estas importaciones, el servlet no tendría acceso a las clases de sesión ni a las operaciones criptográficas del servicio de autenticación.
 */
import Modelo.Servicios.Auth.AuthServicio;
import Modelo.Utilidades.JSONUtil;
import Modelo.Utilidades.ResponseUtil;
import Modelo.Utilidades.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import org.json.JSONObject;

/*
 * Qué hace (la acción): Asocia el servlet UserPerfilServlet con los endpoints de gestión de perfil de usuario mediante la anotación @WebServlet.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - urlPatterns = {...}: Registra las cuatro rutas del perfil (ver perfil, cambiar teléfono, cambiar correo, cambiar contraseña) en Tomcat.
 * Para qué se usa (el propósito): Servir como el controlador exclusivo del subsistema de configuración de la cuenta personal de cada usuario.
 * Por qué es importante (el impacto o problema que resuelve): Permite que cualquier usuario (voluntario o supervisor) modifique sus datos de contacto o contraseña de forma segura y centralizada.
 */
@WebServlet(urlPatterns = {"/api/users/profile", "/api/users/profile/phone", "/api/users/profile/email", "/api/users/profile/password"})
public class UserPerfilServlet extends HttpServlet {

    /*
     * Qué hace (la acción): Instancia de manera privada y constante la variable authServicio.
     * Qué significa (conceptos, métodos, tipos involucrados): Instancia de la clase de servicios de autenticación.
     * Para qué se usa (el propósito): Ejecutar los procesos de lógica de negocio relacionados con la cuenta del usuario.
     */
    private final AuthServicio authServicio = new AuthServicio();

    /*
     * Qué hace (la acción): Sobrescribe el método doGet para obtener y retornar la información detallada del perfil del usuario logueado en la sesión HTTP.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - SessionUtil.getUsuarioId(session): Extrae el ID numérico de usuario almacenado en la sesión.
     *   - authServicio.obtenerPerfilDetalladoMapeado(userId): Consulta a la base de datos y retorna un JSONObject con los datos demográficos y de contacto del usuario.
     * Para qué se usa (el propósito): Cargar los datos del perfil del usuario (nombre, apellido, correo, teléfono, etc.) en los campos de edición cuando este ingresa a la configuración de su cuenta.
     * Por qué es importante (el impacto o problema que resuelve): Detiene la petición de inmediato con un error HTTP 401 Unauthorized si la sesión ha expirado o no es válida, protegiendo los datos personales del usuario.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        PrintWriter out = response.getWriter();

        try {
            HttpSession session = request.getSession(false);
            Integer userId = SessionUtil.getUsuarioId(session);
            if (userId == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print(ResponseUtil.error("Acceso denegado. Sesión inválida."));
                return;
            }
            
            JSONObject dataPerfil = authServicio.obtenerPerfilDetalladoMapeado(userId);
            
            response.setStatus(HttpServletResponse.SC_OK);
            out.print(ResponseUtil.success(dataPerfil));

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(ResponseUtil.error(e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doPut para capturar y procesar las peticiones HTTP PUT de actualización de teléfono, correo o contraseña de la cuenta del usuario.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - doPut: Método de HttpServlet diseñado para recibir actualizaciones de recursos.
     *   - request.getServletPath(): Identifica cuál de las tres rutas de modificación fue invocada.
     *   - authServicio.modificarContrasenaPerfil(...): Modifica la clave validando que la contraseña actual ingresada coincida con la registrada en base de datos.
     * Para qué se usa (el propósito): Modificar selectivamente los datos sensibles del perfil tras realizar validaciones previas de seguridad.
     * Por qué es importante (el impacto o problema que resuelve): Protege la integridad de la cuenta. Por ejemplo, al cambiar el teléfono o el correo, exige la contraseña actual para verificar que el dueño legítimo de la cuenta es quien realiza el cambio.
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        PrintWriter out = response.getWriter();
        String path = request.getServletPath();

        try {
            HttpSession session = request.getSession(false);
            Integer userId = SessionUtil.getUsuarioId(session);
            if (userId == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print(ResponseUtil.error("Acceso denegado. Sesión inválida."));
                return;
            }
            JSONObject body = JSONUtil.leerJson(request);
            String message = "";

            switch (path) {
                case "/api/users/profile/phone":
                    authServicio.modificarTelefonoPerfil(userId, body.getString("phone"), body.getString("password"));
                    message = "Número de teléfono actualizado con éxito.";
                    break;

                case "/api/users/profile/email":
                    authServicio.modificarEmailPerfil(userId, body.getString("email"), body.getString("password"));
                    message = "Correo electrónico actualizado con éxito.";
                    break;

                case "/api/users/profile/password":
                    authServicio.modificarContrasenaPerfil(userId, body.getString("password_actual"), body.getString("password_nueva"));
                    message = "Su contraseña ha sido modificada con éxito.";
                    break;

                default:
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(ResponseUtil.error("Acción no soportada"));
                    return;
            }

            response.setStatus(HttpServletResponse.SC_OK);
            out.print(ResponseUtil.success(message));

        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(ResponseUtil.error("JSON mal formado: " + e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(ResponseUtil.error(e.getMessage()));
        }
    }
}