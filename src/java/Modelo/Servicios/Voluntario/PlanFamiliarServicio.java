package Modelo.Servicios.Voluntario;

import Modelo.DAO.PlanFamiliarDAO;
import Modelo.DTO.RegistroPlanDTO;
import Modelo.Utilidades.ResponseUtil;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Qué hace: Lógica de negocio y servicio de control para la gestión de los planes familiares de emergencias.
 * Por qué existe: Actúa como capa intermedia (Service) que orquesta las llamadas al DAO, procesa validaciones de negocio y formatea las respuestas JSON.
 * Qué problema resuelve: Separa la lógica de procesamiento JSON y las reglas de negocio de los servlets de presentación.
 * Se implementó ResponseUtil para homogeneizar las respuestas hacia el frontend y reducir el boilerplate de bloques try-catch.
 */
public class PlanFamiliarServicio {
    private final PlanFamiliarDAO planDAO = new PlanFamiliarDAO();

    /**
     * Qué hace: Registra un nuevo plan familiar y su respectivo paso de identificación inicial.
     * Por qué se hizo: Crea el plan en base de datos.
     * Qué significa: Valida la información e inicializa el censo familiar en MySQL, retornando el ID generado.
     */
    public String registrarNuevoPlan(RegistroPlanDTO dto) {
        if (dto.getLastNames() == null || dto.getLastNames().trim().isEmpty()) {
            return ResponseUtil.error("Falta el apellido de la familia.");
        }
        if (dto.getZoneId() <= 0 || dto.getOrganizacionId() <= 0) {
            return ResponseUtil.error("Selección de zona geográfica inválida.");
        }

        try {
            int idPlan = planDAO.registrarPasoInicial(dto);
            JSONObject data = new JSONObject();
            data.put("id", idPlan);
            return ResponseUtil.success("Registro familiar inicializado correctamente.", data);
        } catch (Exception e) {
            return ResponseUtil.error("Error de persistencia en el servidor: " + e.getMessage());
        }
    }

    /**
     * Sirve para: Obtener el detalle completo de precarga de un plan familiar y retornarlo en un JSON estructurado.
     * Qué hace: Consulta al DAO el DTO del plan y empaqueta en el nodo "data" todos los campos demográficos y de geografía.
     * Por qué es importante: El frontend depende de estas propiedades para sincronizar correctamente los inputs del formulario.
     */
    public String obtenerPlanDetallado(int id) {
        try {
            Modelo.DTO.IdentificacionPlanDTO plan = planDAO.obtenerDetallePlan(id);
            if (plan == null) {
                return ResponseUtil.error("Plan familiar no encontrado.");
            }

            JSONObject data = new JSONObject();
            data.put("id", plan.getId());
            data.put("last_names", plan.getLastNames());
            data.put("family_type", plan.getFamilyType());
            data.put("zone_id", plan.getZoneId());
            data.put("department_id", plan.getDepartmentId());
            data.put("city_id", plan.getCityId());
            data.put("address", plan.getAddress());
            data.put("sector_id", plan.getSectorId());
            data.put("sector_name", plan.getSectorName());
            data.put("landline_phone", plan.getLandlinePhone());
            data.put("housing_quality_id", plan.getHousingQualityId());
            data.put("status_plan_id", plan.getStatusPlanId());
            data.put("comentary", plan.getComentary());

            return ResponseUtil.success(data);
        } catch (Exception e) {
            return ResponseUtil.error("Error de lectura en el servidor: " + e.getMessage());
        }
    }

