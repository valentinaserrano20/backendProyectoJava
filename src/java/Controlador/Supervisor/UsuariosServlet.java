package Controlador.Supervisor;

/*
 * Qué hace (la acción): Importa la clase DAO para la gestión de usuarios, utilidades de sesión y lectura de JSON, APIs de servlets y objetos JSON.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.DAO.UsuarioDAO: Capa de acceso a datos para ejecutar operaciones de lectura, actualización, aprobación y eliminación física de usuarios en la base de datos MySQL.
 *   - Modelo.Utilidades.SessionUtil: Clase de utilidad para validar la identidad y los privilegios de los usuarios.
 *   - org.json.JSONArray / JSONObject: Librerías para estructurar y transferir datos asíncronos en formato JSON.
 * Para qué se usa (el propósito): Proveer al servlet de todas las dependencias necesarias para controlar la gobernanza de usuarios en la plataforma de emergencia familiar.
 * Por qué es importante (el impacto o problema que resuelve): Sin estas importaciones no se podría interactuar con el repositorio de usuarios en la base de datos SQL ni serializar la información al formato esperado por la SPA.
 */
import Modelo.DAO.UsuarioDAO;
import Modelo.Utilidades.JSONUtil;
import Modelo.Utilidades.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/*
 * Qué hace (la acción): Asocia el servlet UsuariosServlet con el endpoint "/api/usuarios/*" mediante la anotación @WebServlet.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - @WebServlet("/api/usuarios/*"): Comodín "*" que permite capturar e interpretar subrutas dinámicas para la gestión de usuarios (ej: cambiar rol, ver historial, activar cuenta, etc.).
 *   - extends HttpServlet: Modela la clase como un controlador web estándar de Java.
 * Para qué se usa (el propósito): Servir como el panel centralizado del supervisor o administrador para auditar, modificar y aprobar cuentas de voluntarios del sistema.
 * Por qué es importante (el impacto o problema que resuelve): Concentra toda la lógica de gobernanza de usuarios en un solo servlet de la capa de controladores, reduciendo el número de endpoints del backend y facilitando la gestión de permisos.
 */
@WebServlet("/api/usuarios/*")
public class UsuariosServlet extends HttpServlet {

