package Controlador.Public;

/*
 * Qué hace (la acción): Importa la clase de conexión a la base de datos, APIs de servlets HTTP y utilidades auxiliares como SimpleDateFormat y colecciones de JSON.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.Config.Conexion: Clase personalizada para obtener conexiones JDBC con la base de datos relacional.
 *   - java.sql.Connection / PreparedStatement / ResultSet: APIs estándares de Java Database Connectivity (JDBC) para ejecutar consultas SQL.
 *   - org.json.JSONArray / JSONObject: Componentes de la librería JSON para mapear listas y objetos de datos.
 * Para qué se usa (el propósito): Proveer todas las dependencias necesarias para realizar consultas estadísticas, formatear fechas y construir respuestas JSON complejas.
 * Por qué es importante (el impacto o problema que resuelve): Sin estas importaciones, no podríamos conectar Java con la base de datos SQL para generar estadísticas en tiempo real ni serializar la información al formato estructurado que espera el frontend.
 */
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

/*
 * Qué hace (la acción): Declara la clase AuditsServlet heredando de HttpServlet y la registra en el endpoint "/api/audits/*".
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - @WebServlet("/api/audits/*"): Anotación que mapea el servlet a cualquier petición que inicie con "/api/audits/". El comodín "*" permite recibir parámetros dinámicos en la ruta.
 * Para qué se usa (el propósito): Controlar la lógica de obtención de datos estadísticos e historiales para los distintos perfiles de administración (Supervisor, Administrador y Super Administrador).
 * Por qué es importante (el impacto o problema que resuelve): Centraliza toda la lógica de analítica y auditoría en un único controlador, evitando crear múltiples servlets para cada tipo de dashboard.
 */
@WebServlet("/api/audits/*")
public class AuditsServlet extends HttpServlet {

    /*
     * Qué hace (la acción): Sobrescribe el método doGet para canalizar las peticiones del cliente web hacia el dashboard correspondiente basándose en la subruta de la petición.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - request.getPathInfo(): Retorna la parte adicional de la URL después del mapeo del servlet (ej: "/dashBoardSupervisor").
     *   - response.setContentType / setCharacterEncoding: Configura el tipo de respuesta HTTP.
     * Para qué se usa (el propósito): Determinar qué método específico de consulta de base de datos se debe ejecutar según la llamada recibida.
     * Por qué es importante (el impacto o problema que resuelve): Permite bifurcar el flujo web de forma controlada y segura, respondiendo con un error HTTP 404 estructurado si la subruta solicitada no es válida.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        String pathInfo = request.getPathInfo();

        if (pathInfo != null && pathInfo.equals("/dashBoardSupervisor")) {
            obtenerDashBoardSupervisor(response, out);
        } 
        else if (pathInfo != null && pathInfo.equals("/dashBoardAdmin")) {
            obtenerDashBoardAdmin(response, out);
        }
        else if (pathInfo != null && pathInfo.equals("/dashBoardSuperAdmin")) {
            obtenerDashBoardSuperAdmin(response, out);
        }
        else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print(new JSONObject()
                    .put("success", false)
                    .put("message", "Endpoint de auditoría no encontrado").toString());
        }
    }

    /*
     * Qué hace (la acción): Realiza una consulta SQL de agregación para obtener las métricas de conteo de planes de emergencia y las imprime en formato JSON.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - SUM(CASE WHEN ...): Expresión condicional en SQL que suma 1 si el estado del plan coincide con la regla evaluada (ej: estado_id = 7 para Aprobados).
     *   - try-with-resources: Declara e inicializa Connection, PreparedStatement y ResultSet asegurando su cierre automático al terminar el bloque.
     * Para qué se usa (el propósito): Cargar el número de planes en estado pendiente, aprobados, rechazados y en revisión para las tarjetas informativas del panel del supervisor.
     * Por qué es importante (el impacto o problema que resuelve): Permite al supervisor tener un conteo general e inmediato del trabajo realizado por los voluntarios sin necesidad de listar y contar manualmente todos los planes en el frontend.
     */
    private void obtenerDashBoardSupervisor(HttpServletResponse response, PrintWriter out) {
        String sql = "SELECT "
                + "  SUM(CASE WHEN estado_id = 1 THEN 1 ELSE 0 END) as pending, "
                + "  SUM(CASE WHEN estado_id = 5 THEN 1 ELSE 0 END) as in_review, "
                + "  SUM(CASE WHEN estado_id = 7 THEN 1 ELSE 0 END) as approved, "
                + "  SUM(CASE WHEN estado_id IN (4, 6) THEN 1 ELSE 0 END) as rejected "
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

    /*
     * Qué hace (la acción): Consulta a la base de datos múltiples métricas de administración (resumen de usuarios activos/inactivos, conteo por rol, historial de cambios de miembros, historial de catálogos generales y tendencia de cambios de los últimos 6 meses) y las unifica en una respuesta JSON.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - SimpleDateFormat("dd/MM/yyyy HH:mm"): Formateador para convertir fechas SQL (Timestamp) a cadenas de texto legibles.
     *   - Calendar.getInstance(): Clase de Java para realizar operaciones de cálculo con fechas.
     *   - JSONArray: Lista estructurada de objetos JSON.
     * Para qué se usa (el propósito): Proveer al administrador del sistema las estadísticas demográficas, gráficos de actividad y auditorías en tiempo real sobre los datos maestros y cuentas del sistema.
     * Por qué es importante (el impacto o problema que resuelve): Consolida toda la información crítica de gobernanza del sistema en una única consulta integrada. El cálculo dinámico del historial de los últimos 6 meses dibuja correctamente la tendencia de uso de la plataforma.
     */
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
            JSONArray monthlyChanges = new JSONArray();
            String[] mesesNom = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
            
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, -5); // retrocedemos 5 meses en el tiempo para tener los últimos 6 meses
            
