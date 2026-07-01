package Controlador.Auth;

/*
 * Qué hace (la acción): Importa la clase de servicio de registros, DAOs de notificaciones y usuarios, DTO de notificaciones, APIs de servlets, colecciones de Java y la utilidad JSON.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.Servicios.Auth.RegistroServicio: Servicio de negocio que procesa el registro e inscripción de usuarios.
 *   - Modelo.DAO.NotificacionDAO, Modelo.DAO.UsuarioDAO: Clases DAO para manipular registros de notificaciones y usuarios en la BD.
 *   - Modelo.DTO.NotificacionDTO: Data Transfer Object para encapsular datos de alertas.
 *   - Modelo.Utilidades.JSONUtil: Utilidad para leer el flujo JSON de la petición HTTP.
 * Para qué se usa (el propósito): Proveer las herramientas lógicas necesarias para validar, inscribir usuarios y disparar notificaciones internas en el sistema.
 * Por qué es importante (el impacto o problema que resuelve): Sin estas importaciones no se podría interactuar con la lógica de negocio ni enviar las alertas automáticas hacia el panel de los supervisores.
 */
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

/*
 * Qué hace (la acción): Registra el servlet RegisterServlet mapeándolo al endpoint "/api/register".
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - @WebServlet("/api/register"): Anotación que expone este servlet en la ruta indicada ante peticiones entrantes.
 *   - extends HttpServlet: Permite comportarse como un controlador web HTTP.
 * Para qué se usa (el propósito): Servir como el endpoint oficial de registro de nuevos usuarios del sistema.
 * Por qué es importante (el impacto o problema que resuelve): Permite recibir los datos de registro de la aplicación cliente y derivarlos al proceso de negocio.
 */
@WebServlet("/api/register")
public class RegisterServlet extends HttpServlet {

    /*
     * Qué hace (la acción): Sobrescribe el método doPost para atender y procesar la petición POST de creación de cuenta de usuario.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - doPost: Método de HttpServlet especializado en el envío de datos estructurados de creación dentro del cuerpo de la petición.
     * Para qué se usa (el propósito): Recoger los campos de registro, validarlos, crear la cuenta de usuario y notificar a los supervisores.
     * Por qué es importante (el impacto o problema que resuelve): Centraliza la lógica de validación de datos iniciales del formulario del usuario antes de llamar a la base de datos.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        /*
         * Qué hace (la acción): Configura las cabeceras HTTP de respuesta estableciendo que el tipo de datos será JSON codificado en UTF-8.
         * Qué significa (conceptos, métodos, tipos involucrados): setContentType y setCharacterEncoding de HttpServletResponse.
         * Para qué se usa (el propósito): Informar al cliente que los datos retornados serán estructurados en formato JSON y asegurar que caracteres como la "ñ" y acentos se muestren correctamente.
         * Por qué es importante (el impacto o problema que resuelve): Previene corrupciones de texto y garantiza que el frontend pueda interpretar la respuesta inmediatamente.
         */
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        /*
         * Qué hace (la acción): Obtiene el PrintWriter para escribir y enviar la respuesta en texto al cliente.
         * Qué significa (conceptos, métodos, tipos involucrados): response.getWriter() obtiene el canal de flujo de salida de red.
         * Para qué se usa (el propósito): Transmitir el texto JSON de éxito o error al navegador web cliente.
         * Por qué es importante (el impacto o problema que resuelve): Sin este escritor la petición se quedaría en el limbo y el usuario no sabría si su registro fue exitoso.
         */
        PrintWriter out = response.getWriter();

