package Modelo.DAO;

import Modelo.Config.Conexion;
import Modelo.DTO.ActionPlanDTO;
import java.sql.*;

// Qué hace: DAO encargado de realizar operaciones de lectura, escritura y actualización para la cabecera general del Plan de Acción en la tabla plan_accion.
// Por qué existe: Encapsula el acceso directo a la base de datos MySQL usando sentencias preparadas de JDBC.
// Qué problema resuelve: Separa las consultas SQL de la capa de servicio, previene la inyección SQL y coordina transacciones complejas de inicialización de tareas.
public class ActionPlanDAO {

    // Qué hace: Comprueba si existe al menos una tarea registrada para un plan familiar en la tabla plan_accion.
    // Por qué existe: Permite alimentar la verificación de existencia (boolean) consumida en los controladores del frontend para activar o desactivar la interfaz de sub-acciones.
    // Qué problema resuelve: Retorna un indicador simple reduciendo el consumo de memoria al no cargar objetos completos.
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

    // Qué hace: Obtiene la cabecera de configuración general (coordinador y riesgo) leyendo el primer registro asociado al plan.
    // Por qué existe: Carga los datos de preselección en los dropdowns del formulario superior en las vistas SPA.
    // Qué problema resuelve: Mapea la información cruda de la base de datos hacia un objeto estructurado DTO.
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

    // Qué hace: Inicializa transaccionalmente el plan de acción insertando 3 tareas base por defecto (antes, durante y después).
    // Por qué existe: Asegura que el plan de acción familiar inicie con las tareas guías necesarias para que la UI de la SPA funcione fluidamente.
    // Qué problema resuelve: Crea la estructura requerida en base de datos en una única transacción atómica para prevenir estados corruptos o parciales.
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

    // Qué hace: Actualiza el coordinador y factor de riesgo para todas las tareas del plan de acción.
    // Por qué existe: Permite modificar la cabecera del plan (por ejemplo, cambiar el líder de emergencia familiar) propagando el cambio a todas las tareas de la matriz.
    // Qué problema resuelve: Actualiza de forma masiva los registros asociados mediante una consulta preparada única.
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
