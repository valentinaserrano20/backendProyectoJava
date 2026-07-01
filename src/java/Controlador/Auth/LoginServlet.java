package Controlador.Auth;

/*
 * Qué hace (la acción): Importa la entidad Usuario, el DAO de base de datos de usuarios, el servicio de autenticación y las APIs de servlets y JSON.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.Entidades.Usuario: Entidad que modela un usuario en el sistema.
 *   - Modelo.Servicios.Auth.AuthServicio: Servicio de negocio que maneja la autenticación y encriptación.
 *   - Modelo.DAO.UsuarioDAO: Acceso directo a base de datos de usuarios.
 *   - jakarta.servlet.http.HttpSession: Clase de servidor para recordar el estado del cliente mediante sesiones.
 *   - Modelo.Utilidades.JSONUtil: Utilidad para parsear el cuerpo JSON de la petición HTTP.
 * Para qué se usa (el propósito): Proveer las dependencias necesarias para procesar el inicio de sesión y la autorización en el sistema.
 * Por qué es importante (el impacto o problema que resuelve): Sin estas importaciones, el compilador daría errores de sintaxis y no sabríamos cómo procesar los datos recibidos ni guardarlos en sesión.
 */
import Modelo.Entidades.Usuario;
import Modelo.Servicios.Auth.AuthServicio;
import Modelo.DAO.UsuarioDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import Modelo.Utilidades.JSONUtil;
import java.io.IOException;
import java.io.PrintWriter;
import org.json.JSONObject;

/*
 * Qué hace (la acción): Registra e inicializa el servlet LoginServlet mapeándolo al endpoint "/api/login".
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - @WebServlet("/api/login"): Registra el servlet ante Tomcat para que responda cuando alguien llame a esa URL.
 *   - extends HttpServlet: Permite heredar los métodos de ejecución web estándar (doPost, doGet).
 * Para qué se usa (el propósito): Servir como el endpoint de autenticación principal de la aplicación web.
 * Por qué es importante (el impacto o problema que resuelve): Permite interceptar las credenciales (email y contraseña) de los usuarios que intenten loguearse en el sistema.
 */
@WebServlet("/api/login")
public class LoginServlet extends HttpServlet {

    /*
     * Qué hace (la acción): Sobrescribe doPost para capturar y procesar las llamadas de autenticación asíncronas de tipo POST.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - doPost: Método de HttpServlet diseñado para recibir parámetros de forma oculta y segura dentro del cuerpo de la petición.
     *   - HttpServletRequest, HttpServletResponse: Objetos para leer los datos del cliente y escribir la respuesta de red.
     * Para qué se usa (el propósito): Controlar el inicio de sesión, verificar las claves encriptadas y crear la sesión en el servidor.
     * Por qué es importante (el impacto o problema que resuelve): Si no se sobrescribe, cualquier solicitud POST a esta ruta devolverá un código HTTP 405 (Método no permitido).
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        /*
         * Qué hace (la acción): Configura la cabecera de la respuesta como JSON codificado en UTF-8.
         * Qué significa (conceptos, métodos, tipos involucrados):
         *   - setContentType("application/json"): Indica que el tipo de datos devueltos es JSON estructurado.
         *   - setCharacterEncoding("UTF-8"): Establece que la codificación de caracteres es UTF-8.
         * Para qué se usa (el propósito): Notificar al cliente web cómo debe interpretar el texto devuelto y garantizar que caracteres especiales (como acentos o la letra ñ) se rendericen correctamente.
         * Por qué es importante (el impacto o problema que resuelve): Si se omitiera, el navegador podría ver la respuesta como texto sin formato, bloqueando el mapeo en el frontend.
         */
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        /*
         * Qué hace (la acción): Obtiene el flujo de salida PrintWriter para enviar el texto de respuesta al cliente.
         * Qué significa (conceptos, métodos, tipos involucrados): response.getWriter() obtiene el stream de escritura web.
         * Para qué se usa (el propósito): Poder mandar respuestas en formato de texto directamente al navegador cliente.
         * Por qué es importante (el impacto o problema que resuelve): Sin este stream no habría forma física de retornar datos al cliente, dejando su solicitud sin respuesta.
         */
        PrintWriter out = response.getWriter();

