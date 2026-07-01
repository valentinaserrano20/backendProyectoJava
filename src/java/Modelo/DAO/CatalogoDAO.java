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
import java.sql.Connection; // Representa la conexión física con la base de datos
import java.sql.PreparedStatement; // Permite ejecutar consultas SQL parametrizadas de forma segura
import java.sql.ResultSet; // Contiene los resultados devueltos por una consulta SQL
import java.sql.SQLException; // Excepción que se lanza cuando ocurre un error en la base de datos
import java.util.ArrayList; // Implementación de lista dinámica para almacenar los resultados
import java.util.List; // Interfaz genérica para trabajar con colecciones de datos

// Retorna colecciones de DTOs en lugar de JSON org.json.JSONArray para cumplir con la arquitectura MVC limpia
public class CatalogoDAO {

    // Método que obtiene todos los tipos de documento de la base de datos
    public List<TipoDocumentoDTO> getTiposDocumento() throws SQLException {
        // Se define la consulta SQL que selecciona las columnas id, sigla, descripcion
        // (renombrada como nombre) y activo de la tabla tipo_documentos
        String sql = "SELECT id, sigla, descripcion AS nombre, activo FROM tipo_documentos";
        // Se usa try-with-resources para asegurar que los recursos JDBC se cierren
        // automáticamente al terminar
        try (Connection con = Conexion.obtener(); // Se obtiene una conexión a MySQL desde el pool de conexiones
                PreparedStatement ps = con.prepareStatement(sql); // Se prepara la consulta SQL para ejecución segura
                                                                  // contra inyección SQL
                ResultSet rs = ps.executeQuery()) { // Se ejecuta la consulta y se obtienen los resultados en un
                                                    // ResultSet
            // Se crea una lista dinámica para almacenar todos los tipos de documento
            // encontrados
            List<TipoDocumentoDTO> lista = new ArrayList<>();
            // Se recorre cada fila del ResultSet mientras haya registros disponibles
            while (rs.next()) {
                // AGREGADO: Mapeo de fila ResultSet a TipoDocumentoDTO
                // Se crea una nueva instancia del DTO para almacenar los datos de la fila
                // actual
                TipoDocumentoDTO dto = new TipoDocumentoDTO();
                // Se obtiene el valor de la columna id de tipo entero y se asigna al DTO
                dto.setId(rs.getInt("id"));
                // Se obtiene el valor de la columna sigla de tipo texto y se asigna al DTO
                dto.setSigla(rs.getString("sigla"));
                // Se obtiene el valor de la columna nombre (alias de descripcion) de tipo texto
                // y se asigna al DTO
                dto.setNombre(rs.getString("nombre"));
                // Se obtiene el valor de la columna activo de tipo entero (1=activo,
                // 0=inactivo) y se asigna al DTO
                dto.setActivo(rs.getInt("activo"));
                // Se agrega el DTO mapeado a la lista de resultados
                lista.add(dto);
            }
            // Se retorna la lista completa con todos los tipos de documento encontrados en
            // la base de datos
            return lista;
        }
    }

    // Método que obtiene todos los géneros de la base de datos
    public List<GeneroDTO> getGeneros() throws SQLException {
        // Se define la consulta SQL que selecciona las columnas id, nombre y activo de
        // la tabla generos
        String sql = "SELECT id, nombre, activo FROM generos";
        // Se usa try-with-resources para asegurar que los recursos JDBC se cierren
        // automáticamente al terminar
        try (Connection con = Conexion.obtener(); // Se obtiene una conexión a MySQL desde el pool de conexiones
                PreparedStatement ps = con.prepareStatement(sql); // Se prepara la consulta SQL para ejecución segura
                                                                  // contra inyección SQL
                ResultSet rs = ps.executeQuery()) { // Se ejecuta la consulta y se obtienen los resultados en un
                                                    // ResultSet
            // Se crea una lista dinámica para almacenar todos los géneros encontrados
            List<GeneroDTO> lista = new ArrayList<>();
            // Se recorre cada fila del ResultSet mientras haya registros disponibles
            while (rs.next()) {
                // AGREGADO: Mapeo de fila ResultSet a GeneroDTO
                // Se crea una nueva instancia del DTO para almacenar los datos de la fila
                // actual
                GeneroDTO dto = new GeneroDTO();
                // Se obtiene el valor de la columna id de tipo entero y se asigna al DTO
                dto.setId(rs.getInt("id"));
                // Se obtiene el valor de la columna nombre de tipo texto y se asigna al DTO
                dto.setNombre(rs.getString("nombre"));
                // Se obtiene el valor de la columna activo de tipo entero (1=activo,
                // 0=inactivo) y se asigna al DTO
                dto.setActivo(rs.getInt("activo"));
                // Se agrega el DTO mapeado a la lista de resultados
                lista.add(dto);
            }
            // Se retorna la lista completa con todos los géneros encontrados en la base de
            // datos
            return lista;
        }
    }

