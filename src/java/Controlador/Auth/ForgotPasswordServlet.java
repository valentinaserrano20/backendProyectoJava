package Controlador.Auth;

/*
 * Qué hace (la acción): Importa la capa de servicios de autenticación, utilidades JSON, servlets de Jakarta y la clase JSONObject de la librería JSON.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.Servicios.Auth.AuthServicio: Servicio de negocio que procesa y valida lógica de usuarios.
 *   - Modelo.Utilidades.JSONUtil: Utilidad personalizada para la lectura de peticiones JSON.
 *   - jakarta.servlet.*: Clases del motor de ejecución web (Servlet).
 *   - org.json.JSONObject: Librería externa para modelar información estructurada en pares clave-valor (JSON).
 * Para qué se usa (el propósito): Proveer al servlet de las APIs y dependencias indispensables para procesar las solicitudes web de recuperación de contraseñas.
 * Por qué es importante (el impacto o problema que resuelve): Sin estas importaciones, el código lanzaría errores de compilación y no podría interactuar con la base de datos ni con las solicitudes web del frontend.
 */
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

/*
 * Qué hace (la acción): Registra e inicializa el servlet ForgotPasswordServlet en el contenedor de aplicaciones para responder a múltiples endpoints relacionados con el olvido de contraseña.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - @WebServlet(urlPatterns = {...}): Anotación que mapea el servlet a tres rutas específicas de la API.
 *   - extends HttpServlet: Indica que esta clase hereda los comportamientos de un servlet HTTP, pudiendo responder a llamadas doPost, doGet, etc.
 * Para qué se usa (el propósito): Servir como el único controlador web centralizado para las operaciones de olvido de clave, verificación del código y cambio de la contraseña.
 * Por qué es importante (el impacto o problema que resuelve): Centraliza en una sola clase todo el flujo lógico de seguridad de restablecimiento, evitando crear múltiples archivos servlets y facilitando el mantenimiento.
 */
@WebServlet(urlPatterns = {"/api/forgotPassword", "/api/verifyCode", "/api/changePassword"})
public class ForgotPasswordServlet extends HttpServlet {

    /*
     * Qué hace (la acción): Declara e inicializa de manera privada y constante la variable authServicio.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - private: Restringe el acceso a este miembro solo dentro de esta clase.
     *   - final: Evita la reasignación de esta variable a otro objeto después de su inicialización.
     *   - new AuthServicio(): Instancia la clase de servicios de negocio de autenticación.
     * Para qué se usa (el propósito): Invocar las operaciones lógicas de negocio como generación de códigos de seguridad, envío de correos y actualización de registros en la base de datos.
     * Por qué es importante (el impacto o problema que resuelve): Separa la capa de presentación (el servlet) de la capa de negocio (el servicio), manteniendo un diseño de software limpio y estructurado.
     */
    private final AuthServicio authServicio = new AuthServicio();

    /*
     * Qué hace (la acción): Sobrescribe el método doPost para interceptar y gestionar todas las peticiones POST enviadas a las rutas mapeadas del servlet.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - @Override: Anotación que indica que se está redefiniendo el método de la clase padre HttpServlet.
     *   - HttpServletRequest request: Contiene la solicitud de red proveniente del cliente.
     *   - HttpServletResponse response: Permite configurar y enviar la respuesta al cliente.
     *   - ServletException, IOException: Excepciones obligatorias que puede lanzar el motor en caso de fallos del servlet o de entrada/salida.
     * Para qué se usa (el propósito): Atender de forma segura el envío de datos delicados (como correos, tokens y nuevas contraseñas) dentro del cuerpo de la solicitud HTTP.
     * Por qué es importante (el impacto o problema que resuelve): Si no se implementara, cualquier petición POST a estas rutas fallaría con un error HTTP 405 (Method Not Allowed).
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        /*
         * Qué hace (la acción): Establece que el tipo de datos de retorno al cliente será JSON codificado en formato UTF-8.
         * Qué significa (conceptos, métodos, tipos involucrados):
         *   - setContentType("application/json"): Modifica la cabecera Content-Type de la respuesta HTTP.
         *   - setCharacterEncoding("UTF-8"): Fuerza la codificación UTF-8 para admitir caracteres como acentos y la letra ñ.
         * Para qué se usa (el propósito): Asegurar que el navegador o cliente (fetch, axios) interprete el mensaje como un objeto JSON estructurado sin corromper los caracteres en español.
         * Por qué es importante (el impacto o problema que resuelve): Si no estuviera, el frontend recibiría texto plano y no podría mapear las respuestas de éxito o error automáticamente.
         */
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        /*
         * Qué hace (la acción): Obtiene el escritor de caracteres PrintWriter conectado al flujo de respuesta del cliente.
         * Qué significa (conceptos, métodos, tipos involucrados):
         *   - response.getWriter(): Método que retorna un flujo de salida de texto hacia el cliente HTTP.
         * Para qué se usa (el propósito): Escribir y enviar el texto JSON final que representará la respuesta del servidor.
         * Por qué es importante (el impacto o problema que resuelve): Sin este canal no habría forma física de enviar texto al navegador del usuario, dejando la petición colgada.
         */
        PrintWriter out = response.getWriter();
        
