package Modelo.Utilidades;

import Modelo.Config.Conexion;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

public class ListTables {
    public static void main(String[] args) {
        try (Connection con = Conexion.obtener()) {
            DatabaseMetaData metaData = con.getMetaData();
            String[] types = {"TABLE"};
            try (ResultSet rs = metaData.getTables("planEmergenciaDC", null, "%", types)) {
                System.out.println("--- TABLAS EN LA BASE DE DATOS ACTIVA ---");
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    System.out.println("Tabla: " + tableName);
                    try (ResultSet cols = metaData.getColumns("planEmergenciaDC", null, tableName, "%")) {
                        System.out.print("  Columnas: ");
                        while (cols.next()) {
                            System.out.print(cols.getString("COLUMN_NAME") + " (" + cols.getString("TYPE_NAME") + "), ");
                        }
                        System.out.println("\n");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
