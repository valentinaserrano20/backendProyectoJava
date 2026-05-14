package Modelo.DAO;

import Modelo.Config.Conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.json.JSONArray;
import org.json.JSONObject;

public class CatalogoDAO {

    public JSONArray getTiposDocumento() throws SQLException {
        String sql = "SELECT id, sigla, descripcion AS nombre, activo FROM tipo_documentos";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            JSONArray array = new JSONArray();
            while (rs.next()) {
                JSONObject obj = new JSONObject();
                obj.put("id", rs.getInt("id"));
                obj.put("sigla", rs.getString("sigla"));
                obj.put("nombre", rs.getString("nombre"));
                obj.put("activo", rs.getInt("activo"));
                array.put(obj);
            }
            return array;
        }
    }

    public JSONArray getGeneros() throws SQLException {
        String sql = "SELECT id, nombre, activo FROM generos";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            JSONArray array = new JSONArray();
            while (rs.next()) {
                JSONObject obj = new JSONObject();
                obj.put("id", rs.getInt("id"));
                obj.put("nombre", rs.getString("nombre"));
                obj.put("activo", rs.getInt("activo"));
                array.put(obj);
            }
            return array;
        }
    }

    public JSONArray getOrganizaciones() throws SQLException {
        String sql = "SELECT id, nombre, activo FROM organizaciones";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            JSONArray array = new JSONArray();
            while (rs.next()) {
                JSONObject obj = new JSONObject();
                obj.put("id", rs.getInt("id"));
                obj.put("nombre", rs.getString("nombre"));
                obj.put("activo", rs.getInt("activo"));
                array.put(obj);
            }
            return array;
        }
    }
}