        /*
         * Qué hace (la acción): Obtiene la subruta exacta del servlet que el cliente ha invocado.
         * Qué significa (conceptos, métodos, tipos involucrados):
         *   - request.getServletPath(): Método de HttpServletRequest que devuelve el endpoint específico que ejecutó la solicitud (ej: "/api/verifyCode").
         * Para qué se usa (el propósito): Identificar qué acción en particular (solicitar código, validar código o cambiar clave) ha solicitado el usuario para enrutar el flujo en el switch.
         * Por qué es importante (el impacto o problema que resuelve): Permite bifurcar el flujo lógico dentro de un mismo servlet de manera dinámica.
         */
        String path = request.getServletPath();

        try {
            /*
             * Qué hace (la acción): Parsea el stream de datos de entrada de la solicitud HTTP convirtiéndolo en un objeto de tipo JSONObject.
             * Qué significa (conceptos, métodos, tipos involucrados):
             *   - JSONUtil.leerJson(request): Método auxiliar que lee el flujo de datos (InputStream) del cuerpo de la petición y lo parsea a un objeto JSONObject estructurado.
             * Para qué se usa (el propósito): Recuperar de forma sencilla los parámetros enviados por el frontend (como email, token o contraseñas).
             * Por qué es importante (el impacto o problema que resuelve): Evita tener que leer y parsear manualmente el stream de caracteres de la petición en cada endpoint, previniendo errores de sintaxis y reduciendo código duplicado.
             */
            JSONObject body = JSONUtil.leerJson(request);
            
            /*
             * Qué hace (la acción): Instancia un nuevo objeto JSONObject para armar la respuesta que se devolverá al cliente.
             * Qué significa (conceptos, métodos, tipos involucrados):
             *   - new JSONObject(): Creación de un objeto de mapeo clave-valor para formato JSON.
             * Para qué se usa (el propósito): Almacenar datos lógicos como el estado de la operación (success) y los mensajes descriptivos.
             * Por qué es importante (el impacto o problema que resuelve): Provee una interfaz limpia y estructurada para generar la respuesta final de la API sin concatenar texto manualmente.
             */
            JSONObject respuestaJson = new JSONObject();

            /*
             * Qué hace (la acción): Ejecuta un bloque de decisiones tipo switch basado en la ruta (path) del endpoint de la petición.
             * Qué significa (conceptos, métodos, tipos involucrados): Estructura de control condicional sobre cadenas de texto.
             * Para qué se usa (el propósito): Separar los flujos lógicos de "/api/forgotPassword", "/api/verifyCode" y "/api/changePassword".
             * Por qué es importante (el impacto o problema que resuelve): Permite que una sola clase maneje múltiples operaciones secuenciales de seguridad, reduciendo la cantidad de servlets en el proyecto.
             */
            switch (path) {
                case "/api/forgotPassword":
                    /*
                     * Qué hace (la acción): Obtiene la cadena 'email' del cuerpo de la petición, llama al servicio para generar el código y responde un estado exitoso.
                     * Qué significa (conceptos, métodos, tipos involucrados):
                     *   - body.getString("email"): Obtiene el valor asociado a la clave email en el JSON del request.
                     *   - authServicio.procesarSolicitudRecuperacion(email): Lógica de negocio que busca al usuario en la BD, genera un código temporal y lo envía al correo.
                     * Para qué se usa (el propósito): Iniciar el flujo de recuperación de la cuenta del usuario.
                     * Por qué es importante (el impacto o problema que resuelve): Si el correo existe, se le envía el código al usuario. Si falla, el catch capturará el error.
                     */
                    String email = body.getString("email");
                    authServicio.procesarSolicitudRecuperacion(email);
                    
                    respuestaJson.put("success", true);
                    respuestaJson.put("message", "Código de recuperación enviado. Por favor revise su bandeja de correo.");
                    response.setStatus(HttpServletResponse.SC_OK); 
                    break;

                case "/api/verifyCode":
                    /*
                     * Qué hace (la acción): Obtiene el código ingresado ('token') y lo valida a través del servicio, respondiendo 200 OK si es válido o 400 Bad Request si es incorrecto.
                     * Qué significa (conceptos, métodos, tipos involucrados):
                     *   - body.getString("token"): Extrae el token enviado por el frontend.
                     *   - authServicio.verificarTokenValido(token): Lógica de negocio que busca el código en la base de datos y valida que no haya vencido.
                     * Para qué se usa (el propósito): Confirmar si el código de 6 dígitos que el usuario recibió por correo electrónico es correcto antes de permitirle cambiar la contraseña.
                     * Por qué es importante (el impacto o problema que resuelve): Impide que un atacante intente cambiar la contraseña de otro usuario sin haber demostrado poseer el código enviado a su email.
                     */
                    String token = body.getString("token"); 
                    boolean valido = authServicio.verificarTokenValido(token);
                    
                    if (valido) {
                        respuestaJson.put("success", true);
                        respuestaJson.put("message", "Código verificado con éxito.");
                        response.setStatus(HttpServletResponse.SC_OK); 
                    } else {
                        respuestaJson.put("success", false);
                        respuestaJson.put("message", "El código ingresado es incorrecto o ya expiró.");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST); 
                    }
                    break;

                case "/api/changePassword":
                    /*
                     * Qué hace (la acción): Extrae el código ('token') y la nueva contraseña ('password'), ejecuta la actualización mediante el servicio y retorna éxito.
                     * Qué significa (conceptos, métodos, tipos involucrados):
                     *   - body.getString("password"): Obtiene la clave en texto plano.
                     *   - authServicio.restablecerContrasenaFinal(tokenFinal, nuevaClave): Cifra la clave y la actualiza en la tabla de usuarios de la base de datos.
                     * Para qué se usa (el propósito): Realizar la fase final de asignación de la nueva clave de usuario de forma segura.
                     * Por qué es importante (el impacto o problema que resuelve): Completa el ciclo de recuperación de la cuenta, permitiendo al usuario volver a loguearse tras haber olvidado su clave original.
                     */
                    String tokenFinal = body.getString("token");
                    String nuevaClave = body.getString("password");
                    authServicio.restablecerContrasenaFinal(tokenFinal, nuevaClave);
                    
                    respuestaJson.put("success", true);
                    respuestaJson.put("message", "Su contraseña ha sido actualizada con éxito. Ya puede iniciar sesión.");
                    response.setStatus(HttpServletResponse.SC_OK); 
                    break;

                default:
                    /*
                     * Qué hace (la acción): Controla accesos no definidos dentro del switch de rutas asignando un estado 404 (Not Found).
                     * Qué significa (conceptos, métodos, tipos involucrados): SC_NOT_FOUND representa el código de estado HTTP 404.
                     * Para qué se usa (el propósito): Evitar que peticiones dirigidas a endpoints erróneos devuelvan respuestas vacías o éxitos accidentales.
                     * Por qué es importante (el impacto o problema que resuelve): Mantiene la consistencia semántica del API REST frente a rutas inválidas.
                     */
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    respuestaJson.put("success", false).put("message", "Ruta de recuperación inexistente.");
                    break;
            }

