package Modelo.DAO;

import Modelo.Config.Conexion;
import Modelo.Entidades.PreguntaTest;
import Modelo.DTO.RespuestaTestDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Clase de Acceso a Datos (DAO) para el módulo del Test de Vulnerabilidad
public class VulnerabilidadDAO {

    // Recupera la lista completa de preguntas activas de la base de datos como entidades
    public List<PreguntaTest> obtenerPreguntasActivas() throws SQLException {
        // Inicializa la lista que almacenará las entidades de preguntas recuperadas
        List<PreguntaTest> lista = new ArrayList<>();
        // Consulta SQL para traer las preguntas activas ordenadas por su posición
        String sql = "SELECT id, enunciado, es_evaluable, orden, activo FROM preguntas_test WHERE activo = true ORDER BY orden ASC";
        
        // Abre la conexión, prepara y ejecuta la consulta de forma segura
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            // Recorre cada fila devuelta por el motor de base de datos
            while (rs.next()) {
                // Instancia la entidad PreguntaTest con los valores reales de la tabla de la BD
                PreguntaTest entidad = new PreguntaTest(
                    rs.getInt("id"),
                    rs.getString("enunciado"),
                    rs.getBoolean("es_evaluable"),
                    rs.getInt("orden"),
                    rs.getBoolean("activo")
                );
                // Agrega la entidad instanciada a la lista de retorno
                lista.add(entidad);
            }
        }
        // Devuelve la lista con las preguntas en formato de entidad
        return lista;
    }

    // Recupera un subconjunto de preguntas activas aplicando paginación como entidades
    public List<PreguntaTest> obtenerPreguntasPaginadas(int offset, int limit) throws SQLException {
        // Inicializa la lista que almacenará las preguntas entidad de la página actual
        List<PreguntaTest> lista = new ArrayList<>();
        // Consulta SQL paginada ordenando por la columna orden
        String sql = "SELECT id, enunciado, es_evaluable, orden, activo FROM preguntas_test WHERE activo = true ORDER BY orden ASC LIMIT ? OFFSET ?";
        
        // Abre la conexión y prepara la sentencia SQL
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Asigna el límite de preguntas por página (limit)
            ps.setInt(1, limit);
            // Asigna la fila inicial de lectura (offset)
            ps.setInt(2, offset);
            // Ejecuta la consulta estructurada en la base de datos
            try (ResultSet rs = ps.executeQuery()) {
                // Itera por los registros resultantes de la página
                while (rs.next()) {
                    // Instancia y mapea el registro a la entidad PreguntaTest
                    PreguntaTest entidad = new PreguntaTest(
                        rs.getInt("id"),
                        rs.getString("enunciado"),
                        rs.getBoolean("es_evaluable"),
                        rs.getInt("orden"),
                        rs.getBoolean("activo")
                    );
                    // Agrega la entidad a la lista
                    lista.add(entidad);
                }
            }
        }
        // Retorna las preguntas paginadas como entidades
        return lista;
    }

    // Obtiene la cantidad total de preguntas activas en el sistema
    public int contarPreguntasActivas() throws SQLException {
        // Sentencia SQL para contar todos los registros activos
        String sql = "SELECT COUNT(*) FROM preguntas_test WHERE activo = true";
        // Abre conexión, prepara y ejecuta la consulta
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            // Si el resultado contiene un conteo válido
            if (rs.next()) {
                // Retorna el número de preguntas activas obtenido
                return rs.getInt(1);
            }
        }
        // Retorna 0 si no se pudieron recuperar registros
        return 0;
    }

    // Obtiene las respuestas previamente guardadas para un plan familiar específico (precarga)
    public List<RespuestaTestDTO> obtenerRespuestasPorPlan(int planId) throws SQLException {
        // Inicializa la lista de respuestas DTO
        List<RespuestaTestDTO> lista = new ArrayList<>();
        // Consulta SQL para obtener la pregunta y el valor respondido (true/false) para un plan dado
        String sql = "SELECT pregunta_id, valor FROM respuestas_test WHERE plan_id = ?";
        
        // Abre la conexión y prepara la consulta parametrizada
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Reemplaza el marcador de posición con el ID del plan familiar solicitado
            ps.setInt(1, planId);
            // Ejecuta la consulta y obtiene el lector de registros
            try (ResultSet rs = ps.executeQuery()) {
                // Itera por cada respuesta registrada de este plan familiar
                while (rs.next()) {
                    // Crea un objeto DTO asociando el ID de pregunta y el valor booleano
                    RespuestaTestDTO dto = new RespuestaTestDTO(
                        rs.getInt("pregunta_id"),
                        rs.getBoolean("valor")
                    );
                    // Inserta el DTO en la lista
                    lista.add(dto);
                }
            }
        }
        // Devuelve el listado de respuestas existentes
        return lista;
    }

    // Guarda el lote de respuestas y califica/actualiza el plan en una sola transacción atómica
    public boolean guardarTestYActualizarPlan(int planId, List<RespuestaTestDTO> respuestas) throws SQLException {
        // SQL para insertar o actualizar la respuesta de una pregunta utilizando ON DUPLICATE KEY UPDATE
        String sqlRespuesta = "INSERT INTO respuestas_test (valor, plan_id, pregunta_id) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE valor = ?";
        // SQL para contar cuántas respuestas afirmativas marcadas corresponden a preguntas evaluables de riesgo
        String sqlConteo = "SELECT COUNT(*) FROM respuestas_test rt JOIN preguntas_test pt ON rt.pregunta_id = pt.id " +
                           "WHERE rt.plan_id = ? AND rt.valor = true AND pt.es_evaluable = true";
        // SQL para actualizar el estado del plan familiar a 3 (En desarrollo) y su clasificación de tipo de familia
        String sqlActualizarPlan = "UPDATE planes_familiares SET estado_id = 3, tipo_familia_id = ? WHERE id = ?";

        // Inicializa las variables para la conexión y las sentencias SQL de JDBC
        Connection con = null;
        PreparedStatement psResp = null;
        PreparedStatement psCont = null;
        PreparedStatement psPlan = null;
        ResultSet rs = null;

        try {
            // Abre una conexión limpia a la base de datos
            con = Conexion.obtener();
            // Desactiva la confirmación automática para iniciar una transacción ACID
            con.setAutoCommit(false);

            // 1. Prepara la sentencia SQL de inserción y actualización de respuestas
            psResp = con.prepareStatement(sqlRespuesta);
            // Itera por la lista de respuestas provistas en el lote
            for (RespuestaTestDTO r : respuestas) {
                // Establece el valor booleano de la respuesta
                psResp.setBoolean(1, r.isAnswer());
                // Asocia el ID del plan familiar en progreso
                psResp.setInt(2, planId);
                // Asocia el ID de la pregunta evaluada
                psResp.setInt(3, r.getVulnerableQuestionId());
                // Asocia el valor booleano en caso de actualización por clave duplicada
                psResp.setBoolean(4, r.isAnswer());
                // Agrega esta sentencia al lote de ejecución en batch
                psResp.addBatch();
            }
            // Ejecuta todas las inserciones del lote acumuladas en batch
            psResp.executeBatch();

            // 2. Prepara la sentencia para contar las respuestas de riesgo marcadas como SÍ
            psCont = con.prepareStatement(sqlConteo);
            // Reemplaza el ID del plan a calificar
            psCont.setInt(1, planId);
            // Inicializa la variable de puntos ganados
            int puntos = 0;
            // Ejecuta la consulta de conteo y lee el registro
            rs = psCont.executeQuery();
            if (rs.next()) {
                // Recupera la cantidad de respuestas afirmativas a preguntas de riesgo
                puntos = rs.getInt(1);
            }

            // Define por defecto que la familia clasifica como "No Vulnerable" (tipo 2)
            int tipoFamiliaId = 2;
            // Si la puntuación final de riesgo es igual o superior a 5
            if (puntos >= 5) {
                // Clasifica a la familia como "Vulnerable" (tipo 1)
                tipoFamiliaId = 1;
            }

            // 3. Prepara la sentencia para actualizar los campos estado y clasificación de la familia
            psPlan = con.prepareStatement(sqlActualizarPlan);
            // Asigna el tipo de familia resuelto (1 o 2)
            psPlan.setInt(1, tipoFamiliaId);
            // Asigna el ID del plan familiar en progreso
            psPlan.setInt(2, planId);
            // Aplica la actualización en la tabla planes_familiares
            psPlan.executeUpdate();

            // Confirma la transacción en la base de datos de manera definitiva
            con.commit();
            // Retorna éxito absoluto
            return true;
        } catch (SQLException e) {
            // Si ocurrió alguna excepción, realiza un rollback para no dejar datos corruptos
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error en rollback: " + ex.getMessage());
                }
            }
            // Propaga la excepción original para que sea manejada en la capa de servicios
            throw e;
        } finally {
            // Cierra todas las conexiones y recursos abiertos en el bloque try
            if (rs != null) rs.close();
            if (psResp != null) psResp.close();
            if (psCont != null) psCont.close();
            if (psPlan != null) psPlan.close();
            if (con != null) con.close();
        }
    }
    
    // Actualiza el estado_id de un plan familiar de manera aislada (ej. rechazar o aprobar)
    public void actualizarEstadoPlan(int planId, int estadoId) throws SQLException {
        // Sentencia SQL de actualización del estado del plan
        String sql = "UPDATE planes_familiares SET estado_id = ? WHERE id = ?";
        // Abre conexión, prepara la consulta y ejecuta el cambio en la base de datos
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Reemplaza el ID del nuevo estado
            ps.setInt(1, estadoId);
            // Reemplaza el ID del plan familiar
            ps.setInt(2, planId);
            // Ejecuta el cambio
            ps.executeUpdate();
        }
    }
}
