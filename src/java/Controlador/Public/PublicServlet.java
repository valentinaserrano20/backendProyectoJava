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

            CatalogoDAO dao = new CatalogoDAO();

            JSONArray data = null;

            if ("/generos".equals(pathInfo)) {

                System.out.println("Entró a géneros");

                data = dao.getGeneros();

            } else if ("/tipos-documento".equals(pathInfo)) {

                System.out.println("Entró a tipos documento");

                data = dao.getTiposDocumento();

            } else if ("/organizaciones".equals(pathInfo)) {

                System.out.println("Entró a organizaciones");

                data = dao.getOrganizaciones();

            } else {

                response.setStatus(404);

                out.print(new JSONObject()
                        .put("success", false)
                        .put("message", "Ruta no encontrada"));

                return;
            }

            out.print(new JSONObject().put("data", data));

        } catch (Exception e) {

            e.printStackTrace();

            response.setStatus(500);

            out.print(new JSONObject()
                    .put("success", false)
                    .put("message", e.toString()));
        }
    }
}

