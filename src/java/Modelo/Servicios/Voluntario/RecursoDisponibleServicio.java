package Modelo.Servicios.Voluntario;

import Modelo.DAO.RecursoDisponibleDAO;
import Modelo.DTO.RecursoDisponibleDTO;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

// Qué hace: Capa de servicio encargada de controlar la lógica de negocio y mapear las respuestas en JSON para el módulo de Recursos Disponibles.
// Por qué existe: Actúa como capa de abstracción entre los controladores (servlets) y el acceso a datos (DAO).
// Qué problema resuelve: Centraliza la validación de campos obligatorios del servidor y estructura las respuestas JSON adaptándolas al contrato de la SPA.
public class RecursoDisponibleServicio {
    private final RecursoDisponibleDAO dao = new RecursoDisponibleDAO();

    // Qué hace: Consulta un listado paginado de recursos asociados a un plan familiar y lo formatea en JSON.
    // Por qué existe: Suministra los datos necesarios para renderizar el feed de recursos comunitarios en la vista SPA.
    // Qué problema resuelve: Realiza los cálculos de offsets y devuelve metadatos de paginación estructurados para el control de páginas.
    public String listarRecursos(int planId, int page) {
        JSONObject res = new JSONObject();
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

            res.put("success", true);
            res.put("data", dataArr);
            res.put("paginate", paginate);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al listar recursos disponibles: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Obtiene la información detallada de un recurso disponible por su ID y la formatea con objetos anidados para el frontend.
    // Por qué existe: Alimenta el modal "Ver más" de solo lectura y rellena el formulario de edición de recursos.
    // Qué problema resuelve: Estructura la respuesta JSON incluyendo propiedades calculadas y alias relacionales.
    public String obtenerRecurso(int id) {
        JSONObject res = new JSONObject();
        try {
            RecursoDisponibleDTO r = dao.obtenerRecurso(id);
            if (r == null) {
                return res.put("success", false).put("message", "Recurso disponible no encontrado.").toString();
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

            res.put("success", true);
            res.put("data", obj);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al obtener recurso disponible: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Valida los campos obligatorios del recurso y delega su inserción en el DAO.
    // Por qué existe: Asegura que no se persistan registros incompletos o erróneos en la base de datos.
    // Qué problema resuelve: Controla formatos de entrada y campos requeridos a nivel de servidor.
    public String crearRecurso(RecursoDisponibleDTO dto) {
        JSONObject res = new JSONObject();
        if (dto.getPlaceName() == null || dto.getPlaceName().trim().isEmpty()) {
            return res.put("success", false).put("message", "El nombre de la ubicación del recurso es obligatorio.").toString();
        }
        if (dto.getDistance() < 0) {
            return res.put("success", false).put("message", "La distancia debe ser un valor positivo en metros.").toString();
        }
        if (dto.getResourceTypeId() <= 0) {
            return res.put("success", false).put("message", "Debe seleccionar un tipo de recurso válido.").toString();
        }

        try {
            int newId = dao.crearRecurso(dto);
            JSONObject data = new JSONObject();
            data.put("id", newId);
            
            res.put("success", true);
            res.put("message", "Recurso disponible agregado correctamente.");
            res.put("data", data);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al crear recurso disponible: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Valida y actualiza un recurso existente.
    // Por qué existe: Permite modificar la información geográfica o detalles del recurso.
    // Qué problema resuelve: Ejecuta la actualización parametrizada en MySQL previa validación.
    public String actualizarRecurso(int id, RecursoDisponibleDTO dto) {
        JSONObject res = new JSONObject();
        if (dto.getPlaceName() == null || dto.getPlaceName().trim().isEmpty()) {
            return res.put("success", false).put("message", "El nombre de la ubicación del recurso es obligatorio.").toString();
        }
        if (dto.getDistance() < 0) {
            return res.put("success", false).put("message", "La distancia debe ser un valor positivo en metros.").toString();
        }
        if (dto.getResourceTypeId() <= 0) {
            return res.put("success", false).put("message", "Debe seleccionar un tipo de recurso válido.").toString();
        }

        try {
            dao.actualizarRecurso(id, dto);
            res.put("success", true);
            res.put("message", "Recurso disponible actualizado correctamente.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al actualizar recurso disponible: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Elimina un registro de recurso disponible por su ID.
    // Por qué existe: Permite dar de baja recursos que ya no estén operativos para la familia.
    // Qué problema resuelve: Limpia el registro físico mediante JDBC de manera directa.
    public String eliminarRecurso(int id) {
        JSONObject res = new JSONObject();
        try {
            dao.eliminarRecurso(id);
            res.put("success", true);
            res.put("message", "Recurso disponible eliminado correctamente.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al eliminar recurso disponible: " + e.getMessage());
        }
        return res.toString();
    }
}
