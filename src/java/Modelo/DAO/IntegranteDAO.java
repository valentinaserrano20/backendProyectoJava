package Modelo.DAO;

import Modelo.Config.Conexion;
import Modelo.DTO.IntegranteDTO;
import Modelo.DTO.AfeccionDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Qué hace: Clase DAO encargada de encapsular el acceso y la persistencia de datos para las entidades de Integrantes, Afecciones y Medicamentos.
// Por qué existe: Provee una capa limpia de persistencia utilizando consultas SQL parametrizadas directas mediante JDBC.
// Qué problema resuelve: Separa la lógica de acceso a base de datos de la lógica de negocio y presentación, evitando acoplamientos y previniendo la inyección SQL.
public class IntegranteDAO {

    // Qué hace: Consulta y retorna la cantidad total de integrantes registrados para un plan familiar específico.
    // Por qué existe: Es requerido para calcular los metadatos de paginación infinita/clásica expuestos en la UI.
    // Qué problema resuelve: Evita transferir todas las filas en memoria solo para contar los registros, optimizando el rendimiento del servidor.
    public int obtenerTotalIntegrantes(int planId) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM integrantes WHERE plan_id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        }
        return 0;
    }

    // Qué hace: Verifica si un plan familiar ya cuenta con un Jefe de hogar (parentesco_id = 1) registrado.
    // Por qué existe: Garantiza la regla de negocio que restringe a un único Jefe de hogar por familia.
    // Qué problema resuelve: Impide la existencia de duplicidades de cabeza de hogar dentro de la base de datos para un mismo plan.
    public boolean tieneJefeHogar(int planId, int excluirIntegranteId) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM integrantes WHERE plan_id = ? AND parentesco_id = 1 AND id != ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planId);
            ps.setInt(2, excluirIntegranteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total") > 0;
                }
            }
        }
        return false;
    }


    // Qué hace: Consulta un listado paginado de integrantes asociados a un plan, uniendo parentescos y grupos sanguíneos.
    // Por qué existe: Permite renderizar las tarjetas visuales de integrantes en el frontend de forma dosificada.
    // Qué problema resuelve: Limita y dosifica el número de registros cargados en una sola petición, reduciendo el consumo de red y memoria.
    public List<IntegranteDTO> listarIntegrantes(int planId, int limit, int offset) throws SQLException {
        String sql = "SELECT i.id, i.nombre, i.apellido, i.numero_documento, i.fecha_nacimiento, i.celular, "
                   + "p.nombre AS kinship, g.nombre AS blood_group, pf.estado_id AS status_id "
                   + "FROM integrantes i "
                   + "LEFT JOIN parentescos p ON i.parentesco_id = p.id "
                   + "LEFT JOIN grupos_sanguineos g ON i.grupo_sanguineo_id = g.id "
                   + "LEFT JOIN planes_familiares pf ON i.plan_id = pf.id "
                   + "WHERE i.plan_id = ? "
                   + "LIMIT ? OFFSET ?";
        
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            
            List<IntegranteDTO> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    IntegranteDTO dto = new IntegranteDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setNames(rs.getString("nombre"));
                    dto.setLastNames(rs.getString("apellido"));
                    dto.setDocumentTypeAcronym("");
                    dto.setDocumentNumber(rs.getString("numero_documento") != null ? rs.getString("numero_documento") : "No registrado");
                    dto.setPhone(rs.getString("celular") != null ? rs.getString("celular") : "No registrado");
                    dto.setKinshipName(rs.getString("kinship") != null ? rs.getString("kinship") : "No registrado");
                    dto.setBloodGroupName(rs.getString("blood_group") != null ? rs.getString("blood_group") : "No registrado");
                    dto.setBirthDate(rs.getString("fecha_nacimiento"));
                    dto.setStatusId(rs.getInt("status_id"));
                    lista.add(dto);
                }
            }
            return lista;
        }
    }

    // Qué hace: Obtiene la información detallada completa de un integrante familiar, incluyendo tipo de documento, género, parentesco, sangre y nacionalidad.
    // Por qué existe: Se utiliza para alimentar el modal de visualización de detalles completos ("Ver más") y la pantalla de edición de integrantes.
    // Qué problema resuelve: Permite recuperar todos los datos relacionales de un integrante de forma atómica en una sola consulta de unión (JOIN).
    public IntegranteDTO obtenerIntegrante(int id) throws SQLException {
        String sql = "SELECT i.id, i.nombre, i.apellido, i.numero_documento, i.fecha_nacimiento, i.eps, i.celular, i.es_jefe_hogar, "
                   + "i.plan_id, i.tipo_documento_id, i.parentesco_id, i.grupo_sanguineo_id, i.nacionalidad_id, i.genero_id, "
                   + "td.sigla AS document_type_acronym, gen.nombre AS gender_name, "
                   + "p.nombre AS kinship_name, g.nombre AS blood_group_name, n.nombre AS nationality_name "
                   + "FROM integrantes i "
                   + "LEFT JOIN tipo_documentos td ON i.tipo_documento_id = td.id "
                   + "LEFT JOIN generos gen ON i.genero_id = gen.id "
                   + "LEFT JOIN parentescos p ON i.parentesco_id = p.id "
                   + "LEFT JOIN grupos_sanguineos g ON i.grupo_sanguineo_id = g.id "
                   + "LEFT JOIN nacionalidades n ON i.nacionalidad_id = n.id "
                   + "WHERE i.id = ?";
        
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    IntegranteDTO dto = new IntegranteDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setNames(rs.getString("nombre"));
                    dto.setLastNames(rs.getString("apellido"));
                    dto.setDocumentNumber(rs.getString("numero_documento"));
                    dto.setBirthDate(rs.getString("fecha_nacimiento"));
                    dto.setEps(rs.getString("eps"));
                    dto.setPhone(rs.getString("celular"));
                    dto.setEsJefeHogar(rs.getBoolean("es_jefe_hogar"));
                    dto.setPlanId(rs.getInt("plan_id"));
                    dto.setDocumentTypeId(rs.getInt("tipo_documento_id"));
                    dto.setKinshipId(rs.getInt("parentesco_id"));
                    dto.setBloodGroupId(rs.getInt("grupo_sanguineo_id"));
                    dto.setNationalityId(rs.getInt("nacionalidad_id"));
                    dto.setGenderId(rs.getInt("genero_id"));
                    
                    dto.setDocumentTypeAcronym(rs.getString("document_type_acronym") != null ? rs.getString("document_type_acronym") : "");
                    dto.setGenderName(rs.getString("gender_name") != null ? rs.getString("gender_name") : "");
                    dto.setKinshipName(rs.getString("kinship_name") != null ? rs.getString("kinship_name") : "");
                    dto.setBloodGroupName(rs.getString("blood_group_name") != null ? rs.getString("blood_group_name") : "");
                    dto.setNationalityName(rs.getString("nationality_name") != null ? rs.getString("nationality_name") : "");
                    return dto;
                }
            }
        }
        return null;
    }

    // Qué hace: Inserta un nuevo integrante en la base de datos y retorna el ID autogenerado, verificando si es el jefe de hogar.
    // Por qué existe: Permite agregar nuevos miembros a la familia dentro del flujo del plan familiar.
    // Qué problema resuelve: Registra al integrante con todas sus relaciones externas correspondientes, manejando nulos de forma correcta.
    public int crearIntegrante(IntegranteDTO dto) throws SQLException {
        String sql = "INSERT INTO integrantes (nombre, apellido, numero_documento, fecha_nacimiento, eps, celular, es_jefe_hogar, "
                   + "plan_id, tipo_documento_id, parentesco_id, grupo_sanguineo_id, nacionalidad_id, genero_id) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setString(1, dto.getNames());
            ps.setString(2, dto.getLastNames());
            ps.setString(3, dto.getDocumentNumber() != null && !dto.getDocumentNumber().isEmpty() ? dto.getDocumentNumber() : null);
            ps.setDate(4, Date.valueOf(dto.getBirthDate()));
            ps.setString(5, dto.getEps() != null && !dto.getEps().isEmpty() ? dto.getEps() : null);
            ps.setString(6, dto.getPhone() != null && !dto.getPhone().isEmpty() ? dto.getPhone() : null);
            ps.setBoolean(7, dto.getKinshipId() == 1);
            ps.setInt(8, dto.getPlanId());
            
            setNullableInt(ps, 9, dto.getDocumentTypeId());
            setNullableInt(ps, 10, dto.getKinshipId());
            setNullableInt(ps, 11, dto.getBloodGroupId());
            setNullableInt(ps, 12, dto.getNationalityId());
            setNullableInt(ps, 13, dto.getGenderId());
            
            ps.executeUpdate();
            
            try (ResultSet rsKeys = ps.getGeneratedKeys()) {
                if (rsKeys.next()) {
                    return rsKeys.getInt(1);
                }
            }
        }
        throw new SQLException("Error al recuperar el ID generado para el integrante.");
    }

    // Qué hace: Modifica los datos personales y médicos generales de un integrante en la base de datos.
    // Por qué existe: Permite persistir los cambios hechos por el voluntario en el formulario de edición de integrante.
    // Qué problema resuelve: Actualiza los campos opcionales y obligatorios controlando la consistencia del jefe de hogar.
    public void actualizarIntegrante(int id, IntegranteDTO dto) throws SQLException {
        String sql = "UPDATE integrantes SET nombre = ?, apellido = ?, numero_documento = ?, fecha_nacimiento = ?, "
                   + "eps = ?, celular = ?, es_jefe_hogar = ?, tipo_documento_id = ?, parentesco_id = ?, "
                   + "grupo_sanguineo_id = ?, nacionalidad_id = ?, genero_id = ? "
                   + "WHERE id = ?";
        
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, dto.getNames());
            ps.setString(2, dto.getLastNames());
            ps.setString(3, dto.getDocumentNumber() != null && !dto.getDocumentNumber().isEmpty() ? dto.getDocumentNumber() : null);
            ps.setDate(4, Date.valueOf(dto.getBirthDate()));
            ps.setString(5, dto.getEps() != null && !dto.getEps().isEmpty() ? dto.getEps() : null);
            ps.setString(6, dto.getPhone() != null && !dto.getPhone().isEmpty() ? dto.getPhone() : null);
            ps.setBoolean(7, dto.getKinshipId() == 1);
            
            setNullableInt(ps, 8, dto.getDocumentTypeId());
            setNullableInt(ps, 9, dto.getKinshipId());
            setNullableInt(ps, 10, dto.getBloodGroupId());
            setNullableInt(ps, 11, dto.getNationalityId());
            setNullableInt(ps, 12, dto.getGenderId());
            
            ps.setInt(13, id);
            
            ps.executeUpdate();
        }
    }

    // Qué hace: Elimina transaccionalmente un integrante familiar, eliminando primero sus medicamentos y afecciones en cadena.
    // Por qué existe: Previene la violación de restricciones de llave foránea (Foreign Key Constraints) de MySQL durante la baja física de un miembro.
    // Qué problema resuelve: Ejecuta todo el flujo de borrado bajo rollback manual, manteniendo la integridad referencial en caso de error.
    public void eliminarIntegrante(int id) throws SQLException {
        Connection con = null;
        try {
            con = Conexion.obtener();
            con.setAutoCommit(false);
            
            List<Integer> afeccionesIds = new ArrayList<>();
            String sqlGetAfecciones = "SELECT id FROM afecciones WHERE integrante_id = ?";
            try (PreparedStatement psGet = con.prepareStatement(sqlGetAfecciones)) {
                psGet.setInt(1, id);
                try (ResultSet rs = psGet.executeQuery()) {
                    while (rs.next()) {
                        afeccionesIds.add(rs.getInt("id"));
                    }
                }
            }
            
            if (!afeccionesIds.isEmpty()) {
                String sqlDelMeds = "DELETE FROM medicamentos WHERE afeccion_id = ?";
                try (PreparedStatement psDelMeds = con.prepareStatement(sqlDelMeds)) {
                    for (int affId : afeccionesIds) {
                        psDelMeds.setInt(1, affId);
                        psDelMeds.executeUpdate();
                    }
                }
            }
            
            String sqlDelAfecciones = "DELETE FROM afecciones WHERE integrante_id = ?";
            try (PreparedStatement psDelAff = con.prepareStatement(sqlDelAfecciones)) {
                psDelAff.setInt(1, id);
                psDelAff.executeUpdate();
            }
            
            String sqlDelMember = "DELETE FROM integrantes WHERE id = ?";
            try (PreparedStatement psDelMem = con.prepareStatement(sqlDelMember)) {
                psDelMem.setInt(1, id);
                psDelMem.executeUpdate();
            }
            
            con.commit();
        } catch (SQLException e) {
            if (con != null) {
                con.rollback();
            }
            throw e;
        } finally {
            if (con != null) {
                con.close();
            }
        }
    }

    // Qué hace: Recupera las afecciones médicas sufridas por un integrante familiar, uniendo los datos de dosificación de medicamentos.
    // Por qué existe: Es invocado por la UI para desplegar el listado de padecimientos médicos en la pestaña de gestión del integrante.
    // Qué problema resuelve: Combina registros médicos de afecciones y medicamentos de forma atómica en un único resultado unificado.
    public List<AfeccionDTO> listarAfeccionesPorIntegrante(int memberId) throws SQLException {
        String sql = "SELECT a.id, a.tipo, a.nombre_afeccion, a.integrante_id, m.dosis_diaria "
                   + "FROM afecciones a "
                   + "LEFT JOIN medicamentos m ON a.id = m.afeccion_id "
                   + "WHERE a.integrante_id = ?";
        
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            
            List<AfeccionDTO> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AfeccionDTO dto = new AfeccionDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setMemberId(rs.getInt("integrante_id"));
                    dto.setName(rs.getString("nombre_afeccion"));
                    dto.setDose(rs.getString("dosis_diaria"));
                    
                    String tipoStr = rs.getString("tipo");
                    int typeId = 1;
                    String typeName = "Enfermedad";
                    if (tipoStr != null) {
                        if (tipoStr.equals("discapacidad")) {
                            typeId = 2;
                            typeName = "Discapacidad";
                        } else if (tipoStr.equals("alergia")) {
                            typeId = 3;
                            typeName = "Alergia";
                        }
                    }
                    dto.setConditionTypeId(typeId);
                    dto.setConditionTypeName(typeName);
                    lista.add(dto);
                }
            }
            return lista;
        }
    }

    // Qué hace: Obtiene la información estructurada de una afección en particular y su dosis relacionada.
    // Por qué existe: Se utiliza para precargar la información médica en el modal de SweetAlert de edición de afección.
    // Qué problema resuelve: Permite recuperar la dosis de medicamento asociada al diagnóstico específico de manera directa.
    public AfeccionDTO obtenerAfeccion(int id) throws SQLException {
        String sql = "SELECT a.id, a.tipo, a.nombre_afeccion, a.integrante_id, m.dosis_diaria "
                   + "FROM afecciones a "
                   + "LEFT JOIN medicamentos m ON a.id = m.afeccion_id "
                   + "WHERE a.id = ?";
        
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    AfeccionDTO dto = new AfeccionDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setMemberId(rs.getInt("integrante_id"));
                    dto.setName(rs.getString("nombre_afeccion"));
                    dto.setDose(rs.getString("dosis_diaria"));
                    
                    String tipoStr = rs.getString("tipo");
                    int typeId = 1;
                    String typeName = "Enfermedad";
                    if (tipoStr != null) {
                        if (tipoStr.equals("discapacidad")) {
                            typeId = 2;
                            typeName = "Discapacidad";
                        } else if (tipoStr.equals("alergia")) {
                            typeId = 3;
                            typeName = "Alergia";
                        }
                    }
                    dto.setConditionTypeId(typeId);
                    dto.setConditionTypeName(typeName);
                    return dto;
                }
            }
        }
        return null;
    }

    // Qué hace: Registra transaccionalmente una afección y, en caso de incluir dosis, inserta automáticamente un medicamento satélite.
    // Por qué existe: Mapea la recolección simplificada de la UI (nombre y dosis) hacia el esquema relacional estructurado.
    // Qué problema resuelve: Inserta de manera segura en dos tablas distintas en una sola transacción, garantizando la consistencia de datos.
    public int crearAfeccion(AfeccionDTO dto) throws SQLException {
        Connection con = null;
        try {
            con = Conexion.obtener();
            con.setAutoCommit(false);
            
            String sqlAff = "INSERT INTO afecciones (tipo, nombre_afeccion, integrante_id) VALUES (?, ?, ?)";
            int affId = -1;
            
            try (PreparedStatement psAff = con.prepareStatement(sqlAff, Statement.RETURN_GENERATED_KEYS)) {
                String tipoEnum = "enfermedad";
                if (dto.getConditionTypeId() == 2) tipoEnum = "discapacidad";
                else if (dto.getConditionTypeId() == 3) tipoEnum = "alergia";
                
                psAff.setString(1, tipoEnum);
                psAff.setString(2, dto.getName());
                psAff.setInt(3, dto.getMemberId());
                psAff.executeUpdate();
                
                try (ResultSet rsKeys = psAff.getGeneratedKeys()) {
                    if (rsKeys.next()) {
                        affId = rsKeys.getInt(1);
                    }
                }
            }
            
            if (affId == -1) {
                throw new SQLException("No se pudo obtener el ID autogenerado de la afección.");
            }
            
            if (dto.getDose() != null && !dto.getDose().trim().isEmpty()) {
                String sqlMed = "INSERT INTO medicamentos (nombre_droga, dosis_diaria, afeccion_id) VALUES (?, ?, ?)";
                try (PreparedStatement psMed = con.prepareStatement(sqlMed)) {
                    psMed.setString(1, "Tratamiento");
                    psMed.setString(2, dto.getDose());
                    psMed.setInt(3, affId);
                    psMed.executeUpdate();
                }
            }
            
            con.commit();
            return affId;
        } catch (SQLException e) {
            if (con != null) con.rollback();
            throw e;
        } finally {
            if (con != null) con.close();
        }
    }

    // Qué hace: Modifica una afección y actualiza, inserta o remueve su dosificación correspondiente en la tabla medicamentos.
    // Por qué existe: Mantiene actualizados los cambios médicos del integrante, administrando la existencia opcional de medicamentos.
    // Qué problema resuelve: Evalúa transaccionalmente la existencia previa de la dosis para determinar si corresponde UPDATE, INSERT o DELETE.
    public void actualizarAfeccion(int id, AfeccionDTO dto) throws SQLException {
        Connection con = null;
        try {
            con = Conexion.obtener();
            con.setAutoCommit(false);
            
            String sqlAff = "UPDATE afecciones SET tipo = ?, nombre_afeccion = ? WHERE id = ?";
            try (PreparedStatement psAff = con.prepareStatement(sqlAff)) {
                String tipoEnum = "enfermedad";
                if (dto.getConditionTypeId() == 2) tipoEnum = "discapacidad";
                else if (dto.getConditionTypeId() == 3) tipoEnum = "alergia";
                
                psAff.setString(1, tipoEnum);
                psAff.setString(2, dto.getName());
                psAff.setInt(3, id);
                psAff.executeUpdate();
            }
            
            boolean existeMed = false;
            String sqlCheckMed = "SELECT id FROM medicamentos WHERE afeccion_id = ?";
            try (PreparedStatement psCheck = con.prepareStatement(sqlCheckMed)) {
                psCheck.setInt(1, id);
                try (ResultSet rs = psCheck.executeQuery()) {
                    existeMed = rs.next();
                }
            }
            
            if (dto.getDose() != null && !dto.getDose().trim().isEmpty()) {
                if (existeMed) {
                    String sqlUpdMed = "UPDATE medicamentos SET dosis_diaria = ? WHERE afeccion_id = ?";
                    try (PreparedStatement psUpd = con.prepareStatement(sqlUpdMed)) {
                        psUpd.setString(1, dto.getDose());
                        psUpd.setInt(2, id);
                        psUpd.executeUpdate();
                    }
                } else {
                    String sqlInsMed = "INSERT INTO medicamentos (nombre_droga, dosis_diaria, afeccion_id) VALUES (?, ?, ?)";
                    try (PreparedStatement psIns = con.prepareStatement(sqlInsMed)) {
                        psIns.setString(1, "Tratamiento");
                        psIns.setString(2, dto.getDose());
                        psIns.setInt(3, id);
                        psIns.executeUpdate();
                    }
                }
            } else {
                if (existeMed) {
                    String sqlDelMed = "DELETE FROM medicamentos WHERE afeccion_id = ?";
                    try (PreparedStatement psDel = con.prepareStatement(sqlDelMed)) {
                        psDel.setInt(1, id);
                        psDel.executeUpdate();
                    }
                }
            }
            
            con.commit();
        } catch (SQLException e) {
            if (con != null) con.rollback();
            throw e;
        } finally {
            if (con != null) con.close();
        }
    }

    // Qué hace: Elimina una afección y su respectivo medicamento de forma transaccional.
    // Por qué existe: Permite dar de baja un diagnóstico sin romper la integridad física de las tablas.
    // Qué problema resuelve: Remueve en orden los medicamentos huérfanos para evitar errores de claves ajenas.
    public void eliminarAfeccion(int id) throws SQLException {
        Connection con = null;
        try {
            con = Conexion.obtener();
            con.setAutoCommit(false);
            
            String sqlDelMeds = "DELETE FROM medicamentos WHERE afeccion_id = ?";
            try (PreparedStatement psDelMeds = con.prepareStatement(sqlDelMeds)) {
                psDelMeds.setInt(1, id);
                psDelMeds.executeUpdate();
            }
            
            String sqlDelAff = "DELETE FROM afecciones WHERE id = ?";
            try (PreparedStatement psDelAff = con.prepareStatement(sqlDelAff)) {
                psDelAff.setInt(1, id);
                psDelAff.executeUpdate();
            }
            
            con.commit();
        } catch (SQLException e) {
            if (con != null) con.rollback();
            throw e;
        } finally {
            if (con != null) con.close();
        }
    }

    // Qué hace: Obtiene la lista completa de integrantes asociados a un plan familiar, sin límites de paginación.
    // Por qué existe: Requerido para rellenar el listado desplegable de selección de miembros en otros módulos como Plan de Acción.
    // Qué problema resuelve: Provee acceso rápido a todos los familiares registrados de forma estructurada.
    public List<IntegranteDTO> listarTodosIntegrantes(int planId) throws SQLException {
        String sql = "SELECT i.id, i.nombre, i.apellido, i.numero_documento, p.nombre AS kinship "
                   + "FROM integrantes i "
                   + "LEFT JOIN parentescos p ON i.parentesco_id = p.id "
                   + "WHERE i.plan_id = ?";
        
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planId);
            
            List<IntegranteDTO> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    IntegranteDTO dto = new IntegranteDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setNames(rs.getString("nombre"));
                    dto.setLastNames(rs.getString("apellido"));
                    dto.setDocumentNumber(rs.getString("numero_documento") != null ? rs.getString("numero_documento") : "No registrado");
                    dto.setKinshipName(rs.getString("kinship") != null ? rs.getString("kinship") : "No registrado");
                    lista.add(dto);
                }
            }
            return lista;
        }
    }

    // Qué hace: Helper privado para asignar de manera condicional un número entero o el tipo SQL NULL en una sentencia JDBC.
    // Por qué existe: Evita errores al intentar escribir un valor cero (0) o no válido en columnas relacionales de tipo entero en MySQL.
    // Qué problema resuelve: Mapea la ausencia de selección del frontend a un valor NULL real en la base de datos.
    private void setNullableInt(PreparedStatement ps, int index, int value) throws SQLException {
        if (value > 0) {
            ps.setInt(index, value);
        } else {
            ps.setNull(index, Types.INTEGER);
        }
    }
}
