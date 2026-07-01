package Controlador.Voluntario.TestVulnerabilidad;

/*
 * Qué hace (la acción): Importa la clase DTO de respuestas del test, la capa de servicio de vulnerabilidades, utilidades de JSON, respuestas web y APIs estándares de servlets de Jakarta.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.DTO.RespuestaTestDTO: DTO que transporta los datos de cada respuesta (ID de la pregunta y el valor booleano respondido).
 *   - Modelo.Servicios.Voluntario.VulnerabilidadServicio: Servicio que contiene la lógica de negocio para procesar, calificar y guardar el test.
 * Para qué se usa (el propósito): Proveer al servlet de las dependencias requeridas para registrar, procesar y guardar las respuestas del cuestionario de vulnerabilidad.
 * Por qué es importante (el impacto o problema que resuelve): Sin estas importaciones, no se podrían recuperar ni calificar las respuestas del test de vulnerabilidad de la familia.
 */
import Modelo.DTO.RespuestaTestDTO;
import Modelo.Servicios.Voluntario.VulnerabilidadServicio;
import Modelo.Utilidades.JSONUtil;
import Modelo.Utilidades.ResponseUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/*
 * Qué hace (la acción): Asocia el servlet TestVulnerabilidadServlet con el endpoint "/api/vulnerableTest" utilizando la anotación @WebServlet.
 * Qué significa (conceptos, métodos, tipos involucrados): Mapea la ruta para que Tomcat procese el test de vulnerabilidad.
 * Para qué se usa (el propósito): Servir como el endpoint de red para registrar y evaluar las respuestas del cuestionario de vulnerabilidad.
 * Por qué es importante (el impacto o problema que resuelve): Permite guardar y calificar el test en base de datos, determinando de forma ponderada el grado de vulnerabilidad del hogar frente a amenazas.
 */
@WebServlet("/api/vulnerableTest")
public class TestVulnerabilidadServlet extends HttpServlet {

    /*
     * Qué hace (la acción): Instancia de manera privada y constante la variable servicio de tipo VulnerabilidadServicio.
     * Qué significa (conceptos, métodos, tipos involucrados): Instancia de la clase de servicios de negocio para vulnerabilidad.
     * Para qué se usa (el propósito): Invocar las funciones lógicas de guardado y cálculo del test.
     */
    private final VulnerabilidadServicio servicio = new VulnerabilidadServicio();

    /*
     * Qué hace (la acción): Sobrescribe el método doGet para obtener las respuestas previas guardadas del plan familiar a partir de su ID de plan ("family_plan_id") enviado como parámetro de consulta.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - request.getParameter("family_plan_id"): Recupera la clave del plan familiar del query string de la petición HTTP.
     *   - servicio.obtenerRespuestasPlan(planId): Consulta y retorna un objeto JSON con las respuestas previas.
     * Para qué se usa (el propósito): Precargar las respuestas contestadas por la familia si ya se había iniciado el cuestionario antes.
     * Por qué es importante (el impacto o problema que resuelve): Permite que el voluntario no tenga que contestar de nuevo todo el test si sale del formulario y vuelve a ingresar.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String planIdStr = request.getParameter("family_plan_id");
        if (planIdStr == null || planIdStr.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Falta el parámetro family_plan_id."));
            return;
        }

        try {
            int planId = Integer.parseInt(planIdStr);
            String json = servicio.obtenerRespuestasPlan(planId);
            response.getWriter().write(json);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("family_plan_id debe ser un entero válido."));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(ResponseUtil.error("Error del servidor: " + e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doPost para recibir el lote completo de respuestas, parsearlo a una lista de DTOs, calificar la vulnerabilidad general del hogar de forma ponderada y guardar todo en base de datos.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - JSONUtil.leerJson(request): Parsea la petición asíncrona a un objeto JSON.
     *   - answersArray: Arreglo JSON conteniendo cada pregunta y su respuesta booleana.
     *   - servicio.procesarGuardadoLote(planId, respuestas): Guarda las respuestas y calcula el grado de vulnerabilidad final (1, 2, 3 o 4) en la tabla del plan familiar.
     * Para qué se usa (el propósito): Evaluar y calificar el nivel de vulnerabilidad de la familia de forma masiva tras completar el test en la interfaz.
     * Por qué es importante (el impacto o problema que resuelve): Previene la inconsistencia de datos al realizar la transacción de guardado y cálculo del test en un único bloque de lote transaccional.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            JSONObject json = JSONUtil.leerJson(request);

            int planId = json.getInt("family_plan_id");
            JSONArray answersArray = json.getJSONArray("answers");

            List<RespuestaTestDTO> respuestas = new ArrayList<>();
            for (int i = 0; i < answersArray.length(); i++) {
                JSONObject item = answersArray.getJSONObject(i);
                RespuestaTestDTO dto = new RespuestaTestDTO(
                    item.getInt("vulnerable_question_id"),
                    item.getBoolean("answer")
                );
                respuestas.add(dto);
            }

            String respuestaJson = servicio.procesarGuardadoLote(planId, respuestas);
            response.getWriter().write(respuestaJson);

        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("JSON mal formado o inválido: " + e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(ResponseUtil.error("Error al procesar lote de respuestas: " + e.getMessage()));
        }
    }
}
