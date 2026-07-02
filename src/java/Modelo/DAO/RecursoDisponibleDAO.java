package Modelo.DAO;

/*
 * Qué hace (la acción): Importa la conexión física de base de datos relacionales, el DTO de recursos comunitarios de emergencia, y las APIs de JDBC y colecciones de Java.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.Config.Conexion: Módulo de conexión a MySQL.
 *   - Modelo.DTO.RecursoDisponibleDTO: Objeto que empaqueta las propiedades de un recurso externo de emergencia (nombre de lugar, teléfono, descripción, distancia).
 *   - java.sql.*: APIs estándar de interacción relacional (Connection, PreparedStatement, ResultSet, SQLException, Types).
 * Para qué se usa (el propósito): Proveer las herramientas de conectividad y estructuras necesarias para persistir y consultar la asistencia comunitaria disponible para el hogar.
 * Por qué es importante (el impacto o problema que resuelve): Permite registrar lugares de apoyo (ej: centros de salud, CAI, bomberos) que la familia puede utilizar en caso de catástrofe.
 */
import Modelo.Config.Conexion;
import Modelo.DTO.RecursoDisponibleDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/*
 * Qué hace (la acción): Define la clase RecursoDisponibleDAO encargada de realizar operaciones CRUD en la tabla 'recursos_disponibles' de MySQL.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - DAO (Data Access Object): Centraliza las operaciones SQL de la entidad RecursoDisponible.
 * Para qué se usa (el propósito): Gestionar los recursos de asistencia comunitaria disponibles para los hogares censados.
 * Por qué es importante (el impacto o problema que resuelve): Aísla por completo la complejidad SQL relacional cruzada con las tablas tipos_recurso y servicios_emergencia.
 */
public class RecursoDisponibleDAO {

