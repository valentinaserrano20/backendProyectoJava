package Controlador.Public;

import Modelo.DAO.NotificacionDAO;
import Modelo.DTO.NotificacionDTO;
import Modelo.Utilidades.JSONUtil;
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

// Qué hace: Servlet encargado de administrar la recepción, conteo, lectura y eliminación de notificaciones en tiempo real para los usuarios autenticados.
// Por qué existe: Habilita los endpoints REST en '/api/notificaciones/*' que permiten notificar a los usuarios sobre cambios de estado en sus planes de emergencia u otras alertas del sistema.
// Qué pasaría si no estuviera: Los usuarios no recibirían avisos visuales ni sabrían si su plan familiar fue aprobado o rechazado sin consultar manualmente el censo.
@WebServlet(urlPatterns = {"/api/notificaciones", "/api/notificaciones/*"})
public class NotificacionServlet extends HttpServlet {

    // Qué hace: Instancia el objeto de acceso a datos para las notificaciones.
    // Por qué existe: Facilita la persistencia y lectura de las alertas en la base de datos MySQL.
    // Qué pasaría si no estuviera: No podríamos consultar ni guardar el estado de lectura de ninguna alerta.
    // Flujo: De aquí pasamos a NotificacionDAO.
    private final NotificacionDAO notificacionDAO = new NotificacionDAO();

    // Qué hace: Atiende peticiones GET para obtener la lista de alertas del usuario o el conteo de notificaciones no leídas.
    // Por qué existe: Popula la campanita de notificaciones y la barra superior de la interfaz web en la SPA.
    // Qué pasaría si no estuviera: La SPA no mostraría el globo indicador con el total de alertas pendientes del usuario.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        String pathInfo = request.getPathInfo();

        try {
            // Qué hace: Recupera la sesión HTTP actual sin forzar la creación de una nueva.
            // Por qué existe: Valida la identidad del usuario a través de la variable de sesión 'user_id'.
            // Qué pasaría si no estuviera: Cualquier persona anónima podría consultar o espiar las notificaciones de otros usuarios del sistema.
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("user_id") == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print(new JSONObject().put("success", false).put("message", "No autenticado").toString());
                return;
            }

            int userId = (int) session.getAttribute("user_id");

            // Obtener conteo de notificaciones no leídas
            // Ruta: /api/notificaciones/count
            // Qué hace: Retorna la cantidad numérica de alertas sin leer para el usuario actual.
            // Por qué existe: Permite dibujar el indicador con el número sobre la campanita.
            if ("/count".equals(pathInfo)) {
                // Qué hace: Ejecuta la consulta COUNT en la base de datos.
                // y luego de esto pasamos a NotificacionDAO.contarNoLeidas.
                int count = notificacionDAO.contarNoLeidas(userId);
                JSONObject datos = new JSONObject();
                datos.put("count", count);
                
                JSONObject respuesta = new JSONObject();
                respuesta.put("success", true);
                respuesta.put("message", "");
                respuesta.put("data", datos);
                
                response.setStatus(HttpServletResponse.SC_OK);
                out.print(respuesta.toString());
                return;
            }

            // Obtener todas las notificaciones del usuario
            // Ruta: /api/notificaciones
            // Qué hace: Recupera el historial completo de notificaciones del usuario.
            // Por qué existe: Alimenta la bandeja de entrada o modal detallado de notificaciones.
            // Qué hace: Llama al DAO para obtener la lista de DTOs.
            // y luego de esto pasamos a NotificacionDAO.obtenerPorUsuario.
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

