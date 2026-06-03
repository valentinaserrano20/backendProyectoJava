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
        if ("georeferenciacion".equalsIgnoreCase(tipoJava)) {
            return "mapa";
        }
        return tipoJava;
    }

    // Qué hace: Convierte el valor enum de la base de datos al tipo de gráfico consumido por la SPA en el frontend.
    // Por qué existe: Permite que el frontend reciba "georeferenciacion" en lugar de "mapa", manteniendo coherencia con las rutas en español.
    // Qué problema resuelve: Oculta la discrepancia del modelo físico relacional de cara a la API de presentación.
    private String aValorJava(String tipoBD) {
        if ("mapa".equalsIgnoreCase(tipoBD)) {
            return "georeferenciacion";
        }
        return tipoBD;
    }

    // Qué hace: Cuenta la cantidad total de imágenes registradas para un plan familiar y tipo específico.
    // Por qué existe: Suministra el total al servicio para realizar el cálculo de los metadatos de paginación de los gráficos de vivienda.
    // Qué problema resuelve: Evita transferir todas las filas por red solo para realizar el conteo de registros.
    public int contarPorPlanYTipo(int planId, String tipo) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM imagenes WHERE plan_id = ? AND tipo_grafico = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planId);
            ps.setString(2, aValorBD(tipo));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        }
        return 0;
    }

    // Qué hace: Consulta un listado paginado de imágenes asociadas a un plan familiar y tipo de gráfico.
    // Por qué existe: Alimenta la vista principal del listado de croquis de la vivienda en el frontend.
    // Qué problema resuelve: Permite recuperar conjuntos limitados de imágenes de forma paginada para mejorar el tiempo de carga del cliente.
    public List<ImagenDTO> listarPorPlanYTipo(int planId, String tipo, int limit, int offset) throws SQLException {
        String sql = "SELECT id, tipo_grafico, ruta_archivo, descripcion, plan_id "
                   + "FROM imagenes WHERE plan_id = ? AND tipo_grafico = ? "
                   + "ORDER BY id DESC LIMIT ? OFFSET ?";
        
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planId);
            ps.setString(2, aValorBD(tipo));
            ps.setInt(3, limit);
            ps.setInt(4, offset);
            
            List<ImagenDTO> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ImagenDTO dto = new ImagenDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setTipoGrafico(aValorJava(rs.getString("tipo_grafico")));
                    dto.setPath(rs.getString("ruta_archivo"));
                    dto.setDescription(rs.getString("descripcion") != null ? rs.getString("descripcion") : "");
                    dto.setPlanId(rs.getInt("plan_id"));
                    lista.add(dto);
                }
            }
            return lista;
        }
    }

    // Qué hace: Consulta una imagen específica a través de su identificador único ID.
    // Por qué existe: Permite alimentar la visualización modal en grande o el formulario de edición de descripción.
    // Qué problema resuelve: Recupera la información de un único registro de forma atómica y segura mediante JDBC.
    public ImagenDTO obtenerPorId(int id) throws SQLException {
        String sql = "SELECT id, tipo_grafico, ruta_archivo, descripcion, plan_id FROM imagenes WHERE id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ImagenDTO dto = new ImagenDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setTipoGrafico(aValorJava(rs.getString("tipo_grafico")));
                    dto.setPath(rs.getString("ruta_archivo"));
                    dto.setDescription(rs.getString("descripcion") != null ? rs.getString("descripcion") : "");
                    dto.setPlanId(rs.getInt("plan_id"));
                    return dto;
                }
            }
        }
        return null;
    }

    // Qué hace: Consulta la imagen única de entorno o georreferenciación vinculada a un plan familiar.
    // Por qué existe: Módulos de entorno y mapa son de cardinalidad 1-a-1 por plan, requiriendo recuperar el registro único sin ID de imagen.
    // Qué problema resuelve: Facilita la obtención directa de la imagen correspondiente usando únicamente el ID del plan.
    public ImagenDTO obtenerPorPlanYTipo(int planId, String tipo) throws SQLException {
        String sql = "SELECT id, tipo_grafico, ruta_archivo, descripcion, plan_id "
                   + "FROM imagenes WHERE plan_id = ? AND tipo_grafico = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planId);
            ps.setString(2, aValorBD(tipo));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ImagenDTO dto = new ImagenDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setTipoGrafico(aValorJava(rs.getString("tipo_grafico")));
                    dto.setPath(rs.getString("ruta_archivo"));
                    dto.setDescription(rs.getString("descripcion") != null ? rs.getString("descripcion") : "");
                    dto.setPlanId(rs.getInt("plan_id"));
                    return dto;
                }
            }
        }
        return null;
    }

    // Qué hace: Inserta una nueva fila de imagen en la tabla imagenes de la base de datos y retorna su ID autogenerado.
    // Por qué existe: Registra de forma definitiva la ruta física del archivo subido y su descripción en MySQL.
    // Qué problema resuelve: Mapea el objeto DTO en memoria hacia las columnas de la tabla de forma parametrizada y protegida.
    public int crear(ImagenDTO dto) throws SQLException {
        String sql = "INSERT INTO imagenes (tipo_grafico, ruta_archivo, descripcion, plan_id) VALUES (?, ?, ?, ?)";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setString(1, aValorBD(dto.getTipoGrafico()));
            ps.setString(2, dto.getPath());
            ps.setString(3, dto.getDescription() != null && !dto.getDescription().isEmpty() ? dto.getDescription() : null);
            ps.setInt(4, dto.getPlanId());
            
            ps.executeUpdate();
            
            try (ResultSet rsKeys = ps.getGeneratedKeys()) {
                if (rsKeys.next()) {
                    return rsKeys.getInt(1);
                }
            }
        }
        throw new SQLException("No se pudo obtener el ID autogenerado de la imagen.");
    }

    // Qué hace: Modifica la descripción de una imagen existente a través de su ID.
    // Por qué existe: Habilita el guardado del formulario de edición de descripción para el croquis de vivienda.
    // Qué problema resuelve: Ejecuta la actualización parcial en base de datos sin alterar la ruta del archivo.
    public void actualizarDescripcion(int id, String descripcion) throws SQLException {
        String sql = "UPDATE imagenes SET descripcion = ? WHERE id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, descripcion != null && !descripcion.isEmpty() ? descripcion : null);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    // Qué hace: Actualiza la ruta del archivo de una imagen existente en la base de datos por su ID.
    // Por qué existe: Permite cambiar el archivo físico de un gráfico único (entorno o georreferenciación) sin alterar su identificador único en base de datos.
    // Qué problema resuelve: Sobrescribe la referencia de ruta de manera segura mediante JDBC.
    public void actualizarRuta(int id, String ruta) throws SQLException {
        String sql = "UPDATE imagenes SET ruta_archivo = ? WHERE id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ruta);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    // Qué hace: Elimina físicamente el registro de la imagen de la base de datos MySQL por su ID.
    // Por qué existe: Permite dar de baja un croquis de vivienda cargado por error.
    // Qué problema resuelve: Borra el registro en cascada o de forma directa en el motor SQL de forma atómica.
    public void eliminar(int id) throws SQLException {
        String sql = "DELETE FROM imagenes WHERE id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
