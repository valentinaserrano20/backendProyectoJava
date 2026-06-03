package Controlador.Public;

import Modelo.Config.Conexion;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Servlet: AuditsServlet
 * Provee los endpoints de estadísticas y auditoría para los dashboards del Supervisor y Administrador.
 * Mapea la ruta de la API /api/audits/* y procesa las peticiones GET concurrentes.
 */
@WebServlet("/api/audits/*")
public class AuditsServlet extends HttpServlet {

    // Qué hace: Sobrescribe el método doGet para canalizar las peticiones del dashboard.
    // Por qué existe: Escucha e interpreta las solicitudes HTTP GET enviadas por el frontend.
    // Qué problema resuelve: Permite procesar peticiones para supervisor y administrador en un solo servlet.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // Qué hace: Configura las cabeceras de respuesta HTTP como tipo JSON y codificación UTF-8.
        // Por qué existe: Garantiza que el navegador interprete la respuesta como JSON legible y sin fallos de caracteres.
        // Qué problema resuelve: Previene la corrupción de textos con tildes o caracteres especiales en el frontend.
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        String pathInfo = request.getPathInfo();

        // Qué hace: Valida si la ruta solicitada corresponde al dashboard de supervisión.
        // Por qué existe: Determina qué lógica de consulta SQL ejecutar según la solicitud de red.
        // Qué problema resuelve: Enruta la petición hacia el método que genera las métricas de planes familiares.
        if (pathInfo != null && pathInfo.equals("/dashBoardSupervisor")) {
            obtenerDashBoardSupervisor(response, out);
        } 
        // Qué hace: Valida si la ruta solicitada corresponde al dashboard de administración.
        // Por qué existe: Canaliza la petición para calcular las métricas de usuarios, roles e historiales.
        // Qué problema resuelve: Enruta la petición al método correspondiente a la auditoría del administrador.
        else if (pathInfo != null && pathInfo.equals("/dashBoardAdmin")) {
            obtenerDashBoardAdmin(response, out);
        } 
        // Qué hace: Devuelve un error 404 si la ruta solicitada no coincide con ninguna acción.
        // Por qué existe: Provee un fallback seguro de error para evitar loops o estados indefinidos.
        // Qué problema resuelve: Informa al cliente que el endpoint específico no está soportado.
        else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print(new JSONObject()
                    .put("success", false)
                    .put("message", "Endpoint de auditoría no encontrado").toString());
        }
    }

    // Qué hace: Consulta los planes familiares y genera un objeto JSON consolidado con las métricas de supervisión.
    // Por qué existe: Realiza los cálculos agregados directamente en la base de datos para responder al frontend.
    // Qué problema resuelve: Reúne los totales de planes aprobados, rechazados, pendientes y en revisión de forma ágil.
    private void obtenerDashBoardSupervisor(HttpServletResponse response, PrintWriter out) {
        // Sentencia SQL de agregación para contar los planes según su estado_id
        String sql = "SELECT "
                + "  SUM(CASE WHEN estado_id IN (1, 2) THEN 1 ELSE 0 END) as pending, "
                + "  SUM(CASE WHEN estado_id = 3 THEN 1 ELSE 0 END) as in_review, "
                + "  SUM(CASE WHEN estado_id IN (4, 7) THEN 1 ELSE 0 END) as approved, "
                + "  SUM(CASE WHEN estado_id IN (5, 6) THEN 1 ELSE 0 END) as rejected "
                + "FROM planes_familiares";

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            JSONObject data = new JSONObject();
            if (rs.next()) {
                data.put("pending_plans", rs.getInt("pending"));
                data.put("approved_plans", rs.getInt("approved"));
                data.put("rejected_plans", rs.getInt("rejected"));
                data.put("in_review_plans", rs.getInt("in_review"));
            } else {
                data.put("pending_plans", 0);
                data.put("approved_plans", 0);
                data.put("rejected_plans", 0);
                data.put("in_review_plans", 0);
            }

            JSONObject jsonRes = new JSONObject();
            jsonRes.put("success", true);
            jsonRes.put("data", data);
            out.print(jsonRes.toString());

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(new JSONObject()
                    .put("success", false)
                    .put("message", "Error al obtener métricas de supervisor: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Consulta los usuarios, roles y el historial de cambios del administrador en la base de datos.
    // Por qué existe: Obtiene la información estructurada que consume el controlador JS para armar gráficos e historiales.
    // Qué problema resuelve: Provee en una sola respuesta JSON los datos de auditoría de usuarios y de tablas del catálogo general.
    private void obtenerDashBoardAdmin(HttpServletResponse response, PrintWriter out) {
        try (Connection con = Conexion.obtener()) {

            // 1. Obtener conteo de usuarios por estado (summary)
            String sqlSummary = "SELECT "
                    + "  SUM(CASE WHEN estado_id = 1 THEN 1 ELSE 0 END) as active, "
                    + "  SUM(CASE WHEN estado_id = 2 THEN 1 ELSE 0 END) as inactive, "
                    + "  SUM(CASE WHEN estado_id = 3 THEN 1 ELSE 0 END) as request "
                    + "FROM usuarios";
            
            JSONObject summary = new JSONObject();
            try (PreparedStatement ps = con.prepareStatement(sqlSummary);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    summary.put("active", rs.getInt("active"));
                    summary.put("inactive", rs.getInt("inactive"));
                    summary.put("request", rs.getInt("request"));
                } else {
                    summary.put("active", 0);
                    summary.put("inactive", 0);
                    summary.put("request", 0);
                }
            }

            // 2. Obtener conteo de usuarios por rol (rols)
            String sqlRols = "SELECT "
                    + "  SUM(CASE WHEN rol_id = 1 THEN 1 ELSE 0 END) as volunteer, "
                    + "  SUM(CASE WHEN rol_id = 2 THEN 1 ELSE 0 END) as supervisor "
                    + "FROM usuarios";
            
            JSONObject rols = new JSONObject();
            try (PreparedStatement ps = con.prepareStatement(sqlRols);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    rols.put("volunteer", rs.getInt("volunteer"));
                    rols.put("supervisor", rs.getInt("supervisor"));
                } else {
                    rols.put("volunteer", 0);
                    rols.put("supervisor", 0);
                }
            }

            // 3. Obtener el historial de miembros (history_members)
            String sqlHistoryMembers = "SELECT h.tabla_afectada, h.accion, h.valor_anterior, h.valor_nuevo, h.fecha, "
                    + "  CONCAT(u.nombre, ' ', u.apellido) as user_name, r.nombre as rol_nombre "
                    + "FROM historial_datos_maestros h "
                    + "JOIN usuarios u ON h.usuario_id = u.id "
                    + "JOIN roles r ON u.rol_id = r.id "
                    + "WHERE h.tabla_afectada = 'usuarios' "
                    + "ORDER BY h.fecha DESC LIMIT 10";
            
            JSONArray historyMembers = new JSONArray();
            try (PreparedStatement ps = con.prepareStatement(sqlHistoryMembers);
                 ResultSet rs = ps.executeQuery()) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                while (rs.next()) {
                    JSONObject item = new JSONObject();
                    item.put("name_model", rs.getString("tabla_afectada"));
                    item.put("action_execute", rs.getString("accion"));
                    item.put("user_name", rs.getString("user_name"));
                    item.put("rol", rs.getString("rol_nombre"));
                    item.put("date_time", sdf.format(rs.getTimestamp("fecha")));
                    item.put("status_old", rs.getString("valor_anterior"));
                    item.put("status_new", rs.getString("valor_nuevo"));
                    historyMembers.put(item);
                }
            }

            // 4. Obtener el historial general (history_general)
            String sqlHistoryGeneral = "SELECT h.tabla_afectada, h.accion, h.valor_anterior, h.valor_nuevo, h.fecha, "
                    + "  CONCAT(u.nombre, ' ', u.apellido) as user_name, r.nombre as rol_nombre "
                    + "FROM historial_datos_maestros h "
                    + "JOIN usuarios u ON h.usuario_id = u.id "
                    + "JOIN roles r ON u.rol_id = r.id "
                    + "WHERE h.tabla_afectada != 'usuarios' "
                    + "ORDER BY h.fecha DESC LIMIT 10";
            
            JSONArray historyGeneral = new JSONArray();
            try (PreparedStatement ps = con.prepareStatement(sqlHistoryGeneral);
                 ResultSet rs = ps.executeQuery()) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                while (rs.next()) {
                    JSONObject item = new JSONObject();
                    item.put("name_model", rs.getString("tabla_afectada"));
                    item.put("action_execute", rs.getString("accion"));
                    item.put("user_name", rs.getString("user_name"));
                    item.put("rol", rs.getString("rol_nombre"));
                    item.put("date_time", sdf.format(rs.getTimestamp("fecha")));
                    item.put("status_old", rs.getString("valor_anterior"));
                    item.put("status_new", rs.getString("valor_nuevo"));
                    historyGeneral.put(item);
                }
            }

            // 5. Generar tendencia mensual de cambios en catálogos (monthly_changes)
            // Se realiza un padding de los últimos 6 meses para que el gráfico de línea se dibuje completo
            JSONArray monthlyChanges = new JSONArray();
            String[] mesesNom = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
            
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, -5); // retrocedemos 5 meses en el tiempo para tener los últimos 6 meses
            
            for (int i = 0; i < 6; i++) {
                int monthNum = cal.get(Calendar.MONTH) + 1; // Enero es 0 en Calendar, por tanto se suma 1
                int yearNum = cal.get(Calendar.YEAR);
                String labelMes = mesesNom[cal.get(Calendar.MONTH)];
                
                String sqlChanges = "SELECT COUNT(*) as total FROM historial_datos_maestros "
                        + "WHERE MONTH(fecha) = ? AND YEAR(fecha) = ?";
                
                int totalCambios = 0;
                try (PreparedStatement ps = con.prepareStatement(sqlChanges)) {
                    ps.setInt(1, monthNum);
                    ps.setInt(2, yearNum);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            totalCambios = rs.getInt("total");
                        }
                    }
                }
                
                JSONObject monthData = new JSONObject();
                monthData.put("month", labelMes);
                monthData.put("total", totalCambios);
                monthlyChanges.put(monthData);
                
                cal.add(Calendar.MONTH, 1); // avanzamos al siguiente mes
            }

            // Unificar todos los datos en la respuesta
            JSONObject data = new JSONObject();
            data.put("summary", summary);
            data.put("rols", rols);
            data.put("history_members", historyMembers);
            data.put("history_general", historyGeneral);
            data.put("monthly_changes", monthlyChanges);

            JSONObject jsonRes = new JSONObject();
            jsonRes.put("success", true);
            jsonRes.put("data", data);
            out.print(jsonRes.toString());

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(new JSONObject()
                    .put("success", false)
                    .put("message", "Error al obtener métricas de administrador: " + e.getMessage()).toString());
        }
    }
}
