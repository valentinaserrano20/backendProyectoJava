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

        // Configura el encabezado HTTP indicando que el servidor responderá en formato JSON
        response.setContentType("application/json");
        // Asegura que la codificación de caracteres de salida sea UTF-8 para admitir tildes y caracteres especiales
        response.setCharacterEncoding("UTF-8");

        // Obtiene el escritor de salida para imprimir la respuesta JSON de vuelta al cliente
        PrintWriter out = response.getWriter();

        // Inicia el bloque try para atrapar cualquier fallo de parseo, validación o base de datos
        try {

            // =========================================
            // LEER EL BODY JSON QUE MANDA EL FRONTEND
            // =========================================

            // Lee y parsea todo el cuerpo de entrada de la petición HTTP devolviendo un objeto JSONObject
            JSONObject body =JSONUtil.leerJson(request);

            // Valida que la propiedad 'names' exista en el JSON y que su contenido no esté en blanco
            if (!body.has("names") || body.getString("names").trim().isEmpty()) {
                // Lanza una excepción inmediata si el parámetro obligatorio no está presente
                throw new IllegalArgumentException("El nombre es requerido");
            }
            // Valida que la propiedad 'last_names' exista en el JSON y no esté en blanco
            if (!body.has("last_names") || body.getString("last_names").trim().isEmpty()) {
                // Lanza una excepción si el parámetro obligatorio del apellido falta
                throw new IllegalArgumentException("El apellido es requerido");
            }
            // Valida que la propiedad 'email' exista en el JSON y no esté en blanco
            if (!body.has("email") || body.getString("email").trim().isEmpty()) {
                // Detiene la ejecución si el correo electrónico obligatorio está vacío
                throw new IllegalArgumentException("El correo electrónico es requerido");
            }
            // Valida que la propiedad 'password' exista en el JSON y no esté en blanco
            if (!body.has("password") || body.getString("password").trim().isEmpty()) {
                // Arroja un error si la contraseña obligatoria no fue enviada
                throw new IllegalArgumentException("La contraseña es requerida");
            }
            // Valida que la propiedad 'document_number' exista en el JSON y no esté en blanco
            if (!body.has("document_number") || body.getString("document_number").trim().isEmpty()) {
                // Arroja un error si la identificación está ausente
                throw new IllegalArgumentException("El número de documento es requerido");
            }
            // Valida que la propiedad 'birth_date' exista en el JSON y no esté en blanco
            if (!body.has("birth_date") || body.getString("birth_date").trim().isEmpty()) {
                // Arroja un error si la fecha de nacimiento no existe
                throw new IllegalArgumentException("La fecha de nacimiento es requerida");
            }
            // Valida que la propiedad 'phone' exista en el JSON y no esté en blanco
            if (!body.has("phone") || body.getString("phone").trim().isEmpty()) {
                // Arroja un error si el número de teléfono celular está ausente
                throw new IllegalArgumentException("El celular o teléfono es requerido");
            }
            // Valida que la propiedad 'document_type_id' exista en el JSON y no esté en blanco
            if (!body.has("document_type_id") || body.getString("document_type_id").trim().isEmpty()) {
                // Arroja un error si el id del tipo de documento falta
                throw new IllegalArgumentException("El tipo de documento es requerido");
            }
            // Valida que la propiedad 'gender_id' exista en el JSON y no esté en blanco
            if (!body.has("gender_id") || body.getString("gender_id").trim().isEmpty()) {
                // Arroja un error si el género no fue proporcionado
                throw new IllegalArgumentException("El género es requerido");
            }
            // Valida que la propiedad 'organization_id' exista en el JSON y no esté en blanco
            if (!body.has("organization_id") || body.getString("organization_id").trim().isEmpty()) {
                // Arroja un error si la seccional u organización no existe en la petición
                throw new IllegalArgumentException("La seccional u organización es requerida");
            }

            // Sanea y extrae las cadenas de texto del JSON para utilizarlas en las variables locales de Java
            String nombres = body.getString("names").trim();
            // Extrae los apellidos y limpia espacios innecesarios
            String apellidos = body.getString("last_names").trim();
            // Extrae el email y limpia espacios innecesarios
            String email = body.getString("email").trim();
            // Extrae la contraseña en texto plano sin alterar sus caracteres
            String password = body.getString("password");
            // Extrae el número de documento de identificación
            String numDocumento = body.getString("document_number").trim();
            // Extrae la fecha de nacimiento
            String fechaNac = body.getString("birth_date").trim();
            // Extrae el teléfono de contacto del voluntario
            String telefono = body.getString("phone").trim();

            // Declara las variables para almacenar los identificadores numéricos de las tablas catálogo
            int tipoDocumentoId;
            int generoId;
            int organizacionId;

            // Inicia el bloque para convertir a enteros las variables de IDs numéricos que vienen como string
            try {
                // Convierte a tipo entero el ID del tipo de documento
                tipoDocumentoId = Integer.parseInt(body.getString("document_type_id"));
                // Convierte a tipo entero el ID del género
                generoId = Integer.parseInt(body.getString("gender_id"));
                // Convierte a tipo entero el ID de la organización
                organizacionId = Integer.parseInt(body.getString("organization_id"));
            } catch (NumberFormatException e) {
                // Lanza un error controlado si alguno de los IDs no corresponde a un formato numérico válido
                throw new IllegalArgumentException("Los IDs de tipo de documento, género y organización deben ser numéricos");
            }

            // =========================================
            // LLAMAR AL SERVICIO
            // =========================================

            // Instancia la clase de lógica de negocios para el registro del usuario.
            // Sirve para encapsular y separar la lógica funcional del Servlet que atiende la red.
            RegistroServicio servicio = new RegistroServicio();

            // Invoca al método registrarUsuario en la clase de servicio pasándole todas las variables limpias.
            // Esta línea exacta de código despierta la lógica de validación e inicia el Paso 4.
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
                // Instancia el DAO de usuarios para realizar consultas
                UsuarioDAO usuarioDAO = new UsuarioDAO();
                // Instancia el DAO de notificaciones para persistir los avisos en la base de datos
                NotificacionDAO notificacionDAO = new NotificacionDAO();
                
                // Obtiene un listado completo de todos los voluntarios en el sistema
                List<Map<String, Object>> supervisores = usuarioDAO.listarVoluntariosTodos();
                
                // Itera sobre la lista de voluntarios para encontrar a los supervisores del sistema
                for (Map<String, Object> supervisor : supervisores) {
                    // Si el voluntario tiene rol_id igual a 2, significa que es un Supervisor
                    if ((Integer) supervisor.get("rol_id") == 2) {
                        // Crea un nuevo DTO (objeto de transferencia) de notificación
                        NotificacionDTO notificacion = new NotificacionDTO();
                        // Asigna el identificador único del supervisor destinatario
                        notificacion.setUsuarioId((Integer) supervisor.get("id"));
                        // Establece el título descriptivo del aviso
                        notificacion.setTitulo("Nuevo usuario registrado");
                        // Redacta el mensaje detallando el nombre del voluntario recién registrado
                        notificacion.setMensaje("El usuario " + nombres + " " + apellidos + " se ha registrado en el sistema y espera activación");
                        // Define el tipo de la notificación
                        notificacion.setTipo("nuevo_usuario");
                        // Establece el estado de lectura de la notificación en falso
                        notificacion.setLeida(false);
                        // Define el enlace hash interno de la SPA hacia la bandeja de peticiones pendientes
                        notificacion.setEnlace("#/supervisor/usuarios/peticiones");
                        // Define la entidad relacionada del aviso en cero por omisión
                        notificacion.setEntidadId(0);
                        // Persiste físicamente la notificación en la base de datos para este supervisor
                        notificacionDAO.crear(notificacion);
                    }
                }
            } catch (Exception e) {
                // Imprime el fallo en la consola de error pero no cancela la transacción de registro del usuario
                System.err.println("Error al crear notificación: " + e.getMessage());
            }

            // =========================================
            // RESPUESTA EXITOSA
            // =========================================

            // Crea un objeto JSON para retornar los resultados exitosos al frontend
            JSONObject respuesta = new JSONObject();

            // Inserta la bandera de éxito en verdadero
            respuesta.put("success", true);

            // Añade el mensaje que se mostrará en pantalla indicando que debe esperar confirmación
            respuesta.put(
                    "message",
                    "Cuenta creada exitosamente, espera la activación de tu cuenta"
            );

            // Imprime y devuelve la respuesta JSON escrita a través del PrintStream
            out.print(respuesta);

        } catch (Exception e) {

            // Comprueba si el fallo es por datos incorrectos o porque el email/documento ya existía en la BD
            if (e instanceof IllegalArgumentException || e.getMessage().contains("ya está registrado")) {
                // Responde con el estado HTTP 400 Bad Request indicando error del cliente
                response.setStatus(400);
            } else {
                // Responde con el estado HTTP 500 para errores internos inesperados del servidor
                response.setStatus(500);
            }

            // Crea un objeto JSON de respuesta para notificar la falla
            JSONObject error = new JSONObject();

            // Establece la bandera de éxito en falso
            error.put("success", false);

            // Añade el mensaje explicativo de la excepción ocurrida
            error.put("message", e.getMessage());

            // Imprime y despacha la respuesta JSON de error al navegador
            out.print(error);
        }
    }
}