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

/**
 * Qué hace: Servlet encargado de mapear las peticiones HTTP CRUD (GET, POST, PUT, DELETE) sobre afecciones médicas y medicamentos de los integrantes.
 * Por qué existe: Actúa como el controlador de entrada para gestionar el flujo de datos médicos del integrante del plan de emergencia familiar.
 * Qué pasaría si no estuviera: No podríamos registrar discapacidades, enfermedades crónicas o alergias ni sus dosis de medicamentos requeridas en las emergencias.
 */
@WebServlet("/api/conditionMembers/*")
public class AfeccionIntegranteServlet extends HttpServlet {

    // Qué hace: Instancia el servicio de lógica de negocios para los integrantes familiares.
    // Por qué existe: Desacopla la lógica JDBC y de negocio médica del enrutamiento de red.
    // Qué pasaría si no estuviera: Deberíamos escribir PreparedStatements directas en la base de datos dentro de los métodos del controlador web.
    // Flujo: De aquí pasamos a IntegranteServicio.
    private final IntegranteServicio servicio = new IntegranteServicio();

    // Qué hace: Atiende peticiones GET para listar afecciones de un integrante (/member/{memberId}) o consultar los detalles de una afección individual (/{id}).
    // Por qué existe: Permite a las pantallas mostrar los padecimientos, dosis y medicamentos del integrante en el navegador.
    // Qué pasaría si no estuviera: El listado o detalles del diagnóstico no podrían ser visualizados en el formulario del censo.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Qué hace: Comprueba la existencia de una sesión activa.
        // Por qué existe: Resguarda la privacidad de los datos de salud e información clínica confidencial de los ciudadanos.
        // Qué pasaría si no estuviera: Cualquier persona podría listar las enfermedades o alergias de los integrantes familiares sin autenticarse.
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
                
                // Qué hace: Recupera las afecciones del integrante familiar.
                // y luego de esto pasamos a IntegranteServicio.listarAfecciones, el cual realiza el query a la base de datos relacional.
                String resJson = servicio.listarAfecciones(memberId);
                response.getWriter().write(resJson);
            } else {
                int id = Integer.parseInt(parts[1]);
                
                // Qué hace: Obtiene una afección específica.
                // y luego de esto pasamos a IntegranteServicio.obtenerAfeccion, que hace SELECT filtrando por ID.
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
    // Qué pasaría si no estuviera: No podríamos registrar nuevas condiciones de salud del miembro en el censo familiar.
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
            
            // Qué hace: Guarda la nueva condición médica en la base de datos.
            // y luego de esto pasamos a IntegranteServicio.crearAfeccion, que valida la petición e inserta los datos.
            String resJson = servicio.crearAfeccion(dto);
            response.getWriter().write(resJson);
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al registrar afección: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Atiende peticiones PUT para modificar los datos de una afección médica existente (tipo, nombre y dosis) por su ID único.
    // Por qué existe: Permite modificar el tratamiento o corregir padecimientos registrados con anterioridad.
    // Qué pasaría si no estuviera: No se podrían corregir errores de tipografía o cambios de dosificación en los medicamentos del integrante.
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
            
            // Qué hace: Actualiza la afección especificada llamando al servicio.
            // y luego de esto pasamos a IntegranteServicio.actualizarAfeccion, el cual actualiza el registro en la BD.
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
    // Qué pasaría si no estuviera: Las condiciones médicas erróneas o superadas no podrían retirarse de la ficha familiar.
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
            
            // Qué hace: Elimina la afección seleccionada llamando al servicio.
            // y luego de esto pasamos a IntegranteServicio.eliminarAfeccion, que borra el registro en MySQL.
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