    /*
     * Qué hace (la acción): Consulta la cantidad total de recursos comunitarios asignados a un plan de emergencia familiar en MySQL.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - SELECT COUNT(*): Cuenta registros filtrando por plan_id.
     * Para qué se usa (el propósito): Suministrar al paginador de la interfaz web la cantidad exacta de registros del plan.
     */
    public int obtenerTotalRecursos(int planId) throws SQLException {
        // Explicación detallada de la consulta SQL:
        // - Comando SELECT COUNT(*) AS total: Cuenta la cantidad total de registros (filas) de recursos asociados al plan familiar y asigna el alias "total" a la columna resultante para leerla en Java.
        // - Tabla FROM recursos_disponibles: Especifica la tabla física de donde se extraerán y contarán los datos.
        // - Filtro WHERE plan_id = ?: Condición limitadora que restringe el conteo a los registros asociados al ID del plan familiar indicado.
        // - Qué retorna: Una fila única con una columna llamada "total" conteniendo el número de recursos encontrados.
        String sql = "SELECT COUNT(*) AS total FROM recursos_disponibles WHERE plan_id = ?";
        // Qué hace: Abre la conexión a la base de datos y compila el PreparedStatement.
        // Por qué existe: Previene la inyección SQL al parametrizar los valores de entrada.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Vincula el ID del plan familiar al primer marcador de parámetro de la consulta.
            ps.setInt(1, planId);
            // Qué hace: Ejecuta la consulta de conteo y lee el ResultSet.
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Qué hace: Retorna la cantidad total de recursos encontrados.
                    return rs.getInt("total");
                }
            }
        }
        // Qué hace: Retorna 0 si la consulta no arrojó resultados.
        return 0;
    }

    /*
     * Qué hace (la acción): Obtiene una lista paginada de los recursos comunitarios asociados a un plan familiar.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - LEFT JOIN: Une el recurso disponible con el catálogo de tipos de recurso y el de servicios de emergencia.
     *   - LIMIT ? OFFSET ?: Cláusulas JDBC para recuperar un segmento de la tabla.
     * Para qué se usa (el propósito): Mostrar los recursos de asistencia del hogar por segmentos dosificados en la UI.
     */
    public List<RecursoDisponibleDTO> listarRecursosPorPlan(int planId, int limit, int offset) throws SQLException {
        // Explicación detallada de la consulta SQL:
        // - Columnas consultadas: Selecciona las columnas básicas del recurso comunitario y los nombres descriptivos cruzados: 'resource_name' (de tr) y 'service_name' (de se).
        // - Tabla principal FROM recursos_disponibles r: Define la tabla base de la consulta.
        // - Relación LEFT JOIN tipos_recurso tr ON r.tipo_recurso_id = tr.id: Une recursos_disponibles con tipos_recurso. Se utiliza LEFT JOIN para que el recurso comunitario se liste de todas maneras en pantalla aun si carece de un tipo de recurso asignado (tipo_recurso_id es null), poblando con null el nombre en lugar de descartar el recurso.
        // - Relación LEFT JOIN servicios_emergencia se ON tr.servicio_id = se.id: Une la tabla intermedia tipos_recurso con servicios_emergencia para recuperar el nombre del servicio (bomberos, policía, etc.) asociado al recurso, de forma opcional.
        // - Filtro WHERE r.plan_id = ?: Restringe los resultados a los recursos vinculados al plan de emergencia familiar indicado.
        // - Cláusula LIMIT ?: Limita la cantidad de registros devueltos para evitar sobrecargas en la UI (paginación).
        // - Cláusula OFFSET ?: Indica cuántos registros iniciales se deben omitir para retornar la página correspondiente.
        // - Qué retorna: Una lista de filas donde cada fila representa un recurso comunitario con su tipo y servicio asociado.
        String sql = "SELECT r.id, r.nombre_lugar, r.distancia_metros, r.telefono, r.descripcion, r.tipo_recurso_id, "
                   + "tr.nombre AS resource_name, se.nombre AS service_name "
                   + "FROM recursos_disponibles r "
                   + "LEFT JOIN tipos_recurso tr ON r.tipo_recurso_id = tr.id "
                   + "LEFT JOIN servicios_emergencia se ON tr.servicio_id = se.id "
                   + "WHERE r.plan_id = "
                   + "? LIMIT ? OFFSET ?";
        
        // Qué hace: Abre la conexión a base de datos y compila el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Asigna los valores del ID del plan, el límite y el offset al statement JDBC.
            ps.setInt(1, planId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            
            // Qué hace: Inicializa la lista dinámica que contendrá los DTOs de recursos.
            List<RecursoDisponibleDTO> lista = new ArrayList<>();
            // Qué hace: Ejecuta la consulta de lectura y procesa el ResultSet.
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Qué hace: Instancia el DTO para mapear la fila actual.
                    RecursoDisponibleDTO dto = new RecursoDisponibleDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setPlaceName(rs.getString("nombre_lugar"));
                    dto.setDistance(rs.getInt("distancia_metros"));
                    // Qué hace: Valida nulos en teléfono y descripción.
                    dto.setPhone(rs.getString("telefono") != null ? rs.getString("telefono") : "No registrado");
                    dto.setDescription(rs.getString("descripcion") != null ? rs.getString("descripcion") : "");
                    dto.setResourceTypeId(rs.getInt("tipo_recurso_id"));
                    dto.setResourceTypeName(rs.getString("resource_name") != null ? rs.getString("resource_name") : "No especificado");
                    dto.setServiceName(rs.getString("service_name") != null ? rs.getString("service_name") : "Otro");
                    // Qué hace: Añade el recurso DTO al listado de retorno.
                    lista.add(dto);
                }
            }
            // Qué hace: Retorna la lista resultante de recursos.
            return lista;
        }
    }

    /*
     * Qué hace (la acción): Obtiene la información estructurada de un recurso comunitario disponible por su ID.
     * Qué significa (conceptos, métodos, tipos involucrados): Mapea las columnas del recurso y los nombres legibles de su tipo y servicio.
     * Para qué se usa (el propósito): Recuperar los campos de un recurso de emergencia familiar para precargarlos en los formularios de edición de la SPA.
     */
    public RecursoDisponibleDTO obtenerRecurso(int id) throws SQLException {
        // Explicación detallada de la consulta SQL:
        // - Columnas consultadas: Selecciona la información de un recurso individual por su ID único.
        // - Relaciones LEFT JOIN (tr, se): Conecta de forma no restrictiva el recurso con sus tablas catálogos. Se prefiere LEFT JOIN para recuperar exitosamente la información física del lugar aun si este carece de tipo de recurso o servicio de emergencia asignados.
        // - Filtro WHERE r.id = ?: Restringe los resultados a la clave primaria única del recurso.
        // - Qué retorna: Una fila única que se mapea directamente en Java al DTO RecursoDisponibleDTO, o null si no se localiza.
        String sql = "SELECT r.id, r.nombre_lugar, r.distancia_metros, r.telefono, r.descripcion, r.plan_id, r.tipo_recurso_id, "
                   + "tr.nombre AS resource_name, se.nombre AS service_name "
                   + "FROM recursos_disponibles r "
                   + "LEFT JOIN tipos_recurso tr ON r.tipo_recurso_id = tr.id "
                   + "LEFT JOIN servicios_emergencia se ON tr.servicio_id = se.id "
                   + "WHERE r.id = ?";
        
        // Qué hace: Abre la conexión a la base de datos y compila el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Vincula el ID del recurso al statement JDBC.
            ps.setInt(1, id);
            // Qué hace: Ejecuta la consulta de lectura de base de datos.
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Qué hace: Instancia el DTO y mapea cada columna recuperada.
                    RecursoDisponibleDTO dto = new RecursoDisponibleDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setPlaceName(rs.getString("nombre_lugar"));
                    dto.setDistance(rs.getInt("distancia_metros"));
                    dto.setPhone(rs.getString("telefono"));
                    dto.setDescription(rs.getString("descripcion") != null ? rs.getString("descripcion") : "");
                    dto.setPlanId(rs.getInt("plan_id"));
                    dto.setResourceTypeId(rs.getInt("tipo_recurso_id"));
                    dto.setResourceTypeName(rs.getString("resource_name") != null ? rs.getString("resource_name") : "");
                    dto.setServiceName(rs.getString("service_name") != null ? rs.getString("service_name") : "");
                    // Qué hace: Retorna el recurso encontrado.
                    return dto;
                }
            }
        }
        // Qué hace: Retorna null si el recurso no existe.
        return null;
    }

    /*
     * Qué hace (la acción): Inserta un nuevo recurso comunitario en la tabla 'recursos_disponibles' y retorna el ID autogenerado asignado.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - Statement.RETURN_GENERATED_KEYS: Habilita el retorno del ID autoincremental de la base de datos MySQL.
     * Para qué se usa (el propósito): Registrar una zona de ayuda en el plan de emergencia familiar.
     */
    public int crearRecurso(RecursoDisponibleDTO dto) throws SQLException {
        // Explicación detallada de la consulta SQL:
        // - Comando INSERT INTO: Inserta una fila física con los datos del recurso.
        // - Columnas indicadas: nombre_lugar, distancia_metros, telefono, descripcion, plan_id, tipo_recurso_id.
        // - Cláusula VALUES (?, ?, ?, ?, ?, ?): Define los marcadores de posición posicionales que recibirán los valores sanitizados para evitar inyecciones de código.
        String sql = "INSERT INTO recursos_disponibles (nombre_lugar, distancia_metros, telefono, descripcion, plan_id, tipo_recurso_id) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        
        // Qué hace: Abre la conexión a la base de datos y prepara el PreparedStatement con retorno de llaves generadas.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            // Qué hace: Vincula los parámetros básicos del DTO al statement.
            ps.setString(1, dto.getPlaceName());
            ps.setInt(2, dto.getDistance());
            // Qué hace: Setea NULL si el teléfono o la descripción vienen vacíos.
            ps.setString(3, dto.getPhone() != null && !dto.getPhone().isEmpty() ? dto.getPhone() : null);
            ps.setString(4, dto.getDescription() != null && !dto.getDescription().isEmpty() ? dto.getDescription() : null);
            ps.setInt(5, dto.getPlanId());
            
            // Qué hace: Maneja la clave foránea condicional de tipo de recurso.
            if (dto.getResourceTypeId() > 0) {
                ps.setInt(6, dto.getResourceTypeId());
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            
            // Qué hace: Ejecuta la inserción.
            ps.executeUpdate();
            
            // Qué hace: Recupera la llave autoincremental de MySQL.
            try (ResultSet rsKeys = ps.getGeneratedKeys()) {
                if (rsKeys.next()) {
                    return rsKeys.getInt(1);
                }
            }
        }
        // Qué hace: Lanza una excepción si falló la creación del registro.
        throw new SQLException("No se pudo obtener el ID autogenerado del recurso.");
    }

    /*
     * Qué hace (la acción): Modifica la localización, contacto y tipo de recurso de un recurso existente.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - UPDATE: Modifica los campos relacionales del recurso por su ID.
     * Para qué se usa (el propósito): Salvar los cambios del recurso editado en el frontend.
     */
    public void actualizarRecurso(int id, RecursoDisponibleDTO dto) throws SQLException {
        // Explicación detallada de la consulta SQL:
        // - Comando UPDATE: Modifica valores existentes en la tabla recursos_disponibles.
        // - Cláusula SET: Define qué campos serán actualizados y sus marcadores correspondientes.
        // - Filtro WHERE id = ?: Restringe de forma estricta la actualización al ID de recurso especificado, evitando alterar otras filas.
        String sql = "UPDATE recursos_disponibles SET nombre_lugar = ?, distancia_metros = ?, telefono = ?, descripcion = ?, tipo_recurso_id = ? "
                   + "WHERE id = ?";
        
        // Qué hace: Abre la conexión JDBC y prepara el PreparedStatement.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            // Qué hace: Vincula los parámetros modificados.
            ps.setString(1, dto.getPlaceName());
            ps.setInt(2, dto.getDistance());
            // Qué hace: Maneja nulos en campos de texto opcionales.
            ps.setString(3, dto.getPhone() != null && !dto.getPhone().isEmpty() ? dto.getPhone() : null);
            ps.setString(4, dto.getDescription() != null && !dto.getDescription().isEmpty() ? dto.getDescription() : null);
            
            // Qué hace: Setea la clave foránea o NULL si no es válida.
            if (dto.getResourceTypeId() > 0) {
                ps.setInt(5, dto.getResourceTypeId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            
            // Qué hace: Vincula el ID del recurso para el WHERE.
            ps.setInt(6, id);
            // Qué hace: Ejecuta la consulta de actualización en la base de datos.
            ps.executeUpdate();
        }
    }

    // Qué hace: Elimina físicamente un registro de recurso disponible de la base de datos MySQL por su ID.
    // Por qué existe: Habilita la baja o eliminación de recursos erróneos cargados por el voluntario.
    // Qué problema resuelve: Borra el registro de forma atómica y segura mediante JDBC.
    public void eliminarRecurso(int id) throws SQLException {
        // Explicación detallada de la consulta SQL:
        // - Comando DELETE FROM: Remueve permanentemente el registro físico del recurso de la base de datos MySQL.
        // - Filtro WHERE id = ?: Asegura que la eliminación afecte únicamente a la fila identificada por el ID.
        String sql = "DELETE FROM recursos_disponibles WHERE id = ?";
        // Qué hace: Abre la conexión JDBC y prepara el PreparedStatement.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Vincula el ID del recurso y ejecuta el delete.
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
