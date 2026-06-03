package Modelo.DAO;

import Modelo.Config.Conexion;
import Modelo.DTO.RecursoDisponibleDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Qué hace: DAO encargado de realizar operaciones de lectura, escritura y eliminación física en base de datos para la entidad de Recursos Disponibles.
// Por qué existe: Encapsula el acceso directo a la base de datos MySQL usando sentencias preparadas de JDBC.
// Qué problema resuelve: Separa el código de acceso a datos de la capa de lógica de negocio y presentación, previniendo la inyección SQL y manteniendo la arquitectura limpia.
public class RecursoDisponibleDAO {

    // Qué hace: Cuenta la cantidad total de recursos comunitarios registrados para un plan familiar específico.
    // Por qué existe: Suministra el total al servicio para realizar el cálculo de los metadatos de paginación requeridos por el frontend.
    // Qué problema resuelve: Evita transferir toda la lista de filas por red solo para realizar el conteo de registros.
    public int obtenerTotalRecursos(int planId) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM recursos_disponibles WHERE plan_id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        }
        return 0;
    }

    // Qué hace: Consulta un listado de recursos comunitarios asociados a un plan familiar, uniendo con el tipo de recurso y servicio de emergencia.
    // Por qué existe: Alimenta la vista principal del frontend con la información completa de cada recurso registrado.
    // Qué problema resuelve: Resuelve la necesidad de mostrar información relacional legible (nombre del tipo y servicio de emergencia) en lugar de IDs crudos.
    public List<RecursoDisponibleDTO> listarRecursosPorPlan(int planId, int limit, int offset) throws SQLException {
        String sql = "SELECT r.id, r.nombre_lugar, r.distancia_metros, r.telefono, r.descripcion, r.tipo_recurso_id, "
                   + "tr.nombre AS resource_name, se.nombre AS service_name "
                   + "FROM recursos_disponibles r "
                   + "LEFT JOIN tipos_recurso tr ON r.tipo_recurso_id = tr.id "
                   + "LEFT JOIN servicios_emergencia se ON tr.servicio_id = se.id "
                   + "WHERE r.plan_id = ? "
                   + "LIMIT ? OFFSET ?";
        
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            
            List<RecursoDisponibleDTO> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RecursoDisponibleDTO dto = new RecursoDisponibleDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setPlaceName(rs.getString("nombre_lugar"));
                    dto.setDistance(rs.getInt("distancia_metros"));
                    dto.setPhone(rs.getString("telefono") != null ? rs.getString("telefono") : "No registrado");
                    dto.setDescription(rs.getString("descripcion") != null ? rs.getString("descripcion") : "");
                    dto.setResourceTypeId(rs.getInt("tipo_recurso_id"));
                    dto.setResourceTypeName(rs.getString("resource_name") != null ? rs.getString("resource_name") : "No especificado");
                    dto.setServiceName(rs.getString("service_name") != null ? rs.getString("service_name") : "Otro");
                    lista.add(dto);
                }
            }
            return lista;
        }
    }

    // Qué hace: Consulta un recurso comunitario disponible a través de su identificador único ID.
    // Por qué existe: Permite alimentar los detalles de visualización (modal) o cargar el formulario de edición con los datos correctos del recurso.
    // Qué problema resuelve: Recupera la información de un único registro de forma directa y atómica en base de datos.
    public RecursoDisponibleDTO obtenerRecurso(int id) throws SQLException {
        String sql = "SELECT r.id, r.nombre_lugar, r.distancia_metros, r.telefono, r.descripcion, r.plan_id, r.tipo_recurso_id, "
                   + "tr.nombre AS resource_name, se.nombre AS service_name "
                   + "FROM recursos_disponibles r "
                   + "LEFT JOIN tipos_recurso tr ON r.tipo_recurso_id = tr.id "
                   + "LEFT JOIN servicios_emergencia se ON tr.servicio_id = se.id "
                   + "WHERE r.id = ?";
        
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    RecursoDisponibleDTO dto = new RecursoDisponibleDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setPlaceName(rs.getString("nombre_lugar"));
                    dto.setDistance(rs.getInt("distancia_metros"));
                    dto.setPhone(rs.getString("telefono"));
                    dto.setDescription(rs.getString("descripcion") != null ? rs.getString("descripcion") : "");
                    dto.setPlanId(rs.getInt("plan_id"));
                    dto.setResourceTypeId(rs.getInt("tipo_recurso_id"));
                    dto.setResourceTypeName(rs.getString("resource_name") != null ? rs.getString("resource_name") : "");
                    dto.setServiceName(rs.getString("service_name") != null ? rs.getString("service_name") : "");
                    return dto;
                }
            }
        }
        return null;
    }

    // Qué hace: Inserta un nuevo registro de recurso disponible en la tabla correspondiente y devuelve el ID autogenerado.
    // Por qué existe: Facilita el guardado permanente de un recurso asociado al plan de emergencia de la familia.
    // Qué problema resuelve: Mapea la información capturada en el DTO hacia las columnas físicas del motor MySQL de forma parametrizada.
    public int crearRecurso(RecursoDisponibleDTO dto) throws SQLException {
        String sql = "INSERT INTO recursos_disponibles (nombre_lugar, distancia_metros, telefono, descripcion, plan_id, tipo_recurso_id) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setString(1, dto.getPlaceName());
            ps.setInt(2, dto.getDistance());
            ps.setString(3, dto.getPhone() != null && !dto.getPhone().isEmpty() ? dto.getPhone() : null);
            ps.setString(4, dto.getDescription() != null && !dto.getDescription().isEmpty() ? dto.getDescription() : null);
            ps.setInt(5, dto.getPlanId());
            
            if (dto.getResourceTypeId() > 0) {
                ps.setInt(6, dto.getResourceTypeId());
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            
            ps.executeUpdate();
            
            try (ResultSet rsKeys = ps.getGeneratedKeys()) {
                if (rsKeys.next()) {
                    return rsKeys.getInt(1);
                }
            }
        }
        throw new SQLException("No se pudo obtener el ID autogenerado del recurso.");
    }

    // Qué hace: Actualiza los campos específicos de un recurso disponible por su identificador único ID.
    // Por qué existe: Permite modificar la información geográfica, teléfono o tipo de recurso de forma directa.
    // Qué problema resuelve: Guarda los cambios editados por el voluntario de forma segura sin tocar otros campos.
    public void actualizarRecurso(int id, RecursoDisponibleDTO dto) throws SQLException {
        String sql = "UPDATE recursos_disponibles SET nombre_lugar = ?, distancia_metros = ?, telefono = ?, descripcion = ?, tipo_recurso_id = ? "
                   + "WHERE id = ?";
        
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, dto.getPlaceName());
            ps.setInt(2, dto.getDistance());
            ps.setString(3, dto.getPhone() != null && !dto.getPhone().isEmpty() ? dto.getPhone() : null);
            ps.setString(4, dto.getDescription() != null && !dto.getDescription().isEmpty() ? dto.getDescription() : null);
            
            if (dto.getResourceTypeId() > 0) {
                ps.setInt(5, dto.getResourceTypeId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            
            ps.setInt(6, id);
            ps.executeUpdate();
        }
    }

    // Qué hace: Elimina físicamente un registro de recurso disponible de la base de datos MySQL por su ID.
    // Por qué existe: Habilita la baja o eliminación de recursos erróneos cargados por el voluntario.
    // Qué problema resuelve: Borra el registro de forma atómica y segura mediante JDBC.
    public void eliminarRecurso(int id) throws SQLException {
        String sql = "DELETE FROM recursos_disponibles WHERE id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
