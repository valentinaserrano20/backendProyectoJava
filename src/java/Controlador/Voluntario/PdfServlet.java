package Controlador.Voluntario;

import Modelo.Servicios.Voluntario.PdfServicio;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.json.JSONObject;

/**
 * Qué hace: Servlet controlador mapeado a /api/pdf/* que atiende la descarga y visualización del reporte del plan de emergencia.
 * Por qué existe: Expone el endpoint HTTP para generar el documento PDF final y transmitirlo como binario al navegador.
 * Qué pasaría si no estuviera: Las familias y supervisores no podrían descargar ni imprimir la ficha física en PDF del plan familiar de emergencias.
 */
@WebServlet("/api/pdf/*")
public class PdfServlet extends HttpServlet {

    // Qué hace: Instancia el servicio de generación de documentos PDF.
    // Por qué existe: Permite encapsular la maquetación y adición de celdas y tablas del reporte iText en la capa de servicios.
    // Qué pasaría si no estuviera: Tendríamos que escribir la lógica de diseño de iText de decenas de páginas dentro del controlador.
    // Flujo: De aquí pasamos a PdfServicio.
    private final PdfServicio servicio = new PdfServicio();

    // Qué hace: Procesa la petición GET del PDF del plan familiar.
    // Por qué existe: Permite abrir el documento PDF dinámico directamente en una pestaña del navegador al presionar "Ver PDF".
    // Qué pasaría si no estuviera: La SPA no podría abrir ni renderizar el documento PDF binario generado al usuario.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Qué hace: Valida la sesión del voluntario o supervisor activo.
        // Por qué existe: Previene que usuarios no autorizados descarguen de forma masiva los censos de familias.
        // Qué pasaría si no estuviera: Cualquier persona podría descargar los PDFs de los planes de emergencia, lo que vulnera gravemente los datos de las familias.
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioId") == null) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            return;
        }

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de plan de emergencia no provisto.").toString());
            return;
        }

        String[] parts = pathInfo.split("/");
        
        try {
            int planId = Integer.parseInt(parts[1]);
            
            // Qué hace: Configura las cabeceras HTTP de tipo de contenido para indicar que la respuesta contiene bytes de PDF.
            // Por qué existe: Le indica al navegador que debe abrir su visualizador interno de PDF en vez de intentar descargarlo como archivo genérico o interpretarlo como HTML.
            // Qué pasaría si no estuviera: El navegador intentaría abrir los bytes crudos como texto plano, visualizando caracteres extraños incomprensibles.
            response.setContentType("application/pdf");
            
            // Qué hace: Configura la disposición del contenido como "inline".
            // Por qué existe: Habilita la apertura fluida en pestaña nueva conservando un nombre de archivo preestablecido al guardarse.
            response.setHeader("Content-Disposition", "inline; filename=\"Plan_Emergencia_Familiar_" + planId + ".pdf\"");
            
            // Qué hace: Resuelve la ruta física del contexto del servlet en el disco del servidor.
            // Por qué existe: Permite localizar los logos del cabezal y las imágenes de los croquis almacenados en las carpetas internas del servidor Java.
            // Qué pasaría si no estuviera: La librería iText no sabría en qué ruta absoluta de la máquina buscar las imágenes de croquis que debe incrustar en el documento.
            String contextPath = request.getServletContext().getRealPath("/");
            
            // Qué hace: Invoca la lógica de generación pasándole el flujo de salida directo del response (ServletOutputStream).
            // y luego de esto pasamos a PdfServicio.generarPlanEmergenciaPDF, el cual crea el documento iText y escribe directamente en el stream de respuesta.
            servicio.generarPlanEmergenciaPDF(planId, contextPath, response.getOutputStream());
            
        } catch (NumberFormatException e) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "El identificador del plan debe ser numérico.").toString());
        } catch (Exception e) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al generar el PDF del plan: " + e.getMessage()).toString());
        }
    }
}
