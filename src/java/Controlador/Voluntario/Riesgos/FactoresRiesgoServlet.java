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
// Por qué existe: Actúa como el controlador de entrada para gestionar el flujo de riesgos (inundaciones, incendios, deslizamientos, etc.) del plan de emergencia familiar.
// Qué pasaría si no estuviera: Las familias no tendrían la posibilidad de identificar ni registrar los riesgos geográficos o estructurales a los que está expuesta su vivienda.
@WebServlet("/api/factoresRiesgo/*")
public class FactoresRiesgoServlet extends HttpServlet {
    // Qué hace: Instancia el servicio de lógica de negocios para los factores de riesgo.
    // Por qué existe: Encapsula el acceso y manipulación de datos de riesgos del plan.
    // Qué pasaría si no estuviera: El servlet debería instanciar PreparedStatements directos para consultar las tablas en MySQL.
    // Flujo: De aquí pasamos a FactorRiesgoServicio.
    private final FactorRiesgoServicio servicio = new FactorRiesgoServicio();

    // Qué hace: Intercepta peticiones HTTP entrantes redirigiendo el método PATCH a doPatch y enviando los demás a super.service.
    // Por qué existe: Habilita el soporte para peticiones parciales PATCH que la especificación de HttpServlet base no cubre.
    // Qué pasaría si no estuviera: Las llamadas de actualización parcial PATCH hechas desde la SPA web arrojarían error HTTP 405 (Method Not Allowed).
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
    // Por qué existe: Suministra la información de amenazas geográficas detectadas y registradas para poblar los formularios y combos interactivos en el frontend.
    // Qué pasaría si no estuviera: La aplicación SPA no podría renderizar las tarjetas de riesgos ni los combos de selección para asignar tareas preventivas.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Qué hace: Comprueba que el usuario mantenga una sesión activa en el servidor.
        // Por qué existe: Resguarda los datos de ubicación y descripción de riesgos habitacionales de las viviendas.
        // Qué pasaría si no estuviera: Cualquier persona podría descargar reportes de riesgos habitacionales específicos sin autenticación previa.
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            return;
        }
        
        String pathInfo = request.getPathInfo();
        
        // Si no hay path info, se asume listar todos (Supervisor)
        if (pathInfo == null || pathInfo.equals("/")) {
            // Qué hace: Recupera todos los factores de riesgo registrados (uso del supervisor).
            // y luego de esto pasamos a FactorRiesgoServicio.obtenerTodos, que ejecuta la consulta general en MySQL.
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
                    // Qué hace: Obtiene la lista simplificada de factores para poblar los dropdowns de selección.
                    // y luego de esto pasamos a FactorRiesgoServicio.listarFactoresSelect, el cual mapea ID y descripción en un JSON simple.
                    String resJson = servicio.listarFactoresSelect(planId);
                    response.getWriter().write(resJson);
                } else {
                    // Endpoint normal paginado
                    String pageParam = request.getParameter("page");
                    int page = 1;
                    if (pageParam != null && !pageParam.isEmpty()) {
                        page = Integer.parseInt(pageParam);
                    }
                    // Qué hace: Recupera los factores de riesgo paginados vinculados al plan familiar.
                    // y luego de esto pasamos a FactorRiesgoServicio.listarFactores, que retorna el listado de amenazas con sus niveles y distancias.
                    String resJson = servicio.listarFactores(planId, page);
                    response.getWriter().write(resJson);
                }
            } else {
                int id = Integer.parseInt(parts[1]);
                // Qué hace: Recupera la información detallada de una amenaza por su identificador.
                // y luego de esto pasamos a FactorRiesgoServicio.obtenerFactor, que lee el registro de la amenaza por ID.
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
    // Por qué existe: Permite guardar el formulario inicial de una nueva amenaza en la base de datos.
    // Qué pasaría si no estuviera: No se podrían dar de alta nuevos riesgos geográficos detectados durante la evaluación del voluntario.
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Qué hace: Valida la sesión del usuario.
        // Por qué existe: Evita que actores anónimos envíen peticiones de registro de riesgos ficticios.
        // Qué pasaría si no estuviera: Podría inyectarse información de riesgo errónea o malintencionada en los planes.
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
            
            // Qué hace: Persiste el factor de riesgo a través del servicio.
            // y luego de esto pasamos a FactorRiesgoServicio.crearFactor, que guarda la amenaza en base de datos.
            String resJson = servicio.crearFactor(dto);
            response.getWriter().write(resJson);
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al crear factor de riesgo: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Atiende peticiones PATCH para actualizar un factor de riesgo.
    // Por qué existe: Modifica la ubicación, descripción, tipo de amenaza o distancia del riesgo padre.
    // Qué pasaría si no estuviera: El voluntario no podría corregir equivocaciones en las descripciones, ubicaciones o distancias sin eliminar por completo el riesgo.
    protected void doPatch(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Qué hace: Valida la sesión del usuario.
        // Por qué existe: Protege los datos habitacionales contra manipulaciones externas anónimas.
        // Qué pasaría si no estuviera: Cualquiera podría modificar de forma malintencionada la distancia o descripción de una amenaza seria.
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
            
            // Qué hace: Actualiza la información del factor de riesgo delegando la tarea en el servicio.
            // y luego de esto pasamos a FactorRiesgoServicio.actualizarFactor, que persiste los cambios en MySQL.
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
    // Por qué existe: Quita físicamente el factor y sus dependencias de la base de datos (con soporte transaccional).
    // Qué pasaría si no estuviera: Las amenazas descartadas o mal identificadas no podrían ser removidas del plan, distorsionando el censo de vulnerabilidades del hogar.
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Qué hace: Valida la sesión del usuario.
        // Por qué existe: Resguarda la seguridad del censo familiar evitando borrados destructivos no autorizados.
        // Qué pasaría si no estuviera: Usuarios anónimos podrían eliminar por completo la información de riesgos de una vivienda.
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
            
            // Qué hace: Borra de forma lógica o física la amenaza y sus registros sanitarios asociados.
            // y luego de esto pasamos a FactorRiesgoServicio.eliminarFactor, el cual remueve la fila correspondiente en MySQL.
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
