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

    // Recupera la información básica de precarga de un plan familiar (apellidos y tipo de familia) por su ID
    public Modelo.DTO.IdentificacionPlanDTO obtenerDetallePlan(int id) throws SQLException {
        // Inicializa la variable DTO de retorno como null
        Modelo.DTO.IdentificacionPlanDTO plan = null;
        // Consulta SQL con JOINs para traer los apellidos y el nombre del tipo de familia (ej. Vulnerable)
        String sql = "SELECT pf.id, ifa.apellidos_familia, tf.nombre AS tipo_familia_nombre "
                + "FROM planes_familiares pf "
                + "LEFT JOIN identificacion_familiar ifa ON pf.id = ifa.plan_id "
                + "LEFT JOIN tipos_familia tf ON pf.tipo_familia_id = tf.id "
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
                }
            }
        }
        // Devuelve el DTO (será null si el plan no fue encontrado)
        return plan;
    }

    // Actualiza los campos detallados de la vivienda en la tabla identificacion_familiar para un plan familiar
    public void actualizarIdentificacion(int planId, Modelo.DTO.ActualizarIdentificacionDTO dto) throws SQLException {
        // Sentencia SQL para actualizar la información de identificación de la vivienda
        String sql = "UPDATE identificacion_familiar SET "
                + "apellidos_familia = ?, "
                + "nombre_familia = ?, "
                + "direccion = ?, "
                + "barrio_comuna_localidad = ?, "
                + "telefono_fijo = ?, "
                + "calidad_vivienda_id = ?, "
                + "sector_id = ? "
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
            // Asigna el ID del plan familiar en progreso como condición del WHERE
            ps.setInt(8, planId);
            // Ejecuta el comando de actualización en la base de datos
            ps.executeUpdate();
        }
    }
}