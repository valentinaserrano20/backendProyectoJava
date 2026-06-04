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
        // Explicación de consulta SQL:
        // - Información buscada: El conteo total de filas (COUNT(*)) asociadas a un plan.
        // - Tablas participantes: integrantes.
        // - Filtros aplicados: plan_id = ? (el plan familiar a evaluar).
        String sql = "SELECT COUNT(*) AS total FROM integrantes WHERE plan_id = ?";
        // Qué hace: Abre la conexión a la base de datos y compila el statement parametrizado.
        // Por qué existe: Garantiza el uso de PreparedStatement para prevenir inyección SQL.
        // Qué problema resuelve: Administra el cierre automático de la conexión y del statement.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Vincula el ID del plan familiar al primer marcador (?) de la consulta.
            ps.setInt(1, planId);
            // Qué hace: Ejecuta la consulta de conteo en la base de datos.
            try (ResultSet rs = ps.executeQuery()) {
                // Qué hace: Recupera el valor de la columna 'total' de la consulta.
                if (rs.next()) {
                    // Qué hace: Retorna la cantidad de integrantes encontrados.
                    return rs.getInt("total");
                }
            }
        }
        // Qué hace: Retorna 0 si no se encontró información.
        return 0;
    }

    // Qué hace: Verifica si un plan familiar ya cuenta con un Jefe de hogar (parentesco_id = 1) registrado.
    // Por qué existe: Garantiza la regla de negocio que restringe a un único Jefe de hogar por familia.
    // Qué problema resuelve: Impide la existencia de duplicidades de cabeza de hogar dentro de la base de datos para un mismo plan.
    public boolean tieneJefeHogar(int planId, int excluirIntegranteId) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Cantidad de integrantes que tengan parentesco_id = 1 (Jefe de hogar).
        // - Tablas participantes: integrantes.
        // - Filtros aplicados: plan_id = ?, parentesco_id = 1, e id != ? (para excluir al propio integrante al actualizar).
        String sql = "SELECT COUNT(*) AS total FROM integrantes WHERE plan_id = ? AND parentesco_id = 1 AND id != ?";
        // Qué hace: Obtiene la conexión y compila el PreparedStatement.
        // Por qué existe: Asegura que la verificación sea contra datos limpios y seguros contra inyección SQL.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Asigna el ID del plan familiar al primer parámetro.
            ps.setInt(1, planId);
            // Qué hace: Asigna el ID a excluir al segundo parámetro.
            ps.setInt(2, excluirIntegranteId);
            // Qué hace: Ejecuta la consulta en MySQL.
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Qué hace: Retorna verdadero si el conteo es mayor a cero.
                    return rs.getInt("total") > 0;
                }
            }
        }
        // Qué hace: Retorna falso por defecto si no se detectó ningún jefe de hogar preexistente.
        return false;
    }

    // Qué hace: Consulta un listado paginado de integrantes asociados a un plan, uniendo parentescos y grupos sanguíneos.
    // Por qué existe: Permite renderizar las tarjetas visuales de integrantes en el frontend de forma dosificada.
    // Qué problema resuelve: Limita y dosifica el número de registros cargados en una sola petición, reduciendo el consumo de red y memoria.
    public List<IntegranteDTO> listarIntegrantes(int planId, int limit, int offset) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Columnas de integrantes (id, nombre, apellido, documento, nacimiento, celular), parentesco (kinship), grupo sanguíneo (blood_group) y estado del plan familiar (status_id).
        // - Tablas participantes: integrantes (i), parentescos (p), grupos_sanguineos (g), planes_familiares (pf).
        // - Relaciones (JOINs): LEFT JOINs con parentescos en parentesco_id, grupos_sanguineos en grupo_sanguineo_id y planes_familiares en plan_id.
        // - Filtros aplicados: i.plan_id = ?, paginado mediante LIMIT ? OFFSET ?.
        String sql = "SELECT i.id, i.nombre, i.apellido, i.numero_documento, i.fecha_nacimiento, i.celular, "
                   + "p.nombre AS kinship, g.nombre AS blood_group, pf.estado_id AS status_id "
                   + "FROM integrantes i "
                   + "LEFT JOIN parentescos p ON i.parentesco_id = p.id "
                   + "LEFT JOIN grupos_sanguineos g ON i.grupo_sanguineo_id = g.id "
                   + "LEFT JOIN planes_familiares pf ON i.plan_id = pf.id "
                   + "WHERE i.plan_id = ? "
                   + "LIMIT ? OFFSET ?";
        
        // Qué hace: Abre la conexión JDBC y prepara la consulta SQL.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Asigna los marcadores de posición para el plan, límite de registros y desplazamiento.
            ps.setInt(1, planId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            
            // Qué hace: Inicializa la lista que almacenará los DTOs mapeados.
            List<IntegranteDTO> lista = new ArrayList<>();
            // Qué hace: Ejecuta la consulta y recorre cada registro devuelto.
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Qué hace: Instancia un nuevo IntegranteDTO para almacenar la fila actual.
                    IntegranteDTO dto = new IntegranteDTO();
                    // Qué hace: Mapea las columnas del ResultSet al DTO.
                    dto.setId(rs.getInt("id"));
                    dto.setNames(rs.getString("nombre"));
                    dto.setLastNames(rs.getString("apellido"));
                    dto.setDocumentTypeAcronym("");
                    // Qué hace: Maneja de forma segura valores nulos en documento, celular, parentesco y sangre para evitar cadenas vacías o rotas.
                    dto.setDocumentNumber(rs.getString("numero_documento") != null ? rs.getString("numero_documento") : "No registrado");
                    dto.setPhone(rs.getString("celular") != null ? rs.getString("celular") : "No registrado");
                    dto.setKinshipName(rs.getString("kinship") != null ? rs.getString("kinship") : "No registrado");
                    dto.setBloodGroupName(rs.getString("blood_group") != null ? rs.getString("blood_group") : "No registrado");
                    dto.setBirthDate(rs.getString("fecha_nacimiento"));
                    dto.setStatusId(rs.getInt("status_id"));
                    // Qué hace: Agrega el DTO completamente poblado a la lista de retorno.
                    lista.add(dto);
                }
            }
            // Qué hace: Retorna la lista resultante de integrantes.
            return lista;
        }
    }

    // Qué hace: Obtiene la información detallada completa de un integrante familiar, incluyendo tipo de documento, género, parentesco, sangre y nacionalidad.
    // Por qué existe: Se utiliza para alimentar el modal de visualización de detalles completos ("Ver más") y la pantalla de edición de integrantes.
    // Qué problema resuelve: Permite recuperar todos los datos relacionales de un integrante de forma atómica en una sola consulta de unión (JOIN).
    public IntegranteDTO obtenerIntegrante(int id) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Atributos detallados del integrante e información legible asociada (siglas del documento, parentesco, etc.).
        // - Tablas participantes: integrantes (i), tipo_documentos (td), generos (gen), parentescos (p), grupos_sanguineos (g), nacionalidades (n).
        // - Relaciones (JOINs): LEFT JOINs con tipo_documentos en tipo_documento_id, generos en genero_id, parentescos en parentesco_id, grupos_sanguineos en grupo_sanguineo_id y nacionalidades en nacionalidad_id.
        // - Filtros aplicados: i.id = ? (el id único del integrante).
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
        
        // Qué hace: Abre la conexión JDBC y prepara la consulta SQL parametrizada.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Asigna el id del integrante al marcador de parámetro.
            ps.setInt(1, id);
            // Qué hace: Ejecuta la consulta de base de datos.
            try (ResultSet rs = ps.executeQuery()) {
                // Qué hace: Si se encuentra la fila correspondiente al integrante.
                if (rs.next()) {
                    // Qué hace: Crea el DTO correspondiente para transferir la información al frontend.
                    IntegranteDTO dto = new IntegranteDTO();
                    // Qué hace: Setea los valores primitivos y campos de texto del integrante.
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
                    
                    // Qué hace: Setea los valores legibles recuperados de las relaciones foráneas (JOINs).
                    dto.setDocumentTypeAcronym(rs.getString("document_type_acronym") != null ? rs.getString("document_type_acronym") : "");
                    dto.setGenderName(rs.getString("gender_name") != null ? rs.getString("gender_name") : "");
                    dto.setKinshipName(rs.getString("kinship_name") != null ? rs.getString("kinship_name") : "");
                    dto.setBloodGroupName(rs.getString("blood_group_name") != null ? rs.getString("blood_group_name") : "");
                    dto.setNationalityName(rs.getString("nationality_name") != null ? rs.getString("nationality_name") : "");
                    // Qué hace: Retorna el DTO de integrante poblado.
                    return dto;
                }
            }
        }
        // Qué hace: Retorna null en caso de que no exista el registro.
        return null;
    }

    // Qué hace: Inserta un nuevo integrante en la base de datos y retorna el ID autogenerado, verificando si es el jefe de hogar.
    // Por qué existe: Permite agregar nuevos miembros a la familia dentro del flujo del plan familiar.
    // Qué problema resuelve: Registra al integrante con todas sus relaciones externas correspondientes, manejando nulos de forma correcta.
    public int crearIntegrante(IntegranteDTO dto) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Inserción de campos de integrante en integrantes.
        // - Tablas participantes: integrantes.
        // - Filtros aplicados: Ninguno (sentencia INSERT INTO con 13 placeholders parametrizados).
        String sql = "INSERT INTO integrantes (nombre, apellido, numero_documento, fecha_nacimiento, eps, celular, es_jefe_hogar, "
                   + "plan_id, tipo_documento_id, parentesco_id, grupo_sanguineo_id, nacionalidad_id, genero_id) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        // Qué hace: Abre la conexión a la base de datos y prepara el Statement configurado para retornar llaves generadas.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            // Qué hace: Asigna los parámetros básicos del integrante a insertar.
            ps.setString(1, dto.getNames());
            ps.setString(2, dto.getLastNames());
            // Qué hace: Controla si el número de documento viene vacío o nulo para insertarlo como NULL en base de datos.
            ps.setString(3, dto.getDocumentNumber() != null && !dto.getDocumentNumber().isEmpty() ? dto.getDocumentNumber() : null);
            ps.setDate(4, Date.valueOf(dto.getBirthDate()));
            // Qué hace: Controla si la EPS es vacía o nula para insertar NULL en la base de datos.
            ps.setString(5, dto.getEps() != null && !dto.getEps().isEmpty() ? dto.getEps() : null);
            // Qué hace: Controla si el celular es vacío o nulo para insertar NULL en la base de datos.
            ps.setString(6, dto.getPhone() != null && !dto.getPhone().isEmpty() ? dto.getPhone() : null);
            // Qué hace: Regla de negocio: Si el parentesco_id es 1 (Jefe de hogar), se marca es_jefe_hogar como verdadero.
            ps.setBoolean(7, dto.getKinshipId() == 1);
            ps.setInt(8, dto.getPlanId());
            
            // Qué hace: Asigna las claves foráneas utilizando el helper setNullableInt para soportar inserción de valores SQL NULL.
            setNullableInt(ps, 9, dto.getDocumentTypeId());
            setNullableInt(ps, 10, dto.getKinshipId());
            setNullableInt(ps, 11, dto.getBloodGroupId());
            setNullableInt(ps, 12, dto.getNationalityId());
            setNullableInt(ps, 13, dto.getGenderId());
            
            // Qué hace: Ejecuta la consulta INSERT.
            ps.executeUpdate();
            
            // Qué hace: Recupera las llaves generadas de tipo Auto-Increment.
            try (ResultSet rsKeys = ps.getGeneratedKeys()) {
                // Qué hace: Si se generó una clave, la retorna.
                if (rsKeys.next()) {
                    return rsKeys.getInt(1);
                }
            }
        }
        // Qué hace: Lanza una excepción si la inserción falló o no se obtuvo la clave de manera exitosa.
        throw new SQLException("Error al recuperar el ID generado para el integrante.");
    }

    // Qué hace: Modifica los datos personales y médicos generales de un integrante en la base de datos.
    // Por qué existe: Permite persistir los cambios hechos por el voluntario en el formulario de edición de integrante.
    // Qué problema resuelve: Actualiza los campos opcionales y obligatorios controlando la consistencia del jefe de hogar.
    public void actualizarIntegrante(int id, IntegranteDTO dto) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Actualización de columnas del integrante.
        // - Tablas participantes: integrantes.
        // - Filtros aplicados: WHERE id = ? (se actualiza el integrante con el id indicado).
        String sql = "UPDATE integrantes SET nombre = ?, apellido = ?, numero_documento = ?, fecha_nacimiento = ?, "
                   + "eps = ?, celular = ?, es_jefe_hogar = ?, tipo_documento_id = ?, parentesco_id = ?, "
                   + "grupo_sanguineo_id = ?, nacionalidad_id = ?, genero_id = ? "
                   + "WHERE id = ?";
        
        // Qué hace: Obtiene la conexión y compila el PreparedStatement.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            // Qué hace: Vincula los nuevos valores al PreparedStatement.
            ps.setString(1, dto.getNames());
            ps.setString(2, dto.getLastNames());
            // Qué hace: Maneja la inserción de valor nulo si el documento está vacío.
            ps.setString(3, dto.getDocumentNumber() != null && !dto.getDocumentNumber().isEmpty() ? dto.getDocumentNumber() : null);
            ps.setDate(4, Date.valueOf(dto.getBirthDate()));
            // Qué hace: Maneja la inserción de valor nulo si la EPS está vacía.
            ps.setString(5, dto.getEps() != null && !dto.getEps().isEmpty() ? dto.getEps() : null);
            // Qué hace: Maneja la inserción de valor nulo si el celular está vacío.
            ps.setString(6, dto.getPhone() != null && !dto.getPhone().isEmpty() ? dto.getPhone() : null);
            // Qué hace: Regla de negocio: Si el parentesco es Jefe de hogar (ID 1), marca el booleano en base de datos.
            ps.setBoolean(7, dto.getKinshipId() == 1);
            
            // Qué hace: Asigna las claves foráneas utilizando el helper setNullableInt para soportar SQL NULL.
            setNullableInt(ps, 8, dto.getDocumentTypeId());
            setNullableInt(ps, 9, dto.getKinshipId());
            setNullableInt(ps, 10, dto.getBloodGroupId());
            setNullableInt(ps, 11, dto.getNationalityId());
            setNullableInt(ps, 12, dto.getGenderId());
            
            // Qué hace: Asigna el id del integrante para el filtro WHERE.
            ps.setInt(13, id);
            
            // Qué hace: Ejecuta la consulta de actualización.
            ps.executeUpdate();
        }
    }

    // Qué hace: Elimina transaccionalmente un integrante familiar, eliminando primero sus medicamentos y afecciones en cadena.
    // Por qué existe: Previene la violación de restricciones de llave foránea (Foreign Key Constraints) de MySQL durante la baja física de un miembro.
    // Qué problema resuelve: Ejecuta todo el flujo de borrado bajo rollback manual, manteniendo la integridad referencial en caso de error.
    public void eliminarIntegrante(int id) throws SQLException {
        Connection con = null;
        try {
            // Qué hace: Obtiene la conexión a base de datos.
            con = Conexion.obtener();
            // Qué hace: Desactiva el auto-commit automático para iniciar una transacción manual.
            con.setAutoCommit(false);
            
            // Qué hace: Crea una lista para guardar los identificadores de afecciones del integrante.
            List<Integer> afeccionesIds = new ArrayList<>();
            // Explicación de consulta SQL:
            // - Información buscada: Buscar los IDs de afecciones asociadas al integrante.
            // - Tablas participantes: afecciones.
            // - Filtros aplicados: integrante_id = ?.
            String sqlGetAfecciones = "SELECT id FROM afecciones WHERE integrante_id = ?";
            try (PreparedStatement psGet = con.prepareStatement(sqlGetAfecciones)) {
                // Qué hace: Vincula el ID del integrante.
                psGet.setInt(1, id);
                // Qué hace: Lee el conjunto de resultados.
                try (ResultSet rs = psGet.executeQuery()) {
                    while (rs.next()) {
                        // Qué hace: Registra cada ID de afección en la lista temporal.
                        afeccionesIds.add(rs.getInt("id"));
                    }
                }
            }
            
            // Qué hace: Si el integrante posee afecciones asociadas.
            if (!afeccionesIds.isEmpty()) {
                // Explicación de consulta SQL:
                // - Información buscada: Eliminar registros de medicamentos asociados a una afección.
                // - Tablas participantes: medicamentos.
                // - Filtros aplicados: afeccion_id = ?.
                String sqlDelMeds = "DELETE FROM medicamentos WHERE afeccion_id = ?";
                try (PreparedStatement psDelMeds = con.prepareStatement(sqlDelMeds)) {
                    // Qué hace: Recorre la lista de afecciones para borrar en cadena cada medicamento asociado.
                    for (int affId : afeccionesIds) {
                        psDelMeds.setInt(1, affId);
                        psDelMeds.executeUpdate();
                    }
                }
            }
            
            // Explicación de consulta SQL:
            // - Información buscada: Eliminar afecciones del integrante.
            // - Tablas participantes: afecciones.
            // - Filtros aplicados: integrante_id = ?.
            String sqlDelAfecciones = "DELETE FROM afecciones WHERE integrante_id = ?";
            try (PreparedStatement psDelAff = con.prepareStatement(sqlDelAfecciones)) {
                psDelAff.setInt(1, id);
                psDelAff.executeUpdate();
            }
            
            // Explicación de consulta SQL:
            // - Información buscada: Eliminar el registro del integrante.
            // - Tablas participantes: integrantes.
            // - Filtros aplicados: id = ?.
            String sqlDelMember = "DELETE FROM integrantes WHERE id = ?";
            try (PreparedStatement psDelMem = con.prepareStatement(sqlDelMember)) {
                psDelMem.setInt(1, id);
                psDelMem.executeUpdate();
            }
            
            // Qué hace: Consolida y confirma los cambios de manera definitiva en la base de datos.
            con.commit();
        } catch (SQLException e) {
            // Qué hace: Revierte todos los cambios de la transacción si ocurre un error durante el proceso de eliminación.
            if (con != null) {
                con.rollback();
            }
            // Qué hace: Propaga la excepción hacia la capa superior (Servlet).
            throw e;
        } finally {
            // Qué hace: Asegura el cierre de la conexión de base de datos en el bloque finally.
            if (con != null) {
                con.close();
            }
        }
    }

    // Qué hace: Recupera las afecciones médicas sufridas por un integrante familiar, uniendo los datos de dosificación de medicamentos.
    // Por qué existe: Es invocado por la UI para desplegar el listado de padecimientos médicos en la pestaña de gestión del integrante.
    // Qué problema resuelve: Combina registros médicos de afecciones y medicamentos de forma atómica en un único resultado unificado.
    public List<AfeccionDTO> listarAfeccionesPorIntegrante(int memberId) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Columnas de afecciones (id, tipo, nombre_afeccion, integrante_id) y la dosis de su respectivo medicamento.
        // - Tablas participantes: afecciones (a), medicamentos (m).
        // - Relación (JOIN): LEFT JOIN entre afecciones y medicamentos mediante la columna afeccion_id.
        // - Filtros aplicados: a.integrante_id = ? (afecciones que padece este integrante).
        String sql = "SELECT a.id, a.tipo, a.nombre_afeccion, a.integrante_id, m.dosis_diaria "
                   + "FROM afecciones a "
                   + "LEFT JOIN medicamentos m ON a.id = m.afeccion_id "
                   + "WHERE a.integrante_id = ?";
        
        // Qué hace: Abre la conexión JDBC y prepara la consulta SQL.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Asigna el ID del integrante para filtrar los resultados.
            ps.setInt(1, memberId);
            
            // Qué hace: Inicializa la lista que contendrá las afecciones.
            List<AfeccionDTO> lista = new ArrayList<>();
            // Qué hace: Ejecuta la consulta SELECT y recorre el ResultSet.
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Qué hace: Instancia el DTO de afección para almacenar los datos de la fila actual.
                    AfeccionDTO dto = new AfeccionDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setMemberId(rs.getInt("integrante_id"));
                    dto.setName(rs.getString("nombre_afeccion"));
                    dto.setDose(rs.getString("dosis_diaria"));
                    
                    // Qué hace: Mapea la cadena literal de tipo ENMO en MySQL hacia un tipo numérico estructurado para el front.
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
                    // Qué hace: Agrega el elemento a la lista.
                    lista.add(dto);
                }
            }
            // Qué hace: Retorna la lista de afecciones mapeadas del integrante.
            return lista;
        }
    }

    // Qué hace: Obtiene la información estructurada de una afección en particular y su dosis relacionada.
    // Por qué existe: Se utiliza para precargar la información médica en el modal de SweetAlert de edición de afección.
    // Qué problema resuelve: Permite recuperar la dosis de medicamento asociada al diagnóstico específico de manera directa.
    public AfeccionDTO obtenerAfeccion(int id) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Columnas de la afección y dosis de su medicamento asociado.
        // - Tablas participantes: afecciones (a), medicamentos (m).
        // - Relación (JOIN): LEFT JOIN en afeccion_id.
        // - Filtros aplicados: a.id = ? (el id único de la afección).
        String sql = "SELECT a.id, a.tipo, a.nombre_afeccion, a.integrante_id, m.dosis_diaria "
                   + "FROM afecciones a "
                   + "LEFT JOIN medicamentos m ON a.id = m.afeccion_id "
                   + "WHERE a.id = ?";
        
        // Qué hace: Abre la conexión y prepara el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Vincula el id de la afección al statement.
            ps.setInt(1, id);
            // Qué hace: Ejecuta la consulta.
            try (ResultSet rs = ps.executeQuery()) {
                // Qué hace: Si se encuentra la afección, se procede con el mapeo al DTO.
                if (rs.next()) {
                    AfeccionDTO dto = new AfeccionDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setMemberId(rs.getInt("integrante_id"));
                    dto.setName(rs.getString("nombre_afeccion"));
                    dto.setDose(rs.getString("dosis_diaria"));
                    
                    // Qué hace: Convierte el String ENUM tipo en un ID legible y nombre descriptivo.
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
                    // Qué hace: Retorna la afección detallada.
                    return dto;
                }
            }
        }
        // Qué hace: Retorna null si la afección no existe en base de datos.
        return null;
    }

    // Qué hace: Registra transaccionalmente una afección y, en caso de incluir dosis, inserta automáticamente un medicamento satélite.
    // Por qué existe: Mapea la recolección simplificada de la UI (nombre y dosis) hacia el esquema relacional estructurado.
    // Qué problema resuelve: Inserta de manera segura en dos tablas distintas en una sola transacción, garantizando la consistencia de datos.
    public int crearAfeccion(AfeccionDTO dto) throws SQLException {
        Connection con = null;
        try {
            // Qué hace: Abre la conexión a la base de datos.
            con = Conexion.obtener();
            // Qué hace: Deshabilita el auto-commit automático para gestionar manualmente la transacción.
            con.setAutoCommit(false);
            
            // Explicación de consulta SQL:
            // - Información buscada: Registro de una nueva afección.
            // - Tablas participantes: afecciones.
            // - Filtros aplicados: Ninguno (INSERT con placeholders).
            String sqlAff = "INSERT INTO afecciones (tipo, nombre_afeccion, integrante_id) VALUES (?, ?, ?)";
            int affId = -1;
            
            // Qué hace: Prepara el statement para la inserción de la afección retornando la llave autogenerada.
            try (PreparedStatement psAff = con.prepareStatement(sqlAff, Statement.RETURN_GENERATED_KEYS)) {
                // Qué hace: Convierte el ID numérico de tipo de afección proveniente de la interfaz al ENUM que MySQL espera.
                String tipoEnum = "enfermedad";
                if (dto.getConditionTypeId() == 2) tipoEnum = "discapacidad";
                else if (dto.getConditionTypeId() == 3) tipoEnum = "alergia";
                
                psAff.setString(1, tipoEnum);
                psAff.setString(2, dto.getName());
                psAff.setInt(3, dto.getMemberId());
                psAff.executeUpdate();
                
                // Qué hace: Obtiene la llave primaria generada de la afección.
                try (ResultSet rsKeys = psAff.getGeneratedKeys()) {
                    if (rsKeys.next()) {
                        affId = rsKeys.getInt(1);
                    }
                }
            }
            
            // Qué hace: Si no se pudo obtener el ID autogenerado, lanza una excepción para detener la transacción.
            if (affId == -1) {
                throw new SQLException("No se pudo obtener el ID autogenerado de la afección.");
            }
            
            // Qué hace: Si el usuario diligenció una dosificación, procede a crear el medicamento correspondiente.
            if (dto.getDose() != null && !dto.getDose().trim().isEmpty()) {
                // Explicación de consulta SQL:
                // - Información buscada: Registro del medicamento.
                // - Tablas participantes: medicamentos.
                String sqlMed = "INSERT INTO medicamentos (nombre_droga, dosis_diaria, afeccion_id) VALUES (?, ?, ?)";
                try (PreparedStatement psMed = con.prepareStatement(sqlMed)) {
                    // Qué hace: Registra un valor por defecto para 'nombre_droga' y asocia la dosis e ID de la afección recién creada.
                    psMed.setString(1, "Tratamiento");
                    psMed.setString(2, dto.getDose());
                    psMed.setInt(3, affId);
                    psMed.executeUpdate();
                }
            }
            
            // Qué hace: Hace persistentes los cambios de la afección y el medicamento.
            con.commit();
            // Qué hace: Retorna el ID de la afección creada.
            return affId;
        } catch (SQLException e) {
            // Qué hace: Revierte la transacción completa si ocurrió algún fallo.
            if (con != null) con.rollback();
            throw e;
        } finally {
            // Qué hace: Cierra la conexión de base de datos de forma segura.
            if (con != null) con.close();
        }
    }

    // Qué hace: Modifica una afección y actualiza, inserta o remueve su dosificación correspondiente en la tabla medicamentos.
    // Por qué existe: Mantiene actualizados los cambios médicos del integrante, administrando la existencia opcional de medicamentos.
    // Qué problema resuelve: Evalúa transaccionalmente la existencia previa de la dosis para determinar si corresponde UPDATE, INSERT o DELETE.
    public void actualizarAfeccion(int id, AfeccionDTO dto) throws SQLException {
        Connection con = null;
        try {
            // Qué hace: Abre la conexión a la base de datos.
            con = Conexion.obtener();
            // Qué hace: Configura auto-commit en falso para administrar la transacción de forma manual.
            con.setAutoCommit(false);
            
            // Explicación de consulta SQL:
            // - Información buscada: Actualización de los datos de la afección.
            // - Tablas participantes: afecciones.
            // - Filtros aplicados: WHERE id = ?.
            String sqlAff = "UPDATE afecciones SET tipo = ?, nombre_afeccion = ? WHERE id = ?";
            try (PreparedStatement psAff = con.prepareStatement(sqlAff)) {
                // Qué hace: Mapea la interfaz al ENUM esperado en la base de datos.
                String tipoEnum = "enfermedad";
                if (dto.getConditionTypeId() == 2) tipoEnum = "discapacidad";
                else if (dto.getConditionTypeId() == 3) tipoEnum = "alergia";
                
                psAff.setString(1, tipoEnum);
                psAff.setString(2, dto.getName());
                psAff.setInt(3, id);
                psAff.executeUpdate();
            }
            
            // Qué hace: Verifica si la afección ya tiene un registro de medicamento asociado.
            boolean existeMed = false;
            // Explicación de consulta SQL:
            // - Información buscada: ID del medicamento asociado a la afección.
            // - Tablas participantes: medicamentos.
            // - Filtros aplicados: afeccion_id = ?.
            String sqlCheckMed = "SELECT id FROM medicamentos WHERE afeccion_id = ?";
            try (PreparedStatement psCheck = con.prepareStatement(sqlCheckMed)) {
                psCheck.setInt(1, id);
                try (ResultSet rs = psCheck.executeQuery()) {
                    existeMed = rs.next();
                }
            }
            
            // Qué hace: Si se especificó una dosis.
            if (dto.getDose() != null && !dto.getDose().trim().isEmpty()) {
                if (existeMed) {
                    // Explicación de consulta SQL:
                    // - Información buscada: Actualizar la dosis del medicamento.
                    // - Tablas participantes: medicamentos.
                    // - Filtros aplicados: afeccion_id = ?.
                    String sqlUpdMed = "UPDATE medicamentos SET dosis_diaria = ? WHERE afeccion_id = ?";
                    try (PreparedStatement psUpd = con.prepareStatement(sqlUpdMed)) {
                        psUpd.setString(1, dto.getDose());
                        psUpd.setInt(2, id);
                        psUpd.executeUpdate();
                    }
                } else {
                    // Explicación de consulta SQL:
                    // - Información buscada: Crear un nuevo registro de medicamento.
                    // - Tablas participantes: medicamentos.
                    String sqlInsMed = "INSERT INTO medicamentos (nombre_droga, dosis_diaria, afeccion_id) VALUES (?, ?, ?)";
                    try (PreparedStatement psIns = con.prepareStatement(sqlInsMed)) {
                        psIns.setString(1, "Tratamiento");
                        psIns.setString(2, dto.getDose());
                        psIns.setInt(3, id);
                        psIns.executeUpdate();
                    }
                }
            } else {
                // Qué hace: Si la dosis viene vacía y ya existía un medicamento, se procede a su eliminación física.
                if (existeMed) {
                    // Explicación de consulta SQL:
                    // - Información buscada: Eliminar el medicamento.
                    // - Tablas participantes: medicamentos.
                    // - Filtros aplicados: afeccion_id = ?.
                    String sqlDelMed = "DELETE FROM medicamentos WHERE afeccion_id = ?";
                    try (PreparedStatement psDel = con.prepareStatement(sqlDelMed)) {
                        psDel.setInt(1, id);
                        psDel.executeUpdate();
                    }
                }
            }
            
            // Qué hace: Guarda definitivamente los cambios en base de datos.
            con.commit();
        } catch (SQLException e) {
            // Qué hace: Revierte la transacción entera ante cualquier excepción.
            if (con != null) con.rollback();
            throw e;
        } finally {
            // Qué hace: Cierra la conexión a la base de datos de forma segura.
            if (con != null) con.close();
        }
    }

    // Qué hace: Elimina una afección y su respectivo medicamento de forma transaccional.
    // Por qué existe: Permite dar de baja un diagnóstico sin romper la integridad física de las tablas.
    // Qué problema resuelve: Remueve en orden los medicamentos huérfanos para evitar errores de claves ajenas.
    public void eliminarAfeccion(int id) throws SQLException {
        Connection con = null;
        try {
            // Qué hace: Obtiene la conexión JDBC.
            con = Conexion.obtener();
            // Qué hace: Inicia transacción manual (auto-commit en false).
            con.setAutoCommit(false);
            
            // Explicación de consulta SQL:
            // - Información buscada: Borrado del medicamento asociado a la afección.
            // - Tablas participantes: medicamentos.
            // - Filtros aplicados: afeccion_id = ?.
            String sqlDelMeds = "DELETE FROM medicamentos WHERE afeccion_id = ?";
            try (PreparedStatement psDelMeds = con.prepareStatement(sqlDelMeds)) {
                psDelMeds.setInt(1, id);
                psDelMeds.executeUpdate();
            }
            
            // Explicación de consulta SQL:
            // - Información buscada: Borrado de la afección.
            // - Tablas participantes: afecciones.
            // - Filtros aplicados: id = ?.
            String sqlDelAff = "DELETE FROM afecciones WHERE id = ?";
            try (PreparedStatement psDelAff = con.prepareStatement(sqlDelAff)) {
                psDelAff.setInt(1, id);
                psDelAff.executeUpdate();
            }
            
            // Qué hace: Confirma la transacción en base de datos.
            con.commit();
        } catch (SQLException e) {
            // Qué hace: Si ocurre un error, revierte los borrados para mantener la consistencia.
            if (con != null) con.rollback();
            throw e;
        } finally {
            // Qué hace: Libera la conexión a la base de datos.
            if (con != null) con.close();
        }
    }

    // Qué hace: Obtiene la lista completa de integrantes asociados a un plan familiar, sin límites de paginación.
    // Por qué existe: Requerido para rellenar el listado desplegable de selección de miembros en otros módulos como Plan de Acción.
    // Qué problema resuelve: Provee acceso rápido a todos los familiares registrados de forma estructurada.
    public List<IntegranteDTO> listarTodosIntegrantes(int planId) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: ID, nombre, apellido, documento e parentesco.
        // - Tablas participantes: integrantes (i), parentescos (p).
        // - Relación (JOIN): LEFT JOIN con parentescos en parentesco_id.
        // - Filtros aplicados: i.plan_id = ?.
        String sql = "SELECT i.id, i.nombre, i.apellido, i.numero_documento, p.nombre AS kinship "
                   + "FROM integrantes i "
                   + "LEFT JOIN parentescos p ON i.parentesco_id = p.id "
                   + "WHERE i.plan_id = ?";
        
        // Qué hace: Obtiene la conexión y compila el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Asigna el ID del plan familiar.
            ps.setInt(1, planId);
            
            // Qué hace: Inicializa la lista de integrantes.
            List<IntegranteDTO> lista = new ArrayList<>();
            // Qué hace: Ejecuta la consulta de selección.
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    IntegranteDTO dto = new IntegranteDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setNames(rs.getString("nombre"));
                    dto.setLastNames(rs.getString("apellido"));
                    // Qué hace: Previene el despliegue de valores vacíos o nulos en la interfaz.
                    dto.setDocumentNumber(rs.getString("numero_documento") != null ? rs.getString("numero_documento") : "No registrado");
                    dto.setKinshipName(rs.getString("kinship") != null ? rs.getString("kinship") : "No registrado");
                    lista.add(dto);
                }
            }
            // Qué hace: Retorna la lista completa de integrantes.
            return lista;
        }
    }

    // Qué hace: Helper privado para asignar de manera condicional un número entero o el tipo SQL NULL en una sentencia JDBC.
    // Por qué existe: Evita errores al intentar escribir un valor cero (0) o no válido en columnas relacionales de tipo entero en MySQL.
    // Qué problema resuelve: Mapea la ausencia de selección del frontend a un valor NULL real en la base de datos.
    private void setNullableInt(PreparedStatement ps, int index, int value) throws SQLException {
        // Qué hace: Si el valor es mayor a cero (ID válido), lo vincula como entero normal.
        if (value > 0) {
            ps.setInt(index, value);
        } else {
            // Qué hace: En caso contrario, vincula explícitamente el tipo nulo de SQL.
            ps.setNull(index, Types.INTEGER);
        }
    }

    // Qué hace: Obtiene la lista de todos los integrantes familiares registrados de manera compacta (ID de integrante y ID de plan).
    // Por qué existe: Soporta el filtrado en el panel del supervisor sobre los miembros de un plan familiar específico.
    // Qué problema resuelve: Permite recuperar la correspondencia de integrantes y sus planes asociados de forma eficiente y rápida.
    public List<java.util.Map<String, Object>> obtenerTodosFamilyMembers() throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Relación de identificadores de integrante (member_id) y plan familiar (family_plan_id).
        // - Tablas participantes: integrantes.
        // - Filtros aplicados: Ninguno, ya que se listan todos de manera global para que el supervisor filtre en memoria.
        String sql = "SELECT id AS member_id, plan_id AS family_plan_id FROM integrantes";
        // Qué hace: Abre conexión segura mediante recursos JDBC.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            // Qué hace: Inicializa la lista que contendrá el mapeo de integrantes.
            List<java.util.Map<String, Object>> lista = new ArrayList<>();
            // Qué hace: Recorre cada fila del ResultSet de base de datos.
            while (rs.next()) {
                // Qué hace: Instancia un mapa flexible para almacenar los IDs correspondientes.
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("member_id", rs.getInt("member_id"));
                map.put("family_plan_id", rs.getInt("family_plan_id"));
                lista.add(map);
            }
            // Qué hace: Retorna la lista con los mapeos resultantes.
            return lista;
        }
    }
}
