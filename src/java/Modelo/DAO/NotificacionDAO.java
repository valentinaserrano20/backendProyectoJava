package Modelo.DAO;

import Modelo.Config.Conexion;
import Modelo.DTO.NotificacionDTO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Qué hace: DAO para Notificaciones. Maneja las operaciones de persistencia (creación, lectura, actualización y eliminación) en base de datos.
// Por qué existe: Separa la gestión directa de la tabla notificaciones en MySQL de la lógica de negocio y controladores del sistema.
// Qué problema resuelve: Centraliza las consultas JDBC de alertas y previene la inyección SQL mediante sentencias preparadas parametrizadas.
public class NotificacionDAO {

    // Sirve para: Crear una nueva notificación para un usuario específico.
    // Qué hace: Realiza un INSERT parametrizado en la tabla notificaciones.
    // Explicación de consulta SQL:
    // - Información buscada: Registro de campos de notificación en notificaciones.
    // - Tablas participantes: notificaciones.
    public void crear(NotificacionDTO notificacion) throws SQLException {
        String sql = "INSERT INTO notificaciones (usuario_id, titulo, mensaje, tipo, leida, enlace, entidad_id) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        // Qué hace: Abre la conexión a la base de datos y compila el PreparedStatement.
        // Por qué existe: Habilita la inserción parametrizada segura.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Vincula los atributos del DTO de notificación a los marcadores de la consulta.
            ps.setInt(1, notificacion.getUsuarioId());
            ps.setString(2, notificacion.getTitulo());
            ps.setString(3, notificacion.getMensaje());
            ps.setString(4, notificacion.getTipo());
            ps.setBoolean(5, notificacion.isLeida());
            ps.setString(6, notificacion.getEnlace());
            ps.setInt(7, notificacion.getEntidadId());
            // Qué hace: Ejecuta la sentencia INSERT en MySQL.
            ps.executeUpdate();
        }
    }

    // Sirve para: Obtener todas las notificaciones registradas de un usuario.
    // Qué hace: Realiza una consulta SELECT a la tabla notificaciones ordenada por fecha.
    // Explicación de consulta SQL:
    // - Información buscada: Columnas id, usuario_id, titulo, mensaje, tipo, leida, fecha_creacion, enlace y entidad_id.
    // - Tablas participantes: notificaciones.
    // - Filtros aplicados: usuario_id = ? (notificaciones pertenecientes al usuario), ordenadas descendente por fecha_creacion.
    public List<NotificacionDTO> obtenerPorUsuario(int usuarioId) throws SQLException {
        String sql = "SELECT id, usuario_id, titulo, mensaje, tipo, leida, fecha_creacion, enlace, entidad_id "
                   + "FROM notificaciones WHERE usuario_id = ? ORDER BY fecha_creacion DESC";
        // Qué hace: Inicializa la lista que contendrá las notificaciones del usuario.
        List<NotificacionDTO> notificaciones = new ArrayList<>();
        // Qué hace: Abre la conexión y compila la consulta.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Asigna el ID del usuario al statement.
            ps.setInt(1, usuarioId);
            // Qué hace: Ejecuta la consulta de lectura.
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Qué hace: Instancia el DTO y mapea cada columna del ResultSet.
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
                    // Qué hace: Agrega el DTO poblado al listado de retorno.
                    notificaciones.add(notif);
                }
            }
        }
        // Qué hace: Retorna la lista de notificaciones.
        return notificaciones;
    }

    // Sirve para: Obtener las notificaciones no leídas de un usuario.
    // Qué hace: Realiza una consulta SELECT a la tabla notificaciones trayendo los registros pendientes de lectura.
    // Explicación de consulta SQL:
    // - Información buscada: Atributos de las notificaciones sin leer.
    // - Tablas participantes: notificaciones.
    // - Filtros aplicados: usuario_id = ? AND leida = false, ordenadas descendentemente por fecha_creacion.
    public List<NotificacionDTO> obtenerNoLeidas(int usuarioId) throws SQLException {
        String sql = "SELECT id, usuario_id, titulo, mensaje, tipo, leida, fecha_creacion, enlace, entidad_id "
                   + "FROM notificaciones WHERE usuario_id = ? AND leida = false ORDER BY fecha_creacion DESC";
        // Qué hace: Inicializa la lista que almacenará las notificaciones no leídas.
        List<NotificacionDTO> notificaciones = new ArrayList<>();
        // Qué hace: Abre la conexión y prepara el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Vincula el ID del usuario al primer marcador.
            ps.setInt(1, usuarioId);
            // Qué hace: Ejecuta la consulta.
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Qué hace: Instancia el DTO y realiza el mapeo de cada columna.
                    NotificacionDTO notif = new NotificacionDTO();
                    notif.setId(rs.getInt("id"));
                    notif.setUsuarioId(rs.getInt("usuario_id"));
                    notif.setTitulo(rs.getString("titulo"));
                    notif.setMensaje(rs.getString("mensaje"));
                    notif.setTipo(rs.getString("tipo"));
                    notif.setLeida(rs.getBoolean("leida"));
                    notif.setFechaCreacion(rs.getString("fecha_creacion"));
                    notif.setEnlace(rs.getString("enlace"));
                    // Qué hace: Corrección de bug: Se mapea con 'entidad_id' para coincidir con la consulta SQL seleccionada.
                    notif.setEntidadId(rs.getInt("entidad_id"));
                    // Qué hace: Agrega el DTO a la lista.
                    notificaciones.add(notif);
                }
            }
        }
        // Qué hace: Retorna la lista resultante de notificaciones no leídas.
        return notificaciones;
    }

    // Sirve para: Contar las notificaciones no leídas de un usuario.
    // Qué hace: Realiza una consulta SELECT COUNT(*) en la tabla notificaciones.
    // Explicación de consulta SQL:
    // - Información buscada: El total de notificaciones pendientes de leer del usuario.
    // - Tablas participantes: notificaciones.
    // - Filtros aplicados: usuario_id = ? AND leida = false.
    public int contarNoLeidas(int usuarioId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notificaciones WHERE usuario_id = ? AND leida = false";
        // Qué hace: Abre la conexión a la base de datos y compila el PreparedStatement.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Vincula el ID del usuario al primer parámetro.
            ps.setInt(1, usuarioId);
            // Qué hace: Ejecuta la consulta de conteo en MySQL.
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Qué hace: Recupera el valor entero del COUNT.
                    return rs.getInt(1);
                }
            }
        }
        // Qué hace: Retorna 0 por defecto si no produjo resultados.
        return 0;
    }

    // Sirve para: Marca una notificación específica como leída.
    // Qué hace: Realiza un UPDATE en la tabla notificaciones cambiando el estado de lectura a verdadero.
    // Explicación de consulta SQL:
    // - Información buscada: Modificar la columna leida.
    // - Tablas participantes: notificaciones.
    // - Filtros aplicados: id = ? (id de la notificación a modificar).
    public void marcarComoLeida(int notificacionId) throws SQLException {
        String sql = "UPDATE notificaciones SET leida = true WHERE id = ?";
        // Qué hace: Abre la conexión JDBC y prepara el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Asigna el ID de notificación al placeholder del WHERE.
            ps.setInt(1, notificacionId);
            // Qué hace: Ejecuta la actualización en base de datos.
            ps.executeUpdate();
        }
    }

    // Sirve para: Marca todas las notificaciones de un usuario como leídas de una sola vez.
    // Qué hace: Realiza un UPDATE en la tabla notificaciones cambiando el estado de lectura.
    // Explicación de consulta SQL:
    // - Información buscada: Modificar la columna leida.
    // - Tablas participantes: notificaciones.
    // - Filtros aplicados: usuario_id = ? (todas las notificaciones pertenecientes al usuario).
    public void marcarTodasComoLeidas(int usuarioId) throws SQLException {
        String sql = "UPDATE notificaciones SET leida = true WHERE usuario_id = ?";
        // Qué hace: Abre la conexión a base de datos y compila el PreparedStatement.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Vincula el ID del usuario.
            ps.setInt(1, usuarioId);
            // Qué hace: Ejecuta el update de lectura masiva.
            ps.executeUpdate();
        }
    }

    // Sirve para: Eliminar una notificación específica de la base de datos.
    // Qué hace: Ejecuta una sentencia DELETE física sobre el registro de la notificación.
    // Explicación de consulta SQL:
    // - Información buscada: Eliminar el registro.
    // - Tablas participantes: notificaciones.
    // - Filtros aplicados: id = ?.
    public void eliminar(int notificacionId) throws SQLException {
        String sql = "DELETE FROM notificaciones WHERE id = ?";
        // Qué hace: Abre la conexión JDBC y prepara el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Asigna el ID de notificación y realiza el borrado físico.
            ps.setInt(1, notificacionId);
            ps.executeUpdate();
        }
    }
}
