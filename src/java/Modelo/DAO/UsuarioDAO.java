package Modelo.DAO;

import Modelo.Config.Conexion;
import Modelo.Entidades.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UsuarioDAO {

    // CORREGIDO: Retorna el usuario por email incluyendo el hash de contraseña,
    // y realiza un JOIN con la tabla de estados para obtener su nombre legible.
    public Usuario obtenerPorEmail(String email) throws SQLException {
        String sql = "SELECT u.id, u.nombre, u.apellido, u.email, u.contraseña, "
                + "u.rol_id, u.organizacion_id, u.estado_id, e.nombre AS estado_nombre "
                + "FROM usuarios u "
                + "INNER JOIN estado_usuarios e ON u.estado_id = e.id "
                + "WHERE u.email = ?";
                
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Usuario u = new Usuario();
                    u.setId(rs.getInt("id"));
                    u.setNombre(rs.getString("nombre"));
                    u.setApellido(rs.getString("apellido"));
                    u.setEmail(rs.getString("email"));
                    u.setContrasena(rs.getString("contraseña"));
                    u.setRolId(rs.getInt("rol_id"));
                    u.setOrganizacionId(rs.getObject("organizacion_id") != null
                            ? rs.getInt("organizacion_id")
                            : null);
                    u.setEstadoId(rs.getInt("estado_id"));
                    u.setEstado(rs.getString("estado_nombre"));
                    return u;
                }
                return null;
            }
        }
    }

    public boolean existeEmail(String email) throws SQLException {
        String sql = "SELECT id FROM usuarios WHERE email = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean existeDocumento(String numDocumento) throws SQLException {
        String sql = "SELECT id FROM usuarios WHERE numero_documento = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, numDocumento);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // CORREGIDO: Mapea correctamente las columnas estado_id, genero_id, rol_id
    // asignando tipos consistentes y los IDs relacionales correctos.
    public void registrar(String nombres, String apellidos, String email, String passwordHashed,
            String numDocumento, String fechaNac, String telefono,
            int tipoDocumentoId, int generoId, int organizacionId) throws SQLException {

        // rol_id = 1 (Voluntario en BD), estado_id = 3 (Pendiente en BD)
        String sql = "INSERT INTO usuarios (nombre, apellido, email, contraseña, numero_documento, "
                + "fecha_nacimiento, celular, tipo_documento_id, genero_id, organizacion_id, rol_id, estado_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 3)";

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombres);
            ps.setString(2, apellidos);
            ps.setString(3, email);
            ps.setString(4, passwordHashed); // Guarda contraseña ya hasheada por BCrypt
            ps.setString(5, numDocumento);
            ps.setString(6, fechaNac);
            ps.setString(7, telefono);
            ps.setInt(8, tipoDocumentoId);
            ps.setInt(9, generoId); // Mapeado como INT genero_id
            ps.setInt(10, organizacionId);
            ps.executeUpdate();
        }
    }
}
