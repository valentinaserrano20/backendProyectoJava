package Controlador.Supervisor;

import Modelo.DAO.DatoMaestroDAO;
import Modelo.Utilidades.JSONUtil;
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

// Qué hace: Servlet centralizado para gestionar las operaciones CRUD de los 12 catálogos o datos maestros.
// Por qué existe: Concentra los endpoints REST paramétricos, permitiendo operaciones dinámicas con auditoría automática.
// Qué problema resuelve: Reemplaza la necesidad de tener 12 servlets individuales, respetando las convenciones JSON y CORS.
@WebServlet(urlPatterns = {
    "/api/sectors/*",
    "/api/organizations/*",
    "/api/sectionals/*",
    "/api/documentTypes/*",
    "/api/housingQualities/*",
    "/api/vulnerableQuestions/*",
    "/api/nationalities/*",
    "/api/threatTypes/*",
    "/api/species/*",
    "/api/resources/*",
    "/api/vulnerabilities/*"
})
public class DatosMaestrosServlet extends HttpServlet {

    private final DatoMaestroDAO dao = new DatoMaestroDAO();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Qué hace: Sobrescribe el método de servicio para interceptar peticiones PATCH.
        // Por qué existe: Java Servlet API estándar no tiene doPatch nativo, por lo que redirigimos manualmente.
        // Qué problema resuelve: Permite implementar actualizaciones parciales (PATCH) para modificar campos específicos o estados.
        String method = req.getMethod();
        if (method.equalsIgnoreCase("PATCH")) {
            doPatch(req, resp);
        } else {
            super.service(req, resp);
        }
    }

    // Qué hace: Obtiene un listado general de una entidad o un registro por su ID o el historial de auditoría de un registro.
    // Por qué existe: Atiende todas las solicitudes HTTP GET entrantes a las rutas mapeadas de datos maestros.
    // Qué problema resuelve: Centraliza las consultas de lectura de las 12 tablas paramétricas en un único punto.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // Qué hace: Establece el tipo de contenido de la respuesta HTTP como JSON.
        // Por qué existe: Indica al cliente (frontend) que recibirá datos estructurados en formato JSON.
        // Qué problema resuelve: Previene que el navegador web del cliente interprete la respuesta como texto plano o HTML.
        response.setContentType("application/json");
        // Qué hace: Configura la codificación de caracteres de la respuesta en UTF-8.
        // Por qué existe: Asegura que caracteres especiales como acentos y eñes se transmitan correctamente sin corromperse.
        // Qué problema resuelve: Evita problemas de visualización de texto e inconsistencia de codificación en el cliente.
        response.setCharacterEncoding("UTF-8");
        // Qué hace: Obtiene el objeto de escritura PrintWriter para componer el cuerpo de la respuesta.
        // Por qué existe: Permite enviar texto o datos serializados en JSON de vuelta en la respuesta de la petición HTTP.
        // Qué problema resuelve: Habilita el canal de comunicación de salida para responder al cliente.
        PrintWriter out = response.getWriter();

        // Qué hace: Llama al método interno obtenerEntidad para extraer el nombre de la tabla paramétrica solicitada del URL.
        // Por qué existe: Permite determinar dinámicamente con cuál de los 12 catálogos se debe interactuar.
        // Qué problema resuelve: Evita codificar rutas estáticas individuales para cada entidad paramétrica.
        String entity = obtenerEntidad(request);
        // Qué hace: Recupera el pathInfo de la URL para analizar si se enviaron IDs o subrutas adicionales en la petición.
        // Por qué existe: Permite identificar si la solicitud es para un registro específico, historial o paginación.
        // Qué problema resuelve: Provee la información de enrutamiento interno secundario dentro de la entidad seleccionada.
        String pathInfo = request.getPathInfo();

        // Qué hace: Bandera booleana para decidir si se omite la validación de rol de supervisor administrador.
        // Por qué existe: Ciertos endpoints como el listado de preguntas de vulnerabilidad deben estar disponibles para voluntarios.
        // Qué problema resuelve: Evita bloquear las peticiones legítimas de voluntarios al consultar las preguntas del censo.
        boolean bypassRole = false;
        // Qué hace: Verifica si la entidad solicitada corresponde al catálogo de preguntas de vulnerabilidad.
        // Por qué existe: Es el único catálogo al que los voluntarios tienen permiso de lectura para llenar las fichas.
        // Qué problema resuelve: Limita los accesos abiertos exclusivamente a los datos estrictamente necesarios para el censo.
        if (entity.equalsIgnoreCase("vulnerableQuestions")) {
            // Qué hace: Si no hay ID en la ruta o se solicita explícitamente la paginación de preguntas.
            // Por qué existe: Valida los patrones de ruta de lectura requeridos por el rol voluntario.
            // Qué problema resuelve: Permite la lectura general o paginada saltando la restricción de supervisor.
            if (pathInfo == null || pathInfo.equals("/paginate")) {
                bypassRole = true;
            }
        }

        // Qué hace: Evalúa si se requiere validar el rol del usuario actual.
        // Por qué existe: La mayoría de operaciones en datos maestros requieren que el usuario sea supervisor administrador.
        // Qué problema resuelve: Previene el acceso no autorizado a catálogos sensibles que no sean las preguntas del censo.
        if (!bypassRole) {
            // Qué hace: Invoca la función validarRol para verificar autenticación y rol_id = 2.
            // Por qué existe: Verifica que el usuario tenga privilegios activos antes de procesar la consulta.
            // Qué problema resuelve: Si falla, detiene la ejecución del servlet inmediatamente.
            if (!validarRol(request, response)) {
                return;
            }
        } else {
            // Qué hace: Si se omitió el rol, comprueba que al menos exista una sesión HTTP iniciada por cualquier usuario.
            // Por qué existe: Asegura que el cliente sea un usuario autenticado del sistema (voluntario o supervisor).
            // Qué problema resuelve: Bloquea el acceso a personas externas o sesiones expiradas que intenten consultar la API.
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("usuarioId") == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
                return;
            }
        }

        try {
            // Caso 0: Paginación de preguntas de vulnerabilidad para el voluntario
            // Qué hace: Comprueba si la solicitud pide paginar las preguntas de vulnerabilidad.
            // Por qué existe: Permite al voluntario cargar el formulario del censo por bloques de preguntas.
            // Qué problema resuelve: Optimiza la carga de datos del censo evitando enviar todas las preguntas juntas.
            if (entity.equalsIgnoreCase("vulnerableQuestions") && pathInfo != null && pathInfo.equals("/paginate")) {
                int page = 1;
                // Qué hace: Obtiene el parámetro de consulta 'page' desde el URI de la petición.
                // Por qué existe: Permite saber cuál página de registros desea visualizar el cliente.
                // Qué problema resuelve: Define el offset de inicio para el cálculo de la consulta SQL.
                String pageStr = request.getParameter("page");
                if (pageStr != null) {
                    try {
                        page = Integer.parseInt(pageStr);
                    } catch (NumberFormatException e) {
                        page = 1;
                    }
                }
                // Qué hace: Instancia el servicio de vulnerabilidades y obtiene el listado paginado en formato JSON.
                // Por qué existe: Encapsula la lógica de negocio y paginación fuera del controlador servlet.
                // Qué problema resuelve: Mantiene el controlador delgado delegando la consulta compleja al servicio.
                Modelo.Servicios.Voluntario.VulnerabilidadServicio vulnServ = new Modelo.Servicios.Voluntario.VulnerabilidadServicio();
                out.print(vulnServ.obtenerPreguntasPaginadas(page, 3));
                return;
            }

            // Caso 1: Obtener Historial de un registro específico
            // Ruta: /api/{entity}/{id}/history o /api/{entity}/{id}/historial
            // Qué hace: Evalúa si la subruta de la URL coincide con una petición de historial de auditoría para un ID numérico.
            // Por qué existe: Permite al supervisor ver el registro histórico de cambios (auditoría) de un dato maestro.
            // Qué problema resuelve: Facilita el rastreo de modificaciones, inserciones y eliminaciones de un registro en particular.
            if (pathInfo != null && pathInfo.matches("^/\\d+/(historial|history)/?$")) {
                // Qué hace: Extrae el ID entero contenido en la URL a través del helper obtenerIdDePath.
                // Por qué existe: Obtiene la llave primaria del registro para filtrar los registros de auditoría en MySQL.
                // Qué problema resuelve: Provee la variable de vinculación para la consulta a la tabla de auditoría.
                int id = obtenerIdDePath(pathInfo);
                // Qué hace: Llama al DAO para obtener la lista de mapas que contienen los registros históricos.
                // Por qué existe: Ejecuta la consulta SQL con JOIN de usuarios sobre la tabla de auditoría correspondiente.
                // Qué problema resuelve: Recupera quién modificó, cuándo y cuáles eran los valores anteriores de la entidad.
                List<Map<String, Object>> hist = dao.obtenerHistorial(entity, id);
                // Qué hace: Envía la lista de auditoría estructurada en un objeto JSON con estado de éxito.
                // Por qué existe: Entrega la respuesta estructurada esperada por el controlador javascript en el frontend.
                // Qué problema resuelve: Mantiene la uniformidad en el esquema de respuestas JSON de la API.
                out.print(new JSONObject()
                        .put("success", true)
                        .put("data", new JSONArray(hist))
                        .toString());
                return;
            }

            // Caso 2: Obtener un registro por ID
            // Ruta: /api/{entity}/{id}
            // Qué hace: Evalúa si la URL corresponde exactamente a un ID numérico único.
            // Por qué existe: Permite consultar la información detallada de un único elemento paramétrico.
            // Qué problema resuelve: Habilita la consulta individual necesaria para precargar formularios de edición.
            if (pathInfo != null && pathInfo.matches("^/\\d+/?$")) {
                // Qué hace: Obtiene la clave primaria numérica del registro a buscar.
                // Por qué existe: Sirve como parámetro de filtrado id = ? en la consulta SQL.
                // Qué problema resuelve: Identifica el registro exacto a retornar desde la base de datos.
                int id = obtenerIdDePath(pathInfo);
                // Qué hace: Consulta al DAO el mapa con las columnas de la fila del registro.
                // Por qué existe: Ejecuta la consulta select parametrizada por ID.
                // Qué problema resuelve: Obtiene de forma limpia los campos del registro (nombre, descripción, estado, etc.).
                Map<String, Object> item = dao.obtenerPorId(entity, id);
                if (item != null) {
                    out.print(new JSONObject()
                            .put("success", true)
                            .put("data", new JSONObject(item))
                            .toString());
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(new JSONObject().put("success", false).put("message", "Registro no encontrado.").toString());
                }
                return;
            }

            // Caso 3: Listar todos
            // Ruta: /api/{entity} o /api/{entity}/
            // Qué hace: Si se solicita el catálogo de preguntas de vulnerabilidad y no coincide con otras subrutas.
            // Por qué existe: Devuelve la lista completa de preguntas activas sin paginar si es solicitado.
            // Qué problema resuelve: Provee acceso alternativo al listado completo de vulnerabilidades.
            if (entity.equalsIgnoreCase("vulnerableQuestions") && (pathInfo == null || !pathInfo.equals("/"))) {
                Modelo.Servicios.Voluntario.VulnerabilidadServicio vulnServ = new Modelo.Servicios.Voluntario.VulnerabilidadServicio();
                out.print(vulnServ.obtenerPreguntas());
                return;
            }

            // Qué hace: Obtiene la lista completa de registros de la entidad paramétrica actual desde la base de datos.
            // Por qué existe: Ejecuta la consulta SELECT * FROM {tabla} a través del DAO.
            // Qué problema resuelve: Muestra los registros activos e inactivos en las tablas de mantenimiento del supervisor.
            List<Map<String, Object>> lista = dao.listar(entity);
            // Qué hace: Genera e imprime el JSONArray conteniendo la lista de registros de la consulta.
            // Por qué existe: Devuelve los datos en formato compatible para la renderización de tablas en JS vanilla.
            // Qué problema resuelve: Envía el cuerpo de respuesta en formato JSON limpio con éxito = true.
            out.print(new JSONObject()
                    .put("success", true)
                    .put("data", new JSONArray(lista))
                    .toString());

        } catch (Exception e) {
            // Qué hace: Registra la traza del error en la consola del servidor.
            // Por qué existe: Permite diagnosticar errores en base de datos o fallos de casting.
            // Qué problema resuelve: Evita que el servlet falle silenciosamente sin reportar información del error.
            e.printStackTrace();
            // Qué hace: Configura el código de estado de respuesta HTTP en 500 (Error Interno).
            // Por qué existe: Informa al cliente que ocurrió un error no controlado en el servidor.
            // Qué problema resuelve: Responde formalmente con estructura JSON informando el tipo de error.
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(new JSONObject().put("success", false).put("message", "Error al consultar datos: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Crea un nuevo registro en la entidad paramétrica correspondiente.
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Qué hace: Valida si el usuario actual cumple con el rol requerido de Supervisor (rol_id = 2).
        // Por qué existe: Asegura que el creador del registro tenga los privilegios necesarios antes de guardar en base de datos.
        // Qué problema resuelve: Protege la creación de datos maestros de modificaciones de usuarios no autorizados.
        if (!validarRol(request, response)) {
            // Qué hace: Retorna del método suspendiendo la ejecución de la petición.
            // Por qué existe: Detiene el flujo de registro si falló la validación del rol.
            // Qué problema resuelve: Evita la inserción de registros fraudulentos o no autorizados.
            return;
        }

        // Qué hace: Obtiene la sesión HTTP actual del cliente.
        // Por qué existe: Permite acceder a los datos persistidos de sesión del usuario.
        // Qué problema resuelve: Permite recuperar el identificador del operador autenticado de forma segura.
        HttpSession session = request.getSession(false);

        int operatorId = (int) session.getAttribute("usuarioId");
        String entity = obtenerEntidad(request);

        try {
            JSONObject body = JSONUtil.leerJson(request);
            int newId = dao.crear(entity, body);
            
            if (newId != -1) {
                // Registrar en la auditoría
                dao.registrarAuditoria(entity, "INSERT", null, "id: " + newId + "; " + formatJsonBody(body), operatorId);
                
                out.print(new JSONObject()
                        .put("success", true)
                        .put("message", "Registro creado correctamente.")
                        .put("data", new JSONObject().put("id", newId))
                        .toString());
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(new JSONObject().put("success", false).put("message", "No se pudo crear el registro.").toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(new JSONObject().put("success", false).put("message", "Error al crear registro: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Modifica campos específicos de un registro o cambia su estado lógico de activación.
    protected void doPatch(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Qué hace: Valida si el usuario actual tiene privilegios de administrador para realizar modificaciones.
        // Por qué existe: Restringe las operaciones de actualización de datos paramétricos al rol_id = 2.
        // Qué problema resuelve: Previene la alteración fraudulenta o accidental del catálogo por parte de otros usuarios.
        if (!validarRol(request, response)) {
            // Qué hace: Retorna del método impidiendo continuar con el procesamiento.
            // Por qué existe: Detiene la ejecución en caso de que la validación resulte fallida.
            // Qué problema resuelve: Asegura la integridad del catálogo bloqueando la actualización.
            return;
        }

        // Qué hace: Obtiene la sesión HTTP del cliente de forma no constructiva.
        // Por qué existe: Permite leer los datos almacenados de la sesión autenticada.
        // Qué problema resuelve: Facilita la recuperación del id del supervisor para la auditoría de cambios.
        HttpSession session = request.getSession(false);

        int operatorId = (int) session.getAttribute("usuarioId");
        String entity = obtenerEntidad(request);
        String pathInfo = request.getPathInfo();

        try {
            // Caso 1: Actualizar Estado (Activo/Inactivo)
            // Ruta: /api/{entity}/status/{id}
            if (pathInfo != null && pathInfo.contains("/status/")) {
                int id = obtenerIdDePath(pathInfo);
                JSONObject body = JSONUtil.leerJson(request);
                int nuevoEstado = body.getInt("is_active");
                boolean activo = (nuevoEstado == 1);

                Map<String, Object> oldItem = dao.obtenerPorId(entity, id);
                if (oldItem == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(new JSONObject().put("success", false).put("message", "Registro no encontrado.").toString());
                    return;
                }

                dao.cambiarEstado(entity, id, activo);

                boolean oldActive = oldItem.get("is_active") != null ? (boolean) oldItem.get("is_active") : false;
                String oldActiveStr = oldActive ? "Activo" : "Inactivo";
                String newActiveStr = activo ? "Activo" : "Inactivo";

                dao.registrarAuditoria(entity, "UPDATE", "id: " + id + "; activo: " + oldActiveStr, "id: " + id + "; activo: " + newActiveStr, operatorId);

                out.print(new JSONObject()
                        .put("success", true)
                        .put("message", "Estado modificado exitosamente.")
                        .toString());
                return;
            }

            // Caso 2: Actualizar campos generales
            // Ruta: /api/{entity}/{id}
            if (pathInfo != null && pathInfo.matches("^/\\d+/?$")) {
                int id = obtenerIdDePath(pathInfo);
                JSONObject body = JSONUtil.leerJson(request);

                Map<String, Object> oldItem = dao.obtenerPorId(entity, id);
                if (oldItem == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(new JSONObject().put("success", false).put("message", "Registro no encontrado.").toString());
                    return;
                }

                dao.actualizar(entity, id, body);

                dao.registrarAuditoria(entity, "UPDATE", "id: " + id + "; " + formatMap(oldItem), "id: " + id + "; " + formatJsonBody(body), operatorId);

                out.print(new JSONObject()
                        .put("success", true)
                        .put("message", "Registro actualizado correctamente.")
                        .toString());
                return;
            }

            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print(new JSONObject().put("success", false).put("message", "Ruta de modificación no soportada.").toString());

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(new JSONObject().put("success", false).put("message", "Error al actualizar registro: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Elimina físicamente un registro verificando dependencias FK primero.
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Qué hace: Valida el rol de supervisor administrativo del usuario autenticado.
        // Por qué existe: Limita la eliminación física de registros paramétricos únicamente a operadores autorizados.
        // Qué problema resuelve: Evita la pérdida irrecuperable de datos maestros por acciones de usuarios sin privilegios.
        if (!validarRol(request, response)) {
            // Qué hace: Interrumpe la petición retornando del servlet.
            // Por qué existe: Detiene la ejecución para no proceder con la eliminación.
            // Qué problema resuelve: Garantiza que no se eliminen datos de forma no autorizada.
            return;
        }

        // Qué hace: Accede a la sesión de usuario activa actual.
        // Por qué existe: Provee acceso a los atributos asociados del cliente logueado.
        // Qué problema resuelve: Permite recuperar el identificador del operador para registrar la baja en auditoría.
        HttpSession session = request.getSession(false);

        int operatorId = (int) session.getAttribute("usuarioId");
        String entity = obtenerEntidad(request);
        String pathInfo = request.getPathInfo();

        if (pathInfo != null && pathInfo.matches("^/\\d+/?$")) {
            try {
                int id = obtenerIdDePath(pathInfo);

                Map<String, Object> oldItem = dao.obtenerPorId(entity, id);
                if (oldItem == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(new JSONObject().put("success", false).put("message", "Registro no encontrado.").toString());
                    return;
                }

                dao.eliminar(entity, id);

                dao.registrarAuditoria(entity, "DELETE", "id: " + id + "; " + formatMap(oldItem), "Registro eliminado", operatorId);

                out.print(new JSONObject()
                        .put("success", true)
                        .put("message", "Registro eliminado correctamente.")
                        .toString());

            } catch (SQLException e) {
                // Capturar el mensaje personalizado del validador de FK
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(new JSONObject().put("success", false).put("message", e.getMessage()).toString());
            } catch (Exception e) {
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(new JSONObject().put("success", false).put("message", "Error al eliminar registro: " + e.getMessage()).toString());
            }
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print(new JSONObject().put("success", false).put("message", "Ruta de eliminación no soportada.").toString());
        }
    }

    // Qué hace: Verifica si el usuario autenticado tiene el rol de supervisor administrador (rol_id = 2).
    // Por qué existe: Restringe el acceso al panel de datos maestros solo a personal autorizado.
    // Qué problema resuelve: Previene que usuarios sin privilegios (como voluntarios) accedan o alteren información paramétrica crítica.
    private boolean validarRol(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Qué hace: Obtiene la sesión HTTP actual sin crear una nueva si no existe.
        // Por qué existe: Permite comprobar si el cliente tiene una sesión de usuario válida activa.
        // Qué problema resuelve: Evita peticiones anónimas al validar el estado de autenticación de forma segura.
        HttpSession session = request.getSession(false);
        // Qué hace: Evalúa si la sesión es nula o si el atributo del ID de usuario no está registrado en el contexto.
        // Por qué existe: Asegura que el usuario tenga un identificador válido asignado durante el login.
        // Qué problema resuelve: Deniega el acceso inmediatamente si la sesión expiró o nunca fue creada.
        if (session == null || session.getAttribute("usuarioId") == null) {
            // Qué hace: Modifica el código de estado HTTP de la respuesta a 401 (No autorizado).
            // Por qué existe: Cumple con el estándar REST para indicar que se requiere autenticación para el recurso.
            // Qué problema resuelve: Informa al cliente que debe redirigir a la pantalla de inicio de sesión.
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            // Qué hace: Escribe un mensaje JSON estructurado con el estado de fallo y el motivo de rechazo.
            // Por qué existe: Envía la respuesta en formato compatible para que el frontend la muestre en pantalla.
            // Qué problema resuelve: Evita respuestas genéricas o vacías que rompan el flujo de la aplicación.
            response.getWriter().print(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            // Qué hace: Retorna falso indicando que la validación de rol no es exitosa.
            // Por qué existe: Detiene el flujo de ejecución del servlet inmediatamente.
            // Qué problema resuelve: Previene la ejecución de consultas y lógica posterior sin privilegios.
            return false;
        }
        
        // Qué hace: Recupera el ID numérico del usuario almacenado en los atributos de sesión.
        // Por qué existe: Permite buscar los detalles y rol real de este usuario específico en la base de datos.
        // Qué problema resuelve: Provee la llave primaria necesaria para la consulta SQL en la tabla usuarios.
        int userId = (int) session.getAttribute("usuarioId");
        try {
            // Qué hace: Instancia la clase de acceso a datos de usuario para consultar MySQL.
            // Por qué existe: Permite acceder a la base de datos de forma desacoplada respetando la arquitectura DAO del proyecto.
            // Qué problema resuelve: Evita lógica SQL embebida directamente en el controlador servlet.
            Modelo.DAO.UsuarioDAO usuarioDao = new Modelo.DAO.UsuarioDAO();
            // Qué hace: Obtiene el objeto Entidad Usuario correspondiente al identificador recuperado de la sesión.
            // Por qué existe: Recupera la información de rol asignada a nivel de persistencia de forma actualizada.
            // Qué problema resuelve: Garantiza que los permisos coincidan con el estado actual en la base de datos.
            Modelo.Entidades.Usuario usuario = usuarioDao.obtenerPorId(userId);
            // Qué hace: Valida si el usuario existe y si su rol corresponde exactamente al ID de Supervisor (rol_id = 2).
            // Por qué existe: Es la regla de negocio que unifica los roles administrativos bajo el ID número 2.
            // Qué problema resuelve: Rechaza el acceso a usuarios activos que tengan otros roles (ej. voluntarios con rol_id = 1).
            if (usuario == null || usuario.getRolId() != 2) {
                // Qué hace: Modifica el código de estado HTTP de la respuesta a 403 (Prohibido).
                // Por qué existe: Indica de manera estándar que el usuario está autenticado pero no tiene permisos suficientes.
                // Qué problema resuelve: Protege las operaciones críticas de administración frente a otros usuarios del sistema.
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                // Qué hace: Retorna una respuesta JSON estructurada con el motivo específico de la denegación de permisos.
                // Por qué existe: Facilita al frontend la visualización de la alerta correspondiente en la interfaz del usuario.
                // Qué problema resuelve: Evita confusión entre no estar autenticado y no tener permisos de acceso.
                response.getWriter().print(new JSONObject().put("success", false).put("message", "Acceso denegado. Permisos insuficientes.").toString());
                // Qué hace: Retorna falso para detener la ejecución de la petición.
                // Por qué existe: Finaliza el procesamiento del servlet de forma controlada.
                // Qué problema resuelve: Asegura la integridad previniendo accesos no autorizados a la lógica del servlet.
                return false;
            }
            // Qué hace: Retorna verdadero si el usuario está autenticado y tiene rol de Supervisor_Admin.
            // Por qué existe: Permite continuar con el procesamiento normal de la solicitud HTTP en el servlet.
            // Qué problema resuelve: Otorga acceso seguro a las funciones del catálogo de datos maestros.
            return true;
        } catch (SQLException e) {
            // Qué hace: Imprime la traza de la excepción SQL ocurrida en la consola del servidor.
            // Por qué existe: Ayuda al desarrollador a depurar problemas de conexión o consultas en base de datos.
            // Qué problema resuelve: Evita la pérdida del mensaje de error real para propósitos de mantenimiento.
            e.printStackTrace();
            // Qué hace: Configura el código de respuesta de error interno del servidor (500).
            // Por qué existe: Reporta un fallo en el backend que impidió procesar la petición con normalidad.
            // Qué problema resuelve: Maneja de manera limpia las excepciones de base de datos impidiendo caídas del servidor.
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            // Qué hace: Envía una respuesta de fallo con un mensaje claro del error en formato JSON.
            // Por qué existe: Mantiene el formato estándar de respuesta del servlet en caso de excepciones.
            // Qué problema resuelve: Entrega retroalimentación estructurada al frontend en situaciones de error de conexión.
            response.getWriter().print(new JSONObject().put("success", false).put("message", "Error interno al validar permisos.").toString());
            // Qué hace: Retorna falso previniendo que continúe la operation.
            // Por qué existe: Protege el endpoint cancelando el flujo ante cualquier error de validación.
            // Qué problema resuelve: Previene ejecuciones indeterminadas o inseguras ante fallos de conexión a BD.
            return false;
        }
    }

    // =========================================================================
    // HELPERS INTERNOS
    // =========================================================================

    // Qué hace: Obtiene la última sección de la ruta de acceso de la solicitud (ServletPath) como el nombre de la entidad.
    // Por qué existe: Mapea la petición REST paramétrica al nombre del recurso correspondiente (ej. /api/sectors -> sectors).
    // Qué problema resuelve: Permite saber cuál de los 12 catálogos maestros es el destinatario de la acción sin condicionales rígidos.
    private String obtenerEntidad(HttpServletRequest request) {
        // Qué hace: Recupera el path de mapeo del servlet dentro de la URL de la petición.
        // Por qué existe: Obtiene la ruta relativa mapeada (ej. /api/sectors).
        // Qué problema resuelve: Aísla el fragmento de URL de los parámetros de consulta adicionales.
        String servletPath = request.getServletPath();
        // Qué hace: Corta la cadena desde la última aparición de la barra diagonal ("/") hasta el final del string.
        // Por qué existe: Extrae únicamente el nombre de la entidad (ej. "sectors").
        // Qué problema resuelve: Convierte la ruta de URL en un identificador limpio de entidad/tabla paramétrica.
        return servletPath.substring(servletPath.lastIndexOf("/") + 1);
    }

    // Qué hace: Analiza el pathInfo de la URL para buscar y extraer el primer ID entero que encuentre.
    // Por qué existe: Permite identificar la clave primaria del registro en rutas RESTful (ej. /api/sectors/15 -> 15).
    // Qué problema resuelve: Facilita la recuperación del ID de registro de forma robusta e independiente de barras adicionales.
    private int obtenerIdDePath(String pathInfo) {
        // Qué hace: Retorna -1 inmediatamente si el pathInfo es nulo (no contiene segmentos de ruta).
        // Por qué existe: Previene excepciones de puntero nulo al manipular la cadena.
        // Qué problema resuelve: Controla de forma segura rutas que no especifican sub-recursos.
        if (pathInfo == null) return -1;
        // Qué hace: Divide la ruta en segmentos usando la barra diagonal como separador.
        // Por qué existe: Aísla cada elemento del path para su inspección individual.
        // Qué problema resuelve: Convierte la ruta en un arreglo indexable de fragmentos de texto.
        String[] parts = pathInfo.split("/");
        // Qué hace: Itera sobre cada segmento buscando el primer elemento que esté compuesto únicamente por dígitos.
        // Por qué existe: Localiza el componente numérico que representa el ID primario en la base de datos.
        // Qué problema resuelve: Evita confundir otros componentes de ruta de texto (ej. "historial", "status") con el ID de registro.
        for (String part : parts) {
            // Qué hace: Utiliza una expresión regular para verificar si la cadena actual contiene solo dígitos.
            // Por qué existe: Confirma que la parte es casteable a un número entero válido.
            // Qué problema resuelve: Evita excepciones NumberFormatException en tiempo de ejecución.
            if (part.matches("\\d+")) {
                // Qué hace: Convierte el segmento numérico en un entero de Java.
                // Por qué existe: Retorna el ID de la base de datos parseado.
                // Qué problema resuelve: Entrega la clave numérica lista para las consultas preparadas SQL.
                return Integer.parseInt(part);
            }
        }
        // Qué hace: Retorna -1 si no se localizó ningún segmento completamente numérico.
        // Por qué existe: Señala que la petición no apuntaba a ningún ID específico.
        // Qué problema resuelve: Provee un valor centinela estándar de fallo de ID.
        return -1;
    }

    // Qué hace: Formatea las claves y valores de un JSONObject en una sola cadena de texto descriptiva.
    // Por qué existe: Permite serializar los campos enviados por el cliente para el registro de auditoría en base de datos.
    // Qué problema resuelve: Facilita almacenar los nuevos valores ingresados en un único campo de texto plano de la tabla de auditoría.
    private String formatJsonBody(JSONObject body) {
        // Qué hace: Instancia StringBuilder para concatenar las claves y valores eficientemente.
        // Por qué existe: Evita crear múltiples objetos String innecesarios en memoria durante la concatenación en bucle.
        // Qué problema resuelve: Optimiza el consumo de recursos de memoria durante operaciones intensivas de string.
        StringBuilder sb = new StringBuilder();
        // Qué hace: Itera sobre el conjunto de nombres de propiedades (keys) presentes en el objeto JSON recibido.
        // Por qué existe: Procesa dinámicamente cualquier campo enviado en el cuerpo de la petición.
        // Qué problema resuelve: Soporta esquemas variables de los 12 catálogos paramétricos en una sola rutina de formato.
        for (String key : body.keySet()) {
            // Qué hace: Añade el nombre del campo, un separador de dos puntos, su valor y finaliza con punto y coma.
            // Por qué existe: Estructura la cadena para que sea fácil de leer en las bitácoras de auditoría de la base de datos.
            // Qué problema resuelve: Genera una representación textual legible de los datos para monitoreo administrativo.
            sb.append(key).append(": ").append(body.get(key)).append("; ");
        }
        // Qué hace: Retorna la cadena completa concatenada.
        // Por qué existe: Entrega el string serializado al método de auditoría.
        // Qué problema resuelve: Provee el contenido del campo "valor_nuevo" en el log de auditoría.
        return sb.toString();
    }

    // Qué hace: Formatea un mapa de datos proveniente de la base de datos en una cadena de texto, omitiendo la clave 'id'.
    // Por qué existe: Permite describir el estado anterior (old state) de un registro antes de ser modificado en auditoría.
    // Qué problema resuelve: Crea una cadena descriptiva con los valores viejos listos para ser guardados en la tabla de auditoría.
    private String formatMap(Map<String, Object> map) {
        // Qué hace: Inicializa StringBuilder para armar el texto del estado anterior del registro.
        // Por qué existe: Facilita la concatenación en bucle de los atributos originales del elemento.
        // Qué problema resuelve: Ahorra operaciones costosas de creación de Strings en memoria.
        StringBuilder sb = new StringBuilder();
        // Qué hace: Itera sobre cada par clave-valor en el conjunto de entradas del mapa de base de datos.
        // Por qué existe: Recorre dinámicamente todas las columnas devueltas por la consulta SELECT del DAO.
        // Qué problema resuelve: Permite formatear cualquier fila de las 12 tablas paramétricas sin codificación dura de nombres de columna.
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            // Qué hace: Verifica que la clave de la columna actual no sea la clave primaria "id".
            // Por qué existe: El ID ya se registra de manera independiente en la auditoría, por lo que es redundante incluirlo aquí.
            // Qué problema resuelve: Simplifica la cadena centrándose exclusivamente en los campos de datos informativos.
            if (!entry.getKey().equals("id")) {
                // Qué hace: Concatena la columna, su valor previo recuperado y un delimitador.
                // Por qué existe: Construye la representación lineal de la fila vieja.
                // Qué problema resuelve: Provee el contenido textual exacto para la columna "valor_anterior" de la base de datos de auditoría.
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("; ");
            }
        }
        // Qué hace: Devuelve la representación formateada del mapa como un String.
        // Por qué existe: Provee el detalle del estado original antes del cambio de datos.
        // Qué problema resuelve: Envía el string de valores antiguos para guardar el registro de auditoría.
        return sb.toString();
    }
}
