package Controlador.Supervisor;

/*
 * Qué hace (la acción): Importa la capa DAO para datos maestros, utilidades para procesar JSON y sesiones, APIs del servlet de Jakarta y excepciones del controlador SQL.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.DAO.DatoMaestroDAO: Objeto de acceso a datos para realizar operaciones CRUD (crear, leer, actualizar, eliminar) en las tablas de catálogos paramétricos.
 *   - Modelo.Utilidades.SessionUtil: Clase de utilidad para validar la identidad y el rol de supervisor.
 *   - Modelo.Utilidades.JSONUtil: Utilidad que lee el cuerpo de la petición HTTP y lo parsea a un JSONObject.
 * Para qué se usa (el propósito): Proveer las dependencias necesarias para que el servlet controle las 12 tablas paramétricas del sistema en una única clase.
 * Por qué es importante (el impacto o problema que resuelve): Permite modularizar la lógica del controlador de administración y auditoría de datos maestros de manera estructurada y segura.
 */
import Modelo.DAO.DatoMaestroDAO;
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
 * Qué hace (la acción): Asocia el servlet DatosMaestrosServlet con una lista de URLs de mantenimiento de catálogos mediante la anotación @WebServlet.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - urlPatterns = {...}: Mapea el servlet a las rutas REST de sectores, organizaciones, tipos de documentos, especies de mascotas, recursos, etc.
 *   - extends HttpServlet: Convierte a la clase en un servlet web para responder a los métodos HTTP estándares.
 * Para qué se usa (el propósito): Servir como el controlador centralizado para las operaciones de mantenimiento administrativo de las tablas paramétricas.
 * Por qué es importante (el impacto o problema que resuelve): Reemplaza la necesidad de crear 12 servlets separados (uno para cada catálogo), reduciendo la complejidad del proyecto y unificando el formato de las respuestas y la auditoría automática.
 */
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

    /*
     * Qué hace (la acción): Instancia de manera privada y constante la variable dao de tipo DatoMaestroDAO.
     * Qué significa (conceptos, métodos, tipos involucrados): Instancia de la clase de persistencia DatoMaestroDAO.
     * Para qué se usa (el propósito): Ejecutar las operaciones de base de datos en las 12 tablas paramétricas.
     */
    private final DatoMaestroDAO dao = new DatoMaestroDAO();

    /*
     * Qué hace (la acción): Sobrescribe el método service para desviar las peticiones que utilizan el verbo HTTP PATCH hacia el método doPatch.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - service: Método core del ciclo de vida del servlet que se ejecuta antes de delegar a doGet, doPost, etc.
     *   - PATCH: Método HTTP utilizado para la actualización parcial de recursos.
     * Para qué se usa (el propósito): Habilitar el soporte del método PATCH, el cual no viene soportado de forma nativa por doPatch en la API estándar de servlets de Jakarta EE.
     * Por qué es importante (el impacto o problema que resuelve): Permite actualizar un campo específico de una tabla paramétrica (como activar/desactivar un registro) sin requerir un método POST o PUT completo.
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String method = req.getMethod();
        if (method.equalsIgnoreCase("PATCH")) {
            doPatch(req, resp);
        } else {
            super.service(req, resp);
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doGet para procesar consultas de lectura, ya sea para listar todos los registros de un catálogo, cargar una página específica de preguntas de censo o ver el historial de auditoría de un registro.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - pathInfo.matches("^/\\d+/(historial|history)/?$"): Expresión regular que detecta si la ruta pide el historial de auditoría de un registro (ej: "/15/history").
     *   - pathInfo.matches("^/\\d+/?$"): Expresión regular que detecta si se busca un registro específico por su ID.
     * Para qué se usa (el propósito): Servir las peticiones GET para los dropdowns y las tablas del panel de administración del supervisor y censo del voluntario.
     * Por qué es importante (el impacto o problema que resuelve): Permite que tanto supervisores como voluntarios consulten la información paramétrica vigente, aplicando validaciones de rol estrictas (los voluntarios solo pueden leer preguntas de vulnerabilidad para llenar el censo).
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        PrintWriter out = response.getWriter();
        String entity = obtenerEntidad(request);
        String pathInfo = request.getPathInfo();

        boolean bypassRole = false;
        if (entity.equalsIgnoreCase("vulnerableQuestions")) {
            if (pathInfo == null || pathInfo.equals("/paginate")) {
                bypassRole = true;
            }
        }

        if (!bypassRole) {
            if (!validarRol(request, response)) {
                return;
            }
        }

        try {
            // Caso 0: Paginación de preguntas de vulnerabilidad para el voluntario
            if (entity.equalsIgnoreCase("vulnerableQuestions") && pathInfo != null && pathInfo.equals("/paginate")) {
                int page = 1;
                String pageStr = request.getParameter("page");
                if (pageStr != null) {
                    try {
                        page = Integer.parseInt(pageStr);
                    } catch (NumberFormatException e) {
                        page = 1;
                    }
                }
                Modelo.Servicios.Voluntario.VulnerabilidadServicio vulnServ = new Modelo.Servicios.Voluntario.VulnerabilidadServicio();
                out.print(vulnServ.obtenerPreguntasPaginadas(page, 3));
                return;
            }

            // Caso 1: Obtener Historial de un registro específico
            if (pathInfo != null && pathInfo.matches("^/\\d+/(historial|history)/?$")) {
                int id = obtenerIdDePath(pathInfo);
                List<Map<String, Object>> hist = dao.obtenerHistorial(entity, id);
                out.print(new JSONObject()
                        .put("success", true)
                        .put("data", new JSONArray(hist))
                        .toString());
                return;
            }

            // Caso 2: Obtener un registro por ID
            if (pathInfo != null && pathInfo.matches("^/\\d+/?$")) {
                int id = obtenerIdDePath(pathInfo);
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
            if (entity.equalsIgnoreCase("vulnerableQuestions") && (pathInfo == null || !pathInfo.equals("/"))) {
                Modelo.Servicios.Voluntario.VulnerabilidadServicio vulnServ = new Modelo.Servicios.Voluntario.VulnerabilidadServicio();
                out.print(vulnServ.obtenerPreguntas());
                return;
            }

            List<Map<String, Object>> lista = dao.listar(entity);
            out.print(new JSONObject()
                    .put("success", true)
                    .put("data", new JSONArray(lista))
                    .toString());

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(new JSONObject().put("success", false).put("message", "Error al consultar datos: " + e.getMessage()).toString());
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doPost para procesar la creación de un nuevo registro paramétrico e inyectar el evento en el historial de auditoría.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - dao.crear(entity, body): Inserta una nueva fila en la tabla paramétrica en SQL a partir del JSON recibido.
     *   - dao.registrarAuditoria(...): Guarda un registro detallando la acción de inserción ejecutada por el supervisor.
     * Para qué se usa (el propósito): Agregar nuevas opciones a los catálogos (ej: crear un nuevo sector o una nueva organización).
     * Por qué es importante (el impacto o problema que resuelve): Mantiene la consistencia de los catálogos de base de datos de forma dinámica y segura, rastreando quién creó el registro.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        PrintWriter out = response.getWriter();

        if (!validarRol(request, response)) {
            return;
        }

        HttpSession session = request.getSession(false);
        Integer operatorId = SessionUtil.getUsuarioId(session);
        if (operatorId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print(new JSONObject().put("success", false).put("message", "Acceso denegado. Sesión inválida.").toString());
            return;
        }
        String entity = obtenerEntidad(request);

        try {
            JSONObject body = JSONUtil.leerJson(request);
            int newId = dao.crear(entity, body);
            
            if (newId != -1) {
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

    /*
     * Qué hace (la acción): Define la lógica doPatch para actualizar de manera parcial atributos de un registro paramétrico (como el nombre o el estado de activación lógica).
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - pathInfo.contains("/status/"): Detecta si la ruta solicita cambiar el estado de activación lógico de un registro.
     *   - dao.cambiarEstado(entity, id, activo): Activa o desactiva la disponibilidad del elemento en la interfaz del voluntario.
     *   - dao.actualizar(entity, id, body): Actualiza los campos genéricos modificados en base de datos.
     * Para qué se usa (el propósito): Permitir a los supervisores editar y deshabilitar/habilitar opciones del sistema de forma granular.
     * Por qué es importante (el impacto o problema que resuelve): Permite actualizar los valores y de inmediato registrar la auditoría con el estado anterior y el nuevo, garantizando control sobre cambios de datos en el sistema.
     */
    protected void doPatch(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        PrintWriter out = response.getWriter();

        if (!validarRol(request, response)) {
            return;
        }

        HttpSession session = request.getSession(false);
        Integer operatorId = SessionUtil.getUsuarioId(session);
        if (operatorId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print(new JSONObject().put("success", false).put("message", "Acceso denegado. Sesión inválida.").toString());
            return;
        }
        String entity = obtenerEntidad(request);
        String pathInfo = request.getPathInfo();

        try {
            // Caso 1: Actualizar Estado (Activo/Inactivo)
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

    /*
     * Qué hace (la acción): Sobrescribe el método doDelete para procesar la eliminación física de un registro en la base de datos SQL.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - dao.eliminar(entity, id): Borra la fila de la tabla en base de datos.
     *   - SQLException: Se captura de forma separada si existe alguna restricción de llave foránea (FK) que impida el borrado en cascada.
     * Para qué se usa (el propósito): Eliminar opciones paramétricas del sistema de forma controlada.
     * Por qué es importante (el impacto o problema que resuelve): Protege la base de datos contra inconsistencias. Si el elemento ya tiene registros vinculados, el gestor SQL lanzará una excepción y el controlador la reportará al usuario sin dañar la integridad de los datos.
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        PrintWriter out = response.getWriter();

        if (!validarRol(request, response)) {
            return;
        }

        HttpSession session = request.getSession(false);
        Integer operatorId = SessionUtil.getUsuarioId(session);
        if (operatorId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print(new JSONObject().put("success", false).put("message", "Acceso denegado. Sesión inválida.").toString());
            return;
        }
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

    /*
     * Qué hace (la acción): Valida que el usuario tenga una sesión activa y que posea el rol de Supervisor (rol_id = 2).
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - UsuarioDAO: DAO para consultar los atributos actualizados de un usuario en base de datos.
     *   - usuario.getRolId(): Recupera el identificador de rol asociado en base de datos.
     * Para qué se usa (el propósito): Asegurar que las operaciones de modificación de la base de datos estén restringidas a administradores del sistema.
     * Por qué es importante (el impacto o problema que resuelve): Impide el acceso y la manipulación indebida de la parametrización de catálogos generales a usuarios con roles inferiores como los voluntarios (rol_id = 1).
     */
    private boolean validarRol(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        Integer userId = SessionUtil.getUsuarioId(session);
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Sesión inválida.").toString());
            return false;
        }
        try {
            Modelo.DAO.UsuarioDAO usuarioDao = new Modelo.DAO.UsuarioDAO();
            Modelo.Entidades.Usuario usuario = usuarioDao.obtenerPorId(userId);
            if (usuario == null || usuario.getRolId() != 2) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().print(new JSONObject().put("success", false).put("message", "Acceso denegado. Permisos insuficientes.").toString());
                return false;
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().print(new JSONObject().put("success", false).put("message", "Error interno al validar permisos.").toString());
            return false;
        }
    }

    // =========================================================================
    // HELPERS INTERNOS
    // =========================================================================

    /*
     * Qué hace (la acción): Extrae e identifica el nombre de la entidad a partir de la subruta final mapeada del servlet.
     * Qué significa (conceptos, métodos, tipos involucrados): Extrae del ServletPath la última sección (ej: "/api/sectors" -> "sectors").
     * Para qué se usa (el propósito): Determinar de forma dinámica con cuál de las 12 tablas paramétricas interactuar.
     */
    private String obtenerEntidad(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        return servletPath.substring(servletPath.lastIndexOf("/") + 1);
    }

    /*
     * Qué hace (la acción): Recorre y divide el pathInfo para localizar y parsear el ID numérico del recurso solicitado.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - pathInfo.split("/"): Divide la ruta por barras inclinadas.
     *   - part.matches("\\d+"): Revisa si el segmento contiene solo caracteres numéricos.
     * Para qué se usa (el propósito): Obtener la llave primaria del elemento a consultar, actualizar o borrar.
     */
    private int obtenerIdDePath(String pathInfo) {
        if (pathInfo == null) return -1;
        String[] parts = pathInfo.split("/");
        for (String part : parts) {
            if (part.matches("\\d+")) {
                return Integer.parseInt(part);
            }
        }
        return -1;
    }

    /*
     * Qué hace (la acción): Serializa en formato de texto plano legible las claves y valores de un JSONObject recibido en una petición de inserción.
     * Qué significa (conceptos, métodos, tipos involucrados): StringBuilder que concatena llaves y valores.
     * Para qué se usa (el propósito): Formatear los nuevos valores que se registrarán en la tabla de historial de auditoría.
     */
    private String formatJsonBody(JSONObject body) {
        StringBuilder sb = new StringBuilder();
        for (String key : body.keySet()) {
            sb.append(key).append(": ").append(body.get(key)).append("; ");
        }
        return sb.toString();
    }

    /*
     * Qué hace (la acción): Serializa en formato de texto plano descriptivo las columnas y valores de una fila (Map) recuperada de base de datos antes de ser actualizada o borrada.
     * Qué significa (conceptos, métodos, tipos involucrados): StringBuilder que concatena pares clave-valor de un mapa omitiendo la clave "id".
     * Para qué se usa (el propósito): Representar el estado original del registro de cara al historial de auditoría.
     */
    private String formatMap(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!entry.getKey().equals("id")) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("; ");
            }
        }
        return sb.toString();
    }
}
