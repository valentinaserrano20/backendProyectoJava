package Controlador.Auth;

/*
 * Qué hace (la acción): Importa las clases necesarias para el flujo de entrada y salida, la API de servlets (petición, respuesta y sesión) y el formateador de JSON.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - java.io.PrintWriter: Escritor de texto para enviar información de respuesta.
 *   - jakarta.servlet.http.HttpSession: Clase de servidor que administra el almacenamiento en caché del estado del usuario.
 *   - org.json.JSONObject: Biblioteca externa para estructurar la respuesta JSON.
 * Para qué se usa (el propósito): Proveer las herramientas de red necesarias para que el servlet destruya la sesión del cliente e informe el resultado.
 * Por qué es importante (el impacto o problema que resuelve): Sin estas dependencias no se podría invalidar la sesión del servidor ni responder adecuadamente en formato JSON.
 */
import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.json.JSONObject;

/*
 * Qué hace (la acción): Asocia el servlet LogoutServlet a los patrones de URLs "/api/logout" y "/LogoutServlet" mediante la anotación @WebServlet.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - @WebServlet: Anotación para declarar y registrar servlets de forma sencilla ante el contenedor Tomcat sin usar archivos XML.
 *   - extends HttpServlet: Convierte a la clase en un servlet web capaz de responder a peticiones HTTP.
 * Para qué se usa (el propósito): Servir como el endpoint oficial del sistema para que los usuarios cierren sesión de forma segura.
 * Por qué es importante (el impacto o problema que resuelve): Permite que la aplicación SPA de frontend notifique al servidor cuando un usuario ha salido de su cuenta, protegiendo su perfil de accesos no autorizados.
 */
@WebServlet(name = "LogoutServlet", urlPatterns = {"/api/logout", "/LogoutServlet"})
public class LogoutServlet extends HttpServlet {

