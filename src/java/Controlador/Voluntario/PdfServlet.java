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

// Qué hace: Servlet controlador mapeado a /api/pdf/* que atiende la descarga y visualización del reporte del plan de emergencia.
// Por qué existe: Expone el endpoint HTTP para generar el documento PDF final y transmitirlo como binario al navegador.
// Qué problema resuelve: Enruta la petición, valida la sesión del voluntario o supervisor, configura las cabeceras HTTP de contenido PDF y transmite los bytes.
@WebServlet("/api/pdf/*")
public class PdfServlet extends HttpServlet {
    private final PdfServicio servicio = new PdfServicio();

    // Qué hace: Procesa la petición GET del PDF del plan familiar.
    // Por qué existe: Permite abrir el documento PDF dinámico directamente en una pestaña del navegador al presionar "Ver PDF".
    // Qué problema resuelve: Valida la sesión activa, lee el identificador del plan de la URL, delega la construcción del archivo binario y lo envía al cliente.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
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
            
            // Configurar cabeceras de respuesta para PDF binario inline
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "inline; filename=\"Plan_Emergencia_Familiar_" + planId + ".pdf\"");
            
            String contextPath = request.getServletContext().getRealPath("/");
            
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
