package Controlador.Voluntario.Riesgos;

import Modelo.DTO.AccionReduccionDTO;
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

// Qué hace: Servlet encargado de mapear las peticiones HTTP CRUD (GET, POST, PATCH, DELETE) sobre la entidad acciones de reducción de riesgo.
// Por qué existe: Actúa como el controlador para gestionar las tareas preventivas asignadas a los miembros del hogar.
// Qué problema resuelve: Enruta y procesa las peticiones relacionales de las tareas asignadas al factor de riesgo.
@WebServlet("/api/accionesReduccion/*")
public class AccionesReduccionServlet extends HttpServlet {
    private final FactorRiesgoServicio servicio = new FactorRiesgoServicio();

    // Qué hace: Intercepta peticiones HTTP y enruta PATCH a doPatch.
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

    // Qué hace: Atiende peticiones GET para obtener las acciones de un riesgo (/factorRiesgo/{riesgoId}) o una acción por ID (/{id}).
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
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Recurso no especificado.").toString());
            return;
        }
        
        String[] parts = pathInfo.split("/");
        
        try {
            if (parts[1].equals("factorRiesgo")) {
                if (parts.length < 3) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de factor de riesgo no provisto.").toString());
                    return;
                }
                int riesgoId = Integer.parseInt(parts[2]);
                String resJson = servicio.listarAcciones(riesgoId);
                response.getWriter().write(resJson);
            } else {
                int id = Integer.parseInt(parts[1]);
                String resJson = servicio.obtenerAccion(id);
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

    // Qué hace: Atiende peticiones POST para crear una acción.
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
            AccionReduccionDTO dto = new AccionReduccionDTO();
            dto.setAction(json.getString("action"));
            dto.setEndDate(json.getString("end_date"));
            dto.setRiskFactorId(json.getInt("risk_factor_id"));
            dto.setMemberId(json.optInt("member_id", 0));
            
            String resJson = servicio.crearAccion(dto);
            response.getWriter().write(resJson);
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al crear acción de reducción: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Atiende peticiones PATCH para actualizar una acción.
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
            AccionReduccionDTO dto = new AccionReduccionDTO();
            dto.setAction(json.getString("action"));
            dto.setEndDate(json.getString("end_date"));
            dto.setMemberId(json.optInt("member_id", 0));
            
            String resJson = servicio.actualizarAccion(id, dto);
            response.getWriter().write(resJson);
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID debe ser numérico.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al actualizar la acción: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Atiende peticiones DELETE para borrar una acción.
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
            String resJson = servicio.eliminarAccion(id);
            response.getWriter().write(resJson);
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID debe ser numérico.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al eliminar la acción: " + e.getMessage()).toString());
        }
    }
}
