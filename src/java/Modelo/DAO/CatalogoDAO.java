package Modelo.DAO;

// Importa la clase de conexión a la base de datos MySQL
import Modelo.Config.Conexion;
// Importa los DTOs (Data Transfer Objects) para mapear los resultados de las consultas
import Modelo.DTO.GeneroDTO;
import Modelo.DTO.OrganizacionDTO;
import Modelo.DTO.TipoDocumentoDTO;
// Importa las clases de JDBC para manejar conexiones y consultas a MySQL
import java.sql.Connection;          // Representa la conexión física con la base de datos
import java.sql.PreparedStatement;  // Permite ejecutar consultas SQL parametrizadas de forma segura
import java.sql.ResultSet;           // Contiene los resultados devueltos por una consulta SQL
import java.sql.SQLException;        // Excepción que se lanza cuando ocurre un error en la base de datos
import java.util.ArrayList;          // Implementación de lista dinámica para almacenar los resultados
import java.util.List;               // Interfaz genérica para trabajar con colecciones de datos

//Retorna colecciones de DTOs en lugar de JSON org.json.JSONArray para cumplir con la arquitectura MVC limpia
public class CatalogoDAO {

    // Método que obtiene todos los tipos de documento de la base de datos
    public List<TipoDocumentoDTO> getTiposDocumento() throws SQLException {
        // Se define la consulta SQL que selecciona las columnas id, sigla, descripcion (renombrada como nombre) y activo de la tabla tipo_documentos
        String sql = "SELECT id, sigla, descripcion AS nombre, activo FROM tipo_documentos";
        // Se usa try-with-resources para asegurar que los recursos JDBC se cierren automáticamente al terminar
        try (Connection con = Conexion.obtener();                    // Se obtiene una conexión a MySQL desde el pool de conexiones
             PreparedStatement ps = con.prepareStatement(sql);       // Se prepara la consulta SQL para ejecución segura contra inyección SQL
             ResultSet rs = ps.executeQuery()) {                      // Se ejecuta la consulta y se obtienen los resultados en un ResultSet
            // Se crea una lista dinámica para almacenar todos los tipos de documento encontrados
            List<TipoDocumentoDTO> lista = new ArrayList<>();
            // Se recorre cada fila del ResultSet mientras haya registros disponibles
            while (rs.next()) {
                // AGREGADO: Mapeo de fila ResultSet a TipoDocumentoDTO
                // Se crea una nueva instancia del DTO para almacenar los datos de la fila actual
                TipoDocumentoDTO dto = new TipoDocumentoDTO();
                // Se obtiene el valor de la columna id de tipo entero y se asigna al DTO
                dto.setId(rs.getInt("id"));
                // Se obtiene el valor de la columna sigla de tipo texto y se asigna al DTO
                dto.setSigla(rs.getString("sigla"));
                // Se obtiene el valor de la columna nombre (alias de descripcion) de tipo texto y se asigna al DTO
                dto.setNombre(rs.getString("nombre"));
                // Se obtiene el valor de la columna activo de tipo entero (1=activo, 0=inactivo) y se asigna al DTO
                dto.setActivo(rs.getInt("activo"));
                // Se agrega el DTO mapeado a la lista de resultados
                lista.add(dto);
            }
            // Se retorna la lista completa con todos los tipos de documento encontrados en la base de datos
            return lista;
        }
    }

    // Método que obtiene todos los géneros de la base de datos
    public List<GeneroDTO> getGeneros() throws SQLException {
        // Se define la consulta SQL que selecciona las columnas id, nombre y activo de la tabla generos
        String sql = "SELECT id, nombre, activo FROM generos";
        // Se usa try-with-resources para asegurar que los recursos JDBC se cierren automáticamente al terminar
        try (Connection con = Conexion.obtener();                    // Se obtiene una conexión a MySQL desde el pool de conexiones
             PreparedStatement ps = con.prepareStatement(sql);       // Se prepara la consulta SQL para ejecución segura contra inyección SQL
             ResultSet rs = ps.executeQuery()) {                      // Se ejecuta la consulta y se obtienen los resultados en un ResultSet
            // Se crea una lista dinámica para almacenar todos los géneros encontrados
            List<GeneroDTO> lista = new ArrayList<>();
            // Se recorre cada fila del ResultSet mientras haya registros disponibles
            while (rs.next()) {
                // AGREGADO: Mapeo de fila ResultSet a GeneroDTO
                // Se crea una nueva instancia del DTO para almacenar los datos de la fila actual
                GeneroDTO dto = new GeneroDTO();
                // Se obtiene el valor de la columna id de tipo entero y se asigna al DTO
                dto.setId(rs.getInt("id"));
                // Se obtiene el valor de la columna nombre de tipo texto y se asigna al DTO
                dto.setNombre(rs.getString("nombre"));
                // Se obtiene el valor de la columna activo de tipo entero (1=activo, 0=inactivo) y se asigna al DTO
                dto.setActivo(rs.getInt("activo"));
                // Se agrega el DTO mapeado a la lista de resultados
                lista.add(dto);
            }
            // Se retorna la lista completa con todos los géneros encontrados en la base de datos
            return lista;
        }
    }

    // Método que obtiene todas las organizaciones de la base de datos
    public List<OrganizacionDTO> getOrganizaciones() throws SQLException {
        // Se define la consulta SQL que selecciona las columnas id, nombre y activo de la tabla organizaciones
        String sql = "SELECT id, nombre, activo FROM organizaciones";
        // Se usa try-with-resources para asegurar que los recursos JDBC se cierren automáticamente al terminar
        try (Connection con = Conexion.obtener();                    // Se obtiene una conexión a MySQL desde el pool de conexiones
             PreparedStatement ps = con.prepareStatement(sql);       // Se prepara la consulta SQL para ejecución segura contra inyección SQL
             ResultSet rs = ps.executeQuery()) {                      // Se ejecuta la consulta y se obtienen los resultados en un ResultSet
            // Se crea una lista dinámica para almacenar todas las organizaciones encontradas
            List<OrganizacionDTO> lista = new ArrayList<>();
            // Se recorre cada fila del ResultSet mientras haya registros disponibles
            while (rs.next()) {
                // AGREGADO: Mapeo de fila ResultSet a OrganizacionDTO
                // Se crea una nueva instancia del DTO para almacenar los datos de la fila actual
                OrganizacionDTO dto = new OrganizacionDTO();
                // Se obtiene el valor de la columna id de tipo entero y se asigna al DTO
                dto.setId(rs.getInt("id"));
                // Se obtiene el valor de la columna nombre de tipo texto y se asigna al DTO
                dto.setNombre(rs.getString("nombre"));
                // Se obtiene el valor de la columna activo de tipo entero (1=activo, 0=inactivo) y se asigna al DTO
                dto.setActivo(rs.getInt("activo"));
                // Se agrega el DTO mapeado a la lista de resultados
                lista.add(dto);
            }
            // Se retorna la lista completa con todas las organizaciones encontradas en la base de datos
            return lista;
        }
    }
}