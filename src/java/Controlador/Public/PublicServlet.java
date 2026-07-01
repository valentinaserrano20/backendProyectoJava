package Controlador.Public;

/*
 * Qué hace (la acción): Importa la clase CatalogoDAO, APIs de servlets HTTP, excepciones y la biblioteca JSON.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.DAO.CatalogoDAO: DAO para interactuar con la base de datos de catálogos generales.
 *   - jakarta.servlet.*: Clases del ciclo de vida del servlet.
 *   - org.json.JSONObject / JSONArray: Librerías para modelar respuestas JSON.
 * Para qué se usa (el propósito): Proveer al servlet de las APIs de comunicación y acceso a datos geográficos públicos.
 * Por qué es importante (el impacto o problema que resuelve): Sin estas importaciones no se podría interceptar la petición web ni estructurar la respuesta JSON que el cliente espera recibir.
 */
import Modelo.DAO.CatalogoDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import org.json.JSONArray;
import org.json.JSONObject;

/*
 * Qué hace (la acción): Asocia el servlet PublicServlet con el endpoint público "/api/public/*" mediante la anotación @WebServlet.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - @WebServlet("/api/public/*"): Expone el servlet permitiendo subrutas dinámicas para la geografía (ej: "/departments", "/cities", "/sectors").
 *   - extends HttpServlet: Modela la clase como un controlador web HTTP.
 * Para qué se usa (el propósito): Proveer datos geográficos y paramétricos públicos a usuarios no autenticados en el sistema (ej. durante el registro).
 * Por qué es importante (el impacto o problema que resuelve): Centraliza todas las consultas territoriales colombianas en un único endpoint público que no requiere login para operar, facilitando el formulario de registro del nuevo voluntario.
 */
@WebServlet("/api/public/*")
public class PublicServlet extends HttpServlet {

    /*
     * Qué hace (la acción): Sobrescribe el método doGet para procesar consultas geográficas, delegando la carga al servicio de catálogos y respondiendo con un objeto JSON.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - request.getPathInfo(): Obtiene la sección dinámica de la URL (ej: "/cities" o "/sectors").
     *   - CatalogoServicio: Clase de negocio que coordina las consultas geográficas.
     *   - servicio.obtenerCatalogo(pathInfo): Retorna el JSON ya formateado de la base de datos según el tipo de catálogo geográfico solicitado.
     * Para qué se usa (el propósito): Retornar la lista de departamentos, ciudades o sectores para los selectores dependientes en el frontend.
     * Por qué es importante (el impacto o problema que resuelve): Separa las responsabilidades. El servlet solo atiende la red, delega la carga al servicio y formatea el código HTTP de respuesta (200 o 404), garantizando modularidad y estabilidad.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        String pathInfo = request.getPathInfo();

        try {
            System.out.println("PATH INFO: " + pathInfo);

            Modelo.Servicios.Public.CatalogoServicio servicio = new Modelo.Servicios.Public.CatalogoServicio();
            String respuestaJson = servicio.obtenerCatalogo(pathInfo);

            JSONObject jsonRes = new JSONObject(respuestaJson);
            if (!jsonRes.getBoolean("success")) {
                response.setStatus(404);
            }

            out.print(respuestaJson);

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            out.print(new JSONObject()
                    .put("success", false)
                    .put("message", e.toString()));
        }
    }
}
