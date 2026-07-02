package Modelo.DAO;

import Modelo.Config.Conexion;
import Modelo.DTO.FactorRiesgoDTO;
import Modelo.DTO.AccionReduccionDTO;
import Modelo.DTO.FactorVulnerabilidadDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/*
 * Qué hace (la acción): Define la clase FactorRiesgoDAO para realizar operaciones de persistencia (CRUD) relativas a riesgos, tareas de mitigación y vulnerabilidades físicas.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - DAO: Objeto de acceso a datos que gestiona exclusivamente las consultas JDBC con MySQL.
 * Para qué se usa (el propósito): Servir de motor de persistencia del censo de factores de riesgo, sus acciones y vulnerabilidades en la SPA de voluntariado.
 * Por qué es importante (el impacto o problema que resuelve): Aísla por completo la complejidad SQL relacional de las tablas factores_riesgo, acciones_reduccion y factor_riesgo_vulnerabilidad.
 */
public class FactorRiesgoDAO {

    // =========================================================================
    // CRUD: FACTORES DE RIESGO
    // =========================================================================

    /*
     * Qué hace (la acción): Obtiene el listado completo de todos los factores de riesgo registrados a nivel general.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - LEFT JOIN amenazas: Vincula el factor de riesgo con el nombre de la amenaza relacionada (ej: Sismo, Deslizamiento).
     * Para qué se usa (el propósito): Mostrar un inventario general de riesgos al supervisor del sistema.
     * Por qué es importante (el impacto o problema que resuelve): Permite al personal de control tener visibilidad completa sobre los riesgos identificados en el censo territorial.
     */
    public List<FactorRiesgoDTO> obtenerTodos() throws SQLException {
        // Qué hace: Inicializa la lista dinámica que contendrá los factores de riesgo.
        List<FactorRiesgoDTO> lista = new ArrayList<>();
        // Explicación de consulta SQL:
        // - Información buscada: Columnas de factores_riesgo y nombre de la amenaza.
        // - Tablas participantes: factores_riesgo (fr), amenazas (am).
        // - Relación (JOIN): LEFT JOIN entre factores_riesgo y amenazas en amenaza_id.
        // - Filtros aplicados: Ninguno (consulta global para supervisor).
        String sql = "SELECT fr.id, fr.descripcion, fr.ubicacion, fr.distancia_metros, fr.plan_id, fr.amenaza_id, am.nombre AS amenaza_nombre " +
                     "FROM factores_riesgo fr " +
                     "LEFT JOIN amenazas am ON fr.amenaza_id = am.id";
        // Qué hace: Obtiene la conexión JDBC y compila el statement para su ejecución.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             // Qué hace: Ejecuta la consulta SELECT y almacena los resultados en un ResultSet.
             ResultSet rs = ps.executeQuery()) {
            // Qué hace: Recorre cada una de las filas recuperadas del ResultSet.
            while (rs.next()) {
                // Qué hace: Instancia el DTO para almacenar los datos del factor de riesgo actual.
                FactorRiesgoDTO dto = new FactorRiesgoDTO();
                dto.setId(rs.getInt("id"));
                dto.setDescription(rs.getString("descripcion"));
                dto.setUbication(rs.getString("ubicacion"));
                dto.setDistance(rs.getInt("distancia_metros"));
                dto.setFamilyPlanId(rs.getInt("plan_id"));
                dto.setThreatTypeId(rs.getInt("amenaza_id"));
                dto.setThreatTypeName(rs.getString("amenaza_nombre"));
                // Qué hace: Añade el DTO a la lista de retorno.
                lista.add(dto);
            }
        }
        // Qué hace: Retorna la lista de factores de riesgo mapeados.
        return lista;
    }

    /*
     * Qué hace (la acción): Obtiene los detalles específicos de un factor de riesgo mediante su ID.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - ps.setInt(1, id): Asigna el ID del riesgo al marcador de posición de la consulta.
     * Para qué se usa (el propósito): Recuperar la información de un riesgo al cargar el modal de edición.
     */
    public FactorRiesgoDTO obtenerPorId(int id) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Atributos de un factor de riesgo específico y su amenaza.
        // - Tablas participantes: factores_riesgo (fr), amenazas (am).
        // - Relación (JOIN): LEFT JOIN en amenaza_id.
        // - Filtros aplicados: fr.id = ? (el ID del factor de riesgo deseado).
        String sql = "SELECT fr.id, fr.descripcion, fr.ubicacion, fr.distancia_metros, fr.plan_id, fr.amenaza_id, am.nombre AS amenaza_nombre " +
                     "FROM factores_riesgo fr " +
                     "LEFT JOIN amenazas am ON fr.amenaza_id = am.id WHERE fr.id = ?";
        // Qué hace: Abre la conexión a la base de datos y compila el PreparedStatement.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Vincula el ID del factor de riesgo al primer parámetro.
            ps.setInt(1, id);
            // Qué hace: Ejecuta la consulta de lectura.
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Qué hace: Instancia el DTO y mapea cada columna del ResultSet.
                    FactorRiesgoDTO dto = new FactorRiesgoDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setDescription(rs.getString("descripcion"));
                    dto.setUbication(rs.getString("ubicacion"));
                    dto.setDistance(rs.getInt("distancia_metros"));
                    dto.setFamilyPlanId(rs.getInt("plan_id"));
                    dto.setThreatTypeId(rs.getInt("amenaza_id"));
                    dto.setThreatTypeName(rs.getString("amenaza_nombre"));
                    // Qué hace: Retorna el factor de riesgo obtenido.
                    return dto;
                }
            }
        }
        // Qué hace: Retorna null si no se localizó ningún registro.
        return null;
    }

    /*
     * Qué hace (la acción): Obtiene todos los factores de riesgo asociados a un plan familiar.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - WHERE fr.plan_id = ?: Filtra únicamente los factores de riesgo que corresponden a la familia.
     * Para qué se usa (el propósito): Renderizar en la interfaz del voluntario la grilla de riesgos específicos del hogar censado.
     */
    public List<FactorRiesgoDTO> obtenerPorPlan(int planId) throws SQLException {
        // Qué hace: Inicializa la lista dinámica que contendrá los factores de riesgo.
        List<FactorRiesgoDTO> lista = new ArrayList<>();
        // Explicación de consulta SQL:
        // - Información buscada: Factores de riesgo asociados a un plan.
        // - Tablas participantes: factores_riesgo (fr), amenazas (am).
        // - Relación (JOIN): LEFT JOIN en amenaza_id.
        // - Filtros aplicados: fr.plan_id = ? (plan familiar al que pertenecen).
        String sql = "SELECT fr.id, fr.descripcion, fr.ubicacion, fr.distancia_metros, fr.plan_id, fr.amenaza_id, am.nombre AS amenaza_nombre " +
                     "FROM factores_riesgo fr " +
                     "LEFT JOIN amenazas am ON fr.amenaza_id = am.id WHERE fr.plan_id = ?";
        // Qué hace: Obtiene la conexión JDBC y compila el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Asigna el ID del plan de emergencia al statement.
            ps.setInt(1, planId);
            // Qué hace: Ejecuta la consulta de base de datos.
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Qué hace: Instancia el DTO y mapea cada columna del ResultSet.
                    FactorRiesgoDTO dto = new FactorRiesgoDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setDescription(rs.getString("descripcion"));
                    dto.setUbication(rs.getString("ubicacion"));
                    dto.setDistance(rs.getInt("distancia_metros"));
                    dto.setFamilyPlanId(rs.getInt("plan_id"));
                    dto.setThreatTypeId(rs.getInt("amenaza_id"));
                    dto.setThreatTypeName(rs.getString("amenaza_nombre"));
                    // Qué hace: Agrega el DTO al listado de retorno.
                    lista.add(dto);
                }
            }
        }
        // Qué hace: Retorna la lista resultante de factores de riesgo.
        return lista;
    }

    /*
     * Qué hace (la acción): Obtiene de forma paginada los factores de riesgo asociados a un plan familiar.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - LIMIT ? OFFSET ?: Cláusulas SQL que limitan el número de registros devueltos y especifican a partir de qué fila iniciar.
     * Para qué se usa (el propósito): Implementar paginación del lado del servidor en las tablas de riesgos de la SPA.
     * Por qué es importante (el impacto o problema que resuelve): Evita la sobrecarga de datos en red al traer la información por fragmentos bajo demanda.
     */
    public List<FactorRiesgoDTO> obtenerPorPlanPaginado(int planId, int limit, int offset) throws SQLException {
        // Qué hace: Inicializa la lista dinámica que contendrá los factores de riesgo.
        List<FactorRiesgoDTO> lista = new ArrayList<>();
        // Explicación de consulta SQL:
        // - Información buscada: Factores de riesgo asociados a un plan.
        // - Tablas participantes: factores_riesgo (fr), amenazas (am).
        // - Relación (JOIN): LEFT JOIN en amenaza_id.
        // - Filtros aplicados: fr.plan_id = ?, paginado mediante LIMIT ? OFFSET ?.
        String sql = "SELECT fr.id, fr.descripcion, fr.ubicacion, fr.distancia_metros, fr.plan_id, fr.amenaza_id, am.nombre AS amenaza_nombre " +
                     "FROM factores_riesgo fr " +
                     "LEFT JOIN amenazas am ON fr.amenaza_id = am.id WHERE fr.plan_id = ? LIMIT ? OFFSET ?";
        // Qué hace: Abre la conexión a la base de datos y compila el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Asigna el ID de plan, el límite y el offset al statement JDBC.
            ps.setInt(1, planId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            // Qué hace: Ejecuta la consulta SELECT.
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Qué hace: Instancia el DTO y mapea cada columna del ResultSet.
                    FactorRiesgoDTO dto = new FactorRiesgoDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setDescription(rs.getString("descripcion"));
                    dto.setUbication(rs.getString("ubicacion"));
                    dto.setDistance(rs.getInt("distancia_metros"));
                    dto.setFamilyPlanId(rs.getInt("plan_id"));
                    dto.setThreatTypeId(rs.getInt("amenaza_id"));
                    dto.setThreatTypeName(rs.getString("amenaza_nombre"));
                    // Qué hace: Añade el DTO a la lista de retorno.
                    lista.add(dto);
                }
            }
        }
        // Qué hace: Retorna la lista resultante de factores de riesgo paginados.
        return lista;
    }

    /*
     * Qué hace (la acción): Cuenta cuántos factores de riesgo tiene asignados un plan de emergencia.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - SELECT COUNT(*): Función de agregación que cuenta las filas coincidentes.
     * Para qué se usa (el propósito): Calcular el número de páginas necesarias en el componente paginador del cliente.
     */
    public int contarPorPlan(int planId) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Cantidad total de registros asociados a un plan.
        // - Tablas participantes: factores_riesgo.
        // - Filtros aplicados: plan_id = ?.
        String sql = "SELECT COUNT(*) FROM factores_riesgo WHERE plan_id = ?";
        // Qué hace: Abre la conexión a la base de datos y compila el PreparedStatement.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Asigna el ID del plan de emergencia al primer parámetro.
            ps.setInt(1, planId);
            // Qué hace: Ejecuta la consulta de conteo en MySQL.
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Qué hace: Recupera el valor entero del COUNT.
                    return rs.getInt(1);
                }
            }
        }
        // Qué hace: Retorna 0 por defecto.
        return 0;
    }

    /*
     * Qué hace (la acción): Registra un nuevo factor de riesgo en la base de datos MySQL.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - ps.executeUpdate(): Envía la consulta INSERT para guardar permanentemente la información en la tabla factores_riesgo.
     * Para qué se usa (el propósito): Registrar los riesgos que acechan al hogar evaluado.
     */
    public boolean crear(FactorRiesgoDTO dto) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Registrar un nuevo factor de riesgo.
        // - Tablas participantes: factores_riesgo.
        String sql = "INSERT INTO factores_riesgo (descripcion, ubicacion, distancia_metros, plan_id, amenaza_id) VALUES (?, ?, ?, ?, ?)";
        // Qué hace: Obtiene la conexión y compila el statement JDBC.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Vincula los parámetros del DTO al statement.
            ps.setString(1, dto.getDescription());
            ps.setString(2, dto.getUbication());
            ps.setInt(3, dto.getDistance());
            ps.setInt(4, dto.getFamilyPlanId());
            ps.setInt(5, dto.getThreatTypeId());
            // Qué hace: Ejecuta la inserción y retorna verdadero si afectó al menos una fila.
            return ps.executeUpdate() > 0;
        }
    }

    /*
     * Qué hace (la acción): Modifica los datos descriptivos de un factor de riesgo existente.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - UPDATE: Modifica los campos configurados filtrando por el ID de la fila.
     * Para qué se usa (el propósito): Guardar las ediciones realizadas sobre los riesgos familiares.
     */
    public boolean actualizar(int id, FactorRiesgoDTO dto) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Actualización de atributos de un factor de riesgo.
        // - Tablas participantes: factores_riesgo.
        // - Filtros aplicados: WHERE id = ? (id de factor de riesgo).
        String sql = "UPDATE factores_riesgo SET descripcion = ?, ubicacion = ?, distancia_metros = ?, amenaza_id = ? WHERE id = ?";
        // Qué hace: Obtiene la conexión a base de datos y compila la consulta.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Vincula los campos modificados al statement JDBC.
            ps.setString(1, dto.getDescription());
            ps.setString(2, dto.getUbication());
            ps.setInt(3, dto.getDistance());
            ps.setInt(4, dto.getThreatTypeId());
            ps.setInt(5, id);
            // Qué hace: Ejecuta la actualización y retorna verdadero si afectó filas.
            return ps.executeUpdate() > 0;
        }
    }

    /*
     * Qué hace (la acción): Elimina de forma transaccional y en cascada un factor de riesgo junto con sus vulnerabilidades y acciones asociadas.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - con.setAutoCommit(false): Abre una transacción explícita de MySQL.
     *   - con.commit(): Consolida permanentemente el borrado en cascada.
     *   - con.rollback(): Deshace los cambios de la transacción ante cualquier fallo de base de datos.
     * Para qué se usa (el propósito): Borrar por completo la ficha de riesgo y todas sus dependencias relacionadas.
     * Por qué es importante (el impacto o problema que resuelve): Previene violaciones de llaves foráneas e inconsistencias de datos, garantizando que el borrado físico sea exitoso y atómico.
     */
    public boolean eliminar(int id) throws SQLException {
        Connection con = null;
        PreparedStatement psDelVul = null;
        PreparedStatement psDelAct = null;
        PreparedStatement psDelRie = null;
        try {
            // Qué hace: Obtiene la conexión a base de datos.
            con = Conexion.obtener();
            // Qué hace: Desactiva el auto-commit automático para gestionar manualmente la transacción.
            con.setAutoCommit(false);

            // 1. Eliminar relaciones en la tabla intermedia de vulnerabilidades
            // Explicación de consulta SQL:
            // - Información buscada: Borrar relaciones riesgo-vulnerabilidad.
            // - Tablas participantes: factor_riesgo_vulnerabilidad.
            // - Filtros aplicados: riesgo_id = ?.
            String sqlVul = "DELETE FROM factor_riesgo_vulnerabilidad WHERE riesgo_id = ?";
            psDelVul = con.prepareStatement(sqlVul);
            psDelVul.setInt(1, id);
            psDelVul.executeUpdate();

            // 2. Eliminar acciones de reducción asociadas
            // Explicación de consulta SQL:
            // - Información buscada: Borrar acciones de reducción asociadas a este riesgo.
            // - Tablas participantes: acciones_reduccion.
            // - Filtros aplicados: riesgo_id = ?.
            String sqlAct = "DELETE FROM acciones_reduccion WHERE riesgo_id = ?";
            psDelAct = con.prepareStatement(sqlAct);
            psDelAct.setInt(1, id);
            psDelAct.executeUpdate();

            // 3. Eliminar el factor de riesgo padre
            // Explicación de consulta SQL:
            // - Información buscada: Borrado físico del factor de riesgo.
            // - Tablas participantes: factores_riesgo.
            // - Filtros aplicados: id = ?.
            String sqlRie = "DELETE FROM factores_riesgo WHERE id = ?";
            psDelRie = con.prepareStatement(sqlRie);
            psDelRie.setInt(1, id);
            // Qué hace: Ejecuta la consulta de borrado del registro principal.
            int rows = psDelRie.executeUpdate();

            // Qué hace: Consolida y confirma los borrados en cadena.
            con.commit();
            // Qué hace: Retorna verdadero si se borró el registro padre exitosamente.
            return rows > 0;
        } catch (SQLException e) {
            // Qué hace: Revierte todos los cambios si ocurrió algún fallo para evitar inconsistencias.
            if (con != null) {
                con.rollback();
            }
            throw e;
        } finally {
            // Qué hace: Cierra de forma ordenada los PreparedStatement y la conexión JDBC.
            if (psDelVul != null) psDelVul.close();
            if (psDelAct != null) psDelAct.close();
            if (psDelRie != null) psDelRie.close();
            if (con != null) con.close();
        }
    }

    // =========================================================================
    // CRUD: ACCIONES DE REDUCCIÓN
    // =========================================================================

    /*
     * Qué hace (la acción): Obtiene una acción de reducción específica por su ID.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - LEFT JOIN integrantes: Relaciona la acción con el integrante responsable de ejecutarla.
     * Para qué se usa (el propósito): Recuperar los campos de la acción de mitigación para mostrar en la interfaz de edición.
     */
    public AccionReduccionDTO obtenerAccionPorId(int id) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Atributos de una acción de reducción y nombre del integrante asignado.
        // - Tablas participantes: acciones_reduccion (ar), integrantes (intg).
        // - Relación (JOIN): LEFT JOIN en responsable_id.
        // - Filtros aplicados: ar.id = ? (el id único de la acción).
        String sql = "SELECT ar.id, ar.descripcion_tarea, ar.fecha_termino, ar.riesgo_id, ar.responsable_id, " +
                     "intg.nombre AS integrante_nombre, intg.apellido AS integrante_apellido " +
                     "FROM acciones_reduccion ar " +
                     "LEFT JOIN integrantes intg ON ar.responsable_id = intg.id WHERE ar.id = ?";
        // Qué hace: Abre la conexión a base de datos y prepara el PreparedStatement.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Asigna el ID de la acción al statement.
            ps.setInt(1, id);
            // Qué hace: Ejecuta el query y procesa la fila resultante.
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Qué hace: Instancia el DTO y mapea cada columna del ResultSet.
                    AccionReduccionDTO dto = new AccionReduccionDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setAction(rs.getString("descripcion_tarea"));
                    dto.setEndDate(rs.getString("fecha_termino"));
                    dto.setRiskFactorId(rs.getInt("riesgo_id"));
                    dto.setMemberId(rs.getInt("responsable_id"));
                    dto.setMemberName(rs.getString("integrante_nombre"));
                    dto.setMemberLastNames(rs.getString("integrante_apellido"));
                    dto.setCreatedAt(rs.getString("fecha_termino")); // Fallback
                    // Qué hace: Retorna la acción de reducción mapeada.
                    return dto;
                }
            }
        }
        // Qué hace: Retorna null si no se encontró la acción de reducción.
        return null;
    }

    /*
     * Qué hace (la acción): Obtiene la lista de acciones de reducción o mitigación configuradas para un factor de riesgo.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - ar.riesgo_id = ?: Filtro que selecciona únicamente las tareas vinculadas al riesgo.
     * Para qué se usa (el propósito): Mostrar las medidas preventivas adoptadas ante un riesgo en la SPA.
     */
    public List<AccionReduccionDTO> obtenerAccionesPorRiesgo(int riesgoId) throws SQLException {
        // Qué hace: Inicializa la lista dinámica que contendrá las acciones.
        List<AccionReduccionDTO> lista = new ArrayList<>();
        // Explicación de consulta SQL:
        // - Información buscada: Acciones de reducción y nombres de sus responsables.
        // - Tablas participantes: acciones_reduccion (ar), integrantes (intg).
        // - Relación (JOIN): LEFT JOIN en responsable_id.
        // - Filtros aplicados: ar.riesgo_id = ? (el factor de riesgo de interés).
        String sql = "SELECT ar.id, ar.descripcion_tarea, ar.fecha_termino, ar.riesgo_id, ar.responsable_id, " +
                     "intg.nombre AS integrante_nombre, intg.apellido AS integrante_apellido " +
                     "FROM acciones_reduccion ar " +
                     "LEFT JOIN integrantes intg ON ar.responsable_id = intg.id WHERE ar.riesgo_id = ?";
        // Qué hace: Abre la conexión a la base de datos y compila el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Asigna el ID del riesgo al statement.
            ps.setInt(1, riesgoId);
            // Qué hace: Ejecuta la consulta de lectura y procesa el ResultSet.
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Qué hace: Instancia el DTO y mapea cada columna.
                    AccionReduccionDTO dto = new AccionReduccionDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setAction(rs.getString("descripcion_tarea"));
                    dto.setEndDate(rs.getString("fecha_termino"));
                    dto.setRiskFactorId(rs.getInt("riesgo_id"));
                    dto.setMemberId(rs.getInt("responsable_id"));
                    dto.setMemberName(rs.getString("integrante_nombre"));
                    dto.setMemberLastNames(rs.getString("integrante_apellido"));
                    dto.setCreatedAt(rs.getString("fecha_termino")); // Fallback
                    // Qué hace: Agrega el DTO a la lista de retorno.
                    lista.add(dto);
                }
            }
        }
        // Qué hace: Retorna la lista resultante de acciones de reducción.
        return lista;
    }

    /*
     * Qué hace (la acción): Registra una nueva acción de reducción para mitigar un riesgo específico.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - ps.setNull(4, Types.INTEGER): Enlaza un valor NULL si la tarea no tiene un responsable familiar asignado de momento.
     * Para qué se usa (el propósito): Añadir tareas preventivas al plan familiar de emergencia.
     */
    public boolean crearAccion(AccionReduccionDTO dto) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Insertar una acción de reducción.
        // - Tablas participantes: acciones_reduccion.
        String sql = "INSERT INTO acciones_reduccion (descripcion_tarea, fecha_termino, riesgo_id, responsable_id) VALUES (?, ?, ?, ?)";
        // Qué hace: Abre la conexión y prepara la consulta parametrizada.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Vincula los parámetros básicos al statement.
            ps.setString(1, dto.getAction());
            ps.setString(2, dto.getEndDate());
            ps.setInt(3, dto.getRiskFactorId());
            // Qué hace: Maneja la inserción de valor nulo si no se asignó un miembro responsable.
            if (dto.getMemberId() > 0) {
                ps.setInt(4, dto.getMemberId());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            // Qué hace: Ejecuta la inserción y retorna verdadero si afectó filas.
            return ps.executeUpdate() > 0;
        }
    }

    /*
     * Qué hace (la acción): Modifica la descripción, fecha límite e integrante responsable de una acción de reducción.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - UPDATE ar: Modifica la tarea.
     * Para qué se usa (el propósito): Guardar los cambios al editar la tarea de mitigación.
     */
    public boolean actualizarAccion(int id, AccionReduccionDTO dto) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Modificar una acción de reducción.
        // - Tablas participantes: acciones_reduccion.
        // - Filtros aplicados: WHERE id = ? (id de la acción).
        String sql = "UPDATE acciones_reduccion SET descripcion_tarea = ?, fecha_termino = ?, responsable_id = ? WHERE id = ?";
        // Qué hace: Abre la conexión a base de datos y compila el PreparedStatement.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Vincula los campos modificados.
            ps.setString(1, dto.getAction());
            ps.setString(2, dto.getEndDate());
            // Qué hace: Maneja la actualización de responsable nulo.
            if (dto.getMemberId() > 0) {
                ps.setInt(3, dto.getMemberId());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            // Qué hace: Asigna el id de la acción al placeholder del WHERE.
            ps.setInt(4, id);
            // Qué hace: Ejecuta la actualización en MySQL.
            return ps.executeUpdate() > 0;
        }
    }

    /*
     * Qué hace (la acción): Elimina físicamente una acción de reducción de la base de datos.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - DELETE FROM: Borra la fila basándose en su ID primario.
     * Para qué se usa (el propósito): Descartar tareas preventivas del plan familiar.
     */
    public boolean eliminarAccion(int id) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Eliminar una acción de reducción.
        // - Tablas participantes: acciones_reduccion.
        // - Filtros aplicados: WHERE id = ?.
        String sql = "DELETE FROM acciones_reduccion WHERE id = ?";
        // Qué hace: Abre la conexión a la base de datos y compila el PreparedStatement.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Vincula el ID de la acción y ejecuta la eliminación.
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // =========================================================================
    // CRUD: FACTORES DE VULNERABILIDAD
    // =========================================================================

    /*
     * Qué hace (la acción): Recupera la asociación de vulnerabilidad a un riesgo de forma detallada mediante su ID.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - LEFT JOIN vulnerabilidades: Junta el id de vulnerabilidad con su denominación textual del catálogo.
     *   - gradeStrToId: Helper que traduce el ENUM de BD a un ID numérico de grado compatible con el frontend.
     * Para qué se usa (el propósito): Leer el detalle de un factor de vulnerabilidad física para su visualización o modificación.
     */
    public FactorVulnerabilidadDTO obtenerVulnerabilidadPorId(int id) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Grado de vulnerabilidad y nombre del catálogo de vulnerabilidades.
        // - Tablas participantes: factor_riesgo_vulnerabilidad (frv), vulnerabilidades (v).
        // - Relación (JOIN): LEFT JOIN en vulnerabilidad_id.
        // - Filtros aplicados: frv.id = ? (el id único del registro de vulnerabilidad).
        String sql = "SELECT frv.id, frv.grado, frv.vulnerabilidad_personalizada, frv.riesgo_id, frv.vulnerabilidad_id, " +
                     "v.nombre AS vulnerabilidad_nombre " +
                     "FROM factor_riesgo_vulnerabilidad frv " +
                     "LEFT JOIN vulnerabilidades v ON frv.vulnerabilidad_id = v.id WHERE frv.id = ?";
        // Qué hace: Abre la conexión JDBC y prepara el statement.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Vincula el ID de la vulnerabilidad del riesgo.
            ps.setInt(1, id);
            // Qué hace: Ejecuta la consulta.
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Qué hace: Instancia el DTO y mapea cada columna del ResultSet.
                    FactorVulnerabilidadDTO dto = new FactorVulnerabilidadDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setRiskFactorId(rs.getInt("riesgo_id"));
                    dto.setVulnerabilityId(rs.getInt("vulnerabilidad_id"));
                    dto.setVulnerabilityName(rs.getString("vulnerabilidad_nombre"));
                    
                    // Qué hace: Convierte la cadena ENUM grado al respectivo ID de la UI.
                    String gradeStr = rs.getString("grado");
                    dto.setVulnerabilityGradeName(gradeStr);
                    dto.setVulnerabilityGradeId(gradeStrToId(gradeStr));
                    // Qué hace: Retorna la vulnerabilidad del riesgo detallada.
                    return dto;
                }
            }
        }
        // Qué hace: Retorna null si no existe.
        return null;
    }

    /*
     * Qué hace (la acción): Obtiene la lista de factores de vulnerabilidad física o estructural vinculados a un factor de riesgo.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - frv.riesgo_id = ?: Cláusula que selecciona las vulnerabilidades que aquejan a dicho riesgo.
     * Para qué se usa (el propósito): Mostrar el diagnóstico detallado de vulnerabilidad en la UI.
     */
    public List<FactorVulnerabilidadDTO> obtenerVulnerabilidadesPorRiesgo(int riesgoId) throws SQLException {
        // Qué hace: Inicializa la lista dinámica que contendrá las vulnerabilidades.
        List<FactorVulnerabilidadDTO> lista = new ArrayList<>();
        // Explicación de consulta SQL:
        // - Información buscada: Relaciones riesgo-vulnerabilidad y nombres descriptivos del catálogo.
        // - Tablas participantes: factor_riesgo_vulnerabilidad (frv), vulnerabilidades (v).
        // - Relación (JOIN): LEFT JOIN en vulnerabilidad_id.
        // - Filtros aplicados: frv.riesgo_id = ? (el factor de riesgo a consultar).
        String sql = "SELECT frv.id, frv.grado, frv.vulnerabilidad_personalizada, frv.riesgo_id, frv.vulnerabilidad_id, " +
                     "v.nombre AS vulnerabilidad_nombre " +
                     "FROM factor_riesgo_vulnerabilidad frv " +
                     "LEFT JOIN vulnerabilidades v ON frv.vulnerabilidad_id = v.id WHERE frv.riesgo_id = ?";
        // Qué hace: Abre la conexión JDBC y prepara la consulta parametrizada.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Asigna el ID del factor de riesgo al statement.
            ps.setInt(1, riesgoId);
            // Qué hace: Ejecuta la consulta de lectura y procesa el ResultSet.
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Qué hace: Instancia el DTO y mapea cada columna.
                    FactorVulnerabilidadDTO dto = new FactorVulnerabilidadDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setRiskFactorId(rs.getInt("riesgo_id"));
                    dto.setVulnerabilityId(rs.getInt("vulnerabilidad_id"));
                    dto.setVulnerabilityName(rs.getString("vulnerabilidad_nombre"));
                    
                    // Qué hace: Convierte el ENUM a ID para el frontend.
                    String gradeStr = rs.getString("grado");
                    dto.setVulnerabilityGradeName(gradeStr);
                    dto.setVulnerabilityGradeId(gradeStrToId(gradeStr));
                    // Qué hace: Añade el DTO a la lista de retorno.
                    lista.add(dto);
                }
            }
        }
        // Qué hace: Retorna la lista resultante de vulnerabilidades del riesgo.
        return lista;
    }

    /*
     * Qué hace (la acción): Guarda la asociación de un factor de vulnerabilidad a un riesgo con su grado de impacto.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - gradeIdToStr: Traduce el ID del combo UI al ENUM literal de la base de datos ("Muy Alta", "Alta", "Media", "Baja").
     * Para qué se usa (el propósito): Registrar la evaluación de vulnerabilidad de un factor de riesgo en la base de datos.
     */
    public boolean crearVulnerabilidad(FactorVulnerabilidadDTO dto) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Insertar una asociación de vulnerabilidad a un riesgo.
        // - Tablas participantes: factor_riesgo_vulnerabilidad.
        String sql = "INSERT INTO factor_riesgo_vulnerabilidad (grado, riesgo_id, vulnerabilidad_id) VALUES (?, ?, ?)";
        // Qué hace: Abre la conexión a la base de datos y compila el statement.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Vincula los parámetros mapeando el ID numérico de grado a su cadena ENUM.
            ps.setString(1, gradeIdToStr(dto.getVulnerabilityGradeId()));
            ps.setInt(2, dto.getRiskFactorId());
            ps.setInt(3, dto.getVulnerabilityId());
            // Qué hace: Ejecuta el insert y retorna verdadero si afectó filas.
            return ps.executeUpdate() > 0;
        }
    }

    /*
     * Qué hace (la acción): Modifica el grado y la tipificación de la vulnerabilidad en un factor de riesgo específico.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - UPDATE frv: Actualiza la vulnerabilidad por ID.
     * Para qué se usa (el propósito): Modificar la evaluación de vulnerabilidad en la base de datos.
     */
    public boolean actualizarVulnerabilidad(int id, FactorVulnerabilidadDTO dto) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Modificar el grado o catálogo de la vulnerabilidad.
        // - Tablas participantes: factor_riesgo_vulnerabilidad.
        // - Filtros aplicados: WHERE id = ? (id de la vulnerabilidad del riesgo).
        String sql = "UPDATE factor_riesgo_vulnerabilidad SET grado = ?, vulnerabilidad_id = ? WHERE id = ?";
        // Qué hace: Abre la conexión a base de datos y compila el PreparedStatement.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Vincula los campos traduciendo el ID de grado a la cadena literal ENUM.
            ps.setString(1, gradeIdToStr(dto.getVulnerabilityGradeId()));
            ps.setInt(2, dto.getVulnerabilityId());
            ps.setInt(3, id);
            // Qué hace: Ejecuta la consulta de actualización.
            return ps.executeUpdate() > 0;
        }
    }

    /*
     * Qué hace (la acción): Elimina físicamente la asociación de vulnerabilidad de un riesgo.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - DELETE FROM: Elimina el registro por ID.
     * Para qué se usa (el propósito): Quitar una vulnerabilidad del factor de riesgo evaluado.
     */
    public boolean eliminarVulnerabilidad(int id) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Eliminar una vulnerabilidad asociada al riesgo.
        // - Tablas participantes: factor_riesgo_vulnerabilidad.
        // - Filtros aplicados: WHERE id = ?.
        String sql = "DELETE FROM factor_riesgo_vulnerabilidad WHERE id = ?";
        // Qué hace: Abre la conexión a la base de datos y compila el PreparedStatement.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Vincula el ID y ejecuta la eliminación física.
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // =========================================================================
    // HELPERS DE CONVERSIÓN DE ENUM
    // =========================================================================

    /*
     * Qué hace (la acción): Traduce la cadena ENUM de base de datos a su respectivo ID numérico para el selector web.
     * Qué significa (conceptos, métodos, tipos involucrados): Asignación condicional que mapea "Muy Alta" -> 1, "Alta" -> 2, etc.
     * Para qué se usa (el propósito): Adecuar la representación del grado al formato de control dropdown en el frontend.
     */
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

    /*
     * Qué hace (la acción): Traduce el ID numérico del combo UI al String literal requerido por el ENUM en la base de datos MySQL.
     * Qué significa (conceptos, métodos, tipos involucrados): Mapea 1 -> "Muy Alta", 2 -> "Alta", etc.
     * Para qué se usa (el propósito): Formatear los parámetros SQL de inserción y edición para que coincidan con la declaración ENUM física de MySQL.
     */
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
