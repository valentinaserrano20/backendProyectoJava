package Modelo.DAO;

import Modelo.Config.Conexion;
import Modelo.DTO.IntegranteDTO;
import Modelo.DTO.AfeccionDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/*
 * Qué hace (la acción): Define la clase IntegranteDAO que implementa operaciones CRUD sobre las tablas 'integrantes', 'afecciones' y 'medicamentos' en MySQL.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - DAO (Data Access Object): Centraliza las operaciones de acceso físico a datos correspondientes a familiares.
 * Para qué se usa (el propósito): Gestionar la información de los integrantes del hogar, sus parentescos, tipos de sangre, afecciones y dosificaciones médicas.
 * Por qué es importante (el impacto o problema que resuelve): Aísla por completo las consultas relacionales del dominio de familiares y coordina de manera transaccional el borrado o registro en cascada de medicamentos y afecciones.
 */
public class IntegranteDAO {

    /*
     * Qué hace (la acción): Consulta la cantidad total de integrantes que pertenecen a un plan familiar en MySQL.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - SELECT COUNT(*): Cuenta registros de integrantes filtrando por plan_id.
     * Para qué se usa (el propósito): Suministrar al paginador de la interfaz web la cantidad exacta de registros del plan.
     */
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

    /*
     * Qué hace (la acción): Verifica si un plan familiar ya tiene un Jefe de hogar (parentesco_id = 1) asignado.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - parentesco_id = 1: Llave foránea del catálogo de parentescos que representa al Jefe de hogar.
     *   - excluirIntegranteId: ID del integrante que se está editando actualmente para no contar su propio registro en la verificación de duplicados.
     * Para qué se usa (el propósito): Validar la regla de negocio que permite como máximo un solo jefe de hogar por cada familia.
     * Por qué es importante (el impacto o problema que resuelve): Evita inconsistencias de datos e impide que el usuario asigne a múltiples jefes de hogar en una misma vivienda.
     */
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

    /*
     * Qué hace (la acción): Recupera la lista paginada de integrantes familiares asociados a un plan.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - LEFT JOIN: Une la tabla integrantes con parentescos, grupos sanguíneos y planes familiares para traer denominaciones legibles.
     *   - LIMIT ? OFFSET ?: Cláusulas JDBC para recuperar un segmento de la tabla.
     * Para qué se usa (el propósito): Cargar el listado estructurado de familiares en la interfaz de usuario en porciones controladas.
     * Por qué es importante (el impacto o problema que resuelve): Evita la sobrecarga de red al transferir información pesada, garantizando velocidad de carga en dispositivos con conectividad lenta.
     */
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

    /*
     * Qué hace (la acción): Obtiene la ficha de datos detallada de un integrante a partir de su ID.
     * Qué significa (conceptos, métodos, tipos involucrados): Mapea todos los IDs foráneos (documento, género, nacionalidad, parentesco, sangre) y sus descripciones legibles.
     * Para qué se usa (el propósito): Suministrar al frontend la ficha del integrante para su visualización o precarga de datos al editar.
     */
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

    /*
     * Qué hace (la acción): Inserta un nuevo familiar en la tabla 'integrantes' de MySQL y devuelve su ID asignado.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - Statement.RETURN_GENERATED_KEYS: Permite capturar la clave primaria autogenerada.
     *   - setNullableInt: Helper que maneja valores enteros opcionales insertando NULL de SQL en su lugar si su valor es menor o igual a cero.
     * Para qué se usa (el propósito): Registrar un nuevo miembro del hogar dentro del plan familiar de evacuación.
     */
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

    /*
     * Qué hace (la acción): Modifica la información del integrante familiar en base de datos.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - UPDATE: Modifica los campos relacionales del familiar por su ID.
     * Para qué se usa (el propósito): Persistir los cambios del integrante editado desde el frontend.
     */
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

