package Modelo.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexion {

    // URL directa a  MySQL local
    private static final String URL = "jdbc:mysql://localhost:3306/planEmergenciaDC?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root"; //usuario de MySQL
    private static final String PASS = "#Aprendiz2024"; 

    static {
        try {
            // Forzamos la carga del Driver de MySQL en memoria
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("ERROR: No se encontró el Driver de MySQL en el proyecto.");
            e.printStackTrace();
        }
    }

    public static Connection obtener() throws SQLException {
        // Conexión directa y limpia sin depender del JNDI de Tomcat
        return DriverManager.getConnection(URL, USER, PASS);
    }
}