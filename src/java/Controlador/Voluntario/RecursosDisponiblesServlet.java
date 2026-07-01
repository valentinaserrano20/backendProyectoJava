package Controlador.Voluntario;

/*
 * Qué hace (la acción): Importa la clase DTO de recursos disponibles, el servicio de lógica de negocio, utilidades de lectura de JSON y estructuración de respuestas web, y las APIs del servlet de Jakarta.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.DTO.RecursoDisponibleDTO: Clase de transferencia que representa los datos de contacto y distancia de un recurso (ej: centros médicos, bomberos o kits en el hogar).
 *   - Modelo.Servicios.Voluntario.RecursoDisponibleServicio: Capa de servicios que procesa la lógica CRUD de recursos para el plan familiar.
 * Para qué se usa (el propósito): Proveer al servlet de los componentes lógicos indispensables para gestionar el inventario y directorio de recursos del plan.
 * Por qué es importante (el impacto o problema que resuelve): Sin estas importaciones, no se podría registrar qué recursos de auxilio tiene a mano la familia ante emergencias.
 */
import Modelo.DTO.RecursoDisponibleDTO;
import Modelo.Servicios.Voluntario.RecursoDisponibleServicio;
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
 * Qué hace (la acción): Asocia el servlet RecursosDisponiblesServlet con el patrón de URL "/api/recursosDisponibles/*" mediante @WebServlet.
 * Qué significa (conceptos, métodos, tipos involucrados): Registra el enrutador REST para que capture todas las peticiones CRUD de recursos externos e internos del plan de contingencia.
 * Para qué se usa (el propósito): Exponer endpoints para dar de alta, consultar, modificar o eliminar recursos vinculados al plan de emergencia de la vivienda.
 * Por qué es importante (el impacto o problema que resuelve): Permite que el voluntario registre qué medios o apoyo externo e interno tiene para responder a una catástrofe en su hogar.
 */
@WebServlet("/api/recursosDisponibles/*")
public class RecursosDisponiblesServlet extends HttpServlet {

    /*
     * Qué hace (la acción): Instancia de manera privada y constante la variable servicio de tipo RecursoDisponibleServicio.
     * Qué significa (conceptos, métodos, tipos involucrados): Instancia de la clase de servicios de negocio para recursos.
     * Para qué se usa (el propósito): Invocar las funciones lógicas de administración de recursos.
     */
    private final RecursoDisponibleServicio servicio = new RecursoDisponibleServicio();

    /*
     * Qué hace (la acción): Sobrescribe el método service para canalizar las peticiones HTTP PATCH hacia el método doPatch.
     * Qué significa (conceptos, métodos, tipos involucrados): Redirecciona el método de red PATCH en Jakarta Servlet API.
     * Para qué se usa (el propósito): Habilitar la edición parcial de recursos en el servidor.
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
     * Qué hace (la acción): Sobrescribe el método doGet para listar los recursos disponibles registrados en un plan familiar a través de la subruta "/planFamiliar/{planId}" de manera paginada, o retornar la información puntual de un único recurso.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - parts[1].equals("planFamiliar"): Detecta si se solicita listar todos los recursos de un plan familiar específico.
     *   - parts[1]:ID numérico que indica la consulta unitaria de un recurso.
     * Para qué se usa (el propósito): Cargar el listado de teléfonos de ayuda y recursos en la interfaz del voluntario.
     * Por qué es importante (el impacto o problema que resuelve): Posibilita mostrar de forma organizada qué recursos de respuesta a desastres están disponibles a corta distancia de la vivienda.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Ruta o ID no especificado."));
            return;
        }

        String[] parts = pathInfo.split("/");
        
        try {
            // Caso 1: recursosDisponibles/planFamiliar/{planId}
            if (parts[1].equals("planFamiliar")) {
                if (parts.length < 3) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write(ResponseUtil.error("ID de plan familiar no provisto."));
                    return;
                }
                int planId = Integer.parseInt(parts[2]);
                String pageParam = request.getParameter("page");
                int page = (pageParam != null) ? Integer.parseInt(pageParam) : 1;
                
                String jsonRes = servicio.listarRecursos(planId, page);
                response.getWriter().write(jsonRes);
            } 
            // Caso 2: recursosDisponibles/{id}
            else {
                int id = Integer.parseInt(parts[1]);
                String jsonRes = servicio.obtenerRecurso(id);
                response.getWriter().write(jsonRes);
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("El identificador de ruta debe ser numérico."));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al procesar consulta: " + e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doPost para recibir el JSON de creación de un recurso (tipo, ubicación, distancia, teléfono y descripción) y guardarlo en la base de datos SQL.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - JSONUtil.leerJson(request): Parsea la petición asíncrona a un objeto JSON.
     *   - dto.setPlaceName: Setea el nombre o ubicación del recurso.
     * Para qué se usa (el propósito): Crear y registrar un nuevo recurso en el inventario del plan familiar.
     * Por qué es importante (el impacto o problema que resuelve): Previene el registro de datos nulos o inconsistentes en MySQL validando el cuerpo JSON antes de ejecutar la inserción en base de datos.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        try {
            JSONObject json = JSONUtil.leerJson(request);
            RecursoDisponibleDTO dto = new RecursoDisponibleDTO();
            dto.setPlaceName(json.getString("location")); 
            dto.setDistance(json.getInt("distance"));
            dto.setPhone(json.optString("phone", null));
            dto.setDescription(json.optString("description", null));
            dto.setPlanId(json.getInt("family_plan_id")); 
            dto.setResourceTypeId(json.getInt("resource_id")); 

            String jsonRes = servicio.crearRecurso(dto);
            response.getWriter().write(jsonRes);
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("JSON mal formado: " + e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al registrar recurso: " + e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Define la lógica doPatch para actualizar de manera parcial los datos de ubicación, distancia, tipo, teléfono y descripción de un recurso existente por su ID.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - doPatch: Procesador de actualizaciones parciales.
     *   - servicio.actualizarRecurso(id, dto): Actualiza en base de datos el recurso seleccionado.
     * Para qué se usa (el propósito): Modificar datos del recurso sin tener que eliminarlo y volverlo a crear.
     */
    protected void doPatch(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID del recurso no provisto."));
            return;
        }

        String[] parts = pathInfo.split("/");
        
        try {
            int id = Integer.parseInt(parts[1]);
            JSONObject json = JSONUtil.leerJson(request);
            RecursoDisponibleDTO dto = new RecursoDisponibleDTO();
            dto.setPlaceName(json.getString("location")); 
            dto.setDistance(json.getInt("distance"));
            dto.setPhone(json.optString("phone", null));
            dto.setDescription(json.optString("description", null));
            dto.setResourceTypeId(json.getInt("resource_id")); 

            String jsonRes = servicio.actualizarRecurso(id, dto);
            response.getWriter().write(jsonRes);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID de recurso debe ser numérico."));
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("JSON mal formado: " + e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al actualizar recurso: " + e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doDelete para dar de baja física y eliminar un recurso a partir de su ID en la base de datos SQL.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - servicio.eliminarRecurso(id): Ejecuta la eliminación física del recurso de la tabla en base de datos.
     * Para qué se usa (el propósito): Permitir al voluntario remover un recurso obsoleto o erróneo de su plan.
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID de recurso no provisto."));
            return;
        }

        String[] parts = pathInfo.split("/");
        
        try {
            int id = Integer.parseInt(parts[1]);
            String jsonRes = servicio.eliminarRecurso(id);
            response.getWriter().write(jsonRes);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID de recurso debe ser numérico."));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al eliminar recurso: " + e.getMessage()));
        }
    }
}
