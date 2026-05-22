package Controlador.Auth;

import Modelo.Entidades.Usuario;
import Modelo.Servicios.Auth.AuthServicio;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import Modelo.Utilidades.JSONUtil;
import java.io.IOException;
import java.io.PrintWriter;

import org.json.JSONObject;

@WebServlet("/api/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        // Tipo de respuesta
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        try {

            // =========================================
            // 1. LEER EL JSON DEL FRONTEND
            // =========================================

            JSONObject body =JSONUtil.leerJson(request);

            String email = body.getString("email");
            String password = body.getString("password");

            // =========================================
            // 2. AUTENTICAR USUARIO (SERVICIO)
            // =========================================

            AuthServicio authServicio = new AuthServicio();

            Usuario usuario = authServicio.login(email, password);

            // =========================================
            // 3. ARMAR RESPUESTA JSON
            // =========================================

            JSONObject data = new JSONObject();

            data.put("id", usuario.getId());

            data.put(
                    "full_name",
                    usuario.getNombre() + " " + usuario.getApellido()
            );

            data.put("role_id", usuario.getRolId());

            data.put(
                    "permissions",
                    "home-frontend." + obtenerRolNombre(usuario.getRolId())
            );

            JSONObject respuesta = new JSONObject();

            respuesta.put("success", true);

            respuesta.put(
                    "message",
                    "Bienvenido, " + usuario.getNombre() + "!"
            );

            respuesta.put("data", data);

            out.print(respuesta);

        } catch (Exception e) {

            // ERROR GENERAL

            response.setStatus(500);

            JSONObject error = new JSONObject();

            error.put("success", false);

            error.put(
                    "message",
                    e.getMessage()
            );

            out.print(error);
        }
    }

    // TRADUCE ID ROL -> NOMBRE ROL


    private String obtenerRolNombre(int rolId) {

        switch (rolId) {

            case 1:
                return "Voluntario";

            case 2:
                return "Supervisor_Admin";
                
            default:
                return "desconocido";
        }
    }
}