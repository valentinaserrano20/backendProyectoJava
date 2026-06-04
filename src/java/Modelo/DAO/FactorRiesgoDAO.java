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

    // Qué hace: Obtiene los detalles de un factor de riesgo por su ID único.
    // Por qué se implementó: Permite cargar los datos de un riesgo en el modal o formulario de edición.
    // Qué problema resuelve: Recupera la información exacta de un registro para su visualización o modificación.
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

    // Qué hace: Obtiene todos los factores de riesgo colgados de un plan familiar.
    // Por qué se implementó: Permite listar los riesgos en la grilla visual del plan de emergencia de una familia.
    // Qué problema resuelve: Filtra y retorna los registros que pertenecen exclusivamente al plan del voluntario.
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

    // Qué hace: Obtiene un subconjunto de factores de riesgo de un plan familiar aplicando paginación.
    // Por qué se implementó: Permite a la grilla de riesgos del frontend cargar por páginas.
    // Qué problema resuelve: Controla el flujo de datos optimizando el rendimiento de red.
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

    // Qué hace: Obtiene la cantidad total de factores de riesgo registrados para un plan.
    // Por qué se implementó: Se requiere para calcular el total de páginas en el componente de paginación del frontend.
    // Qué problema resuelve: Suministra el total de registros de forma ligera con un COUNT.
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

    // Qué hace: Inserta un nuevo factor de riesgo en la base de datos.
    // Por qué se implementó: Permite guardar el formulario inicial de agregar riesgo de la UI.
    // Qué problema resuelve: Inserta el registro de manera parametrizada y retorna true si fue exitoso.
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

    // Qué hace: Actualiza los campos de un factor de riesgo.
    // Por qué se implementó: Permite guardar las modificaciones realizadas en el acordeón "Datos del riesgo".
    // Qué problema resuelve: Ejecuta la consulta de UPDATE sobre los datos específicos del registro padre.
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

    // Qué hace: Elimina un factor de riesgo por su ID, eliminando previamente de forma transaccional sus dependencias.
    // Por qué se implementó: Habilita el botón "Eliminar" de la UI limpiando relaciones hijas para evitar errores de llave foránea.
    // Qué problema resuelve: Ejecuta un borrado ACID en cascada.
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

    // Qué hace: Obtiene la información estructurada de una acción de reducción específica por su ID.
    // Por qué se implementó: Permite cargar los datos de una acción para editarla.
    // Qué problema resuelve: Recupera los datos de la acción uniendo el integrante asignado como responsable.
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

    // Qué hace: Obtiene la lista completa de acciones de reducción asociadas a un factor de riesgo.
    // Por qué se implementó: Alimenta la pestaña de tareas de mitigación del riesgo en la UI.
    // Qué problema resuelve: Recupera todas las tareas vinculadas a un riesgo familiar particular de manera unificada.
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

    // Qué hace: Inserta una nueva acción de reducción en la base de datos.
    // Por qué se implementó: Permite guardar una nueva tarea del plan de acción para reducir un riesgo.
    // Qué problema resuelve: Registra la tarea asociando un familiar responsable (soportando responsables nulos).
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

    // Qué hace: Actualiza los campos de una acción de reducción específica.
    // Por qué se implementó: Habilita la corrección de tareas, plazos o responsables en la UI.
    // Qué problema resuelve: Persiste los cambios de la acción controlando la nulidad del responsable.
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

    // Qué hace: Elimina una acción de reducción por su ID único.
    // Por qué se implementó: Permite descartar una tarea del plan de mitigación.
    // Qué problema resuelve: Elimina físicamente el registro correspondiente en la tabla.
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

    // Qué hace: Obtiene la información detallada de la vulnerabilidad de un riesgo por su ID único.
    // Por qué se implementó: Soporta la visualización y edición del grado de vulnerabilidad.
    // Qué problema resuelve: Recupera la correspondencia de grado y catálogo de vulnerabilidad relacional.
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

    // Qué hace: Obtiene la lista de vulnerabilidades asignadas a un factor de riesgo particular.
    // Por qué se implementó: Alimenta la visualización de vulnerabilidades del riesgo en la UI.
    // Qué problema resuelve: Permite recuperar de forma legible el catálogo de padecimientos de infraestructura del riesgo.
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

    // Qué hace: Registra una nueva vulnerabilidad asociada a un riesgo.
    // Por qué se implementó: Permite asociar ítems del catálogo de vulnerabilidades al riesgo evaluado.
    // Qué problema resuelve: Escribe la relación traduciendo el ID de grado a la cadena ENUM correspondiente de base de datos.
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

    // Qué hace: Actualiza la vulnerabilidad asociada a un riesgo.
    // Por qué se implementó: Permite modificar el grado o tipo de vulnerabilidad desde el acordeón de la UI.
    // Qué problema resuelve: Actualiza los valores traduciendo los IDs de la interfaz hacia el ENUM de base de datos.
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

    // Qué hace: Elimina una relación de vulnerabilidad de un riesgo por su ID único.
    // Por qué se implementó: Permite desvincular una vulnerabilidad del riesgo en la UI.
    // Qué problema resuelve: Elimina físicamente la fila correspondiente de la tabla de unión.
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

    // Qué hace: Helper para convertir la cadena descriptiva de grado en MySQL a su correspondiente identificador numérico de la interfaz.
    // Por qué existe: Asegura que el frontend reciba un ID numérico que pueda controlar de manera limpia en su selector dropdown.
    // Qué problema resuelve: Resuelve la disparidad de representación entre el String ENUM de base de datos y el modelo DTO de la vista.
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

    // Qué hace: Helper para convertir el identificador de grado del DTO a la cadena descriptiva ENUM requerida por la base de datos.
    // Por qué existe: Permite que las consultas de inserción y actualización utilicen el formato literal exacto del ENUM en MySQL.
    // Qué problema resuelve: Previene excepciones de inserción por incompatibilidad de tipo ENUM en la base de datos.
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
