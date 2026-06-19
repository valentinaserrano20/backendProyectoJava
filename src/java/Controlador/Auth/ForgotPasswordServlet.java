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
 * Qué hace: Define e inicializa el Servlet para la recuperación de contraseñas mapeado a múltiples endpoints.
 * Por qué existe: Centraliza el flujo de olvido de contraseña, verificación de token y cambio final en un único controlador.
 * Qué pasaría si no estuviera: Los usuarios que olviden su contraseña no tendrían forma de recuperarla ni restablecerla desde la SPA.
 */
@WebServlet(urlPatterns = {"/api/forgotPassword", "/api/verifyCode", "/api/changePassword"})
public class ForgotPasswordServlet extends HttpServlet {

    // Qué hace: Crea una instancia del servicio de autenticación.
    // Por qué existe: Delega la lógica de negocio (generar tokens, enviar correos, hashing) a la capa correspondiente.
    // Qué pasaría si no estuviera: Tendríamos que escribir lógica de negocio y consultas directas en el controlador, rompiendo la arquitectura limpia.
    // Flujo: De aquí pasaremos a AuthServicio para ejecutar procesos de validación de tokens y actualización.
    private final AuthServicio authServicio = new AuthServicio();

    /**
     * SOPORTE PARA CORS PRE-FLIGHT (MÉTODO OPTIONS)
     * El navegador web envía de forma invisible una petición OPTIONS antes del POST
     * para verificar si el servidor Java acepta llamadas desde el puerto de Vite (5173).
     */
    // doOptions() ya no es necesario aquí porque el CorsFilter (@WebFilter("/*"))
    // intercepta TODAS las rutas, incluyendo las preflight OPTIONS, antes de llegar al servlet.
    // Mantenerlo causaría headers CORS duplicados que el navegador rechaza.

    // Qué hace: Sobrescribe doPost para atender peticiones POST HTTP en las rutas configuradas.
    // Por qué existe: Los datos sensibles (emails, códigos de verificación, contraseñas nuevas) deben transmitirse en el cuerpo de la petición.
    // Qué pasaría si no estuviera: El servidor respondería con error 405 (Method Not Allowed) al intentar hacer POST en estas rutas.
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // CORS ya fue configurado globalmente por CorsFilter antes de llegar aquí.
        // Qué hace: Establece el tipo de contenido como JSON para que el cliente lo reciba correctamente estructurado.
        // Por qué existe: Le informa al cliente (fetch/axios) que la respuesta es un JSON parseable.
        // Qué pasaría si no estuviera: El frontend recibiría texto plano y no podría interpretar la estructura JSON de respuesta.
        response.setContentType("application/json");
        
        // Qué hace: Define la codificación de caracteres a UTF-8.
        // Por qué existe: Asegura que caracteres especiales (como acentos y la ñ) se transmitan sin corromperse en los mensajes.
        // Qué pasaría si no estuviera: Mensajes con acentos (como "Código verificado") llegarían deformados al cliente.
        response.setCharacterEncoding("UTF-8");
        
        // Qué hace: Obtiene el PrintWriter para escribir la respuesta en el cuerpo del mensaje HTTP.
        // Por qué existe: Canal necesario para enviar datos de retorno en formato de texto al frontend.
        // Qué pasaría si no estuviera: No podríamos escribir la respuesta en el canal de salida y el cliente se quedaría esperando.
        PrintWriter out = response.getWriter();
        
        // Qué hace: Obtiene la ruta del endpoint solicitada en la petición actual.
        // Por qué existe: Permite identificar cuál de las tres rutas mapeadas se está intentando consumir para derivar al switch correspondiente.
        // Qué pasaría si no estuviera: No podríamos bifurcar el flujo y procesar solicitudes diferenciadas de olvido, verificación o cambio.
        String path = request.getServletPath();

