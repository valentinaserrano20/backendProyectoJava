package Controlador;

import Modelo.DAO.UsuarioDAO;
import Modelo.Entidades.Usuario;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import org.json.JSONObject;

@WebServlet("/api/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            // 1. Leer el JSON que manda el SPA
            BufferedReader reader = request.getReader();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);

            JSONObject body = new JSONObject(sb.toString());
            String email = body.getString("email");
            String password = body.getString("password");

            // 2. Consultar en la BD
            UsuarioDAO dao = new UsuarioDAO();
            Usuario usuario = dao.login(email, password);

            // 3. Armar respuesta
            JSONObject respuesta = new JSONObject();

            if (usuario == null) {
                respuesta.put("success", false);
                respuesta.put("message", "Credenciales incorrectas");
                response.setStatus(200);
                out.print(respuesta);
                return;
            }

            if (!usuario.getEstado().equals("activo")) {
                respuesta.put("success", false);
                respuesta.put("message", "Tu cuenta aún no está activa");
                out.print(respuesta);
                return;
            }

            // 4. Armar el objeto "data" que espera el SPA
            JSONObject data = new JSONObject();
            data.put("id", usuario.getId());
            data.put("full_name", usuario.getNombre() + " " + usuario.getApellido());
            data.put("role_id", usuario.getRolId());
            data.put("permissions", "home-frontend." + obtenerRolNombre(usuario.getRolId()));

            respuesta.put("success", true);
            respuesta.put("message", "Bienvenido, " + usuario.getNombre() + "!");
            respuesta.put("data", data);

            out.print(respuesta);

        } catch (Exception e) {
            JSONObject error = new JSONObject();
            error.put("success", false);
            error.put("message", "Error interno: " + e.getMessage());
            response.setStatus(500);
            out.print(error);
        }
    }

    private String obtenerRolNombre(int rolId) {
        switch (rolId) {
            case 1: return "administrador";
            case 2: return "supervisor";
            case 3: return "voluntario";
            default: return "desconocido";
        }
    }
}