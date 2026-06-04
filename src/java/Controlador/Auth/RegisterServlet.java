package Controlador.Auth;

import Modelo.Servicios.Auth.RegistroServicio;
import Modelo.DAO.NotificacionDAO;
import Modelo.DTO.NotificacionDTO;
import Modelo.DAO.UsuarioDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import Modelo.Utilidades.JSONUtil;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
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

            // MODIFICADO: Extracción y validación robusta de datos de entrada (previene excepciones de parseo JSON y HTTP 500)
            if (!body.has("names") || body.getString("names").trim().isEmpty()) {
                throw new IllegalArgumentException("El nombre es requerido");
            }
            if (!body.has("last_names") || body.getString("last_names").trim().isEmpty()) {
                throw new IllegalArgumentException("El apellido es requerido");
            }
            if (!body.has("email") || body.getString("email").trim().isEmpty()) {
                throw new IllegalArgumentException("El correo electrónico es requerido");
            }
            if (!body.has("password") || body.getString("password").trim().isEmpty()) {
                throw new IllegalArgumentException("La contraseña es requerida");
            }
            if (!body.has("document_number") || body.getString("document_number").trim().isEmpty()) {
                throw new IllegalArgumentException("El número de documento es requerido");
            }
            if (!body.has("birth_date") || body.getString("birth_date").trim().isEmpty()) {
                throw new IllegalArgumentException("La fecha de nacimiento es requerida");
            }
            if (!body.has("phone") || body.getString("phone").trim().isEmpty()) {
                throw new IllegalArgumentException("El celular o teléfono es requerido");
            }
            if (!body.has("document_type_id") || body.getString("document_type_id").trim().isEmpty()) {
                throw new IllegalArgumentException("El tipo de documento es requerido");
            }
            if (!body.has("gender_id") || body.getString("gender_id").trim().isEmpty()) {
                throw new IllegalArgumentException("El género es requerido");
            }
            if (!body.has("organization_id") || body.getString("organization_id").trim().isEmpty()) {
                throw new IllegalArgumentException("La seccional u organización es requerida");
            }

            String nombres = body.getString("names").trim();
            String apellidos = body.getString("last_names").trim();
            String email = body.getString("email").trim();
            String password = body.getString("password");
            String numDocumento = body.getString("document_number").trim();
            String fechaNac = body.getString("birth_date").trim();
            String telefono = body.getString("phone").trim();

            int tipoDocumentoId;
            int generoId;
            int organizacionId;

            try {
                tipoDocumentoId = Integer.parseInt(body.getString("document_type_id"));
                generoId = Integer.parseInt(body.getString("gender_id"));
                organizacionId = Integer.parseInt(body.getString("organization_id"));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Los IDs de tipo de documento, género y organización deben ser numéricos");
            }

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
            // CREAR NOTIFICACIÓN PARA SUPERVISORES
            // =========================================
            try {
                UsuarioDAO usuarioDAO = new UsuarioDAO();
                NotificacionDAO notificacionDAO = new NotificacionDAO();
                
                // Obtener todos los supervisores (rol_id = 2)
                List<Map<String, Object>> supervisores = usuarioDAO.listarVoluntariosTodos();
                
                for (Map<String, Object> supervisor : supervisores) {
                    if ((Integer) supervisor.get("rol_id") == 2) {
                        NotificacionDTO notificacion = new NotificacionDTO();
                        notificacion.setUsuarioId((Integer) supervisor.get("id"));
                        notificacion.setTitulo("Nuevo usuario registrado");
                        notificacion.setMensaje("El usuario " + nombres + " " + apellidos + " se ha registrado en el sistema y espera activación");
                        notificacion.setTipo("nuevo_usuario");
                        notificacion.setLeida(false);
                        notificacion.setEnlace("#/supervisor/usuarios/peticiones");
                        notificacion.setEntidadId(0);
                        notificacionDAO.crear(notificacion);
                    }
                }
            } catch (Exception e) {
                // No fallar el registro si la notificación falla
                System.err.println("Error al crear notificación: " + e.getMessage());
            }

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

            // MODIFICADO: Retornar código HTTP 400 (Bad Request) si es un error de validación o duplicidad, de lo contrario 500
            if (e instanceof IllegalArgumentException || e.getMessage().contains("ya está registrado")) {
                response.setStatus(400);
            } else {
                response.setStatus(500);
            }

            JSONObject error = new JSONObject();

            error.put("success", false);

            error.put("message", e.getMessage());

            out.print(error);
        }
    }
}