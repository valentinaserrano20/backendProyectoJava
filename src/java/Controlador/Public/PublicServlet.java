package Controlador.Public;

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

@WebServlet("/api/public/*")
public class PublicServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        String pathInfo = request.getPathInfo();

        try {
            System.out.println("PATH INFO: " + pathInfo);

            // MODIFICADO: Uso correcto del servicio de negocio (MVC) en lugar de saltarse la capa llamando directamente al DAO
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

