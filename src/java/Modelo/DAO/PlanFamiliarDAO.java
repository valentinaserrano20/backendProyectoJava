package Modelo.DAO;

import Modelo.Config.Conexion;
import Modelo.DTO.RegistroPlanDTO;
import java.sql.*;

public class PlanFamiliarDAO {

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
            con = Conexion.obtener();
            con.setAutoCommit(false); // Transacción ACID abierta

            // 1. Crear la cabecera del plan familiar
            psPlan = con.prepareStatement(sqlPlan, Statement.RETURN_GENERATED_KEYS);
            psPlan.setBoolean(1, false); // No enviado aún (Pendiente)
            psPlan.setInt(2, dto.getUserId());
            psPlan.setInt(3, 2); // Estado ID 2 = 'Pendiente' en tu tabla estados_plan
            psPlan.executeUpdate();

            rsKeys = psPlan.getGeneratedKeys();
            if (rsKeys.next()) {
                idPlanGenerado = rsKeys.getInt(1);
            } else {
                throw new SQLException("Incapaz de recuperar el ID generado para planes_familiares.");
            }

            // 2. Crear el registro de Identificación Familiar asociado
            psIdent = con.prepareStatement(sqlIdentificacion);
            psIdent.setString(1, "Familia " + dto.getLastNames());
            psIdent.setString(2, dto.getLastNames());
            psIdent.setString(3, "Por definir"); // Salvaguarda contra la restricción NOT NULL de la BD
            psIdent.setString(4, "Por definir"); // Salvaguarda contra la restricción NOT NULL de la BD
            psIdent.setBoolean(5, true);         // Consentimiento otorgado legalmente por el paso previo
            psIdent.setInt(6, idPlanGenerado);
            psIdent.setInt(7, dto.getZoneId());
            psIdent.executeUpdate();

