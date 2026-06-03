package Modelo.Servicios.Voluntario;

import Modelo.DAO.ActionPlanActionDAO;
import Modelo.DTO.ActionPlanActionDTO;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

// Qué hace: Servicio encargado de centralizar la lógica de negocio y validación para las micro-acciones individuales de las tres fases.
// Por qué existe: Une la capa del servlet de acciones con la persistencia JDBC realizándole validaciones de negocio.
// Qué problema resuelve: Formatea colecciones o DTOs individuales a la respuesta JSON estructurada y compatible con los modales SweetAlert.
public class ActionPlanActionServicio {
    private final ActionPlanActionDAO dao = new ActionPlanActionDAO();

    // Qué hace: Obtiene todas las acciones asignadas a un plan familiar y las empaqueta en un JSONArray.
    // Por qué existe: Suministra el origen de datos para renderizar las tarjetas visuales inferiores del plan.
    // Qué problema resuelve: Formatea cada fila a JSON compatible con las propiedades leídas por la SPA.
    public String listarPorPlan(int planId) {
        JSONObject res = new JSONObject();
        try {
            List<ActionPlanActionDTO> lista = dao.listarPorPlan(planId);
            JSONArray array = new JSONArray();
            
            for (ActionPlanActionDTO item : lista) {
                JSONObject jsonItem = new JSONObject();
                jsonItem.put("id", item.getId());
                jsonItem.put("description", item.getDescription());
                jsonItem.put("action_type_id", item.getActionTypeId());
                jsonItem.put("action_plan_id", item.getActionPlanId());
                jsonItem.put("member_id", item.getMemberId());
                jsonItem.put("member_name", item.getMemberName());
                
                JSONObject memberObj = new JSONObject();
                memberObj.put("names", item.getMember().getNames());
                memberObj.put("last_names", item.getMember().getLast_names());
                jsonItem.put("member", memberObj);
                
                array.put(jsonItem);
            }
            
            res.put("success", true);
            res.put("data", array);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al listar acciones del plan: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Obtiene la información detallada de una única acción por su identificador.
    // Por qué existe: Sirve de insumo para el modal de vista individual y edición de una acción.
    // Qué problema resuelve: Devuelve una respuesta JSON estructurada con el miembro anidado requerido.
    public String obtenerPorId(int id) {
        JSONObject res = new JSONObject();
        try {
            ActionPlanActionDTO dto = dao.obtenerPorId(id);
            if (dto == null) {
                return res.put("success", false).put("message", "Acción no encontrada.").toString();
            }
            
            JSONObject data = new JSONObject();
            data.put("id", dto.getId());
            data.put("description", dto.getDescription());
            data.put("action_type_id", dto.getActionTypeId());
            data.put("action_plan_id", dto.getActionPlanId());
            data.put("member_id", dto.getMemberId());
            
            JSONObject memberObj = new JSONObject();
            memberObj.put("names", dto.getMember().getNames());
            memberObj.put("last_names", dto.getMember().getLast_names());
            data.put("member", memberObj);
            
            res.put("success", true);
            res.put("data", data);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al obtener acción: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Valida los campos obligatorios e inserta una nueva tarea individual en base de datos.
    // Por qué existe: Procesa la creación de micro-acciones desde el botón (+) en el modal de SweetAlert.
    // Qué problema resuelve: Valida la descripción y asocia el plan familiar devolviendo una respuesta de éxito/error estructurada.
    public String crear(ActionPlanActionDTO dto) {
        JSONObject res = new JSONObject();
        if (dto.getDescription() == null || dto.getDescription().trim().isEmpty()) {
            return res.put("success", false).put("message", "La descripción de la acción es obligatoria.").toString();
        }
        if (dto.getActionPlanId() <= 0) {
            return res.put("success", false).put("message", "El identificador del plan de acción es inválido.").toString();
        }
        if (dto.getActionTypeId() < 1 || dto.getActionTypeId() > 3) {
            return res.put("success", false).put("message", "El tipo de momento o fase no es válido.").toString();
        }
        
        try {
            dao.crear(dto);
            res.put("success", true);
            res.put("message", "Acción agregada correctamente.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al guardar la acción: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Valida y actualiza los campos de una micro-acción existente.
    // Por qué existe: Atiende el guardado del formulario de modificación de micro-acciones en el modal.
    // Qué problema resuelve: Ejecuta la lógica de actualización en base de datos retornando confirmación formateada en JSON.
    public String actualizar(int id, ActionPlanActionDTO dto) {
        JSONObject res = new JSONObject();
        if (dto.getDescription() == null || dto.getDescription().trim().isEmpty()) {
            return res.put("success", false).put("message", "La descripción de la acción no puede quedar vacía.").toString();
        }
        
        try {
            dao.actualizar(id, dto);
            res.put("success", true);
            res.put("message", "Acción actualizada correctamente.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al actualizar la acción: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Elimina una acción en base de datos.
    // Por qué existe: Atiende la solicitud del botón borrar de la ventana interactiva.
    // Qué problema resuelve: Remueve permanentemente la fila en base de datos por su ID principal.
    public String eliminar(int id) {
        JSONObject res = new JSONObject();
        try {
            dao.eliminar(id);
            res.put("success", true);
            res.put("message", "Acción eliminada correctamente.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al eliminar la acción: " + e.getMessage());
        }
        return res.toString();
    }
}
