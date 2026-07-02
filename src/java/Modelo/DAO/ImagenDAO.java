package Modelo.DAO;

/*
 * Qué hace (la acción): Importa el gestor de conexiones a bases de datos relacionales, el DTO que almacena las propiedades de los archivos gráficos y las clases e interfaces del paquete java.sql e java.util.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.Config.Conexion: Establece conexiones de base de datos MySQL.
 *   - Modelo.DTO.ImagenDTO: Almacena metadatos del archivo de imagen (ruta, descripción, tipo de gráfico).
 *   - java.sql.*: Librería estándar de Java para interacción con bases de datos SQL.
 * Para qué se usa (el propósito): Habilitar la recuperación y almacenamiento de croquis, mapas de georreferenciación y croquis de entorno del plan familiar.
 * Por qué es importante (el impacto o problema que resuelve): Permite registrar las rutas locales o URL de los archivos subidos para que la aplicación muestre planos de evacuación dinámicos.
 */
import Modelo.Config.Conexion;
import Modelo.DTO.ImagenDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/*
 * Qué hace (la acción): Define la clase ImagenDAO que implementa operaciones CRUD físicas en la tabla 'imagenes' de MySQL.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - DAO (Data Access Object): Centraliza las operaciones SQL de la entidad Imagen.
 * Para qué se usa (el propósito): Administrar los archivos gráficos adjuntos a las viviendas familiares dentro de los planes de emergencia.
 * Por qué es importante (el impacto o problema que resuelve): Aísla por completo el código de base de datos para la entidad imágenes, controlando discrepancias de nombres entre el frontend y la base de datos.
 */
public class ImagenDAO {

    /*
     * Qué hace (la acción): Traduce el término del tipo de gráfico manejado en la aplicación al equivalente literal del ENUM de la base de datos.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - "georeferenciacion": Término usado en el frontend.
     *   - "mapa": Término literal configurado en el ENUM físico de MySQL.
     * Para qué se usa (el propósito): Garantizar la compatibilidad relacional al persistir datos sin requerir una alteración física al esquema de base de datos.
     * Por qué es importante (el impacto o problema que resuelve): Evita fallos de inserción y de violación de restricciones de ENUM en MySQL por discrepancias de nomenclatura.
     */
    private String aValorBD(String tipoJava) {
        // Qué hace: Si el valor de java equivale a "georeferenciacion" (ignorando mayúsculas), lo traduce al término enum "mapa" de la base de datos.
        if ("georeferenciacion".equalsIgnoreCase(tipoJava)) {
            return "mapa";
        }
        // Qué hace: En caso contrario, retorna el mismo tipo literal de entrada.
        return tipoJava;
    }

    /*
     * Qué hace (la acción): Traduce el valor literal del ENUM de base de datos al formato esperado por el frontend.
     * Qué significa (conceptos, métodos, tipos involucrados): Mapea "mapa" -> "georeferenciacion".
     * Para qué se usa (el propósito): Devolver al frontend una nomenclatura limpia en español de las rutas e interfaces geográficas.
     */
    private String aValorJava(String tipoBD) {
        // Qué hace: Si el valor de base de datos es "mapa" (ignorando mayúsculas), lo traduce a "georeferenciacion" para consumo de la UI.
        if ("mapa".equalsIgnoreCase(tipoBD)) {
            return "georeferenciacion";
        }
        // Qué hace: En caso contrario, retorna el valor del enum sin modificaciones.
        return tipoBD;
    }

