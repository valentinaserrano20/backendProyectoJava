package Modelo.Servicios.Voluntario;

import Modelo.DAO.ActionPlanDAO;
import Modelo.DTO.ActionPlanDTO;
import org.json.JSONObject;

// Qué hace: Servicio encargado de centralizar la lógica de negocio y las validaciones del Plan de Acción general (cabecera).
// Por qué existe: Actúa como capa de abstracción entre los controladores web (servlets) y el acceso directo a la base de datos (DAO).
// Qué problema resuelve: Valida los datos recibidos antes de persistir, maneja excepciones relacionales y formatea las respuestas JSON que la SPA en el frontend espera consumir.
public class ActionPlanServicio {
    // Qué hace: Instancia el objeto de acceso a datos para planes de acción.
    // Por qué existe: Habilita el guardado y lectura del plan de acción en la base de datos MySQL.
    // Qué pasaría si no estuviera: No se podrían persistir las configuraciones de los planes de acción familiares.
    // Flujo: De aquí pasamos a ActionPlanDAO.
    private final ActionPlanDAO dao = new ActionPlanDAO();

    // Qué hace: Verifica si un plan familiar posee algún registro de tareas en base de datos.
    // Por qué existe: Suministra el veredicto lógico requerido por el frontend para activar el wizard o menú de tres fases.
    // Qué pasaría si no estuviera: La interfaz de usuario del voluntario no sabría si debe pintar el formulario de creación o el de edición.
    public String verificarExistePlan(int planId) {
        JSONObject res = new JSONObject();
        try {
            // Qué hace: Consulta al DAO si la cabecera existe.
            // y luego de esto pasamos a ActionPlanDAO.existePlan, que ejecuta un COUNT en MySQL.
            boolean existe = dao.existePlan(planId);
            JSONObject data = new JSONObject();
            data.put("boolean", existe);
            
            res.put("success", true);
            res.put("data", data);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al verificar existencia de plan de acción: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Obtiene la cabecera de datos del plan de acción por su ID de plan familiar.
    // Por qué existe: Provee los identificadores del coordinador y factor de riesgo para rellenar las listas desplegables del formulario de edición.
    // Qué pasaría si no estuviera: No se podría precargar quién es el coordinador familiar de emergencias ni cuál es el riesgo priorizado en la UI.
    public String obtenerPlan(int planId) {
        JSONObject res = new JSONObject();
        try {
            // Qué hace: Consulta la cabecera del plan de acción.
            // y luego de esto pasamos a ActionPlanDAO.obtenerPlan, que realiza la consulta SELECT de la cabecera.
            ActionPlanDTO dto = dao.obtenerPlan(planId);
            if (dto == null) {
                return res.put("success", false).put("message", "Plan de acción no encontrado.").toString();
            }
            
            JSONObject data = new JSONObject();
            data.put("id", dto.getFamilyPlanId()); // Usamos el ID del plan familiar como identificador del plan de acción
            data.put("member_id", dto.getMemberId());
            dto.setFamilyPlanId(dto.getFamilyPlanId());
            data.put("risk_factor_id", dto.getRiskFactorId());
            
            res.put("success", true);
            res.put("data", data);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al obtener plan de acción: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Valida las claves foráneas obligatorias e inicializa las tres tareas de fase en el DAO.
    // Por qué existe: Atiende la creación inicial del plan desde la primera pestaña (Antes).
    // Qué pasaría si no estuviera: Se insertarían planes de acción vacíos, sin responsables asignados o sin factores de riesgo asociados, corrompiendo el reporte de preparación.
    public String crearPlan(ActionPlanDTO dto) {
        JSONObject res = new JSONObject();
        if (dto.getFamilyPlanId() <= 0) {
            return res.put("success", false).put("message", "El identificador del plan familiar es requerido.").toString();
        }
        if (dto.getMemberId() <= 0) {
            return res.put("success", false).put("message", "Debe seleccionar un miembro familiar responsable.").toString();
        }
        if (dto.getRiskFactorId() <= 0) {
            return res.put("success", false).put("message", "Debe seleccionar un factor de riesgo para el plan.").toString();
        }
        
        try {
            // Qué hace: Inserta el registro del plan de acción en la base de datos.
            // y luego de esto pasamos a ActionPlanDAO.crear, el cual realiza la transacción de inserción en MySQL.
            dao.crear(dto.getFamilyPlanId(), dto.getMemberId(), dto.getRiskFactorId());
            res.put("success", true);
            res.put("message", "Plan de acción guardado y creado correctamente con sus momentos iniciales.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al crear plan de acción: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Valida y actualiza los campos de coordinador y riesgo de las tareas del plan familiar.
    // Por qué existe: Atiende la modificación de la configuración superior desde el formulario.
    // Qué pasaría si no estuviera: Si el voluntario cambia al coordinador familiar o el riesgo prioritario, el cambio no se reflejaría en la base de datos.
    public String actualizarPlan(int planId, ActionPlanDTO dto) {
        JSONObject res = new JSONObject();
        if (dto.getMemberId() <= 0) {
            return res.put("success", false).put("message", "Debe seleccionar un miembro familiar responsable.").toString();
        }
        if (dto.getRiskFactorId() <= 0) {
            return res.put("success", false).put("message", "Debe seleccionar un factor de riesgo para el plan.").toString();
        }
        
        try {
            // Qué hace: Actualiza la cabecera en el DAO.
            // y luego de esto pasamos a ActionPlanDAO.actualizar, que ejecuta el UPDATE en la base de datos MySQL.
            dao.actualizar(planId, dto.getMemberId(), dto.getRiskFactorId());
            res.put("success", true);
            res.put("message", "Plan de acción actualizado correctamente.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al actualizar plan de acción: " + e.getMessage());
        }
        return res.toString();
    }
}
