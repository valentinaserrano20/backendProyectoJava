package Modelo.Servicios.Voluntario;

import Modelo.DAO.FactorRiesgoDAO;
import Modelo.DTO.FactorRiesgoDTO;
import Modelo.DTO.AccionReduccionDTO;
import Modelo.DTO.FactorVulnerabilidadDTO;
import Modelo.Utilidades.ResponseUtil;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Qué hace: Servicio que encapsula la lógica de negocio y serialización JSON para los factores de riesgo, sus acciones y vulnerabilidades.
 * Por qué existe: Actúa como intermediario entre los Servlets del controlador y el DAO de persistencia, formateando datos según lo espera la SPA.
 */
public class FactorRiesgoServicio {
    private final FactorRiesgoDAO dao = new FactorRiesgoDAO();

    // =========================================================================
    // SERVICIOS: FACTORES DE RIESGO
    // =========================================================================

    /**
     * Qué hace: Retorna todos los factores de riesgo (para el Supervisor) en JSON.
     */
    public String obtenerTodos() {
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
            return ResponseUtil.success(arr);
        } catch (Exception e) {
            return ResponseUtil.error("Error al recuperar todos los factores de riesgo: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Consulta los factores de riesgo de un plan de forma paginada.
     */
    public String listarFactores(int planId, int page) {
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

            return ResponseUtil.paginate(dataArr, paginate);
        } catch (Exception e) {
            return ResponseUtil.error("Error al listar factores de riesgo: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Retorna los factores de riesgo de un plan en formato simple de selección.
     */
    public String listarFactoresSelect(int planId) {
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
            return ResponseUtil.success(arr);
        } catch (Exception e) {
            return ResponseUtil.error("Error al listar factores para selección: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Recupera un único factor de riesgo por su ID mapeándolo en un formato que coincide con el modal ver.
     */
    public String obtenerFactor(int id) {
        try {
            FactorRiesgoDTO f = dao.obtenerPorId(id);
            if (f == null) {
                return ResponseUtil.error("Factor de riesgo no encontrado.");
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

            return ResponseUtil.success(obj);
        } catch (Exception e) {
            return ResponseUtil.error("Error al obtener factor de riesgo: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Valida los campos obligatorios del factor de riesgo y los guarda en base de datos.
     */
    public String crearFactor(FactorRiesgoDTO dto) {
        if (dto.getThreatTypeId() <= 0) {
            return ResponseUtil.error("El tipo de amenaza es obligatorio.");
        }
        if (dto.getDescription() == null || dto.getDescription().trim().isEmpty()) {
            return ResponseUtil.error("La descripción del riesgo es obligatoria.");
        }
        if (dto.getUbication() == null || dto.getUbication().trim().isEmpty()) {
            return ResponseUtil.error("La ubicación del riesgo es obligatoria.");
        }
        if (dto.getDistance() < 0) {
            return ResponseUtil.error("La distancia debe ser un valor positivo.");
        }

        try {
            boolean success = dao.crear(dto);
            if (success) {
                return ResponseUtil.success("Factor de riesgo registrado correctamente.");
            } else {
                return ResponseUtil.error("No se pudo registrar el factor de riesgo.");
            }
        } catch (Exception e) {
            return ResponseUtil.error("Error al crear factor de riesgo: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Valida y actualiza los campos modificados del factor.
     */
    public String actualizarFactor(int id, FactorRiesgoDTO dto) {
        if (dto.getThreatTypeId() <= 0) {
            return ResponseUtil.error("El tipo de amenaza es obligatorio.");
        }
        if (dto.getDescription() == null || dto.getDescription().trim().isEmpty()) {
            return ResponseUtil.error("La descripción del riesgo es obligatoria.");
        }
        if (dto.getUbication() == null || dto.getUbication().trim().isEmpty()) {
            return ResponseUtil.error("La ubicación del riesgo es obligatoria.");
        }
        if (dto.getDistance() < 0) {
            return ResponseUtil.error("La distancia debe ser un valor positivo.");
        }

        try {
            boolean success = dao.actualizar(id, dto);
            if (success) {
                return ResponseUtil.success("Factor de riesgo actualizado correctamente.");
            } else {
                return ResponseUtil.error("No se pudo actualizar el factor de riesgo.");
            }
        } catch (Exception e) {
            return ResponseUtil.error("Error al actualizar factor de riesgo: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Elimina el factor de riesgo padre y sus relaciones.
     */
    public String eliminarFactor(int id) {
        try {
            boolean success = dao.eliminar(id);
            if (success) {
                return ResponseUtil.success("Factor de riesgo eliminado correctamente.");
            } else {
                return ResponseUtil.error("No se pudo eliminar el factor de riesgo.");
            }
        } catch (Exception e) {
            return ResponseUtil.error("Error al eliminar factor de riesgo: " + e.getMessage());
        }
    }

    // =========================================================================
    // SERVICIOS: ACCIONES DE REDUCCIÓN
    // =========================================================================

    /**
     * Qué hace: Obtiene la lista de acciones de reducción asociadas a un factor de riesgo determinado.
     */
    public String listarAcciones(int riesgoId) {
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
                
                obj.put("created_at", a.getEndDate() != null ? a.getEndDate() + "T00:00:00Z" : "");
                arr.put(obj);
            }
            return ResponseUtil.success(arr);
        } catch (Exception e) {
            return ResponseUtil.error("Error al listar acciones de reducción: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Recupera los detalles de una única acción de reducción según su ID.
     */
    public String obtenerAccion(int id) {
        try {
            AccionReduccionDTO a = dao.obtenerAccionPorId(id);
            if (a == null) {
                return ResponseUtil.error("Acción no encontrada.");
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

            return ResponseUtil.success(obj);
        } catch (Exception e) {
            return ResponseUtil.error("Error al obtener la acción de reducción: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Valida los campos obligatorios del DTO de acción de reducción y delega su inserción en la BD.
     */
    public String crearAccion(AccionReduccionDTO dto) {
        if (dto.getAction() == null || dto.getAction().trim().isEmpty()) {
            return ResponseUtil.error("El nombre de la acción es obligatorio.");
        }
        if (dto.getEndDate() == null || dto.getEndDate().trim().isEmpty()) {
            return ResponseUtil.error("La fecha de finalización es obligatoria.");
        }
        if (dto.getRiskFactorId() <= 0) {
            return ResponseUtil.error("El ID del factor de riesgo es obligatorio.");
        }

        try {
            boolean success = dao.crearAccion(dto);
            if (success) {
                return ResponseUtil.success("Acción agregada correctamente.");
            } else {
                return ResponseUtil.error("No se pudo registrar la acción.");
            }
        } catch (Exception e) {
            return ResponseUtil.error("Error al crear la acción: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Valida las correcciones realizadas a una acción de reducción y aplica la actualización en la BD.
     */
    public String actualizarAccion(int id, AccionReduccionDTO dto) {
        if (dto.getAction() == null || dto.getAction().trim().isEmpty()) {
            return ResponseUtil.error("El nombre de la acción es obligatorio.");
        }
        if (dto.getEndDate() == null || dto.getEndDate().trim().isEmpty()) {
            return ResponseUtil.error("La fecha de finalización es obligatoria.");
        }

        try {
            boolean success = dao.actualizarAccion(id, dto);
            if (success) {
                return ResponseUtil.success("Acción actualizada correctamente.");
            } else {
                return ResponseUtil.error("No se pudo actualizar la acción.");
            }
        } catch (Exception e) {
            return ResponseUtil.error("Error al actualizar la acción: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Elimina una acción de reducción específica del plan.
     */
    public String eliminarAccion(int id) {
        try {
            boolean success = dao.eliminarAccion(id);
            if (success) {
                return ResponseUtil.success("Acción de reducción eliminada correctamente.");
            } else {
                return ResponseUtil.error("No se pudo eliminar la acción.");
            }
        } catch (Exception e) {
            return ResponseUtil.error("Error al eliminar la acción: " + e.getMessage());
        }
    }

    // =========================================================================
    // SERVICIOS: FACTORES DE VULNERABILIDAD
    // =========================================================================

    /**
     * Qué hace: Retorna la lista de factores de vulnerabilidad asociados a un factor de riesgo específico.
     */
    public String listarVulnerabilidades(int riesgoId) {
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
            return ResponseUtil.success(arr);
        } catch (Exception e) {
            return ResponseUtil.error("Error al listar vulnerabilidades asociadas: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Obtiene la información detallada de una vulnerabilidad asociada en base a su ID.
     */
    public String obtenerVulnerabilidad(int id) {
        try {
            FactorVulnerabilidadDTO v = dao.obtenerVulnerabilidadPorId(id);
            if (v == null) {
                return ResponseUtil.error("Factor de vulnerabilidad no encontrado.");
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

            return ResponseUtil.success(obj);
        } catch (Exception e) {
            return ResponseUtil.error("Error al obtener la vulnerabilidad asociada: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Valida y registra una nueva vulnerabilidad asociada a un factor de riesgo en la base de datos.
     */
    public String crearVulnerabilidad(FactorVulnerabilidadDTO dto) {
        if (dto.getVulnerabilityId() <= 0) {
            return ResponseUtil.error("La vulnerabilidad es obligatoria.");
        }
        if (dto.getVulnerabilityGradeId() <= 0) {
            return ResponseUtil.error("El grado de vulnerabilidad es obligatorio.");
        }
        if (dto.getRiskFactorId() <= 0) {
            return ResponseUtil.error("El ID del factor de riesgo es obligatorio.");
        }

        try {
            boolean success = dao.crearVulnerabilidad(dto);
            if (success) {
                return ResponseUtil.success("Vulnerabilidad agregada correctamente.");
            } else {
                return ResponseUtil.error("No se pudo registrar la vulnerabilidad.");
            }
        } catch (Exception e) {
            return ResponseUtil.error("Error al crear la vulnerabilidad asociada: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Valida y actualiza los parámetros de una vulnerabilidad asociada (tipo o grado) en la BD.
     */
    public String actualizarVulnerabilidad(int id, FactorVulnerabilidadDTO dto) {
        if (dto.getVulnerabilityId() <= 0) {
            return ResponseUtil.error("La vulnerabilidad es obligatoria.");
        }
        if (dto.getVulnerabilityGradeId() <= 0) {
            return ResponseUtil.error("El grado de vulnerabilidad es obligatorio.");
        }

        try {
            boolean success = dao.actualizarVulnerabilidad(id, dto);
            if (success) {
                return ResponseUtil.success("Vulnerabilidad actualizada correctamente.");
            } else {
                return ResponseUtil.error("No se pudo actualizar la vulnerabilidad.");
            }
        } catch (Exception e) {
            return ResponseUtil.error("Error al actualizar la vulnerabilidad asociada: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Elimina físicamente la asociación de vulnerabilidad del factor de riesgo.
     */
    public String eliminarVulnerabilidad(int id) {
        try {
            boolean success = dao.eliminarVulnerabilidad(id);
            if (success) {
                return ResponseUtil.success("Vulnerabilidad eliminada correctamente.");
            } else {
                return ResponseUtil.error("No se pudo eliminar la vulnerabilidad.");
            }
        } catch (Exception e) {
            return ResponseUtil.error("Error al eliminar la vulnerabilidad: " + e.getMessage());
        }
    }
}
