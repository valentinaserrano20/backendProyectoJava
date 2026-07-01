package Controlador.Public;

/*
 * Qué hace (la acción): Importa la capa DAO para persistir notificaciones, la clase DTO que representa las alertas, las utilidades de JSON, sesión y respuestas HTTP, y las clases del servlet de Jakarta.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.DAO.NotificacionDAO: Capa de persistencia que interactúa con la base de datos SQL para guardar y actualizar alertas.
 *   - Modelo.DTO.NotificacionDTO: Clase contenedor que contiene los datos de la notificación.
 *   - Modelo.Utilidades.SessionUtil: Clase de utilidad para extraer el ID de usuario de forma segura desde la sesión HTTP.
 *   - org.json.JSONArray / JSONObject: Estructuras de la librería JSON para mapear arreglos y objetos.
 * Para qué se usa (el propósito): Proveer las dependencias requeridas para consultar, crear, marcar como leídas y eliminar notificaciones asociadas al usuario autenticado.
 * Por qué es importante (el impacto o problema que resuelve): Sin estas importaciones no se podría procesar las alertas del usuario ni mapearlas dinámicamente al formato de intercambio JSON.
 */
import Modelo.DAO.NotificacionDAO;
import Modelo.DTO.NotificacionDTO;
import Modelo.Utilidades.JSONUtil;
import Modelo.Utilidades.ResponseUtil;
import Modelo.Utilidades.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import org.json.JSONArray;
import org.json.JSONObject;

/*
 * Qué hace (la acción): Asocia el servlet NotificacionServlet con los endpoints de red "/api/notificaciones" y "/api/notificaciones/*" utilizando la anotación @WebServlet.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - @WebServlet: Registra de manera declarativa este servlet en el servidor web.
 *   - extends HttpServlet: Permite sobreescribir los métodos de procesamiento web (doGet, doPost, doPut, doDelete).
 * Para qué se usa (el propósito): Administrar de forma integral y centralizada el ciclo de vida de las alertas de los usuarios en la base de datos.
 * Por qué es importante (el impacto o problema que resuelve): Concentra toda la funcionalidad REST de alertas en un solo lugar, permitiendo operaciones CRUD rápidas (Leer, Crear, Actualizar y Eliminar) sobre la tabla de notificaciones.
 */
@WebServlet(urlPatterns = {"/api/notificaciones", "/api/notificaciones/*"})
public class NotificacionServlet extends HttpServlet {

    /*
     * Qué hace (la acción): Instancia de manera privada y constante la variable notificacionDAO.
     * Qué significa (conceptos, métodos, tipos involucrados): Creación del objeto de acceso a datos para notificaciones.
     * Para qué se usa (el propósito): Ejecutar las sentencias SQL en base de datos.
     */
    private final NotificacionDAO notificacionDAO = new NotificacionDAO();

