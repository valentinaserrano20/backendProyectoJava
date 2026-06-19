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

// Qué hace: Servlet público encargado de enrutar las peticiones de catálogos geográficos (departamentos, ciudades, sectores) sin requerir autenticación.
// Por qué existe: Habilita el registro de nuevos voluntarios y la consulta de ubicaciones para visitantes anónimos en la SPA.
// Qué problema resuelve: Centraliza las consultas geográficas públicas en un único endpoint paramétrico.
@WebServlet("/api/public/*")
public class PublicServlet extends HttpServlet {

    // Qué hace: Atiende peticiones GET públicas para consultar divisiones territoriales colombianas.
    // Por qué existe: Permite a los dropdowns de ubicación (Departamento -> Ciudad -> Sector) cargarse dinámicamente en el formulario de registro.
    // Qué problema resuelve: Delega la lógica de negocio al servicio correspondiente en lugar de llamar directamente al DAO, respetando la arquitectura MVC.
    // Flujo: De aquí pasamos a CatalogoServicio.obtenerCatalogo para resolver el listado.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Configuración de cabeceras HTTP estándar para respuestas JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        String pathInfo = request.getPathInfo();

        try {
            System.out.println("PATH INFO: " + pathInfo);

            // Qué hace: Instancia el servicio de catálogos públicos y solicita el recurso correspondiente.
            // Por qué existe: Sigue la arquitectura de capas evitando acoplamiento directo entre el controlador y el DAO.
            // Qué problema resuelve: Ejecuta la consulta de departamentos, ciudades o sectores de forma parametrizada.
            Modelo.Servicios.Public.CatalogoServicio servicio = new Modelo.Servicios.Public.CatalogoServicio();
            String respuestaJson = servicio.obtenerCatalogo(pathInfo);

            JSONObject jsonRes = new JSONObject(respuestaJson);
            // Si la consulta falló o la subruta no existe, responde con código 404 (No encontrado)
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

