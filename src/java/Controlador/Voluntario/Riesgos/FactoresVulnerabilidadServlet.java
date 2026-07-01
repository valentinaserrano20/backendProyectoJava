package Controlador.Voluntario.Riesgos;

/*
 * Qué hace (la acción): Importa la clase DTO de factores de vulnerabilidad, la capa de servicio de factores de riesgo, utilidades de JSON, respuestas web y APIs estándares de servlets de Jakarta.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.DTO.FactorVulnerabilidadDTO: Clase que transporta la vulnerabilidad asociada a un riesgo (ej. material inflamable) y su nivel de gravedad (Muy Alta, Alta, Media, Baja).
 *   - Modelo.Servicios.Voluntario.FactorRiesgoServicio: Servicio de negocio que gestiona las operaciones en base de datos para la mitigación preventiva de factores de riesgo y vulnerabilidad.
 * Para qué se usa (el propósito): Proveer al servlet de las dependencias requeridas para registrar, modificar, listar y borrar vulnerabilidades asociadas a factores de riesgo.
 * Por qué es importante (el impacto o problema que resuelve): Sin estas importaciones, no se podrían diagnosticar las vulnerabilidades físicas de la vivienda y asociarles un grado de severidad.
 */
import Modelo.DTO.FactorVulnerabilidadDTO;
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
 * Qué hace (la acción): Asocia el servlet FactoresVulnerabilidadServlet con el endpoint de red "/api/factoresVulnerabilidad/*" utilizando la anotación @WebServlet.
 * Qué significa (conceptos, métodos, tipos involucrados): Mapea la ruta para que Tomcat enrute la gestión de las vulnerabilidades físicas de la vivienda.
 * Para qué se usa (el propósito): Servir como el endpoint de la API para administrar las vulnerabilidades específicas vinculadas a factores de riesgo en el plan de emergencia familiar.
 * Por qué es importante (el impacto o problema que resuelve): Permite registrar de forma asíncrona qué tipo de debilidades físicas tiene la vivienda y qué tan grave es la amenaza para coordinar de forma segura el avance del plan de emergencia.
 */
@WebServlet("/api/factoresVulnerabilidad/*")
public class FactoresVulnerabilidadServlet extends HttpServlet {

    /*
     * Qué hace (la acción): Instancia de manera privada y constante la variable servicio de tipo FactorRiesgoServicio.
     * Qué significa (conceptos, métodos, tipos involucrados): Instancia de la clase de servicios de negocio para el censo de riesgos.
     * Para qué se usa (el propósito): Invocar las funciones lógicas de administración de vulnerabilidades asociadas.
     */
    private final FactorRiesgoServicio servicio = new FactorRiesgoServicio();

    /*
     * Qué hace (la acción): Sobrescribe el método service para desviar las peticiones que utilizan el verbo HTTP PATCH hacia el método doPatch.
     * Qué significa (conceptos, métodos, tipos involucrados): Redirecciona el método PATCH de forma manual en Jakarta Servlet API.
     * Para qué se usa (el propósito): Habilitar la actualización parcial de vulnerabilidades en el servidor.
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
     * Qué hace (la acción): Sobrescribe el método doGet para listar las vulnerabilidades asociadas a un factor de riesgo en la subruta "/factorRiesgo/{riesgoId}", o consultar el detalle de una vulnerabilidad asociada individual por su ID.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - parts[1].equals("factorRiesgo"): Detecta si se solicita listar todas las vulnerabilidades asociadas a un riesgo específico de la casa.
     *   - parts[1]: ID numérico directo que representa la consulta de una vulnerabilidad asociada.
     * Para qué se usa (el propósito): Mostrar las vulnerabilidades físicas en la interfaz del voluntario.
     * Por qué es importante (el impacto o problema que resuelve): Permite que el voluntario obtenga la lista exacta de vulnerabilidades y sus niveles de gravedad para priorizar la evacuación.
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
                String resJson = servicio.listarVulnerabilidades(riesgoId);
                response.getWriter().write(resJson);
            } else {
                int id = Integer.parseInt(parts[1]);
                String resJson = servicio.obtenerVulnerabilidad(id);
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
     * Qué hace (la acción): Sobrescribe el método doPost para crear y guardar un nuevo registro de vulnerabilidad asociada en la base de datos SQL.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - JSONUtil.leerJson(request): Parsea la petición asíncrona a un objeto JSON.
     *   - dto.setRiskFactorId: Vincula la vulnerabilidad al factor de riesgo.
     * Para qué se usa (el propósito): Vincular una vulnerabilidad y su grado de gravedad a un factor de riesgo en la base de datos.
     * Por qué es importante (el impacto o problema que resuelve): Previene el registro de datos nulos o inconsistentes en MySQL validando el cuerpo JSON antes de ejecutar la inserción en base de datos.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            JSONObject json = JSONUtil.leerJson(request);
            FactorVulnerabilidadDTO dto = new FactorVulnerabilidadDTO();
            dto.setVulnerabilityId(json.getInt("vulnerability_id"));
            dto.setVulnerabilityGradeId(json.getInt("vulnerability_grade_id"));
            dto.setRiskFactorId(json.getInt("risk_factor_id"));
            
            String resJson = servicio.crearVulnerabilidad(dto);
            response.getWriter().write(resJson);
            
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("JSON mal formado: " + e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al crear vulnerabilidad asociada: " + e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doPatch para actualizar de manera parcial el tipo de vulnerabilidad o el grado de gravedad de un registro existente por su ID.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - doPatch: Procesador de modificaciones parciales.
     *   - servicio.actualizarVulnerabilidad(id, dto): Actualiza la fila en base de datos.
     * Para qué se usa (el propósito): Modificar datos de la vulnerabilidad asociada sin tener que eliminarla y volverla a crear.
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
            FactorVulnerabilidadDTO dto = new FactorVulnerabilidadDTO();
            dto.setVulnerabilityId(json.getInt("vulnerability_id"));
            dto.setVulnerabilityGradeId(json.getInt("vulnerability_grade_id"));
            
            String resJson = servicio.actualizarVulnerabilidad(id, dto);
            response.getWriter().write(resJson);
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID debe ser numérico."));
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("JSON mal formado: " + e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al actualizar la vulnerabilidad: " + e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doDelete para dar de baja física y eliminar un registro de vulnerabilidad asociada por su ID en la base de datos SQL.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - servicio.eliminarVulnerabilidad(id): Remueve la fila correspondiente en base de datos.
     * Para qué se usa (el propósito): Eliminar del plan de emergencia familiar una vulnerabilidad asociada de forma definitiva.
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
            String resJson = servicio.eliminarVulnerabilidad(id);
            response.getWriter().write(resJson);
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID debe ser numérico."));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al eliminar la vulnerabilidad: " + e.getMessage()));
        }
    }
}
