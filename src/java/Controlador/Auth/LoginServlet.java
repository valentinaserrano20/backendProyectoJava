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

// Qué hace: Registra este Servlet ante el servidor Tomcat para responder en la ruta "/api/login".
// Por qué existe: Habilita el punto de entrada (Endpoint) del API para la autenticación de usuarios.
// Qué pasaría si no estuviera: El servidor no sabría qué controlador debe responder a la petición de inicio de sesión de la SPA.
@WebServlet("/api/login")
public class LoginServlet extends HttpServlet {

    // Qué hace: Sobrescribe el método doPost para atender peticiones de tipo POST HTTP.
    // Por qué existe: El inicio de sesión transmite credenciales que deben enviarse en el cuerpo de la petición por seguridad.
    // Qué pasaría si no estuviera: Las llamadas de tipo POST al endpoint /api/login retornarían error 405 (Method Not Allowed).
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        // Usamos setContentType para indicarle al navegador del cliente que la respuesta vendrá estructurada como un documento JSON.
        // Si no se define, el navegador podría interpretar el resultado como texto plano o HTML sin formato.
        response.setContentType("application/json");
        
        // Usamos setCharacterEncoding para forzar que el flujo de salida sea interpretado en UTF-8, evitando la mutilación de caracteres especiales como acentos.
        // Si no estuviera, los nombres de usuarios con tildes o la letra 'ñ' se verían corrompidos con caracteres extraños en el frontend.
        response.setCharacterEncoding("UTF-8");

        // Obtenemos el PrintWriter para escribir texto formateado directamente en el cuerpo de la respuesta HTTP que viaja al navegador.
        // Si no se obtiene, sería imposible enviar de vuelta cualquier respuesta de éxito o fracaso al cliente.
        PrintWriter out = response.getWriter();

        try {
            // =========================================
            // 1. LEER EL JSON DEL FRONTEND
            // =========================================
            // Invocamos el método leerJson de JSONUtil para leer el cuerpo de la petición de red y transformarlo en un JSONObject manipulable
            JSONObject body = JSONUtil.leerJson(request);

            // Obtenemos el correo electrónico asociado a la llave "email" del JSON recibido.
            String email = body.getString("email");
            // Obtenemos la contraseña asociada a la llave "password" del JSON recibido.
            String password = body.getString("password");

            // =========================================
            // 2. AUTENTICAR USUARIO (SERVICIO)
            // =========================================
            // Instanciamos el servicio encargado de coordinar la lógica de negocio asociada a la autenticación.
            // y luego de esto pasamos a AuthServicio, el cual se encarga de la lógica de comparación de contraseñas seguras.
            AuthServicio authServicio = new AuthServicio();
            
            // Ejecutamos la lógica de verificación de credenciales con BCrypt delegando al servicio.
            // Si la contraseña coincide y el correo existe, nos retornará el objeto del Usuario logueado.
            Usuario usuario = authServicio.login(email, password);

            // Validamos defensivamente si el objeto de retorno es nulo para evitar fallos catastróficos de puntero nulo.
            if (usuario == null) {
                // Seteamos el estado HTTP a 401 (No autorizado) porque las credenciales no son válidas o el usuario no existe.
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); 
                
                // Creamos un nuevo objeto JSON para compilar la respuesta de credenciales incorrectas.
                JSONObject errorJson = new JSONObject();
                errorJson.put("success", false);
                errorJson.put("message", "Usuario no encontrado en el sistema.");
                
                // Enviamos el mensaje de error formateado como string al cliente a través del PrintWriter.
                out.print(errorJson);
                
                // Detenemos la ejecución del servlet inmediatamente para no proceder a crear la sesión.
                return; 
            }

            // =========================================
            // 3. ESTABLECER LA SESIÓN EN EL SERVIDOR (HttpSession)
            // =========================================
            // Solicitamos al contenedor Servlet crear o recuperar la sesión activa del cliente mediante request.getSession(true).
            // Si se pasa 'true', crea una nueva sesión si es que el cliente aún no tiene una cookie JSESSIONID válida en sus cabeceras.
            // Si no establesiéramos la sesión, el servidor olvidaría al usuario en la siguiente petición y no funcionaría el estado persistente.
            HttpSession session = request.getSession(true);
            
            // Almacenamos el ID del usuario en la sesión bajo el atributo "user_id" para recordar su identidad en futuras llamadas.
            session.setAttribute("user_id", usuario.getId());
            // Almacenamos el ID también bajo el atributo "usuarioId" para mantener la compatibilidad con otros Servlets del backend.
            session.setAttribute("usuarioId", usuario.getId()); 
            
            // Configuramos un tiempo de expiración incondicional de 1800 segundos (30 minutos) de inactividad para proteger la sesión.
            // Si transcurre este lapso sin interacción, el servidor destruye la sesión liberando memoria y protegiendo al usuario.
            session.setMaxInactiveInterval(1800); 