        try {
            /*
             * Qué hace (la acción): Lee e interpreta el cuerpo JSON de la petición HTTP transformándolo en un objeto JSONObject.
             * Qué significa (conceptos, métodos, tipos involucrados): JSONUtil.leerJson(request) lee el flujo del cuerpo de la solicitud y lo parsea.
             * Para qué se usa (el propósito): Extraer de manera sencilla los campos requeridos para el registro del usuario.
             * Por qué es importante (el impacto o problema que resuelve): Evita leer y parsear manualmente la corriente de datos del socket de red, simplificando la lógica.
             */
            JSONObject body = JSONUtil.leerJson(request);

            /*
             * Qué hace (la acción): Valida que los campos requeridos estén presentes en el JSON y que no sean cadenas vacías.
             * Qué significa (conceptos, métodos, tipos involucrados):
             *   - body.has(clave): Verifica la presencia del atributo.
             *   - body.getString(clave).trim().isEmpty(): Verifica que la cadena no contenga solo espacios en blanco o esté vacía.
             *   - throw new IllegalArgumentException(mensaje): Lanza un error controlado que interrumpe la ejecución del código.
             * Para qué se usa (el propósito): Garantizar que la solicitud cumpla con los requisitos mínimos de datos antes de intentar procesar el registro.
             * Por qué es importante (el impacto o problema que resuelve): Evita que se inserten registros incompletos o erróneos en la base de datos.
             */
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

            /*
             * Qué hace (la acción): Extrae los datos de tipo String del JSON limpiándolos de espacios innecesarios con trim().
             * Qué significa (conceptos, métodos, tipos involucrados): body.getString(...) extrae los textos correspondientes.
             * Para qué se usa (el propósito): Asignar los datos del formulario a variables locales legibles.
             * Por qué es importante (el impacto o problema que resuelve): Prepara los datos en variables limpias antes de pasárselos a la capa de servicio.
             */
            String nombres = body.getString("names").trim();
            String apellidos = body.getString("last_names").trim();
            String email = body.getString("email").trim();
            String password = body.getString("password");
            String numDocumento = body.getString("document_number").trim();
            String fechaNac = body.getString("birth_date").trim();
            String telefono = body.getString("phone").trim();

            /*
             * Qué hace (la acción): Declara variables numéricas y parsea los identificadores de tipo de documento, género y organización de String a int.
             * Qué significa (conceptos, métodos, tipos involucrados):
             *   - Integer.parseInt(texto): Convierte texto numérico a un entero primitivo de Java.
             *   - NumberFormatException: Excepción lanzada si el texto contiene caracteres no numéricos.
             * Para qué se usa (el propósito): Validar que las llaves foráneas correspondan a números de ID válidos.
             * Por qué es importante (el impacto o problema que resuelve): Previene errores de consistencia en el backend si un cliente malicioso enviara letras en lugar de llaves numéricas para estas propiedades.
             */
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

            /*
             * Qué hace (la acción): Instancia RegistroServicio y llama al método registrarUsuario con todos los datos recolectados.
             * Qué significa (conceptos, métodos, tipos involucrados):
             *   - new RegistroServicio(): Crea el objeto de negocio para el registro.
             *   - servicio.registrarUsuario(...): Lógica de negocio que valida unicidad del correo y cédula, encripta la clave del usuario e inserta el nuevo registro en la base de datos con rol inactivo.
             * Para qué se usa (el propósito): Delegar y resolver el registro del nuevo usuario en la base de datos.
             * Por qué es importante (el impacto o problema que resuelve): Centraliza la lógica transaccional de registro en la capa correspondiente, evitando la mezcla de SQL y lógica criptográfica en el controlador servlet.
             */
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

            /*
             * Qué hace (la acción): De forma opcional e interna, busca a todos los usuarios supervisores activos en el sistema y les crea una notificación de alerta sobre el registro del nuevo voluntario.
             * Qué significa (conceptos, métodos, tipos involucrados):
             *   - UsuarioDAO / NotificacionDAO: Acceso a datos de usuarios y alertas.
             *   - list.listarVoluntariosTodos(): Devuelve la lista completa de personas registradas.
             *   - NotificacionDTO: Contenedor temporal de atributos de la notificación.
             *   - notificacionDAO.crear(notificacion): Registra el DTO de alerta en la base de datos SQL.
             * Para qué se usa (el propósito): Notificar de manera proactiva al personal del nivel de supervisión para que procedan a activar la cuenta del voluntario recién registrado.
             * Por qué es importante (el impacto o problema que resuelve): Mejora la experiencia y el flujo del sistema. El try-catch de notificaciones es defensivo: si por alguna razón falla el registro de la alerta, el registro general del usuario no se revierte (sigue siendo exitoso).
             */
            try {
                UsuarioDAO usuarioDAO = new UsuarioDAO();
                NotificacionDAO notificacionDAO = new NotificacionDAO();
                
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
                System.err.println("Error al crear notificación: " + e.getMessage());
            }

            /*
             * Qué hace (la acción): Instancia un JSONObject de confirmación, le añade el estado exitoso y lo imprime en el flujo de salida hacia el cliente.
             * Qué significa (conceptos, métodos, tipos involucrados): respuesta.put() añade claves lógicas al objeto JSON que se transmite.
             * Para qué se usa (el propósito): Comunicar al frontend que la cuenta ha sido creada exitosamente y está pendiente de activación por parte del supervisor.
             * Por qué es importante (el impacto o problema que resuelve): Envía la respuesta de éxito de vuelta al navegador del cliente finalizando el flujo asíncrono satisfactoriamente.
             */
            JSONObject respuesta = new JSONObject();
            respuesta.put("success", true);
            respuesta.put("message", "Cuenta creada exitosamente, espera la activación de tu cuenta");
            out.print(respuesta);

        } catch (Exception e) {
            /*
             * Qué hace (la acción): Captura errores ocurridos durante el registro, establece el código de estado HTTP adecuado (400 o 500) y responde un JSON con el mensaje detallado.
             * Qué significa (conceptos, métodos, tipos involucrados):
             *   - e instanceof IllegalArgumentException: Verifica si el error fue por validación de campos.
             *   - response.setStatus(código): Configura el código HTTP de respuesta.
             * Para qué se usa (el propósito): Reportar fallos controlados (ej: email ya registrado, celular inválido) para que el frontend informe al usuario final de manera adecuada.
             * Por qué es importante (el impacto o problema que resuelve): Previene la inestabilidad de la aplicación, oculta los detalles internos de base de datos Java (StackTrace) y ofrece retroalimentación precisa al usuario.
             */
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