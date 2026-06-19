package Modelo.DAO;

import Modelo.Config.Conexion;
import Modelo.DTO.ImagenDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Qué hace: DAO encargado de realizar operaciones de lectura, escritura y eliminación física en base de datos para la entidad de Imágenes (tabla imagenes).
// Por qué existe: Encapsula el acceso directo a la base de datos MySQL usando sentencias preparadas de JDBC.
// Qué problema resuelve: Separa el código de acceso a datos de la lógica de negocio, previniendo la inyección SQL, asegurando el cierre de conexiones y mapeando los tipos de gráficos.
public class ImagenDAO {

    // Qué hace: Convierte el tipo de gráfico usado en el frontend al valor enum almacenado físicamente en la base de datos.
    // Por qué existe: El DER de la base de datos utiliza el valor "mapa" en el enum, mientras que el frontend y los endpoints consumen "georeferenciacion".
    // Qué problema resuelve: Garantiza compatibilidad estricta con las restricciones del enum en MySQL sin obligar a cambiar la estructura de la base de datos.
    private String aValorBD(String tipoJava) {
        // Qué hace: Si el valor de java equivale a "georeferenciacion" (ignorando mayúsculas), lo traduce al término enum "mapa" de la base de datos.
        if ("georeferenciacion".equalsIgnoreCase(tipoJava)) {
            return "mapa";
        }
        // Qué hace: En caso contrario, retorna el mismo tipo literal de entrada.
        return tipoJava;
    }

    // Qué hace: Convierte el valor enum de la base de datos al tipo de gráfico consumido por la SPA en el frontend.
    // Por qué existe: Permite que el frontend reciba "georeferenciacion" en lugar de "mapa", manteniendo coherencia con las rutas en español.
    // Qué problema resuelve: Oculta la discrepancia del modelo físico relacional de cara a la API de presentación.
    private String aValorJava(String tipoBD) {
        // Qué hace: Si el valor de base de datos es "mapa" (ignorando mayúsculas), lo traduce a "georeferenciacion" para consumo de la UI.
        if ("mapa".equalsIgnoreCase(tipoBD)) {
            return "georeferenciacion";
        }
        // Qué hace: En caso contrario, retorna el valor del enum sin modificaciones.
        return tipoBD;
    }

    // Qué hace: Cuenta la cantidad total de imágenes registradas para un plan familiar y tipo específico.
    // Por qué existe: Suministra el total al servicio para realizar el cálculo de los metadatos de paginación de los gráficos de vivienda.
    // Qué problema resuelve: Evita transferir todas las filas por red solo para realizar el conteo de registros.
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

    // Qué hace: Consulta un listado paginado de imágenes asociadas a un plan familiar y tipo de gráfico.
    // Por qué existe: Alimenta la vista principal del listado de croquis de la vivienda en el frontend.
    // Qué problema resuelve: Permite recuperar conjuntos limitados de imágenes de forma paginada para mejorar el tiempo de carga del cliente.
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

    // Qué hace: Consulta una imagen específica a través de su identificador único ID.
    // Por qué existe: Permite alimentar la visualización modal en grande o el formulario de edición de descripción.
    // Qué problema resuelve: Recupera la información de un único registro de forma atómica y segura mediante JDBC.
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

    // Qué hace: Consulta la imagen única de entorno o georreferenciación vinculada a un plan familiar.
    // Por qué existe: Módulos de entorno y mapa son de cardinalidad 1-a-1 por plan, requiriendo recuperar el registro único sin ID de imagen.
    // Qué problema resuelve: Facilita la obtención directa de la imagen correspondiente usando únicamente el ID del plan.
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

    // Qué hace: Inserta una nueva fila de imagen en la tabla imagenes de la base de datos y retorna su ID autogenerado.
    // Por qué existe: Registra de forma definitiva la ruta física del archivo subido y su descripción en MySQL.
    // Qué problema resuelve: Mapea el objeto DTO en memoria hacia las columnas de la tabla de forma parametrizada y protegida.
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

    // Qué hace: Modifica la descripción de una imagen existente a través de su ID.
    // Por qué existe: Habilita el guardado del formulario de edición de descripción para el croquis de vivienda.
    // Qué problema resuelve: Ejecuta la actualización parcial en base de datos sin alterar la ruta del archivo.
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

    // Qué hace: Actualiza la ruta del archivo de una imagen existente en la base de datos por su ID.
    // Por qué existe: Permite cambiar el archivo físico de un gráfico único (entorno o georreferenciación) sin alterar su identificador único en base de datos.
    // Qué problema resuelve: Sobrescribe la referencia de ruta de manera segura mediante JDBC.
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

    // Qué hace: Elimina físicamente el registro de la imagen de la base de datos MySQL por su ID.
    // Por qué existe: Permite dar de baja un croquis de vivienda cargado por error.
    // Qué problema resuelve: Borra el registro en cascada o de forma directa en el motor SQL de forma atómica.
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
