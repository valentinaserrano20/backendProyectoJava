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
// Qué pasaría si no estuviera: Las familias no tendrían la posibilidad de asignar tareas específicas de reducción (como fijar estanterías, almacenar agua, asegurar techos) a los integrantes de la familia.
@WebServlet("/api/accionesReduccion/*")
public class AccionesReduccionServlet extends HttpServlet {
    // Qué hace: Instancia el servicio de lógica de negocios para los factores de riesgo y sus acciones asociadas.
    // Por qué existe: Mantiene desacoplada la capa de presentación de la persistencia de datos.
    // Qué pasaría si no estuviera: El controlador tendría que orquestar las consultas SQL directamente.
    // Flujo: De aquí pasamos a FactorRiesgoServicio.
    private final FactorRiesgoServicio servicio = new FactorRiesgoServicio();

    // Qué hace: Intercepta peticiones HTTP y enruta PATCH a doPatch.
    // Por qué existe: El servlet base no provee soporte para PATCH nativo.
    // Qué pasaría si no estuviera: Las peticiones PATCH hechas por la SPA web fallarían con código HTTP 405.
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
    // Por qué existe: Suministra los datos de las tareas asignadas para que se muestren en el plan de emergencia familiar del frontend.
    // Qué pasaría si no estuviera: No podríamos recuperar ni desplegar en pantalla las acciones correctivas planeadas para mitigar los riesgos.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Qué hace: Verifica que el usuario cuente con una sesión de servidor iniciada.
        // Por qué existe: Restringe el acceso a la información confidencial de las viviendas de los ciudadanos.
        // Qué pasaría si no estuviera: Cualquier persona podría auditar los planes y tareas de evacuación familiares externamente.
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
                
                // Qué hace: Lista las acciones de reducción ligadas al factor de riesgo especificado.
                // y luego de esto pasamos a FactorRiesgoServicio.listarAcciones, el cual realiza el SELECT correspondiente en la base de datos.
                String resJson = servicio.listarAcciones(riesgoId);
                response.getWriter().write(resJson);
            } else {
                int id = Integer.parseInt(parts[1]);
                
                // Qué hace: Obtiene los detalles de una acción de reducción individual.
                // y luego de esto pasamos a FactorRiesgoServicio.obtenerAccion, que lee la base de datos por ID.
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

    // Qué hace: Atiende peticiones POST para crear una acción de reducción asociada a un factor de riesgo.
    // Por qué existe: Habilita el registro de una nueva tarea preventiva en la base de datos.
    // Qué pasaría si no estuviera: Los voluntarios no tendrían un endpoint para añadir nuevas tareas preventivas a la familia evaluada.
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Qué hace: Valida la sesión del usuario.
        // Por qué existe: Previene que usuarios anónimos envíen payloads e inyecten tareas falsas.
        // Qué pasaría si no estuviera: Podrían insertarse tareas preventivas no válidas de forma malintencionada.
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
            
            // Qué hace: Persiste el DTO de la acción llamando al servicio.
            // y luego de esto pasamos a FactorRiesgoServicio.crearAccion, que ejecuta la inserción SQL de la tarea.
            String resJson = servicio.crearAccion(dto);
            response.getWriter().write(resJson);
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al crear acción de reducción: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Atiende peticiones PATCH para actualizar una acción de reducción de riesgo.
    // Por qué existe: Permite modificar el contenido de la tarea, su fecha límite de ejecución o el familiar responsable.
    // Qué pasaría si no estuviera: No podríamos reasignar tareas o cambiar fechas de compromiso de reducción en el censo familiar.
    protected void doPatch(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Qué hace: Comprueba la sesión activa.
        // Por qué existe: Evita alteraciones de la planificación preventiva familiar de forma anónima.
        // Qué pasaría si no estuviera: Usuarios sin autorización podrían manipular quién es responsable de qué tarea de evacuación.
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
            
            // Qué hace: Actualiza la acción correspondiente en la capa de servicios.
            // y luego de esto pasamos a FactorRiesgoServicio.actualizarAccion, que guarda los cambios en MySQL.
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

    // Qué hace: Atiende peticiones DELETE para borrar una acción de reducción.
    // Por qué existe: Permite desvincular o descartar tareas preventivas obsoletas del plan de mitigación.
    // Qué pasaría si no estuviera: Las acciones erróneas o canceladas se quedarían registradas permanentemente, confundiendo a los miembros de la familia.
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Qué hace: Valida la sesión de usuario.
        // Por qué existe: Previene la eliminación de medidas de seguridad y planeación familiar por actores no autenticados.
        // Qué pasaría si no estuviera: Cualquier persona externa podría borrar la planificación de reducción de riesgos.
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
            
            // Qué hace: Elimina la tarea especificada.
            // y luego de esto pasamos a FactorRiesgoServicio.eliminarAccion, que remueve físicamente el registro de la base de datos MySQL.
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
