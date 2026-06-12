package Modelo.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// Qué hace: Clase encargada de la configuración y suministro de conexiones JDBC hacia la base de datos MySQL.
// Por qué existe: Centraliza las credenciales y parámetros de conexión para que todos los DAOs los consuman uniformemente.
// Qué problema resuelve: Elimina la necesidad de registrar credenciales en cada clase DAO, resolviendo incompatibilidades con Tomcat y JNDI mediante una conexión JDBC pura y directa.
public class Conexion {

    // Qué hace: Cadena de conexión JDBC para conectarse al servidor MySQL local.
    // Por qué existe: Especifica el protocolo (jdbc:mysql), host (localhost), puerto (3306), nombre de la base de datos (planEmergenciaDC) y configuraciones adicionales de seguridad y huso horario.
    // Qué problema resuelve: Habilita la conexión segura con allowPublicKeyRetrieval=true y desactiva el uso obligatorio de certificados SSL.
    private static final String URL = "jdbc:mysql://localhost:3306/planEmergenciaDC?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    
    // Qué hace: Identifica el usuario con privilegios de conexión en la base de datos.
    // Por qué existe: MySQL requiere autenticación para iniciar sesión.
    // Qué problema resuelve: Autentica al servidor de Java frente a la instancia de base de datos MySQL local.
    private static final String USER = "root"; 
    
    // Qué hace: Define la contraseña del usuario root en la base de datos.
    // Por qué existe: Es la credencial de seguridad del gestor de base de datos.
    // Qué problema resuelve: Completa el handshake de autenticación de MySQL.
    private static final String PASS = "#Aprendiz2024"; 

    // Bloque estático que se ejecuta la primera vez que se carga la clase Conexion en la JVM.
    static {
        try {
            // Qué hace: Carga de manera dinámica el Driver JDBC de MySQL en la memoria de la JVM.
            // Por qué existe: Registrar el driver es un prerrequisito obligatorio de JDBC para que DriverManager reconozca la URL jdbc:mysql.
            // Qué problema resuelve: Asegura que el driver esté presente y registrado en tiempo de ejecución para evitar fallos de conexión.
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            // Qué hace: Captura el error en caso de que el driver de base de datos de MySQL no esté presente en las librerías del proyecto.
            // Por qué existe: Advierte al desarrollador si el conector .jar hace falta en el Build Path.
            // Qué problema resuelve: Facilita la depuración de dependencias imprimiendo la traza del error en consola.
            System.err.println("ERROR: No se encontró el Driver de MySQL en el proyecto.");
            e.printStackTrace();
        }
    }

    // Qué hace: Abre y retorna un nuevo objeto java.sql.Connection hacia MySQL.
    // Por qué existe: Provee el canal activo para enviar consultas y comandos SQL desde los DAOs.
    // Qué problema resuelve: Establece la sesión activa utilizando DriverManager sin depender del pool JNDI de Tomcat.
    public static Connection obtener() throws SQLException {
        // Qué hace: Llama al DriverManager para iniciar y autenticar la conexión física.
        // Por qué existe: Retorna el socket de conexión listo para preparar sentencias.
        // Qué problema resuelve: Conecta la aplicación de Java con el servidor de base de datos MySQL local.
        return DriverManager.getConnection(URL, USER, PASS);
    }
}