package Modelo.DAO;

import Modelo.Config.Conexion;
import Modelo.DTO.GeneroDTO;
import Modelo.DTO.OrganizacionDTO;
import Modelo.DTO.TipoDocumentoDTO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// MODIFICADO: Retorna colecciones de DTOs en lugar de JSON org.json.JSONArray para cumplir con la arquitectura MVC limpia
public class CatalogoDAO {

    public List<TipoDocumentoDTO> getTiposDocumento() throws SQLException {
        String sql = "SELECT id, sigla, descripcion AS nombre, activo FROM tipo_documentos";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<TipoDocumentoDTO> lista = new ArrayList<>();
            while (rs.next()) {
                // AGREGADO: Mapeo de fila ResultSet a TipoDocumentoDTO
                TipoDocumentoDTO dto = new TipoDocumentoDTO();
                dto.setId(rs.getInt("id"));
                dto.setSigla(rs.getString("sigla"));
                dto.setNombre(rs.getString("nombre"));
                dto.setActivo(rs.getInt("activo"));
                lista.add(dto);
            }
            return lista;
        }
    }

    public List<GeneroDTO> getGeneros() throws SQLException {
        String sql = "SELECT id, nombre, activo FROM generos";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<GeneroDTO> lista = new ArrayList<>();
            while (rs.next()) {
                // AGREGADO: Mapeo de fila ResultSet a GeneroDTO
                GeneroDTO dto = new GeneroDTO();
                dto.setId(rs.getInt("id"));
                dto.setNombre(rs.getString("nombre"));
                dto.setActivo(rs.getInt("activo"));
                lista.add(dto);
            }
            return lista;
        }
    }

    public List<OrganizacionDTO> getOrganizaciones() throws SQLException {
        String sql = "SELECT id, nombre, activo FROM organizaciones";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<OrganizacionDTO> lista = new ArrayList<>();
            while (rs.next()) {
                // AGREGADO: Mapeo de fila ResultSet a OrganizacionDTO
                OrganizacionDTO dto = new OrganizacionDTO();
                dto.setId(rs.getInt("id"));
                dto.setNombre(rs.getString("nombre"));
                dto.setActivo(rs.getInt("activo"));
                lista.add(dto);
            }
            return lista;
        }
    }
}