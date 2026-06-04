package Modelo.DAO;

import Modelo.Config.Conexion;
import Modelo.DTO.RegistroPlanDTO;
import java.sql.*;

public class PlanFamiliarDAO {

    // Sirve para: Crear la cabecera de un nuevo plan familiar y su respectiva ficha de identificación en una transacción atómica.
    // Qué hace: Realiza dos sentencias INSERT dentro de una transacción ACID, recuperando la clave primaria generada del plan.
    // Explicación de consultas SQL:
    // - Inserción 1: INSERT INTO planes_familiares (enviado, voluntario_id, estado_id) VALUES (?, ?, ?).
    //   * Inserta la cabecera inicial del plan asignándolo al voluntario creador y poniéndolo en estado 2 (Pendiente).
    // - Inserción 2: INSERT INTO identificacion_familiar (nombre_familia, apellidos_familia, direccion, barrio_comuna_localidad, consentimiento_datos, plan_id, tipo_zona_id) VALUES (?, ?, ?, ?, ?, ?, ?).
    //   * Inserta el registro básico inicial de identificación familiar enlazado al plan recién creado.
    public int registrarPasoInicial(RegistroPlanDTO dto) throws SQLException {
        String sqlPlan = "INSERT INTO planes_familiares (enviado, voluntario_id, estado_id) VALUES (?, ?, ?)";
        String sqlIdentificacion = "INSERT INTO identificacion_familiar "
                + "(nombre_familia, apellidos_familia, direccion, barrio_comuna_localidad, consentimiento_datos, plan_id, tipo_zona_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        Connection con = null;
        PreparedStatement psPlan = null;
        PreparedStatement psIdent = null;
        ResultSet rsKeys = null;
        int idPlanGenerado = -1;

        try {
            // Qué hace: Obtiene la conexión física al motor de base de datos MySQL.
            con = Conexion.obtener();
            // Qué hace: Desactiva el auto-commit manual para iniciar una transacción controlada (ACID).
            // Por qué existe: Asegura que si falla la creación de la identificación familiar, la cabecera tampoco se persista (consistencia).
            con.setAutoCommit(false); 

            // Qué hace: Prepara el statement para la cabecera y configura recuperar las llaves primarias autogeneradas.
            psPlan = con.prepareStatement(sqlPlan, Statement.RETURN_GENERATED_KEYS);
            // Qué hace: Enlaza los parámetros de enviado (falso/borrador), voluntario_id y el estado_id inicial (2 = Pendiente).
            psPlan.setBoolean(1, false); 
            psPlan.setInt(2, dto.getUserId());
            psPlan.setInt(3, 2); 
            // Qué hace: Ejecuta la inserción de la cabecera en la base de datos.
            psPlan.executeUpdate();

            // Qué hace: Obtiene las llaves primarias autogeneradas de la base de datos.
            rsKeys = psPlan.getGeneratedKeys();
            if (rsKeys.next()) {
                // Qué hace: Recupera el ID numérico asignado de forma automática por la base de datos.
                idPlanGenerado = rsKeys.getInt(1);
            } else {
                throw new SQLException("Incapaz de recuperar el ID generado para planes_familiares.");
            }

            // Qué hace: Prepara el statement para la inserción en identificacion_familiar.
            psIdent = con.prepareStatement(sqlIdentificacion);
            // Qué hace: Enlaza los campos de nombres familiares temporales, el consentimiento de tratamiento de datos personales, el ID del plan generado y la zona.
            psIdent.setString(1, "Familia " + dto.getLastNames());
            psIdent.setString(2, dto.getLastNames());
            psIdent.setString(3, "Por definir"); // Asigna un valor predeterminado para evitar restricciones NOT NULL
            psIdent.setString(4, "Por definir"); 
            psIdent.setBoolean(5, true);         
            psIdent.setInt(6, idPlanGenerado);
            psIdent.setInt(7, dto.getZoneId());
            // Qué hace: Ejecuta la inserción del registro relacional de identificación.
            psIdent.executeUpdate();

            // Qué hace: Confirma y consolida todos los cambios de la transacción de forma definitiva en la base de datos.
            con.commit(); 
            // Qué hace: Retorna el identificador del plan familiar generado.
            return idPlanGenerado;

        } catch (SQLException e) {
            // Qué hace: Si ocurre un error, cancela los cambios realizados en la transacción.
            // Por qué existe: Restaura la base de datos a su estado anterior en caso de excepciones (rollback).
            if (con != null) {
                try { con.rollback(); } catch (SQLException ex) { System.err.println(ex.getMessage()); }
            }
            throw e;
        } finally {
            // Qué hace: Cierra de forma explícita todos los recursos abiertos para liberar memoria y conexiones.
            if (rsKeys != null) rsKeys.close();
            if (psPlan != null) psPlan.close();
            if (psIdent != null) psIdent.close();
            if (con != null) con.close();
        }
    }

