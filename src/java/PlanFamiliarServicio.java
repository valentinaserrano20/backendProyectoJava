package Modelo.Servicios.Voluntario;

import Modelo.DAO.PlanFamiliarDAO;
import Modelo.DTO.RegistroPlanDTO;
import org.json.JSONObject;

public class PlanFamiliarServicio {
    private final PlanFamiliarDAO planDAO = new PlanFamiliarDAO();

    public String registrarNuevoPlan(RegistroPlanDTO dto) {
        JSONObject res = new JSONObject();

        // Validaciones rigurosas de lógica de negocio
        if (dto.getLastNames() == null || dto.getLastNames().trim().isEmpty()) {
            return res.put("success", false).put("message", "Falta el apellido de la familia.").toString();
        }
        if (dto.getZoneId() <= 0 || dto.getOrganizacionId() <= 0) {
            return res.put("success", false).put("message", "Selección de zona geográfica inválida.").toString();
        }

        try {
            int idPlan = planDAO.registrarPasoInicial(dto);
            
            JSONObject data = new JSONObject();
            data.put("id", idPlan);

            return res.put("success", true)
                      .put("message", "Registro familiar inicializado correctamente.")
                      .put("data", data)
                      .toString();
        } catch (Exception e) {
            return res.put("success", false)
                      .put("message", "Error de persistencia en el servidor: " + e.getMessage())
                      .toString();
        }
    }

    // Obtiene el detalle básico de precarga de un plan familiar y lo retorna en formato JSON String
    public String obtenerPlanDetallado(int id) {
        // Inicializa el objeto de respuesta JSON principal
        JSONObject res = new JSONObject();
        try {
            // Llama al DAO para recuperar el detalle del plan familiar
            Modelo.DTO.IdentificacionPlanDTO plan = planDAO.obtenerDetallePlan(id);
            // Si el plan no fue encontrado
            if (plan == null) {
                // Retorna un error indicando la no existencia
                return res.put("success", false).put("message", "Plan familiar no encontrado.").toString();
            }

            // Mapea los valores del DTO a un objeto JSON
            JSONObject data = new JSONObject();
            data.put("id", plan.getId());
            data.put("last_names", plan.getLastNames());
            data.put("family_type", plan.getFamilyType());

            // Agrega el éxito de la operación
            res.put("success", true);
            // Envuelve el objeto en el nodo "data" requerido por el frontend
            res.put("data", data);
        } catch (Exception e) {
            // Captura cualquier excepción de base de datos e informa al cliente
            res.put("success", false);
            res.put("message", "Error de lectura en el servidor: " + e.getMessage());
        }
        // Devuelve el JSON estructurado
        return res.toString();
    }

    // Procesa y persiste la actualización de la fase de identificación del plan familiar
    public String guardarIdentificacion(int id, Modelo.DTO.ActualizarIdentificacionDTO dto) {
        // Inicializa el objeto JSON de respuesta
        JSONObject res = new JSONObject();

        // Validaciones rigurosas de la lógica de negocio
        if (dto.getLastNames() == null || dto.getLastNames().trim().isEmpty()) {
            return res.put("success", false).put("message", "Faltan los apellidos de la familia.").toString();
        }
        if (dto.getAddress() == null || dto.getAddress().trim().isEmpty()) {
            return res.put("success", false).put("message", "Falta la dirección de la vivienda.").toString();
        }
        if (dto.getSectorId() <= 0 || dto.getHousingQualityId() <= 0) {
            return res.put("success", false).put("message", "Selección de sector o calidad de vivienda inválida.").toString();
        }

        try {
            // Invoca al DAO para realizar el UPDATE sobre la base de datos
            planDAO.actualizarIdentificacion(id, dto);
            // Retorna respuesta de éxito y el mensaje informativo correspondiente
            return res.put("success", true)
                      .put("message", "Identificación familiar registrada correctamente.")
                      .toString();
        } catch (Exception e) {
            // Captura errores e informa del fallo de persistencia
            return res.put("success", false)
                      .put("message", "Error de persistencia en el servidor: " + e.getMessage())
                      .toString();
        }
    }
}