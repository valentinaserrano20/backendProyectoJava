package Controlador.Voluntario.Riesgos;

import Modelo.DTO.FactorVulnerabilidadDTO;
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

// Qué hace: Servlet encargado de mapear las peticiones HTTP CRUD (GET, POST, PATCH, DELETE) sobre la entidad factores de vulnerabilidad.
// Por qué existe: Actúa como el controlador para gestionar la asociación de vulnerabilidades específicas y sus grados (alto, medio, bajo) a un riesgo identificado.
// Qué pasaría si no estuviera: No se podrían asociar vulnerabilidades particulares (como estructura débil, falta de equipamiento, falta de capacitación) a los factores de riesgo del plan.
@WebServlet("/api/factoresVulnerabilidad/*")
public class FactoresVulnerabilidadServlet extends HttpServlet {
    // Qué hace: Instancia el servicio de lógica de negocios para los factores de riesgo y vulnerabilidades.
    // Por qué existe: Separa la gestión del protocolo HTTP de la lógica y persistencia de base de datos de los riesgos.
    // Qué pasaría si no estuviera: El servlet tendría que escribir código JDBC y manejar PreparedStatements de forma directa.
    // Flujo: De aquí pasamos a FactorRiesgoServicio.
    private final FactorRiesgoServicio servicio = new FactorRiesgoServicio();

    // Qué hace: Intercepta peticiones HTTP y enruta PATCH a doPatch.
    // Por qué existe: Permite dar soporte a actualizaciones de campos aislados del factor de vulnerabilidad.
    // Qué pasaría si no estuviera: Las peticiones PATCH enviadas por el frontend recibirían un código HTTP 405 (Method Not Allowed).
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

    // Qué hace: Atiende peticiones GET para obtener las vulnerabilidades de un riesgo (/factorRiesgo/{riesgoId}) o una vulnerabilidad por ID (/{id}).
    // Por qué existe: Permite al frontend visualizar y poblar los cuadros de vulnerabilidades asociadas a un determinado peligro en el plan familiar.
    // Qué pasaría si no estuviera: El voluntario no podría ver qué vulnerabilidades específicas están agravando cada factor de riesgo de la vivienda.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Qué hace: Valida la sesión activa del usuario.
        // Por qué existe: Evita accesos ilegítimos a los detalles de seguridad habitacional de los ciudadanos.
        // Qué pasaría si no estuviera: Usuarios no autenticados podrían ver los puntos débiles y fallos estructurales de cada vivienda.
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
                
                // Qué hace: Recupera las vulnerabilidades asociadas al factor de riesgo.
                // y luego de esto pasamos a FactorRiesgoServicio.listarVulnerabilidades, que realiza el query correspondiente en MySQL.
                String resJson = servicio.listarVulnerabilidades(riesgoId);
                response.getWriter().write(resJson);
            } else {
                int id = Integer.parseInt(parts[1]);
                
                // Qué hace: Recupera el detalle de una vulnerabilidad específica.
                // y luego de esto pasamos a FactorRiesgoServicio.obtenerVulnerabilidad, el cual ejecuta el SELECT por ID.
                String resJson = servicio.obtenerVulnerabilidad(id);
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

    // Qué hace: Atiende peticiones POST para crear una vulnerabilidad asociada a un riesgo.
    // Por qué existe: Permite asociar una nueva debilidad o vulnerabilidad del catálogo a una amenaza física del hogar.
    // Qué pasaría si no estuviera: No podríamos registrar nuevos puntos débiles vinculados a los riesgos de la vivienda evaluada.
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Qué hace: Valida que la sesión sea válida y activa.
        // Por qué existe: Previene la inyección de vulnerabilidades falsas en los censos de la comunidad.
        // Qué pasaría si no estuviera: Atacantes anónimos podrían desvirtuar el análisis de vulnerabilidad habitacional de los planes.
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
            FactorVulnerabilidadDTO dto = new FactorVulnerabilidadDTO();
            dto.setVulnerabilityId(json.getInt("vulnerability_id"));
            dto.setVulnerabilityGradeId(json.getInt("vulnerability_grade_id"));
            dto.setRiskFactorId(json.getInt("risk_factor_id"));
            
            // Qué hace: Registra la asociación del factor de vulnerabilidad a través de la capa de servicios.
            // y luego de esto pasamos a FactorRiesgoServicio.crearVulnerabilidad, que inserta la relación en MySQL.
            String resJson = servicio.crearVulnerabilidad(dto);
            response.getWriter().write(resJson);
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al crear vulnerabilidad asociada: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Atiende peticiones PATCH para actualizar una vulnerabilidad asociada.
    // Por qué existe: Permite modificar el grado de severidad o el tipo de vulnerabilidad asociada a una amenaza.
    // Qué pasaría si no estuviera: El voluntario no podría reclasificar la severidad de una vulnerabilidad (por ejemplo, de media a alta) sin tener que eliminarla y volverla a crear.
    protected void doPatch(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Qué hace: Valida la sesión activa.
        // Por qué existe: Asegura que solo usuarios autorizados realicen cambios en el grado de las vulnerabilidades.
        // Qué pasaría si no estuviera: Podrían alterarse los niveles de vulnerabilidad de las viviendas sin control de acceso.
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
            FactorVulnerabilidadDTO dto = new FactorVulnerabilidadDTO();
            dto.setVulnerabilityId(json.getInt("vulnerability_id"));
            dto.setVulnerabilityGradeId(json.getInt("vulnerability_grade_id"));
            
            // Qué hace: Actualiza la relación de vulnerabilidad en el servicio.
            // y luego de esto pasamos a FactorRiesgoServicio.actualizarVulnerabilidad, el cual guarda los cambios en la base de datos MySQL.
            String resJson = servicio.actualizarVulnerabilidad(id, dto);
            response.getWriter().write(resJson);
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID debe ser numérico.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al actualizar la vulnerabilidad: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Atiende peticiones DELETE para borrar una vulnerabilidad asociada.
    // Por qué existe: Permite remover debilidades mitigadas o erróneamente vinculadas de las amenazas del hogar.
    // Qué pasaría si no estuviera: Las vulnerabilidades ya corregidas no podrían quitarse del plan de emergencia, devaluando el censo de preparación del hogar.
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Qué hace: Comprueba la sesión activa.
        // Por qué existe: Asegura que las vulnerabilidades críticas del hogar no sean borradas por agentes no identificados.
        // Qué pasaría si no estuviera: Podrían eliminarse de forma ilegítima registros de vulnerabilidades reales en la base de datos.
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
            
            // Qué hace: Elimina la vulnerabilidad asociada a la amenaza.
            // y luego de esto pasamos a FactorRiesgoServicio.eliminarVulnerabilidad, que borra el registro de asociación en MySQL.
            String resJson = servicio.eliminarVulnerabilidad(id);
            response.getWriter().write(resJson);
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID debe ser numérico.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al eliminar la vulnerabilidad: " + e.getMessage()).toString());
        }
    }
}