    /*
     * Qué hace (la acción): Sobrescribe el método doGet para obtener el listado completo de alertas del usuario o el conteo de notificaciones no leídas si se llama al sub-recurso "/count".
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - SessionUtil.getUsuarioId(session): Recupera de forma segura el ID numérico del usuario logueado en la sesión.
     *   - notificacionDAO.contarNoLeidas(userId): Consulta a la base de datos la cantidad de alertas con estado de lectura falso.
     *   - notificacionDAO.obtenerPorUsuario(userId): Consulta en base de datos y retorna la lista de DTOs de alertas.
     * Para qué se usa (el propósito): Entregar en formato JSON las alertas no leídas y la lista de mensajes acumulados al usuario logueado en el frontend.
     * Por qué es importante (el impacto o problema que resuelve): Permite que el panel de control del usuario muestre un globo indicador del total de mensajes pendientes (ej. "3 alertas nuevas") y alimente la bandeja de notificaciones.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        PrintWriter out = response.getWriter();
        String pathInfo = request.getPathInfo();

        try {
            HttpSession session = request.getSession(false);
            Integer userId = SessionUtil.getUsuarioId(session);
            if (userId == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print(ResponseUtil.error("Acceso denegado. Sesión inválida."));
                return;
            }

            // Obtener conteo de notificaciones no leídas
            // Ruta: /api/notificaciones/count
            if ("/count".equals(pathInfo)) {
                int count = notificacionDAO.contarNoLeidas(userId);
                JSONObject datos = new JSONObject();
                datos.put("count", count);
                
                response.setStatus(HttpServletResponse.SC_OK);
                out.print(ResponseUtil.success(datos));
                return;
            }

            // Obtener todas las notificaciones del usuario
            // Ruta: /api/notificaciones
            var notificaciones = notificacionDAO.obtenerPorUsuario(userId);
            JSONArray notificacionesArray = new JSONArray();
            
            for (NotificacionDTO notif : notificaciones) {
                JSONObject notifJson = new JSONObject();
                notifJson.put("id", notif.getId());
                notifJson.put("titulo", notif.getTitulo());
                notifJson.put("mensaje", notif.getMensaje());
                notifJson.put("tipo", notif.getTipo());
                notifJson.put("leida", notif.isLeida());
                notifJson.put("fecha_creacion", notif.getFechaCreacion());
                notifJson.put("enlace", notif.getEnlace());
                notifJson.put("entidad_id", notif.getEntidadId());
                notificacionesArray.put(notifJson);
            }

            response.setStatus(HttpServletResponse.SC_OK);
            out.print(ResponseUtil.success(notificacionesArray));

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(ResponseUtil.error(e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doPost para recibir un JSON con la estructura de una nueva notificación y guardarla en la base de datos.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - JSONUtil.leerJson(request): Parsea la petición asíncrona a JSONObject.
     *   - notificacion.setLeida(false): Configura que una notificación nueva inicie por defecto marcada como no leída.
     *   - body.optString / optInt: Obtiene valores opcionales del JSON asignando un fallback (nulo o cero) si no estuvieran presentes.
     * Para qué se usa (el propósito): Crear y programar alertas dinámicas en el sistema (ej. cuando se evalúa un plan familiar, se envía una notificación al voluntario creador).
     * Por qué es importante (el impacto o problema que resuelve): Posibilita la creación de alertas entre usuarios (como la comunicación del Supervisor al Voluntario) de forma asíncrona.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        PrintWriter out = response.getWriter();

        try {
            JSONObject body = JSONUtil.leerJson(request);
            
            NotificacionDTO notificacion = new NotificacionDTO();
            notificacion.setUsuarioId(body.getInt("usuario_id"));
            notificacion.setTitulo(body.getString("titulo"));
            notificacion.setMensaje(body.getString("mensaje"));
            notificacion.setTipo(body.getString("tipo"));
            notificacion.setLeida(false);
            notificacion.setEnlace(body.optString("enlace", null));
            notificacion.setEntidadId(body.optInt("entidad_id", 0));

            notificacionDAO.crear(notificacion);

            response.setStatus(HttpServletResponse.SC_CREATED);
            out.print(ResponseUtil.success("Notificación creada exitosamente"));

        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(ResponseUtil.error("JSON mal formado: " + e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(ResponseUtil.error(e.getMessage()));
        }
    }

    /*
     * Qué hace (la hace): Sobrescribe el método doPut para marcar una alerta en particular o todas las alertas del usuario como leídas.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - pathInfo.equals("/marcar-leidas"): Determina si la llamada pide una actualización en masa de todas las notificaciones del usuario.
     *   - notificacionDAO.marcarTodasComoLeidas(userId): Ejecuta la actualización de base de datos de todos los registros del usuario a leída = true.
     *   - notificacionDAO.marcarComoLeida(notificacionId): Actualiza una sola fila en la base de datos por ID.
     * Para qué se usa (el propósito): Cambiar el estado de lectura de las alertas cuando el usuario abre la bandeja en la interfaz.
     * Por qué es importante (el impacto o problema que resuelve): Permite actualizar y borrar el indicador de globos de alertas en el frontend a medida que el usuario visualiza los mensajes.
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        PrintWriter out = response.getWriter();
        String pathInfo = request.getPathInfo();

        try {
            HttpSession session = request.getSession(false);
            Integer userId = SessionUtil.getUsuarioId(session);
            if (userId == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print(ResponseUtil.error("Acceso denegado. Sesión inválida."));
                return;
            }

            // Marcar todas como leídas
            // Ruta: /api/notificaciones/marcar-leidas
            if ("/marcar-leidas".equals(pathInfo)) {
                notificacionDAO.marcarTodasComoLeidas(userId);
                response.setStatus(HttpServletResponse.SC_OK);
                out.print(ResponseUtil.success("Todas las notificaciones marcadas como leídas"));
                return;
            }

            // Marcar una notificación específica como leída
            // Ruta: /api/notificaciones
            JSONObject body = JSONUtil.leerJson(request);
            int notificacionId = body.getInt("id");
            
            notificacionDAO.marcarComoLeida(notificacionId);

            response.setStatus(HttpServletResponse.SC_OK);
            out.print(ResponseUtil.success("Notificación marcada como leída"));

        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(ResponseUtil.error("JSON mal formado: " + e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(ResponseUtil.error(e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doDelete para eliminar físicamente una alerta de la base de datos a partir de su ID.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - notificacionDAO.eliminar(notificacionId): Remueve el registro de la tabla en base de datos.
     * Para qué se usa (el propósito): Permitir que el usuario elimine u oculte alertas antiguas de su panel.
     * Por qué es importante (el impacto o problema que resuelve): Limpia y optimiza la tabla de base de datos de notificaciones inservibles o descartadas por el usuario.
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        PrintWriter out = response.getWriter();

        try {
            JSONObject body = JSONUtil.leerJson(request);
            int notificacionId = body.getInt("id");
            
            notificacionDAO.eliminar(notificacionId);

            response.setStatus(HttpServletResponse.SC_OK);
            out.print(ResponseUtil.success("Notificación eliminada"));

        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(ResponseUtil.error("JSON mal formado: " + e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(ResponseUtil.error(e.getMessage()));
        }
    }
}
