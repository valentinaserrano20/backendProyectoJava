package Modelo.Servicios.Voluntario;

import Modelo.DAO.PlanFamiliarDAO;
import Modelo.DTO.RegistroPlanDTO;
import org.json.JSONArray;
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
            
            // Empaqueta el estado del plan y las observaciones para consumo del frontend
            data.put("status_plan_id", plan.getStatusPlanId());
            data.put("comentary", plan.getComentary());

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

        // Si la zona no fue provista por el frontend, intentamos recuperar la que ya estaba en la base de datos
        // para evitar que la validación posterior falle y se pierda el dato de zona original.
        if (dto.getZoneId() <= 0) {
            try {
                Modelo.DTO.IdentificacionPlanDTO planExistente = planDAO.obtenerDetallePlan(id);
                if (planExistente != null) {
                    dto.setZoneId(planExistente.getZoneId());
                }
            } catch (Exception e) {
                // Si falla la consulta, dejamos el valor en 0 (será rechazado por la validación)
            }
        }

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

    // Sirve para: Retornar los planes de emergencia familiar pertenecientes a un voluntario específico en formato paginado JSON
    // Qué hace: Realiza validaciones de página, calcula límites y offsets, llama al DAO y arma la estructura JSON requerida
    // Por qué es importante: El frontend espera el listado bajo la clave 'data' y los metadatos de paginación bajo 'paginate'
    public String listarPlanesPaginado(int voluntarioId, int page) {
        // Inicializa el objeto JSON de respuesta
        JSONObject res = new JSONObject();
        try {
            // Define el límite de registros por página
            int limit = 10;
            // Asegura que la página solicitada sea válida
            if (page < 1) page = 1;
            // Calcula el desplazamiento (offset) para la consulta SQL
            int offset = (page - 1) * limit;

            // Obtiene el número total de planes del voluntario
            int total = planDAO.contarPlanesPorVoluntario(voluntarioId);
            // Obtiene la lista de planes familiares del voluntario
            java.util.List<java.util.Map<String, Object>> list = planDAO.listarPlanesPorVoluntario(voluntarioId, limit, offset);

            // Instancia un arreglo JSON para almacenar las tarjetas
            JSONArray dataArr = new JSONArray();
            // Recorre la lista de mapas obtenida del DAO
            for (java.util.Map<String, Object> map : list) {
                // Crea un objeto JSON para representar cada plan
                JSONObject obj = new JSONObject(map);
                // Agrega el objeto al arreglo JSON
                dataArr.put(obj);
            }

            // Calcula el número de la última página disponible
            int lastPage = (int) Math.ceil((double) total / limit);
            if (lastPage < 1) lastPage = 1;

            // Crea un objeto JSON para almacenar la metadata de paginación
            JSONObject paginate = new JSONObject();
            // Guarda el conteo total de registros
            paginate.put("total", total);
            // Guarda el límite de ítems por página
            paginate.put("per_page", limit);
            // Guarda el número de página actual
            paginate.put("current_page", page);
            // Guarda el total de páginas calculado
            paginate.put("last_page", lastPage);

            // Mapea la confirmación de éxito en la respuesta principal
            res.put("success", true);
            // Mapea los registros de planes en el nodo 'data'
            res.put("data", dataArr);
            // Mapea la información de paginación en el nodo 'paginate'
            res.put("paginate", paginate);

        } catch (Exception e) {
            // Mapea el estado de error en caso de excepción
            res.put("success", false);
            // Asigna el mensaje de error para informar al cliente
            res.put("message", "Error al listar los planes familiares: " + e.getMessage());
        }
        // Devuelve el JSON serializado como String
        return res.toString();
    }
}