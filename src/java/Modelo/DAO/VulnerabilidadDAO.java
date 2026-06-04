package Modelo.DAO;

import Modelo.Config.Conexion;
import Modelo.Entidades.PreguntaTest;
import Modelo.DTO.RespuestaTestDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Qué hace: Clase de Acceso a Datos (DAO) para el módulo del Test de Vulnerabilidad del Plan Familiar.
// Por qué existe: Provee métodos específicos para interactuar con las tablas de preguntas del censo y guardar las respuestas relacionales correspondientes.
// Qué problema resuelve: Encapsula la lógica de calificación de vulnerabilidad y la actualización de estados del plan familiar de forma transaccional.
public class VulnerabilidadDAO {

    // Sirve para: Recuperar la lista completa de preguntas activas de la base de datos como entidades.
    // Qué hace: Realiza una consulta SELECT a la tabla preguntas_test trayendo los registros activos ordenados.
    // Explicación de consulta SQL:
    // - Información buscada: Columnas id, enunciado, es_evaluable, orden y activo de las preguntas de vulnerabilidad.
    // - Tablas participantes: preguntas_test.
    // - Filtros aplicados: activo = true (solo preguntas habilitadas), ordenadas de forma ascendente por el campo orden.
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

    // Sirve para: Guardar el lote de respuestas y calificar/actualiza el plan en una sola transacción atómica.
    // Qué hace: Realiza inserciones en lote (batch) de respuestas y actualiza el tipo de familia del plan familiar según la puntuación.
    // Explicación de consultas SQL:
    // - sqlRespuesta: Inserción de respuestas en respuestas_test. Si ya existe, actualiza el valor (ON DUPLICATE KEY UPDATE).
    // - sqlConteo: Cuenta las respuestas afirmativas del plan que corresponden a preguntas evaluables de riesgo.
    // - sqlActualizarPlan: Modifica el estado del plan familiar a 3 (En desarrollo) y asigna su tipo de familia (tipo_familia_id).
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

    // Sirve para: Registrar una bitácora de seguimiento cada vez que el plan cambia de estado.
    // Qué hace: Inserta observaciones, el ID del plan, el ID del supervisor gestor y el nuevo estado en seguimiento_plan.
    // Por qué es importante: Permite auditar el flujo del plan familiar y mostrar observaciones al voluntario en caso de rechazo.
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