    /*
     * Qué hace (la acción): Define un método común llamado processRequest para invalidar la sesión y retornar la respuesta JSON al cliente.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - processRequest: Método auxiliar personalizado que centraliza el procesamiento común sin importar el método HTTP (GET o POST) usado.
     *   - HttpServletRequest request: Solicitud de red del cliente.
     *   - HttpServletResponse response: Respuesta de red que devolveremos al cliente.
     *   - ServletException, IOException: Excepciones estándares lanzadas ante fallos de servidor o de entrada/salida.
     * Para qué se usa (el propósito): Centralizar las operaciones de destrucción de sesión para evitar duplicidad de código.
     * Por qué es importante (el impacto o problema que resuelve): Garantiza que sin importar si el logout se llama mediante un enlace simple (GET) o un envío asíncrono (POST), el comportamiento sea idéntico y seguro.
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        /*
         * Qué hace (la acción): Configura las cabeceras HTTP de respuesta estableciendo el tipo de contenido como JSON con codificación UTF-8.
         * Qué significa (conceptos, métodos, tipos involucrados):
         *   - setContentType("application/json"): Indica que retornaremos un objeto JSON estructurado.
         *   - setCharacterEncoding("UTF-8"): Fuerza la codificación UTF-8 para evitar problemas de caracteres.
         * Para qué se usa (el propósito): Informar al frontend que la respuesta debe ser parseada como un JSON y visualizar correctamente caracteres con acento.
         * Por qué es importante (el impacto o problema que resuelve): Evita que el navegador reciba la respuesta de cierre como texto plano sin formato, previniendo fallos en la interpretación del cliente web.
         */
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        /*
         * Qué hace (la acción): Obtiene e inicializa el canal PrintWriter de salida usando un bloque try-with-resources.
         * Qué significa (conceptos, métodos, tipos involucrados):
         *   - try (PrintWriter out = ...): Bloque try-with-resources que cierra automáticamente el objeto de escritura al finalizar el bloque para liberar memoria.
         * Para qué se usa (el propósito): Escribir de forma segura el texto de respuesta hacia el cliente.
         * Por qué es importante (el impacto o problema que resuelve): Previene fugas de recursos de red (memory leaks) si se nos olvidara cerrar manualmente el canal de comunicación.
         */
        try (PrintWriter out = response.getWriter()) {
            
            try {
                /*
                 * Qué hace (la acción): Busca la sesión HTTP activa del cliente mediante getSession(false).
                 * Qué significa (conceptos, métodos, tipos involucrados):
                 *   - getSession(false): Obtiene la sesión en curso. Si no existe, retorna null en lugar de crear una nueva.
                 * Para qué se usa (el propósito): Verificar si de verdad existe una sesión activa vinculada a la cookie del usuario para proceder a destruirla.
                 * Por qué es importante (el impacto o problema que resuelve): Evita crear una sesión fantasma inútil en la memoria del servidor si el cliente llama a logout sin tener una sesión activa.
                 */
                HttpSession session = request.getSession(false);
                
                /*
                 * Qué hace (la acción): Si el objeto session existe y no es nulo, invalida por completo el objeto eliminando sus atributos asociados en el servidor.
                 * Qué significa (conceptos, métodos, tipos involucrados):
                 *   - session.invalidate(): Método de la API de servlets que destruye la sesión del usuario del lado del servidor.
                 * Para qué se usa (el propósito): Revocar los permisos de acceso y el identificador de sesión del usuario.
                 * Por qué es importante (el impacto o problema que resuelve): Es fundamental para la seguridad del usuario. Si no se hiciera, la sesión seguiría activa en el servidor hasta que caducara por tiempo, permitiendo que cualquiera con la cookie secuestrara la cuenta del usuario.
                 */
                if (session != null) {
                    session.invalidate();
                }
                
                /*
                 * Qué hace (la acción): Crea un JSONObject de éxito, configura el estado HTTP a 200 (OK) e imprime el resultado al flujo de salida.
                 * Qué significa (conceptos, métodos, tipos involucrados):
                 *   - SC_OK: Código de estado HTTP 200 que indica una ejecución exitosa.
                 *   - json.put(): Añade claves al objeto JSON.
                 * Para qué se usa (el propósito): Confirmar al cliente que su sesión fue borrada con éxito de la memoria del backend.
                 * Por qué es importante (el impacto o problema que resuelve): Es la confirmación que le permite al frontend borrar los datos de su almacenamiento local y redirigir al usuario al login.
                 */
                JSONObject json = new JSONObject();
                json.put("success", true);
                json.put("message", "Sesión cerrada exitosamente.");
                
                response.setStatus(HttpServletResponse.SC_OK);
                out.print(json.toString());
                
            } catch (Exception e) {
                /*
                 * Qué hace (la acción): Captura cualquier error ocurrido durante el cierre de sesión, establece el código HTTP 500 (Server Error) y escribe la descripción del error.
                 * Qué significa (conceptos, métodos, tipos involucrados): SC_INTERNAL_SERVER_ERROR representa el código HTTP 500.
                 * Para qué se usa (el propósito): Controlar fallos internos inesperados sin romper la ejecución del servidor.
                 * Por qué es importante (el impacto o problema que resuelve): Previene que el servidor retorne un StackTrace de error técnico en formato HTML y en su lugar envía una respuesta JSON limpia al frontend.
                 */
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                JSONObject error = new JSONObject();
                error.put("success", false);
                error.put("message", e.getMessage());
                out.print(error.toString());
            }
        }
    }

    /*
     * Qué hace (la acción): Redirecciona las solicitudes GET hacia el método processRequest.
     * Qué significa (conceptos, métodos, tipos involucrados): doGet es el método estándar de HttpServlet para recibir llamadas de tipo GET.
     * Para qué se usa (el propósito): Permitir cierres de sesión rápidos iniciados por redirecciones directas o enlaces ordinarios.
     * Por qué es importante (el impacto o problema que resuelve): Proporciona compatibilidad con llamadas tradicionales GET.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /*
     * Qué hace (la acción): Redirecciona las solicitudes POST hacia el método processRequest.
     * Qué significa (conceptos, métodos, tipos involucrados): doPost es el método estándar de HttpServlet para recibir llamadas de tipo POST.
     * Para qué se usa (el propósito): Permitir cierres de sesión seguros iniciados por llamadas AJAX asíncronas desde botones de la SPA.
     * Por qué es importante (el impacto o problema que resuelve): Proporciona compatibilidad con llamadas POST seguras.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /*
     * Qué hace (la acción): Devuelve un texto descriptivo corto sobre el servlet.
     * Qué significa (conceptos, métodos, tipos involucrados): getServletInfo es un método estándar de la API Servlet.
     * Para qué se usa (el propósito): Proveer información de identificación para herramientas de depuración o de administración del servidor.
     * Por qué es importante (el impacto o problema que resuelve): Es útil para la autodescripción del servlet.
     */
    @Override
    public String getServletInfo() {
        return "Servlet de cierre de sesión";
    }
}
