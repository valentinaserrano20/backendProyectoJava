package Controlador;

import Modelo.DAO.UsuarioDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import org.json.JSONObject;

@WebServlet("/api/register")
public class RegisterServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            // Leer body JSON
            BufferedReader reader = request.getReader();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);

            JSONObject body = new JSONObject(sb.toString());

            String nombres      = body.getString("names");
            String apellidos    = body.getString("last_names");
            String email        = body.getString("email");
            String password     = body.getString("password");
            String numDocumento = body.getString("document_number");
            String fechaNac     = body.getString("birth_date");
            String telefono     = body.getString("phone");
            int tipoDocumentoId = Integer.parseInt(body.getString("document_type_id"));
            int generoId        = Integer.parseInt(body.getString("gender_id"));
            int organizacionId  = Integer.parseInt(body.getString("organization_id"));

            UsuarioDAO dao = new UsuarioDAO();

            if (dao.existeEmail(email)) {
                out.print(new JSONObject()
                    .put("success", false)
                    .put("message", "El correo ya está registrado"));
                return;
            }

            if (dao.existeDocumento(numDocumento)) {
                out.print(new JSONObject()
                    .put("success", false)
                    .put("message", "El número de documento ya está registrado"));
                return;
            }

            dao.registrar(nombres, apellidos, email, password,
                          numDocumento, fechaNac, telefono,
                          tipoDocumentoId, generoId, organizacionId);

            out.print(new JSONObject()
                .put("success", true)
                .put("message", "Cuenta creada exitosamente, espera la activación de tu cuenta"));

        } catch (Exception e) {
            response.setStatus(500);
            out.print(new JSONObject()
                .put("success", false)
                .put("message", "Error interno: " + e.getMessage()));
        }
    }
}