            JSONObject respuesta = new JSONObject();
            respuesta.put("success", true);
            respuesta.put("data", notificacionesArray);
            response.setStatus(HttpServletResponse.SC_OK);
            out.print(respuesta.toString());

        } catch (Exception e) {
            // Qué hace: Captura errores inesperados, cambia el estado HTTP a 500 y retorna el error en JSON.
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(new JSONObject().put("success", false).put("message", e.getMessage()).toString());
        }
    }

    // Qué hace: Procesa peticiones POST para crear una notificación dirigida a un usuario específico.
    // Por qué existe: Permite a los supervisores u otros módulos del sistema emitir avisos (ej. cuando se rechaza un plan).
    // Qué pasaría si no estuviera: No se podrían registrar nuevas alertas desde el backend hacia los usuarios.
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("user_id") == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print(new JSONObject().put("success", false).put("message", "No autenticado").toString());
                return;
            }

            // Qué hace: Lee el cuerpo de la petición en JSON y construye el DTO de Notificación.
            // Por qué existe: Convierte el payload en una entidad utilizable por Java.
            JSONObject body = JSONUtil.leerJson(request);
            
            NotificacionDTO notificacion = new NotificacionDTO();
            notificacion.setUsuarioId(body.getInt("usuario_id"));
            notificacion.setTitulo(body.getString("titulo"));
            notificacion.setMensaje(body.getString("mensaje"));
            notificacion.setTipo(body.getString("tipo"));
            notificacion.setLeida(false);
            notificacion.setEnlace(body.optString("enlace", null));
            notificacion.setEntidadId(body.optInt("entidad_id", 0));

            // Qué hace: Inserta el registro en la base de datos.
            // y luego de esto pasamos a NotificacionDAO.crear.
            notificacionDAO.crear(notificacion);

            JSONObject respuesta = new JSONObject();
            respuesta.put("success", true);
            respuesta.put("message", "Notificación creada exitosamente");
            response.setStatus(HttpServletResponse.SC_CREATED);
            out.print(respuesta.toString());

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(new JSONObject().put("success", false).put("message", e.getMessage()).toString());
        }
    }

    // Qué hace: Procesa peticiones PUT para marcar notificaciones individuales o todas juntas como leídas.
    // Por qué existe: Permite actualizar el estado de lectura de las alertas para vaciar la campanita.
    // Qué pasaría si no estuviera: El contador de notificaciones no leídas nunca bajaría, molestando al usuario.
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        String pathInfo = request.getPathInfo();

        try {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("user_id") == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print(new JSONObject().put("success", false).put("message", "No autenticado").toString());
                return;
            }

            int userId = (int) session.getAttribute("user_id");

            // Marcar todas como leídas
            // Ruta: /api/notificaciones/marcar-leidas
            if ("/marcar-leidas".equals(pathInfo)) {
                // Qué hace: Actualiza la columna 'leida = 1' en lote para el usuario.
                // y luego de esto pasamos a NotificacionDAO.marcarTodasComoLeidas.
                notificacionDAO.marcarTodasComoLeidas(userId);
                JSONObject respuesta = new JSONObject();
                respuesta.put("success", true);
                respuesta.put("message", "Todas las notificaciones marcadas como leídas");
                response.setStatus(HttpServletResponse.SC_OK);
                out.print(respuesta.toString());
                return;
            }

            // Marcar una notificación específica como leída
            // Ruta: /api/notificaciones
            JSONObject body = JSONUtil.leerJson(request);
            int notificacionId = body.getInt("id");
            
            // Qué hace: Actualiza una única fila en MySQL cambiando su estado de lectura.
            // y luego de esto pasamos a NotificacionDAO.marcarComoLeida.
            notificacionDAO.marcarComoLeida(notificacionId);

            JSONObject respuesta = new JSONObject();
            respuesta.put("success", true);
            respuesta.put("message", "Notificación marcada como leída");
            response.setStatus(HttpServletResponse.SC_OK);
            out.print(respuesta.toString());

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(new JSONObject().put("success", false).put("message", e.getMessage()).toString());
        }
    }

    // Qué hace: Procesa peticiones DELETE para eliminar físicamente una notificación.
    // Por qué existe: Permite a los usuarios limpiar notificaciones viejas o no deseadas de su lista.
    // Qué pasaría si no estuviera: El historial de notificaciones crecería indefinidamente sin control de limpieza.
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("user_id") == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print(new JSONObject().put("success", false).put("message", "No autenticado").toString());
                return;
            }

            JSONObject body = JSONUtil.leerJson(request);
            int notificacionId = body.getInt("id");
            
            // Qué hace: Borra el registro de la notificación de la BD.
            // y luego de esto pasamos a NotificacionDAO.eliminar.
            notificacionDAO.eliminar(notificacionId);

            JSONObject respuesta = new JSONObject();
            respuesta.put("success", true);
            respuesta.put("message", "Notificación eliminada");
            response.setStatus(HttpServletResponse.SC_OK);
            out.print(respuesta.toString());

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(new JSONObject().put("success", false).put("message", e.getMessage()).toString());
        }
    }
}
