package Modelo.DAO;

import Modelo.Config.Conexion;
import Modelo.Entidades.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UsuarioDAO {

    public Usuario login(String email, String contrasena) throws SQLException {
        String sql = "SELECT id, nombre, apellido, email, estado, rol_id, organizacion_id "
                + "FROM usuarios WHERE email = ? AND contraseña = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, contrasena);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Usuario u = new Usuario();
                    u.setId(rs.getInt("id"));
                    u.setNombre(rs.getString("nombre"));
                    u.setApellido(rs.getString("apellido"));
                    u.setEmail(rs.getString("email"));
                    u.setEstado(rs.getString("estado"));
                    u.setRolId(rs.getInt("rol_id"));
                    u.setOrganizacionId(rs.getObject("organizacion_id") != null
                            ? rs.getInt("organizacion_id")
                            : null);
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

    public void registrar(String nombres, String apellidos, String email, String password,
            String numDocumento, String fechaNac, String telefono,
            int tipoDocumentoId, int generoId, int organizacionId) throws SQLException {

        String sql = "INSERT INTO usuarios (nombre, apellido, email, contraseña, numero_documento, "
                + "fecha_nacimiento, celular, tipo_documento_id, genero, organizacion_id, rol_id, estado) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 2, 'pendiente')";

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombres);
            ps.setString(2, apellidos);
            ps.setString(3, email);
            ps.setString(4, password);
            ps.setString(5, numDocumento);
            ps.setString(6, fechaNac);
            ps.setString(7, telefono);
            ps.setInt(8, tipoDocumentoId);
            ps.setString(9, String.valueOf(generoId));
            ps.setInt(10, organizacionId);
            ps.executeUpdate();
        }
    }

}