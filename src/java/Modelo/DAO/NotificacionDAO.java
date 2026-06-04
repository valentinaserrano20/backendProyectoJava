package Modelo.DAO;

import Modelo.Config.Conexion;
import Modelo.DTO.NotificacionDTO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para Notificaciones
 * Maneja las operaciones CRUD de notificaciones en la base de datos
 */
public class NotificacionDAO {

    /**
     * Crea una nueva notificación
     */
    public void crear(NotificacionDTO notificacion) throws SQLException {
        String sql = "INSERT INTO notificaciones (usuario_id, titulo, mensaje, tipo, leida, enlace, entidad_id) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, notificacion.getUsuarioId());
            ps.setString(2, notificacion.getTitulo());
            ps.setString(3, notificacion.getMensaje());
            ps.setString(4, notificacion.getTipo());
            ps.setBoolean(5, notificacion.isLeida());
            ps.setString(6, notificacion.getEnlace());
            ps.setInt(7, notificacion.getEntidadId());
            ps.executeUpdate();
        }
    }

    /**
     * Obtiene todas las notificaciones de un usuario
     */
    public List<NotificacionDTO> obtenerPorUsuario(int usuarioId) throws SQLException {
        String sql = "SELECT id, usuario_id, titulo, mensaje, tipo, leida, fecha_creacion, enlace, entidad_id "
                   + "FROM notificaciones WHERE usuario_id = ? ORDER BY fecha_creacion DESC";
        List<NotificacionDTO> notificaciones = new ArrayList<>();
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    NotificacionDTO notif = new NotificacionDTO();
                    notif.setId(rs.getInt("id"));
                    notif.setUsuarioId(rs.getInt("usuario_id"));
                    notif.setTitulo(rs.getString("titulo"));
                    notif.setMensaje(rs.getString("mensaje"));
                    notif.setTipo(rs.getString("tipo"));
                    notif.setLeida(rs.getBoolean("leida"));
                    notif.setFechaCreacion(rs.getString("fecha_creacion"));
                    notif.setEnlace(rs.getString("enlace"));
                    notif.setEntidadId(rs.getInt("entidad_id"));
                    notificaciones.add(notif);
                }
            }
        }
        return notificaciones;
    }

    /**
     * Obtiene las notificaciones no leídas de un usuario
     */
    public List<NotificacionDTO> obtenerNoLeidas(int usuarioId) throws SQLException {
        String sql = "SELECT id, usuario_id, titulo, mensaje, tipo, leida, fecha_creacion, enlace, entidad_id "
                   + "FROM notificaciones WHERE usuario_id = ? AND leida = false ORDER BY fecha_creacion DESC";
        List<NotificacionDTO> notificaciones = new ArrayList<>();
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    NotificacionDTO notif = new NotificacionDTO();
                    notif.setId(rs.getInt("id"));
                    notif.setUsuarioId(rs.getInt("usuario_id"));
                    notif.setTitulo(rs.getString("titulo"));
                    notif.setMensaje(rs.getString("mensaje"));
                    notif.setTipo(rs.getString("tipo"));
                    notif.setLeida(rs.getBoolean("leida"));
                    notif.setFechaCreacion(rs.getString("fecha_creacion"));
                    notif.setEnlace(rs.getString("enlace"));
                    notif.setEntidadId(rs.getInt("entidadId"));
                    notificaciones.add(notif);
                }
            }
        }
        return notificaciones;
    }

    /**
     * Cuenta las notificaciones no leídas de un usuario
     */
    public int contarNoLeidas(int usuarioId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notificaciones WHERE usuario_id = ? AND leida = false";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Marca una notificación como leída
     */
    public void marcarComoLeida(int notificacionId) throws SQLException {
        String sql = "UPDATE notificaciones SET leida = true WHERE id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, notificacionId);
            ps.executeUpdate();
        }
    }

    /**
     * Marca todas las notificaciones de un usuario como leídas
     */
    public void marcarTodasComoLeidas(int usuarioId) throws SQLException {
        String sql = "UPDATE notificaciones SET leida = true WHERE usuario_id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.executeUpdate();
        }
    }

    /**
     * Elimina una notificación
     */
    public void eliminar(int notificacionId) throws SQLException {
        String sql = "DELETE FROM notificaciones WHERE id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, notificacionId);
            ps.executeUpdate();
        }
    }
}
