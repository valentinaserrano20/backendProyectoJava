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

    // Sirve para: Obtener el detalle completo de precarga de un plan familiar y retornarlo en un JSON estructurado
    // Qué hace: Consulta al DAO el DTO del plan y empaqueta en el nodo "data" todos los campos de geografía, sector, dirección, teléfono y calidad.
    // Por qué es importante: Permite al frontend autorrellenar de forma íntegra el formulario de datos principales sin limpiar ni omitir campos existentes.
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
            
            // Sirve para: Empaquetar los campos extendidos recuperados del plan familiar
            // Qué hace: Añade llaves adicionales para zona, dirección, departamento, ciudad, sector y calidad al objeto "data"
            // Por qué es importante: El frontend depende de estas propiedades para sincronizar correctamente los inputs del formulario
            data.put("zone_id", plan.getZoneId());
            data.put("department_id", plan.getDepartmentId());
            data.put("city_id", plan.getCityId());
            data.put("address", plan.getAddress());
            data.put("sector_id", plan.getSectorId());
            data.put("sector_name", plan.getSectorName());
            data.put("landline_phone", plan.getLandlinePhone());
            data.put("housing_quality_id", plan.getHousingQualityId());

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

    // Sirve para: Procesar y persistir la actualización de la fase de identificación del plan familiar
    // Qué hace: Valida los campos de entrada y llama al DAO para guardar los cambios en la base de datos
    // Por qué es importante: Garantiza consistencia en las reglas de negocio al guardar, previniendo datos inválidos en la base de datos
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
        if (dto.getZoneId() <= 0 || dto.getSectorId() <= 0 || dto.getHousingQualityId() <= 0) {
            return res.put("success", false).put("message", "Selección de zona, sector o calidad de vivienda inválida.").toString();
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

    // Sirve para: Procesar la solicitud de validación de acceso al plan
    // Qué hace: Llama al DAO para determinar el acceso del usuario y empaqueta el resultado en JSON
    // Por qué es importante: El frontend utiliza la propiedad 'access_check' para bloquear o permitir el ingreso
    public String verificarAccesoAPlan(int planId, int usuarioId) {
        JSONObject res = new JSONObject();
        try {
            boolean tieneAcceso = planDAO.verificarAcceso(planId, usuarioId);
            JSONObject data = new JSONObject();
            data.put("access_check", tieneAcceso);
            return res.put("success", true).put("data", data).toString();
        } catch (Exception e) {
            return res.put("success", false).put("message", "Error al verificar acceso: " + e.getMessage()).toString();
        }
    }

    // Sirve para: Comprobar si el plan familiar posee integrantes registrados
    // Qué hace: Consulta al DAO el conteo de integrantes y lo encapsula en un JSON
    // Por qué es importante: Evita que el voluntario intente continuar con el test de vulnerabilidad sin registrar integrantes
    public String verificarTieneIntegrantes(int planId) {
        JSONObject res = new JSONObject();
        try {
            boolean hasMembers = planDAO.tieneIntegrantes(planId);
            JSONObject data = new JSONObject();
            data.put("has_members", hasMembers);
            return res.put("success", true).put("data", data).toString();
        } catch (Exception e) {
            return res.put("success", false).put("message", "Error al verificar integrantes: " + e.getMessage()).toString();
        }
    }
}