        try {
            // Qué hace: Parsea el cuerpo del request HTTP en un objeto JSON manipulable.
            // Por qué existe: Facilita la extracción de las variables enviadas por el frontend en formato JSON.
            // Qué pasaría si no estuviera: Tendríamos que leer manualmente el InputStream del request y parsearlo, lo cual es redundante y propenso a errores.
            JSONObject body = JSONUtil.leerJson(request);
            
            // Qué hace: Inicializa un objeto JSON de respuesta.
            // Por qué existe: Servirá para empaquetar los campos "success", "message" y cualquier dato de retorno.
            // Qué pasaría si no estuviera: Tendríamos que concatenar strings JSON de forma manual, lo que es peligroso por la sintaxis JSON.
            JSONObject respuestaJson = new JSONObject();

            // Qué hace: Bifurca el flujo según el endpoint consumido.
            // Por qué existe: Permite reutilizar este servlet para todo el subflujo de recuperación de contraseñas.
            // Qué pasaría si no estuviera: Necesitaríamos tres Servlets diferentes, lo que incrementaría la cantidad de archivos y complejidad.
            switch (path) {
                case "/api/forgotPassword":
                    // Qué hace: Obtiene el correo electrónico enviado en la petición.
                    // Por qué existe: Identifica a qué cuenta de usuario se le desea generar el código de recuperación.
                    // Qué pasaría si no estuviera: No sabríamos de qué usuario buscar el perfil en el sistema.
                    String email = body.getString("email");
                    
                    // Qué hace: Llama a la lógica de negocio para generar el token y enviar el correo.
                    // y luego de esto pasamos a AuthServicio.procesarSolicitudRecuperacion, el cual busca al usuario en la BD, genera un código y envía el email.
                    authServicio.procesarSolicitudRecuperacion(email);
                    
                    // Qué hace: Configura el JSON de respuesta exitosa.
                    // Por qué existe: Indica al frontend que el proceso de inicio de recuperación fue exitoso.
                    // Qué pasaría si no estuviera: El frontend no sabría si debe avanzar al paso de verificación de código o mostrar un error.
                    respuestaJson.put("success", true);
                    respuestaJson.put("message", "Código de recuperación enviado. Por favor revise su bandeja de correo.");
                    
                    // Qué hace: Establece el estado HTTP a 200 (OK).
                    // Por qué existe: Indica una ejecución exitosa de la petición.
                    // Qué pasaría si no estuviera: Por defecto se usaría 200, pero explicitarlo garantiza claridad en la semántica del API REST.
                    response.setStatus(HttpServletResponse.SC_OK); 
                    break;

                case "/api/verifyCode":
                    // Qué hace: Recupera el token/código ingresado por el usuario en el formulario.
                    // Por qué existe: Necesario para contrastar el código de 6 dígitos que el usuario recibió por email.
                    // Qué pasaría si no estuviera: No podríamos validar si el usuario realmente tiene acceso a la cuenta de correo.
                    String token = body.getString("token"); 
                    
                    // Qué hace: Llama a la validación del código contra la base de datos y la fecha de expiración.
                    // y luego de esto pasamos a AuthServicio.verificarTokenValido, que verifica si el token existe en la BD y no ha vencido.
                    boolean valido = authServicio.verificarTokenValido(token);
                    
                    if (valido) {
                        // Qué hace: Almacena éxito y mensaje correspondiente en la respuesta.
                        respuestaJson.put("success", true);
                        respuestaJson.put("message", "Código verificado con éxito.");
                        
                        // Qué hace: Responde con HTTP 200 OK.
                        // Por qué existe: Indica que la credencial temporal de token es válida y puede proceder a cambiar la clave.
                        // Qué pasaría si no estuviera: El frontend podría asumir que el código fue incorrecto si no recibe la confirmación.
                        response.setStatus(HttpServletResponse.SC_OK); 
                    } else {
                        // Qué hace: Almacena fallo y mensaje descriptivo en la respuesta.
                        respuestaJson.put("success", false);
                        respuestaJson.put("message", "El código ingresado es incorrecto o ya expiró.");
                        
                        // Qué hace: Setea HTTP 400 Bad Request.
                        // Por qué existe: Indica al frontend que la petición es inválida debido a credenciales temporales incorrectas.
                        // Qué pasaría si no estuviera: El cliente podría interpretar que todo salió bien a nivel de negocio si respondemos con código 200.
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST); 
                    }
                    break;

                case "/api/changePassword":
                    // Qué hace: Extrae el código de seguridad (token) y la nueva contraseña elegida.
                    // Por qué existe: Parámetros requeridos para hacer el cambio final de la clave de forma segura.
                    // Qué pasaría si no estuviera: No sabríamos de qué solicitud de cambio se trata ni qué clave colocar.
                    String tokenFinal = body.getString("token");
                    String nuevaClave = body.getString("password");
                    
                    // Qué hace: Ejecuta la actualización de contraseña en la base de datos.
                    // y luego de esto pasamos a AuthServicio.restablecerContrasenaFinal, que encripta la nueva clave y actualiza el registro del usuario.
                    authServicio.restablecerContrasenaFinal(tokenFinal, nuevaClave);
                    
                    // Qué hace: Estructura la respuesta exitosa final del flujo.
                    respuestaJson.put("success", true);
                    respuestaJson.put("message", "Su contraseña ha sido actualizada con éxito. Ya puede iniciar sesión.");
                    
                    // Qué hace: Setea HTTP 200 OK.
                    response.setStatus(HttpServletResponse.SC_OK); 
                    break;

                default:
                    // Qué hace: Maneja cualquier acceso a una ruta que no esté explícitamente soportada en el switch.
                    // Por qué existe: Evita comportamientos indefinidos si el mapeo del servlet recibe una ruta no controlada.
                    // Qué pasaría si no estuviera: Se podría retornar un éxito vacío al cliente en lugar de un error semántico apropiado.
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND); // 404 Not Found
                    respuestaJson.put("success", false).put("message", "Ruta de recuperación inexistente.");
                    break;
            }

            // Qué hace: Transmite el JSON construido al flujo de salida del cliente.
            // Por qué existe: Completa el ciclo de solicitud-respuesta enviando los resultados al frontend.
            // Qué pasaría si no estuviera: El cliente (navegador) se quedaría en estado de espera permanente hasta dar timeout.
            out.print(respuestaJson.toString());

        } catch (Exception e) {
            // Qué hace: Captura errores imprevistos o excepciones de lógica de negocio (ej. usuario no encontrado).
            // Por qué existe: Previene la caída del servlet y garantiza que el frontend reciba un mensaje amigable con el error.
            // Qué pasaría si no estuviera: El servidor Java arrojaría un StackTrace HTML por defecto, revelando detalles internos y rompiendo el parseo JSON.
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400 Bad Request
            out.print(new JSONObject()
                    .put("success", false)
                    .put("message", e.getMessage())
                    .toString());
        }
    }

    // ELIMINADO: configurarCabecerasCORS() fue removido porque duplicaba la lógica del CorsFilter.
    // El filtro global en Controlador.Filter.CorsFilter maneja CORS de forma centralizada
    // para toda la aplicación, evitando headers duplicados que el navegador rechaza.
}