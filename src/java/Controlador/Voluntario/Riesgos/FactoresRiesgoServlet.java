package Controlador.Voluntario.Riesgos;

/*
 * Qué hace (la acción): Importa la clase DTO de factores de riesgo, la capa de servicio de factores de riesgo, utilidades de JSON, respuestas web y APIs estándares de servlets de Jakarta.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.DTO.FactorRiesgoDTO: Clase de transferencia que representa los datos de amenazas (internas o externas), descripción, ubicación física y distancia a la vivienda.
 *   - Modelo.Servicios.Voluntario.FactorRiesgoServicio: Servicio de negocio que procesa las validaciones de riesgos y la interacción JDBC con la base de datos SQL.
 * Para qué se usa (el propósito): Proveer al servlet de las dependencias requeridas para registrar, modificar, listar y borrar factores de riesgo del plan.
 * Por qué es importante (el impacto o problema que resuelve): Sin estas importaciones, no se podrían diagnosticar ni catalogar los peligros físicos (ej: taludes inestables o tanques de gas expuestos) asociados al hogar.
 */
import Modelo.DTO.FactorRiesgoDTO;
import Modelo.Servicios.Voluntario.FactorRiesgoServicio;
import Modelo.Utilidades.JSONUtil;
import Modelo.Utilidades.ResponseUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.json.JSONObject;

/*
 * Qué hace (la acción): Asocia el servlet FactoresRiesgoServlet con el endpoint de red "/api/factoresRiesgo/*" utilizando la anotación @WebServlet.
 * Qué significa (conceptos, métodos, tipos involucrados): Mapea la ruta para que Tomcat enrute la gestión de los factores de riesgo del hogar.
 * Para qué se usa (el propósito): Servir como el endpoint de la API para administrar los riesgos identificados en el plan de emergencia familiar.
 * Por qué es importante (el impacto o problema que resuelve): Permite registrar de forma asíncrona qué amenazas acechan a la vivienda (tanto internas de la estructura como externas del entorno) de manera segura y ordenada.
 */
@WebServlet("/api/factoresRiesgo/*")
public class FactoresRiesgoServlet extends HttpServlet {

    /*
     * Qué hace (la acción): Instancia de manera privada y constante la variable servicio de tipo FactorRiesgoServicio.
     * Qué significa (conceptos, métodos, tipos involucrados): Instancia de la clase de servicios de negocio para el módulo de riesgos del plan familiar.
     * Para qué se usa (el propósito): Invocar las funciones lógicas de administración de riesgos.
     */
    private final FactorRiesgoServicio servicio = new FactorRiesgoServicio();

    /*
     * Qué hace (la acción): Sobrescribe el método service para desviar las peticiones que utilizan el verbo HTTP PATCH hacia el método doPatch.
     * Qué significa (conceptos, métodos, tipos involucrados): Redirecciona el método PATCH de forma manual en Jakarta Servlet API.
     * Para qué se usa (el propósito): Habilitar la actualización parcial de factores de riesgo en el servidor.
     */
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

    /*
     * Qué hace (la acción): Sobrescribe el método doGet para listar todos los riesgos registrados (para auditoría general de supervisor si la ruta es raíz), listar por plan familiar en la subruta "/planFamiliar/{planId}" de manera paginada, o en "/planFamiliar/{planId}/seleccion" de manera plana sin paginar para dropdowns de asignación, o consultar un riesgo unitario.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - seleccion: Sub-segmento que indica la recuperación de la lista simplificada de riesgos para rellenar los dropdowns de las micro-acciones.
     *   - pageParam: Especifica la página actual de la tabla en el frontend.
     * Para qué se usa (el propósito): Alimentar la tabla de factores de riesgo y los selectores de planes de acción en la interfaz del voluntario o supervisor.
     * Por qué es importante (el impacto o problema que resuelve): Permite que el voluntario obtenga la lista exacta de riesgos de su vivienda de forma estructurada con control de errores de formato numérico.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
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
                    response.getWriter().write(ResponseUtil.error("ID de plan familiar no provisto."));
                    return;
                }
                int planId = Integer.parseInt(parts[2]);
                
                if (parts.length > 3 && parts[3].equals("seleccion")) {
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
            response.getWriter().write(ResponseUtil.error("El identificador debe ser numérico."));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(ResponseUtil.error("Error interno: " + e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doPost para crear y guardar un nuevo registro de factor de riesgo en la base de datos SQL.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - JSONUtil.leerJson(request): Parsea la petición asíncrona a un objeto JSON.
     *   - dto.setFamilyPlanId: Vincula el riesgo al plan familiar del voluntario.
     * Para qué se usa (el propósito): Insertar un nuevo factor de riesgo en la base de datos.
     * Por qué es importante (el impacto o problema que resuelve): Previene el registro de riesgos con datos nulos o inconsistentes en MySQL validando el cuerpo JSON antes de ejecutar la inserción en base de datos.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            JSONObject json = JSONUtil.leerJson(request);
            FactorRiesgoDTO dto = new FactorRiesgoDTO();
            dto.setThreatTypeId(json.getInt("threat_type_id"));
            dto.setDescription(json.getString("description"));
            dto.setUbication(json.getString("ubication"));
            dto.setDistance(json.optInt("distance", 0));
            dto.setFamilyPlanId(json.getInt("family_plan_id"));
            
            String resJson = servicio.crearFactor(dto);
            response.getWriter().write(resJson);
            
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("JSON mal formado: " + e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al crear factor de riesgo: " + e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doPatch para actualizar de manera parcial la amenaza, descripción, ubicación o distancia de un factor de riesgo existente por su ID.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - doPatch: Procesador de modificaciones parciales.
     *   - servicio.actualizarFactor(id, dto): Actualiza la fila en base de datos.
     * Para qué se usa (el propósito): Modificar datos del riesgo sin tener que eliminarlo y volverlo a crear.
     */
    protected void doPatch(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID no provisto."));
            return;
        }
        
        String[] parts = pathInfo.split("/");
        
        try {
            int id = Integer.parseInt(parts[1]);
            JSONObject json = JSONUtil.leerJson(request);
            FactorRiesgoDTO dto = new FactorRiesgoDTO();
            dto.setThreatTypeId(json.getInt("threat_type_id"));
            dto.setDescription(json.getString("description"));
            dto.setUbication(json.optString("location", json.optString("ubication", "")));
            dto.setDistance(json.optInt("distance", 0));
            
            String resJson = servicio.actualizarFactor(id, dto);
            response.getWriter().write(resJson);
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID debe ser numérico."));
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("JSON mal formado: " + e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al actualizar: " + e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doDelete para dar de baja física y eliminar un factor de riesgo por su ID en la base de datos SQL.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - servicio.eliminarFactor(id): Remueve la fila correspondiente en base de datos.
     * Para qué se usa (el propósito): Eliminar del plan de emergencia familiar un factor de riesgo de forma definitiva.
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID no provisto."));
            return;
        }
        
        String[] parts = pathInfo.split("/");
        
        try {
            int id = Integer.parseInt(parts[1]);
            String resJson = servicio.eliminarFactor(id);
            response.getWriter().write(resJson);
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID debe ser numérico."));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al eliminar: " + e.getMessage()));
        }
    }
}
