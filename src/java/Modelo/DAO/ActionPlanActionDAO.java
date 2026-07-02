package Modelo.DAO;

/*
 * Qué hace (la acción): Importa la clase de configuración de la conexión a la base de datos, el DTO de acciones del plan, las clases de acceso JDBC (java.sql.*) y colecciones estándar de Java.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.Config.Conexion: Administrador centralizado de la conexión física a la base de datos MySQL.
 *   - Modelo.DTO.ActionPlanActionDTO: Objeto de transferencia de datos que representa una tarea individual en un momento específico del plan.
 *   - java.sql.*: APIs estándar de Java (Connection, PreparedStatement, ResultSet, SQLException) para interactuar con bases de datos relacionales.
 * Para qué se usa (el propósito): Proveer las herramientas necesarias para la ejecución y mapeo de sentencias SQL en la base de datos.
 * Por qué es importante (el impacto o problema que resuelve): Permite que la clase DAO interactúe con el motor de base de datos MySQL y estructure las respuestas en listas o DTOs.
 */
import Modelo.Config.Conexion;
import Modelo.DTO.ActionPlanActionDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/*
 * Qué hace (la acción): Define la clase ActionPlanActionDAO encargada de realizar operaciones de acceso a datos (CRUD) sobre la tabla 'plan_accion'.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - DAO (Data Access Object): Patrón de diseño que aísla la lógica de negocio de los detalles de la base de datos.
 * Para qué se usa (el propósito): Administrar las micro-acciones (tareas antes, durante y después) de los planes familiares de emergencia.
 * Por qué es importante (el impacto o problema que resuelve): Centraliza toda la lógica SQL correspondiente a las acciones individuales del plan de emergencia, facilitando su mantenimiento.
 */
public class ActionPlanActionDAO {

