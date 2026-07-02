package Modelo.DAO;

/*
 * Qué hace (la acción): Importa el conector de base de datos de la configuración del proyecto, la clase DTO que representa un plan de acción y las clases de acceso a datos JDBC estándar.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.Config.Conexion: Módulo de conexión a MySQL.
 *   - Modelo.DTO.ActionPlanDTO: Objeto de transferencia que transporta los metadatos de cabecera del plan (coordinador y riesgo asociado).
 *   - java.sql.*: Clases e interfaces (Connection, PreparedStatement, ResultSet, SQLException, Types) necesarias para realizar operaciones de base de datos relacionales en Java.
 * Para qué se usa (el propósito): Proveer los objetos necesarios para interactuar con la base de datos MySQL a través de consultas SQL parametrizadas.
 * Por qué es importante (el impacto o problema que resuelve): Permite que la clase acceda a los servicios de persistencia, manejando de forma segura y estructurada la información del plan.
 */
import Modelo.Config.Conexion;
import Modelo.DTO.ActionPlanDTO;
import java.sql.*;

/*
 * Qué hace (la acción): Define la clase ActionPlanDAO para gestionar la persistencia y transaccionalidad de las cabeceras del plan de acción.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - DAO (Data Access Object): Capa encargada de realizar operaciones CRUD en la base de datos sin mezclar la lógica de negocio ni presentación.
 * Para qué se usa (el propósito): Centralizar las operaciones de inicialización, verificación y actualización de los planes de emergencia familiar.
 * Por qué es importante (el impacto o problema que resuelve): Encapsula y aísla la lógica de base de datos para la entidad plan de acción, evitando código redundante y mejorando la mantenibilidad.
 */
public class ActionPlanDAO {

    /*
     * Qué hace (la acción): Comprueba si existe al menos una tarea registrada para un plan familiar específico.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - SELECT COUNT(*): Cuenta la cantidad de filas coincidentes.
     *   - ps.setInt(1, planId): Reemplaza el primer marcador '?' por el ID del plan.
     * Para qué se usa (el propósito): Validar rápidamente en el servidor si un plan familiar ya cuenta con tareas inicializadas o necesita ser creado desde cero.
     * Por qué es importante (el impacto o problema que resuelve): Evita cargar toda la lista de tareas en memoria cuando solo se necesita una respuesta booleana rápida (true/false).
     */
    public boolean existePlan(int planId) throws SQLException {
        // SELECT COUNT(*) cuenta el número total de registros coincidentes.
        // WHERE plan_id = ? filtra por el identificador del plan familiar especificado.
        String sql = "SELECT COUNT(*) AS total FROM plan_accion WHERE plan_id = ?";
        
        // try-with-resources: Abre la conexión y el statement de manera segura, cerrándolos automáticamente al finalizar.
        try (Connection con = Conexion.obtener(); // Obtiene la conexión activa de la base de datos.
             PreparedStatement ps = con.prepareStatement(sql)) { // Prepara la consulta SQL para evitar inyección de código.
             
            // Vincula el parámetro entero 'planId' al primer marcador de posición '?' de la consulta.
            ps.setInt(1, planId);
            
            // Ejecuta la consulta SELECT y vuelca los resultados en un ResultSet.
            try (ResultSet rs = ps.executeQuery()) {
                // Si hay un resultado disponible (siempre habrá uno debido a la función de agregación COUNT).
                if (rs.next()) {
                    // Retorna verdadero si el conteo total de filas es mayor que cero (es decir, el plan existe).
                    return rs.getInt("total") > 0;
                }
            }
        }
        // Retorna falso por defecto si algo falla o no se encuentran registros.
        return false;
    }

