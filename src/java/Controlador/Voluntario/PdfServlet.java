package Controlador.Voluntario;

/*
 * Qué hace (la acción): Importa la clase de servicio de PDFs, la clase de utilidad de respuestas web, excepciones y APIs del servlet de Jakarta.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.Servicios.Voluntario.PdfServicio: Servicio de negocio que lee el plan de emergencia familiar de base de datos y utiliza librerías de generación (como iText) para escribir el documento en un flujo de salida (OutputStream).
 *   - jakarta.servlet.http.HttpServletRequest / HttpServletResponse: Objetos para procesar la petición y construir la respuesta HTTP.
 * Para qué se usa (el propósito): Proveer al servlet de las APIs requeridas para compilar y transmitir el reporte PDF del plan de emergencia familiar.
 * Por qué es importante (el impacto o problema que resuelve): Sin estas importaciones, no se podría retornar el archivo de manera directa al navegador del usuario.
 */
import Modelo.Servicios.Voluntario.PdfServicio;
import Modelo.Utilidades.ResponseUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/*
 * Qué hace (la acción): Asocia el servlet PdfServlet con el endpoint de red "/api/pdf/*" utilizando la anotación @WebServlet.
 * Qué significa (conceptos, métodos, tipos involucrados): Mapea la ruta para que cualquier llamada que inicie con "/api/pdf/" sea procesada por este controlador.
 * Para qué se usa (el propósito): Servir como el endpoint de exportación para la descarga o visualización del PDF del Plan de Emergencia Familiar.
 * Por qué es importante (el impacto o problema que resuelve): Permite descargar el documento final del plan familiar en un formato estándar y portable (PDF), ideal para ser impreso o archivado físicamente por la familia.
 */
@WebServlet("/api/pdf/*")
public class PdfServlet extends HttpServlet {

    /*
     * Qué hace (la acción): Instancia de manera privada y constante la variable servicio de tipo PdfServicio.
     * Qué significa (conceptos, métodos, tipos involucrados): Instancia de la clase de servicios de negocio para reportes PDF.
     * Para qué se usa (el propósito): Invocar el generador del reporte PDF.
     */
    private final PdfServicio servicio = new PdfServicio();

    /*
     * Qué hace (la acción): Sobrescribe el método doGet para capturar el ID del plan de la URL, configurar el tipo de contenido HTTP a "application/pdf" y llamar al servicio para transmitir el documento generado directamente al flujo de salida del cliente.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - response.setContentType("application/pdf"): Configura la respuesta HTTP indicando que el cuerpo contiene datos binarios del protocolo PDF.
     *   - response.setHeader("Content-Disposition", "inline; filename=..."): Cabecera HTTP que indica al navegador que abra el archivo en su visor de PDF integrado en lugar de forzar la descarga inmediata.
     *   - request.getServletContext().getRealPath("/"): Recupera la ruta física raíz en el servidor para que el generador acceda a recursos estáticos (ej. logotipos de la Cruz Roja).
     *   - response.getOutputStream(): Flujo de salida binario nativo que escribe directo en el socket de red del cliente.
     * Para qué se usa (el propósito): Renderizar el documento completo del Plan de Emergencia (información familiar, riesgos, croquis, plan de acción y maletín) en el navegador del voluntario o supervisor.
     * Por qué es importante (el impacto o problema que resuelve): Permite la descarga fluida y en tiempo real del PDF del plan, administrando de forma segura y separada los casos de ID no válidos o excepciones mediante respuestas JSON alternativas.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID de plan de emergencia no provisto."));
            return;
        }

        String[] parts = pathInfo.split("/");
        
        try {
            int planId = Integer.parseInt(parts[1]);
            
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "inline; filename=\"Plan_Emergencia_Familiar_" + planId + ".pdf\"");
            
            String contextPath = request.getServletContext().getRealPath("/");
            
            servicio.generarPlanEmergenciaPDF(planId, contextPath, response.getOutputStream());
            
        } catch (NumberFormatException e) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("El identificador del plan debe ser numérico."));
        } catch (Exception e) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(ResponseUtil.error("Error al generar el PDF del plan: " + e.getMessage()));
        }
    }
}
