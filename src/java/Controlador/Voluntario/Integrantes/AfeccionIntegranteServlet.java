package Controlador.Voluntario.Integrantes;

/*
 * Qué hace (la acción): Importa la clase DTO de afecciones, la capa de servicio de integrantes, utilidades de JSON, respuestas web y APIs de Jakarta Servlet.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.DTO.AfeccionDTO: Objeto que encapsula la información de una condición de salud (enfermedad, alergia, discapacidad) y dosis de medicamentos.
 *   - Modelo.Servicios.Voluntario.IntegranteServicio: Servicio de negocio que realiza operaciones CRUD en base de datos para los familiares.
 * Para qué se usa (el propósito): Proveer las herramientas de red y negocio necesarias para gestionar el estado de salud de cada integrante.
 * Por qué es importante (el impacto o problema que resuelve): Sin estas importaciones, no se podrían asociar medicamentos, enfermedades o dosis médicas a los miembros de la familia.
 */
import Modelo.DTO.AfeccionDTO;
import Modelo.Servicios.Voluntario.IntegranteServicio;
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
 * Qué hace (la acción): Asocia el servlet AfeccionIntegranteServlet con el patrón de URL "/api/conditionMembers/*" mediante la anotación @WebServlet.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - @WebServlet: Registra de manera declarativa este servlet en Tomcat.
 * Para qué se usa (el propósito): Servir como el endpoint de la API para administrar las afecciones de salud y tratamientos del grupo familiar.
 * Por qué es importante (el impacto o problema que resuelve): Permite registrar de forma granular si algún miembro de la familia requiere cuidados especiales, medicamentos continuos o tiene alergias, lo cual es vital durante una evacuación de emergencia.
 */
@WebServlet("/api/conditionMembers/*")
public class AfeccionIntegranteServlet extends HttpServlet {

    /*
     * Qué hace (la acción): Instancia de manera privada y constante la variable servicio de tipo IntegranteServicio.
     * Qué significa (conceptos, métodos, tipos involucrados): Instancia de la clase de servicios de negocio para los integrantes.
     * Para qué se usa (el propósito): Invocar los métodos lógicos para administrar el censo de salud.
     */
    private final IntegranteServicio servicio = new IntegranteServicio();

    /*
     * Qué hace (la acción): Sobrescribe el método doGet para listar las afecciones médicas asociadas a un integrante en particular en la subruta "/member/{memberId}", o para consultar el detalle de una afección individual por su ID.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - parts[1].equals("member"): Identifica que se solicita listar todas las condiciones de salud de un familiar.
     *   - parts[1]: ID numérico directo que representa la consulta unitaria de una condición médica.
     * Para qué se usa (el propósito): Mostrar las enfermedades y medicamentos asignados a cada integrante en los formularios del plan familiar.
     * Por qué es importante (el impacto o problema que resuelve): Permite que el voluntario visualice de manera inmediata las necesidades de salud de su familia facilitando el control y prevención ante emergencias.
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
            if (parts[1].equals("member")) {
                if (parts.length < 3) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write(ResponseUtil.error("ID de integrante no provisto."));
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
            response.getWriter().write(ResponseUtil.error("El identificador debe ser numérico."));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(ResponseUtil.error("Error del servidor: " + e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doPost para crear y guardar un nuevo registro de afección médica en la base de datos SQL.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - JSONUtil.leerJson(request): Parsea la petición asíncrona a un objeto JSON.
     *   - dto.setDose: Especifica la dosis de medicamento de forma opcional.
     * Para qué se usa (el propósito): Vincular una enfermedad, alergia o discapacidad con su respectivo integrante.
     * Por qué es importante (el impacto o problema que resuelve): Previene el registro de datos nulos o inconsistentes en MySQL validando el cuerpo JSON antes de ejecutar la inserción en base de datos.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            JSONObject json = JSONUtil.leerJson(request);
            AfeccionDTO dto = new AfeccionDTO();
            dto.setMemberId(json.getInt("member_id"));
            dto.setConditionTypeId(json.getInt("condition_type_id"));
            dto.setName(json.getString("name"));
            dto.setDose(json.optString("dose", null));
            
            String resJson = servicio.crearAfeccion(dto);
            response.getWriter().write(resJson);
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("JSON mal formado: " + e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al registrar afección: " + e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doPut para actualizar de manera parcial el nombre de la enfermedad, tipo de afección o dosis de medicamentos de una condición de salud existente por su ID.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - doPut: Procesador estándar de modificaciones completas de recursos.
     *   - servicio.actualizarAfeccion(id, dto): Actualiza la fila en base de datos.
     * Para qué se usa (el propósito): Modificar datos médicos del integrante sin tener que eliminarlos y recrearlos.
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID de afección no provisto."));
            return;
        }
        
        String[] parts = pathInfo.split("/");
        
        try {
            int id = Integer.parseInt(parts[1]);
            JSONObject json = JSONUtil.leerJson(request);
            AfeccionDTO dto = new AfeccionDTO();
            dto.setMemberId(json.getInt("member_id"));
            dto.setConditionTypeId(json.getInt("condition_type_id"));
            dto.setName(json.getString("name"));
            dto.setDose(json.optString("dose", null));
            
            String resJson = servicio.actualizarAfeccion(id, dto);
            response.getWriter().write(resJson);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID de afección debe ser numérico."));
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("JSON mal formado: " + e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al actualizar afección: " + e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doDelete para dar de baja física y eliminar un registro de afección médica por su ID en la base de datos SQL.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - servicio.eliminarAfeccion(id): Remueve la fila correspondiente en base de datos.
     * Para qué se usa (el propósito): Eliminar del censo médico una afección errónea o que ya fue superada por el integrante.
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID de afección no provisto."));
            return;
        }
        
        String[] parts = pathInfo.split("/");
        
        try {
            int id = Integer.parseInt(parts[1]);
            String resJson = servicio.eliminarAfeccion(id);
            response.getWriter().write(resJson);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID de afección debe ser numérico."));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al eliminar afección: " + e.getMessage()));
        }
    }
}
