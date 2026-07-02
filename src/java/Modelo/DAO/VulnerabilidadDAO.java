package Modelo.DAO;

/*
 * Qué hace (la acción): Importa la conexión física de base de datos relacionales, las entidades y DTOs del test de vulnerabilidad familiar y las APIs de JDBC y colecciones de Java.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.Config.Conexion: Módulo de conexión a MySQL.
 *   - Modelo.Entidades.PreguntaTest: Entidad que encapsula una pregunta de evaluación (enunciado, orden, es_evaluable).
 *   - Modelo.DTO.RespuestaTestDTO: Objeto para transferir la respuesta booleana del usuario a cada pregunta.
 *   - java.sql.*: APIs estándar de interacción relacional JDBC de Java.
 * Para qué se usa (el propósito): Proveer el soporte de conexión y objetos para listar, calificar y persistir la evaluación de riesgo de la vivienda familiar.
 * Por qué es importante (el impacto o problema que resuelve): Permite al sistema diagnosticar si la vivienda de una familia es calificada como Vulnerable o No Vulnerable según sus factores constructivos y ambientales.
 */
import Modelo.Config.Conexion;
import Modelo.Entidades.PreguntaTest;
import Modelo.DTO.RespuestaTestDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/*
 * Qué hace (la acción): Define la clase VulnerabilidadDAO encargada de realizar operaciones de persistencia e inserción en lote (batch) para las preguntas y respuestas del test de riesgo en MySQL.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - DAO: Objeto de acceso a datos que gestiona exclusivamente las consultas JDBC con MySQL.
 * Para qué se usa (el propósito): Manejar el cuestionario evaluativo de riesgos, calcular el grado de vulnerabilidad del hogar y guardar el historial de seguimiento del supervisor.
 * Por qué es important (el impacto o problema que resuelve): Permite registrar de manera transaccional e indivisible las respuestas del test y recalcular de forma ACID el estado del plan de emergencia de la familia.
 */
public class VulnerabilidadDAO {