            // =========================================
            // 4. ARMAR RESPUESTA JSON CON MAPEO DE ROLES
            // =========================================
            // Instanciamos un JSONObject para estructurar los datos del usuario que se le devolverán a la SPA.
            JSONObject data = new JSONObject();
            // Asignamos el identificador del usuario.
            data.put("id", usuario.getId());
            // Asignamos el nombre completo uniendo los campos de nombre y apellido.
            data.put(
                    "full_name",
                    usuario.getNombre() + " " + usuario.getApellido()
            );

            // Obtiene el ID del rol del usuario autenticado (1 = Voluntario, 2 = Supervisor).
            int mappedRoleId = usuario.getRolId();
            
            // Instanciamos la clase de acceso a datos de Usuarios para consultar la base de datos relacional.
            // y luego de esto pasamos a UsuarioDAO, el cual se encarga de consultar los permisos asignados a este rol en MySQL.
            UsuarioDAO usuarioDAO = new UsuarioDAO();
            
            // Llama al método obtenerPermisosPorRol pasando el ID del rol para recuperar la lista de permisos en texto (ej. "planes:crear").
            // Si no se hiciera, no podríamos cargar permisos de forma dinámica desde las tablas del sistema de base de datos.
            java.util.List<String> dbPerms = usuarioDAO.obtenerPermisosPorRol(mappedRoleId);
            
            // Inicializamos un StringBuilder para compilar la cadena de permisos que la SPA espera recibir.
            StringBuilder permissionsBuilder = new StringBuilder();

            // Mapeo inicial de permisos base requeridos por el enrutador Vue/JS en el frontend para dirigir a la landing page.
            if (mappedRoleId == 1) {
                // Si es voluntario, le asignamos inicialmente el permiso de entrada al módulo voluntario.
                permissionsBuilder.append("home-frontend.voluntario");
            } else if (mappedRoleId == 2) {
                // Si es supervisor/gestor, le agregamos permisos a la bandeja del supervisor y administración.
                permissionsBuilder.append("home-frontend.supervisor,home-frontend.administrador");
            } else {
                // Permiso de fallback por si existiese otro tipo de rol en el futuro.
                permissionsBuilder.append("home-frontend.desconocido");
            }

            // Iteramos sobre todos los permisos que retornó la base de datos para este rol en específico.
            for (String perm : dbPerms) {
                // Concatenamos cada permiso obtenido de base de datos delimitándolo con una coma.
                permissionsBuilder.append(",").append(perm);
            }

            // Convertimos la acumulación de permisos a String para asignarlo en el JSON de respuesta.
            String permissions = permissionsBuilder.toString();

            // Guardamos el ID del rol en el JSON.
            data.put("role_id", mappedRoleId);
            // Guardamos la cadena completa de permisos concatenada en el JSON.
            data.put("permissions", permissions);
            // Mapeamos el ID de la organización (seccional) a la que pertenece el usuario. Si es nulo asigna 1 por defecto (Bucaramanga/Santander).
            data.put("sectional_id", usuario.getOrganizacionId() != null ? usuario.getOrganizacionId() : 1);
            // Mapeamos el ID del género del usuario para completar el perfil demográfico.
            data.put("gender", usuario.getGeneroId());

            // Instanciamos el JSON principal de respuesta satisfactoria.
            JSONObject respuesta = new JSONObject();
            respuesta.put("success", true);
            respuesta.put(
                    "message",
                    "Bienvenido, " + usuario.getNombre() + "!"
            );
            respuesta.put("data", data);

            // Imprimimos la respuesta serializada al canal de salida para transmitirla a la petición HTTP del navegador.
            out.print(respuesta);

        } catch (Exception e) {
            // Capturamos cualquier excepción (ej. credenciales inválidas, fallo de conexión a BD, etc.).
            String msg = e.getMessage();

            // Asigna los códigos de estado HTTP correctos para responder al cliente según la excepción capturada.
            if ("El correo electrónico no se encuentra registrado.".equals(msg) || 
                "La contraseña ingresada es incorrecta.".equals(msg)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized
            } else if ("Tu cuenta aún no está activa".equals(msg)) {
                // Si la cuenta del voluntario aún no está habilitada por el supervisor, retornamos 403 Forbidden.
                response.setStatus(HttpServletResponse.SC_FORBIDDEN); 
            } else {
                // Seteamos el estado HTTP a 500 para cualquier otro error crítico interno del servidor.
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); 
            }

            // Compilamos la estructura del JSON de error que interpretará el cliente frontend.
            JSONObject error = new JSONObject();
            error.put("success", false);
            error.put("message", msg);
            
            // Escribimos el JSON de error en el PrintWriter de salida.
            out.print(error);
        }
    }
}