            con.commit(); // Éxito absoluto, consolidamos en el disco
            return idPlanGenerado;

        } catch (SQLException e) {
            if (con != null) {
                try { con.rollback(); } catch (SQLException ex) { System.err.println(ex.getMessage()); }
            }
            throw e;
        } finally {
            if (rsKeys != null) rsKeys.close();
            if (psPlan != null) psPlan.close();
            if (psIdent != null) psIdent.close();
            if (con != null) con.close();
        }
    }

    // Sirve para: Recuperar la información detallada de precarga del plan familiar por su ID único
    // Qué hace: Realiza un SELECT con múltiples JOINs en la BD para traer apellidos, tipo de familia, dirección, zona, sector, comuna, teléfono y calidad de vivienda.
    // Por qué es importante: Provee toda la información que el frontend necesita desplegar en la pantalla de datos básicos para evitar pérdida de datos.
    public Modelo.DTO.IdentificacionPlanDTO obtenerDetallePlan(int id) throws SQLException {
        // Inicializa la variable DTO de retorno como null
        Modelo.DTO.IdentificacionPlanDTO plan = null;
        // Consulta SQL completa para traer los datos detallados de identificación
        String sql = "SELECT pf.id, ifa.apellidos_familia, tf.nombre AS tipo_familia_nombre, "
                + "ifa.direccion, ifa.barrio_comuna_localidad, ifa.telefono_fijo, "
                + "ifa.calidad_vivienda_id, ifa.sector_id, ifa.tipo_zona_id, "
                + "u.organizacion_id AS city_id "
                + "FROM planes_familiares pf "
                + "LEFT JOIN identificacion_familiar ifa ON pf.id = ifa.plan_id "
                + "LEFT JOIN tipos_familia tf ON pf.tipo_familia_id = tf.id "
                + "LEFT JOIN usuarios u ON pf.voluntario_id = u.id "
                + "WHERE pf.id = ?";

        // Abre la conexión y prepara la sentencia
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Reemplaza el marcador con el ID del plan familiar solicitado
            ps.setInt(1, id);
            // Ejecuta la consulta
            try (ResultSet rs = ps.executeQuery()) {
                // Si el plan existe en el sistema
                if (rs.next()) {
                    // Instancia el DTO con el ID, apellidos y el tipo de familia de las tablas cruzadas
                    plan = new Modelo.DTO.IdentificacionPlanDTO(
                        rs.getInt("id"),
                        rs.getString("apellidos_familia"),
                        rs.getString("tipo_familia_nombre")
                    );
                    
                    // Sirve para: Mapear las columnas de vivienda y geografía recuperadas al DTO
                    // Qué hace: Invoca a los setters del DTO con los resultados del ResultSet
                    // Por qué es importante: Garantiza que los campos editables no queden nulos y conserven su valor previo en la base de datos
                    plan.setAddress(rs.getString("direccion"));
                    plan.setSectorName(rs.getString("barrio_comuna_localidad"));
                    plan.setLandlinePhone(rs.getString("telefono_fijo"));
                    plan.setHousingQualityId(rs.getInt("calidad_vivienda_id"));
                    plan.setSectorId(rs.getInt("sector_id"));
                    plan.setZoneId(rs.getInt("tipo_zona_id"));
                    plan.setCityId(rs.getInt("city_id"));
                    plan.setDepartmentId(1); // Santander es el departamento predeterminado fijo (ID 1)
                }
            }
        }
        // Devuelve el DTO (será null si el plan no fue encontrado)
        return plan;
    }

    // Sirve para: Actualizar la información detallada de la vivienda en el plan familiar
    // Qué hace: Ejecuta un UPDATE sobre identificacion_familiar modificando dirección, barrio, teléfono, calidad de vivienda, sector y zona
    // Por qué es importante: Registra de manera permanente las modificaciones provistas por el usuario en la interfaz
    public void actualizarIdentificacion(int planId, Modelo.DTO.ActualizarIdentificacionDTO dto) throws SQLException {
        // Sentencia SQL para actualizar la información de identificación de la vivienda
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

        // Abre la conexión y prepara la actualización parametrizada
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Asigna los apellidos familiares (ej. García Pérez)
            ps.setString(1, dto.getLastNames());
            // Asigna el nombre formal del plan (ej. Familia García Pérez) para mantener consistencia
            ps.setString(2, "Familia " + dto.getLastNames());
            // Asigna la dirección o calle de la vivienda
            ps.setString(3, dto.getAddress());
            // Asigna la aclaración textual del barrio/comuna
            ps.setString(4, dto.getSectorName());
            // Asigna el teléfono de contacto fijo
            ps.setString(5, dto.getLandlinePhone());
            // Asigna el ID de calidad de vivienda relacional
            ps.setInt(6, dto.getHousingQualityId());
            // Asigna el ID del sector seleccionado
            ps.setInt(7, dto.getSectorId());
            // Asigna el ID del tipo de zona geográfica seleccionado (Urbana/Rural)
            ps.setInt(8, dto.getZoneId());
            // Asigna el ID del plan familiar en progreso como condición del WHERE
            ps.setInt(9, planId);
            // Ejecuta el comando de actualización en la base de datos
            ps.executeUpdate();
        }
    }

    // Sirve para: Validar si un usuario tiene permisos de acceso sobre un plan familiar específico
    // Qué hace: Consulta el rol del usuario y el voluntario_id del plan; autoriza si es supervisor o si es el voluntario creador
    // Por qué es importante: Evita que voluntarios no autorizados accedan o modifiquen información de otros planes familiares
    public boolean verificarAcceso(int planId, int usuarioId) throws SQLException {
        // Consulta el rol del usuario de la sesión
        String sqlUsuario = "SELECT rol_id FROM usuarios WHERE id = ?";
        // Consulta el id del voluntario que creó el plan
        String sqlPlan = "SELECT voluntario_id FROM planes_familiares WHERE id = ?";
        // Establece conexiones y declaraciones JDBC seguras
        try (Connection con = Conexion.obtener();
             PreparedStatement psUser = con.prepareStatement(sqlUsuario);
             PreparedStatement psPlan = con.prepareStatement(sqlPlan)) {
            // Asigna el parámetro del id de usuario
            psUser.setInt(1, usuarioId);
            // Variable para almacenar el ID de rol
            int rolId = -1;
            // Ejecuta la consulta del usuario
            try (ResultSet rsUser = psUser.executeQuery()) {
                // Si el usuario existe, extrae su rol
                if (rsUser.next()) {
                    rolId = rsUser.getInt("rol_id");
                }
            }
            // Si el rol no fue encontrado, se deniega el acceso
            if (rolId == -1) {
                return false;
            }
            // Si el rol es de Supervisor (2), tiene permiso total
            if (rolId == 2) {
                return true;
            }
            // Asigna el parámetro del id del plan
            psPlan.setInt(1, planId);
            // Variable para almacenar el ID del voluntario
            int voluntarioId = -1;
            // Ejecuta la consulta del plan
            try (ResultSet rsPlan = psPlan.executeQuery()) {
                // Si el plan existe, extrae su voluntario
                if (rsPlan.next()) {
                    voluntarioId = rsPlan.getInt("voluntario_id");
                }
            }
            // Compara si el voluntario creador coincide con el usuario consultado
            return voluntarioId == usuarioId;
        }
    }

    // Sirve para: Determinar si un plan familiar ya posee integrantes en el sistema
    // Qué hace: Realiza una cuenta de las filas asociadas en la tabla integrantes
    // Por qué es importante: El frontend requiere esta validación antes de permitir el análisis de factores de riesgo
    public boolean tieneIntegrantes(int planId) throws SQLException {
        // Consulta SQL para contar integrantes del plan
        String sql = "SELECT COUNT(*) AS total FROM integrantes WHERE plan_id = ?";
        // Establece conexión segura con try-with-resources
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Asigna el ID de plan consultado
            ps.setInt(1, planId);
            // Ejecuta la consulta
            try (ResultSet rs = ps.executeQuery()) {
                // Si obtiene resultado, retorna si el conteo es mayor a cero
                if (rs.next()) {
                    return rs.getInt("total") > 0;
                }
            }
        }
        // Retorna falso por defecto en caso de no encontrar registros
        return false;
    }
}