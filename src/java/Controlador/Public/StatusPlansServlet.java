package Controlador.Public;

/*
 * Qué hace (la acción): Importa la clase de conexión a base de datos, utilidad de respuestas web, APIs de servlets y objetos JSON.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.Config.Conexion: Mapea la conexión JDBC del sistema.
 *   - Modelo.Utilidades.ResponseUtil: Genera la estructura JSON de respuesta estándar del backend.
 * Para qué se usa (el propósito): Proveer el acceso al listado de estados de los planes familiares registrados.
 * Por qué es importante (el impacto o problema que resuelve): Sin estas importaciones, no podríamos conectar con la base de datos de estados de planes ni devolver la información estructurada al cliente.
 */
import Modelo.Config.Conexion;
import Modelo.Utilidades.ResponseUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.json.JSONArray;
import org.json.JSONObject;

/*
 * Qué hace (la acción): Mapea el servlet StatusPlansServlet al endpoint de red "/api/statusPlans/*" mediante la anotación @WebServlet.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - @WebServlet: Registra e inicializa el servlet ante el contenedor web Tomcat.
 * Para qué se usa (el propósito): Exponer de forma pública la consulta de los estados posibles que puede tener un plan de emergencia.
 * Por qué es importante (el impacto o problema que resuelve): Permite que cualquier componente que necesite mapear o filtrar planes de emergencia conozca los estados vigentes en el sistema (ej. Borrador, Enviado, Aprobado, etc.).
 */
@WebServlet("/api/statusPlans/*")
public class StatusPlansServlet extends HttpServlet {

    /*
     * Qué hace (la acción): Sobrescribe el método doGet para consultar la tabla 'estados_plan' en la base de datos SQL y retornar un arreglo JSON con los registros encontrados.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - PreparedStatement: Objeto para compilar y ejecutar de forma segura la consulta SQL "SELECT id, nombre FROM estados_plan".
     *   - ResultSet: Puntero que itera las filas devueltas por la consulta SQL.
     * Para qué se usa (el propósito): Cargar el listado paramétrico de estados para los filtros de búsqueda en el dashboard del supervisor o del voluntario.
     * Por qué es importante (el impacto o problema que resuelve): Provee el catálogo oficial de estados de planes en tiempo real directamente desde la base de datos relacional.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        PrintWriter out = response.getWriter();
        JSONArray data = new JSONArray();
        String sql = "SELECT id, nombre FROM estados_plan";

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                JSONObject estado = new JSONObject();
                estado.put("id", rs.getInt("id"));
                estado.put("name", rs.getString("nombre"));
                data.put(estado);
            }

            out.print(ResponseUtil.success(data));

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(ResponseUtil.error("Error al cargar estados de planes: " + e.getMessage()));
        }
    }
}
