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

@WebServlet(urlPatterns = {"/api/notificaciones", "/api/notificaciones/*"})
public class NotificacionServlet extends HttpServlet {

    private final NotificacionDAO notificacionDAO = new NotificacionDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
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

            // Obtener conteo de notificaciones no leídas
            if ("/count".equals(pathInfo)) {
                int count = notificacionDAO.contarNoLeidas(userId);
                JSONObject respuesta = new JSONObject();
                respuesta.put("success", true);
                respuesta.put("count", count);
                response.setStatus(HttpServletResponse.SC_OK);
                out.print(respuesta.toString());
                return;
            }

            // Obtener todas las notificaciones del usuario
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
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(new JSONObject().put("success", false).put("message", e.getMessage()).toString());
        }
    }

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
            if ("/marcar-leidas".equals(pathInfo)) {
                notificacionDAO.marcarTodasComoLeidas(userId);
                JSONObject respuesta = new JSONObject();
                respuesta.put("success", true);
                respuesta.put("message", "Todas las notificaciones marcadas como leídas");
                response.setStatus(HttpServletResponse.SC_OK);
                out.print(respuesta.toString());
                return;
            }

            // Marcar una notificación específica como leída
            JSONObject body = JSONUtil.leerJson(request);
            int notificacionId = body.getInt("id");
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
