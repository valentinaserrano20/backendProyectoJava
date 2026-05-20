package Modelo.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

//creamos la clase conexion para centralizar los parametros de nuestra base de datos en un solo lugar
public class Conexion {
   //declaramos las variables globales, las cuales llevan la palabra reservada final lo que significa que no cambiaran.
    private static final String URL      = "jdbc:mysql://localhost:3306/planEmergenciaDC";
    private static final String USUARIO  = "root";
    private static final String PASSWORD = "Sol2004.";

    public static Connection obtener() throws SQLException {
        try {
            //carga en memoria y registra el controlador (driver) de MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");
            //si hay conexion usa como parametro la url, el usuario y contraseña para que sean llevados a mysql
            return DriverManager.getConnection(URL, USUARIO, PASSWORD);
        } catch (ClassNotFoundException e) {
            //en caso de que no lo encuentre ejecutara el error inidcandonos que el driver no fue encontrado
            throw new SQLException("Driver MySQL no encontrado", e);
        }
    }
}