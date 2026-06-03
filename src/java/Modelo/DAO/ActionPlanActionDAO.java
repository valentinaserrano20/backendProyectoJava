package Modelo.DAO;

import Modelo.Config.Conexion;
import Modelo.DTO.ActionPlanActionDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Qué hace: DAO encargado de realizar operaciones CRUD sobre la tabla plan_accion para micro-acciones específicas de las fases (antes, durante, después).
// Por qué existe: Provee acceso parametrizado a MySQL para registrar, modificar, listar y borrar tareas individuales por momento.
// Qué problema resuelve: Encapsula el acceso JDBC directo, previniendo inyección SQL y resolviendo dinámicamente claves foráneas como el riesgo_id.
public class ActionPlanActionDAO {

    // Qué hace: Obtiene el riesgo_id actualmente configurado para un plan familiar en la tabla plan_accion.
    // Por qué existe: Al agregar una nueva acción individual, el frontend no envía el riesgo_id; se debe consultar desde la configuración existente.
    // Qué problema resuelve: Asegura la consistencia referencial con la tabla de factores de riesgo sin requerir que la vista exponga o envíe datos redundantes.
    public int obtenerRiesgoIdPorPlan(int planId) throws SQLException {
        String sql = "SELECT riesgo_id FROM plan_accion WHERE plan_id = ? LIMIT 1";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("riesgo_id");
                }
            }
        }
        return 0;
    }

    // Qué hace: Obtiene la lista de todas las acciones/tareas específicas registradas para un plan familiar, uniendo los nombres de los responsables.
    // Por qué existe: Alimenta las tarjetas visuales de las tres pestañas en la pantalla del voluntario.
    // Qué problema resuelve: Realiza una unión (JOIN) con la tabla integrantes para retornar los nombres concatenados en una sola petición.
    public List<ActionPlanActionDTO> listarPorPlan(int planId) throws SQLException {
        String sql = "SELECT pa.id, pa.momento, pa.descripcion_tarea, pa.plan_id, pa.riesgo_id, pa.coordinador_id, "
                   + "i.nombre, i.apellido "
                   + "FROM plan_accion pa "
                   + "LEFT JOIN integrantes i ON pa.coordinador_id = i.id "
                   + "WHERE pa.plan_id = ?";
        
        List<ActionPlanActionDTO> lista = new ArrayList<>();
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ActionPlanActionDTO dto = new ActionPlanActionDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setDescription(rs.getString("descripcion_tarea"));
                    dto.setActionPlanId(rs.getInt("plan_id"));
                    
                    int coordId = rs.getInt("coordinador_id");
                    dto.setMemberId(coordId);
                    
                    String momentoStr = rs.getString("momento");
                    int actionTypeId = 1;
                    if ("durante".equals(momentoStr)) {
                        actionTypeId = 2;
                    } else if ("despues".equals(momentoStr)) {
                        actionTypeId = 3;
                    }
                    dto.setActionTypeId(actionTypeId);
                    
                    String nombre = rs.getString("nombre");
                    String apellido = rs.getString("apellido");
                    if (nombre != null && apellido != null) {
                        dto.setMemberName(nombre + " " + apellido);
                        dto.getMember().setNames(nombre);
                        dto.getMember().setLast_names(apellido);
                    } else {
                        dto.setMemberName("Sin coordinador");
                        dto.getMember().setNames("Sin");
                        dto.getMember().setLast_names("coordinador");
                    }
                    
                    lista.add(dto);
                }
            }
        }
        return lista;
    }

    // Qué hace: Obtiene los detalles completos de una micro-acción específica por su ID.
    // Por qué existe: Es consumido al abrir el modal "Ver/Editar/Eliminar" en la SPA.
    // Qué problema resuelve: Mapea los campos de una sola fila de base de datos junto con el responsable en un DTO.
    public ActionPlanActionDTO obtenerPorId(int id) throws SQLException {
        String sql = "SELECT pa.id, pa.momento, pa.descripcion_tarea, pa.plan_id, pa.riesgo_id, pa.coordinador_id, "
                   + "i.nombre, i.apellido "
                   + "FROM plan_accion pa "
                   + "LEFT JOIN integrantes i ON pa.coordinador_id = i.id "
                   + "WHERE pa.id = ?";
        
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ActionPlanActionDTO dto = new ActionPlanActionDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setDescription(rs.getString("descripcion_tarea"));
                    dto.setActionPlanId(rs.getInt("plan_id"));
                    
                    int coordId = rs.getInt("coordinador_id");
                    dto.setMemberId(coordId);
                    
                    String momentoStr = rs.getString("momento");
                    int actionTypeId = 1;
                    if ("durante".equals(momentoStr)) {
                        actionTypeId = 2;
                    } else if ("despues".equals(momentoStr)) {
                        actionTypeId = 3;
                    }
                    dto.setActionTypeId(actionTypeId);
                    
                    String nombre = rs.getString("nombre");
                    String apellido = rs.getString("apellido");
                    if (nombre != null && apellido != null) {
                        dto.setMemberName(nombre + " " + apellido);
                        dto.getMember().setNames(nombre);
                        dto.getMember().setLast_names(apellido);
                    } else {
                        dto.setMemberName("Sin coordinador");
                        dto.getMember().setNames("Sin");
                        dto.getMember().setLast_names("coordinador");
                    }
                    
                    return dto;
                }
            }
        }
        return null;
    }

    // Qué hace: Inserta una nueva micro-acción en la tabla plan_accion.
    // Por qué existe: Permite agregar tareas personalizadas en cualquiera de los tres momentos del plan.
    // Qué problema resuelve: Resuelve y asocia de forma atómica el riesgo_id y maneja la asignación de nulos si no hay miembro id.
    public void crear(ActionPlanActionDTO dto) throws SQLException {
        int riesgoId = obtenerRiesgoIdPorPlan(dto.getActionPlanId());
        
        String sql = "INSERT INTO plan_accion (momento, descripcion_tarea, plan_id, riesgo_id, coordinador_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            String momentoStr = "antes";
            if (dto.getActionTypeId() == 2) {
                momentoStr = "durante";
            } else if (dto.getActionTypeId() == 3) {
                momentoStr = "despues";
            }
            
            ps.setString(1, momentoStr);
            ps.setString(2, dto.getDescription());
            ps.setInt(3, dto.getActionPlanId());
            ps.setInt(4, riesgoId);
            
            if (dto.getMemberId() > 0) {
                ps.setInt(5, dto.getMemberId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            
            ps.executeUpdate();
        }
    }

    // Qué hace: Actualiza la descripción y el miembro coordinador responsable para una tarea específica.
    // Por qué existe: Atiende la solicitud del usuario al editar una acción en los modales.
    // Qué problema resuelve: Modifica únicamente las columnas deseadas sin tocar llaves del plan o momento.
    public void actualizar(int id, ActionPlanActionDTO dto) throws SQLException {
        String sql = "UPDATE plan_accion SET descripcion_tarea = ?, coordinador_id = ? WHERE id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, dto.getDescription());
            
            if (dto.getMemberId() > 0) {
                ps.setInt(2, dto.getMemberId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    // Qué hace: Elimina físicamente el registro de la tarea de la base de datos por su ID.
    // Por qué existe: Permite descartar micro-acciones desde el modal de eliminación.
    // Qué problema resuelve: Borra el registro en MySQL liberando memoria y actualizando la interfaz SPA al recargar.
    public void eliminar(int id) throws SQLException {
        String sql = "DELETE FROM plan_accion WHERE id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