    /*
     * Qué hace (la acción): Elimina transaccionalmente un integrante de la base de datos, barriendo previamente sus medicamentos y afecciones registradas.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - con.setAutoCommit(false): Desactiva la confirmación automática para realizar una transacción ACID segura.
     *   - DELETE FROM medicamentos: Borra medicamentos huérfanos asociados a las afecciones del integrante.
     *   - DELETE FROM afecciones: Borra los diagnósticos médicos del familiar.
     * Para qué se usa (el propósito): Dar de baja a un integrante sin romper las restricciones físicas de llaves foráneas de MySQL.
     * Por qué es importante (el impacto o problema que resuelve): Previene excepciones críticas y caídas de base de datos al asegurar un borrado limpio en cadena de la información médica.
     */
    public void eliminarIntegrante(int id) throws SQLException {
        // Usamos try-with-resources para cerrar automáticamente la conexión al salir del bloque
        try (Connection con = Conexion.obtener()) {
            // Inicia la transacción manual desactivando auto-commit
            con.setAutoCommit(false);
            try {
                // Almacenamos los IDs de afecciones asociadas para poder eliminar los medicamentos satélite correspondientes
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
                
                // Si el integrante posee afecciones asociadas, removemos primero sus medicamentos para evitar fallos de FK
                if (!afeccionesIds.isEmpty()) {
                    String sqlDelMeds = "DELETE FROM medicamentos WHERE afeccion_id = ?";
                    try (PreparedStatement psDelMeds = con.prepareStatement(sqlDelMeds)) {
                        for (int affId : afeccionesIds) {
                            psDelMeds.setInt(1, affId);
                            psDelMeds.executeUpdate();
                        }
                    }
                }
                
                // Eliminamos las afecciones del integrante
                String sqlDelAfecciones = "DELETE FROM afecciones WHERE integrante_id = ?";
                try (PreparedStatement psDelAff = con.prepareStatement(sqlDelAfecciones)) {
                    psDelAff.setInt(1, id);
                    psDelAff.executeUpdate();
                }
                
                // Eliminamos finalmente el registro físico del integrante
                String sqlDelMember = "DELETE FROM integrantes WHERE id = ?";
                try (PreparedStatement psDelMem = con.prepareStatement(sqlDelMember)) {
                    psDelMem.setInt(1, id);
                    psDelMem.executeUpdate();
                }
                
                // Confirmamos la transacción
                con.commit();
            } catch (SQLException e) {
                // Revertimos todos los cambios en caso de excepción
                con.rollback();
                throw e; // Propagamos el error
            }
        }
    }

