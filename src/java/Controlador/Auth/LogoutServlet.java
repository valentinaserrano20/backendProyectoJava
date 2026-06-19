package Controlador.Auth;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.json.JSONObject;

/**
 * Qué hace: Define e inicializa el Servlet para el cierre de sesión de usuarios.
 * Por qué existe: Habilita el punto de entrada (Endpoint) para destruir la sesión del usuario en el servidor.
 * Qué pasaría si no estuviera: El usuario no podría cerrar su sesión de forma segura y sus datos de sesión seguirían activos en la memoria del servidor.
 */
@WebServlet(name = "LogoutServlet", urlPatterns = {"/api/logout", "/LogoutServlet"})
public class LogoutServlet extends HttpServlet {

    /**
     * Qué hace: Procesa las solicitudes HTTP GET y POST para invalidar la sesión y responder en formato JSON.
     * Por qué existe: Centraliza la lógica de cierre de sesión para cualquier método HTTP de red por el que se invoque.
     * Qué pasaría si no estuviera: Habría que duplicar el código de invalidación tanto en doGet como en doPost.
     * 
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // Qué hace: Establece el tipo de contenido como JSON para que el cliente lo reciba correctamente estructurado.
        // Por qué existe: Asegura que el cliente (fetch/axios) reciba un JSON parseable e interpretable.
        // Qué pasaría si no estuviera: El navegador podría recibir la respuesta como texto plano o HTML por defecto, rompiendo el flujo del frontend.
        response.setContentType("application/json");
        
        // Qué hace: Establece la codificación de caracteres en UTF-8.
        // Por qué existe: Garantiza la codificación correcta de acentos en el mensaje de respuesta.
        // Qué pasaría si no estuviera: Los acentos podrían verse deformados en el navegador cliente.
        response.setCharacterEncoding("UTF-8");
        
        // Qué hace: Obtiene el PrintWriter de salida mediante try-with-resources para asegurar el cierre automático del flujo.
        // Por qué existe: Permite escribir el cuerpo de la respuesta que viaja al navegador, liberando recursos automáticamente al terminar el bloque.
        // Qué pasaría si no estuviera: No podríamos escribir la respuesta JSON al cliente y además correríamos el riesgo de fugas de memoria por no cerrar el buffer.
        try (PrintWriter out = response.getWriter()) {
            
            try {
                // Qué hace: Recupera la sesión activa actual del usuario sin crear una nueva pasándole 'false' como argumento.
                // Por qué existe: Evita crear una sesión fantasma innecesaria en memoria en caso de que ya no exista o haya expirado.
                // Qué pasaría si no estuviera: Si usáramos request.getSession() sin false, se crearía una sesión nueva si no existía, lo cual es inútil en un logout.
                HttpSession session = request.getSession(false);
                
                if (session != null) {
                    // Qué hace: Destruye/invalida la sesión activa y elimina todos los atributos asociados (como 'user_id') en el servidor web.
                    // Por qué existe: Libera la memoria consumida por los datos del usuario en sesión y revoca de forma definitiva el JSESSIONID.
                    // Qué pasaría si no estuviera: La sesión y su cookie seguirían existiendo en el servidor hasta que expiren por inactividad, lo que representa un fallo de seguridad grave.
                    session.invalidate();
                }
                
                // Qué hace: Crea el JSON de respuesta exitosa.
                // Por qué existe: Estructura la respuesta de éxito para que el frontend pueda procesarla y redirigir al login.
                // Qué pasaría si no estuviera: El frontend no sabría si la acción se completó con éxito en el servidor.
                JSONObject json = new JSONObject();
                json.put("success", true);
                json.put("message", "Sesión cerrada exitosamente.");
                
                // Qué hace: Establece el estado HTTP a 200 OK.
                // Por qué existe: Indica explícitamente el éxito semántico de la llamada.
                response.setStatus(HttpServletResponse.SC_OK);
                
                // Qué hace: Imprime la respuesta JSON.
                out.print(json.toString());
                
            } catch (Exception e) {
                // Qué hace: Captura cualquier error ocurrido durante la destrucción de la sesión.
                // Por qué existe: Previene la caída del servlet ante errores imprevistos.
                // Qué pasaría si no estuviera: El servidor enviaría una traza de error en formato HTML exponiendo detalles internos e inseguros.
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                JSONObject error = new JSONObject();
                error.put("success", false);
                error.put("message", e.getMessage());
                out.print(error.toString());
            }
        }
    }

    /**
     * Qué hace: Redirige las peticiones GET hacia processRequest.
     * Por qué existe: Permite procesar cierres de sesión iniciados por redirecciones simples de tipo GET.
     * 
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Qué hace: Redirige las peticiones POST hacia processRequest.
     * Por qué existe: Permite procesar cierres de sesión iniciados de forma segura mediante llamadas asíncronas de tipo POST.
     * 
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Qué hace: Retorna una descripción corta sobre este Servlet.
     * 
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Servlet de cierre de sesión";
    }
}
