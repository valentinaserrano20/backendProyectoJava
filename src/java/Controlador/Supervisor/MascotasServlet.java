package Controlador.Supervisor;

/*
 * Qué hace (la acción): Importa la clase de servicio del supervisor, utilidad de respuestas y APIs de servlets de Jakarta.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.Servicios.Supervisor.SupervisorServicio: Servicio de negocio con lógica de lectura a nivel de supervisor.
 *   - Modelo.Utilidades.ResponseUtil: Clase para formatear respuestas JSON consistentes.
 * Para qué se usa (el propósito): Proveer al servlet de las APIs e interfaces para consultar el censo total de mascotas.
 * Por qué es importante (el impacto o problema que resuelve): Permite que la clase interactúe con el servicio del supervisor para traer los registros de mascotas almacenados en la base de datos.
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
 * Qué hace (la acción): Asocia el servlet MascotasServlet con el endpoint "/api/mascotas/*" mediante @WebServlet.
 * Qué significa (conceptos, métodos, tipos involucrados): @WebServlet define la registración automática del servlet en Tomcat.
 * Para qué se usa (el propósito): Servir como el endpoint de administración del supervisor para listar todas las mascotas y animales censados.
 * Por qué es importante (el impacto o problema que resuelve): Diferencia el flujo de administración (que lista todas las mascotas) del flujo de mascotas familiar del voluntario, manteniendo políticas de acceso e integridad robustas.
 */
@WebServlet("/api/mascotas/*")
public class MascotasServlet extends HttpServlet {

    /*
     * Qué hace (la acción): Instancia de manera privada y constante la variable servicio de tipo SupervisorServicio.
     * Qué significa (conceptos, métodos, tipos involucrados): Instancia de la clase de servicios de negocio del supervisor.
     * Para qué se usa (el propósito): Invocar la consulta de la lista completa de mascotas.
     */
    private final SupervisorServicio servicio = new SupervisorServicio();

    /*
     * Qué hace (la acción): Sobrescribe el método doGet para obtener el censo global de mascotas y sus respectivos dueños/planes de emergencia en formato JSON.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - servicio.obtenerTodasMascotas(): Método de negocio que une la información de mascotas con la de sus respectivos planes de emergencia familiares en base de datos.
     *   - response.getWriter().write(): Retorna el JSON directo en el canal de red del cliente.
     * Para qué se usa (el propósito): Alimentar la tabla del panel de administración de mascotas para los supervisores del censo.
     * Por qué es importante (el impacto o problema que resuelve): Facilita al supervisor auditar las mascotas del sector y verificar que cuenten con su respectiva planeación de evacuación o datos de vacunación.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            String resJson = servicio.obtenerTodasMascotas();
            response.getWriter().write(resJson);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(ResponseUtil.error("Error interno del servidor: " + e.getMessage()));
        }
    }
}
