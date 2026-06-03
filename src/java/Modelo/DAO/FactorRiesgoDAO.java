package Modelo.DAO;

import Modelo.Config.Conexion;
import Modelo.DTO.FactorRiesgoDTO;
import Modelo.DTO.AccionReduccionDTO;
import Modelo.DTO.FactorVulnerabilidadDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Qué hace: DAO para gestionar la persistencia en MySQL de los factores de riesgo, sus acciones de reducción y vulnerabilidades.
// Por qué existe: Separa la lógica de acceso a datos de la lógica de negocio y de los controladores, respetando MVC vanilla.
// Qué problema resuelve: Centraliza las consultas, inserciones, modificaciones y eliminaciones JDBC, previniendo inyección SQL.
public class FactorRiesgoDAO {

    // =========================================================================
    // CRUD: FACTORES DE RIESGO
    // =========================================================================

    // Qué hace: Obtiene todos los factores de riesgo registrados (para el Supervisor).
    // Por qué se implementó: Permite listar todos los riesgos del censo.
    // Qué problema resuelve: Facilita al supervisor la consulta de todos los factores cargados.
    public List<FactorRiesgoDTO> obtenerTodos() throws SQLException {
        List<FactorRiesgoDTO> lista = new ArrayList<>();
        String sql = "SELECT fr.id, fr.descripcion, fr.ubicacion, fr.distancia_metros, fr.plan_id, fr.amenaza_id, am.nombre AS amenaza_nombre " +
                     "FROM factores_riesgo fr " +
                     "LEFT JOIN amenazas am ON fr.amenaza_id = am.id";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                FactorRiesgoDTO dto = new FactorRiesgoDTO();
                dto.setId(rs.getInt("id"));
                dto.setDescription(rs.getString("descripcion"));
                dto.setUbication(rs.getString("ubicacion"));
                dto.setDistance(rs.getInt("distancia_metros"));
                dto.setFamilyPlanId(rs.getInt("plan_id"));
                dto.setThreatTypeId(rs.getInt("amenaza_id"));
                dto.setThreatTypeName(rs.getString("amenaza_nombre"));
                lista.add(dto);
            }
        }
        return lista;
    }

    // Qué hace: Obtiene los detalles de un factor de riesgo por su ID único.
    // Por qué se implementó: Permite cargar los datos de un riesgo en el modal o formulario de edición.
    // Qué problema resuelve: Recupera la información exacta de un registro para su visualización o modificación.
    public FactorRiesgoDTO obtenerPorId(int id) throws SQLException {
        String sql = "SELECT fr.id, fr.descripcion, fr.ubicacion, fr.distancia_metros, fr.plan_id, fr.amenaza_id, am.nombre AS amenaza_nombre " +
                     "FROM factores_riesgo fr " +
                     "LEFT JOIN amenazas am ON fr.amenaza_id = am.id WHERE fr.id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    FactorRiesgoDTO dto = new FactorRiesgoDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setDescription(rs.getString("descripcion"));
                    dto.setUbication(rs.getString("ubicacion"));
                    dto.setDistance(rs.getInt("distancia_metros"));
                    dto.setFamilyPlanId(rs.getInt("plan_id"));
                    dto.setThreatTypeId(rs.getInt("amenaza_id"));
                    dto.setThreatTypeName(rs.getString("amenaza_nombre"));
                    return dto;
                }
            }
        }
        return null;
    }

    // Qué hace: Obtiene todos los factores de riesgo colgados de un plan familiar.
    // Por qué se implementó: Permite listar los riesgos en la grilla visual del plan de emergencia de una familia.
    // Qué problema resuelve: Filtra y retorna los registros que pertenecen exclusivamente al plan del voluntario.
    public List<FactorRiesgoDTO> obtenerPorPlan(int planId) throws SQLException {
        List<FactorRiesgoDTO> lista = new ArrayList<>();
        String sql = "SELECT fr.id, fr.descripcion, fr.ubicacion, fr.distancia_metros, fr.plan_id, fr.amenaza_id, am.nombre AS amenaza_nombre " +
                     "FROM factores_riesgo fr " +
                     "LEFT JOIN amenazas am ON fr.amenaza_id = am.id WHERE fr.plan_id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FactorRiesgoDTO dto = new FactorRiesgoDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setDescription(rs.getString("descripcion"));
                    dto.setUbication(rs.getString("ubicacion"));
                    dto.setDistance(rs.getInt("distancia_metros"));
                    dto.setFamilyPlanId(rs.getInt("plan_id"));
                    dto.setThreatTypeId(rs.getInt("amenaza_id"));
                    dto.setThreatTypeName(rs.getString("amenaza_nombre"));
                    lista.add(dto);
                }
            }
        }
        return lista;
    }

    // Qué hace: Obtiene un subconjunto de factores de riesgo de un plan familiar aplicando paginación.
    // Por qué se implementó: Permite a la grilla de riesgos del frontend cargar por páginas.
    // Qué problema resuelve: Controla el flujo de datos optimizando el rendimiento de red.
    public List<FactorRiesgoDTO> obtenerPorPlanPaginado(int planId, int limit, int offset) throws SQLException {
        List<FactorRiesgoDTO> lista = new ArrayList<>();
        String sql = "SELECT fr.id, fr.descripcion, fr.ubicacion, fr.distancia_metros, fr.plan_id, fr.amenaza_id, am.nombre AS amenaza_nombre " +
                     "FROM factores_riesgo fr " +
                     "LEFT JOIN amenazas am ON fr.amenaza_id = am.id WHERE fr.plan_id = ? LIMIT ? OFFSET ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FactorRiesgoDTO dto = new FactorRiesgoDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setDescription(rs.getString("descripcion"));
                    dto.setUbication(rs.getString("ubicacion"));
                    dto.setDistance(rs.getInt("distancia_metros"));
                    dto.setFamilyPlanId(rs.getInt("plan_id"));
                    dto.setThreatTypeId(rs.getInt("amenaza_id"));
                    dto.setThreatTypeName(rs.getString("amenaza_nombre"));
                    lista.add(dto);
                }
            }
        }
        return lista;
    }

    // Qué hace: Obtiene la cantidad total de factores de riesgo registrados para un plan.
    // Por qué se implementó: Se requiere para calcular el total de páginas en el componente de paginación del frontend.
    // Qué problema resuelve: Suministra el total de registros de forma ligera con un COUNT.
    public int contarPorPlan(int planId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM factores_riesgo WHERE plan_id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }


    // Qué hace: Inserta un nuevo factor de riesgo en la base de datos.
    // Por qué se implementó: Permite guardar el formulario inicial de agregar riesgo de la UI.
    // Qué problema resuelve: Inserta el registro de manera parametrizada y retorna true si fue exitoso.
    public boolean crear(FactorRiesgoDTO dto) throws SQLException {
        String sql = "INSERT INTO factores_riesgo (descripcion, ubicacion, distancia_metros, plan_id, amenaza_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, dto.getDescription());
            ps.setString(2, dto.getUbication());
            ps.setInt(3, dto.getDistance());
            ps.setInt(4, dto.getFamilyPlanId());
            ps.setInt(5, dto.getThreatTypeId());
            return ps.executeUpdate() > 0;
        }
    }

    // Qué hace: Actualiza los campos de un factor de riesgo.
    // Por qué se implementó: Permite guardar las modificaciones realizadas en el acordeón "Datos del riesgo".
    // Qué problema resuelve: Ejecuta la consulta de UPDATE sobre los datos específicos del registro padre.
    public boolean actualizar(int id, FactorRiesgoDTO dto) throws SQLException {
        String sql = "UPDATE factores_riesgo SET descripcion = ?, ubicacion = ?, distancia_metros = ?, amenaza_id = ? WHERE id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, dto.getDescription());
            ps.setString(2, dto.getUbication());
            ps.setInt(3, dto.getDistance());
            ps.setInt(4, dto.getThreatTypeId());
            ps.setInt(5, id);
            return ps.executeUpdate() > 0;
        }
    }

    // Qué hace: Elimina un factor de riesgo por su ID, eliminando previamente de forma transaccional sus dependencias.
    // Por qué se implementó: Habilita el botón "Eliminar" de la UI limpiando relaciones hijas para evitar errores de llave foránea.
    // Qué problema resuelve: Ejecuta un borrado ACID en cascada.
    public boolean eliminar(int id) throws SQLException {
        Connection con = null;
        PreparedStatement psDelVul = null;
        PreparedStatement psDelAct = null;
        PreparedStatement psDelRie = null;
        try {
            con = Conexion.obtener();
            con.setAutoCommit(false);

            // 1. Eliminar relaciones en la tabla intermedia de vulnerabilidades
            String sqlVul = "DELETE FROM factor_riesgo_vulnerabilidad WHERE riesgo_id = ?";
            psDelVul = con.prepareStatement(sqlVul);
            psDelVul.setInt(1, id);
            psDelVul.executeUpdate();

            // 2. Eliminar acciones de reducción asociadas
            String sqlAct = "DELETE FROM acciones_reduccion WHERE riesgo_id = ?";
            psDelAct = con.prepareStatement(sqlAct);
            psDelAct.setInt(1, id);
            psDelAct.executeUpdate();

            // 3. Eliminar el factor de riesgo padre
            String sqlRie = "DELETE FROM factores_riesgo WHERE id = ?";
            psDelRie = con.prepareStatement(sqlRie);
            psDelRie.setInt(1, id);
            int rows = psDelRie.executeUpdate();

            con.commit();
            return rows > 0;
        } catch (SQLException e) {
            if (con != null) {
                con.rollback();
            }
            throw e;
        } finally {
            if (psDelVul != null) psDelVul.close();
            if (psDelAct != null) psDelAct.close();
            if (psDelRie != null) psDelRie.close();
            if (con != null) con.close();
        }
    }

    // =========================================================================
    // CRUD: ACCIONES DE REDUCCIÓN
    // =========================================================================

    public AccionReduccionDTO obtenerAccionPorId(int id) throws SQLException {
        String sql = "SELECT ar.id, ar.descripcion_tarea, ar.fecha_termino, ar.riesgo_id, ar.responsable_id, " +
                     "intg.nombre AS integrante_nombre, intg.apellido AS integrante_apellido " +
                     "FROM acciones_reduccion ar " +
                     "LEFT JOIN integrantes intg ON ar.responsable_id = intg.id WHERE ar.id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    AccionReduccionDTO dto = new AccionReduccionDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setAction(rs.getString("descripcion_tarea"));
                    dto.setEndDate(rs.getString("fecha_termino"));
                    dto.setRiskFactorId(rs.getInt("riesgo_id"));
                    dto.setMemberId(rs.getInt("responsable_id"));
                    dto.setMemberName(rs.getString("integrante_nombre"));
                    dto.setMemberLastNames(rs.getString("integrante_apellido"));
                    dto.setCreatedAt(rs.getString("fecha_termino")); // Fallback
                    return dto;
                }
            }
        }
        return null;
    }

    public List<AccionReduccionDTO> obtenerAccionesPorRiesgo(int riesgoId) throws SQLException {
        List<AccionReduccionDTO> lista = new ArrayList<>();
        String sql = "SELECT ar.id, ar.descripcion_tarea, ar.fecha_termino, ar.riesgo_id, ar.responsable_id, " +
                     "intg.nombre AS integrante_nombre, intg.apellido AS integrante_apellido " +
                     "FROM acciones_reduccion ar " +
                     "LEFT JOIN integrantes intg ON ar.responsable_id = intg.id WHERE ar.riesgo_id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, riesgoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AccionReduccionDTO dto = new AccionReduccionDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setAction(rs.getString("descripcion_tarea"));
                    dto.setEndDate(rs.getString("fecha_termino"));
                    dto.setRiskFactorId(rs.getInt("riesgo_id"));
                    dto.setMemberId(rs.getInt("responsable_id"));
                    dto.setMemberName(rs.getString("integrante_nombre"));
                    dto.setMemberLastNames(rs.getString("integrante_apellido"));
                    dto.setCreatedAt(rs.getString("fecha_termino")); // Fallback
                    lista.add(dto);
                }
            }
        }
        return lista;
    }

    public boolean crearAccion(AccionReduccionDTO dto) throws SQLException {
        String sql = "INSERT INTO acciones_reduccion (descripcion_tarea, fecha_termino, riesgo_id, responsable_id) VALUES (?, ?, ?, ?)";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, dto.getAction());
            ps.setString(2, dto.getEndDate());
            ps.setInt(3, dto.getRiskFactorId());
            if (dto.getMemberId() > 0) {
                ps.setInt(4, dto.getMemberId());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            return ps.executeUpdate() > 0;
        }
    }

    public boolean actualizarAccion(int id, AccionReduccionDTO dto) throws SQLException {
        String sql = "UPDATE acciones_reduccion SET descripcion_tarea = ?, fecha_termino = ?, responsable_id = ? WHERE id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, dto.getAction());
            ps.setString(2, dto.getEndDate());
            if (dto.getMemberId() > 0) {
                ps.setInt(3, dto.getMemberId());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.setInt(4, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminarAccion(int id) throws SQLException {
        String sql = "DELETE FROM acciones_reduccion WHERE id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // =========================================================================
    // CRUD: FACTORES DE VULNERABILIDAD
    // =========================================================================

    public FactorVulnerabilidadDTO obtenerVulnerabilidadPorId(int id) throws SQLException {
        String sql = "SELECT frv.id, frv.grado, frv.vulnerabilidad_personalizada, frv.riesgo_id, frv.vulnerabilidad_id, " +
                     "v.nombre AS vulnerabilidad_nombre " +
                     "FROM factor_riesgo_vulnerabilidad frv " +
                     "LEFT JOIN vulnerabilidades v ON frv.vulnerabilidad_id = v.id WHERE frv.id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    FactorVulnerabilidadDTO dto = new FactorVulnerabilidadDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setRiskFactorId(rs.getInt("riesgo_id"));
                    dto.setVulnerabilityId(rs.getInt("vulnerabilidad_id"));
                    dto.setVulnerabilityName(rs.getString("vulnerabilidad_nombre"));
                    
                    String gradeStr = rs.getString("grado");
                    dto.setVulnerabilityGradeName(gradeStr);
                    dto.setVulnerabilityGradeId(gradeStrToId(gradeStr));
                    return dto;
                }
            }
        }
        return null;
    }

    public List<FactorVulnerabilidadDTO> obtenerVulnerabilidadesPorRiesgo(int riesgoId) throws SQLException {
        List<FactorVulnerabilidadDTO> lista = new ArrayList<>();
        String sql = "SELECT frv.id, frv.grado, frv.vulnerabilidad_personalizada, frv.riesgo_id, frv.vulnerabilidad_id, " +
                     "v.nombre AS vulnerabilidad_nombre " +
                     "FROM factor_riesgo_vulnerabilidad frv " +
                     "LEFT JOIN vulnerabilidades v ON frv.vulnerabilidad_id = v.id WHERE frv.riesgo_id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, riesgoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FactorVulnerabilidadDTO dto = new FactorVulnerabilidadDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setRiskFactorId(rs.getInt("riesgo_id"));
                    dto.setVulnerabilityId(rs.getInt("vulnerabilidad_id"));
                    dto.setVulnerabilityName(rs.getString("vulnerabilidad_nombre"));
                    
                    String gradeStr = rs.getString("grado");
                    dto.setVulnerabilityGradeName(gradeStr);
                    dto.setVulnerabilityGradeId(gradeStrToId(gradeStr));
                    lista.add(dto);
                }
            }
        }
        return lista;
    }

    public boolean crearVulnerabilidad(FactorVulnerabilidadDTO dto) throws SQLException {
        String sql = "INSERT INTO factor_riesgo_vulnerabilidad (grado, riesgo_id, vulnerabilidad_id) VALUES (?, ?, ?)";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, gradeIdToStr(dto.getVulnerabilityGradeId()));
            ps.setInt(2, dto.getRiskFactorId());
            ps.setInt(3, dto.getVulnerabilityId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean actualizarVulnerabilidad(int id, FactorVulnerabilidadDTO dto) throws SQLException {
        String sql = "UPDATE factor_riesgo_vulnerabilidad SET grado = ?, vulnerabilidad_id = ? WHERE id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, gradeIdToStr(dto.getVulnerabilityGradeId()));
            ps.setInt(2, dto.getVulnerabilityId());
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminarVulnerabilidad(int id) throws SQLException {
        String sql = "DELETE FROM factor_riesgo_vulnerabilidad WHERE id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // =========================================================================
    // HELPERS DE CONVERSIÓN DE ENUM
    // =========================================================================

    private int gradeStrToId(String gradeStr) {
        if (gradeStr == null) return 3;
        switch (gradeStr) {
            case "Muy Alta": return 1;
            case "Alta": return 2;
            case "Media": return 3;
            case "Baja": return 4;
            default: return 3;
        }
    }

    private String gradeIdToStr(int gradeId) {
        switch (gradeId) {
            case 1: return "Muy Alta";
            case 2: return "Alta";
            case 3: return "Media";
            case 4: return "Baja";
            default: return "Media";
        }
    }
}
