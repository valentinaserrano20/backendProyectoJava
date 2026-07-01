package Controlador.Voluntario;

/*
 * Qué hace (la acción): Importa el DTO de plan de acción, el servicio de lógica de negocio del plan de acción, las utilidades de JSON y respuestas web, y las APIs estándares de servlets de Jakarta.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.DTO.ActionPlanDTO: Contenedor que transporta los datos generales del Plan de Acción familiar (coordinador del plan y factor de riesgo principal).
 *   - Modelo.Servicios.Voluntario.ActionPlanServicio: Servicio de negocio que encapsula la persistencia, validaciones de existencia y armado de JSON para el plan de acción.
 * Para qué se usa (el propósito): Proveer al servlet de las dependencias requeridas para registrar, modificar y consultar el plan de acción familiar general.
 * Por qué es importante (el impacto o problema que resuelve): Sin estas importaciones, no se podría interactuar con el backend de negocio para estructurar las respuestas web JSON.
 */
import Modelo.DTO.ActionPlanDTO;
import Modelo.Servicios.Voluntario.ActionPlanServicio;
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
 * Qué hace (la acción): Asocia el servlet ActionPlansServlet con el endpoint de red "/api/actionPlans/*" a través de la anotación @WebServlet.
 * Qué significa (conceptos, métodos, tipos involucrados): @WebServlet es la anotación declarativa de Jakarta para mapear el servlet a la URL correspondiente.
 * Para qué se usa (el propósito): Servir como el endpoint central para la creación, verificación y actualización del Plan de Acción general de las familias.
 * Por qué es importante (el impacto o problema que resuelve): Permite registrar quién es el coordinador del plan familiar (ej. el padre de familia) y qué factor de riesgo principal se está mitigando en el hogar.
 */
@WebServlet("/api/actionPlans/*")
public class ActionPlansServlet extends HttpServlet {

    /*
     * Qué hace (la acción): Instancia de manera privada y constante la variable servicio de tipo ActionPlanServicio.
     * Qué significa (conceptos, métodos, tipos involucrados): Instancia de la clase de servicios de negocio para el Plan de Acción.
     * Para qué se usa (el propósito): Invocar los métodos lógicos para administrar el plan familiar.
     */
    private final ActionPlanServicio servicio = new ActionPlanServicio();

    /*
     * Qué hace (la acción): Sobrescribe el método doGet para verificar la existencia del plan de acción familiar en el sub-endpoint "/familyPlan/boolean/{planId}", o para recuperar su detalle general en "/familyPlan/{planId}".
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - parts[1].equals("familyPlan") && parts[2].equals("boolean"): Valida si la petición es para comprobar la existencia lógica del plan (retorna true o false).
     *   - parts[1].equals("familyPlan") && parts.length == 3: Valida si la petición pide retornar el objeto JSON del plan de acción.
     * Para qué se usa (el propósito): Determinar en la UI del voluntario si ya existe un plan de acción para pintar los controles de edición o si se debe crear uno nuevo.
     * Por qué es importante (el impacto o problema que resuelve): Permite al frontend reaccionar de forma dinámica adaptando la interfaz según el estado de avance del plan de emergencia familiar del voluntario.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Ruta no especificada."));
            return;
        }

        String[] parts = pathInfo.split("/");
        
        try {
            // Caso 1: actionPlans/familyPlan/boolean/{planId}
            if (parts[1].equals("familyPlan") && parts.length > 2 && parts[2].equals("boolean")) {
                int planId = Integer.parseInt(parts[3]);
                String jsonRes = servicio.verificarExistePlan(planId);
                response.getWriter().write(jsonRes);
            } 
            // Caso 2: actionPlans/familyPlan/{planId}
            else if (parts[1].equals("familyPlan") && parts.length == 3) {
                int planId = Integer.parseInt(parts[2]);
                String jsonRes = servicio.obtenerPlan(planId);
                response.getWriter().write(jsonRes);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(ResponseUtil.error("Ruta de consulta no válida."));
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
     * Qué hace (la acción): Sobrescribe el método doPost para inicializar y guardar un nuevo registro de Plan de Acción familiar general en la base de datos SQL.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - JSONUtil.leerJson(request): Parsea la petición asíncrona a un objeto JSON.
     *   - dto.setFamilyPlanId: Vincula el plan de acción al plan familiar global del voluntario.
     * Para qué se usa (el propósito): Crear el registro base del plan de acción estableciendo el integrante coordinador y el factor de riesgo priorizado.
     * Por qué es importante (el impacto o problema que resuelve): Previene el registro de planes huérfanos o con datos nulos validando el formato estructurado JSON antes de insertar en MySQL.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        try {
            JSONObject json = JSONUtil.leerJson(request);
            ActionPlanDTO dto = new ActionPlanDTO();
            dto.setMemberId(json.getInt("member_id"));
            dto.setRiskFactorId(json.getInt("risk_factor_id"));
            dto.setFamilyPlanId(json.getInt("family_plan_id")); 

            String jsonRes = servicio.crearPlan(dto);
            response.getWriter().write(jsonRes);
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("JSON mal formado: " + e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al registrar plan de acción: " + e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doPut para actualizar el miembro coordinador o el factor de riesgo asignado a un plan de acción familiar existente en base de datos.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - doPut: Procesador estándar de modificaciones completas de recursos.
     *   - servicio.actualizarPlan(planId, dto): Modifica la fila en SQL.
     * Para qué se usa (el propósito): Permitir al voluntario corregir o cambiar el coordinador familiar del plan o el factor de riesgo principal desde la interfaz.
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID del plan de acción no provisto."));
            return;
        }

        String[] parts = pathInfo.split("/");
        
        try {
            int planId = Integer.parseInt(parts[1]); 
            JSONObject json = JSONUtil.leerJson(request);
            ActionPlanDTO dto = new ActionPlanDTO();
            dto.setMemberId(json.getInt("member_id"));
            dto.setRiskFactorId(json.getInt("risk_factor_id"));

            String jsonRes = servicio.actualizarPlan(planId, dto);
            response.getWriter().write(jsonRes);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID de plan de acción debe ser numérico."));
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("JSON mal formado: " + e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al actualizar plan de acción: " + e.getMessage()));
        }
    }
}
