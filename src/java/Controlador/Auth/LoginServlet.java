package Controlador.Auth;

import Modelo.Entidades.Usuario;
import Modelo.Servicios.Auth.AuthServicio;
import Modelo.DAO.UsuarioDAO;

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

            // Qué hace: Obtiene el ID del rol del usuario autenticado.
            // Por qué existe: Determina las facultades y vistas iniciales asignadas en el sistema.
            // Qué problema resuelve: Permite perfilar el nivel de acceso del usuario de forma estructurada.
            int mappedRoleId = usuario.getRolId();
            
            // Qué hace: Crea una instancia de UsuarioDAO para consultar la base de datos.
            // Por qué existe: Habilita el acceso a la capa de persistencia para obtener información relacional de roles y permisos.
            // Qué problema resuelve: Permite recuperar los permisos reales guardados en MySQL.
            UsuarioDAO usuarioDAO = new UsuarioDAO();
            
            // Qué hace: Llama a obtenerPermisosPorRol pasando el ID del rol para recuperar la lista de permisos de BD.
            // Por qué existe: Carga la lista dinámica de llaves autorizadas para este usuario.
            // Qué problema resuelve: Evita la asignación estática o "hardcoded" de permisos a nivel de base de datos.
            java.util.List<String> dbPerms = usuarioDAO.obtenerPermisosPorRol(mappedRoleId);
            
            // Qué hace: Inicializa un StringBuilder para compilar la cadena de permisos que espera el frontend.
            // Por qué existe: Facilita la concatenación eficiente de múltiples strings en un formato legible.
            // Qué problema resuelve: Centraliza la serialización de permisos en un solo string delimitado por comas.
            StringBuilder permissionsBuilder = new StringBuilder();

            // Qué hace: Evalúa el ID de rol y añade los permisos heredados del enrutamiento de la SPA.
            // Por qué existe: Mantiene la compatibilidad hacia atrás con el router JS evitando que se bloquee el acceso a vistas principales.
            // Qué problema resuelve: Resuelve la validación de rutas privadas basadas en permisos heredados.
            if (mappedRoleId == 1) {
                permissionsBuilder.append("home-frontend.voluntario");
            } else if (mappedRoleId == 2) {
                permissionsBuilder.append("home-frontend.supervisor,home-frontend.administrador");
            } else {
                permissionsBuilder.append("home-frontend.desconocido");
            }

            // Qué hace: Itera sobre la lista de permisos cargados de la base de datos.
            // Por qué existe: Agrega las nuevas llaves dinámicas a la lista consolidada de permisos.
            // Qué problema resuelve: Expone los nuevos permisos dinámicos (ej: planes:ver, usuarios:listar) al localStorage del cliente.
            for (String perm : dbPerms) {
                permissionsBuilder.append(",").append(perm);
            }

            // Qué hace: Convierte el StringBuilder a un String final y lo asigna a la variable local de control.
            // Por qué existe: Asigna el valor definitivo al campo 'permissions' esperado en el objeto JSON de respuesta.
            // Qué problema resuelve: Permite cumplir con el contrato de la API enviando el string concatenado exacto.
            String permissions = permissionsBuilder.toString();

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

            // Sirve para: Retornar los códigos de estado HTTP correspondientes según la naturaleza de la excepción
            // Qué hace: Si el correo no está registrado o la contraseña es inválida, retorna 401. Si la cuenta está inactiva, retorna 403. Para otros errores, retorna 500.
            // Por qué es importante: Permite al cliente frontend diferenciar las validaciones de negocio de los fallos internos del servidor
            if ("El correo electrónico no se encuentra registrado.".equals(msg) || 
                "La contraseña ingresada es incorrecta.".equals(msg)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized
            } else if ("Tu cuenta aún no está activa".equals(msg)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403 Forbidden
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500 Internal Server Error
            }

            JSONObject error = new JSONObject();
            error.put("success", false);
            error.put("message", msg);
            out.print(error);
        }
    }
}
