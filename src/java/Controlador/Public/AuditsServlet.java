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
        // Qué hace: Valida si la ruta solicitada corresponde al dashboard de super administrador.
        // Por qué existe: Canaliza la petición para calcular las métricas de planes por organización y estado.
        // Qué problema resuelve: Enruta la petición al método correspondiente a la auditoría del super administrador.
        else if (pathInfo != null && pathInfo.equals("/dashBoardSuperAdmin")) {
            obtenerDashBoardSuperAdmin(response, out);
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
    // Qué problema resuelve: Reúne los totales de planes aprobados, rechazados y pendientes (enviados) excluyendo borradores.
    private void obtenerDashBoardSupervisor(HttpServletResponse response, PrintWriter out) {
        // Qué hace: Define la consulta SQL de agregación sumando planes por estado excluyendo borradores (estados 2 y 3).
        // Por qué existe: Obtiene las métricas en una única consulta agregada optimizando el rendimiento.
        // Qué problema resuelve: Evita procesar múltiples lecturas o bucles para calcular conteos de estados.
        // Explicación de consulta SQL:
        // - Información buscada: Conteo total de planes pendientes (enviados), aprobados y rechazados.
        // - Tablas participantes: planes_familiares.
        // - Filtros aplicados: Ninguno explícito en WHERE ya que se filtra con condicionales CASE WHEN para contar solo estados enviados (1) y evaluados (4, 7, 5, 6).
        // Qué hace: Ejecuta una consulta SQL de agregación para totalizar los planes según su estado.
        // Por qué existe: Permite alimentar las métricas estadísticas del supervisor en el dashboard del home.
        // Qué problema resuelve: Corrige la asignación errónea de totales donde planes rechazados contaban como aprobados, y calcula planes en revisión.
        String sql = "SELECT "
                + "  SUM(CASE WHEN estado_id = 1 THEN 1 ELSE 0 END) as pending, "
                + "  SUM(CASE WHEN estado_id = 5 THEN 1 ELSE 0 END) as in_review, "
                + "  SUM(CASE WHEN estado_id = 7 THEN 1 ELSE 0 END) as approved, "
                + "  SUM(CASE WHEN estado_id IN (4, 6) THEN 1 ELSE 0 END) as rejected "
                + "FROM planes_familiares";

        // Qué hace: Abre conexión JDBC limpia y compila la sentencia para su ejecución.
        // Por qué existe: Permite interactuar directamente con la base de datos de manera segura.
        // Qué problema resuelve: Previene fugas de recursos cerrando la conexión y el statement automáticamente.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            // Qué hace: Crea el objeto JSON de salida que contendrá el mapa de métricas.
            // Por qué existe: Serializa los resultados en el formato estructurado esperado por el controlador JS.
            // Qué problema resuelve: Adapta los tipos de datos de base de datos a un formato consumible por la web SPA.
            JSONObject data = new JSONObject();
            // Qué hace: Evalúa si la consulta devolvió resultados válidos.
            // Por qué existe: Recupera los totales acumulados del ResultSet de base de datos.
            // Qué problema resuelve: Asigna fallback de 0 si no se encuentran filas.
            if (rs.next()) {
                // Qué hace: Asigna cada valor numérico calculado a su clave correspondiente en el JSON.
                // Por qué existe: Llena las propiedades para los badges y tarjetas de métricas del frontend.
                // Qué problema resuelve: Reemplaza los datos vacíos con conteos reales en tiempo real.
                data.put("pending_plans", rs.getInt("pending"));
                data.put("approved_plans", rs.getInt("approved"));
                data.put("rejected_plans", rs.getInt("rejected"));
                data.put("in_review_plans", rs.getInt("in_review"));
            } else {
                // Qué hace: Inicializa los valores en cero en caso de no existir registros en la tabla.
                // Por qué existe: Asegura que el frontend reciba un contrato válido con valores numéricos.
                // Qué problema resuelve: Previene errores de ejecución por propiedades indefinidas en JS.
                data.put("pending_plans", 0);
                data.put("approved_plans", 0);
                data.put("rejected_plans", 0);
                data.put("in_review_plans", 0);
            }

            // Qué hace: Encapsula el nodo de datos en el formato de respuesta general de la API.
            // Por qué existe: Envuelve el objeto en la estructura estándar de respuesta del servidor {success, data}.
            // Qué problema resuelve: Cumple el protocolo de comunicación establecido con el cliente.
            JSONObject jsonRes = new JSONObject();
            jsonRes.put("success", true);
            jsonRes.put("data", data);
            // Qué hace: Imprime la respuesta JSON en el flujo de salida del Servlet.
            // Por qué existe: Envía de vuelta el stream de texto JSON al navegador del cliente.
            // Qué problema resuelve: Completa la petición HTTP del cliente de manera exitosa.
            out.print(jsonRes.toString());

        } catch (Exception e) {
            // Qué hace: Captura cualquier excepción de base de datos e imprime el stack trace.
            // Por qué existe: Registra los errores en el log del servidor para depuración técnica.
            // Qué problema resuelve: Previene la caída total del hilo de Tomcat enviando un error HTTP estructurado.
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

    // Qué hace: Consulta planes por organización, planes por estado, usuarios por estado y roles para el super administrador.
    // Por qué existe: Obtiene métricas consolidadas para el dashboard del super administrador con datos de planes y usuarios.
    // Qué problema resuelve: Provee en una sola respuesta JSON los datos de planes por organización, estado y métricas de usuarios.
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

            // 3. Obtener conteo de usuarios por estado (users_by_status) - reutilizando lógica de dashBoardAdmin
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

            // 4. Obtener conteo de usuarios por rol (roles) - reutilizando lógica de dashBoardAdmin
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
