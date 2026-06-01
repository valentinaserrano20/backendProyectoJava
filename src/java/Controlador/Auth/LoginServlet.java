package Controlador.Auth;

import Modelo.Entidades.Usuario;
import Modelo.Servicios.Auth.AuthServicio;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

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
            JSONObject body = JSONUtil.leerJson(request);

            String email = body.getString("email");
            String password = body.getString("password");

            // =========================================
            // 2. AUTENTICAR USUARIO (SERVICIO)
            // =========================================
            AuthServicio authServicio = new AuthServicio();
            Usuario usuario = authServicio.login(email, password);

            // AGREGADO: Validación defensiva para evitar el error 500 si el usuario no existe
            if (usuario == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
                JSONObject errorJson = new JSONObject();
                errorJson.put("success", false);
                errorJson.put("message", "Usuario no encontrado en el sistema.");
                out.print(errorJson);
                return; // Corta la ejecución para que no intente leer .getId()
            }

            // =========================================
            // 3. ESTABLECER LA SESIÓN EN EL SERVIDOR (HttpSession)
            // =========================================
            HttpSession session = request.getSession(true);
            session.setAttribute("user_id", usuario.getId());
            session.setAttribute("usuarioId", usuario.getId()); // Añadido para compatibilidad con PlanFamiliarServlet
            session.setMaxInactiveInterval(1800); // 30 minutos de inactividad

            // =========================================
            // 4. ARMAR RESPUESTA JSON CON MAPEO DE ROLES
            // =========================================
            JSONObject data = new JSONObject();
            data.put("id", usuario.getId());
            data.put(
                    "full_name",
                    usuario.getNombre() + " " + usuario.getApellido()
            );

            // MODIFICADO: Mapeo directo y unificado de roles (Rol 1 = Voluntario, Rol 2 = Supervisor_Administrador)
            int mappedRoleId = usuario.getRolId();
            String permissions;

            if (mappedRoleId == 1) {
                permissions = "home-frontend.voluntario";
            } else if (mappedRoleId == 2) {
                permissions = "home-frontend.supervisor,home-frontend.administrador";
            } else {
                permissions = "home-frontend.desconocido";
            }

            data.put("role_id", mappedRoleId);
            data.put("permissions", permissions);
            // Agregamos el sectional_id y gender_id simulados o por defecto si no aplican
            data.put("sectional_id", usuario.getOrganizacionId() != null ? usuario.getOrganizacionId() : 1);
            data.put("gender", usuario.getGeneroId());

            JSONObject respuesta = new JSONObject();
            respuesta.put("success", true);
            respuesta.put(
                    "message",
                    "Bienvenido, " + usuario.getNombre() + "!"
            );
            respuesta.put("data", data);

            out.print(respuesta);

        } catch (Exception e) {
            String msg = e.getMessage();

            // CORREGIDO: Retornar códigos de estado HTTP correctos en lugar de siempre 500
            if ("Credenciales incorrectas".equals(msg)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
            } else if ("Tu cuenta aún no está activa".equals(msg)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500
            }

            JSONObject error = new JSONObject();
            error.put("success", false);
            error.put("message", msg);
            out.print(error);
        }
    }
}
