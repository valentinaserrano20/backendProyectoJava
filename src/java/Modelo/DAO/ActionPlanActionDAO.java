package Modelo.DAO;

import Modelo.Config.Conexion;
import Modelo.DTO.ActionPlanActionDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Qué hace: DAO encargado de realizar operaciones CRUD sobre la tabla plan_accion para micro-acciones específicas de las fases (antes, durante, después).
// Por qué existe: Provee acceso parametrizado a MySQL para registrar, modificar, listar y borrar tareas individuales por momento.
// Qué problema resuelve: Encapsula el acceso JDBC directo, previniendo inyección SQL y resolviendo dinámicamente claves foráneas como el riesgo_id.
public class ActionPlanActionDAO {

    // Qué hace: Obtiene el riesgo_id actualmente configurado para un plan familiar en la tabla plan_accion.
    // Por qué existe: Al agregar una nueva acción individual, el frontend no envía el riesgo_id; se debe consultar desde la configuración existente.
    // Qué problema resuelve: Asegura la consistencia referencial con la tabla de factores de riesgo sin requerir que la vista exponga o envíe datos redundantes.
    public int obtenerRiesgoIdPorPlan(int planId) throws SQLException {
        // SELECT recupera la columna riesgo_id de la tabla plan_accion.
        // WHERE plan_id = ? filtra por el ID del plan de emergencia familiar.
        // LIMIT 1 limita el resultado a una única fila (optimización).
        String sql = "SELECT riesgo_id FROM plan_accion WHERE plan_id = ? LIMIT 1";
        
        // try-with-resources: Inicializa y administra de forma segura la conexión y el statement.
        try (Connection con = Conexion.obtener(); // Abre la conexión física con MySQL.
             PreparedStatement ps = con.prepareStatement(sql)) { // Prepara la consulta parametrizada.
             
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

    // Qué hace: Obtiene la lista de todas las acciones/tareas específicas registradas para un plan familiar, uniendo los nombres de los responsables.
    // Por qué existe: Alimenta las tarjetas visuales de las tres pestañas en la pantalla del voluntario.
    // Qué problema resuelve: Realiza una unión (JOIN) con la tabla integrantes para retornar los nombres concatenados en una sola petición.
    public List<ActionPlanActionDTO> listarPorPlan(int planId) throws SQLException {
        // SELECT recupera los atributos de la sub-tarea de plan_accion (pa) y los nombres del integrante (i).
        // LEFT JOIN se utiliza porque un plan de acción puede no tener un coordinador familiar asignado (el campo coordinador_id es nulo).
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
                    int actionTypeId = 1; // Default a 1 ("antes")
                    if ("durante".equals(momentoStr)) {
                        actionTypeId = 2; // Tipo 2 ("durante")
                    } else if ("despues".equals(momentoStr)) {
                        actionTypeId = 3; // Tipo 3 ("despues")
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

    // Qué hace: Obtiene los detalles completos de una micro-acción específica por su ID.
    // Por qué existe: Es consumido al abrir el modal "Ver/Editar/Eliminar" en la SPA.
    // Qué problema resuelve: Mapea los campos de una sola fila de base de datos junto con el responsable en un DTO.
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

    // Qué hace: Inserta una nueva micro-acción en la tabla plan_accion.
    // Por qué existe: Permite agregar tareas personalizadas en cualquiera de los tres momentos del plan.
    // Qué problema resuelve: Resuelve y asocia de forma atómica el riesgo_id y maneja la asignación de nulos si no hay miembro id.
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

    // Qué hace: Actualiza la descripción y el miembro coordinador responsable para una tarea específica.
    // Por qué existe: Atiende la solicitud del usuario al editar una acción en los modales.
    // Qué problema resuelve: Modifica únicamente las columnas deseadas sin tocar llaves del plan o momento.
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

    // Qué hace: Elimina físicamente el registro de la tarea de la base de datos por su ID.
    // Por qué existe: Permite descartar micro-acciones desde el modal de eliminación.
    // Qué problema resuelve: Borra el registro en MySQL liberando memoria y actualizando la interfaz SPA al recargar.
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
