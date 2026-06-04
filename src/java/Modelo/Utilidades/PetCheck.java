package Modelo.Utilidades;

import Modelo.Config.Conexion;
import java.sql.*;

public class PetCheck {
    public static void main(String[] args) {
        try (Connection con = Conexion.obtener()) {
            System.out.println("=== MASCOTAS TABLE COLUMNS ===");
            DatabaseMetaData meta = con.getMetaData();
            try (ResultSet rs = meta.getColumns("planEmergenciaDC", null, "mascotas", "%")) {
                while (rs.next()) {
                    System.out.println("Column: " + rs.getString("COLUMN_NAME") + " | Type: " + rs.getString("TYPE_NAME"));
                }
            }

            System.out.println("\n=== MASCOTAS RECORDS ===");
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM mascotas LIMIT 10")) {
                ResultSetMetaData rsmd = rs.getMetaData();
                int cols = rsmd.getColumnCount();
                while (rs.next()) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i <= cols; i++) {
                        sb.append(rsmd.getColumnName(i)).append(": ").append(rs.getObject(i)).append(" | ");
                    }
                    System.out.println(sb.toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
