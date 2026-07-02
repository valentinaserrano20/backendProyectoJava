package Modelo.DAO;

/*
 * Qué hace (la acción): Importa el administrador de conexiones de base de datos MySQL, varios DTOs paramétricos y las clases JDBC y colecciones de Java estándar.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.Config.Conexion: Clase que gestiona el pool de conexiones físicas con la base de datos MySQL.
 *   - java.sql.*: APIs estándar para interacción relacional en Java.
 *   - java.util.*: Clases de colecciones como ArrayList y List.
 * Para qué se usa (el propósito): Proporcionar soporte para realizar consultas SQL de tipo diccionario (catálogos) y retornar colecciones fuertemente tipadas en DTOs.
 * Por qué es importante (el impacto o problema que resuelve): Permite alimentar los elementos interactivos desplegables (comboboxes) del frontend con datos estructurados directamente desde la base de datos.
 */
import Modelo.Config.Conexion;
// Importa los DTOs (Data Transfer Objects) para mapear los resultados de las consultas
import Modelo.DTO.GeneroDTO;
import Modelo.DTO.OrganizacionDTO;
import Modelo.DTO.TipoDocumentoDTO;
import Modelo.DTO.UbicacionDTO; // CORREGIDO: Importación requerida para mapear las zonas
import Modelo.DTO.TipoRecursoDTO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/*
 * Qué hace (la acción): Define la clase CatalogoDAO que centraliza las lecturas de los catálogos y diccionarios estáticos del sistema en la base de datos.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - DAO: Objeto de acceso a datos que interactúa exclusivamente con SQL.
 * Para qué se usa (el propósito): Abastecer los listados de parametrización como géneros, tipos de documento, nacionalidades, amenazas, etc.
 * Por qué es importante (el impacto o problema que resuelve): Garantiza que los formularios web del cliente usen únicamente valores válidos y sincronizados con las llaves foráneas definidas en MySQL.
 */
public class CatalogoDAO {

    /*
     * Qué hace (la acción): Consulta y devuelve todos los tipos de documento registrados en la base de datos.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - SELECT id, sigla, descripcion AS nombre, activo FROM tipo_documentos: Consulta SQL para extraer la información.
     *   - TipoDocumentoDTO: Objeto que empaqueta las propiedades de un tipo de documento.
     * Para qué se usa (el propósito): Cargar el selector de tipo de documento en formularios de registro y perfiles de usuarios.
     * Por qué es importante (el impacto o problema que resuelve): Evita codificar en duro las opciones en el frontend, permitiendo la adición de nuevos tipos de documento desde la base de datos.
     */
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

    /*
     * Qué hace (la acción): Consulta y devuelve el catálogo completo de géneros.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - GeneroDTO: DTO que contiene ID, nombre y estado activo/inactivo del género.
     * Para qué se usa (el propósito): Llenar selectores correspondientes en los formularios de registro de integrantes de la familia.
     * Por qué es importante (el impacto o problema que resuelve): Normaliza el campo género a nivel de base de datos e interfaz gráfica.
     */
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

    /*
     * Qué hace (la acción): Recupera la lista de todas las organizaciones o entidades externas registradas.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - OrganizacionDTO: Representa una entidad (ej. Cruz Roja, Bomberos) que brinda soporte o supervisa.
     * Para qué se usa (el propósito): Listar las organizaciones disponibles para la asignación de supervisores.
     * Por qué es importante (el impacto o problema que resuelve): Permite asociar dinámicamente personal de campo a sus respectivas instituciones oficiales.
     */
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

    /*
     * Qué hace (la acción): Consulta de forma genérica cualquier tabla de catálogo simple que contenga las columnas 'id', 'nombre' y 'activo = 1'.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - UbicacionDTO: Objeto genérico para pares clave-valor (ID y Nombre) usado para ubicaciones, amenazas y listados simples.
     * Para qué se usa (el propósito): Centralizar y reutilizar la lógica SQL para tablas paramétricas idénticas.
     * Por qué es importante (el impacto o problema que resuelve): Reduce la redundancia de código en más de 10 métodos, facilitando futuras modificaciones globales en catálogos simples.
     */
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

    /*
     * Qué hace (la acción): Obtiene los tipos de zona residencial (Urbana / Rural) activos.
     * Qué significa (conceptos, métodos, tipos involucrados): Invoca a getCatalogoGenerico pasando "tipos_zona".
     * Para qué se usa (el propósito): Seleccionar el tipo de zona en la localización de la vivienda familiar.
     */
    public List<UbicacionDTO> getTiposZona() throws SQLException {
        return getCatalogoGenerico("tipos_zona");
    }