            /*
             * Qué hace (la acción): Convierte el JSONObject de respuesta en una cadena de caracteres y la imprime en la salida hacia el cliente.
             * Qué significa (conceptos, métodos, tipos involucrados): respuestaJson.toString() convierte el mapeo del JSON en texto plano JSON estándar.
             * Para qué se usa (el propósito): Enviar los resultados finales del procesamiento hacia el navegador del usuario.
             * Por qué es importante (el impacto o problema que resuelve): Sin esta impresión final, el frontend jamás se enteraría del resultado de la petición, quedando a la espera de forma indefinida.
             */
            out.print(respuestaJson.toString());

        } catch (Exception e) {
            /*
             * Qué hace (la acción): Captura cualquier error o fallo imprevisto, configura un código HTTP 400 (Bad Request) y escribe un JSON que describe el mensaje del error.
             * Qué significa (conceptos, métodos, tipos involucrados):
             *   - e.getMessage(): Obtiene el texto descriptivo de la excepción ocurrida.
             *   - SC_BAD_REQUEST: Código HTTP 400.
             * Para qué se usa (el propósito): Evitar la caída del servidor y proveer al cliente de un mensaje explicativo y legible en formato JSON.
             * Por qué es importante (el impacto o problema que resuelve): Previene que el servidor exponga información confidencial de la pila de llamadas (StackTrace) de Java en formato HTML y mantiene la consistencia del API en formato JSON frente a errores.
             */
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(new JSONObject()
                    .put("success", false)
                    .put("message", e.getMessage())
                    .toString());
        }
    }
}