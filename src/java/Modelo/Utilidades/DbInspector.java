package Modelo.Utilidades;

import Modelo.Config.Conexion;
import java.sql.*;

/**
 * Qué hace: Clase de inspección rápida que consulta e imprime en consola los registros paramétricos y de auditoría de la base de datos.
 * Por qué existe: Sirve de herramienta de diagnóstico para verificar visualmente que las tablas de roles, permisos, usuarios y estados de planes tengan la semilla de datos correcta.
 * Qué pasaría si no estuviera: El desarrollador tendría que conectarse manualmente con una herramienta de cliente SQL externa (como MySQL Workbench o DBeaver) para verificar la persistencia de las tablas maestras.
 */
public class DbInspector {
    
    /**
     * Qué hace: Ejecuta consultas SELECT directas sobre roles, estados de planes, permisos, roles_permisos y usuarios, imprimiendo sus contenidos en la salida estándar de sistema.
     * Qué significa: Abre un canal JDBC a través de Conexion.obtener(), compila sentencias SQL simples y itera sobre cada ResultSet.
     * Para qué se usa: Diagnóstico en caliente y depuración local en la etapa de desarrollo y pruebas.
     * Por qué es importante: Permite verificar la correcta asignación de roles y llaves foráneas de forma inmediata en consola.
     * 
     * @param args Argumentos de la línea de comandos (no utilizados).
     */
    public static void main(String[] args) {
        // Qué hace: Intenta abrir una sesión JDBC activa con la base de datos en un bloque try-with-resources.
        // Por qué existe: Asegura que el socket de conexión se cierre de forma automática al culminar la impresión, evitando bloqueos de sockets.
        try (Connection con = Conexion.obtener()) {
            System.out.println("=== TABLA ROLES ===");
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM roles")) {
                while (rs.next()) {
                    System.out.println("ID: " + rs.getInt("id") + " | Nombre: " + rs.getString("nombre"));
                }
            }

            System.out.println("\n=== TABLA ESTADOS PLAN ===");
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM estados_plan")) {
                while (rs.next()) {
                    System.out.println("ID: " + rs.getInt("id") + " | Nombre: " + rs.getString("nombre"));
                }
            }

            System.out.println("\n=== TABLA PERMISOS ===");
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM permisos")) {
                while (rs.next()) {
                    System.out.println("ID: " + rs.getInt("id") + " | Key: " + rs.getString("key_name") + " | Desc: " + rs.getString("description"));
                }
            }

            System.out.println("\n=== TABLA ROLES_PERMISOS ===");
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM roles_permisos")) {
                while (rs.next()) {
                    System.out.println("Role ID: " + rs.getInt("role_id") + " | Permission ID: " + rs.getInt("permission_id"));
                }
            }

            System.out.println("\n=== TABLA USUARIOS (RESUMEN) ===");
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT id, nombre, email, rol_id, estado_id FROM usuarios")) {
                while (rs.next()) {
                    System.out.println("ID: " + rs.getInt("id") + " | Nombre: " + rs.getString("nombre") + " | Email: " + rs.getString("email") + " | Rol ID: " + rs.getInt("rol_id") + " | Estado ID: " + rs.getInt("estado_id"));
                }
            }

            System.out.println("\n=== TABLA PLANES_FAMILIARES (ÚLTIMOS 5) ===");
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT id, voluntario_id, estado_id, tipo_familia_id FROM planes_familiares ORDER BY id DESC LIMIT 5")) {
                while (rs.next()) {
                    System.out.println("ID: " + rs.getInt("id") + " | Voluntario ID: " + rs.getInt("voluntario_id") + " | Estado ID: " + rs.getInt("estado_id") + " | Tipo Familia ID: " + rs.getInt("tipo_familia_id"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
