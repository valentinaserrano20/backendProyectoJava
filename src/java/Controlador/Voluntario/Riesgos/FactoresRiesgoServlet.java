package Controlador.Voluntario.Riesgos;

import Modelo.DTO.FactorRiesgoDTO;
import Modelo.Servicios.Voluntario.FactorRiesgoServicio;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import org.json.JSONObject;

// Qué hace: Servlet encargado de mapear las peticiones HTTP CRUD (GET, POST, PATCH, DELETE) sobre la entidad factores de riesgo.
// Por qué existe: Actúa como el controlador de entrada para gestionar el flujo de riesgos del plan de emergencia familiar.
// Qué problema resuelve: Enruta y valida los payloads mediante la sesión activa y delega su procesamiento en la capa de servicios.
@WebServlet("/api/factoresRiesgo/*")
public class FactoresRiesgoServlet extends HttpServlet {
    private final FactorRiesgoServicio servicio = new FactorRiesgoServicio();

    // Qué hace: Intercepta peticiones HTTP entrantes redirigiendo el método PATCH a doPatch y enviando los demás a super.service.
    // Por qué existe: Habilita el soporte para peticiones parciales PATCH que la especificación de HttpServlet base no cubre.
    // Qué problema resuelve: Permite implementar actualizaciones parciales de campos en la interfaz web de riesgos.
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        String method = req.getMethod();
        if (method.equalsIgnoreCase("PATCH")) {
            doPatch(req, resp);
        } else {
            super.service(req, resp);
        }
    }

    // Qué hace: Atiende peticiones GET para listar todos los riesgos, por plan familiar (bajo /planFamiliar/{planId}), por selección (bajo /planFamiliar/seleccion/{planId}) o por ID de riesgo (bajo /{id}).
    // Por qué existe: Suministra los datos de factores de riesgo al frontend.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            return;
        }
        
        String pathInfo = request.getPathInfo();
        
        // Si no hay path info, se asume listar todos (Supervisor)
        if (pathInfo == null || pathInfo.equals("/")) {
            String resJson = servicio.obtenerTodos();
            response.getWriter().write(resJson);
            return;
        }
        
        String[] parts = pathInfo.split("/");
        
        try {
            if (parts[1].equals("planFamiliar")) {
                if (parts.length < 3) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de plan familiar no provisto.").toString());
                    return;
                }
                int planId = Integer.parseInt(parts[2]);
                
                if (parts.length > 3 && parts[3].equals("seleccion")) {
                    // Endpoint para combo de selección
                    String resJson = servicio.listarFactoresSelect(planId);
                    response.getWriter().write(resJson);
                } else {
                    // Endpoint normal paginado
                    String pageParam = request.getParameter("page");
                    int page = 1;
                    if (pageParam != null && !pageParam.isEmpty()) {
                        page = Integer.parseInt(pageParam);
                    }
                    String resJson = servicio.listarFactores(planId, page);
                    response.getWriter().write(resJson);
                }
            } else {
                int id = Integer.parseInt(parts[1]);
                String resJson = servicio.obtenerFactor(id);
                response.getWriter().write(resJson);
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "El identificador debe ser numérico.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error interno: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Atiende peticiones POST para registrar un nuevo factor de riesgo.
    // Por qué existe: Permite guardar el formulario inicial en MySQL.
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            return;
        }
        
        try {
            StringBuilder buffer = new StringBuilder();
            String line;
            try (BufferedReader reader = request.getReader()) {
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }
            }
            
            JSONObject json = new JSONObject(buffer.toString());
            FactorRiesgoDTO dto = new FactorRiesgoDTO();
            dto.setThreatTypeId(json.getInt("threat_type_id"));
            dto.setDescription(json.getString("description"));
            dto.setUbication(json.getString("ubication"));
            dto.setDistance(json.optInt("distance", 0));
            dto.setFamilyPlanId(json.getInt("family_plan_id"));
            
            String resJson = servicio.crearFactor(dto);
            response.getWriter().write(resJson);
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al crear factor de riesgo: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Atiende peticiones PATCH para actualizar un factor de riesgo.
    // Por qué existe: Modifica la ubicación, descripción y tipo de amenaza del riesgo padre.
    protected void doPatch(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            return;
        }
        
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID no provisto.").toString());
            return;
        }
        
        String[] parts = pathInfo.split("/");
        
        try {
            int id = Integer.parseInt(parts[1]);
            
            StringBuilder buffer = new StringBuilder();
            String line;
            try (BufferedReader reader = request.getReader()) {
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }
            }
            
            JSONObject json = new JSONObject(buffer.toString());
            FactorRiesgoDTO dto = new FactorRiesgoDTO();
            dto.setThreatTypeId(json.getInt("threat_type_id"));
            dto.setDescription(json.getString("description"));
            // Soporta tanto 'location' de editarController como 'ubication' de crearController
            dto.setUbication(json.optString("location", json.optString("ubication", "")));
            dto.setDistance(json.optInt("distance", 0));
            
            String resJson = servicio.actualizarFactor(id, dto);
            response.getWriter().write(resJson);
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID debe ser numérico.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al actualizar: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Atiende peticiones DELETE para eliminar un factor de riesgo.
    // Por qué existe: Quita físicamente el factor y sus dependencias (transaccional).
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            return;
        }
        
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID no provisto.").toString());
            return;
        }
        
        String[] parts = pathInfo.split("/");
        
        try {
            int id = Integer.parseInt(parts[1]);
            String resJson = servicio.eliminarFactor(id);
            response.getWriter().write(resJson);
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID debe ser numérico.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al eliminar: " + e.getMessage()).toString());
        }
    }
}
