package Controlador.Voluntario.TestVulnerabilidad;

/*
 * Qué hace (la acción): Importa la capa de servicios de vulnerabilidad, utilidad de respuestas web y APIs estándares de servlets de Jakarta.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.Servicios.Voluntario.VulnerabilidadServicio: Servicio de negocio que realiza las consultas JDBC en MySQL para las preguntas y opciones del test de vulnerabilidad.
 *   - Modelo.Utilidades.ResponseUtil: Clase de utilidad para dar formato JSON estructurado a las respuestas.
 * Para qué se usa (el propósito): Proveer al servlet las dependencias requeridas para consultar el banco de preguntas del test.
 * Por qué es importante (el impacto o problema que resuelve): Sin estas importaciones, no se podrían recuperar las preguntas del censo de vulnerabilidad para enviárselas al cliente.
 */
import Modelo.Servicios.Voluntario.VulnerabilidadServicio;
import Modelo.Utilidades.ResponseUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/*
 * Qué hace (la acción): Asocia el servlet PreguntasVulnerabilidadServlet con los endpoints "/api/vulnerableQuestions" y "/api/vulnerableQuestions/paginate" mediante @WebServlet.
 * Qué significa (conceptos, métodos, tipos involucrados): Registra el servlet ante Tomcat permitiendo responder en ambos patrones de URL geográficos.
 * Para qué se usa (el propósito): Proveer el catálogo de preguntas del test de vulnerabilidad familiar al voluntario en porciones o en masa.
 * Por qué es importante (el impacto o problema que resuelve): Habilita al frontend para que cargue la batería de preguntas oficiales que componen el test de vulnerabilidad del hogar.
 */
@WebServlet(urlPatterns = {
    "/api/vulnerableQuestions",
    "/api/vulnerableQuestions/paginate"
})
public class PreguntasVulnerabilidadServlet extends HttpServlet {

    /*
     * Qué hace (la acción): Instancia de manera privada y constante la variable servicio de tipo VulnerabilidadServicio.
     * Qué significa (conceptos, métodos, tipos involucrados): Instancia de la clase de servicios de negocio para vulnerabilidad.
     * Para qué se usa (el propósito): Invocar las consultas de preguntas.
     */
    private final VulnerabilidadServicio servicio = new VulnerabilidadServicio();

    /*
     * Qué hace (la acción): Sobrescribe el método doGet para obtener el catálogo completo de preguntas si se llama al endpoint general, o de forma paginada en el endpoint "/paginate" con el número de página suministrado.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - request.getServletPath(): Retorna la ruta mapeada que disparó el servlet (ej. "/api/vulnerableQuestions/paginate").
     *   - servicio.obtenerPreguntasPaginadas(page, 3): Obtiene bloques de 3 preguntas para alivianar el peso del formulario.
     * Para qué se usa (el propósito): Renderizar las preguntas del censo en el cuestionario de vulnerabilidad del voluntario.
     * Por qué es importante (el impacto o problema que resuelve): Permite que el cuestionario se cargue de forma modular (por ejemplo, de 3 en 3), reduciendo los tiempos de carga en dispositivos con conectividad limitada.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String path = request.getServletPath();
        if (path.endsWith("/paginate")) {
            int page = 1;
            String pageStr = request.getParameter("page");
            if (pageStr != null) {
                try {
                    page = Integer.parseInt(pageStr);
                } catch (NumberFormatException e) {
                    page = 1;
                }
            }
            String json = servicio.obtenerPreguntasPaginadas(page, 3);
            response.getWriter().write(json);
        } else if (path.equals("/api/vulnerableQuestions")) {
            String json = servicio.obtenerPreguntas();
            response.getWriter().write(json);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write(ResponseUtil.error("Recurso no encontrado."));
        }
    }
}