    /**
     * Sirve para: Procesar y persistir la actualización de la fase de identificación del plan familiar.
     * Qué hace: Valida los campos de entrada y llama al DAO para guardar los cambios en la base de datos.
     * Por qué es importante: Garantiza consistencia en las reglas de negocio al guardar, previniendo datos inválidos en la base de datos.
     */
    public String guardarIdentificacion(int id, Modelo.DTO.ActualizarIdentificacionDTO dto) {
        if (dto.getZoneId() <= 0) {
            try {
                Modelo.DTO.IdentificacionPlanDTO planExistente = planDAO.obtenerDetallePlan(id);
                if (planExistente != null) {
                    dto.setZoneId(planExistente.getZoneId());
                }
            } catch (Exception e) {
                // Fallo de consulta ignorado, caerá en la validación
            }
        }

        if (dto.getLastNames() == null || dto.getLastNames().trim().isEmpty()) {
            return ResponseUtil.error("Faltan los apellidos de la familia.");
        }
        if (dto.getAddress() == null || dto.getAddress().trim().isEmpty()) {
            return ResponseUtil.error("Falta la dirección de la vivienda.");
        }
        if (dto.getZoneId() <= 0 || dto.getSectorId() <= 0 || dto.getHousingQualityId() <= 0) {
            return ResponseUtil.error("Selección de zona, sector o calidad de vivienda inválida.");
        }

        try {
            planDAO.actualizarIdentificacion(id, dto);
            return ResponseUtil.success("Identificación familiar registrada correctamente.");
        } catch (Exception e) {
            return ResponseUtil.error("Error de persistencia en el servidor: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Comprueba si el usuario tiene acceso asignado o de propiedad sobre el plan familiar.
     * Por qué se hizo: Evita brechas de seguridad donde un voluntario modifique planes ajenos.
     * Qué significa: Retorna true/false en la clave access_check envuelto en un JSON de éxito.
     */
    public String verificarAccesoAPlan(int planId, int usuarioId) {
        try {
            boolean tieneAcceso = planDAO.verificarAcceso(planId, usuarioId);
            JSONObject data = new JSONObject();
            data.put("access_check", tieneAcceso);
            return ResponseUtil.success(data);
        } catch (Exception e) {
            return ResponseUtil.error("Error al verificar acceso: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Comprueba si hay por lo menos un integrante registrado en el plan familiar.
     * Por qué se hizo: El plan de emergencias necesita que exista al menos una persona registrada para calificar test o riesgos.
     * Qué significa: Retorna true/false en la clave has_members envuelto en un JSON de éxito.
     */
    public String verificarTieneIntegrantes(int planId) {
        try {
            boolean hasMembers = planDAO.tieneIntegrantes(planId);
            JSONObject data = new JSONObject();
            data.put("has_members", hasMembers);
            return ResponseUtil.success(data);
        } catch (Exception e) {
            return ResponseUtil.error("Error al verificar integrantes: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Retorna la lista paginada de planes de emergencia en formato JSON.
     * Por qué se hizo: Adapta los límites de visualización de planes en la tabla del panel según el rol (Supervisor lee todo, Voluntario solo lo propio).
     * Qué significa: Retorna un JSON con la lista de planes familiares paginada según el perfil del usuario.
     */
    public String listarPlanesPaginado(int usuarioId, int page) {
        try {
            int limit = 10;
            if (page < 1) page = 1;
            int offset = (page - 1) * limit;

            int rolId = planDAO.obtenerRolUsuario(usuarioId);
            int total;
            java.util.List<java.util.Map<String, Object>> list;

            if (rolId == 2) {
                total = planDAO.contarTodosLosPlanes();
                list = planDAO.listarTodosLosPlanes(limit, offset);
            } else {
                total = planDAO.contarPlanesPorVoluntario(usuarioId);
                list = planDAO.listarPlanesPorVoluntario(usuarioId, limit, offset);
            }

            JSONArray dataArr = new JSONArray();
            for (java.util.Map<String, Object> map : list) {
                JSONObject obj = new JSONObject(map);
                dataArr.put(obj);
            }

            int lastPage = (int) Math.ceil((double) total / limit);
            if (lastPage < 1) lastPage = 1;

            JSONObject paginate = new JSONObject();
            paginate.put("total", total);
            paginate.put("per_page", limit);
            paginate.put("current_page", page);
            paginate.put("last_page", lastPage);

            return ResponseUtil.paginate(dataArr, paginate);
        } catch (Exception e) {
            return ResponseUtil.error("Error al listar los planes familiares: " + e.getMessage());
        }
    }
}