package Controlador.Voluntario.Riesgos;

/*
 * Qué hace (la acción): Importa la clase DTO de acciones de reducción, la capa de servicio de factores de riesgo, utilidades de JSON, respuestas web y APIs estándares de servlets de Jakarta.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.DTO.AccionReduccionDTO: Clase que transporta la descripción de la acción preventiva, fecha de término y miembro familiar responsable.
 *   - Modelo.Servicios.Voluntario.FactorRiesgoServicio: Servicio de negocio que realiza operaciones CRUD en base de datos para la mitigación preventiva de factores de riesgo.
 * Para qué se usa (el propósito): Proveer las dependencias de red y negocio necesarias para gestionar las tareas de reducción de riesgos de la familia.
 * Por qué es importante (el impacto o problema que resuelve): Sin estas importaciones, no se podrían planificar ni registrar medidas preventivas (ej: asegurar estanterías contra sismos) vinculadas al plan de evacuación familiar.
 */
import Modelo.DTO.AccionReduccionDTO;
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
 * Qué hace (la acción): Asocia el servlet AccionesReduccionServlet con el endpoint de red "/api/accionesReduccion/*" utilizando la anotación @WebServlet.
 * Qué significa (conceptos, métodos, tipos involucrados): Mapea la ruta para que Tomcat enrute la gestión de las tareas preventivas del hogar.
 * Para qué se usa (el propósito): Servir como el endpoint de la API para administrar las acciones destinadas a mitigar riesgos físicos o estructurales identificados en la vivienda.
 * Por qué es importante (el impacto o problema que resuelve): Permite registrar de forma asíncrona qué miembro de la familia realizará una acción de prevención y en qué fecha límite, organizando de forma segura el avance del plan de emergencia.
 */
@WebServlet("/api/accionesReduccion/*")
public class AccionesReduccionServlet extends HttpServlet {

    /*
     * Qué hace (la acción): Instancia de manera privada y constante la variable servicio de tipo FactorRiesgoServicio.
     * Qué significa (conceptos, métodos, tipos involucrados): Instancia de la clase de servicios de negocio para el módulo de riesgos del plan familiar.
     * Para qué se usa (el propósito): Invocar las funciones lógicas de administración de acciones de mitigación.
     */
    private final FactorRiesgoServicio servicio = new FactorRiesgoServicio();

    /*
     * Qué hace (la acción): Sobrescribe el método service para desviar las peticiones que utilizan el verbo HTTP PATCH hacia el método doPatch.
     * Qué significa (conceptos, métodos, tipos involucrados): Redirecciona el método PATCH de forma manual en Jakarta Servlet API.
     * Para qué se usa (el propósito): Habilitar la actualización parcial de acciones en el servidor.
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
     * Qué hace (la acción): Sobrescribe el método doGet para listar las acciones preventivas asociadas a un factor de riesgo en particular en la subruta "/factorRiesgo/{riesgoId}", o consultar el detalle de una acción individual por su ID.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - parts[1].equals("factorRiesgo"): Detecta si se solicita listar todas las tareas asociadas a un riesgo específico de la casa.
     *   - parts[1]: ID numérico directo que representa la consulta unitaria de una acción de reducción.
     * Para qué se usa (el propósito): Mostrar las acciones de reducción y sus responsables familiares en los formularios del plan.
     * Por qué es importante (el impacto o problema que resuelve): Permite que el voluntario visualice de manera inmediata las tareas preventivas programadas en el hogar.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Recurso no especificado."));
            return;
        }
        
        String[] parts = pathInfo.split("/");
        
        try {
            if (parts[1].equals("factorRiesgo")) {
                if (parts.length < 3) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write(ResponseUtil.error("ID de factor de riesgo no provisto."));
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
            response.getWriter().write(ResponseUtil.error("El identificador debe ser numérico."));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(ResponseUtil.error("Error interno: " + e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doPost para crear y guardar un nuevo registro de acción de reducción en la base de datos SQL.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - JSONUtil.leerJson(request): Parsea la petición asíncrona a un objeto JSON.
     *   - dto.setMemberId: Setea opcionalmente el ID del miembro de la familia responsable de ejecutar la tarea.
     * Para qué se usa (el propósito): Vincular una tarea preventiva a un factor de riesgo en la base de datos de manera persistente.
     * Por qué es importante (el impacto o problema que resuelve): Previene el registro de acciones con datos nulos o inconsistentes en MySQL validando el cuerpo JSON antes de ejecutar la inserción en base de datos.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            JSONObject json = JSONUtil.leerJson(request);
            AccionReduccionDTO dto = new AccionReduccionDTO();
            dto.setAction(json.getString("action"));
            dto.setEndDate(json.getString("end_date"));
            dto.setRiskFactorId(json.getInt("risk_factor_id"));
            dto.setMemberId(json.optInt("member_id", 0));
            
            String resJson = servicio.crearAccion(dto);
            response.getWriter().write(resJson);
            
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("JSON mal formado: " + e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al crear acción de reducción: " + e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doPatch para actualizar de manera parcial la descripción de la acción, la fecha límite o el miembro familiar responsable de una tarea de mitigación existente por su ID.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - doPatch: Procesador de modificaciones parciales.
     *   - servicio.actualizarAccion(id, dto): Actualiza la fila en base de datos.
     * Para qué se usa (el propósito): Modificar datos preventivos sin tener que eliminarlos y volverlos a registrar.
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
            AccionReduccionDTO dto = new AccionReduccionDTO();
            dto.setAction(json.getString("action"));
            dto.setEndDate(json.getString("end_date"));
            dto.setMemberId(json.optInt("member_id", 0));
            
            String resJson = servicio.actualizarAccion(id, dto);
            response.getWriter().write(resJson);
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID debe ser numérico."));
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("JSON mal formado: " + e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al actualizar la acción: " + e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doDelete para dar de baja física y eliminar un registro de acción de reducción por su ID en la base de datos SQL.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - servicio.eliminarAccion(id): Remueve la fila correspondiente en base de datos.
     * Para qué se usa (el propósito): Eliminar del plan de emergencia familiar una medida preventiva obsoleta.
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
            String resJson = servicio.eliminarAccion(id);
            response.getWriter().write(resJson);
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID debe ser numérico."));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al eliminar la acción: " + e.getMessage()));
        }
    }
}