    /*
     * Qué hace (la acción): Recupera la información básica (cabecera) del plan de acción de una familia, mapeándola a un DTO.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - LIMIT 1: Limita el resultado a la primera fila que coincida con el plan_id.
     *   - ActionPlanDTO: Estructura de datos utilizada para almacenar y transferir el ID de la tarea, del plan, del riesgo y del coordinador.
     * Para qué se usa (el propósito): Suministrar al frontend la configuración de cabecera seleccionada para un plan.
     * Por qué es importante (el impacto o problema que resuelve): Permite cargar los selectores de la cabecera (como el factor de riesgo y el coordinador familiar) en los formularios correspondientes de la interfaz visual.
     */
    public ActionPlanDTO obtenerPlan(int planId) throws SQLException {
        // SELECT recupera las columnas id, plan_id, riesgo_id y coordinador_id.
        // WHERE plan_id = ? filtra las tareas del plan específico.
        // LIMIT 1 detiene la búsqueda tras encontrar la primera coincidencia (optimiza el rendimiento).
        String sql = "SELECT id, plan_id, riesgo_id, coordinador_id FROM plan_accion WHERE plan_id = ? LIMIT 1";
        
        // try-with-resources: Inicializa y administra de forma segura la conexión y el PreparedStatement.
        try (Connection con = Conexion.obtener(); // Solicita una conexión física al gestor de base de datos.
             PreparedStatement ps = con.prepareStatement(sql)) { // Prepara la sentencia SQL parametrizada.
             
            // Asigna el ID del plan de emergencia familiar al marcador '?' de la consulta.
            ps.setInt(1, planId);
            
            // Ejecuta la consulta de selección.
            try (ResultSet rs = ps.executeQuery()) {
                // Si se encuentra al menos una fila con el registro del plan de acción.
                if (rs.next()) {
                    // Crea una nueva instancia del objeto DTO para transportar los datos a las capas superiores.
                    ActionPlanDTO dto = new ActionPlanDTO();
                    // Obtiene el valor de la columna 'id' (entero) y lo asigna al DTO.
                    dto.setId(rs.getInt("id"));
                    // Obtiene la columna 'plan_id' y la mapea a la propiedad familyPlanId.
                    dto.setFamilyPlanId(rs.getInt("plan_id"));
                    // Obtiene la columna 'coordinador_id' y la mapea a la propiedad memberId (coordinador responsable).
                    dto.setMemberId(rs.getInt("coordinador_id"));
                    // Obtiene la columna 'riesgo_id' y la mapea a la propiedad riskFactorId (factor de riesgo).
                    dto.setRiskFactorId(rs.getInt("riesgo_id"));
                    // Retorna el DTO de cabecera con la información estructurada.
                    return dto;
                }
            }
        }
        // Retorna null si no se localizó ningún plan de acción creado para el ID suministrado.
        return null;
    }

