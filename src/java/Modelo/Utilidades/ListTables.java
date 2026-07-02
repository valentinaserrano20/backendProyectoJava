package Modelo.Utilidades;

import Modelo.Config.Conexion;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

/**
 * Qué hace: Clase utilitaria ejecutable que inspecciona y lista la estructura interna de la base de datos de manera dinámica.
 * Por qué existe: Provee un mecanismo automático para verificar que todas las tablas y columnas requeridas por el modelo relacional estén creadas y configuradas con sus tipos de datos correctos.
 * Qué pasaría si no estuviera: El desarrollador tendría que consultar de forma manual la estructura de tablas a través de comandos SQL crudos (como "SHOW TABLES" o "DESCRIBE") en un gestor MySQL.
 */
public class ListTables {
    
    /**
     * Qué hace: Recupera los metadatos de la conexión JDBC para listar las tablas de tipo "TABLE" y detalla el nombre y tipo de sus columnas.
     * Qué significa: Llama a con.getMetaData() para consultar el diccionario de datos del motor de base de datos sin ejecutar comandos SQL directos.
     * Para qué se usa: Diagnóstico rápido de la estructura DDL física de la base de datos durante el despliegue o actualizaciones del esquema.
     * Por qué es importante: Permite verificar la existencia de columnas nuevas e identificar rápidamente errores de tipeo en los nombres de las columnas que causarían excepciones SQLException en tiempo de ejecución.
     * 
     * @param args Argumentos de la línea de comandos (no utilizados).
     */
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