    /*
     * Qué hace (la acción): Obtiene el ID del factor de riesgo asociado a un plan de emergencia familiar específico.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - sql: Consulta SELECT que recupera el riesgo_id de la tabla plan_accion filtrando por plan_id.
     *   - ps.setInt(1, planId): Asigna el identificador del plan al primer marcador '?'.
     * Para qué se usa (el propósito): Recuperar la referencia del riesgo que el plan ya tiene asociado antes de registrar una nueva tarea.
     * Por qué es importante (el impacto o problema que resuelve): Garantiza la coherencia relacional de la base de datos al heredar el mismo factor de riesgo para las nuevas tareas del plan sin que el frontend lo tenga que enviar.
     */
    public int obtenerRiesgoIdPorPlan(int planId) throws SQLException {
        String sql = "SELECT riesgo_id FROM plan_accion WHERE plan_id = ? LIMIT 1";
        
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
             
            // Vincula el ID del plan familiar al primer marcador de posición '?'.
            ps.setInt(1, planId);
            
            // Ejecuta la consulta de selección.
            try (ResultSet rs = ps.executeQuery()) {
                // Si la consulta devolvió al menos un registro de plan de acción.
                if (rs.next()) {
                    // Retorna el ID numérico del factor de riesgo asociado al plan familiar.
                    return rs.getInt("riesgo_id");
                }
            }
        }
        // Retorna 0 si no se encontró ningún plan de acción configurado previamente.
        return 0;
    }

    /*
     * Qué hace (la acción): Obtiene la lista de todas las acciones/tareas detalladas asociadas a un plan familiar, incluyendo datos del integrante coordinador.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - LEFT JOIN: Unión que permite traer las tareas del plan de acción incluso si aún no se les ha asignado ningún integrante coordinador.
     *   - rs.getString("momento"): Recupera el momento de la base de datos ("antes", "durante", "despues") para traducirlo a IDs numéricos de tipo de acción.
     * Para qué se usa (el propósito): Mostrar en la interfaz de usuario la lista organizada de micro-acciones que los integrantes deben realizar en una emergencia.
     * Por qué es importante (el impacto o problema que resuelve): Permite al voluntario o supervisor visualizar en una sola vista quién es responsable de qué tarea y en qué etapa se ejecuta.
     */
    public List<ActionPlanActionDTO> listarPorPlan(int planId) throws SQLException {
        // Si usáramos INNER JOIN, perderíamos las tareas que aún no tengan responsable asignado.
        // WHERE pa.plan_id = ? filtra las tareas del plan específico.
        String sql = "SELECT pa.id, pa.momento, pa.descripcion_tarea, pa.plan_id, pa.riesgo_id, pa.coordinador_id, "
                   + "i.nombre, i.apellido "
                   + "FROM plan_accion pa "
                   + "LEFT JOIN integrantes i ON pa.coordinador_id = i.id "
                   + "WHERE pa.plan_id = ?";
        
        // Inicializa la lista dinámica que contendrá los DTOs de las tareas.
        List<ActionPlanActionDTO> lista = new ArrayList<>();
        
        // try-with-resources: Administra de manera segura la conexión física y el PreparedStatement de JDBC.
        try (Connection con = Conexion.obtener(); // Solicita la conexión.
             PreparedStatement ps = con.prepareStatement(sql)) { // Compila la consulta preparada.
             
            // Vincula el ID del plan familiar al parámetro WHERE.
            ps.setInt(1, planId);
            
            // Ejecuta la consulta y recorre todas las filas resultantes.
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Instancia un DTO de tipo ActionPlanActionDTO para almacenar la información de la fila actual.
                    ActionPlanActionDTO dto = new ActionPlanActionDTO();
                    dto.setId(rs.getInt("id")); // Mapea la columna id.
                    dto.setDescription(rs.getString("descripcion_tarea")); // Mapea la descripción de la tarea.
                    dto.setActionPlanId(rs.getInt("plan_id")); // Mapea el plan familiar id.
                    
                    int coordId = rs.getInt("coordinador_id"); // Obtiene el ID del coordinador.
                    dto.setMemberId(coordId); // Lo asigna en el DTO.
                    
                    // Traduce la cadena de momento ("antes", "durante", "despues") al respectivo ID de tipo de acción en la UI.
                    String momentoStr = rs.getString("momento");
                    int actionTypeId = 1; // 1 = Antes
                    if ("durante".equals(momentoStr)) {
                        actionTypeId = 2; // 2 = Durante
                    } else if ("despues".equals(momentoStr)) {
                        actionTypeId = 3; // 3 = Después
                    }
                    dto.setActionTypeId(actionTypeId); // Asigna el tipo de acción.
                    
                    // Obtiene nombre y apellido del integrante del LEFT JOIN
                    String nombre = rs.getString("nombre");
                    String apellido = rs.getString("apellido");
                    
                    // Si el integrante existe (no es nulo en el LEFT JOIN)
                    if (nombre != null && apellido != null) {
                        dto.setMemberName(nombre + " " + apellido); // Setea nombre concatenado.
                        dto.getMember().setNames(nombre); // Setea el nombre en el objeto interno Member.
                        dto.getMember().setLast_names(apellido); // Setea el apellido en el objeto interno.
                    } else {
                        // De lo contrario, se asigna texto alternativo que indique la ausencia de responsable.
                        dto.setMemberName("Sin coordinador");
                        dto.getMember().setNames("Sin");
                        dto.getMember().setLast_names("coordinador");
                    }
                    
                    // Agrega el DTO a la lista de retorno.
                    lista.add(dto);
                }
            }
        }
        // Retorna la lista resultante con todas las tareas encontradas y mapeadas.
        return lista;
    }

    /*
     * Qué hace (la acción): Obtiene una única micro-acción detallada a partir de su ID único de base de datos.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - ResultSet rs: Objeto de lectura secuencial que representa la fila de datos extraída por la consulta de MySQL.
     * Para qué se usa (el propósito): Recuperar la información específica de una tarea para su edición o visualización en la interfaz SPA.
     * Por qué es importante (el impacto o problema que resuelve): Evita consultar listas enteras cuando solo se requiere interactuar o modificar una tarea específica.
     */
    public ActionPlanActionDTO obtenerPorId(int id) throws SQLException {
        // SELECT recupera las columnas de la tarea y de su integrante responsable.
        // LEFT JOIN se utiliza para permitir que tareas sin responsable sigan siendo legibles.
        // WHERE pa.id = ? filtra por la clave primaria única de la tarea.
        String sql = "SELECT pa.id, pa.momento, pa.descripcion_tarea, pa.plan_id, pa.riesgo_id, pa.coordinador_id, "
                   + "i.nombre, i.apellido "
                   + "FROM plan_accion pa "
                   + "LEFT JOIN integrantes i ON pa.coordinador_id = i.id "
                   + "WHERE pa.id = ?";
        
        // try-with-resources: Abre la conexión a la base de datos y prepara el statement.
        try (Connection con = Conexion.obtener(); // Abre la conexión física.
             PreparedStatement ps = con.prepareStatement(sql)) { // Prepara la consulta parametrizada.
             
            // Vincula el ID de la tarea al marcador de posición.
            ps.setInt(1, id);
            
            // Ejecuta la consulta de base de datos.
            try (ResultSet rs = ps.executeQuery()) {
                // Si la tarea con ese ID existe
                if (rs.next()) {
                    // Crea una instancia del DTO.
                    ActionPlanActionDTO dto = new ActionPlanActionDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setDescription(rs.getString("descripcion_tarea"));
                    dto.setActionPlanId(rs.getInt("plan_id"));
                    
                    int coordId = rs.getInt("coordinador_id");
                    dto.setMemberId(coordId);
                    
                    // Traduce el momento a ID del frontend
                    String momentoStr = rs.getString("momento");
                    int actionTypeId = 1;
                    if ("durante".equals(momentoStr)) {
                        actionTypeId = 2;
                    } else if ("despues".equals(momentoStr)) {
                        actionTypeId = 3;
                    }
                    dto.setActionTypeId(actionTypeId);
                    
                    String nombre = rs.getString("nombre");
                    String apellido = rs.getString("apellido");
                    if (nombre != null && apellido != null) {
                        dto.setMemberName(nombre + " " + apellido);
                        dto.getMember().setNames(nombre);
                        dto.getMember().setLast_names(apellido);
                    } else {
                        dto.setMemberName("Sin coordinador");
                        dto.getMember().setNames("Sin");
                        dto.getMember().setLast_names("coordinador");
                    }
                    
                    // Retorna el DTO de la tarea.
                    return dto;
                }
            }
        }
        // Retorna null si la tarea no existe en la base de datos.
        return null;
    }

    /*
     * Qué hace (la acción): Inserta un nuevo registro de micro-acción (tarea) en la base de datos.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - ps.setNull(5, Types.INTEGER): Almacena un nulo de base de datos cuando no hay integrante asignado a la tarea.
     *   - ps.executeUpdate(): Envía la instrucción de inserción para que sea guardada de forma persistente en MySQL.
     * Para qué se usa (el propósito): Crear y registrar nuevas tareas de mitigación para un plan de emergencia.
     * Por qué es importante (el impacto o problema que resuelve): Permite expandir de forma dinámica la planificación familiar ante emergencias, añadiendo tareas específicas en el momento adecuado.
     */
    public void crear(ActionPlanActionDTO dto) throws SQLException {
        // Primero, obtiene de forma dinámica el riesgo_id configurado para el plan familiar.
        int riesgoId = obtenerRiesgoIdPorPlan(dto.getActionPlanId());
        
        // INSERT INTO agrega un nuevo registro a la tabla plan_accion.
        // Recibe 5 valores parametrizados por los placeholders '?'.
        String sql = "INSERT INTO plan_accion (momento, descripcion_tarea, plan_id, riesgo_id, coordinador_id) VALUES (?, ?, ?, ?, ?)";
        
        // try-with-resources: Administra de manera segura la conexión y el statement de inserción.
        try (Connection con = Conexion.obtener(); // Abre la conexión física.
             PreparedStatement ps = con.prepareStatement(sql)) { // Prepara el statement.
            
            // Traduce el ID numérico de tipo de acción del frontend al String esperado en base de datos.
            String momentoStr = "antes";
            if (dto.getActionTypeId() == 2) {
                momentoStr = "durante";
            } else if (dto.getActionTypeId() == 3) {
                momentoStr = "despues";
            }
            
            // Vincula el momento al primer marcador '?'.
            ps.setString(1, momentoStr);
            // Vincula la descripción de la tarea al segundo marcador '?'.
            ps.setString(2, dto.getDescription());
            // Vincula el ID del plan familiar al tercer marcador '?'.
            ps.setInt(3, dto.getActionPlanId());
            // Vincula el ID del riesgo obtenido al cuarto marcador '?'.
            ps.setInt(4, riesgoId);
            
            // Si hay un coordinador asignado (ID mayor que cero)
            if (dto.getMemberId() > 0) {
                // Vincula el ID de integrante al quinto marcador '?'.
                ps.setInt(5, dto.getMemberId());
            } else {
                // De lo contrario, vincula un valor NULL de tipo entero.
                ps.setNull(5, Types.INTEGER);
            }
            
            // Ejecuta la inserción física en MySQL.
            ps.executeUpdate();
        }
    }

    /*
     * Qué hace (la acción): Actualiza la descripción y el integrante responsable de una micro-acción existente.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - sql: Sentencia UPDATE que modifica campos condicionados por la llave primaria única 'id'.
     * Para qué se usa (el propósito): Guardar los cambios realizados al editar una tarea particular del plan familiar.
     * Por qué es importante (el impacto o problema que resuelve): Mantiene al día la información y responsabilidades del plan de emergencia familiar en tiempo real.
     */
    public void actualizar(int id, ActionPlanActionDTO dto) throws SQLException {
        // UPDATE modifica campos específicos del registro en plan_accion.
        // SET descripcion_tarea = ?, coordinador_id = ? actualiza el texto y el responsable.
        // WHERE id = ? restringe el cambio únicamente al registro de la tarea seleccionada.
        String sql = "UPDATE plan_accion SET descripcion_tarea = ?, coordinador_id = ? WHERE id = ?";
        
        // try-with-resources: Garantiza el cierre automático de conexiones y statements.
        try (Connection con = Conexion.obtener(); // Abre la conexión.
             PreparedStatement ps = con.prepareStatement(sql)) { // Prepara la actualización.
            
            // Vincula la nueva descripción al primer parámetro '?'.
            ps.setString(1, dto.getDescription());
            
            // Si hay un coordinador asignado (ID mayor que cero)
            if (dto.getMemberId() > 0) {
                // Vincula el ID de integrante al segundo parámetro '?'.
                ps.setInt(2, dto.getMemberId());
            } else {
                // De lo contrario, vincula un valor NULL.
                ps.setNull(2, Types.INTEGER);
            }
            
            // Vincula el ID de la tarea al tercer parámetro '?' del WHERE.
            ps.setInt(3, id);
            
            // Ejecuta la actualización física del registro.
            ps.executeUpdate();
        }
    }

    /*
     * Qué hace (la acción): Elimina físicamente el registro de una tarea de la base de datos.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - ps.executeUpdate(): En este contexto, ejecuta la sentencia de eliminación y retorna el número de filas afectadas.
     * Para qué se usa (el propósito): Dar de baja tareas que ya no son necesarias o que fueron agregadas por error.
     * Por qué es importante (el impacto o problema que resuelve): Mantiene la base de datos limpia de registros obsoletos y actualiza la interfaz visual al instante.
     */
    public void eliminar(int id) throws SQLException {
        // DELETE FROM elimina físicamente registros que cumplan la condición WHERE.
        // WHERE id = ? restringe el borrado al ID exacto de la sub-tarea.
        String sql = "DELETE FROM plan_accion WHERE id = ?";
        
        // try-with-resources: Abre y cierra de forma segura la conexión y el statement.
        try (Connection con = Conexion.obtener(); // Abre la conexión física.
             PreparedStatement ps = con.prepareStatement(sql)) { // Prepara la eliminación.
             
            // Vincula el ID de la tarea al parámetro del WHERE.
            ps.setInt(1, id);
            
            // Ejecuta el borrado físico en MySQL.
            ps.executeUpdate();
        }
    }
}
