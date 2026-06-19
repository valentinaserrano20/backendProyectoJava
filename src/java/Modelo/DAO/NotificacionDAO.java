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
        // Sentencia SQL que especifica las columnas de la tabla notificaciones que recibirán los valores.
        String sql = "INSERT INTO notificaciones (usuario_id, titulo, mensaje, tipo, leida, enlace, entidad_id) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
                   
        // try-with-resources: Abre la conexión JDBC y prepara el statement de forma segura, asegurando el cierre automático.
        try (Connection con = Conexion.obtener(); // Solicita una conexión activa a la base de datos MySQL.
             PreparedStatement ps = con.prepareStatement(sql)) { // Compila la consulta INSERT de forma segura.
             
            // Vincula el ID del usuario destinatario al primer parámetro '?'.
            ps.setInt(1, notificacion.getUsuarioId());
            // Vincula el título de la alerta o notificación al segundo parámetro '?'.
            ps.setString(2, notificacion.getTitulo());
            // Vincula el contenido o mensaje detallado de la alerta al tercer parámetro '?'.
            ps.setString(3, notificacion.getMensaje());
            // Vincula el tipo de notificación (ej. "vivienda", "riesgo") al cuarto parámetro '?'.
            ps.setString(4, notificacion.getTipo());
            // Vincula el estado booleano de lectura (inicialmente falso) al quinto parámetro '?'.
            ps.setBoolean(5, notificacion.isLeida());
            // Vincula el enlace o ruta de redirección en la SPA al sexto parámetro '?'.
            ps.setString(6, notificacion.getEnlace());
            // Vincula el ID de la entidad relacionada (ej. plan familiar ID) al séptimo parámetro '?'.
            ps.setInt(7, notificacion.getEntidadId());
            
            // Ejecuta la inserción física del registro en la base de datos MySQL.
            ps.executeUpdate();
        }
    }

    // Sirve para: Obtener todas las notificaciones registradas de un usuario.
    // Qué hace: Realiza una consulta SELECT a la tabla notificaciones ordenada por fecha.
    // Explicación de consulta SQL:
    // - Columnas seleccionadas: id, usuario_id, titulo, mensaje, tipo, leida, fecha_creacion, enlace y entidad_id.
    // - Filtro aplicado: WHERE usuario_id = ? (selecciona únicamente las alertas asignadas a este usuario).
    // - Ordenamiento: ORDER BY fecha_creacion DESC (las notificaciones más recientes aparecen al principio).
    public List<NotificacionDTO> obtenerPorUsuario(int usuarioId) throws SQLException {
        String sql = "SELECT id, usuario_id, titulo, mensaje, tipo, leida, fecha_creacion, enlace, entidad_id "
                   + "FROM notificaciones WHERE usuario_id = ? ORDER BY fecha_creacion DESC";
                   
        // Inicializa la lista dinámica que contendrá las notificaciones recuperadas.
        List<NotificacionDTO> notificaciones = new ArrayList<>();
        
        // try-with-resources: Inicializa y administra de forma segura la conexión y el statement de lectura.
        try (Connection con = Conexion.obtener(); // Obtiene la conexión activa de MySQL.
             PreparedStatement ps = con.prepareStatement(sql)) { // Prepara la consulta SELECT.
             
            // Vincula el ID del usuario en sesión al primer parámetro '?'.
            ps.setInt(1, usuarioId);
            
            // Ejecuta la consulta de selección y almacena el resultado en un ResultSet.
            try (ResultSet rs = ps.executeQuery()) {
                // Itera sobre cada fila devuelta por la base de datos
                while (rs.next()) {
                    // Instancia un nuevo DTO para mapear las columnas de la fila actual
                    NotificacionDTO notif = new NotificacionDTO();
                    notif.setId(rs.getInt("id")); // Obtiene el identificador único.
                    notif.setUsuarioId(rs.getInt("usuario_id")); // Obtiene el ID de usuario.
                    notif.setTitulo(rs.getString("titulo")); // Obtiene el título.
                    notif.setMensaje(rs.getString("mensaje")); // Obtiene el cuerpo del mensaje.
                    notif.setTipo(rs.getString("tipo")); // Obtiene el tipo de alerta.
                    notif.setLeida(rs.getBoolean("leida")); // Obtiene el indicador de lectura.
                    notif.setFechaCreacion(rs.getString("fecha_creacion")); // Obtiene la fecha de registro.
                    notif.setEnlace(rs.getString("enlace")); // Obtiene el enlace SPA.
                    notif.setEntidadId(rs.getInt("entidad_id")); // Obtiene el identificador relacional.
                    
                    // Agrega el DTO a la lista de retorno.
                    notificaciones.add(notif);
                }
            }
        }
        // Retorna la colección con todas las notificaciones encontradas del usuario.
        return notificaciones;
    }

    // Sirve para: Obtener las notificaciones no leídas de un usuario.
    // Qué hace: Realiza una consulta SELECT a la tabla notificaciones trayendo los registros pendientes de lectura.
    // Explicación de consulta SQL:
    // - Columnas seleccionadas: id, usuario_id, titulo, mensaje, tipo, leida, fecha_creacion, enlace y entidad_id.
    // - Filtro aplicado: WHERE usuario_id = ? AND leida = false (notificaciones pertenecientes al usuario que no han sido vistas).
    // - Ordenamiento: ORDER BY fecha_creacion DESC (orden cronológico inverso).
    public List<NotificacionDTO> obtenerNoLeidas(int usuarioId) throws SQLException {
        String sql = "SELECT id, usuario_id, titulo, mensaje, tipo, leida, fecha_creacion, enlace, entidad_id "
                   + "FROM notificaciones WHERE usuario_id = ? AND leida = false ORDER BY fecha_creacion DESC";
                   
        // Inicializa la lista dinámica que contendrá las notificaciones sin leer.
        List<NotificacionDTO> notificaciones = new ArrayList<>();
        
        // try-with-resources: Gestiona de forma automática la apertura y cierre de recursos JDBC.
        try (Connection con = Conexion.obtener(); // Abre la conexión con MySQL.
             PreparedStatement ps = con.prepareStatement(sql)) { // Prepara la consulta parametrizada.
             
            // Vincula el ID del usuario en el filtro WHERE.
            ps.setInt(1, usuarioId);
            
            // Ejecuta la consulta de lectura de base de datos.
            try (ResultSet rs = ps.executeQuery()) {
                // Itera sobre cada registro no leído
                while (rs.next()) {
                    // Mapea la información a un nuevo DTO
                    NotificacionDTO notif = new NotificacionDTO();
                    notif.setId(rs.getInt("id"));
                    notif.setUsuarioId(rs.getInt("usuario_id"));
                    notif.setTitulo(rs.getString("titulo"));
                    notif.setMensaje(rs.getString("mensaje"));
                    notif.setTipo(rs.getString("tipo"));
                    notif.setLeida(rs.getBoolean("leida"));
                    notif.setFechaCreacion(rs.getString("fecha_creacion"));
                    notif.setEnlace(rs.getString("enlace"));
                    notif.setEntidadId(rs.getInt("entidad_id")); // Mapea el ID de entidad relacionada.
                    
                    // Agrega el DTO al listado final.
                    notificaciones.add(notif);
                }
            }
        }
        // Retorna la lista con todas las notificaciones pendientes de lectura.
        return notificaciones;
    }

    // Sirve para: Contar las notificaciones no leídas de un usuario.
    // Qué hace: Realiza una consulta SELECT COUNT(*) en la tabla notificaciones.
    // Explicación de consulta SQL:
    // - SELECT COUNT(*) cuenta la cantidad total de filas.
    // - WHERE usuario_id = ? AND leida = false restringe el conteo a las notificaciones sin leer de ese usuario.
    public int contarNoLeidas(int usuarioId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notificaciones WHERE usuario_id = ? AND leida = false";
        
        // try-with-resources: Asegura que la conexión y el statement se cierren al terminar la ejecución.
        try (Connection con = Conexion.obtener(); // Obtiene la conexión activa a base de datos.
             PreparedStatement ps = con.prepareStatement(sql)) { // Prepara la consulta de agregación.
             
            // Vincula el ID de usuario al primer marcador de posición '?'.
            ps.setInt(1, usuarioId);
            
            // Ejecuta la consulta de conteo y lee la fila única resultante.
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Recupera el valor entero de la primera columna (el conteo total) y lo retorna.
                    return rs.getInt(1);
                }
            }
        }
        // Retorna 0 si la consulta no arrojó resultados.
        return 0;
    }

    // Sirve para: Marca una notificación específica como leída.
    // Qué hace: Realiza un UPDATE en la tabla notificaciones cambiando el estado de lectura a verdadero.
    // Explicación de consulta SQL:
    // - UPDATE modifica la columna leida asignándole el valor lógico true.
    // - WHERE id = ? restringe el cambio a la notificación con la clave primaria correspondiente.
    public void marcarComoLeida(int notificacionId) throws SQLException {
        String sql = "UPDATE notificaciones SET leida = true WHERE id = ?";
        
        // try-with-resources: Administra de manera segura los recursos JDBC abiertos.
        try (Connection con = Conexion.obtener(); // Solicita la conexión.
             PreparedStatement ps = con.prepareStatement(sql)) { // Prepara la sentencia UPDATE.
             
            // Vincula el ID de la notificación a modificar al primer marcador '?'.
            ps.setInt(1, notificacionId);
            
            // Ejecuta la modificación física en el motor MySQL.
            ps.executeUpdate();
        }
    }

    // Sirve para: Marca todas las notificaciones de un usuario como leídas de una sola vez.
    // Qué hace: Realiza un UPDATE en la tabla notificaciones cambiando el estado de lectura.
    // Explicación de consulta SQL:
    // - UPDATE cambia el campo leida a true para todos los registros coincidentes.
    // - WHERE usuario_id = ? aplica la modificación a todas las alertas asignadas a ese usuario.
    public void marcarTodasComoLeidas(int usuarioId) throws SQLException {
        String sql = "UPDATE notificaciones SET leida = true WHERE usuario_id = ?";
        
        // try-with-resources: Inicializa y administra de forma limpia el statement y la conexión.
        try (Connection con = Conexion.obtener(); // Abre la conexión activa.
             PreparedStatement ps = con.prepareStatement(sql)) { // Prepara la sentencia preparada de actualización.
             
            // Vincula el ID del usuario en sesión.
            ps.setInt(1, usuarioId);
            
            // Ejecuta la modificación masiva física en MySQL.
            ps.executeUpdate();
        }
    }

    // Sirve para: Eliminar una notificación específica de la base de datos.
    // Qué hace: Ejecuta una sentencia DELETE física sobre el registro de la notificación.
    // Explicación de consulta SQL:
    // - DELETE FROM elimina físicamente registros de la tabla.
    // - WHERE id = ? condiciona el borrado exclusivamente al identificador de la notificación.
    public void eliminar(int notificacionId) throws SQLException {
        String sql = "DELETE FROM notificaciones WHERE id = ?";
        
        // try-with-resources: Garantiza la liberación ordenada de conexiones y statements.
        try (Connection con = Conexion.obtener(); // Solicita la conexión.
             PreparedStatement ps = con.prepareStatement(sql)) { // Prepara el statement de borrado.
             
            // Vincula el ID de la notificación al filtro WHERE.
            ps.setInt(1, notificacionId);
            
            // Ejecuta el borrado físico en base de datos.
            ps.executeUpdate();
        }
    }
}
