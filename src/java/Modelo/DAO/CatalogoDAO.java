package Modelo.DAO;

// Importa la clase de conexión a la base de datos MySQL
import Modelo.Config.Conexion;
// Importa los DTOs (Data Transfer Objects) para mapear los resultados de las consultas
import Modelo.DTO.GeneroDTO;
import Modelo.DTO.OrganizacionDTO;
import Modelo.DTO.TipoDocumentoDTO;
import Modelo.DTO.UbicacionDTO; // CORREGIDO: Importación requerida para mapear las zonas
import Modelo.DTO.TipoRecursoDTO;

// Importa las clases de JDBC para manejar conexiones y consultas a MySQL
import java.sql.Connection;          // Representa la conexión física con la base de datos
import java.sql.PreparedStatement;  // Permite ejecutar consultas SQL parametrizadas de forma segura
import java.sql.ResultSet;           // Contiene los resultados devueltos por una consulta SQL
import java.sql.SQLException;        // Excepción que se lanza cuando ocurre un error en la base de datos
import java.util.ArrayList;          // Implementación de lista dinámica para almacenar los resultados
import java.util.List;               // Interfaz genérica para trabajar con colecciones de datos

// Retorna colecciones de DTOs en lugar de JSON org.json.JSONArray para cumplir con la arquitectura MVC limpia
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

    // Método que obtiene los tipos de zona residencial (Urbana / Rural)
    public List<UbicacionDTO> getTiposZona() throws SQLException {
        // Se define la consulta SQL para obtener las zonas residenciales activas (activo = 1) de la tabla tipos_zona.
        String sql = "SELECT id, nombre FROM tipos_zona WHERE activo = 1";
        
        // try-with-resources: Abre la conexión a la base de datos y ejecuta el statement.
        try (Connection con = Conexion.obtener(); // Se obtiene una conexión activa de MySQL.
             PreparedStatement ps = con.prepareStatement(sql); // Se prepara la consulta.
             ResultSet rs = ps.executeQuery()) { // Ejecuta la consulta de selección.
             
            // Se inicializa la lista dinámica para almacenar las zonas mapeadas.
            List<UbicacionDTO> lista = new ArrayList<>();
            // Itera sobre las filas devueltas por la consulta.
            while (rs.next()) {
                // Mapea la fila creando un nuevo objeto UbicacionDTO con el ID y el nombre de la zona.
                lista.add(new UbicacionDTO(rs.getInt("id"), rs.getString("nombre")));
            }
            // Retorna la lista con los tipos de zona activa.
            return lista;
        }
    }

    // Método que obtiene todos los sectores geográficos activos de la base de datos
    public List<UbicacionDTO> getSectores() throws SQLException {
        // Sentencia SQL para seleccionar el ID y nombre de los sectores que se encuentran activos (activo = 1)
        String sql = "SELECT id, nombre FROM sectores WHERE activo = 1";
        
        // try-with-resources: Abre la conexión y prepara la consulta parametrizada de JDBC
        try (Connection con = Conexion.obtener(); // Se conecta a MySQL.
             PreparedStatement ps = con.prepareStatement(sql); // Prepara el statement.
             ResultSet rs = ps.executeQuery()) { // Ejecuta la lectura.
             
            // Inicializa la lista dinámica para almacenar los sectores mapeados a UbicacionDTO
            List<UbicacionDTO> lista = new ArrayList<>();
            // Recorre cada registro devuelto por la base de datos
            while (rs.next()) {
                // Instancia el DTO genérico UbicacionDTO con el ID y el nombre del sector y lo agrega a la lista
                lista.add(new UbicacionDTO(rs.getInt("id"), rs.getString("nombre")));
            }
            // Retorna el listado completo de sectores
            return lista;
        }
    }

    // Método que obtiene todos los regímenes o calidades de vivienda activos de la base de datos
    public List<UbicacionDTO> getCalidadesVivienda() throws SQLException {
        // Sentencia SQL para seleccionar el ID y nombre de las calidades de vivienda que se encuentran activas (activo = 1)
        String sql = "SELECT id, nombre FROM calidad_vivienda WHERE activo = 1";
        
        // try-with-resources: Abre la conexión y prepara la consulta de forma segura
        try (Connection con = Conexion.obtener(); // Obtiene la conexión activa de MySQL.
             PreparedStatement ps = con.prepareStatement(sql); // Prepara la consulta.
             ResultSet rs = ps.executeQuery()) { // Ejecuta la consulta.
             
            // Inicializa la lista para almacenar las calidades de vivienda
            List<UbicacionDTO> lista = new ArrayList<>();
            // Recorre cada fila devuelta por la consulta
            while (rs.next()) {
                // Mapea la fila al DTO UbicacionDTO y lo añade al listado de retorno
                lista.add(new UbicacionDTO(rs.getInt("id"), rs.getString("nombre")));
            }
            // Retorna la lista completa
            return lista;
        }
    }

    // Qué hace: Obtiene todos los parentescos activos de la base de datos para mapearlos a objetos UbicacionDTO.
    // Por qué existe: Suministra las opciones de parentesco para la creación de integrantes en el formulario familiar.
    // Qué problema resuelve: Permite poblar dinámicamente las relaciones de parentesco en la UI en lugar de dejarlas fijas en el código.
    public List<UbicacionDTO> getParentescos() throws SQLException {
        // SELECT recupera el ID y nombre de los parentescos activos.
        String sql = "SELECT id, nombre FROM parentescos WHERE activo = 1";
        
        // try-with-resources: Inicializa y administra de forma segura la conexión y el ResultSet.
        try (Connection con = Conexion.obtener(); // Se conecta a MySQL.
             PreparedStatement ps = con.prepareStatement(sql); // Prepara la consulta.
             ResultSet rs = ps.executeQuery()) { // Ejecuta la lectura.
             
            // Se inicializa la lista dinámica.
            List<UbicacionDTO> lista = new ArrayList<>();
            // Itera sobre los registros y los añade a la lista.
            while (rs.next()) {
                lista.add(new UbicacionDTO(rs.getInt("id"), rs.getString("nombre")));
            }
            // Retorna la lista con los parentescos.
            return lista;
        }
    }

    // Qué hace: Obtiene todos los grupos sanguíneos activos de la base de datos y los mapea a UbicacionDTO.
    // Por qué existe: Provee las opciones de tipos de sangre para la ficha demográfica y de salud del integrante.
    // Qué problema resuelve: Evita la captura libre de texto o listas harcodeadas propensas a errores ortográficos o inconsistencias médicas.
    public List<UbicacionDTO> getGruposSanguineos() throws SQLException {
        // SELECT recupera el ID y nombre de los grupos sanguíneos de la tabla grupos_sanguineos.
        String sql = "SELECT id, nombre FROM grupos_sanguineos WHERE activo = 1";
        
        // try-with-resources: Abre de forma segura la conexión JDBC y compila la consulta.
        try (Connection con = Conexion.obtener(); // Abre la conexión.
             PreparedStatement ps = con.prepareStatement(sql); // Prepara la sentencia.
             ResultSet rs = ps.executeQuery()) { // Ejecuta la consulta.
             
            // Se crea la lista.
            List<UbicacionDTO> lista = new ArrayList<>();
            // Recorre y mapea las columnas.
            while (rs.next()) {
                lista.add(new UbicacionDTO(rs.getInt("id"), rs.getString("nombre")));
            }
            // Retorna la lista de tipos de sangre.
            return lista;
        }
    }

    // Qué hace: Obtiene todas las nacionalidades activas de la base de datos y las mapea a UbicacionDTO.
    // Por qué existe: Suministra las opciones de país/nacionalidad de origen del familiar en los formularios del plan de emergencia.
    // Qué problema resuelve: Garantiza que los registros de nacionalidad coincidan estrictamente con las opciones homologadas en la base de datos.
    public List<UbicacionDTO> getNacionalidades() throws SQLException {
        // SELECT recupera nacionalidades activas.
        String sql = "SELECT id, nombre FROM nacionalidades WHERE activo = 1";
        
        // try-with-resources: Garantiza el cierre de recursos JDBC.
        try (Connection con = Conexion.obtener(); // Se conecta a MySQL.
             PreparedStatement ps = con.prepareStatement(sql); // Prepara la consulta.
             ResultSet rs = ps.executeQuery()) { // Ejecuta el query.
             
            // Inicializa la lista.
            List<UbicacionDTO> lista = new ArrayList<>();
            // Recorre los resultados.
            while (rs.next()) {
                lista.add(new UbicacionDTO(rs.getInt("id"), rs.getString("nombre")));
            }
            // Retorna la lista.
            return lista;
        }
    }

    // Qué hace: Obtiene todas las especies de mascotas activas de la base de datos y las mapea a UbicacionDTO.
    // Por qué existe: Suministra las opciones de especie animal para la creación y edición de mascotas de la familia.
    // Qué problema resuelve: Permite poblar dinámicamente las especies registradas en MySQL para evitar valores inconsistentes.
    public List<UbicacionDTO> getEspeciesMascota() throws SQLException {
        // SELECT consulta el catálogo de especies_mascota activos.
        String sql = "SELECT id, nombre FROM especies_mascota WHERE activo = 1";
        
        // try-with-resources: Administra la conexión física y el PreparedStatement de JDBC.
        try (Connection con = Conexion.obtener(); // Solicita la conexión.
             PreparedStatement ps = con.prepareStatement(sql); // Prepara la sentencia.
             ResultSet rs = ps.executeQuery()) { // Ejecuta la lectura.
             
            // Crea la lista y itera mapeando cada fila.
            List<UbicacionDTO> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(new UbicacionDTO(rs.getInt("id"), rs.getString("nombre")));
            }
            // Retorna la lista.
            return lista;
        }
    }

    // Qué hace: Obtiene los géneros de mascota activos de la base de datos.
    // Por qué existe: Suministra las opciones de género para la creación y edición de mascotas.
    // Qué problema resuelve: Permite poblar dinámicamente los géneros de mascota registrados.
    public List<UbicacionDTO> getGenerosMascota() throws SQLException {
        // SELECT consulta el catálogo de géneros_mascota activos.
        String sql = "SELECT id, nombre FROM generos_mascota WHERE activo = 1";
        
        // try-with-resources: Abre y cierra de forma segura la conexión y el ResultSet.
        try (Connection con = Conexion.obtener(); // Se conecta a MySQL.
             PreparedStatement ps = con.prepareStatement(sql); // Prepara la consulta.
             ResultSet rs = ps.executeQuery()) { // Ejecuta la consulta.
             
            // Inicializa la lista y mapea las columnas.
            List<UbicacionDTO> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(new UbicacionDTO(rs.getInt("id"), rs.getString("nombre")));
            }
            // Retorna la lista.
            return lista;
        }
    }

    // Qué hace: Obtiene todas las amenazas o tipos de riesgo activos de la base de datos.
    // Por qué existe: Provee las opciones de amenazas para el select del formulario de factores de riesgo.
    // Qué problema resuelve: Permite listar dinámicamente las amenazas registradas en la base de datos.
    public List<UbicacionDTO> getAmenazas() throws SQLException {
        // SELECT consulta amenazas activas en amenazas.
        String sql = "SELECT id, nombre FROM amenazas WHERE activo = 1";
        
        // try-with-resources: Asegura la correcta liberación de la conexión física a MySQL.
        try (Connection con = Conexion.obtener(); // Solicita la conexión.
             PreparedStatement ps = con.prepareStatement(sql); // Prepara la consulta.
             ResultSet rs = ps.executeQuery()) { // Ejecuta el query.
             
            // Inicializa la lista.
            List<UbicacionDTO> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(new UbicacionDTO(rs.getInt("id"), rs.getString("nombre")));
            }
            // Retorna las amenazas.
            return lista;
        }
    }

    // Qué hace: Obtiene todas las vulnerabilidades activas de la base de datos.
    // Por qué existe: Suministra las opciones para asociar una vulnerabilidad a un factor de riesgo.
    // Qué problema resuelve: Permite listar dinámicamente las vulnerabilidades de la base de datos.
    public List<UbicacionDTO> getVulnerabilidades() throws SQLException {
        // SELECT selecciona del catálogo de vulnerabilidades activas.
        String sql = "SELECT id, nombre FROM vulnerabilidades WHERE activo = 1";
        
        // try-with-resources: Administra la conexión JDBC de forma automática.
        try (Connection con = Conexion.obtener(); // Abre la conexión.
             PreparedStatement ps = con.prepareStatement(sql); // Prepara la sentencia.
             ResultSet rs = ps.executeQuery()) { // Ejecuta la consulta de lectura.
             
            // Se crea la lista y se itera mapeando la fila.
            List<UbicacionDTO> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(new UbicacionDTO(rs.getInt("id"), rs.getString("nombre")));
            }
            // Retorna la lista.
            return lista;
        }
    }

    // Qué hace: Obtiene todos los tipos de recursos activos con sus respectivos servicios de emergencia asociados.
    // Por qué existe: Alimenta el dropdown enlazado del formulario de creación y edición de recursos en el frontend.
    // Qué problema resuelve: Combina de forma óptima las tablas de tipos de recurso y servicios mediante un LEFT JOIN en SQL.
    public List<TipoRecursoDTO> getTiposRecurso() throws SQLException {
        // SELECT selecciona id, nombre, activo y el nombre del servicio de emergencia relacionado.
        // LEFT JOIN se realiza con servicios_emergencia para incluir tipos_recurso que no tengan servicio asignado.
        // WHERE tr.activo = 1 filtra solo los tipos de recurso que estén marcados como activos.
        String sql = "SELECT tr.id, tr.nombre, tr.activo, se.nombre AS servicio "
                   + "FROM tipos_recurso tr "
                   + "LEFT JOIN servicios_emergencia se ON tr.servicio_id = se.id "
                   + "WHERE tr.activo = 1";
        
        // try-with-resources: Abre la conexión a MySQL y prepara el statement de forma segura.
        try (Connection con = Conexion.obtener(); // Obtiene la conexión activa.
             PreparedStatement ps = con.prepareStatement(sql); // Prepara el statement.
             ResultSet rs = ps.executeQuery()) { // Ejecuta la lectura.
             
            // Inicializa la lista de tipos de recurso.
            List<TipoRecursoDTO> lista = new ArrayList<>();
            while (rs.next()) {
                // Instancia el DTO correspondiente y mapea cada columna del ResultSet.
                TipoRecursoDTO dto = new TipoRecursoDTO();
                dto.setId(rs.getInt("id"));
                dto.setNombre(rs.getString("nombre"));
                // Setea el servicio de emergencia, controlando nulos con valor por defecto "Otro".
                dto.setServicio(rs.getString("servicio") != null ? rs.getString("servicio") : "Otro");
                dto.setActivo(rs.getInt("activo"));
                
                // Agrega el DTO a la lista de retorno.
                lista.add(dto);
            }
            // Retorna la lista resultante.
            return lista;
        }
    }
}       lista.add(dto);
            }
            return lista;
        }
    }
}