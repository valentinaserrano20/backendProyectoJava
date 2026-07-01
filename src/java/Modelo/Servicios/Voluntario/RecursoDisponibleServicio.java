package Modelo.Servicios.Voluntario;

import Modelo.DAO.RecursoDisponibleDAO;
import Modelo.DTO.RecursoDisponibleDTO;
import Modelo.Utilidades.ResponseUtil;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Qué hace: Capa de servicio encargada de controlar la lógica de negocio y mapear las respuestas en JSON para el módulo de Recursos Disponibles.
 * Por qué existe: Actúa como capa de abstracción entre los controladores (servlets) y el acceso a datos (DAO).
 */
public class RecursoDisponibleServicio {
    private final RecursoDisponibleDAO dao = new RecursoDisponibleDAO();

    /**
     * Qué hace: Consulta un listado paginado de recursos asociados a un plan familiar y lo formatea en JSON.
     */
    public String listarRecursos(int planId, int page) {
        try {
            int limit = 10;
            int offset = (page - 1) * limit;
            int total = dao.obtenerTotalRecursos(planId);
            List<RecursoDisponibleDTO> list = dao.listarRecursosPorPlan(planId, limit, offset);

            JSONArray dataArr = new JSONArray();
            for (RecursoDisponibleDTO r : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", r.getId());
                obj.put("location", r.getPlaceName()); // Mapeado a 'location' en el frontend
                obj.put("distance", r.getDistance());
                obj.put("phone", r.getPhone());
                obj.put("description", r.getDescription());
                obj.put("resource_id", r.getResourceTypeId());
                obj.put("resource_name", r.getResourceTypeName());
                obj.put("service", r.getServiceName()); // Servicio padre
                dataArr.put(obj);
            }

            JSONObject paginate = new JSONObject();
            paginate.put("total", total);
            paginate.put("per_page", limit);
            paginate.put("current_page", page);
            paginate.put("last_page", (int) Math.ceil((double) total / limit));

            return ResponseUtil.paginate(dataArr, paginate);
        } catch (Exception e) {
            return ResponseUtil.error("Error al listar recursos disponibles: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Obtiene la información detallada de un recurso disponible por su ID y la formatea con objetos anidados para el frontend.
     */
    public String obtenerRecurso(int id) {
        try {
            RecursoDisponibleDTO r = dao.obtenerRecurso(id);
            if (r == null) {
                return ResponseUtil.error("Recurso disponible no encontrado.");
            }

            JSONObject obj = new JSONObject();
            obj.put("id", r.getId());
            obj.put("location", r.getPlaceName()); // Mapea a location en frontend
            obj.put("distance", r.getDistance());
            obj.put("phone", r.getPhone());
            obj.put("description", r.getDescription());
            obj.put("resource_id", r.getResourceTypeId());
            obj.put("resource_name", r.getResourceTypeName());
            obj.put("resource_service", r.getServiceName()); // Requerido por el modal ver de la SPA

            return ResponseUtil.success(obj);
        } catch (Exception e) {
            return ResponseUtil.error("Error al obtener recurso disponible: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Valida los campos obligatorios del recurso y delega su inserción en el DAO.
     */
    public String crearRecurso(RecursoDisponibleDTO dto) {
        if (dto.getPlaceName() == null || dto.getPlaceName().trim().isEmpty()) {
            return ResponseUtil.error("El nombre de la ubicación del recurso es obligatorio.");
        }
        if (dto.getDistance() < 0) {
            return ResponseUtil.error("La distancia debe ser un valor positivo en metros.");
        }
        if (dto.getResourceTypeId() <= 0) {
            return ResponseUtil.error("Debe seleccionar un tipo de recurso válido.");
        }

        try {
            int newId = dao.crearRecurso(dto);
            JSONObject data = new JSONObject();
            data.put("id", newId);
            
            return ResponseUtil.success("Recurso disponible agregado correctamente.", data);
        } catch (Exception e) {
            return ResponseUtil.error("Error al crear recurso disponible: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Valida y actualiza un recurso existente.
     */
    public String actualizarRecurso(int id, RecursoDisponibleDTO dto) {
        if (dto.getPlaceName() == null || dto.getPlaceName().trim().isEmpty()) {
            return ResponseUtil.error("El nombre de la ubicación del recurso es obligatorio.");
        }
        if (dto.getDistance() < 0) {
            return ResponseUtil.error("La distancia debe ser un valor positivo en metros.");
        }
        if (dto.getResourceTypeId() <= 0) {
            return ResponseUtil.error("Debe seleccionar un tipo de recurso válido.");
        }

        try {
            dao.actualizarRecurso(id, dto);
            return ResponseUtil.success("Recurso disponible actualizado correctamente.");
        } catch (Exception e) {
            return ResponseUtil.error("Error al actualizar recurso disponible: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Elimina un registro de recurso disponible por su ID.
     */
    public String eliminarRecurso(int id) {
        try {
            dao.eliminarRecurso(id);
            return ResponseUtil.success("Recurso disponible eliminado correctamente.");
        } catch (Exception e) {
            return ResponseUtil.error("Error al eliminar recurso disponible: " + e.getMessage());
        }
    }
}
