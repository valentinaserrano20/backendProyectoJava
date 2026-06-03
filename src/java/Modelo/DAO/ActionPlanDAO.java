package Modelo.DAO;

import Modelo.Config.Conexion;
import Modelo.DTO.ActionPlanDTO;
import java.sql.*;

// Qué hace: DAO encargado de realizar operaciones de lectura, escritura y actualización para la cabecera general del Plan de Acción en la tabla plan_accion.
// Por qué existe: Encapsula el acceso directo a la base de datos MySQL usando sentencias preparadas de JDBC.
// Qué problema resuelve: Separa las consultas SQL de la capa de servicio, previene la inyección SQL y coordina transacciones complejas de inicialización de tareas.
public class ActionPlanDAO {

    // Qué hace: Comprueba si existe al menos una tarea registrada para un plan familiar en la tabla plan_accion.
    // Por qué existe: Permite alimentar la verificación de existencia (boolean) consumida en los controladores del frontend para activar o desactivar la interfaz de sub-acciones.
    // Qué problema resuelve: Retorna un indicador simple reduciendo el consumo de memoria al no cargar objetos completos.
    public boolean existePlan(int planId) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM plan_accion WHERE plan_id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total") > 0;
                }
            }
        }
        return false;
    }

    // Qué hace: Obtiene la cabecera de configuración general (coordinador y riesgo) leyendo el primer registro asociado al plan.
    // Por qué existe: Carga los datos de preselección en los dropdowns del formulario superior en las vistas SPA.
    // Qué problema resuelve: Mapea la información cruda de la base de datos hacia un objeto estructurado DTO.
    public ActionPlanDTO obtenerPlan(int planId) throws SQLException {
        String sql = "SELECT id, plan_id, riesgo_id, coordinador_id FROM plan_accion WHERE plan_id = ? LIMIT 1";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ActionPlanDTO dto = new ActionPlanDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setFamilyPlanId(rs.getInt("plan_id"));
                    dto.setMemberId(rs.getInt("coordinador_id"));
                    dto.setRiskFactorId(rs.getInt("riesgo_id"));
                    return dto;
                }
            }
        }
        return null;
    }

    // Qué hace: Inicializa transaccionalmente el plan de acción insertando 3 tareas base por defecto (antes, durante y después).
    // Por qué existe: Asegura que el plan de acción familiar inicie con las tareas guías necesarias para que la UI de la SPA funcione fluidamente.
    // Qué problema resuelve: Crea la estructura requerida en base de datos en una única transacción atómica para prevenir estados corruptos o parciales.
    public void crear(int planId, int coordinatorId, int riskFactorId) throws SQLException {
        String sql = "INSERT INTO plan_accion (momento, descripcion_tarea, plan_id, riesgo_id, coordinador_id) VALUES (?, ?, ?, ?, ?)";
        Connection con = null;
        try {
            con = Conexion.obtener();
            con.setAutoCommit(false); // Inicia la transacción manual
            
            // Momentos base y descripciones sugeridas por defecto
            String[] momentos = {"antes", "durante", "despues"};
            String[] descripciones = {
                "Definir medidas de prevención y preparación del hogar.",
                "Coordinar la evacuación y el punto de encuentro familiar.",
                "Evaluar afectaciones y coordinar la rehabilitación de servicios."
            };
            
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                for (int i = 0; i < momentos.length; i++) {
                    ps.setString(1, momentos[i]);
                    ps.setString(2, descripciones[i]);
                    ps.setInt(3, planId);
                    ps.setInt(4, riskFactorId);
                    
                    if (coordinatorId > 0) {
                        ps.setInt(5, coordinatorId);
                    } else {
                        ps.setNull(5, Types.INTEGER);
                    }
                    
                    ps.executeUpdate();
                }
            }
            con.commit(); // Confirma la inserción de las tres filas
        } catch (SQLException e) {
            if (con != null) {
                con.rollback(); // Deshace los cambios en caso de error
            }
            throw e;
        } finally {
            if (con != null) {
                con.close();
            }
        }
    }

    // Qué hace: Actualiza el coordinador y factor de riesgo para todas las tareas del plan de acción.
    // Por qué existe: Permite modificar la cabecera del plan (por ejemplo, cambiar el líder de emergencia familiar) propagando el cambio a todas las tareas de la matriz.
    // Qué problema resuelve: Actualiza de forma masiva los registros asociados mediante una consulta preparada única.
    public void actualizar(int planId, int coordinatorId, int riskFactorId) throws SQLException {
        String sql = "UPDATE plan_accion SET coordinador_id = ?, riesgo_id = ? WHERE plan_id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            if (coordinatorId > 0) {
                ps.setInt(1, coordinatorId);
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setInt(2, riskFactorId);
            ps.setInt(3, planId);
            
            ps.executeUpdate();
        }
    }
}
