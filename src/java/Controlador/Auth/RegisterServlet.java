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

    // =========================================================================
    // UBICACIÓN: RegisterServlet.java (Método doPost)
    // Código real de tu proyecto con comentarios explicativos inyectados
    // =========================================================================
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // EXPLICACIÓN DE CONCEPTOS LÍNEA POR LÍNEA:
        
        // response.setContentType("application/json") le pone un sello a la cabecera de la respuesta HTTP.
        // Sirve para avisarle al navegador del usuario que lo que le vamos a devolver al final del día
        // no es una página web visual (HTML), sino un bloque de datos estructurado en formato JSON.
        response.setContentType("application/json");
        
        // response.setCharacterEncoding("UTF-8") define el mapa de traducción binaria para las letras.
        // Sirve para que caracteres como la "ñ", los acentos o caracteres especiales en español no se rompan 
        // ni se transformen en símbolos extraños (como 'Ã±') durante su viaje de regreso por internet.
        response.setCharacterEncoding("UTF-8");

        // PrintWriter es una clase de Java que actúa como un "escribano de red". El método response.getWriter()
        // nos entrega un objeto conectado directamente al puerto de internet del usuario que hizo la petición.
        // Todo lo que escribamos en la variable 'out' viajará inmediatamente de vuelta al navegador web.
        PrintWriter out = response.getWriter();

        // Iniciamos un bloque try-catch. Si algo falla adentro (un dato inválido, base de datos caída),
        // el código saltará de inmediato al bloque "catch" de abajo para evitar que el servidor colapse.
        try {

            // JSONUtil.leerJson(request) es una clase de utilidad de tu proyecto. Va al flujo de entrada de la red,
            // lee todo el texto JSON plano que envió JavaScript en el Paso 2, y lo transforma en un objeto JSONObject
            // de Java para que podamos extraer sus propiedades usando métodos como .getString().
            JSONObject body = JSONUtil.leerJson(request);

            // VALIDACIONES DE ENTRADA: 
            // .has("names") revisa si la propiedad existe en el JSON. .getString("names").trim().isEmpty() 
            // extrae el texto, le borra los espacios de los lados y comprueba si el usuario lo dejó en blanco.
            if (!body.has("names") || body.getString("names").trim().isEmpty()) {
                // Si la validación falla, lanza un error controlado deteniendo el flujo del programa de inmediato.
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

            // Una vez que sabemos que ningún campo viene vacío, extraemos los textos del objeto JSON
            // y los guardamos dentro de variables locales estándar de Java (String).
            String nombres = body.getString("names").trim();
            String apellidos = body.getString("last_names").trim();
            String email = body.getString("email").trim();
            String password = body.getString("password"); // Conserva la clave original para encriptarla luego
            String numDocumento = body.getString("document_number").trim();
            String fechaNac = body.getString("birth_date").trim();
            String telefono = body.getString("phone").trim();

            // Declaramos variables enteras (int) para almacenar las claves numéricas de los catálogos de la base de datos.
            int tipoDocumentoId;
            int generoId;
            int organizacionId;

            // Usamos un try interno porque convertir texto a número puede fallar si mandan letras en lugar de números.
            try {
                // Integer.parseInt() toma el texto del JSON (ej: "1") y lo transforma en un número entero real (1).
                tipoDocumentoId = Integer.parseInt(body.getString("document_type_id"));
                generoId = Integer.parseInt(body.getString("gender_id"));
                organizacionId = Integer.parseInt(body.getString("organization_id"));
            } catch (NumberFormatException e) {
                // Si el formato de texto no se pudo convertir a número, se lanza este error para proteger el sistema.
                throw new IllegalArgumentException("Los IDs de tipo de documento, género y organización deben ser numéricos");
            }

            // =========================================================================
            // LÍNEA CRÍTICA DE REACCIÓN EN CADENA (Invocación al Servicio):
            // =========================================================================
            
            // Creamos un objeto vivo en la memoria (Instancia) de la clase RegistroServicio usando la palabra clave 'new'.
            // Hacemos esto porque el Servlet solo maneja la red, no sabe de reglas de negocio.
            RegistroServicio servicio = new RegistroServicio();

            // Invocamos al método .registrarUsuario() pasándole todas nuestras variables limpias como argumentos.
            // Esta línea exacta transfiere el flujo de ejecución del Servlet hacia el Paso 4 (La Capa de Servicio).
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

            // =========================================================================
            // INTERACCIÓN CON COMPONENTES ADICIONALES (Uso de DTO y DAO complementario)
            // =========================================================================
            try {
                // Instancia el DAO de usuarios para poder consultar registros existentes de la base de datos.
                UsuarioDAO usuarioDAO = new UsuarioDAO();
                // Instancia el DAO de notificaciones, encargado exclusivo de guardar alertas en SQL.
                NotificacionDAO notificacionDAO = new NotificacionDAO();
                
                // Llama al método del DAO para traer una lista con todos los usuarios registrados en el sistema.
                List<Map<String, Object>> supervisores = usuarioDAO.listarVoluntariosTodos();
                
                // Itera (recorre) uno por uno los usuarios de la lista mediante un ciclo for
                for (Map<String, Object> supervisor : supervisores) {
                    // Extrae el valor de la columna 'rol_id'. Si es igual a 2, significa que este usuario es un Supervisor.
                    if ((Integer) supervisor.get("rol_id") == 2) {
                        
                        // USO DEL DTO: Creamos un Data Transfer Object (un contenedor vacío diseñado solo para mover datos).
                        NotificacionDTO notificacion = new NotificacionDTO();
                        
                        // Metemos la información dentro de la "caja" del DTO usando sus métodos setter (.set...)
                        notificacion.setUsuarioId((Integer) supervisor.get("id")); // ID del supervisor que recibirá la alerta
                        notificacion.setTitulo("Nuevo usuario registrado");
                        notificacion.setMensaje("El usuario " + nombres + " " + apellidos + " se ha registrado en el sistema y espera activación");
                        notificacion.setTipo("nuevo_usuario"); // Clasificación interna de la alerta
                        notificacion.setLeida(false); // Por defecto la alerta nace marcada como "No leída"
                        notificacion.setEnlace("#/supervisor/usuarios/peticiones"); // Destino al hacer clic en el frontend
                        notificacion.setEntidadId(0);
                        
                        // REACCIÓN EN CADENA SECUNDARIA: Pasamos la caja DTO llena al método .crear() de NotificacionDAO.
                        // Esto hace que la alerta viaje directamente hacia su propia tabla en la base de datos.
                        notificacionDAO.crear(notificacion);
                    }
                }
            } catch (Exception e) {
                // Si falla el envío de notificaciones (por ejemplo, la tabla de alertas no existe), imprimimos el error 
                // en la consola del servidor, pero NO detenemos el registro del usuario. El voluntario se registra igual.
                System.err.println("Error al crear notificación: " + e.getMessage());
            }

            // RESPUESTA DE ÉXITO EN JSON:
            // Creamos una respuesta vacía usando la clase JSONObject de la librería.
            JSONObject respuesta = new JSONObject();
            // Le insertamos una clave lógica 'success' establecida en verdadero (true).
            respuesta.put("success", true);
            // Inyectamos el mensaje descriptivo de éxito.
            respuesta.put("message", "Cuenta creada exitosamente, espera la activación de tu cuenta");

            // El escritor de red 'out' toma el objeto JSON, lo convierte en texto plano y lo empuja 
            // a través de internet de vuelta al archivo 'registerController.js' del Paso 1.
            out.print(respuesta);

        } catch (Exception e) {
            // Si algo falló arriba o el Servicio arrojó una excepción, el programa se salta todo y cae en este bloque.
            
            // Si el error ocurrió porque faltó un campo (IllegalArgumentException) o porque el correo/cédula ya existían:
            if (e instanceof IllegalArgumentException || e.getMessage().contains("ya está registrado")) {
                // Modificamos el estado de la respuesta HTTP a 400 (Bad Request), indicándole al navegador que fue un error del cliente.
                response.setStatus(400);
            } else {
                // Si fue un error imprevisto (ej: código Java mal escrito o conexión a base de datos muerta), ponemos estado 500 (Server Error).
                response.setStatus(500);
            }

            // Creamos un objeto JSON exclusivo para empacar los datos de la falla.
            JSONObject error = new JSONObject();
            error.put("success", false);
            error.put("message", e.getMessage()); // Captura el mensaje exacto del error (ej: "El correo ya está registrado")

            // Escribe el JSON de error en el canal de red hacia el navegador del cliente.
            out.print(error);
        }
    }
}