    /*
     * Qué hace (la acción): Inicializa un nuevo plan de acción insertando tres tareas guías predefinidas (Antes, Durante y Después) en una transacción atómica.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - con.setAutoCommit(false): Deshabilita la confirmación automática de consultas de JDBC para iniciar una transacción manual explícita.
     *   - con.commit(): Aplica y consolida todos los cambios de la transacción de manera permanente en el motor de base de datos MySQL.
     *   - con.rollback(): Deshace todas las operaciones realizadas dentro de la transacción si ocurre un error, volviendo al estado anterior.
     * Para qué se usa (el propósito): Crear la estructura base sugerida del plan familiar de forma segura y consistente cuando se configura por primera vez.
     * Por qué es importante (el impacto o problema que resuelve): Garantiza la atomicidad de la operación; si falla la inserción de alguna de las tres tareas base, ninguna se guardará, evitando dejar un plan parcialmente creado en la base de datos.
     */
    public void crear(int planId, int coordinatorId, int riskFactorId) throws SQLException {
        // INSERT INTO agrega nuevos registros en plan_accion.
        // Las columnas momento, descripcion_tarea, plan_id, riesgo_id y coordinador_id reciben valores mediante marcadores '?'.
        String sql = "INSERT INTO plan_accion (momento, descripcion_tarea, plan_id, riesgo_id, coordinador_id) VALUES (?, ?, ?, ?, ?)";
        Connection con = null; // Variable para controlar de forma fina la conexión y la transacción manual.
        try {
            con = Conexion.obtener(); // Abre la conexión física a la base de datos MySQL.
            con.setAutoCommit(false); // Inicia la transacción manual (desactiva el auto-commit automático).
            
            // Momentos base y descripciones sugeridas por defecto que se registrarán para la familia
            String[] momentos = {"antes", "durante", "despues"};
            String[] descripciones = {
                "Definir medidas de prevención y preparación del hogar.",
                "Coordinar la evacuación y el punto de encuentro familiar.",
                "Evaluar afectaciones y coordinar la rehabilitación de servicios."
            };
            
            // Prepara el PreparedStatement para realizar múltiples inserciones.
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                // Recorre el arreglo de momentos para insertar cada tarea base
                for (int i = 0; i < momentos.length; i++) {
                    // Establece el momento de la tarea ("antes", "durante", "despues") en el primer marcador '?'.
                    ps.setString(1, momentos[i]);
                    // Establece la descripción sugerida por defecto en el segundo marcador '?'.
                    ps.setString(2, descripciones[i]);
                    // Asigna el identificador del plan familiar en el tercer marcador '?'.
                    ps.setInt(3, planId);
                    // Asigna el identificador del factor de riesgo en el cuarto marcador '?'.
                    ps.setInt(4, riskFactorId);
                    
                    // Si el identificador de coordinador es válido (mayor que cero)
                    if (coordinatorId > 0) {
                        // Lo vincula al quinto marcador '?'.
                        ps.setInt(5, coordinatorId);
                    } else {
                        // Si no hay coordinador asignado, asocia el tipo nulo de SQL (Types.INTEGER).
                        ps.setNull(5, Types.INTEGER);
                    }
                    
                    // Ejecuta la inserción individual en la base de datos.
                    ps.executeUpdate();
                }
            }
            con.commit(); // Confirma la inserción de las tres filas de forma definitiva en la transacción.
        } catch (SQLException e) {
            // Si ocurre algún fallo durante la inserción de cualquiera de las tareas base
            if (con != null) {
                con.rollback(); // Revierte (deshace) todos los cambios de la transacción para mantener consistencia.
            }
            throw e; // Lanza la excepción para que sea controlada en el controlador.
        } finally {
            // Bloque final para asegurar el cierre de la conexión a la base de datos
            if (con != null) {
                con.close(); // Libera la conexión física con la base de datos MySQL.
            }
        }
    }

    /*
     * Qué hace (la acción): Actualiza de manera masiva el coordinador familiar y el factor de riesgo para todas las tareas que pertenecen al plan especificado.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - UPDATE: Sentencia SQL para modificar registros existentes.
     *   - WHERE plan_id = ?: Cláusula que limita la actualización masiva a las filas que pertenecen únicamente a ese plan familiar.
     * Para qué se usa (el propósito): Modificar la configuración de cabecera del plan (coordinador o riesgo) y propagar ese cambio automáticamente a todas sus tareas.
     * Por qué es importante (el impacto o problema que resuelve): Evita que el usuario tenga que actualizar la cabecera tarea por tarea, realizando una actualización masiva eficiente en una sola consulta.
     */
    public void actualizar(int planId, int coordinatorId, int riskFactorId) throws SQLException {
        // UPDATE modifica los campos de los registros existentes en plan_accion.
        // SET coordinador_id = ?, riesgo_id = ? actualiza los valores correspondientes.
        // WHERE plan_id = ? restringe los cambios para que apliquen exclusivamente al plan familiar especificado.
        String sql = "UPDATE plan_accion SET coordinador_id = ?, riesgo_id = ? WHERE plan_id = ?";
        
        // try-with-resources: Garantiza el cierre de la conexión y del PreparedStatement.
        try (Connection con = Conexion.obtener(); // Abre la conexión con la base de datos.
             PreparedStatement ps = con.prepareStatement(sql)) { // Prepara la consulta UPDATE de JDBC.
            
            // Si el identificador de coordinador es válido (mayor que cero)
            if (coordinatorId > 0) {
                // Lo asigna en el primer marcador de posición.
                ps.setInt(1, coordinatorId);
            } else {
                // De lo contrario, asigna un valor NULL para la columna del coordinador.
                ps.setNull(1, Types.INTEGER);
            }
            // Asigna el identificador del factor de riesgo en el segundo marcador de posición.
            ps.setInt(2, riskFactorId);
            // Asigna el identificador del plan familiar para el filtro WHERE de la actualización.
            ps.setInt(3, planId);
            
            // Ejecuta la sentencia UPDATE de actualización física en MySQL.
            ps.executeUpdate();
        }
    }
}