    /*
     * Qué hace (la acción): Obtiene los sectores geográficos o comunas registrados en el sistema.
     * Qué significa (conceptos, métodos, tipos involucrados): Invoca a getCatalogoGenerico pasando "sectores".
     * Para qué se usa (el propósito): Asignar el sector correspondiente a una vivienda.
     */
    public List<UbicacionDTO> getSectores() throws SQLException {
        return getCatalogoGenerico("sectores");
    }

    /*
     * Qué hace (la acción): Obtiene la tipificación o calidades de construcción del hogar (vivienda).
     * Qué significa (conceptos, métodos, tipos involucrados): Invoca a getCatalogoGenerico pasando "calidad_vivienda".
     * Para qué se usa (el propósito): Estudiar la resistencia física del hogar en el análisis de vulnerabilidad.
     */
    public List<UbicacionDTO> getCalidadesVivienda() throws SQLException {
        return getCatalogoGenerico("calidad_vivienda");
    }

    /*
     * Qué hace (la acción): Obtiene los grados de parentesco del núcleo familiar.
     * Qué significa (conceptos, métodos, tipos involucrados): Invoca a getCatalogoGenerico pasando "parentescos".
     * Para qué se usa (el propósito): Identificar el rol (Padre, Madre, Hijo, etc.) de un integrante respecto al jefe de hogar.
     */
    public List<UbicacionDTO> getParentescos() throws SQLException {
        return getCatalogoGenerico("parentescos");
    }

    /*
     * Qué hace (la acción): Obtiene los tipos de grupos sanguíneos activos.
     * Qué significa (conceptos, métodos, tipos involucrados): Invoca a getCatalogoGenerico pasando "grupos_sanguineos".
     * Para qué se usa (el propósito): Registrar datos médicos básicos en la ficha familiar de emergencias.
     */
    public List<UbicacionDTO> getGruposSanguineos() throws SQLException {
        return getCatalogoGenerico("grupos_sanguineos");
    }

    /*
     * Qué hace (la acción): Obtiene todas las nacionalidades configuradas en la base de datos.
     * Qué significa (conceptos, métodos, tipos involucrados): Invoca a getCatalogoGenerico pasando "nacionalidades".
     * Para qué se usa (el propósito): Indicar el origen de los integrantes en censos familiares.
     */
    public List<UbicacionDTO> getNacionalidades() throws SQLException {
        return getCatalogoGenerico("nacionalidades");
    }

    /*
     * Qué hace (la acción): Obtiene el catálogo de especies de mascotas admitidas (ej: perro, gato).
     * Qué significa (conceptos, métodos, tipos involucrados): Invoca a getCatalogoGenerico pasando "especies_mascota".
     * Para qué se usa (el propósito): Formular el registro animal dentro del plan familiar de evacuación.
     */
    public List<UbicacionDTO> getEspeciesMascota() throws SQLException {
        return getCatalogoGenerico("especies_mascota");
    }

    /*
     * Qué hace (la acción): Obtiene los géneros biológicos aplicables para el registro animal.
     * Qué significa (conceptos, métodos, tipos involucrados): Invoca a getCatalogoGenerico pasando "generos_mascota".
     * Para qué se usa (el propósito): Caracterizar el sexo de las mascotas del hogar.
     */
    public List<UbicacionDTO> getGenerosMascota() throws SQLException {
        return getCatalogoGenerico("generos_mascota");
    }

    /*
     * Qué hace (la acción): Obtiene todas las amenazas o factores de peligro ambiental (ej: inundación, sismo).
     * Qué significa (conceptos, métodos, tipos involucrados): Invoca a getCatalogoGenerico pasando "amenazas".
     * Para qué se usa (el propósito): Determinar qué catástrofe acecha al territorio de la familia.
     */
    public List<UbicacionDTO> getAmenazas() throws SQLException {
        return getCatalogoGenerico("amenazas");
    }

    /*
     * Qué hace (la acción): Obtiene la lista de factores de vulnerabilidad física y social.
     * Qué significa (conceptos, métodos, tipos involucrados): Invoca a getCatalogoGenerico pasando "vulnerabilidades".
     * Para qué se usa (el propósito): Respaldar los análisis de riesgo integrados.
     */
    public List<UbicacionDTO> getVulnerabilidades() throws SQLException {
        return getCatalogoGenerico("vulnerabilidades");
    }

    /*
     * Qué hace (la acción): Consulta todos los tipos de recursos de emergencia activos junto con su entidad del servicio asociado.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - LEFT JOIN tr.servicio_id = se.id: Conecta el tipo de recurso con el servicio que lo provee o atiende (ej: bomberos).
     *   - TipoRecursoDTO: Contiene ID, nombre del recurso, servicio y estado.
     * Para qué se usa (el propósito): Desplegar el listado de recursos de asistencia comunitaria disponibles para los voluntarios en campo.
     * Por qué es important (el impacto o problema que resuelve): Relaciona lógicamente cada recurso con la institución que lo gestiona directamente, evitando confusión al despachar ayuda.
     */
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