        try {
            /*
             * Qué hace (la acción): Parsea el stream del request HTTP convirtiéndolo en un objeto de tipo JSONObject.
             * Qué significa (conceptos, métodos, tipos involucrados): JSONUtil.leerJson(request) lee el flujo de entrada de la petición y lo mapea a un JSONObject.
             * Para qué se usa (el propósito): Recuperar los campos estructurados "email" y "password" enviados desde el formulario del frontend.
             * Por qué es importante (el impacto o problema que resuelve): Evita leer y parsear manualmente bytes de red, reduciendo el código y previniendo fallos de parseo.
             */
            JSONObject body = JSONUtil.leerJson(request);

            /*
             * Qué hace (la acción): Extrae las cadenas correspondientes a las llaves "email" y "password" de los datos de la petición.
             * Qué significa (conceptos, métodos, tipos involucrados): body.getString(clave) extrae la cadena asociada a esa clave en el JSON.
             * Para qué se usa (el propósito): Obtener las credenciales en texto del usuario para procesar su autenticación.
             * Por qué es importante (el impacto o problema que resuelve): Son las variables clave indispensables para buscar al usuario y contrastar su contraseña.
             */
            String email = body.getString("email");
            String password = body.getString("password");

            /*
             * Qué hace (la acción): Crea una instancia de AuthServicio y ejecuta su método login para validar las credenciales contra la base de datos.
             * Qué significa (conceptos, métodos, tipos involucrados):
             *   - new AuthServicio(): Instancia la clase de servicios de autenticación.
             *   - authServicio.login(email, password): Método que busca el correo electrónico del usuario y comprueba la contraseña usando la encriptación BCrypt.
             * Para qué se usa (el propósito): Resolver la autenticación delegando la lógica de seguridad a la capa de negocio.
             * Por qué es importante (el impacto o problema que resuelve): Evita mezclar consultas directas a base de datos y validación de hash dentro del servlet, respetando la arquitectura de capas del proyecto.
             */
            AuthServicio authServicio = new AuthServicio();
            Usuario usuario = authServicio.login(email, password);

            /*
             * Qué hace (la acción): Valida si el objeto usuario retornado es nulo. Si es así, responde con estado 401 Unauthorized, genera un JSON de error y detiene la petición.
             * Qué significa (conceptos, métodos, tipos involucrados):
             *   - SC_UNAUTHORIZED: Constante de HttpServletResponse equivalente al error HTTP 401.
             *   - return: Corta la ejecución del servlet inmediatamente.
             * Para qué se usa (el propósito): Proteger la sesión e impedir que usuarios con credenciales inválidas se logueen en la plataforma.
             * Por qué es importante (el impacto o problema que resuelve): Si no se detuviera la ejecución aquí, el flujo procedería de manera errónea a crear una sesión para un usuario inexistente o no autorizado.
             */
            if (usuario == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); 
                JSONObject errorJson = new JSONObject();
                errorJson.put("success", false);
                errorJson.put("message", "Usuario no encontrado en el sistema.");
                out.print(errorJson);
                return; 
            }

            /*
             * Qué hace (la acción): Obtiene o crea la sesión HTTP para este usuario y le asigna el tiempo máximo de inactividad de 30 minutos (1800 segundos).
             * Qué significa (conceptos, métodos, tipos involucrados):
             *   - request.getSession(true): Obtiene la sesión actual o crea una nueva si no existe.
             *   - setMaxInactiveInterval(1800): Configura la expiración de la sesión por inactividad.
             * Para qué se usa (el propósito): Guardar el estado de autenticación de forma segura del lado del servidor para futuras peticiones del cliente.
             * Por qué es importante (el impacto o problema que resuelve): Mantiene al usuario logueado en la aplicación de manera segura y automática sin que tenga que loguearse con cada click. El tiempo de expiración previene que la sesión quede abierta indefinidamente si el usuario abandona la pestaña.
             */
            HttpSession session = request.getSession(true);
            session.setAttribute("user_id", usuario.getId());
            session.setAttribute("usuarioId", usuario.getId()); 
            session.setMaxInactiveInterval(1800); 