    /*
     * Qué hace (la acción): Cuenta el total de imágenes subidas para un plan familiar y tipo de gráfico específico.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - SELECT COUNT(*): Cuenta registros coincidentes.
     *   - aValorBD(tipo): Traduce el tipo de gráfico antes de vincular el parámetro SQL.
     * Para qué se usa (el propósito): Proveer el conteo necesario para paginar el listado de croquis de la vivienda.
     * Por qué es importante (el impacto o problema que resuelve): Permite calcular la paginación del lado del servidor sin sobrecargar la memoria del servidor cargando objetos completos.
     */
    public int contarPorPlanYTipo(int planId, String tipo) throws SQLException {
        // Explicación de consulta SQL:
        // - SELECT COUNT(*) AS total cuenta el número total de filas.
        // - WHERE plan_id = ? AND tipo_grafico = ? filtra las imágenes pertenecientes al plan y al tipo de gráfico.
        String sql = "SELECT COUNT(*) AS total FROM imagenes WHERE plan_id = ? AND tipo_grafico = ?";
        
        // try-with-resources: Abre de forma segura la conexión JDBC y prepara la consulta.
        try (Connection con = Conexion.obtener(); // Obtiene la conexión activa de base de datos.
             PreparedStatement ps = con.prepareStatement(sql)) { // Prepara la consulta parametrizada.
             
            // Vincula el ID del plan de emergencia familiar al primer parámetro '?'.
            ps.setInt(1, planId);
            // Vincula el tipo de gráfico mapeado (traducido para la BD) al segundo parámetro '?'.
            ps.setString(2, aValorBD(tipo));
            
            // Ejecuta la consulta de lectura y lee la fila única resultante.
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Retorna el valor numérico entero obtenido en la columna 'total'.
                    return rs.getInt("total");
                }
            }
        }
        // Retorna 0 si la consulta no arrojó resultados.
        return 0;
    }

    /*
     * Qué hace (la acción): Recupera un subconjunto paginado de imágenes para un plan y tipo de gráfico.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - LIMIT ? OFFSET ?: Permite extraer únicamente un número limitado de registros a partir de una posición inicial.
     * Para qué se usa (el propósito): Listar los croquis de vivienda por páginas en la interfaz SPA.
     * Por qué es importante (el impacto o problema que resuelve): Optimiza significativamente el tiempo de carga del cliente al traer sólo las imágenes solicitadas.
     */
    public List<ImagenDTO> listarPorPlanYTipo(int planId, String tipo, int limit, int offset) throws SQLException {
        // Explicación de consulta SQL:
        // - SELECT recupera las columnas id, tipo_grafico, ruta_archivo, descripcion y plan_id.
        // - WHERE plan_id = ? AND tipo_grafico = ? filtra por el plan y tipo de gráfico.
        // - ORDER BY id DESC ordena las imágenes de forma descendente (las más recientes primero).
        // - LIMIT ? OFFSET ? pagina los resultados en base de datos MySQL.
        String sql = "SELECT id, tipo_grafico, ruta_archivo, descripcion, plan_id "
                   + "FROM imagenes WHERE plan_id = ? AND tipo_grafico = ? "
                   + "ORDER BY id DESC LIMIT ? OFFSET ?";
        
        // try-with-resources: Inicializa y administra de forma limpia el statement y la conexión.
        try (Connection con = Conexion.obtener(); // Solicita la conexión a base de datos.
             PreparedStatement ps = con.prepareStatement(sql)) { // Prepara la consulta parametrizada.
             
            // Vincula el ID del plan familiar.
            ps.setInt(1, planId);
            // Vincula el tipo de gráfico traducido.
            ps.setString(2, aValorBD(tipo));
            // Vincula el límite de registros de imágenes a obtener.
            ps.setInt(3, limit);
            // Vincula el desplazamiento (offset) inicial de lectura.
            ps.setInt(4, offset);
            
            // Inicializa la lista dinámica para almacenar las imágenes mapeadas.
            List<ImagenDTO> lista = new ArrayList<>();
            
            // Ejecuta la consulta de lectura de base de datos.
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Instancia un nuevo DTO para mapear las columnas de la fila actual
                    ImagenDTO dto = new ImagenDTO();
                    dto.setId(rs.getInt("id"));
                    // Traduce el valor enum de la BD ("mapa" / "entorno") al formato java del front ("georeferenciacion" / "entorno").
                    dto.setTipoGrafico(aValorJava(rs.getString("tipo_grafico")));
                    dto.setPath(rs.getString("ruta_archivo"));
                    // Controla valores nulos en la descripción asignando cadena vacía por defecto.
                    dto.setDescription(rs.getString("descripcion") != null ? rs.getString("descripcion") : "");
                    dto.setPlanId(rs.getInt("plan_id"));
                    
                    // Agrega el DTO a la lista de retorno.
                    lista.add(dto);
                }
            }
            // Retorna la colección completa con las imágenes de la página actual.
            return lista;
        }
    }

    /*
     * Qué hace (la acción): Obtiene una única imagen de base de datos a partir de su ID.
     * Qué significa (conceptos, métodos, tipos involucrados): Mapea las columnas id, tipo_grafico, ruta_archivo, descripcion y plan_id a un DTO.
     * Para qué se usa (el propósito): Mostrar un croquis particular en una ventana flotante o modal en grande.
     */
    public ImagenDTO obtenerPorId(int id) throws SQLException {
        // Explicación de consulta SQL:
        // - SELECT recupera los atributos del registro de imagen.
        // - WHERE id = ? filtra por la clave primaria única.
        String sql = "SELECT id, tipo_grafico, ruta_archivo, descripcion, plan_id FROM imagenes WHERE id = ?";
        
        // try-with-resources: Abre la conexión a la base de datos y compila el PreparedStatement.
        try (Connection con = Conexion.obtener(); // Abre la conexión física.
             PreparedStatement ps = con.prepareStatement(sql)) { // Prepara la consulta parametrizada.
             
            // Vincula el ID de la imagen en la consulta.
            ps.setInt(1, id);
            
            // Ejecuta la consulta y lee el ResultSet.
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Instancia el DTO de imagen y mapea las columnas.
                    ImagenDTO dto = new ImagenDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setTipoGrafico(aValorJava(rs.getString("tipo_grafico")));
                    dto.setPath(rs.getString("ruta_archivo"));
                    dto.setDescription(rs.getString("descripcion") != null ? rs.getString("descripcion") : "");
                    dto.setPlanId(rs.getInt("plan_id"));
                    // Retorna la imagen encontrada.
                    return dto;
                }
            }
        }
        // Retorna null si no se localizó ningún registro con ese ID.
        return null;
    }

    /*
     * Qué hace (la acción): Obtiene la imagen de tipo georreferenciación (mapa) o entorno asociada de forma directa a un plan.
     * Qué significa (conceptos, métodos, tipos involucrados): Relación de cardinalidad de una sola imagen de ese tipo por plan familiar.
     * Para qué se usa (el propósito): Cargar el mapa georreferenciado o la foto de la fachada del hogar en sus pestañas de presentación directa.
     */
    public ImagenDTO obtenerPorPlanYTipo(int planId, String tipo) throws SQLException {
        // Explicación de consulta SQL:
        // - SELECT recupera las columnas de la imagen asociada al plan.
        // - WHERE plan_id = ? AND tipo_grafico = ? filtra las imágenes pertenecientes al plan familiar y tipo de gráfico.
        String sql = "SELECT id, tipo_grafico, ruta_archivo, descripcion, plan_id "
                   + "FROM imagenes WHERE plan_id = ? AND tipo_grafico = ?";
                   
        // try-with-resources: Abre la conexión JDBC y prepara el statement de forma segura.
        try (Connection con = Conexion.obtener(); // Abre la conexión física.
             PreparedStatement ps = con.prepareStatement(sql)) { // Prepara la consulta.
             
            // Vincula el ID del plan familiar.
            ps.setInt(1, planId);
            // Vincula el tipo de gráfico traducido para la base de datos.
            ps.setString(2, aValorBD(tipo));
            
            // Ejecuta la consulta de lectura.
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Instancia y realiza el mapeo al DTO.
                    ImagenDTO dto = new ImagenDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setTipoGrafico(aValorJava(rs.getString("tipo_grafico")));
                    dto.setPath(rs.getString("ruta_archivo"));
                    dto.setDescription(rs.getString("descripcion") != null ? rs.getString("descripcion") : "");
                    dto.setPlanId(rs.getInt("plan_id"));
                    // Retorna el DTO de la imagen única encontrada.
                    return dto;
                }
            }
        }
        // Retorna null si la imagen única no ha sido creada para este plan.
        return null;
    }

    /*
     * Qué hace (la acción): Registra una nueva imagen en la base de datos MySQL y retorna el identificador primario asignado.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - Statement.RETURN_GENERATED_KEYS: Habilita el retorno del ID autoincremental de la base de datos MySQL.
     * Para qué se usa (el propósito): Guardar la ruta física del croquis de evacuación o fachada subida al servidor.
     * Por qué es importante (el impacto o problema que resuelve): Permite asociar lógicamente la imagen a la ficha del plan de emergencia, retornando el ID para confirmación en el cliente.
     */
    public int crear(ImagenDTO dto) throws SQLException {
        // Explicación de consulta SQL:
        // - INSERT INTO registra un nuevo registro de imagen en imagenes.
        // - Las columnas tipo_grafico, ruta_archivo, descripcion y plan_id reciben valores mediante marcadores '?'.
        String sql = "INSERT INTO imagenes (tipo_grafico, ruta_archivo, descripcion, plan_id) VALUES (?, ?, ?, ?)";
        
        // try-with-resources: Abre la conexión y prepara el PreparedStatement con retorno de llaves autogeneradas.
        try (Connection con = Conexion.obtener(); // Abre la conexión física.
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { // Habilita la obtención del ID generado.
            
            // Vincula el tipo de gráfico traducido al primer parámetro '?'.
            ps.setString(1, aValorBD(dto.getTipoGrafico()));
            // Vincula la ruta física del archivo en el servidor al segundo parámetro '?'.
            ps.setString(2, dto.getPath());
            // Setea NULL si la descripción viene vacía, de lo contrario vincula el texto.
            ps.setString(3, dto.getDescription() != null && !dto.getDescription().isEmpty() ? dto.getDescription() : null);
            // Vincula el ID del plan de emergencia familiar al cuarto parámetro '?'.
            ps.setInt(4, dto.getPlanId());
            
            // Ejecuta la inserción en la base de datos.
            ps.executeUpdate();
            
            // Recupera la llave primaria autoincremental asignada de forma automática por MySQL.
            try (ResultSet rsKeys = ps.getGeneratedKeys()) {
                if (rsKeys.next()) {
                    // Retorna el ID autogenerado.
                    return rsKeys.getInt(1);
                }
            }
        }
        // Lanza una excepción si falla la inserción de la imagen.
        throw new SQLException("No se pudo obtener el ID autogenerado de la imagen.");
    }

    /*
     * Qué hace (la acción): Modifica la descripción textual o anotaciones de una imagen por su ID.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - UPDATE imagenes: Modifica únicamente la columna descriptiva.
     * Para qué se usa (el propósito): Guardar modificaciones de pie de foto o detalles de los croquis de vivienda.
     */
    public void actualizarDescripcion(int id, String descripcion) throws SQLException {
        // Explicación de consulta SQL:
        // - UPDATE modifica la columna descripcion de la tabla imagenes.
        // - WHERE id = ? restringe el cambio a la imagen especificada.
        String sql = "UPDATE imagenes SET descripcion = ? WHERE id = ?";
        
        // try-with-resources: Abre la conexión JDBC y prepara el statement.
        try (Connection con = Conexion.obtener(); // Abre la conexión física.
             PreparedStatement ps = con.prepareStatement(sql)) { // Prepara la actualización.
             
            // Vincula la descripción (o NULL si viene vacía) al primer parámetro.
            ps.setString(1, descripcion != null && !descripcion.isEmpty() ? descripcion : null);
            // Vincula el ID de la imagen al segundo parámetro.
            ps.setInt(2, id);
            
            // Ejecuta la actualización en MySQL.
            ps.executeUpdate();
        }
    }

    /*
     * Qué hace (la acción): Actualiza la ruta del archivo físico de una imagen (croquis único).
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - UPDATE imagenes SET ruta_archivo: Sobrescribe la ubicación del archivo.
     * Para qué se usa (el propósito): Reemplazar el archivo de imagen de mapa o fachada del hogar conservando el mismo ID de registro.
     */
    public void actualizarRuta(int id, String ruta) throws SQLException {
        // Explicación de consulta SQL:
        // - UPDATE modifica la columna ruta_archivo.
        // - WHERE id = ? restringe la actualización al registro correspondiente.
        String sql = "UPDATE imagenes SET ruta_archivo = ? WHERE id = ?";
        
        // try-with-resources: Abre la conexión a la base de datos y compila el PreparedStatement.
        try (Connection con = Conexion.obtener(); // Abre la conexión física.
             PreparedStatement ps = con.prepareStatement(sql)) { // Prepara la actualización.
             
            // Vincula la nueva ruta de archivo al primer parámetro.
            ps.setString(1, ruta);
            // Vincula el ID de la imagen al segundo parámetro.
            ps.setInt(2, id);
            
            // Ejecuta la actualización física en MySQL.
            ps.executeUpdate();
        }
    }

    /*
     * Qué hace (la acción): Elimina físicamente el registro de la imagen de la base de datos.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - DELETE FROM: Sentencia de eliminación física.
     * Para qué se usa (el propósito): Borrar croquis de evacuación o fotos de entorno descartadas por los voluntarios.
     */
    public void eliminar(int id) throws SQLException {
        // Explicación de consulta SQL:
        // - DELETE FROM elimina físicamente registros que cumplan la condición WHERE.
        // - WHERE id = ? restringe el borrado al ID exacto de la imagen.
        String sql = "DELETE FROM imagenes WHERE id = ?";
        
        // try-with-resources: Abre la conexión JDBC y prepara el statement de forma segura.
        try (Connection con = Conexion.obtener(); // Abre la conexión física.
             PreparedStatement ps = con.prepareStatement(sql)) { // Prepara el statement de borrado.
             
            // Vincula el ID de la imagen al parámetro del WHERE.
            ps.setInt(1, id);
            
            // Ejecuta la eliminación física en MySQL.
            ps.executeUpdate();
        }
    }
}
