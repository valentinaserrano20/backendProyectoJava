package Controlador.Supervisor;

/*
 * Qué hace (la acción): Importa la capa de servicios del supervisor, utilidad de respuestas web y APIs estándares de servlets de Jakarta.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.Servicios.Supervisor.SupervisorServicio: Servicio de negocio encargado de coordinar la lógica de gestión a nivel de supervisor.
 *   - Modelo.Utilidades.ResponseUtil: Clase de utilidad para dar formato JSON unificado a las respuestas.
 * Para qué se usa (el propósito): Proveer al servlet las dependencias de red y servicios para listar los sectores geográficos.
 * Por qué es importante (el impacto o problema que resuelve): Permite desvincular el servlet del acceso directo a la base de datos SQL de sectores.
 */
import Modelo.Servicios.Supervisor.SupervisorServicio;
import Modelo.Utilidades.ResponseUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/*
 * Qué hace (la acción): Asocia el servlet SectoresServlet al patrón de URL "/api/sectores/*" mediante @WebServlet.
 * Qué significa (conceptos, métodos, tipos involucrados): @WebServlet realiza el mapeo de red del servlet en Tomcat.
 * Para qué se usa (el propósito): Servir como el endpoint de administración del supervisor para listar todos los sectores territoriales activos.
 * Por qué es importante (el impacto o problema que resuelve): Diferencia las rutas y accesos de administración del supervisor de las rutas públicas generales, manteniendo un control de acceso centralizado.
 */
@WebServlet("/api/sectores/*")
public class SectoresServlet extends HttpServlet {

    /*
     * Qué hace (la acción): Instancia de manera privada y constante la variable servicio de tipo SupervisorServicio.
     * Qué significa (conceptos, métodos, tipos involucrados): Instancia de la clase de servicios de negocio del supervisor.
     * Para qué se usa (el propósito): Invocar el listado completo de sectores.
     */
    private final SupervisorServicio servicio = new SupervisorServicio();

    /*
     * Qué hace (la acción): Sobrescribe el método doGet para obtener todos los sectores activos y escribir la respuesta en formato JSON.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - servicio.obtenerTodosSectores(): Consulta a la base de datos y retorna los sectores con su respectiva información geográfica.
     *   - response.getWriter().write(): Retorna la respuesta serializada al cliente web.
     * Para qué se usa (el propósito): Proveer el listado de sectores geográficos para el panel de asignación o filtrado de planes del supervisor.
     * Por qué es importante (el impacto o problema que resuelve): Permite que el supervisor de Cruz Roja audite y cargue los sectores territoriales directamente de la base de datos en tiempo real.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            String resJson = servicio.obtenerTodosSectores();
            response.getWriter().write(resJson);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(ResponseUtil.error("Error interno del servidor: " + e.getMessage()));
        }
    }
}