            for (int i = 0; i < 6; i++) {
                int monthNum = cal.get(Calendar.MONTH) + 1; 
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
                
                cal.add(Calendar.MONTH, 1); 
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

    /*
     * Qué hace (la acción): Consulta a la base de datos la distribución global de planes familiares por organización, por estado y el resumen de cuentas de usuarios para el super administrador.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - GROUP BY o.id, o.nombre: Agrupa los planes de acuerdo a la seccional de Cruz Roja a la que pertenece el voluntario.
     *   - COUNT(pf.id): Cuenta el volumen total de registros.
     * Para qué se usa (el propósito): Proveer métricas macro a nivel nacional u organizacional para la toma de decisiones estratégicas.
     * Por qué es importante (el impacto o problema que resuelve): Permite identificar qué seccionales tienen mayor participación de voluntarios y cuál es el estado general del desarrollo de planes familiares de emergencia.
     */
    private void obtenerDashBoardSuperAdmin(HttpServletResponse response, PrintWriter out) {
        try (Connection con = Conexion.obtener()) {

            // 1. Obtener planes por organización (plans_by_organization)
            String sqlPlansByOrg = "SELECT o.nombre AS organizacion, COUNT(pf.id) AS total "
                    + "FROM planes_familiares pf "
                    + "JOIN usuarios u ON pf.voluntario_id = u.id "
                    + "JOIN organizaciones o ON u.organizacion_id = o.id "
                    + "GROUP BY o.id, o.nombre "
                    + "ORDER BY total DESC";

            JSONArray plansByOrganization = new JSONArray();
            try (PreparedStatement ps = con.prepareStatement(sqlPlansByOrg);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject item = new JSONObject();
                    item.put("organization", rs.getString("organizacion"));
                    item.put("total", rs.getInt("total"));
                    plansByOrganization.put(item);
                }
            }

            // 2. Obtener planes por estado (plans_by_status)
            String sqlPlansByStatus = "SELECT ep.nombre AS estado, COUNT(pf.id) AS total "
                    + "FROM planes_familiares pf "
                    + "JOIN estados_plan ep ON pf.estado_id = ep.id "
                    + "GROUP BY ep.id, ep.nombre";

            JSONArray plansByStatus = new JSONArray();
            try (PreparedStatement ps = con.prepareStatement(sqlPlansByStatus);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject item = new JSONObject();
                    item.put("status", rs.getString("estado"));
                    item.put("total", rs.getInt("total"));
                    plansByStatus.put(item);
                }
            }

            // 3. Obtener conteo de usuarios por estado (users_by_status)
            String sqlSummary = "SELECT "
                    + "  SUM(CASE WHEN estado_id = 1 THEN 1 ELSE 0 END) as active, "
                    + "  SUM(CASE WHEN estado_id = 2 THEN 1 ELSE 0 END) as inactive, "
                    + "  SUM(CASE WHEN estado_id = 3 THEN 1 ELSE 0 END) as request "
                    + "FROM usuarios";

            JSONObject usersByStatus = new JSONObject();
            try (PreparedStatement ps = con.prepareStatement(sqlSummary);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    usersByStatus.put("active", rs.getInt("active"));
                    usersByStatus.put("inactive", rs.getInt("inactive"));
                    usersByStatus.put("pending", rs.getInt("request"));
                } else {
                    usersByStatus.put("active", 0);
                    usersByStatus.put("inactive", 0);
                    usersByStatus.put("pending", 0);
                }
            }

            // 4. Obtener conteo de usuarios por rol (roles)
            String sqlRols = "SELECT "
                    + "  SUM(CASE WHEN rol_id = 1 THEN 1 ELSE 0 END) as volunteer, "
                    + "  SUM(CASE WHEN rol_id = 2 THEN 1 ELSE 0 END) as supervisor "
                    + "FROM usuarios";

            JSONObject roles = new JSONObject();
            try (PreparedStatement ps = con.prepareStatement(sqlRols);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    roles.put("volunteer", rs.getInt("volunteer"));
                    roles.put("supervisor", rs.getInt("supervisor"));
                } else {
                    roles.put("volunteer", 0);
                    roles.put("supervisor", 0);
                }
            }

            // Unificar todos los datos en la respuesta
            JSONObject data = new JSONObject();
            data.put("plans_by_organization", plansByOrganization);
            data.put("plans_by_status", plansByStatus);
            data.put("users_by_status", usersByStatus);
            data.put("roles", roles);

            JSONObject jsonRes = new JSONObject();
            jsonRes.put("success", true);
            jsonRes.put("data", data);
            out.print(jsonRes.toString());

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(new JSONObject()
                    .put("success", false)
                    .put("message", "Error al obtener métricas de super administrador: " + e.getMessage()).toString());
        }
    }
}
