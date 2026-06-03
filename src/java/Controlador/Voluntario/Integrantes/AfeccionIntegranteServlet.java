package Controlador.Voluntario.Integrantes;

import Modelo.DTO.AfeccionDTO;
import Modelo.Servicios.Voluntario.IntegranteServicio;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import org.json.JSONObject;

// Qué hace: Servlet encargado de mapear las peticiones HTTP CRUD (GET, POST, PUT, DELETE) sobre afecciones médicas y medicamentos de los integrantes.
// Por qué existe: Actúa como el controlador de entrada para gestionar el flujo de datos médicos del integrante del plan de emergencia familiar.
// Qué problema resuelve: Enruta las peticiones de red de afecciones hacia la capa de negocio respectiva en base al usuario autenticado.
@WebServlet("/api/conditionMembers/*")
public class AfeccionIntegranteServlet extends HttpServlet {
    private final IntegranteServicio servicio = new IntegranteServicio();

    // Qué hace: Atiende peticiones GET para listar afecciones de un integrante (/member/{memberId}) o consultar los detalles de una afección individual (/{id}).
    // Por qué existe: Permite a las pantallas mostrar los padecimientos, dosis y medicamentos del integrante en el navegador.
    // Qué problema resuelve: Provee acceso de lectura a la bitácora médica de los integrantes familiares en formato JSON compatible.
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
            if (parts[1].equals("member")) {
                if (parts.length < 3) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de integrante no provisto.").toString());
                    return;
                }
                int memberId = Integer.parseInt(parts[2]);
                String resJson = servicio.listarAfecciones(memberId);
                response.getWriter().write(resJson);
            } else {
                int id = Integer.parseInt(parts[1]);
                String resJson = servicio.obtenerAfeccion(id);
                response.getWriter().write(resJson);
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "El identificador debe ser numérico.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error del servidor: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Atiende peticiones POST para registrar una nueva afección médica para un integrante.
    // Por qué existe: Permite persistir en la base de datos el padecimiento (enfermedad/discapacidad/alergia) y su dosis de medicamento.
    // Qué problema resuelve: Recibe el payload JSON, mapea a DTO e inicia el proceso transaccional de escritura en afecciones y medicamentos.
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
            AfeccionDTO dto = new AfeccionDTO();
            dto.setMemberId(json.getInt("member_id"));
            dto.setConditionTypeId(json.getInt("condition_type_id"));
            dto.setName(json.getString("name"));
            dto.setDose(json.optString("dose", null));
            
            String resJson = servicio.crearAfeccion(dto);
            response.getWriter().write(resJson);
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al registrar afección: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Atiende peticiones PUT para modificar los datos de una afección médica existente (tipo, nombre y dosis) por su ID único.
    // Por qué existe: Permite modificar el tratamiento o corregir padecimientos registrados con anterioridad.
    // Qué problema resuelve: Recibe el cuerpo JSON actualizando concurrentemente la afección y la dosis en la base de datos de manera atómica.
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
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
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de afección no provisto.").toString());
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
            AfeccionDTO dto = new AfeccionDTO();
            dto.setMemberId(json.getInt("member_id"));
            dto.setConditionTypeId(json.getInt("condition_type_id"));
            dto.setName(json.getString("name"));
            dto.setDose(json.optString("dose", null));
            
            String resJson = servicio.actualizarAfeccion(id, dto);
            response.getWriter().write(resJson);
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de afección debe ser numérico.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al actualizar afección: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Atiende peticiones DELETE para remover físicamente una afección y sus medicamentos.
    // Por qué existe: Permite descartar diagnósticos o alergias previamente asociadas al integrante familiar.
    // Qué problema resuelve: Purga en cascada los medicamentos del registro médico para evitar inconsistencias de llaves foráneas.
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
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de afección no provisto.").toString());
            return;
        }
        
        String[] parts = pathInfo.split("/");
        
        try {
            int id = Integer.parseInt(parts[1]);
            String resJson = servicio.eliminarAfeccion(id);
            response.getWriter().write(resJson);
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de afección debe ser numérico.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al eliminar afección: " + e.getMessage()).toString());
        }
    }
}