    /*
     * Qué hace (la acción): Obtiene la lista de afecciones médicas que padece un integrante familiar.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - LEFT JOIN medicamentos: Vincula la afección con la dosificación diaria de su tratamiento asociado.
     * Para qué se usa (el propósito): Alimentar la tabla de padecimientos médicos de la ficha familiar en la interfaz SPA.
     */
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
                    // Utiliza el método de mapeo reutilizable para evitar código duplicado
                    lista.add(mapearAfeccion(rs));
                }
            }
            // Qué hace: Retorna la lista de afecciones mapeadas del integrante.
            return lista;
        }
    }

    /*
     * Qué hace (la acción): Consulta la información detallada de una afección en particular y su dosis relacionada.
     * Qué significa (conceptos, métodos, tipos involucrados): Mapea las columnas de la fila actual de afecciones y medicamentos a un DTO.
     * Para qué se usa (el propósito): Recuperar los datos de una enfermedad o alergia para precargarla en el formulario de edición.
     */
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
                    // Utiliza el método de mapeo reutilizable para evitar código duplicado
                    return mapearAfeccion(rs);
                }
            }
        }
        // Qué hace: Retorna null si la afección no existe en base de datos.
        return null;
    }

    /*
     * Qué hace (la acción): Inserta transaccionalmente una afección y su respectivo medicamento dosis si se incluye.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - con.setAutoCommit(false): Habilita modo transaccional.
     *   - Statement.RETURN_GENERATED_KEYS: Obtiene la clave generada del diagnóstico para enlazar el medicamento relacional.
     * Para qué se usa (el propósito): Guardar el diagnóstico de salud y dosificación médica del integrante.
     */
    public int crearAfeccion(AfeccionDTO dto) throws SQLException {
        // Usamos try-with-resources para la conexión física JDBC
        try (Connection con = Conexion.obtener()) {
            // Deshabilitamos el auto-commit para control transaccional manual
            con.setAutoCommit(false);
            try {
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
                
                // Guardamos definitivamente los cambios de ambas tablas
                con.commit();
                return affId;
            } catch (SQLException e) {
                // Si ocurre cualquier error, revertimos la transacción
                con.rollback();
                throw e;
            }
        }
    }

    /*
     * Qué hace (la acción): Modifica una afección y actualiza, inserta o elimina el medicamento asociado en la misma transacción.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - con.rollback(): Deshace la transacción en caso de errores JDBC.
     * Para qué se usa (el propósito): Salvar los cambios de la ficha médica de un familiar de forma consistente.
     */
    public void actualizarAfeccion(int id, AfeccionDTO dto) throws SQLException {
        // Usamos try-with-resources para la conexión física JDBC
        try (Connection con = Conexion.obtener()) {
            // Habilitamos el modo transaccional
            con.setAutoCommit(false);
            try {
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
                
                // Confirmamos la actualización transaccional
                con.commit();
            } catch (SQLException e) {
                // En caso de error, revertimos la transacción
                con.rollback();
                throw e;
            }
        }
    }

    /*
     * Qué hace (la acción): Elimina físicamente una afección y su respectivo medicamento de manera transaccional.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - DELETE FROM medicamentos WHERE afeccion_id = ?: Limpia los tratamientos huérfanos.
     * Para qué se usa (el propósito): Dar de baja un diagnóstico de salud del integrante familiar.
     */
    public void eliminarAfeccion(int id) throws SQLException {
        // Usamos try-with-resources para asegurar el cierre automático de la conexión
        try (Connection con = Conexion.obtener()) {
            // Configuramos auto-commit en falso para control transaccional manual
            con.setAutoCommit(false);
            try {
                // Elimina primero los medicamentos dependientes de la afección
                String sqlDelMeds = "DELETE FROM medicamentos WHERE afeccion_id = ?";
                try (PreparedStatement psDelMeds = con.prepareStatement(sqlDelMeds)) {
                    psDelMeds.setInt(1, id);
                    psDelMeds.executeUpdate();
                }
                
                // Elimina finalmente la afección
                String sqlDelAff = "DELETE FROM afecciones WHERE id = ?";
                try (PreparedStatement psDelAff = con.prepareStatement(sqlDelAff)) {
                    psDelAff.setInt(1, id);
                    psDelAff.executeUpdate();
                }
                
                // Confirmamos la eliminación de ambos registros
                con.commit();
            } catch (SQLException e) {
                // En caso de error, revertimos la transacción
                con.rollback();
                throw e;
            }
        }
    }

    /*
     * Qué hace (la acción): Obtiene a todos los integrantes asociados a un plan familiar de emergencia, sin límite de paginación.
     * Qué significa (conceptos, métodos, tipos involucrados): SELECT con filtro WHERE plan_id = ? y LEFT JOIN con la tabla parentescos.
     * Para qué se usa (el propósito): Cargar el selector de responsables o coordinadores familiares en el resto de los módulos.
     */
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

    /*
     * Qué hace (la acción): Helper privado para enlazar un número entero al PreparedStatement o almacenar un valor NULL de SQL en su lugar.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - ps.setNull(index, Types.INTEGER): Almacena un NULL en MySQL si el ID suministrado es inválido (menor o igual a cero).
     * Para qué se usa (el propósito): Manejar llaves foráneas opcionales del familiar (EPS, tipo de sangre, etc.) sin arrojar excepciones JDBC.
     */
    private void setNullableInt(PreparedStatement ps, int index, int value) throws SQLException {
        // Qué hace: Si el valor es mayor a cero (ID válido), lo vincula como entero normal.
        if (value > 0) {
            ps.setInt(index, value);
        } else {
            // Qué hace: En caso contrario, vincula explícitamente el tipo nulo de SQL.
            ps.setNull(index, Types.INTEGER);
        }
    }

    /*
     * Qué hace (la acción): Obtiene la lista completa de todos los familiares de la base de datos de manera ligera (ID de integrante y ID de plan).
     * Qué significa (conceptos, métodos, tipos involucrados): SELECT id AS member_id, plan_id AS family_plan_id FROM integrantes.
     * Para qué se usa (el propósito): Permitir que el supervisor realice búsquedas o filtrados rápidos de planes familiares en base a los integrantes en memoria.
     */
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

    /*
     * Qué hace (la acción): Helper que mapea las columnas del ResultSet a un DTO de Afección, resolviendo el nombre y el ID del tipo de condición.
     * Qué significa (conceptos, métodos, tipos involucrados): Traduce el ENUM de base de datos ("enfermedad", "discapacidad", "alergia") a IDs numéricos del frontend.
     * Para qué se usa (el propósito): Reutilizar la lógica de mapeo de afecciones en múltiples métodos de consulta del DAO.
     */
    private AfeccionDTO mapearAfeccion(ResultSet rs) throws SQLException {
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