    // Sirve para: Recuperar la información detallada de precarga del plan familiar por su ID único.
    // Qué hace: Realiza un SELECT con múltiples LEFT JOINs en la BD para traer apellidos, tipo de familia, dirección, zona, sector, comuna, teléfono y calidad de vivienda.
    // Explicación de consulta SQL:
    // - Información buscada: pf.id, pf.estado_id (status_plan_id), ifa.apellidos_familia, tf.nombre (tipo_familia_nombre), ifa.direccion, ifa.barrio_comuna_localidad, ifa.telefono_fijo, ifa.calidad_vivienda_id, ifa.sector_id, ifa.tipo_zona_id y u.organizacion_id (city_id).
    // - Tablas participantes: planes_familiares pf (principal), identificacion_familiar ifa (ficha vivienda), tipos_familia tf (catálogo familiar), usuarios u (voluntario responsable).
    // - Relaciones (JOINs):
    //   1. LEFT JOIN identificacion_familiar ifa ON pf.id = ifa.plan_id (cruza con la ficha de identificación).
    //   2. LEFT JOIN tipos_familia tf ON pf.tipo_familia_id = tf.id (cruza con catálogo de tipos de familia).
    //   3. LEFT JOIN usuarios u ON pf.voluntario_id = u.id (cruza con el usuario que cargó el plan).
    // - Filtros aplicados: pf.id = ? (el identificador único del plan familiar).
    public Modelo.DTO.IdentificacionPlanDTO obtenerDetallePlan(int id) throws SQLException {
        Modelo.DTO.IdentificacionPlanDTO plan = null;
        String sql = "SELECT pf.id, pf.estado_id AS status_plan_id, ifa.apellidos_familia, tf.nombre AS tipo_familia_nombre, "
                + "ifa.direccion, ifa.barrio_comuna_localidad, ifa.telefono_fijo, "
                + "ifa.calidad_vivienda_id, ifa.sector_id, ifa.tipo_zona_id, "
                + "u.organizacion_id AS city_id "
                + "FROM planes_familiares pf "
                + "LEFT JOIN identificacion_familiar ifa ON pf.id = ifa.plan_id "
                + "LEFT JOIN tipos_familia tf ON pf.tipo_familia_id = tf.id "
                + "LEFT JOIN usuarios u ON pf.voluntario_id = u.id "
                + "WHERE pf.id = ?";

        // Qué hace: Obtiene la conexión y compila el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Configura el ID del plan familiar en la consulta.
            ps.setInt(1, id);
            // Qué hace: Ejecuta la consulta y lee el ResultSet.
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Qué hace: Instancia el objeto DTO y mapea los resultados del ResultSet.
                    plan = new Modelo.DTO.IdentificacionPlanDTO(
                        rs.getInt("id"),
                        rs.getString("apellidos_familia"),
                        rs.getString("tipo_familia_nombre")
                    );
                    
                    plan.setAddress(rs.getString("direccion"));
                    plan.setSectorName(rs.getString("barrio_comuna_localidad"));
                    plan.setLandlinePhone(rs.getString("telefono_fijo"));
                    plan.setHousingQualityId(rs.getInt("calidad_vivienda_id"));
                    plan.setSectorId(rs.getInt("sector_id"));
                    plan.setZoneId(rs.getInt("tipo_zona_id"));
                    plan.setCityId(rs.getInt("city_id"));
                    plan.setDepartmentId(1); // Departamento por defecto de la seccional (Santander = ID 1)
                    plan.setStatusPlanId(rs.getInt("status_plan_id"));
                }
            }

            // Qué hace: Si el plan existe, consulta la última observación de rechazo o aprobación registrada.
            // Explicación de consulta SQL:
            // - Información buscada: observaciones de seguimiento.
            // - Tablas participantes: seguimiento_plan.
            // - Filtros aplicados: plan_id = ? (el plan en progreso).
            // - Ordenamiento: Ordenado por id descendente, limitado a 1 para traer el último registro.
            if (plan != null) {
                String sqlSeguimiento = "SELECT observaciones FROM seguimiento_plan WHERE plan_id = ? ORDER BY id DESC LIMIT 1";
                try (PreparedStatement psSeg = con.prepareStatement(sqlSeguimiento)) {
                    psSeg.setInt(1, id);
                    try (ResultSet rsSeg = psSeg.executeQuery()) {
                        if (rsSeg.next()) {
                            plan.setComentary(rsSeg.getString("observaciones"));
                        }
                    }
                }
            }
        }
        return plan;
    }

    // Sirve para: Actualizar la información detallada de la vivienda en el plan familiar.
    // Qué hace: Ejecuta una sentencia UPDATE sobre la tabla identificacion_familiar.
    // Explicación de consulta SQL:
    // - Operación: Actualización de columnas.
    // - Tabla afectada: identificacion_familiar.
    // - Columnas modificadas: apellidos_familia, nombre_familia, direccion, barrio_comuna_localidad, telefono_fijo, calidad_vivienda_id, sector_id, tipo_zona_id.
    // - Filtros aplicados: plan_id = ? (identificador único del plan).
    public void actualizarIdentificacion(int planId, Modelo.DTO.ActualizarIdentificacionDTO dto) throws SQLException {
        String sql = "UPDATE identificacion_familiar SET "
                + "apellidos_familia = ?, "
                + "nombre_familia = ?, "
                + "direccion = ?, "
                + "barrio_comuna_localidad = ?, "
                + "telefono_fijo = ?, "
                + "calidad_vivienda_id = ?, "
                + "sector_id = ?, "
                + "tipo_zona_id = ? "
                + "WHERE plan_id = ?";

        // Qué hace: Obtiene la conexión JDBC y prepara la actualización parametrizada.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Enlaza secuencialmente todos los parámetros correspondientes del DTO.
            ps.setString(1, dto.getLastNames());
            ps.setString(2, "Familia " + dto.getLastNames());
            ps.setString(3, dto.getAddress());
            ps.setString(4, dto.getSectorName());
            ps.setString(5, dto.getLandlinePhone());
            ps.setInt(6, dto.getHousingQualityId());
            ps.setInt(7, dto.getSectorId());
            ps.setInt(8, dto.getZoneId());
            ps.setInt(9, planId);
            // Qué hace: Ejecuta la actualización física en base de datos.
            ps.executeUpdate();
        }
    }

    // Sirve para: Validar si un usuario tiene permisos de acceso sobre un plan familiar específico.
    // Qué hace: Consulta el rol del usuario y el voluntario_id del plan; autoriza si es supervisor o si es el voluntario creador.
    // Explicación de consultas SQL:
    // - Consulta 1: SELECT rol_id FROM usuarios WHERE id = ?.
    //   * Busca el identificador del rol del usuario de sesión en la tabla usuarios.
    // - Consulta 2: SELECT voluntario_id FROM planes_familiares WHERE id = ?.
    //   * Busca el ID del voluntario que creó y es responsable de ese plan familiar.
    // - Filtros aplicados: id = ? (clave primaria del usuario) en la primera y id = ? (clave primaria del plan) en la segunda.
    public boolean verificarAcceso(int planId, int usuarioId) throws SQLException {
        String sqlUsuario = "SELECT rol_id FROM usuarios WHERE id = ?";
        String sqlPlan = "SELECT voluntario_id FROM planes_familiares WHERE id = ?";
        // Qué hace: Abre la conexión a la base de datos y compila los statements para la consulta parametrizada.
        try (Connection con = Conexion.obtener();
             PreparedStatement psUser = con.prepareStatement(sqlUsuario);
             PreparedStatement psPlan = con.prepareStatement(sqlPlan)) {
            // Qué hace: Enlaza el ID de usuario.
            psUser.setInt(1, usuarioId);
            int rolId = -1;
            // Qué hace: Ejecuta la consulta de rol.
            try (ResultSet rsUser = psUser.executeQuery()) {
                if (rsUser.next()) {
                    rolId = rsUser.getInt("rol_id");
                }
            }
            // Qué hace: Si no se encuentra el usuario, deniega el acceso.
            if (rolId == -1) {
                return false;
            }
            // Qué hace: Si el usuario posee rol_id = 2 (Supervisor_Admin), se le concede acceso inmediato global.
            if (rolId == 2) {
                return true;
            }
            // Qué hace: Enlaza el ID del plan familiar en el segundo statement.
            psPlan.setInt(1, planId);
            int voluntarioId = -1;
            // Qué hace: Ejecuta la consulta para determinar el voluntario creador.
            try (ResultSet rsPlan = psPlan.executeQuery()) {
                if (rsPlan.next()) {
                    voluntarioId = rsPlan.getInt("voluntario_id");
                }
            }
            // Qué hace: Otorga acceso únicamente si el usuario solicitante es el creador del plan.
            return voluntarioId == usuarioId;
        }
    }

    // Sirve para: Determinar si un plan familiar ya posee integrantes cargados en el censo.
    // Qué hace: Ejecuta una consulta COUNT en la tabla integrantes filtrada por el ID del plan.
    // Explicación de consulta SQL:
    // - Información buscada: Cantidad de registros (total).
    // - Tablas participantes: integrantes.
    // - Filtros aplicados: plan_id = ? (el plan consultado).
    public boolean tieneIntegrantes(int planId) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM integrantes WHERE plan_id = ?";
        // Qué hace: Obtiene la conexión JDBC y prepara el statement de conteo.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Enlaza el planId al marcador posicional.
            ps.setInt(1, planId);
            // Qué hace: Ejecuta la consulta de conteo.
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Qué hace: Retorna true si el conteo es mayor que cero.
                    return rs.getInt("total") > 0;
                }
            }
        }
        return false;
    }

    // Sirve para: Contar el número total de planes asociados a un voluntario (para cálculo de paginación).
    // Qué hace: Ejecuta una consulta SELECT COUNT(*) en planes_familiares.
    // Explicación de consulta SQL:
    // - Información buscada: Cantidad total de registros.
    // - Tablas participantes: planes_familiares.
    // - Filtros aplicados: voluntario_id = ? (el ID del voluntario consultado).
    public int contarPlanesPorVoluntario(int voluntarioId) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM planes_familiares WHERE voluntario_id = ?";
        // Qué hace: Obtiene la conexión y compila el statement.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Enlaza el voluntarioId.
            ps.setInt(1, voluntarioId);
            // Qué hace: Ejecuta el conteo.
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        }
        return 0;
    }

    // Sirve para: Obtener una lista de planes familiares pertenecientes a un voluntario, de forma paginada y cruzando catálogos relacionales.
    // Qué hace: Hace un SELECT con múltiples LEFT JOINs a las tablas de identificacion_familiar, estados_plan, tipos_familia y organizaciones.
    // Explicación de consulta SQL:
    // - Información buscada: El ID del plan, los apellidos familiares (ifa.apellidos_familia), el ID de estado del plan, el nombre legible del estado (ep.nombre), el ID y nombre del tipo de familia (tf.nombre), el departamento (org.seccional), la ciudad (org.nombre) y la fecha formateada de actualización (date_create).
    // - Tablas participantes: planes_familiares pf (principal), identificacion_familiar ifa (ficha), estados_plan ep (catálogo estados), tipos_familia tf (catálogo familiar), usuarios u (voluntario), organizaciones org (catálogo organizaciones).
    // - Relaciones (JOINs):
    //   1. LEFT JOIN identificacion_familiar ifa ON pf.id = ifa.plan_id.
    //   2. LEFT JOIN estados_plan ep ON pf.estado_id = ep.id.
    //   3. LEFT JOIN tipos_familia tf ON pf.tipo_familia_id = tf.id.
    //   4. LEFT JOIN usuarios u ON pf.voluntario_id = u.id.
    //   5. LEFT JOIN organizaciones org ON u.organizacion_id = org.id.
    // - Filtros aplicados: pf.voluntario_id = ? (el creador del plan).
    // - Ordenamiento y paginación: ORDER BY pf.updated_at DESC, pf.id DESC LIMIT ? OFFSET ?.
    public java.util.List<java.util.Map<String, Object>> listarPlanesPorVoluntario(int voluntarioId, int limit, int offset) throws SQLException {
        java.util.List<java.util.Map<String, Object>> planes = new java.util.ArrayList<>();
        String sql = "SELECT pf.id, "
                + "COALESCE(ifa.apellidos_familia, 'Por definir') AS last_names, "
                + "pf.estado_id AS status_id, "
                + "COALESCE(ep.nombre, 'Pendiente') AS status, "
                + "COALESCE(pf.tipo_familia_id, 3) AS family_type_id, "
                + "COALESCE(tf.nombre, 'Por Definir') AS family_type, "
                + "COALESCE(org.seccional, 'Santander') AS department, "
                + "COALESCE(org.nombre, 'Sin definir') AS city, "
                + "DATE_FORMAT(pf.updated_at, '%d/%m/%Y %H:%i') AS date_create "
                + "FROM planes_familiares pf "
                + "LEFT JOIN identificacion_familiar ifa ON pf.id = ifa.plan_id "
                + "LEFT JOIN estados_plan ep ON pf.estado_id = ep.id "
                + "LEFT JOIN tipos_familia tf ON pf.tipo_familia_id = tf.id "
                + "LEFT JOIN usuarios u ON pf.voluntario_id = u.id "
                + "LEFT JOIN organizaciones org ON u.organizacion_id = org.id "
                + "WHERE pf.voluntario_id = ? "
                + "ORDER BY pf.updated_at DESC, pf.id DESC "
                + "LIMIT ? OFFSET ?";

        // Qué hace: Abre la conexión a la base de datos y compila el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Enlaza el voluntarioId, límite y desplazamiento.
            ps.setInt(1, voluntarioId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            // Qué hace: Ejecuta la consulta y lee el ResultSet.
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Qué hace: Mapea los resultados para retornar una lista flexible de mapas.
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", rs.getInt("id"));
                    map.put("last_names", rs.getString("last_names"));
                    map.put("status_id", rs.getInt("status_id"));
                    map.put("status", rs.getString("status"));
                    map.put("family_type_id", rs.getInt("family_type_id"));
                    map.put("family_type", rs.getString("family_type"));
                    map.put("department", rs.getString("department"));
                    map.put("city", rs.getString("city"));
                    map.put("date_create", rs.getString("date_create"));
                    planes.add(map);
                }
            }
        }
        // Retorna la lista resultante
        return planes;
    }

    // Sirve para: Obtener el identificador del rol asignado a un usuario específico en el sistema.
    // Qué hace: Consulta el campo rol_id de la tabla usuarios filtrando por el ID de usuario proporcionado.
    // Explicación de consulta SQL:
    // - Información buscada: El campo rol_id del usuario.
    // - Tablas participantes: usuarios.
    // - Filtros aplicados: id = ? (clave primaria del usuario).
    public int obtenerRolUsuario(int usuarioId) throws SQLException {
        String sql = "SELECT rol_id FROM usuarios WHERE id = ?";
        // Qué hace: Obtiene la conexión a la base de datos MySQL y compila el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Enlaza el ID del usuario como parámetro de filtro.
            ps.setInt(1, usuarioId);
            // Qué hace: Ejecuta la consulta y lee el ResultSet.
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Qué hace: Retorna el ID numérico del rol obtenido.
                    return rs.getInt("rol_id");
                }
            }
        }
        // Qué hace: Retorna -1 si no se encontró ningún usuario con ese ID.
        return -1;
    }

    // Qué hace: Cuenta el número total de planes familiares registrados de forma global en el censo que ya fueron enviados por el voluntario.
    // Por qué existe: Permite calcular la paginación correcta para el supervisor excluyendo borradores no enviados (estados 2 y 3).
    // Qué problema resuelve: Evita que el supervisor visualice planes pendientes o en desarrollo que el voluntario aún no ha remitido.
    public int contarTodosLosPlanes() throws SQLException {
        // Qué hace: Define la consulta SQL para contar el total de planes que no estén en estado borrador (2 y 3).
        // Por qué existe: Garantiza que los borradores del voluntario permanezcan ocultos al supervisor.
        // Qué problema resuelve: Evita mostrar información incompleta o no autorizada para revisión.
        // Explicación de consulta SQL:
        // - Información buscada: Total de planes familiares que no están en borrador (estado_id diferente de 2 y 3).
        // - Tablas participantes: planes_familiares.
        // - Filtros aplicados: estado_id NOT IN (2, 3) para excluir planes no enviados.
        String sql = "SELECT COUNT(*) AS total FROM planes_familiares WHERE estado_id NOT IN (2, 3)";
        // Qué hace: Abre una conexión limpia y prepara el statement para ejecutar la consulta SQL.
        // Por qué existe: Permite la interacción segura y eficiente con la base de datos MySQL local.
        // Qué problema resuelve: Libera automáticamente los recursos para evitar fugas de memoria.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            // Qué hace: Evalúa si hay un resultado de conteo disponible.
            // Por qué existe: Permite recuperar el valor numérico entero retornado por la base de datos.
            // Qué problema resuelve: Evita excepciones de puntero nulo o errores al procesar el ResultSet.
            if (rs.next()) {
                return rs.getInt("total");
            }
        }
        return 0;
    }

    // Qué hace: Retorna todos los planes de emergencia familiar en el sistema de forma paginada para supervisión.
    // Por qué existe: Permite al supervisor visualizar la bandeja global de planes y los últimos planes recibidos.
    // Qué problema resuelve: Filtra los planes no enviados (estados 2 y 3) y recupera el nombre del voluntario responsable.
    public java.util.List<java.util.Map<String, Object>> listarTodosLosPlanes(int limit, int offset) throws SQLException {
        // Qué hace: Inicializa la lista que contendrá los mapas de datos de cada plan familiar.
        // Por qué existe: Provee el contenedor estructurado para retornar la información al servicio y luego al controlador JS.
        // Qué problema resuelve: Evita retornos de objetos nulos en caso de que la consulta no devuelva resultados.
        java.util.List<java.util.Map<String, Object>> planes = new java.util.ArrayList<>();
        // Qué hace: Define la consulta SQL para recuperar los campos de identificación familiar, estado y voluntario de forma cruzada.
        // Por qué existe: Consolida en una sola llamada SQL la información de familias, seccionales, ciudades, estados y nombres de voluntarios.
        // Qué problema resuelve: Reduce la latencia al realizar joins eficientes y evitar consultas N+1 en bucles de java.
        // Explicación de consulta SQL:
        // - Información buscada: Listado de planes familiares con sus datos de familia, estado, tipo de familia, departamento, ciudad y el responsable.
        // - Tablas participantes: planes_familiares (pf), identificacion_familiar (ifa), estados_plan (ep), tipos_familia (tf), usuarios (u), organizaciones (org).
        // - Filtros aplicados: pf.estado_id NOT IN (2, 3) para omitir planes en borrador.
        // - Ordenamiento y paginación: Ordenado por fecha de actualización descendente y paginado con LIMIT/OFFSET.
        String sql = "SELECT pf.id, "
                + "COALESCE(ifa.apellidos_familia, 'Por definir') AS last_names, "
                + "pf.estado_id AS status_id, "
                + "COALESCE(ep.nombre, 'Pendiente') AS status, "
                + "COALESCE(pf.tipo_familia_id, 3) AS family_type_id, "
                + "COALESCE(tf.nombre, 'Por Definir') AS family_type, "
                + "COALESCE(org.seccional, 'Santander') AS department, "
                + "COALESCE(org.nombre, 'Sin definir') AS city, "
                + "CONCAT(u.nombre, ' ', COALESCE(u.apellido, '')) AS responsable, "
                + "DATE_FORMAT(pf.updated_at, '%d/%m/%Y %H:%i') AS date_create "
                + "FROM planes_familiares pf "
                + "LEFT JOIN identificacion_familiar ifa ON pf.id = ifa.plan_id "
                + "LEFT JOIN estados_plan ep ON pf.estado_id = ep.id "
                + "LEFT JOIN tipos_familia tf ON pf.tipo_familia_id = tf.id "
                + "LEFT JOIN usuarios u ON pf.voluntario_id = u.id "
                + "LEFT JOIN organizaciones org ON u.organizacion_id = org.id "
                + "WHERE pf.estado_id NOT IN (2, 3) "
                + "ORDER BY pf.updated_at DESC, pf.id DESC "
                + "LIMIT ? OFFSET ?";

        // Qué hace: Abre la conexión a la base de datos y compila el PreparedStatement parametrizado.
        // Por qué existe: Asegura la correcta inyección de parámetros para prevenir ataques de SQL Injection.
        // Qué problema resuelve: Gestiona la concurrencia de conexiones de manera eficiente cerrando los recursos abiertos al terminar.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Reemplaza los marcadores de la consulta SQL con los parámetros limit y offset recibidos.
            // Por qué existe: Define el tamaño de página y el desplazamiento de los registros para la paginación.
            // Qué problema resuelve: Controla el flujo de la consulta para no desbordar el consumo de memoria con listados completos.
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            // Qué hace: Ejecuta la consulta SQL y recupera el conjunto de resultados ResultSet.
            // Por qué existe: Provee acceso secuencial a las filas coincidentes en la base de datos de planes familiares.
            // Qué problema resuelve: Facilita la iteración de los registros devueltos por el motor MySQL.
            try (ResultSet rs = ps.executeQuery()) {
                // Qué hace: Itera sobre cada fila del conjunto de resultados del ResultSet.
                // Por qué existe: Permite leer y procesar uno a uno los registros de planes cargados.
                // Qué problema resuelve: Convierte filas tabulares relacionales en objetos HashMap de java.
                while (rs.next()) {
                    // Qué hace: Crea un mapa de tipo clave-valor para representar el plan familiar.
                    // Por qué existe: Permite una estructura flexible para serialización posterior a formato JSON.
                    // Qué problema resuelve: Desvincula la estructura de la base de datos de la lógica de negocio final.
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    // Qué hace: Almacena cada columna del ResultSet dentro del mapa correspondiente.
                    // Por qué existe: Hace accesible la información de ID, apellidos, estados, geografía y responsable.
                    // Qué problema resuelve: Rellena los datos de la tarjeta que el frontend supervisor espera procesar.
                    map.put("id", rs.getInt("id"));
                    map.put("last_names", rs.getString("last_names"));
                    map.put("status_id", rs.getInt("status_id"));
                    map.put("status", rs.getString("status"));
                    map.put("family_type_id", rs.getInt("family_type_id"));
                    map.put("family_type", rs.getString("family_type"));
                    map.put("department", rs.getString("department"));
                    map.put("city", rs.getString("city"));
                    map.put("responsable", rs.getString("responsable"));
                    map.put("date_create", rs.getString("date_create"));
                    // Qué hace: Añade el mapa recién creado a la lista general de planes.
                    // Por qué existe: Permite consolidar todos los registros de la página actual para el retorno.
                    // Qué problema resuelve: Mantiene el orden y paginación en la entrega de resultados.
                    planes.add(map);
                }
            }
        }
        // Qué hace: Retorna la lista con los mapas de planes familiares.
        // Por qué existe: Completa el contrato del método retornando la colección al servicio solicitante.
        // Qué problema resuelve: Transfiere la información limpia y filtrada a las capas superiores.
        return planes;
    }
}