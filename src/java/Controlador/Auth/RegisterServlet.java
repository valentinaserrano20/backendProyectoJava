package Controlador.Auth;

import Modelo.Servicios.Auth.RegistroServicio;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import Modelo.Utilidades.JSONUtil;
import java.io.IOException;
import java.io.PrintWriter;
import org.json.JSONObject;

@WebServlet("/api/register")
public class RegisterServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // =========================================
        // CONFIGURACIÓN DE RESPUESTA HTTP
        // =========================================

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        try {

            // =========================================
            // LEER EL BODY JSON QUE MANDA EL FRONTEND
            // =========================================

            JSONObject body =JSONUtil.leerJson(request);

            // =========================================
            // EXTRAER DATOS DEL JSON
            // =========================================

            String nombres = body.getString("names");

            String apellidos = body.getString("last_names");

            String email = body.getString("email");

            String password = body.getString("password");

            String numDocumento = body.getString("document_number");

            String fechaNac = body.getString("birth_date");

            String telefono = body.getString("phone");

            int tipoDocumentoId =
                    Integer.parseInt(body.getString("document_type_id"));

            int generoId =
                    Integer.parseInt(body.getString("gender_id"));

            int organizacionId =
                    Integer.parseInt(body.getString("organization_id"));

            // =========================================
            // LLAMAR AL SERVICIO
            // =========================================

            RegistroServicio servicio = new RegistroServicio();

            servicio.registrarUsuario(
                    nombres,
                    apellidos,
                    email,
                    password,
                    numDocumento,
                    fechaNac,
                    telefono,
                    tipoDocumentoId,
                    generoId,
                    organizacionId
            );

            // =========================================
            // RESPUESTA EXITOSA
            // =========================================

            JSONObject respuesta = new JSONObject();

            respuesta.put("success", true);

            respuesta.put(
                    "message",
                    "Cuenta creada exitosamente, espera la activación de tu cuenta"
            );

            out.print(respuesta);

        } catch (Exception e) {

            // =========================================
            // RESPUESTA DE ERROR
            // =========================================

            response.setStatus(500);

            JSONObject error = new JSONObject();

            error.put("success", false);

            error.put("message", e.getMessage());

            out.print(error);
        }
    }
}