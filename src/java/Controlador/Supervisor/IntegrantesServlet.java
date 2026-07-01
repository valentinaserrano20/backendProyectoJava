package Controlador.Supervisor;

/*
 * Qué hace (la acción): Importa la capa de servicios de supervisor, utilidad de respuestas web y las APIs estándares de servlets de Jakarta.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.Servicios.Supervisor.SupervisorServicio: Servicio de negocio que procesa las consultas e interacciones de administración y supervisión.
 *   - Modelo.Utilidades.ResponseUtil: Utilidad para formatear respuestas JSON de éxito o error.
 * Para qué se usa (el propósito): Proveer al servlet de las dependencias requeridas para consultar el listado global de integrantes de planes familiares.
 * Por qué es importante (el impacto o problema que resuelve): Sin estas importaciones no se podría coordinar la lógica de negocio ni responder al cliente en formato JSON estándar.
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
 * Qué hace (la acción): Mapea el servlet a la URL "/api/integrantes/*" a través de la anotación @WebServlet.
 * Qué significa (conceptos, métodos, tipos involucrados): @WebServlet es el decorador del contenedor Tomcat para la registración del servlet.
 * Para qué se usa (el propósito): Servir como el endpoint de administración del supervisor para listar de manera consolidada a todos los miembros de familias registrados en el sistema.
 * Por qué es importante (el impacto o problema que resuelve): Separa las rutas del voluntario (quien gestiona a su propia familia) de las rutas del supervisor (quien tiene acceso al censo completo de integrantes de la comunidad), garantizando control de roles.
 */
@WebServlet("/api/integrantes/*")
public class IntegrantesServlet extends HttpServlet {

    /*
     * Qué hace (la acción): Instancia de manera privada y constante la variable servicio de tipo SupervisorServicio.
     * Qué significa (conceptos, métodos, tipos involucrados): Instancia de la capa de lógica de negocio del supervisor.
     * Para qué se usa (el propósito): Llamar a las funciones de consulta de integrantes y sus datos vinculados.
     */
    private final SupervisorServicio servicio = new SupervisorServicio();

    /*
     * Qué hace (la acción): Sobrescribe el método doGet para obtener todos los integrantes de familias registrados y enviarlos al escritor de respuesta en formato JSON.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - servicio.obtenerTodosFamilyMembers(): Método del servicio que consulta la base de datos y retorna los miembros de las familias junto con el estado del plan familiar de cada uno.
     *   - response.getWriter().write(): Escribe los bytes de texto directamente a la red del cliente.
     * Para qué se usa (el propósito): Alimentar la tabla de censo general en el panel de control del supervisor de Cruz Roja.
     * Por qué es importante (el impacto o problema que resuelve): Permite al supervisor visualizar la totalidad de integrantes y verificar a qué plan de emergencia pertenecen en una única vista consolidada.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            String resJson = servicio.obtenerTodosFamilyMembers();
            response.getWriter().write(resJson);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(ResponseUtil.error("Error interno del servidor: " + e.getMessage()));
        }
    }
}
