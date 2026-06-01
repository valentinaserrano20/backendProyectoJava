package Controlador.Public;

import Modelo.Config.Conexion;
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

// Sirve para: Proveer un endpoint unificado para obtener los estados de los planes familiares de emergencia
// Qué hace: Consulta los estados de plan de la base de datos y los retorna en un arreglo JSON mapeando 'nombre' a 'name'
// Por qué es importante: El frontend utiliza este endpoint para poblar los filtros de selección en las bandejas del supervisor y del voluntario
@WebServlet("/api/statusPlans/*")
public class StatusPlansServlet extends HttpServlet {

    // Sobrescribe doGet para procesar solicitudes HTTP GET sobre los estados de planes
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Establece el tipo de contenido y codificación UTF-8 para evitar caracteres extraños
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        JSONArray data = new JSONArray();

        // Consulta SQL para extraer los identificadores y descripciones de los estados
        String sql = "SELECT id, nombre FROM estados_plan";

        // Inicializa la conexión y la consulta segura mediante recursos JDBC
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            // Recorre cada registro de la tabla estados_plan
            while (rs.next()) {
                JSONObject estado = new JSONObject();
                // Inyecta el ID del estado
                estado.put("id", rs.getInt("id"));
                // Mapea 'nombre' en base de datos a 'name' esperado por el filtro de la SPA en el frontend
                estado.put("name", rs.getString("nombre"));
                // Agrega el objeto individual al arreglo principal
                data.put(estado);
            }

            // Encapsula los datos en un objeto de respuesta exitoso
            JSONObject jsonRes = new JSONObject();
            jsonRes.put("success", true);
            jsonRes.put("data", data);

            // Imprime y envía la respuesta al cliente
            out.print(jsonRes.toString());

        } catch (Exception e) {
            // Imprime la traza en la consola de Tomcat para depuración del desarrollador
            e.printStackTrace();
            // Retorna un código HTTP 500 en caso de fallo crítico de base de datos
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(new JSONObject()
                    .put("success", false)
                    .put("message", "Error al cargar estados de planes: " + e.getMessage()).toString());
        }
    }
}