    /*
     * Qué hace (la acción): Consulta y devuelve la lista completa de preguntas activas de la base de datos, ordenadas por su orden predefinido.
     * Qué significa (conceptos, métodos, tipos involucrados): SELECT con filtro activo = true ordenadas ascendentemente (ORDER BY orden ASC).
     * Para qué se usa (el propósito): Recuperar el cuestionario de vulnerabilidad completo para renderizarlo en la vista del test del voluntario.
     */
    public List<PreguntaTest> obtenerPreguntasActivas() throws SQLException {
        // Qué hace: Inicializa la lista que almacenará las entidades de preguntas recuperadas.
        List<PreguntaTest> lista = new ArrayList<>();
        // Qué hace: Define la sentencia SQL a ejecutar.
        String sql = "SELECT id, enunciado, es_evaluable, orden, activo FROM preguntas_test WHERE activo = true ORDER BY orden ASC";
        
        // Qué hace: Abre la conexión a la base de datos y compila el statement parametrizado.
        // Por qué existe: Previene la inyección SQL y garantiza la liberación de recursos automáticos.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             // Qué hace: Ejecuta la consulta de lectura y la vuelca en un ResultSet.
             ResultSet rs = ps.executeQuery()) {
            // Qué hace: Recorre cada una de las filas devueltas por el motor de base de datos.
            while (rs.next()) {
                // Qué hace: Instancia la entidad PreguntaTest con los valores de la fila actual.
                PreguntaTest entidad = new PreguntaTest(
                    rs.getInt("id"),
                    rs.getString("enunciado"),
                    rs.getBoolean("es_evaluable"),
                    rs.getInt("orden"),
                    rs.getBoolean("activo")
                );
                // Qué hace: Agrega la entidad a la lista de retorno.
                lista.add(entidad);
            }
        }
        // Qué hace: Devuelve la lista con las preguntas activas.
        return lista;
    }

    // Sirve para: Recuperar un subconjunto de preguntas activas aplicando paginación.
    // Qué hace: Realiza un SELECT paginado mediante LIMIT y OFFSET a la tabla preguntas_test.
    // Explicación de consulta SQL:
    // - Información buscada: Columnas id, enunciado, es_evaluable, orden y activo.
    // - Tablas participantes: preguntas_test.
    // - Filtros aplicados: activo = true, paginado con LIMIT ? OFFSET ? y ordenado ascendentemente por orden.
    public List<PreguntaTest> obtenerPreguntasPaginadas(int offset, int limit) throws SQLException {
        // Qué hace: Inicializa la lista que almacenará las preguntas de la página actual.
        List<PreguntaTest> lista = new ArrayList<>();
        // Qué hace: Define la sentencia SQL paginada.
        String sql = "SELECT id, enunciado, es_evaluable, orden, activo FROM preguntas_test WHERE activo = true ORDER BY orden ASC LIMIT ? OFFSET ?";
        
        // Qué hace: Abre la conexión a base de datos y compila el PreparedStatement.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Vincula el límite de preguntas al primer placeholder de la consulta.
            ps.setInt(1, limit);
            // Qué hace: Vincula la fila inicial de lectura (offset) al segundo placeholder.
            ps.setInt(2, offset);
            // Qué hace: Ejecuta la consulta y lee los resultados.
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Qué hace: Instancia y mapea el registro actual a la entidad PreguntaTest.
                    PreguntaTest entidad = new PreguntaTest(
                        rs.getInt("id"),
                        rs.getString("enunciado"),
                        rs.getBoolean("es_evaluable"),
                        rs.getInt("orden"),
                        rs.getBoolean("activo")
                    );
                    // Qué hace: Agrega la entidad a la lista.
                    lista.add(entidad);
                }
            }
        }
        // Qué hace: Retorna las preguntas mapeadas de la página.
        return lista;
    }

    // Sirve para: Obtener la cantidad total de preguntas activas registradas en el sistema.
    // Qué hace: Realiza una consulta SELECT COUNT(*) para contar preguntas activas.
    // Explicación de consulta SQL:
    // - Información buscada: El conteo total de filas activas.
    // - Tablas participantes: preguntas_test.
    // - Filtros aplicados: activo = true.
    public int contarPreguntasActivas() throws SQLException {
        // Qué hace: Define la sentencia SQL de conteo.
        String sql = "SELECT COUNT(*) FROM preguntas_test WHERE activo = true";
        // Qué hace: Abre la conexión y compila el PreparedStatement.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             // Qué hace: Ejecuta la consulta de conteo en base de datos.
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                // Qué hace: Recupera el valor entero del conteo y lo retorna.
                return rs.getInt(1);
            }
        }
        // Qué hace: Retorna 0 por defecto.
        return 0;
    }

    // Sirve para: Obtener las respuestas previamente guardadas para un plan familiar específico.
    // Qué hace: Realiza una consulta SELECT a respuestas_test filtrando por el ID del plan.
    // Explicación de consulta SQL:
    // - Información buscada: Preguntas respondidas (pregunta_id) y su valor booleano (valor).
    // - Tablas participantes: respuestas_test.
    // - Filtros aplicados: plan_id = ? (el plan familiar evaluado).
    public List<RespuestaTestDTO> obtenerRespuestasPorPlan(int planId) throws SQLException {
        // Qué hace: Inicializa la lista que almacenará las respuestas en formato DTO.
        List<RespuestaTestDTO> lista = new ArrayList<>();
        // Qué hace: Define la consulta SQL de respuestas.
        String sql = "SELECT pregunta_id, valor FROM respuestas_test WHERE plan_id = ?";
        
        // Qué hace: Abre la conexión y compila el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Asigna el ID del plan de emergencia familiar al primer parámetro.
            ps.setInt(1, planId);
            // Qué hace: Ejecuta el SELECT en MySQL.
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Qué hace: Instancia el DTO asociando el ID de pregunta y el valor booleano obtenido.
                    RespuestaTestDTO dto = new RespuestaTestDTO(
                        rs.getInt("pregunta_id"),
                        rs.getBoolean("valor")
                    );
                    // Qué hace: Agrega el DTO a la lista de retorno.
                    lista.add(dto);
                }
            }
        }
        // Qué hace: Retorna el listado de respuestas existentes.
        return lista;
    }

    /*
     * Qué hace (la acción): Registra de manera transaccional e indivisible las respuestas del censo, evalúa el total de puntos de riesgo y actualiza el tipo de familia del plan a Vulnerable o No Vulnerable.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - ON DUPLICATE KEY UPDATE: Cláusula de MySQL que sobrescribe la respuesta anterior si el plan ya había respondido esa misma pregunta.
     *   - executeBatch(): Ejecuta en lote las respuestas para minimizar los accesos de red a la base de datos.
     *   - Puntos de riesgo >= 5: Califica a la familia como Vulnerable (tipo_familia_id = 1), en caso contrario No Vulnerable (tipo_familia_id = 2).
     *   - con.rollback(): Deshace todas las operaciones del test si ocurre algún fallo intermedio de base de datos.
     * Para qué se usa (el propósito): Guardar el cuestionario de vulnerabilidad completo de una familia y clasificar su grado de riesgo en la cabecera del plan.
     * Por qué es importante (el impacto o problema que resuelve): Garantiza que si falla la actualización del plan familiar, las respuestas tampoco se inserten, evitando inconsistencias o planes sin calificar.
     */
    public boolean guardarTestYActualizarPlan(int planId, List<RespuestaTestDTO> respuestas) throws SQLException {
        String sqlRespuesta = "INSERT INTO respuestas_test (valor, plan_id, pregunta_id) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE valor = ?";
        String sqlConteo = "SELECT COUNT(*) FROM respuestas_test rt JOIN preguntas_test pt ON rt.pregunta_id = pt.id " +
                           "WHERE rt.plan_id = ? AND rt.valor = true AND pt.es_evaluable = true";
        String sqlActualizarPlan = "UPDATE planes_familiares SET estado_id = 3, tipo_familia_id = ? WHERE id = ?";

        // Qué hace: Inicializa las variables para controlar de forma fina los recursos JDBC en bloque catch.
        Connection con = null;
        PreparedStatement psResp = null;
        PreparedStatement psCont = null;
        PreparedStatement psPlan = null;
        ResultSet rs = null;

        try {
            // Qué hace: Abre la conexión JDBC.
            con = Conexion.obtener();
            // Qué hace: Desactiva el auto-commit automático para iniciar una transacción manual.
            con.setAutoCommit(false);

            // 1. Guardar las respuestas del test utilizando sentencias por lotes (Batch)
            psResp = con.prepareStatement(sqlRespuesta);
            for (RespuestaTestDTO r : respuestas) {
                // Qué hace: Asigna el valor booleano de la respuesta, el plan y la pregunta en la inserción.
                psResp.setBoolean(1, r.isAnswer());
                psResp.setInt(2, planId);
                psResp.setInt(3, r.getVulnerableQuestionId());
                // Qué hace: Asigna la respuesta booleana para el caso de duplicidad.
                psResp.setBoolean(4, r.isAnswer());
                // Qué hace: Añade el comando al lote de ejecución JDBC.
                psResp.addBatch();
            }
            // Qué hace: Ejecuta todas las inserciones del lote acumuladas en MySQL.
            psResp.executeBatch();

            // 2. Contar las respuestas de riesgo marcadas como afirmativas para calificar la vulnerabilidad
            psCont = con.prepareStatement(sqlConteo);
            psCont.setInt(1, planId);
            int puntos = 0;
            rs = psCont.executeQuery();
            if (rs.next()) {
                // Qué hace: Recupera el total de puntos de riesgo acumulados.
                puntos = rs.getInt(1);
            }

            // Qué hace: Aplica la regla de negocio para clasificar a la familia.
            // ID 2 = No Vulnerable, ID 1 = Vulnerable (si tiene 5 o más puntos de riesgo).
            int tipoFamiliaId = 2;
            if (puntos >= 5) {
                tipoFamiliaId = 1;
            }

            // 3. Modificar el plan familiar asignando el tipo de familia calificado y pasándolo a estado 'En desarrollo'
            psPlan = con.prepareStatement(sqlActualizarPlan);
            psPlan.setInt(1, tipoFamiliaId);
            psPlan.setInt(2, planId);
            psPlan.executeUpdate();

            // Qué hace: Confirma la transacción en la base de datos de manera definitiva.
            con.commit();
            // Qué hace: Retorna verdadero indicando éxito.
            return true;
        } catch (SQLException e) {
            // Qué hace: Si ocurre un error, deshace todos los cambios realizados en el test para prevenir datos parciales o corruptos.
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error en rollback: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            // Qué hace: Cierra de forma ordenada todos los recursos abiertos en el bloque try.
            if (rs != null) rs.close();
            if (psResp != null) psResp.close();
            if (psCont != null) psCont.close();
            if (psPlan != null) psPlan.close();
            if (con != null) con.close();
        }
    }
    
    // Sirve para: Actualizar el estado_id de un plan familiar de manera aislada (ej. rechazar o aprobar).
    // Qué hace: Ejecuta un query UPDATE en la tabla planes_familiares.
    // Explicación de consulta SQL:
    // - Información buscada: Modificar la columna estado_id.
    // - Tablas participantes: planes_familiares.
    // - Filtros aplicados: id = ? (id del plan familiar a actualizar).
    public void actualizarEstadoPlan(int planId, int estadoId) throws SQLException {
        // Qué hace: Define la consulta de actualización.
        String sql = "UPDATE planes_familiares SET estado_id = ? WHERE id = ?";
        // Qué hace: Abre la conexión a la base de datos y compila el PreparedStatement.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Asigna el ID del nuevo estado al statement.
            ps.setInt(1, estadoId);
            // Qué hace: Asigna el ID del plan familiar.
            ps.setInt(2, planId);
            // Qué hace: Ejecuta la consulta en base de datos.
            ps.executeUpdate();
        }
    }

    /*
     * Qué hace (la acción): Inserta un registro en la tabla de bitácora 'seguimiento_plan' cada vez que el plan familiar cambia de estado.
     * Qué significa (conceptos, métodos, tipos involucrados): INSERT de observaciones, plan_id, usuario_gestor_id, estado_id y leido.
     * Para qué se usa (el propósito): Guardar la justificación y comentarios técnicos del supervisor al aprobar o rechazar el censo familiar.
     * Por qué es importante (el impacto o problema que resuelve): Permite que el voluntario lea los motivos detallados de por qué fue rechazado su plan familiar y realice los ajustes correspondientes.
     */
    public void registrarSeguimiento(int planId, int usuarioId, int estadoId, String observaciones) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Registrar una fila en seguimiento_plan.
        // - Tablas participantes: seguimiento_plan.
        String sql = "INSERT INTO seguimiento_plan (observaciones, plan_id, usuario_gestor_id, estado_id, leido) VALUES (?, ?, ?, ?, ?)";
        // Qué hace: Abre la conexión a la base de datos y compila el PreparedStatement.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Vincula los parámetros sanitizados del seguimiento.
            ps.setString(1, observaciones != null ? observaciones.trim() : "");
            ps.setInt(2, planId);
            ps.setInt(3, usuarioId);
            ps.setInt(4, estadoId);
            // Qué hace: Establece leido en falso por defecto hasta que el voluntario visualice la observación.
            ps.setBoolean(5, false);
            // Qué hace: Ejecuta el INSERT en MySQL.
            ps.executeUpdate();
        }
    }
}
