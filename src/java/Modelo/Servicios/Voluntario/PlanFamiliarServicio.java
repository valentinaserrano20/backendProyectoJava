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

    // Sirve para: Obtener el detalle completo de precarga de un plan familiar y
    // retornarlo en un JSON estructurado
    // Qué hace: Consulta al DAO el DTO del plan y empaqueta en el nodo "data" todos
    // los campos de geografía, sector, dirección, teléfono y calidad.
    // Por qué es importante: Permite al frontend autorrellenar de forma íntegra el
    // formulario de datos principales sin limpiar ni omitir campos existentes.
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
            // Qué hace: Añade llaves adicionales para zona, dirección, departamento,
            // ciudad, sector y calidad al objeto "data"
            // Por qué es importante: El frontend depende de estas propiedades para
            // sincronizar correctamente los inputs del formulario
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

    // Sirve para: Procesar y persistir la actualización de la fase de
    // identificación del plan familiar
    // Qué hace: Valida los campos de entrada y llama al DAO para guardar los
    // cambios en la base de datos
    // Por qué es importante: Garantiza consistencia en las reglas de negocio al
    // guardar, previniendo datos inválidos en la base de datos
    public String guardarIdentificacion(int id, Modelo.DTO.ActualizarIdentificacionDTO dto) {
        // Inicializa el objeto JSON de respuesta
        JSONObject res = new JSONObject();

        // Si la zona no fue provista por el frontend, intentamos recuperar la que ya
        // estaba en la base de datos
        // para evitar que la validación posterior falle y se pierda el dato de zona
        // original.
        if (dto.getZoneId() <= 0) {
            try {
                // Qué hace: Consulta el plan existente para no perder el dato de zona original
                // si no viene en el payload.
                // y luego de esto pasamos a PlanFamiliarDAO.obtenerDetallePlan para extraer los
                // datos de la fila original.
                Modelo.DTO.IdentificacionPlanDTO planExistente = planDAO.obtenerDetallePlan(id);
                if (planExistente != null) {
                    dto.setZoneId(planExistente.getZoneId());
                }
            } catch (Exception e) {
                // Fallo de consulta ignorado, caerá en la validación
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
            return res.put("success", false).put("message", "Selección de zona, sector o calidad de vivienda inválida.")
                    .toString();
        }

        try {
            // Qué hace: Ejecuta la actualización de los datos del plan.
            // y luego de esto pasamos a PlanFamiliarDAO.actualizarIdentificacion, el cual
            // corre la sentencia UPDATE.
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

    // Qué hace: Comprueba si el usuario tiene acceso asignado o de propiedad sobre
    // el plan familiar.
    // Por qué existe: Evita brechas de seguridad donde un voluntario modifique
    // planes creados por otros.
    // Qué pasaría si no estuviera: Un voluntario malintencionado podría ver o
    // alterar datos de cualquier otra vivienda.
    public String verificarAccesoAPlan(int planId, int usuarioId) {
        JSONObject res = new JSONObject();
        try {
            // Qué hace: Consulta al DAO si el usuario posee permiso de autor sobre el plan.
            // y luego de esto pasamos a PlanFamiliarDAO.verificarAcceso, que ejecuta una
            // consulta COUNT en MySQL.
            boolean tieneAcceso = planDAO.verificarAcceso(planId, usuarioId);
            JSONObject data = new JSONObject();
            data.put("access_check", tieneAcceso);
            return res.put("success", true).put("data", data).toString();
        } catch (Exception e) {
            return res.put("success", false).put("message", "Error al verificar acceso: " + e.getMessage()).toString();
        }
    }

    // Qué hace: Comprueba si hay por lo menos un integrante registrado en el plan
    // familiar.
    // Por qué existe: El plan de emergencias necesita que exista al menos una
    // persona para que tenga sentido calificar el test.
    // Qué pasaría si no estuviera: Se calificaría la vulnerabilidad del hogar de
    // viviendas completamente vacías sin integrantes humanos.
    public String verificarTieneIntegrantes(int planId) {
        JSONObject res = new JSONObject();
        try {
            // Qué hace: Consulta el total de integrantes del plan.
            // y luego de esto pasamos a PlanFamiliarDAO.tieneIntegrantes, el cual verifica
            // si la tabla de integrantes posee filas para este ID de plan.
            boolean hasMembers = planDAO.tieneIntegrantes(planId);
            JSONObject data = new JSONObject();
            data.put("has_members", hasMembers);
            return res.put("success", true).put("data", data).toString();
        } catch (Exception e) {
            return res.put("success", false).put("message", "Error al verificar integrantes: " + e.getMessage())
                    .toString();
        }
    }

    // Qué hace: Retorna la lista paginada de planes de emergencia en formato JSON.
    // Por qué existe: Adapta los límites de visualización de planes en la tabla del
    // panel según el rol (Supervisor lee todo, Voluntario solo lo propio).
    // Qué pasaría si no estuviera: El backend enviaría miles de registros al
    // navegador en una sola llamada, colapsando el rendimiento del cliente.
    public String listarPlanesPaginado(int usuarioId, int page) {
        // Inicializa el objeto JSON de respuesta
        JSONObject res = new JSONObject();
        try {
            // Define el límite de registros por página
            int limit = 10;
            // Asegura que la página solicitada sea válida
            if (page < 1)
                page = 1;
            // Calcula el desplazamiento (offset) para la consulta SQL
            int offset = (page - 1) * limit;

            // Qué hace: Obtiene el rol del usuario actual.
            // y luego de esto pasamos a PlanFamiliarDAO.obtenerRolUsuario para hacer la
            // consulta.
            int rolId = planDAO.obtenerRolUsuario(usuarioId);
            int total;
            java.util.List<java.util.Map<String, Object>> list;

            if (rolId == 2) {
                // Qué hace: Consulta el total de planes y la lista para supervisor.
                // y luego de esto pasamos a PlanFamiliarDAO.contarTodosLosPlanes y
                // PlanFamiliarDAO.listarTodosLosPlanes.
                total = planDAO.contarTodosLosPlanes();
                list = planDAO.listarTodosLosPlanes(limit, offset);
            } else {
                // Qué hace: Consulta el total de planes y la lista específicos para el
                // voluntario en sesión.
                // y luego de esto pasamos a PlanFamiliarDAO.contarPlanesPorVoluntario y
                // PlanFamiliarDAO.listarPlanesPorVoluntario.
                total = planDAO.contarPlanesPorVoluntario(usuarioId);
                list = planDAO.listarPlanesPorVoluntario(usuarioId, limit, offset);
            }

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
            if (lastPage < 1)
                lastPage = 1;

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