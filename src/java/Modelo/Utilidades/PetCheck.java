package Modelo.Utilidades;

import Modelo.Config.Conexion;
import java.sql.*;

/**
 * Qué hace: Clase utilitaria ejecutable que inspecciona la tabla de mascotas en la base de datos.
 * Por qué existe: Sirve de herramienta de diagnóstico para verificar que la estructura física y las columnas de la tabla de mascotas coincidan exactamente con el modelo relacional mapeado.
 * Qué pasaría si no estuviera: Se tendría que verificar el esquema de mascotas y el mapeo de registros utilizando clientes SQL externos.
 */
public class PetCheck {
    
    /**
     * Qué hace: Muestra las columnas físicas de la tabla de mascotas y lista los primeros 10 registros detallando cada valor de sus campos.
     * Qué significa: Obtiene los metadatos de las columnas con DatabaseMetaData.getColumns() y lee dinámicamente los nombres de columnas a través del ResultSetMetaData.
     * Para qué se usa: Diagnóstico en caliente y depuración local en la etapa de desarrollo de la funcionalidad de mascotas.
     * Por qué es importante: Permite detectar discrepancias de tipos de datos o nombres de columnas modificados que de otro modo romperían el CRUD del DAO.
     * 
     * @param args Argumentos de la línea de comandos (no utilizados).
     */
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
