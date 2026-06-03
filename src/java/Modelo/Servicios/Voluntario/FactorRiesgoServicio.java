package Modelo.Servicios.Voluntario;

import Modelo.DAO.FactorRiesgoDAO;
import Modelo.DTO.FactorRiesgoDTO;
import Modelo.DTO.AccionReduccionDTO;
import Modelo.DTO.FactorVulnerabilidadDTO;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

// Qué hace: Servicio que encapsula la lógica de negocio y serialización JSON para los factores de riesgo, sus acciones y vulnerabilidades.
// Por qué existe: Actúa como intermediario entre los Servlets del controlador y el DAO de persistencia, formateando datos según lo espera la SPA.
// Qué problema resuelve: Valida la obligatoriedad de campos del lado del servidor, gestiona transacciones y formatea los datos en JSON.
public class FactorRiesgoServicio {
    private final FactorRiesgoDAO dao = new FactorRiesgoDAO();

    // =========================================================================
    // SERVICIOS: FACTORES DE RIESGO
    // =========================================================================

    // Qué hace: Retorna todos los factores de riesgo (para el Supervisor) en JSON.
    // Por qué existe: Permite al supervisor consultar la lista global de factores para filtrar por plan.
    // Qué problema resuelve: Convierte la lista de DTOs en un JSONArray encapsulado.
    public String obtenerTodos() {
        JSONObject res = new JSONObject();
        try {
            List<FactorRiesgoDTO> list = dao.obtenerTodos();
            JSONArray arr = new JSONArray();
            for (FactorRiesgoDTO f : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", f.getId());
                obj.put("description", f.getDescription());
                obj.put("ubication", f.getUbication());
                obj.put("distance", f.getDistance());
                obj.put("family_plan_id", f.getFamilyPlanId());
                obj.put("threat_type_id", f.getThreatTypeId());
                obj.put("threat_type_name", f.getThreatTypeName() != null ? f.getThreatTypeName() : "");
                arr.put(obj);
            }
            res.put("success", true);
            res.put("data", arr);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al recuperar todos los factores de riesgo: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Consulta los factores de riesgo de un plan de forma paginada.
    // Por qué existe: Suministra el feed de riesgos a la grilla visual de la SPA.
    // Qué problema resuelve: Provee estructura paginada con per_page, current_page y last_page.
    public String listarFactores(int planId, int page) {
        JSONObject res = new JSONObject();
        try {
            int limit = 10;
            int offset = (page - 1) * limit;
            int total = dao.contarPorPlan(planId);
            List<FactorRiesgoDTO> list = dao.obtenerPorPlanPaginado(planId, limit, offset);

            JSONArray dataArr = new JSONArray();
            for (FactorRiesgoDTO f : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", f.getId());
                obj.put("description", f.getDescription());
                obj.put("ubication", f.getUbication());
                obj.put("distance", f.getDistance());
                obj.put("family_plan_id", f.getFamilyPlanId());
                obj.put("threat_type_id", f.getThreatTypeId());
                obj.put("threat_type_name", f.getThreatTypeName() != null ? f.getThreatTypeName() : "");
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
            res.put("message", "Error al listar factores de riesgo: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Retorna los factores de riesgo de un plan en formato simple de selección.
    // Por qué existe: Popula los dropdowns de "Factores de Riesgo" en el plan de acción (antes, durante, despues).
    // Qué problema resuelve: Retorna un listado compacto que mapea id, threat_type_name y description.
    public String listarFactoresSelect(int planId) {
        JSONObject res = new JSONObject();
        try {
            List<FactorRiesgoDTO> list = dao.obtenerPorPlan(planId);
            JSONArray arr = new JSONArray();
            for (FactorRiesgoDTO f : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", f.getId());
                obj.put("threat_type_name", f.getThreatTypeName() != null ? f.getThreatTypeName() : "");
                obj.put("description", f.getDescription());
                arr.put(obj);
            }
            res.put("success", true);
            res.put("data", arr);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al listar factores para selección: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Recupera un único factor de riesgo por su ID mapeándolo en un formato que coincide con el modal ver.
    // Por qué existe: Llena de datos la vista detalle o la pantalla de edición del riesgo.
    // Qué problema resuelve: Estructura los objetos anidados requeridos por el frontend (ej: threat_type.name).
    public String obtenerFactor(int id) {
        JSONObject res = new JSONObject();
        try {
            FactorRiesgoDTO f = dao.obtenerPorId(id);
            if (f == null) {
                return res.put("success", false).put("message", "Factor de riesgo no encontrado.").toString();
            }

            JSONObject obj = new JSONObject();
            obj.put("id", f.getId());
            obj.put("description", f.getDescription());
            obj.put("ubication", f.getUbication());
            obj.put("distance", f.getDistance());
            obj.put("family_plan_id", f.getFamilyPlanId());
            obj.put("threat_type_id", f.getThreatTypeId());

            JSONObject threatTypeObj = new JSONObject();
            threatTypeObj.put("id", f.getThreatTypeId());
            threatTypeObj.put("name", f.getThreatTypeName() != null ? f.getThreatTypeName() : "");
            obj.put("threat_type", threatTypeObj);

            res.put("success", true);
            res.put("data", obj);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al obtener factor de riesgo: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Valida los campos obligatorios del factor de riesgo y los guarda en base de datos.
    // Por qué existe: Asegura la consistencia del registro del factor antes de persistirlo.
    // Qué problema resuelve: Previene ingresos nulos o inconsistentes en MySQL.
    public String crearFactor(FactorRiesgoDTO dto) {
        JSONObject res = new JSONObject();
        if (dto.getThreatTypeId() <= 0) {
            return res.put("success", false).put("message", "El tipo de amenaza es obligatorio.").toString();
        }
        if (dto.getDescription() == null || dto.getDescription().trim().isEmpty()) {
            return res.put("success", false).put("message", "La descripción del riesgo es obligatoria.").toString();
        }
        if (dto.getUbication() == null || dto.getUbication().trim().isEmpty()) {
            return res.put("success", false).put("message", "La ubicación del riesgo es obligatoria.").toString();
        }
        if (dto.getDistance() < 0) {
            return res.put("success", false).put("message", "La distancia debe ser un valor positivo.").toString();
        }

        try {
            boolean success = dao.crear(dto);
            if (success) {
                res.put("success", true);
                res.put("message", "Factor de riesgo registrado correctamente.");
            } else {
                res.put("success", false);
                res.put("message", "No se pudo registrar el factor de riesgo.");
            }
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al crear factor de riesgo: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Valida y actualiza los campos modificados del factor.
    // Por qué existe: Aplica las correcciones físicas ingresadas en la edición del riesgo.
    // Qué problema resuelve: Valida e impacta la base de datos de forma segura.
    public String actualizarFactor(int id, FactorRiesgoDTO dto) {
        JSONObject res = new JSONObject();
        if (dto.getThreatTypeId() <= 0) {
            return res.put("success", false).put("message", "El tipo de amenaza es obligatorio.").toString();
        }
        if (dto.getDescription() == null || dto.getDescription().trim().isEmpty()) {
            return res.put("success", false).put("message", "La descripción del riesgo es obligatoria.").toString();
        }
        if (dto.getUbication() == null || dto.getUbication().trim().isEmpty()) {
            return res.put("success", false).put("message", "La ubicación del riesgo es obligatoria.").toString();
        }
        if (dto.getDistance() < 0) {
            return res.put("success", false).put("message", "La distancia debe ser un valor positivo.").toString();
        }

        try {
            boolean success = dao.actualizar(id, dto);
            if (success) {
                res.put("success", true);
                res.put("message", "Factor de riesgo actualizado correctamente.");
            } else {
                res.put("success", false);
                res.put("message", "No se pudo actualizar el factor de riesgo.");
            }
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al actualizar factor de riesgo: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Elimina el factor de riesgo padre y sus relaciones.
    // Por qué existe: Habilita el borrado total de un factor de riesgo en cascada.
    // Qué problema resuelve: Evita la persistencia de datos huérfanos o errores de llave foránea.
    public String eliminarFactor(int id) {
        JSONObject res = new JSONObject();
        try {
            boolean success = dao.eliminar(id);
            if (success) {
                res.put("success", true);
                res.put("message", "Factor de riesgo eliminado correctamente.");
            } else {
                res.put("success", false);
                res.put("message", "No se pudo eliminar el factor de riesgo.");
            }
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al eliminar factor de riesgo: " + e.getMessage());
        }
        return res.toString();
    }

    // =========================================================================
    // SERVICIOS: ACCIONES DE REDUCCIÓN
    // =========================================================================

    public String listarAcciones(int riesgoId) {
        JSONObject res = new JSONObject();
        try {
            List<AccionReduccionDTO> list = dao.obtenerAccionesPorRiesgo(riesgoId);
            JSONArray arr = new JSONArray();
            for (AccionReduccionDTO a : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", a.getId());
                obj.put("action", a.getAction());
                obj.put("end_date", a.getEndDate());
                obj.put("risk_factor_id", a.getRiskFactorId());
                obj.put("member_id", a.getMemberId());
                
                JSONObject memberObj = new JSONObject();
                memberObj.put("names", a.getMemberName() != null ? a.getMemberName() : "");
                memberObj.put("last_names", a.getMemberLastNames() != null ? a.getMemberLastNames() : "");
                obj.put("member", memberObj);
                
                // Mapear created_at con fallback de fecha límite
                obj.put("created_at", a.getEndDate() != null ? a.getEndDate() + "T00:00:00Z" : "");
                arr.put(obj);
            }
            res.put("success", true);
            res.put("data", arr);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al listar acciones de reducción: " + e.getMessage());
        }
        return res.toString();
    }

    public String obtenerAccion(int id) {
        JSONObject res = new JSONObject();
        try {
            AccionReduccionDTO a = dao.obtenerAccionPorId(id);
            if (a == null) {
                return res.put("success", false).put("message", "Acción no encontrada.").toString();
            }

            JSONObject obj = new JSONObject();
            obj.put("id", a.getId());
            obj.put("action", a.getAction());
            obj.put("end_date", a.getEndDate());
            obj.put("risk_factor_id", a.getRiskFactorId());
            obj.put("member_id", a.getMemberId());

            JSONObject memberObj = new JSONObject();
            memberObj.put("names", a.getMemberName() != null ? a.getMemberName() : "");
            memberObj.put("last_names", a.getMemberLastNames() != null ? a.getMemberLastNames() : "");
            obj.put("member", memberObj);
            obj.put("created_at", a.getEndDate() != null ? a.getEndDate() + "T00:00:00Z" : "");

            res.put("success", true);
            res.put("data", obj);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al obtener la acción de reducción: " + e.getMessage());
        }
        return res.toString();
    }

    public String crearAccion(AccionReduccionDTO dto) {
        JSONObject res = new JSONObject();
        if (dto.getAction() == null || dto.getAction().trim().isEmpty()) {
            return res.put("success", false).put("message", "El nombre de la acción es obligatorio.").toString();
        }
        if (dto.getEndDate() == null || dto.getEndDate().trim().isEmpty()) {
            return res.put("success", false).put("message", "La fecha de finalización es obligatoria.").toString();
        }
        if (dto.getRiskFactorId() <= 0) {
            return res.put("success", false).put("message", "El ID del factor de riesgo es obligatorio.").toString();
        }

        try {
            boolean success = dao.crearAccion(dto);
            if (success) {
                res.put("success", true);
                res.put("message", "Acción agregada correctamente.");
            } else {
                res.put("success", false);
                res.put("message", "No se pudo registrar la acción.");
            }
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al crear la acción: " + e.getMessage());
        }
        return res.toString();
    }

    public String actualizarAccion(int id, AccionReduccionDTO dto) {
        JSONObject res = new JSONObject();
        if (dto.getAction() == null || dto.getAction().trim().isEmpty()) {
            return res.put("success", false).put("message", "El nombre de la acción es obligatorio.").toString();
        }
        if (dto.getEndDate() == null || dto.getEndDate().trim().isEmpty()) {
            return res.put("success", false).put("message", "La fecha de finalización es obligatoria.").toString();
        }

        try {
            boolean success = dao.actualizarAccion(id, dto);
            if (success) {
                res.put("success", true);
                res.put("message", "Acción actualizada correctamente.");
            } else {
                res.put("success", false);
                res.put("message", "No se pudo actualizar la acción.");
            }
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al actualizar la acción: " + e.getMessage());
        }
        return res.toString();
    }

    public String eliminarAccion(int id) {
        JSONObject res = new JSONObject();
        try {
            boolean success = dao.eliminarAccion(id);
            if (success) {
                res.put("success", true);
                res.put("message", "Acción de reducción eliminada correctamente.");
            } else {
                res.put("success", false);
                res.put("message", "No se pudo eliminar la acción.");
            }
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al eliminar la acción: " + e.getMessage());
        }
        return res.toString();
    }

    // =========================================================================
    // SERVICIOS: FACTORES DE VULNERABILIDAD
    // =========================================================================

    public String listarVulnerabilidades(int riesgoId) {
        JSONObject res = new JSONObject();
        try {
            List<FactorVulnerabilidadDTO> list = dao.obtenerVulnerabilidadesPorRiesgo(riesgoId);
            JSONArray arr = new JSONArray();
            for (FactorVulnerabilidadDTO v : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", v.getId());
                obj.put("vulnerability_id", v.getVulnerabilityId());
                obj.put("vulnerability_grade_id", v.getVulnerabilityGradeId());
                obj.put("risk_factor_id", v.getRiskFactorId());
                
                JSONObject vulObj = new JSONObject();
                vulObj.put("id", v.getVulnerabilityId());
                vulObj.put("name", v.getVulnerabilityName() != null ? v.getVulnerabilityName() : "");
                obj.put("vulnerability", vulObj);
                
                JSONObject gradeObj = new JSONObject();
                gradeObj.put("id", v.getVulnerabilityGradeId());
                gradeObj.put("name", v.getVulnerabilityGradeName() != null ? v.getVulnerabilityGradeName() : "");
                obj.put("vulnerability_grade", gradeObj);
                
                arr.put(obj);
            }
            res.put("success", true);
            res.put("data", arr);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al listar vulnerabilidades asociadas: " + e.getMessage());
        }
        return res.toString();
    }

    public String obtenerVulnerabilidad(int id) {
        JSONObject res = new JSONObject();
        try {
            FactorVulnerabilidadDTO v = dao.obtenerVulnerabilidadPorId(id);
            if (v == null) {
                return res.put("success", false).put("message", "Factor de vulnerabilidad no encontrado.").toString();
            }

            JSONObject obj = new JSONObject();
            obj.put("id", v.getId());
            obj.put("vulnerability_id", v.getVulnerabilityId());
            obj.put("vulnerability_grade_id", v.getVulnerabilityGradeId());
            obj.put("risk_factor_id", v.getRiskFactorId());

            JSONObject vulObj = new JSONObject();
            vulObj.put("id", v.getVulnerabilityId());
            vulObj.put("name", v.getVulnerabilityName() != null ? v.getVulnerabilityName() : "");
            obj.put("vulnerability", vulObj);

            JSONObject gradeObj = new JSONObject();
            gradeObj.put("id", v.getVulnerabilityGradeId());
            gradeObj.put("name", v.getVulnerabilityGradeName() != null ? v.getVulnerabilityGradeName() : "");
            obj.put("vulnerability_grade", gradeObj);

            res.put("success", true);
            res.put("data", obj);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al obtener la vulnerabilidad asociada: " + e.getMessage());
        }
        return res.toString();
    }

    public String crearVulnerabilidad(FactorVulnerabilidadDTO dto) {
        JSONObject res = new JSONObject();
        if (dto.getVulnerabilityId() <= 0) {
            return res.put("success", false).put("message", "La vulnerabilidad es obligatoria.").toString();
        }
        if (dto.getVulnerabilityGradeId() <= 0) {
            return res.put("success", false).put("message", "El grado de vulnerabilidad es obligatorio.").toString();
        }
        if (dto.getRiskFactorId() <= 0) {
            return res.put("success", false).put("message", "El ID del factor de riesgo es obligatorio.").toString();
        }

        try {
            boolean success = dao.crearVulnerabilidad(dto);
            if (success) {
                res.put("success", true);
                res.put("message", "Vulnerabilidad agregada correctamente.");
            } else {
                res.put("success", false);
                res.put("message", "No se pudo registrar la vulnerabilidad.");
            }
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al crear la vulnerabilidad asociada: " + e.getMessage());
        }
        return res.toString();
    }

    public String actualizarVulnerabilidad(int id, FactorVulnerabilidadDTO dto) {
        JSONObject res = new JSONObject();
        if (dto.getVulnerabilityId() <= 0) {
            return res.put("success", false).put("message", "La vulnerabilidad es obligatoria.").toString();
        }
        if (dto.getVulnerabilityGradeId() <= 0) {
            return res.put("success", false).put("message", "El grado de vulnerabilidad es obligatorio.").toString();
        }

        try {
            boolean success = dao.actualizarVulnerabilidad(id, dto);
            if (success) {
                res.put("success", true);
                res.put("message", "Vulnerabilidad actualizada correctamente.");
            } else {
                res.put("success", false);
                res.put("message", "No se pudo actualizar la vulnerabilidad.");
            }
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al actualizar la vulnerabilidad asociada: " + e.getMessage());
        }
        return res.toString();
    }

    public String eliminarVulnerabilidad(int id) {
        JSONObject res = new JSONObject();
        try {
            boolean success = dao.eliminarVulnerabilidad(id);
            if (success) {
                res.put("success", true);
                res.put("message", "Vulnerabilidad eliminada correctamente.");
            } else {
                res.put("success", false);
                res.put("message", "No se pudo eliminar la vulnerabilidad.");
            }
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al eliminar la vulnerabilidad: " + e.getMessage());
        }
        return res.toString();
    }
}