    // Método que obtiene todas las organizaciones de la base de datos
    public List<OrganizacionDTO> getOrganizaciones() throws SQLException {
        // Se define la consulta SQL que selecciona las columnas id, nombre y activo de
        // la tabla organizaciones
        String sql = "SELECT id, nombre, activo FROM organizaciones";
        // Se usa try-with-resources para asegurar que los recursos JDBC se cierren
        // automáticamente al terminar
        try (Connection con = Conexion.obtener(); // Se obtiene una conexión a MySQL desde el pool de conexiones
                PreparedStatement ps = con.prepareStatement(sql); // Se prepara la consulta SQL para ejecución segura
                                                                  // contra inyección SQL
                ResultSet rs = ps.executeQuery()) { // Se ejecuta la consulta y se obtienen los resultados en un
                                                    // ResultSet
            // Se crea una lista dinámica para almacenar todas las organizaciones
            // encontradas
            List<OrganizacionDTO> lista = new ArrayList<>();
            // Se recorre cada fila del ResultSet mientras haya registros disponibles
            while (rs.next()) {
                // AGREGADO: Mapeo de fila ResultSet a OrganizacionDTO
                // Se crea una nueva instancia del DTO para almacenar los datos de la fila
                // actual
                OrganizacionDTO dto = new OrganizacionDTO();
                // Se obtiene el valor de la columna id de tipo entero y se asigna al DTO
                dto.setId(rs.getInt("id"));
                // Se obtiene el valor de la columna nombre de tipo texto y se asigna al DTO
                dto.setNombre(rs.getString("nombre"));
                // Se obtiene el valor de la columna activo de tipo entero (1=activo,
                // 0=inactivo) y se asigna al DTO
                dto.setActivo(rs.getInt("activo"));
                // Se agrega el DTO mapeado a la lista de resultados
                lista.add(dto);
            }
            // Se retorna la lista completa con todas las organizaciones encontradas en la
            // base de datos
            return lista;
        }
    }

    // =========================================================
    // MÉTODOS DE CATÁLOGOS OPTIMIZADOS (VÍA HELPER GENÉRICO)
    // =========================================================

    // Qué hace: Método helper genérico privado para consultar catálogos paramétricos activos.
    // Por qué existe: Reduce drásticamente la duplicación de código en consultas SQL idénticas para listados simples.
    private List<UbicacionDTO> getCatalogoGenerico(String tabla) throws SQLException {
        String sql = "SELECT id, nombre FROM " + tabla + " WHERE activo = 1";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<UbicacionDTO> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(new UbicacionDTO(rs.getInt("id"), rs.getString("nombre")));
            }
            return lista;
        }
    }

    // Método que obtiene los tipos de zona residencial (Urbana / Rural)
    public List<UbicacionDTO> getTiposZona() throws SQLException {
        return getCatalogoGenerico("tipos_zona");
    }

    // Método que obtiene todos los sectores geográficos activos
    public List<UbicacionDTO> getSectores() throws SQLException {
        return getCatalogoGenerico("sectores");
    }

    // Método que obtiene todos los regímenes o calidades de vivienda activos
    public List<UbicacionDTO> getCalidadesVivienda() throws SQLException {
        return getCatalogoGenerico("calidad_vivienda");
    }

    // Qué hace: Obtiene todos los parentescos activos de la base de datos
    public List<UbicacionDTO> getParentescos() throws SQLException {
        return getCatalogoGenerico("parentescos");
    }

    // Qué hace: Obtiene todos los grupos sanguíneos activos de la base de datos
    public List<UbicacionDTO> getGruposSanguineos() throws SQLException {
        return getCatalogoGenerico("grupos_sanguineos");
    }

    // Qué hace: Obtiene todas las nacionalidades activas de la base de datos
    public List<UbicacionDTO> getNacionalidades() throws SQLException {
        return getCatalogoGenerico("nacionalidades");
    }

    // Qué hace: Obtiene todas las especies de mascotas activas de la base de datos
    public List<UbicacionDTO> getEspeciesMascota() throws SQLException {
        return getCatalogoGenerico("especies_mascota");
    }

    // Qué hace: Obtiene los géneros de mascota activos de la base de datos
    public List<UbicacionDTO> getGenerosMascota() throws SQLException {
        return getCatalogoGenerico("generos_mascota");
    }

    // Qué hace: Obtiene todas las amenazas o tipos de riesgo activos de la base de datos
    public List<UbicacionDTO> getAmenazas() throws SQLException {
        return getCatalogoGenerico("amenazas");
    }

    // Qué hace: Obtiene todas las vulnerabilidades activas de la base de datos
    public List<UbicacionDTO> getVulnerabilidades() throws SQLException {
        return getCatalogoGenerico("vulnerabilidades");
    }

    // Qué hace: Obtiene todos los tipos de recursos activos con sus respectivos
    // servicios de emergencia asociados.
    // Por qué existe: Alimenta el dropdown enlazado del formulario de creación y
    // edición de recursos en el frontend.
    // Qué problema resuelve: Combina de forma óptima las tablas de tipos de recurso
    // y servicios mediante un LEFT JOIN en SQL.
    public List<TipoRecursoDTO> getTiposRecurso() throws SQLException {
        // SELECT selecciona id, nombre, activo y el nombre del servicio de emergencia
        // relacionado.
        // LEFT JOIN se realiza con servicios_emergencia para incluir tipos_recurso que
        // no tengan servicio asignado.
        // WHERE tr.activo = 1 filtra solo los tipos de recurso que estén marcados como
        // activos.
        String sql = "SELECT tr.id, tr.nombre, tr.activo, se.nombre AS servicio "
                + "FROM tipos_recurso tr "
                + "LEFT JOIN servicios_emergencia se ON tr.servicio_id = se.id "
                + "WHERE tr.activo = 1";

        // try-with-resources: Abre la conexión a MySQL y prepara el statement de forma
        // segura.
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
                // Setea el servicio de emergencia, controlando nulos con valor por defecto
                // "Otro".
                dto.setServicio(rs.getString("servicio") != null ? rs.getString("servicio") : "Otro");
                dto.setActivo(rs.getInt("activo"));

                // Agrega el DTO a la lista de retorno.
                lista.add(dto);
            }
            // Retorna la lista resultante.
            return lista;
        }
    }
}