            /*
             * Qué hace (la acción): Crea un JSONObject de respuesta, consulta los permisos del rol mediante el DAO de usuarios y compila una cadena de texto de permisos.
             * Qué significa (conceptos, métodos, tipos involucrados):
             *   - new UsuarioDAO(): Instancia del DAO de base de datos de usuarios.
             *   - usuarioDAO.obtenerPermisosPorRol(rolId): Método de base de datos que trae la lista de permisos asignados a ese rol (ej: "home-frontend.voluntario").
             * Para qué se usa (el propósito): Retornar al cliente web la información necesaria (id, nombre, rol, organización, permisos) para inicializar el perfil en el frontend de Vue.
             * Por qué es importante (el impacto o problema que resuelve): Permite que el frontend (Vue/Vite) sepa dinámicamente qué páginas y funcionalidades habilitar para el usuario (si es Voluntario o Supervisor) de acuerdo a sus roles.
             */
            JSONObject data = new JSONObject();
            data.put("id", usuario.getId());
            data.put("full_name", usuario.getNombre() + " " + usuario.getApellido());

            int mappedRoleId = usuario.getRolId();
            UsuarioDAO usuarioDAO = new UsuarioDAO();
            java.util.List<String> dbPerms = usuarioDAO.obtenerPermisosPorRol(mappedRoleId);
            StringBuilder permissionsBuilder = new StringBuilder();

            if (mappedRoleId == 1) {
                permissionsBuilder.append("home-frontend.voluntario");
            } else if (mappedRoleId == 2) {
                permissionsBuilder.append("home-frontend.supervisor,home-frontend.administrador");
            } else {
                permissionsBuilder.append("home-frontend.desconocido");
            }

            for (String perm : dbPerms) {
                permissionsBuilder.append(",").append(perm);
            }

            String permissions = permissionsBuilder.toString();
            data.put("role_id", mappedRoleId);
            data.put("permissions", permissions);
            data.put("sectional_id", usuario.getOrganizacionId() != null ? usuario.getOrganizacionId() : 1);
            data.put("gender", usuario.getGeneroId());

            JSONObject respuesta = new JSONObject();
            respuesta.put("success", true);
            respuesta.put("message", "Bienvenido, " + usuario.getNombre() + "!");
            respuesta.put("data", data);

            /*
             * Qué hace (la acción): Imprime la respuesta JSON construida en el stream PrintWriter hacia la red.
             * Qué significa (conceptos, métodos, tipos involucrados): out.print(respuesta) envía el JSON de respuesta serializado en texto al cliente HTTP.
             * Para qué se usa (el propósito): Finalizar exitosamente la llamada de inicio de sesión comunicando al frontend los datos del perfil y sus permisos.
             * Por qué es importante (el impacto o problema que resuelve): Es el paso de confirmación que le permite a la interfaz de usuario dar la bienvenida y redirigir al panel principal.
             */
            out.print(respuesta);

        } catch (Exception e) {
            /*
             * Qué hace (la acción): Captura cualquier error ocurrido durante el flujo de login, configura un estado HTTP de error apropiado (401, 403 o 500) y responde un JSON explicativo.
             * Qué significa (conceptos, métodos, tipos involucrados):
             *   - SC_UNAUTHORIZED: Error HTTP 401.
             *   - SC_FORBIDDEN: Error HTTP 403 (ej: cuenta no activa).
             *   - SC_INTERNAL_SERVER_ERROR: Error HTTP 500 para fallos del servidor.
             * Para qué se usa (el propósito): Controlar de forma limpia las excepciones y notificar al frontend exactamente qué falló.
             * Por qué es importante (el impacto o problema que resuelve): Previene fugas de información interna al ocultar las trazas del servidor Java y proporciona al usuario final una explicación clara de por qué falló su inicio de sesión (por ejemplo, si su cuenta voluntaria no ha sido aprobada por un supervisor).
             */
            String msg = e.getMessage();

            if ("El correo electrónico no se encuentra registrado.".equals(msg) || 
                "La contraseña ingresada es incorrecta.".equals(msg)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); 
            } else if ("Tu cuenta aún no está activa".equals(msg)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN); 
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); 
            }

            JSONObject error = new JSONObject();
            error.put("success", false);
            error.put("message", msg);
            out.print(error);
        }
    }
}
