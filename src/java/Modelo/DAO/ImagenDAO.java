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
        // - Información buscada: El total de imágenes asociadas a un plan y tipo de gráfico.
        // - Tablas participantes: imagenes.
        // - Filtros aplicados: plan_id = ? AND tipo_grafico = ?.
        String sql = "SELECT COUNT(*) AS total FROM imagenes WHERE plan_id = ? AND tipo_grafico = ?";
        // Qué hace: Abre la conexión JDBC y prepara el statement parametrizado.
        // Por qué existe: Evita inyección SQL al no concatenar variables de forma cruda.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Vincula el ID del plan de emergencia familiar al primer parámetro.
            ps.setInt(1, planId);
            // Qué hace: Asigna el valor del enum traducido para coincidir con el tipo_grafico en base de datos.
            ps.setString(2, aValorBD(tipo));
            // Qué hace: Ejecuta la consulta de conteo y procesa el ResultSet.
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Qué hace: Retorna la cantidad total de imágenes encontradas en la columna total.
                    return rs.getInt("total");
                }
            }
        }
        // Qué hace: Retorna 0 si la consulta no arrojó resultados.
        return 0;
    }

    // Qué hace: Consulta un listado paginado de imágenes asociadas a un plan familiar y tipo de gráfico.
    // Por qué existe: Alimenta la vista principal del listado de croquis de la vivienda en el frontend.
    // Qué problema resuelve: Permite recuperar conjuntos limitados de imágenes de forma paginada para mejorar el tiempo de carga del cliente.
    public List<ImagenDTO> listarPorPlanYTipo(int planId, String tipo, int limit, int offset) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Identificador, tipo de gráfico, ruta del archivo y descripción de las imágenes del plan.
        // - Tablas participantes: imagenes.
        // - Filtros aplicados: plan_id = ? AND tipo_grafico = ?, paginados ascendentemente/descendentemente con LIMIT ? OFFSET ?.
        String sql = "SELECT id, tipo_grafico, ruta_archivo, descripcion, plan_id "
                   + "FROM imagenes WHERE plan_id = ? AND tipo_grafico = ? "
                   + "ORDER BY id DESC LIMIT ? OFFSET ?";
        
        // Qué hace: Abre la conexión a base de datos y compila el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Vincula los parámetros del plan, tipo de gráfico traducido, límite y offset.
            ps.setInt(1, planId);
            ps.setString(2, aValorBD(tipo));
            ps.setInt(3, limit);
            ps.setInt(4, offset);
            
            // Qué hace: Inicializa la lista dinámica que contendrá las imágenes.
            List<ImagenDTO> lista = new ArrayList<>();
            // Qué hace: Ejecuta el query de lectura y procesa el ResultSet.
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Qué hace: Instancia el DTO de imagen y mapea cada columna.
                    ImagenDTO dto = new ImagenDTO();
                    dto.setId(rs.getInt("id"));
                    // Qué hace: Traduce la cadena enum al formato que el frontend espera mediante aValorJava.
                    dto.setTipoGrafico(aValorJava(rs.getString("tipo_grafico")));
                    dto.setPath(rs.getString("ruta_archivo"));
                    // Qué hace: Maneja nulos en el campo descripción de la imagen.
                    dto.setDescription(rs.getString("descripcion") != null ? rs.getString("descripcion") : "");
                    dto.setPlanId(rs.getInt("plan_id"));
                    // Qué hace: Agrega el DTO a la lista de retorno.
                    lista.add(dto);
                }
            }
            // Qué hace: Retorna la lista resultante de imágenes.
            return lista;
        }
    }

    // Qué hace: Consulta una imagen específica a través de su identificador único ID.
    // Por qué existe: Permite alimentar la visualización modal en grande o el formulario de edición de descripción.
    // Qué problema resuelve: Recupera la información de un único registro de forma atómica y segura mediante JDBC.
    public ImagenDTO obtenerPorId(int id) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Detalles del registro de imagen.
        // - Tablas participantes: imagenes.
        // - Filtros aplicados: id = ? (id de la imagen).
        String sql = "SELECT id, tipo_grafico, ruta_archivo, descripcion, plan_id FROM imagenes WHERE id = ?";
        // Qué hace: Abre la conexión a la base de datos y compila el PreparedStatement.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Asigna el ID de la imagen al statement.
            ps.setInt(1, id);
            // Qué hace: Ejecuta el query y lee la fila única resultante.
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Qué hace: Instancia el DTO de imagen y mapea cada columna del ResultSet.
                    ImagenDTO dto = new ImagenDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setTipoGrafico(aValorJava(rs.getString("tipo_grafico")));
                    dto.setPath(rs.getString("ruta_archivo"));
                    dto.setDescription(rs.getString("descripcion") != null ? rs.getString("descripcion") : "");
                    dto.setPlanId(rs.getInt("plan_id"));
                    // Qué hace: Retorna la imagen encontrada.
                    return dto;
                }
            }
        }
        // Qué hace: Retorna null si la imagen no existe.
        return null;
    }

    // Qué hace: Consulta la imagen única de entorno o georreferenciación vinculada a un plan familiar.
    // Por qué existe: Módulos de entorno y mapa son de cardinalidad 1-a-1 por plan, requiriendo recuperar el registro único sin ID de imagen.
    // Qué problema resuelve: Facilita la obtención directa de la imagen correspondiente usando únicamente el ID del plan.
    public ImagenDTO obtenerPorPlanYTipo(int planId, String tipo) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Columnas de la imagen asociada al plan y tipo.
        // - Tablas participantes: imagenes.
        // - Filtros aplicados: plan_id = ? AND tipo_grafico = ?.
        String sql = "SELECT id, tipo_grafico, ruta_archivo, descripcion, plan_id "
                   + "FROM imagenes WHERE plan_id = ? AND tipo_grafico = ?";
        // Qué hace: Abre la conexión JDBC y prepara el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Vincula el ID del plan y el tipo de gráfico traducido.
            ps.setInt(1, planId);
            ps.setString(2, aValorBD(tipo));
            // Qué hace: Ejecuta la consulta y lee el ResultSet.
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Qué hace: Instancia el DTO y realiza el mapeo.
                    ImagenDTO dto = new ImagenDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setTipoGrafico(aValorJava(rs.getString("tipo_grafico")));
                    dto.setPath(rs.getString("ruta_archivo"));
                    dto.setDescription(rs.getString("descripcion") != null ? rs.getString("descripcion") : "");
                    dto.setPlanId(rs.getInt("plan_id"));
                    // Qué hace: Retorna el DTO de la imagen encontrada.
                    return dto;
                }
            }
        }
        // Qué hace: Retorna null si no se localizó la imagen.
        return null;
    }

    // Qué hace: Inserta una nueva fila de imagen en la tabla imagenes de la base de datos y retorna su ID autogenerado.
    // Por qué existe: Registra de forma definitiva la ruta física del archivo subido y su descripción en MySQL.
    // Qué problema resuelve: Mapea el objeto DTO en memoria hacia las columnas de la tabla de forma parametrizada y protegida.
    public int crear(ImagenDTO dto) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Registrar una nueva imagen.
        // - Tablas participantes: imagenes.
        String sql = "INSERT INTO imagenes (tipo_grafico, ruta_archivo, descripcion, plan_id) VALUES (?, ?, ?, ?)";
        // Qué hace: Abre la conexión a base de datos y prepara el PreparedStatement con retorno de llaves generadas.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            // Qué hace: Vincula los parámetros básicos de la imagen.
            ps.setString(1, aValorBD(dto.getTipoGrafico()));
            ps.setString(2, dto.getPath());
            // Qué hace: Setea NULL si la descripción viene vacía.
            ps.setString(3, dto.getDescription() != null && !dto.getDescription().isEmpty() ? dto.getDescription() : null);
            ps.setInt(4, dto.getPlanId());
            
            // Qué hace: Ejecuta la inserción.
            ps.executeUpdate();
            
            // Qué hace: Recupera la llave autoincremental generada por MySQL.
            try (ResultSet rsKeys = ps.getGeneratedKeys()) {
                if (rsKeys.next()) {
                    // Qué hace: Retorna el ID autogenerado.
                    return rsKeys.getInt(1);
                }
            }
        }
        // Qué hace: Lanza una excepción si falla la inserción de la imagen.
        throw new SQLException("No se pudo obtener el ID autogenerado de la imagen.");
    }

    // Qué hace: Modifica la descripción de una imagen existente a través de su ID.
    // Por qué existe: Habilita el guardado del formulario de edición de descripción para el croquis de vivienda.
    // Qué problema resuelve: Ejecuta la actualización parcial en base de datos sin alterar la ruta del archivo.
    public void actualizarDescripcion(int id, String descripcion) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Modificar la columna descripción.
        // - Tablas participantes: imagenes.
        // - Filtros aplicados: WHERE id = ?.
        String sql = "UPDATE imagenes SET descripcion = ? WHERE id = ?";
        // Qué hace: Abre la conexión JDBC y prepara el PreparedStatement.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Vincula la descripción (o NULL si viene vacía) y el ID de la imagen.
            ps.setString(1, descripcion != null && !descripcion.isEmpty() ? descripcion : null);
            ps.setInt(2, id);
            // Qué hace: Ejecuta la actualización.
            ps.executeUpdate();
        }
    }

    // Qué hace: Actualiza la ruta del archivo de una imagen existente en la base de datos por su ID.
    // Por qué existe: Permite cambiar el archivo físico de un gráfico único (entorno o georreferenciación) sin alterar su identificador único en base de datos.
    // Qué problema resuelve: Sobrescribe la referencia de ruta de manera segura mediante JDBC.
    public void actualizarRuta(int id, String ruta) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Modificar la columna ruta_archivo.
        // - Tablas participantes: imagenes.
        // - Filtros aplicados: WHERE id = ?.
        String sql = "UPDATE imagenes SET ruta_archivo = ? WHERE id = ?";
        // Qué hace: Abre la conexión a la base de datos y compila el PreparedStatement.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Vincula la nueva ruta de archivo y el ID de imagen.
            ps.setString(1, ruta);
            ps.setInt(2, id);
            // Qué hace: Ejecuta la actualización en MySQL.
            ps.executeUpdate();
        }
    }

    // Qué hace: Elimina físicamente el registro de la imagen de la base de datos MySQL por su ID.
    // Por qué existe: Permite dar de baja un croquis de vivienda cargado por error.
    // Qué problema resuelve: Borra el registro en cascada o de forma directa en el motor SQL de forma atómica.
    public void eliminar(int id) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Eliminar el registro de imagen.
        // - Tablas participantes: imagenes.
        // - Filtros aplicados: WHERE id = ?.
        String sql = "DELETE FROM imagenes WHERE id = ?";
        // Qué hace: Abre la conexión JDBC y prepara el PreparedStatement.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Vincula el ID de imagen y ejecuta la eliminación física.
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