    /*
     * Qué hace (la acción): Instancia de manera privada y constante la variable usuarioDAO de tipo UsuarioDAO.
     * Qué significa (conceptos, métodos, tipos involucrados): Instancia de la clase de persistencia UsuarioDAO.
     * Para qué se usa (el propósito): Acceder a los métodos SQL de la tabla 'usuarios' y 'historial_datos_maestros'.
     */
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    /*
     * Qué hace (la acción): Sobrescribe el método doGet para canalizar las solicitudes de lectura (como peticiones pendientes de aprobación, listados paginados de voluntarios, fichas técnicas individuales e historiales de cambios).
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - pathInfo.matches("^/\\d+/(historial|history)$"): Detecta si la ruta solicita el historial de cambios del usuario (ej: "/5/historial").
     *   - pathInfo.matches("^/\\d+$"): Detecta si se pide la ficha técnica del usuario por su identificador numérico directo.
     * Para qué se usa (el propósito): Servir la información de usuarios en los módulos de administración y auditoría del frontend.
     * Por qué es importante (el impacto o problema que resuelve): Permite bifurcar el flujo web GET de manera segura y controlada, respondiendo con un error HTTP 404 estructurado si la subruta no es válida.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        PrintWriter out = response.getWriter();
        String pathInfo = request.getPathInfo();

        try {
            // Caso 1: Obtener listado de peticiones de activación pendientes
            // Ruta: /api/usuarios/peticiones
            // Qué hace: Evalúa si la ruta de consulta es de peticiones pendientes de aprobación.
            // Por qué existe: Los supervisores y administradores necesitan verificar y aprobar a los nuevos voluntarios.
            // Qué problema resuelve: Permite filtrar rápidamente los usuarios en estado pendiente (state_user_id = 3).
            if (pathInfo != null && (pathInfo.equals("/peticiones") || pathInfo.equals("/requests/supervisors"))) {
                // Qué hace: Delega la lógica de negocio y paginación al método auxiliar listarPeticiones.
                // Por qué existe: Mantiene ordenado el flujo del doGet al no saturarlo de código de consultas.
                // Qué problema resuelve: Imprime la lista estructurada de solicitudes de registro pendientes.
                listarPeticiones(request, out);
            }
            // Caso 2: Obtener listado global de usuarios para administrador
            // Ruta: /api/usuarios/administrador
            // Qué hace: Verifica si la ruta corresponde al listado total de usuarios para el rol administrador.
            // Por qué existe: Permite al administrador ver todos los usuarios (voluntarios, supervisores, activos e inactivos) del sistema.
            // Qué problema resuelve: Facilita la vista de control global en la interfaz de administración.
            else if (pathInfo != null && (pathInfo.equals("/administrador") || pathInfo.equals("/userForAdmin"))) {
                // Qué hace: Llama al método auxiliar listarAdmin para procesar la paginación y recuperar de base de datos.
                // Por qué existe: Aísla el procesamiento del listado del administrador del flujo de enrutamiento principal.
                // Qué problema resuelve: Retorna los usuarios paginados en formato JSON estructurado.
                listarAdmin(request, out);
            }
            // Caso 3: Obtener listado completo de voluntarios sin paginación (para grid con scroll)
            // Ruta: /api/usuarios/todos
            // Qué hace: Verifica si la subruta es para cargar todo el listado de voluntarios de una sola vez.
            // Por qué existe: Utilizado en elementos de selección o grids dinámicos con scroll infinito del frontend.
            // Qué problema resuelve: Provee acceso total al padrón de voluntarios sin filtros de offset/limit.
            else if (pathInfo != null && pathInfo.equals("/todos")) {
                // Qué hace: Llama al método auxiliar listarVoluntariosTodos para volcar toda la tabla.
                // Por qué existe: Encapsula la consulta SELECT global de voluntarios a la base de datos.
                // Qué problema resuelve: Devuelve una lista JSON simplificada de todos los voluntarios en el sistema.
                listarVoluntariosTodos(out);
            }
            // Caso 4: Obtener listado paginado de voluntarios activos/inactivos para supervisor
            // Ruta: /api/usuarios o /api/usuarios/
            // Qué hace: Enruta peticiones a la raíz de la ruta del API de usuarios.
            // Por qué existe: Muestra el listado por defecto de voluntarios administrados por el supervisor.
            // Qué problema resuelve: Retorna el listado paginado básico para la interfaz principal de voluntarios.
            else if (pathInfo == null || pathInfo.equals("/")) {
                // Qué hace: Ejecuta listarVoluntarios leyendo los parámetros de página enviados por el frontend.
                // Por qué existe: Procesa la visualización segmentada de los usuarios para mejorar el rendimiento de red.
                // Qué problema resuelve: Envía el listado estructurado de voluntarios activos e inactivos.
                listarVoluntarios(request, out);
            }
            // Caso 4.5: Obtener historial de auditoría de un usuario específico
            // Ruta: /api/usuarios/{id}/historial o /api/usuarios/{id}/history
            // Qué hace: Comprueba mediante regex si la ruta solicita la bitácora de auditoría de un ID numérico de usuario.
            // Por qué existe: Permite ver la trazabilidad de modificaciones que ha sufrido la cuenta de un usuario en el tiempo.
            // Qué problema resuelve: Provee transparencia sobre quién, cuándo y qué se modificó en el perfil del usuario.
            else if (pathInfo.matches("^/\\d+/(historial|history)$")) {
                // Qué hace: Extrae el ID numérico del usuario de la URL usando el helper interno obtenerIdDePath.
                // Por qué existe: Sirve como parámetro de filtrado en la consulta SQL de auditoría de usuarios.
                // Qué problema resuelve: Identifica el registro exacto para filtrar la bitácora de auditoría.
                int userId = obtenerIdDePath(pathInfo);
                // Qué hace: Invoca a obtenerHistorial enviando el ID del usuario y el PrintWriter.
                // Por qué existe: Consulta la tabla de auditoría filtrando por el ID de usuario involucrado.
                // Qué problema resuelve: Imprime la lista ordenada cronológicamente de modificaciones realizadas sobre este usuario.
                obtenerHistorial(userId, out);
            }
            // Caso 5: Obtener ficha técnica detallada de un usuario por su ID
            // Ruta: /api/usuarios/{id}
            // Qué hace: Valida si la subruta contiene únicamente un ID numérico de usuario.
            // Por qué existe: Permite consultar el detalle completo de campos de un voluntario o supervisor (perfil).
            // Qué problema resuelve: Facilita al frontend la información necesaria para precargar formularios de actualización.
            else if (pathInfo.matches("^/\\d+$")) {
                // Qué hace: Extrae el ID del usuario de la ruta de la petición.
                // Por qué existe: Se utiliza como parámetro clave de búsqueda en el DAO.
                // Qué problema resuelve: Identifica al usuario exacto a consultar en la base de datos.
                int userId = Integer.parseInt(pathInfo.substring(1));
                // Qué hace: Consulta y renderiza la ficha del usuario seleccionado.
                // Por qué existe: Recupera la información combinada del usuario (JOIN con organizaciones, roles, estados).
                // Qué problema resuelve: Devuelve los campos estructurados del usuario en formato JSON estándar.
                obtenerFichaUsuario(userId, out);
            }
            // Fallback: Ruta no soportada
            // Qué hace: Envía una respuesta HTTP 404 si la URL no coincide con ningún patrón previsto.
            // Por qué existe: Maneja de forma segura las peticiones dirigidas a rutas incorrectas o inexistentes.
            // Qué problema resuelve: Informa adecuadamente al frontend sobre la invalidez de la ruta.
            else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print(new JSONObject()
                        .put("success", false)
                        .put("message", "Ruta de consulta de usuarios no encontrada.")
                        .toString());
            }
        } catch (Exception e) {
            // Qué hace: Imprime el error en los logs del servidor.
            // Por qué existe: Permite al administrador del servidor depurar cualquier fallo en tiempo de ejecución.
            // Qué problema resuelve: Evita la caída silenciosa del servlet, respondiendo con un error HTTP 500 estructurado.
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(new JSONObject()
                    .put("success", false)
                    .put("message", "Error interno al consultar datos: " + e.getMessage())
                    .toString());
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doPut para procesar modificaciones en la información personal de un voluntario o supervisor y guardar el historial de cambios.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - usuarioDAO.obtenerUsuarioDetalleGestion(userId): Recupera el estado original del usuario antes de aplicar los cambios.
     *   - usuarioDAO.actualizarDatosPersonales(...): Ejecuta el UPDATE con los nuevos datos recibidos.
     *   - usuarioDAO.registrarAuditoria(...): Registra los pares de valores modificados en la tabla de historial.
     * Para qué se usa (el propósito): Permitir a los administradores o supervisores corregir o actualizar la ficha demográfica y de contacto de un voluntario.
     * Por qué es importante (el impacto o problema que resuelve): Mantiene la consistencia de los datos del usuario y asegura la trazabilidad al guardar de manera exacta qué campos fueron editados, por quién y cuándo.
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        PrintWriter out = response.getWriter();
        HttpSession session = request.getSession(false);
        Integer actorId = SessionUtil.getUsuarioId(session);
        if (actorId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print(new JSONObject().put("success", false).put("message", "Acceso denegado. Sesión inválida."));
            return;
        }
        // Qué hace: Obtiene la información de subruta de la petición.
        // Por qué existe: Permite identificar a qué ID de usuario se dirigen los cambios.
        // Qué problema resuelve: Habilita el enrutamiento RESTful para el identificador del usuario.
        String pathInfo = request.getPathInfo();

        // Comprueba si la ruta es /api/usuarios/{id}
        // Qué hace: Evalúa si la ruta de actualización corresponde exactamente a un ID numérico de usuario.
        // Por qué existe: Es el patrón para actualización de un registro de usuario individual.
        // Qué problema resuelve: Evita procesar rutas incorrectas o malformadas.
        if (pathInfo != null && pathInfo.matches("^/\\d+$")) {
            try {
                // Qué hace: Extrae el ID del usuario como un entero.
                // Por qué existe: Identifica la clave primaria del registro de usuario a modificar.
                // Qué problema resuelve: Obtiene la llave de registro para actualizar en base de datos.
                int userId = Integer.parseInt(pathInfo.substring(1));
                // Qué hace: Lee y parsea el cuerpo de la solicitud HTTP en un objeto JSON.
                // Por qué existe: Recupera los datos del perfil actualizados que envía el frontend.
                // Qué problema resuelve: Permite leer el payload estructurado enviado en formato JSON.
                JSONObject body = JSONUtil.leerJson(request);

                // Obtener datos anteriores para registrar auditoría comparativa
                // Qué hace: Consulta el estado actual del usuario en la base de datos antes de aplicar los cambios.
                // Por qué existe: Permite comparar campo por campo qué información fue modificada.
                // Qué problema resuelve: Provee la base para registrar el valor anterior en la auditoría.
                Map<String, Object> usuarioAntes = usuarioDAO.obtenerUsuarioDetalleGestion(userId);
                if (usuarioAntes == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(
                            new JSONObject().put("success", false).put("message", "Usuario no encontrado.").toString());
                    return;
                }

                // Extraer parámetros enviados por el frontend
                // Qué hace: Extrae cada uno de los campos modificados desde el JSON del cuerpo de la petición.
                // Por qué existe: Asigna los nuevos valores a variables locales para ser inyectadas en el query SQL.
                // Qué problema resuelve: Mapea el objeto JSON del cliente a variables fuertemente tipadas en Java.
                String nombres = body.getString("names");
                String apellidos = body.getString("last_names");
                int tipoDocId = body.getInt("document_type_id");
                String numDoc = body.getString("document_number");
                String fechaNac = body.getString("birth_date");
                int generoId = body.getInt("gender_id");
                String telefono = body.getString("phone");
                int orgId = body.getInt("organization_id");
                String email = body.getString("email");

                // Actualizar en base de datos
                // Qué hace: Ejecuta la sentencia UPDATE sobre la tabla usuarios con los nuevos valores.
                // Por qué existe: Aplica los cambios de datos personales de forma persistente en MySQL.
                // Qué problema resuelve: Modifica el perfil del usuario en la capa de datos.
                usuarioDAO.actualizarDatosPersonales(userId, nombres, apellidos, tipoDocId, numDoc, fechaNac, generoId,
                        telefono, orgId, email);

                // Construir bitácora de auditoría detallando los cambios
                // Qué hace: Inicializa constructores de cadenas para documentar qué columnas cambiaron de valor.
                // Por qué existe: Genera una lista concisa de los cambios efectuados para la tabla de auditoría.
                // Qué problema resuelve: Evita guardar toda la fila si solo cambió un campo, optimizando el espacio.
                StringBuilder oldVal = new StringBuilder();
                StringBuilder newVal = new StringBuilder();

                // Qué hace: Compara cada nuevo valor con el estado anterior y agrega al log si difieren.
                // Por qué existe: Registra la traza detallada de qué atributos modificó el operador.
                // Qué problema resuelve: Construye las cadenas descriptivas de "valor anterior" y "valor nuevo".
                if (!nombres.equals(usuarioAntes.get("names"))) {
                    oldVal.append("nombre: ").append(usuarioAntes.get("names")).append("; ");
                    newVal.append("nombre: ").append(nombres).append("; ");
                }
                if (!apellidos.equals(usuarioAntes.get("last_names"))) {
                    oldVal.append("apellido: ").append(usuarioAntes.get("last_names")).append("; ");
                    newVal.append("apellido: ").append(apellidos).append("; ");
                }
                if (tipoDocId != (int) usuarioAntes.get("document_type_id")) {
                    oldVal.append("tipo_documento_id: ").append(usuarioAntes.get("document_type_id")).append("; ");
                    newVal.append("tipo_documento_id: ").append(tipoDocId).append("; ");
                }
                if (!numDoc.equals(usuarioAntes.get("document_number"))) {
                    oldVal.append("numero_documento: ").append(usuarioAntes.get("document_number")).append("; ");
                    newVal.append("numero_documento: ").append(numDoc).append("; ");
                }
                if (!fechaNac.equals(usuarioAntes.get("birth_date"))) {
                    oldVal.append("fecha_nacimiento: ").append(usuarioAntes.get("birth_date")).append("; ");
                    newVal.append("fecha_nacimiento: ").append(fechaNac).append("; ");
                }
                if (generoId != (int) usuarioAntes.get("gender_id")) {
                    oldVal.append("genero_id: ").append(usuarioAntes.get("gender_id")).append("; ");
                    newVal.append("genero_id: ").append(generoId).append("; ");
                }
                if (!telefono.equals(usuarioAntes.get("phone"))) {
                    oldVal.append("celular: ").append(usuarioAntes.get("phone")).append("; ");
                    newVal.append("celular: ").append(telefono).append("; ");
                }
                if (orgId != (int) usuarioAntes.get("organization_id")) {
                    oldVal.append("organizacion_id: ").append(usuarioAntes.get("organization_id")).append("; ");
                    newVal.append("organizacion_id: ").append(orgId).append("; ");
                }
                if (!email.equals(usuarioAntes.get("email"))) {
                    oldVal.append("email: ").append(usuarioAntes.get("email")).append("; ");
                    newVal.append("email: ").append(email).append("; ");
                }

                // Guardar auditoría si hubo algún cambio real
                // Qué hace: Si se detectaron discrepancias entre los valores anteriores y los nuevos, las escribe en la auditoría.
                // Por qué existe: Garantiza la trazabilidad y la integridad de las modificaciones en el padrón de usuarios.
                // Qué problema resuelve: Registra en auditoría_datos_maestros una fila detallando el cambio de perfil del usuario.
                if (oldVal.length() > 0) {
                    usuarioDAO.registrarAuditoria("usuarios", "UPDATE", oldVal.toString(), newVal.toString(), userId);
                }

                // Qué hace: Devuelve una respuesta JSON estándar con el éxito de la transacción.
                // Por qué existe: Notifica al frontend que la edición de perfil se persistió correctamente.
                // Qué problema resuelve: Envía el mensaje y estado final de la solicitud.
                out.print(new JSONObject()
                        .put("success", true)
                        .put("message", "Datos personales actualizados correctamente.")
                        .toString());

            } catch (Exception e) {
                // Qué hace: Imprime la traza de la excepción e informa un error 400.
                // Por qué existe: Maneja de forma segura las excepciones de análisis JSON o de base de datos.
                // Qué problema resuelve: Entrega un mensaje estructurado de error al frontend sin colapsar el hilo de ejecución.
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(new JSONObject()
                        .put("success", false)
                        .put("message", "Error al procesar la actualización: " + e.getMessage())
                        .toString());
            }
        } else {
            // Qué hace: Configura el código de error HTTP 404.
            // Por qué existe: Avisa al cliente que la ruta PUT para actualizar usuario es inválida.
            // Qué problema resuelve: Evita respuestas genéricas o de timeout ante rutas incorrectas.
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print(new JSONObject().put("success", false).put("message", "Ruta de actualización no encontrada.")
                    .toString());
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método service para desviar las llamadas PATCH hacia el método doPatch.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - service: Método del ciclo de vida del servlet.
     *   - PATCH: Verbo HTTP para actualizaciones parciales.
     * Para qué se usa (el propósito): Agregar soporte para modificaciones de rol o de estado parciales en el servlet.
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (req.getMethod().equalsIgnoreCase("PATCH")) {
            doPatch(req, resp);
        } else {
            super.service(req, resp);
        }
    }

    /*
     * Qué hace (la acción): Procesa las peticiones PATCH para suspender/reactivar cuentas, cambiar roles o aprobar solicitudes de voluntarios pendientes.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - change-status: Sub-endpoint para modificar el estado lógico del usuario (Activo/Inactivo).
     *   - role / rol: Sub-endpoint para asignar un nuevo rol_id (1 = Voluntario, 2 = Supervisor).
     *   - aprobar: Sub-endpoint para aprobar un registro pendiente activando su cuenta (estado = 1) y asignándole un rol.
     * Para qué se usa (el propósito): Cambiar de manera selectiva las propiedades administrativas del usuario y guardar los eventos de auditoría.
     * Por qué es importante (el impacto o problema que resuelve): Permite a los supervisores activar cuentas o cambiar roles ágilmente sin alterar el resto de los datos demográficos del usuario.
     */
    protected void doPatch(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        PrintWriter out = response.getWriter();

        // Qué hace: Recupera el pathInfo de la URL para analizar a qué sub-endpoint PATCH se llamó.
        // Por qué existe: Permite enrutar la petición a la acción adecuada (cambio de estado, rol o aprobación).
        // Qué problema resuelve: Habilita múltiples operaciones parciales en el mismo servlet.
        String pathInfo = request.getPathInfo();

        try {
            // Caso 1: Cambiar el estado de activación (Activo/Inactivo)
            // Ruta: /api/usuarios/{id}/change-status o /api/usuarios/{id}/cambiar-estado
            // Qué hace: Verifica si la ruta solicitada corresponde a la modificación del estado del usuario.
            // Por qué existe: Los supervisores y administradores necesitan poder suspender o reactivar cuentas.
            // Qué problema resuelve: Modifica la columna estado_usuario_id de un usuario en base de datos.
            if (pathInfo != null
                    && (pathInfo.matches("^/\\d+/change-status$") || pathInfo.matches("^/\\d+/cambiar-estado$"))) {
                // Qué hace: Obtiene el ID numérico del usuario a modificar desde el path.
                // Por qué existe: Sirve como parámetro identificador en el DAO.
                // Qué problema resuelve: Determina qué usuario sufrirá la modificación de estado.
                int userId = obtenerIdDePath(pathInfo);
                // Qué hace: Parsea el JSON del cuerpo de la petición.
                // Por qué existe: Recupera el nuevo id de estado enviado en el payload.
                // Qué problema resuelve: Lee el estado objetivo (1 = Activo, 2 = Inactivo, etc.).
                JSONObject body = JSONUtil.leerJson(request);
                int nuevoEstadoId = body.getInt("state_user_id");

                // Qué hace: Obtiene el estado anterior del usuario de la base de datos.
                // Por qué existe: Requerido para contrastar los cambios y registrar la auditoría.
                // Qué problema resuelve: Provee los datos previos antes de la modificación.
                Map<String, Object> usuarioAntes = usuarioDAO.obtenerUsuarioDetalleGestion(userId);
                if (usuarioAntes == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(
                            new JSONObject().put("success", false).put("message", "Usuario no encontrado.").toString());
                    return;
                }

                // Qué hace: Ejecuta el query UPDATE en base de datos para asignar el nuevo estado.
                // Por qué existe: Actualiza la columna de estado del usuario en la tabla usuarios.
                // Qué problema resuelve: Cambia persistentemente el estado del usuario en MySQL.
                usuarioDAO.actualizarEstado(userId, nuevoEstadoId);

                // Mapear nombres legibles de estado
                String estadoAntNom = (int) usuarioAntes.get("state_user_id") == 1 ? "Activo" : "Inactivo";
                String estadoNvoNom = nuevoEstadoId == 1 ? "Activo" : "Inactivo";

                String anteriorStr = "estado_id: " + usuarioAntes.get("state_user_id") + " (" + estadoAntNom + ")";
                String nuevoStr = "estado_id: " + nuevoEstadoId + " (" + estadoNvoNom + ")";

                // Registrar en historial de datos maestros
                // Qué hace: Escribe la modificación de estado en la bitácora de auditoría.
                // Por qué existe: Registra quién realizó el cambio y los estados viejo/nuevo.
                // Qué problema resuelve: Guarda el registro en la tabla de auditoría para fines de control administrativo.
                usuarioDAO.registrarAuditoria("usuarios", "UPDATE", anteriorStr, nuevoStr, userId);

                // Qué hace: Responde al cliente informando el éxito de la modificación.
                // Por qué existe: Retorna el estado en formato compatible para la UI.
                // Qué problema resuelve: Cierra la petición con JSON success true.
                out.print(new JSONObject()
                        .put("success", true)
                        .put("message", "Estado del usuario modificado con éxito.")
                        .toString());
            }
            // Caso 2: Cambiar el rol del usuario (Voluntario/Supervisor_Admin)
            // Ruta: /api/usuarios/role/{id} o /api/usuarios/rol/{id}
            // Qué hace: Verifica si la ruta solicita el cambio de rol del usuario.
            // Por qué existe: Permite ascender voluntarios a supervisores o degradar supervisores a voluntarios.
            // Qué problema resuelve: Modifica el rol_id del usuario en la base de datos.
            else if (pathInfo != null && (pathInfo.matches("^/role/\\d+$") || pathInfo.matches("^/rol/\\d+$"))) {
                // Qué hace: Extrae el ID del usuario objetivo.
                // Por qué existe: Indica qué registro de usuario será modificado.
                // Qué problema resuelve: Localiza el usuario en la consulta UPDATE.
                int userId = obtenerIdDePath(pathInfo);
                // Qué hace: Lee el cuerpo JSON del request.
                // Por qué existe: Recupera la cadena que define el rol deseado.
                // Qué problema resuelve: Obtiene el rol objetivo enviado por el frontend.
                JSONObject body = JSONUtil.leerJson(request);
                String rolTexto = body.getString("role");

                // Mapear el nombre del rol a ID de base de datos
                int nuevoRolId = (rolTexto.equalsIgnoreCase("Supervisor")
                        || rolTexto.equalsIgnoreCase("Supervisor_Admin")) ? 2 : 1;

                // Qué hace: Consulta el detalle actual del usuario antes del cambio.
                // Por qué existe: Permite generar la auditoría comparativa de roles.
                // Qué problema resuelve: Obtiene el rol anterior registrado.
                Map<String, Object> usuarioAntes = usuarioDAO.obtenerUsuarioDetalleGestion(userId);
                if (usuarioAntes == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(
                            new JSONObject().put("success", false).put("message", "Usuario no encontrado.").toString());
                    return;
                }

                // Qué hace: Actualiza la columna de rol en la tabla de usuarios.
                // Por qué existe: Aplica el cambio de privilegios en MySQL.
                // Qué problema resuelve: Cambia persistentemente el rol en la base de datos.
                usuarioDAO.actualizarRol(userId, nuevoRolId);

                String rolAntNom = (int) usuarioAntes.get("rol_id") == 1 ? "Voluntario" : "Supervisor_Admin";
                String rolNvoNom = nuevoRolId == 1 ? "Voluntario" : "Supervisor_Admin";

                String anteriorStr = "rol_id: " + usuarioAntes.get("rol_id") + " (" + rolAntNom + ")";
                String nuevoStr = "rol_id: " + nuevoRolId + " (" + rolNvoNom + ")";

                // Registrar en historial de datos maestros
                // Qué hace: Escribe la modificación de rol en la bitácora.
                // Por qué existe: Asegura trazabilidad sobre cambios de privilegios.
                // Qué problema resuelve: Mantiene registro de auditoría del cambio de rol.
                usuarioDAO.registrarAuditoria("usuarios", "UPDATE", anteriorStr, nuevoStr, userId);

                // Qué hace: Confirma al cliente que el rol se actualizó correctamente.
                // Por qué existe: Notificación estándar para la interfaz de administración.
                // Qué problema resuelve: Finaliza la operación PATCH con estado de éxito.
                out.print(new JSONObject()
                        .put("success", true)
                        .put("message", "Rol del usuario cambiado con éxito.")
                        .toString());
            }
            // Caso 3: Aprobar petición de usuario con selección de rol
            // Ruta: /api/usuarios/aprobar/{id}
            // Qué hace: Verifica si la ruta corresponde a la aprobación de un usuario pendiente.
            // Por qué existe: Permite aprobar las solicitudes de registro asignando el rol correspondiente.
            // Qué problema resuelve: Activa la cuenta del usuario (estado_id = 1) y le asigna su rol.
            else if (pathInfo != null && pathInfo.matches("^/aprobar/\\d+$")) {
                // Qué hace: Obtiene el ID del usuario a aprobar.
                // Por qué existe: Especifica la clave del usuario a activar.
                // Qué problema resuelve: Localiza el registro en la base de datos.
                int userId = obtenerIdDePath(pathInfo);
                // Qué hace: Lee el JSON del cuerpo de la petición.
                // Por qué existe: Permite recuperar el rol_id asignado a la nueva cuenta.
                // Qué problema resuelve: Obtiene el ID de rol (1 = Voluntario, 2 = Supervisor).
                JSONObject body = JSONUtil.leerJson(request);
                int rolId = body.optInt("rol_id", 1);

                Map<String, Object> usuarioAntes = usuarioDAO.obtenerUsuarioDetalleGestion(userId);
                if (usuarioAntes == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(
                            new JSONObject().put("success", false).put("message", "Usuario no encontrado.").toString());
                    return;
                }

                // Aprueba usuario: estado_id = 1 (Activo) + rol asignado
                // Qué hace: Ejecuta la consulta para cambiar el estado a activo y configurar el rol asignado.
                // Por qué existe: Consolida la aprobación en un solo paso en base de datos.
                // Qué problema resuelve: Activa el acceso del usuario en MySQL.
                usuarioDAO.aprobarUsuarioConRol(userId, rolId);

                // Mapear nombre legible del rol para la auditoría
                String rolNombre = rolId == 2 ? "Supervisor_Admin" : "Voluntario";
                String anteriorStr = "estado_id: 3 (Pendiente); rol_id: " + usuarioAntes.get("rol_id")
                        + " (Voluntario)";
                String nuevoStr = "estado_id: 1 (Activo); rol_id: " + rolId + " (" + rolNombre + ")";

                // Registrar en auditoría
                // Qué hace: Registra el evento de aprobación de alta como un UPDATE en la tabla de auditoría
                // Por qué existe: Documenta el evento de aprobación administrativa manteniendo compatibilidad con el ENUM('INSERT','UPDATE','DELETE') de la base de datos
                // Qué problema resuelve: Previene el error 'Data truncated for column accion' al usar la acción 'UPDATE' que es la soportada
                usuarioDAO.registrarAuditoria("usuarios", "UPDATE", anteriorStr, nuevoStr, userId);

                // Qué hace: Informa al cliente que la aprobación fue exitosa.
                // Por qué existe: Provee feedback visual a la UI de gestión de peticiones.
                // Qué problema resuelve: Finaliza exitosamente el proceso de alta.
                out.print(new JSONObject()
                        .put("success", true)
                        .put("message", "Usuario aprobado como " + rolNombre + " exitosamente.")
                        .toString());
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print(new JSONObject().put("success", false).put("message", "Ruta de modificación no soportada.")
                        .toString());
            }
        } catch (Exception e) {
            // Qué hace: Gestiona excepciones durante el proceso de PATCH y notifica al cliente.
            // Por qué existe: Previene la exposición de errores internos y permite manejo de fallos controlado.
            // Qué problema resuelve: Evita respuestas HTTP 500 crudas, retornando JSON estructurado.
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(new JSONObject()
                    .put("success", false)
                    .put("message", "Error al modificar usuario: " + e.getMessage())
                    .toString());
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doDelete para procesar la eliminación física de un registro de usuario de la base de datos SQL y dejar registro en la auditoría.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - usuarioDAO.eliminarUsuario(userId): Ejecuta la sentencia SQL DELETE FROM usuarios WHERE id = ?.
     * Para qué se usa (el propósito): Eliminar definitivamente del sistema las solicitudes de registro rechazadas o no deseadas.
     * Por qué es importante (el impacto o problema que resuelve): Limpia y libera espacio en la base de datos eliminando registros inconsistentes, salvaguardando antes una copia de auditoría de los datos borrados.
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        PrintWriter out = response.getWriter();

        // Qué hace: Recupera los segmentos adicionales de ruta (pathInfo) de la URL.
        // Por qué existe: Sirve para identificar el ID del usuario que se desea eliminar.
        // Qué problema resuelve: Provee el identificador del registro objetivo de forma RESTful.
        String pathInfo = request.getPathInfo();

        // Ruta esperada: /api/usuarios/{id}
        // Qué hace: Verifica mediante regex si la ruta de eliminación corresponde exactamente al ID del usuario.
        // Por qué existe: Limita las eliminaciones físicas únicamente al patrón de ruta establecido.
        // Qué problema resuelve: Previene ejecuciones accidentales sobre rutas erróneas.
        if (pathInfo != null && pathInfo.matches("^/\\d+$")) {
            try {
                // Qué hace: Extrae el identificador del usuario como un valor entero.
                // Por qué existe: Determina el ID primario del registro en la tabla de usuarios.
                // Qué problema resuelve: Provee la clave del usuario para las operaciones de búsqueda y borrado.
                int userId = Integer.parseInt(pathInfo.substring(1));

                // Verificar que el usuario exista antes de eliminarlo
                // Qué hace: Consulta si el registro del usuario existe en MySQL.
                // Por qué existe: Evita intentar eliminar un registro que ya no está presente o no existe.
                // Qué problema resuelve: Protege la consistencia de las llamadas del API.
                Map<String, Object> usuario = usuarioDAO.obtenerUsuarioDetalleGestion(userId);
                if (usuario == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(
                            new JSONObject().put("success", false).put("message", "Usuario no encontrado.").toString());
                    return;
                }

                // Registrar auditoría antes de eliminar para conservar trazabilidad
                // Qué hace: Serializa los datos básicos del usuario antes de proceder a borrarlo.
                // Por qué existe: Permite auditar qué información se eliminó permanentemente de la base de datos.
                // Qué problema resuelve: Conserva una copia textual del usuario borrado en el historial de auditoría.
                String valorAnterior = "nombre: " + usuario.get("names") + " " + usuario.get("last_names")
                        + "; email: " + usuario.get("email")
                        + "; estado_id: " + usuario.get("state_user_id");
                usuarioDAO.registrarAuditoria("usuarios", "DELETE", valorAnterior, "Registro eliminado", userId);

                // Ejecutar eliminación definitiva
                // Qué hace: Ejecuta la consulta SQL DELETE FROM usuarios WHERE id = ? en MySQL.
                // Por qué existe: Remueve físicamente el registro de la base de datos.
                // Qué problema resuelve: Limpia las peticiones rechazadas del almacenamiento del sistema.
                usuarioDAO.eliminarUsuario(userId);

                // Qué hace: Imprime la respuesta JSON confirmando la eliminación del usuario.
                // Por qué existe: Notifica al frontend que la operación fue completada con éxito.
                // Qué problema resuelve: Retorna success: true y el mensaje de confirmación correspondiente.
                out.print(new JSONObject()
                        .put("success", true)
                        .put("message", "Usuario eliminado correctamente.")
                        .toString());

            } catch (Exception e) {
                // Qué hace: Captura cualquier excepción y responde con error 400.
                // Por qué existe: Maneja fallos inesperados de conversión o de restricción de base de datos.
                // Qué problema resuelve: Retorna un JSON estructurado describiendo el fallo.
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(new JSONObject()
                        .put("success", false)
                        .put("message", "Error al eliminar usuario: " + e.getMessage())
                        .toString());
            }
        } else {
            // Qué hace: Establece estado HTTP 404 e informa que la ruta de borrado es inválida.
            // Por qué existe: Controla llamadas erróneas sobre la ruta DELETE del servlet.
            // Qué problema resuelve: Provee un mensaje claro en formato JSON.
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print(new JSONObject().put("success", false).put("message", "Ruta de eliminación no soportada.")
                    .toString());
        }
    }

    // ==========================================
    // MÉTODOS AUXILIARES Y DE RENDERIZACIÓN
    // ==========================================

    /*
     * Qué hace (la acción): Retorna todos los voluntarios registrados sin paginación en un JSONArray.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - usuarioDAO.listarVoluntariosTodos(): Obtiene la lista completa de voluntarios activos/inactivos de base de datos.
     * Para qué se usa (el propósito): Alimentar rejillas dinámicas o controles que requieran scroll infinito.
     */
    private void listarVoluntariosTodos(PrintWriter out) throws SQLException {
        // Obtiene la lista completa de voluntarios activos e inactivos
        List<Map<String, Object>> voluntarios = usuarioDAO.listarVoluntariosTodos();
        JSONArray arr = new JSONArray(voluntarios);
        // Envuelve en estructura estándar {success, data} para compatibilidad con
        // api.get() del frontend
        out.print(new JSONObject()
                .put("success", true)
                .put("data", arr)
                .toString());
    }

    /*
     * Qué hace (la acción): Carga de la base de datos la lista de voluntarios de forma paginada para la rejilla principal del panel del supervisor.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - offset: Punto de inicio calculado para la cláusula SQL LIMIT (ej. (current_page - 1) * limit).
     *   - paginate: Estructura JSON que detalla total de páginas, registros por página y la página actual.
     * Para qué se usa (el propósito): Visualizar segmentadamente los voluntarios para no sobrecargar el navegador de datos.
     */
    private void listarVoluntarios(HttpServletRequest request, PrintWriter out) throws SQLException {
        // Qué hace: Define la página actual por defecto en 1.
        // Por qué existe: Asegura que si no se envía página se muestren los primeros resultados.
        // Qué problema resuelve: Previene fallos ante la ausencia del parámetro "page" en la petición.
        int page = 1;
        if (request.getParameter("page") != null) {
            page = Integer.parseInt(request.getParameter("page"));
        }
        // Qué hace: Fija el límite de visualización en 6 registros por página.
        // Por qué existe: Es la cantidad de tarjetas que entran en la rejilla visual de la interfaz.
        // Qué problema resuelve: Estandariza el tamaño de página.
        int limit = 6;
        // Qué hace: Calcula el desplazamiento (offset) restando 1 a la página actual y multiplicando por el límite.
        // Por qué existe: Define a partir de qué fila MySQL debe empezar a retornar registros.
        // Qué problema resuelve: Provee el parámetro de inicio para la cláusula LIMIT ?, ? en SQL.
        int offset = (page - 1) * limit;

        // Qué hace: Obtiene el conteo total de voluntarios activos/inactivos en el sistema.
        // Por qué existe: Necesario para calcular el número total de páginas disponibles para el paginador.
        // Qué problema resuelve: Permite dibujar dinámicamente los botones de anterior/siguiente y páginas intermedias.
        int total = usuarioDAO.contarVoluntarios();
        // Qué hace: Ejecuta la consulta SQL con JOINs y paginación LIMIT/OFFSET.
        // Por qué existe: Recupera la porción de registros correspondientes a la página solicitada.
        // Qué problema resuelve: Retorna los voluntarios del segmento actual de base de datos.
        List<Map<String, Object>> voluntarios = usuarioDAO.listarVoluntariosPaginados(offset, limit);

        // Qué hace: Convierte la lista en un JSONArray de objetos JSON.
        // Por qué existe: Prepara la representación de datos para ser enviada por HTTP.
        // Qué problema resuelve: Adapta los tipos de Java a estructuras nativas JSON legibles por JavaScript.
        JSONArray arr = new JSONArray(voluntarios);
        // Qué hace: Calcula la última página redondeando hacia arriba el total dividido entre el límite.
        // Por qué existe: Define el límite del control de paginación del frontend.
        // Qué problema resuelve: Evita peticiones a páginas inexistentes controlando el fin de los datos.
        int lastPage = (int) Math.ceil((double) total / limit);
        if (lastPage < 1)
            lastPage = 1;

        // Qué hace: Estructura la metadata de paginación en un JSONObject.
        // Por qué existe: El componente de paginación del frontend requiere estos campos para renderizar los controles.
        // Qué problema resuelve: Agrupa los datos de control del paginador.
        JSONObject paginate = new JSONObject();
        paginate.put("total", total);
        paginate.put("per_page", limit);
        paginate.put("current_page", page);
        paginate.put("last_page", lastPage);

        // Qué hace: Envía la respuesta JSON estructurada con data y metadatos de paginación.
        // Por qué existe: Respeta la estructura de API estándar del proyecto.
        // Qué problema resuelve: Envía el cuerpo de respuesta serializado de voluntarios.
        out.print(new JSONObject()
                .put("success", true)
                .put("data", arr)
                .put("paginate", paginate)
                .toString());
    }

    /*
     * Qué hace (la acción): Carga de la base de datos la lista de solicitudes de registro pendientes de aprobación (en estado 3) de manera paginada.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - usuarioDAO.contarPeticionesVoluntarios(): Retorna la cantidad de usuarios pendientes de aprobación.
     * Para qué se usa (el propósito): Alimentar la bandeja de peticiones de nuevos usuarios en el panel del supervisor.
     */
    private void listarPeticiones(HttpServletRequest request, PrintWriter out) throws SQLException {
        // Qué hace: Inicializa la página en 1 por defecto.
        // Por qué existe: Asegura un valor por defecto si no se incluye el parámetro.
        // Qué problema resuelve: Controla el flujo ante la falta del parámetro de página.
        int page = 1;
        if (request.getParameter("page") != null) {
            page = Integer.parseInt(request.getParameter("page"));
        }
        int limit = 6;
        int offset = (page - 1) * limit;

        // Qué hace: Cuenta cuántos usuarios están con estado_id = 3 (Pendiente).
        // Por qué existe: Permite establecer el total para la paginación de la tabla de solicitudes.
        // Qué problema resuelve: Provee los metadatos de tamaño total de peticiones.
        int total = usuarioDAO.contarPeticionesVoluntarios();
        // Qué hace: Obtiene la lista de usuarios pendientes correspondientes al offset y limit.
        // Por qué existe: Trae los detalles de nombres, correo y fecha de solicitud de base de datos.
        // Qué problema resuelve: Genera la lista de elementos pendientes para la vista actual.
        List<Map<String, Object>> peticiones = usuarioDAO.listarPeticionesVoluntariosPaginados(offset, limit);

        JSONArray arr = new JSONArray(peticiones);
        int lastPage = (int) Math.ceil((double) total / limit);
        if (lastPage < 1)
            lastPage = 1;

        JSONObject paginate = new JSONObject();
        paginate.put("total", total);
        paginate.put("per_page", limit);
        paginate.put("current_page", page);
        paginate.put("last_page", lastPage);

        out.print(new JSONObject()
                .put("success", true)
                .put("data", arr)
                .put("paginate", paginate)
                .toString());
    }

    /*
     * Qué hace (la acción): Carga todos los usuarios del sistema (excepto superadministradores) de forma paginada para la consola del Administrador.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - usuarioDAO.contarUsuariosAdmin(): Cuenta los usuarios para el rol de administración.
     * Para qué se usa (el propósito): Alimentar el panel principal de administración de usuarios.
     */
    private void listarAdmin(HttpServletRequest request, PrintWriter out) throws SQLException {
        int page = 1;
        if (request.getParameter("page") != null) {
            page = Integer.parseInt(request.getParameter("page"));
        }
        int limit = 6;
        int offset = (page - 1) * limit;

        // Qué hace: Cuenta todos los usuarios mapeados en base de datos.
        // Por qué existe: Requerido para el cálculo de última página de la consola de administrador.
        // Qué problema resuelve: Determina las páginas totales.
        int total = usuarioDAO.contarUsuariosAdmin();
        // Qué hace: Recupera los usuarios paginados para la consola de administrador.
        // Por qué existe: Obtiene nombres, correo, roles y estados.
        // Qué problema resuelve: Proporciona la porción de datos del usuario.
        List<Map<String, Object>> usuarios = usuarioDAO.listarUsuariosAdminPaginados(offset, limit);

        JSONArray arr = new JSONArray(usuarios);
        int lastPage = (int) Math.ceil((double) total / limit);
        if (lastPage < 1)
            lastPage = 1;

        JSONObject paginate = new JSONObject();
        paginate.put("total", total);
        paginate.put("per_page", limit);
        paginate.put("current_page", page);
        paginate.put("last_page", lastPage);

        out.print(new JSONObject()
                .put("success", true)
                .put("data", arr)
                .put("paginate", paginate)
                .toString());
    }

    /*
     * Qué hace (la acción): Consulta a la base de datos el perfil detallado del usuario por su identificador y lo imprime en formato JSON de éxito.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - usuarioDAO.obtenerUsuarioDetalleGestion(id): Consulta SQL con JOIN a organizaciones y roles.
     * Para qué se usa (el propósito): Rellenar los formularios de visualización o edición del voluntario seleccionado en la UI.
     */
    private void obtenerFichaUsuario(int id, PrintWriter out) throws SQLException {
        // Qué hace: Ejecuta la consulta SQL con JOINs para obtener toda la información del usuario por su ID.
        // Por qué existe: Recupera la información unificada desde la base de datos de forma limpia.
        // Qué problema resuelve: Evita hacer múltiples llamadas, trayendo todo en un solo mapa de datos.
        Map<String, Object> usuario = usuarioDAO.obtenerUsuarioDetalleGestion(id);
        if (usuario == null) {
            out.print(new JSONObject().put("success", false).put("message", "Usuario inexistente.").toString());
        } else {
            out.print(new JSONObject().put("success", true).put("data", new JSONObject(usuario)).toString());
        }
    }

    /*
     * Qué hace (la acción): Consulta y escribe el historial de cambios del usuario seleccionado directamente en la respuesta como un JSONArray.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - usuarioDAO.obtenerHistorialUsuario(id): Consulta cronológica sobre la tabla 'historial_datos_maestros' para el usuario.
     * Para qué se usa (el propósito): Visualizar el historial de modificaciones del voluntario en la ventana de auditoría.
     */
    private void obtenerHistorial(int id, PrintWriter out) throws SQLException {
        // Qué hace: Realiza la consulta SQL sobre la tabla de auditoría filtrando por el ID de usuario involucrado.
        // Por qué existe: Recupera las bitácoras ordenadas de forma cronológica descendente.
        // Qué problema resuelve: Obtiene la lista de acciones de auditoría (tipo de cambio, anterior, nuevo, fecha).
        List<Map<String, Object>> historial = usuarioDAO.obtenerHistorialUsuario(id);
        JSONArray arr = new JSONArray(historial);
        // Qué hace: Imprime directamente la cadena del JSONArray.
        // Por qué existe: El controlador javascript en el frontend (historial.js) espera un array plano, no un objeto {success, data}.
        // Qué problema resuelve: Garantiza compatibilidad directa con los componentes del frontend existentes sin romperlos.
        out.print(arr.toString());
    }

    /*
     * Qué hace (la acción): Recorre el pathInfo de la URL extrayendo el primer número entero que sirva como ID.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - segment.matches("^\\d+$"): RegExp para validar que un segmento sea puramente numérico.
     * Para qué se usa (el propósito): Extraer el ID de usuario de rutas complejas REST (ej: "/role/5" -> 5).
     */
    private int obtenerIdDePath(String pathInfo) {
        // Separa el pathInfo por "/" y busca el segmento numérico
        String[] parts = pathInfo.split("/");
        for (String segment : parts) {
            // Qué hace: Compara si el segmento consiste enteramente en dígitos numéricos.
            // Por qué existe: Aísla el ID numérico de otros segmentos como "change-status" o "rol".
            // Qué problema resuelve: Retorna el ID numérico parseado a entero.
            if (segment.matches("^\\d+$")) {
                return Integer.parseInt(segment);
            }
        }
        throw new IllegalArgumentException("No se encontró ID numérico en la ruta.");
    }
}
