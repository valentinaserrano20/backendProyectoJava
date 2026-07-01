package Modelo.Servicios.Voluntario;

import Modelo.DAO.IntegranteDAO;
import Modelo.DTO.IntegranteDTO;
import Modelo.DTO.AfeccionDTO;
import Modelo.Utilidades.ResponseUtil;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Qué hace: Lógica de negocio y servicio de control para la gestión de integrantes de planes familiares y sus afecciones médicas.
 * Por qué existe: Actúa como capa intermedia (Service) que orquesta las llamadas al DAO, procesa validaciones de negocio y formatea las respuestas JSON.
 * Qué problema resuelve: Separa la lógica de procesamiento JSON, paginación y reglas de negocio de los servlets de presentación y del acceso directo a base de datos.
 * Se implementó ResponseUtil para homogeneizar las respuestas hacia el frontend y reducir el boilerplate de bloques try-catch.
 */
public class IntegranteServicio {
    private final IntegranteDAO dao = new IntegranteDAO();

    /**
     * Qué hace: Consulta un listado paginado de integrantes familiares y formatea la respuesta en una estructura JSON con paginación clásica/infinita.
     * Por qué se hizo: Es invocado por los controladores cuando el frontend solicita listar a los familiares del plan de emergencia de forma dosificada.
     * Qué significa: Retorna un JSON estructurado con la lista de integrantes y los metadatos de paginación o el error correspondiente.
     */
    public String listarIntegrantes(int planId, int page) {
        try {
            int limit = 10;
            int offset = (page - 1) * limit;
            
            int total = dao.obtenerTotalIntegrantes(planId);
            List<IntegranteDTO> list = dao.listarIntegrantes(planId, limit, offset);
            
            JSONArray dataArr = new JSONArray();
            for (IntegranteDTO m : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", m.getId());
                obj.put("full_name", m.getNames() + " " + m.getLastNames());
                obj.put("blood_group", m.getBloodGroupName());
                obj.put("document_number", m.getDocumentNumber());
                obj.put("phone", m.getPhone());
                obj.put("kinship", m.getKinshipName());
                obj.put("birth_date", m.getBirthDate());
                obj.put("status_id", m.getStatusId());
                dataArr.put(obj);
            }
            
            JSONObject paginate = new JSONObject();
            paginate.put("total", total);
            paginate.put("per_page", limit);
            paginate.put("current_page", page);
            paginate.put("last_page", (int) Math.ceil((double) total / limit));
            
            return ResponseUtil.paginate(dataArr, paginate);
        } catch (Exception e) {
            return ResponseUtil.error("Error al listar integrantes: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Obtiene la información detallada de un integrante por su identificador único y la empaqueta en un objeto JSON con sus relaciones completas.
     * Por qué se hizo: Permite alimentar la ventana de detalle o el formulario de edición en la interfaz de usuario.
     * Qué significa: Retorna el DTO de integrante estructurado en un JSON de éxito o el error correspondiente.
     */
    public String obtenerIntegrante(int id) {
        try {
            IntegranteDTO m = dao.obtenerIntegrante(id);
            if (m == null) {
                return ResponseUtil.error("Integrante no encontrado.");
            }
            
            JSONObject obj = new JSONObject();
            obj.put("id", m.getId());
            obj.put("names", m.getNames());
            obj.put("last_names", m.getLastNames());
            obj.put("document_number", m.getDocumentNumber() != null ? m.getDocumentNumber() : "");
            obj.put("birth_date", m.getBirthDate());
            obj.put("eps", m.getEps() != null ? m.getEps() : "");
            obj.put("phone", m.getPhone() != null ? m.getPhone() : "");
            obj.put("es_jefe_hogar", m.isEsJefeHogar());
            obj.put("plan_id", m.getPlanId());
            obj.put("document_type_id", m.getDocumentTypeId());
            obj.put("kinship_id", m.getKinshipId());
            obj.put("blood_group_id", m.getBloodGroupId());
            obj.put("nationality_id", m.getNationalityId());
            obj.put("gender_id", m.getGenderId());
            
            // Nested objects for view modal
            obj.put("document_type", new JSONObject().put("acronym", m.getDocumentTypeAcronym()));
            obj.put("gender", new JSONObject().put("name", m.getGenderName()));
            obj.put("kinship", new JSONObject().put("name", m.getKinshipName()));
            obj.put("blood_group", new JSONObject().put("name", m.getBloodGroupName()));
            obj.put("nationality", new JSONObject().put("name", m.getNationalityName()));
            
            return ResponseUtil.success(obj);
        } catch (Exception e) {
            return ResponseUtil.error("Error al obtener integrante: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Valida los campos obligatorios del integrante y delega su persistencia al DAO.
     * Por qué se hizo: Previene que registros incompletos o con formatos incorrectos sean guardados en el censo.
     * Qué significa: Valida y registra al familiar, retornando el ID recién creado en caso de éxito.
     */
    public String crearIntegrante(IntegranteDTO dto) {
        if (dto.getNames() == null || dto.getNames().trim().isEmpty()) {
            return ResponseUtil.error("El nombre es obligatorio.");
        }
        if (dto.getLastNames() == null || dto.getLastNames().trim().isEmpty()) {
            return ResponseUtil.error("El apellido es obligatorio.");
        }
        if (dto.getBirthDate() == null || dto.getBirthDate().trim().isEmpty()) {
            return ResponseUtil.error("La fecha de nacimiento es obligatoria.");
        }
        
        try {
            java.time.LocalDate birth = java.time.LocalDate.parse(dto.getBirthDate());
            java.time.LocalDate now = java.time.LocalDate.now();
            if (birth.isAfter(now)) {
                return ResponseUtil.error("La fecha de nacimiento no puede ser una fecha futura.");
            }
        } catch (Exception e) {
            return ResponseUtil.error("El formato de la fecha de nacimiento no es válido.");
        }

        try {
            int total = dao.obtenerTotalIntegrantes(dto.getPlanId());
            
            if (total == 0 && dto.getKinshipId() != 1) {
                return ResponseUtil.error("El primer integrante registrado en el plan familiar debe ser el Jefe de hogar.");
            }

            if (dto.getKinshipId() == 1) {
                if (dao.tieneJefeHogar(dto.getPlanId(), 0)) {
                    return ResponseUtil.error("El plan familiar ya cuenta con un Jefe de hogar.");
                }
                
                java.time.LocalDate birth = java.time.LocalDate.parse(dto.getBirthDate());
                java.time.LocalDate now = java.time.LocalDate.now();
                int age = java.time.Period.between(birth, now).getYears();
                if (age < 18) {
                    return ResponseUtil.error("El Jefe de hogar debe ser mayor de edad (mínimo 18 años).");
                }
            }
        } catch (Exception e) {
            return ResponseUtil.error("Error al procesar reglas de negocio del integrante: " + e.getMessage());
        }
        
        try {
            int newId = dao.crearIntegrante(dto);
            JSONObject data = new JSONObject();
            data.put("id", newId);
            return ResponseUtil.success("Integrante agregado correctamente.", data);
        } catch (Exception e) {
            return ResponseUtil.error("Error al crear integrante: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Realiza validaciones previas a la modificación del integrante y delega el UPDATE en el DAO.
     * Por qué se hizo: Permite modificar de forma segura los datos demográficos y de salud del miembro del hogar.
     * Qué significa: Valida, actualiza el registro demográfico y retorna JSON con el resultado.
     */
    public String actualizarIntegrante(int id, IntegranteDTO dto) {
        if (dto.getNames() == null || dto.getNames().trim().isEmpty()) {
            return ResponseUtil.error("El nombre es obligatorio.");
        }
        if (dto.getLastNames() == null || dto.getLastNames().trim().isEmpty()) {
            return ResponseUtil.error("El apellido es obligatorio.");
        }
        if (dto.getBirthDate() == null || dto.getBirthDate().trim().isEmpty()) {
            return ResponseUtil.error("La fecha de nacimiento es obligatoria.");
        }
        try {
            java.time.LocalDate birth = java.time.LocalDate.parse(dto.getBirthDate());
            java.time.LocalDate now = java.time.LocalDate.now();
            if (birth.isAfter(now)) {
                return ResponseUtil.error("La fecha de nacimiento no puede ser una fecha futura.");
            }
        } catch (Exception e) {
            return ResponseUtil.error("El formato de la fecha de nacimiento no es válido.");
        }

        try {
            IntegranteDTO existente = dao.obtenerIntegrante(id);
            if (existente == null) {
                return ResponseUtil.error("Integrante no encontrado para actualizar.");
            }
            
            if (dto.getKinshipId() == 1) {
                if (dao.tieneJefeHogar(existente.getPlanId(), id)) {
                    return ResponseUtil.error("El plan familiar ya cuenta con un Jefe de hogar.");
                }
                
                java.time.LocalDate birth = java.time.LocalDate.parse(dto.getBirthDate());
                java.time.LocalDate now = java.time.LocalDate.now();
                int age = java.time.Period.between(birth, now).getYears();
                if (age < 18) {
                    return ResponseUtil.error("El Jefe de hogar debe ser mayor de edad (mínimo 18 años).");
                }
            } else {
                if (existente.getKinshipId() == 1) {
                    int total = dao.obtenerTotalIntegrantes(existente.getPlanId());
                    if (total > 1) {
                        return ResponseUtil.error("No se puede cambiar el parentesco del Jefe de hogar actual si no se asigna previamente otra cabeza de familia.");
                    }
                }
            }
        } catch (Exception e) {
            return ResponseUtil.error("Error al procesar reglas de negocio en actualización: " + e.getMessage());
        }
        
        try {
            dao.actualizarIntegrante(id, dto);
            return ResponseUtil.success("Integrante actualizado correctamente.");
        } catch (Exception e) {
            return ResponseUtil.error("Error al actualizar integrante: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Invoca el proceso transaccional de borrado físico del integrante y sus padecimientos satélite.
     * Por qué se hizo: Permite dar de baja familiares inactivos o cargados por error del voluntario.
     * Qué significa: Remueve al integrante y sus dependencias de la BD, retornando confirmación JSON.
     */
    public String eliminarIntegrante(int id) {
        try {
            dao.eliminarIntegrante(id);
            return ResponseUtil.success("Integrante eliminado correctamente.");
        } catch (Exception e) {
            return ResponseUtil.error("Error al eliminar integrante: " + e.getMessage());
        }
    }

    // =========================================================================
    // SUB-MÓDULO DE AFECCIONES
    // =========================================================================

    /**
     * Qué hace: Recupera todas las afecciones médicas registradas para un integrante específico del plan familiar.
     * Por qué se hizo: Permite listar los padecimientos de un miembro (enfermedad/alergia/discapacidad) en su pantalla de gestión.
     * Qué significa: Retorna una respuesta JSON exitosa con la colección de afecciones mapeada.
     */
    public String listarAfecciones(int memberId) {
        try {
            List<AfeccionDTO> list = dao.listarAfeccionesPorIntegrante(memberId);
            JSONArray arr = new JSONArray();
            for (AfeccionDTO a : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", a.getId());
                obj.put("member_id", a.getMemberId());
                obj.put("condition_type_id", a.getConditionTypeId());
                obj.put("name", a.getName());
                obj.put("dose", a.getDose() != null ? a.getDose() : "");
                
                JSONObject cType = new JSONObject();
                cType.put("id", a.getConditionTypeId());
                cType.put("name", a.getConditionTypeName());
                obj.put("condition_type", cType);
                
                arr.put(obj);
            }
            return ResponseUtil.success(arr);
        } catch (Exception e) {
            return ResponseUtil.error("Error al listar afecciones: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Obtiene la información estructurada de un diagnóstico médico singular por su identificador.
     * Por qué se hizo: Permite rellenar los datos de dosis y nombre de la afección en el modal de SweetAlert de edición.
     * Qué significa: Retorna el DTO de la afección enlazado en un JSON de éxito.
     */
    public String obtenerAfeccion(int id) {
        try {
            AfeccionDTO a = dao.obtenerAfeccion(id);
            if (a == null) {
                return ResponseUtil.error("Afección no encontrada.");
            }
            
            JSONObject obj = new JSONObject();
            obj.put("id", a.getId());
            obj.put("member_id", a.getMemberId());
            obj.put("condition_type_id", a.getConditionTypeId());
            obj.put("name", a.getName());
            obj.put("dose", a.getDose() != null ? a.getDose() : "");
            
            JSONObject cType = new JSONObject();
            cType.put("id", a.getConditionTypeId());
            cType.put("name", a.getConditionTypeName());
            obj.put("condition_type", cType);
            
            return ResponseUtil.success(obj);
        } catch (Exception e) {
            return ResponseUtil.error("Error al obtener afección: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Valida los campos requeridos para un diagnóstico de salud y delega el registro al DAO.
     * Por qué se hizo: Asegura que cada afección declarada posea un nombre y un tipo válido del catálogo médico.
     * Qué significa: Registra la afección y retorna el ID generado en un JSON de éxito.
     */
    public String crearAfeccion(AfeccionDTO dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            return ResponseUtil.error("El nombre de la afección es obligatorio.");
        }
        if (dto.getConditionTypeId() <= 0) {
            return ResponseUtil.error("El tipo de afección es obligatorio.");
        }
        
        try {
            int newId = dao.crearAfeccion(dto);
            JSONObject data = new JSONObject();
            data.put("id", newId);
            return ResponseUtil.success("Afección agregada correctamente.", data);
        } catch (Exception e) {
            return ResponseUtil.error("Error al crear afección: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Valida y actualiza los parámetros médicos del diagnóstico de un integrante, administrando sus dosis.
     * Por qué se hizo: Permite modificar el tratamiento, tipo o denominación de la condición de salud.
     * Qué significa: Actualiza el registro en la BD y retorna confirmación.
     */
    public String actualizarAfeccion(int id, AfeccionDTO dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            return ResponseUtil.error("El nombre de la afección es obligatorio.");
        }
        if (dto.getConditionTypeId() <= 0) {
            return ResponseUtil.error("El tipo de afección es obligatorio.");
        }
        
        try {
            dao.actualizarAfeccion(id, dto);
            return ResponseUtil.success("Afección actualizada correctamente.");
        } catch (Exception e) {
            return ResponseUtil.error("Error al actualizar afección: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Ejecuta la baja de un diagnóstico médico y su medicamento satélite por su identificador.
     * Por qué se hizo: Permite quitar afecciones cargadas erróneamente o que han sido superadas por el integrante.
     * Qué significa: Elimina los registros y retorna el resultado de la operación.
     */
    public String eliminarAfeccion(int id) {
        try {
            dao.eliminarAfeccion(id);
            return ResponseUtil.success("Afección eliminada correctamente.");
        } catch (Exception e) {
            return ResponseUtil.error("Error al eliminar afección: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Obtiene la lista completa de integrantes de un plan y la formatea en un JSONArray simplificado para los selectores HTML.
     * Por qué se hizo: Es invocado por el servlet de integrantes para rellenar listados desplegables del frontend (como en el Plan de Acción).
     * Qué significa: Retorna una respuesta JSON con el listado simplificado para selects.
     */
    public String obtenerIntegrantesSeleccion(int planId) {
        try {
            List<IntegranteDTO> list = dao.listarTodosIntegrantes(planId);
            JSONArray dataArr = new JSONArray();
            for (IntegranteDTO m : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", m.getId());
                obj.put("full_name", m.getNames() + " " + m.getLastNames());
                obj.put("document_number", m.getDocumentNumber());
                obj.put("kinship", m.getKinshipName());
                dataArr.put(obj);
            }
            return ResponseUtil.success(dataArr);
        } catch (Exception e) {
            return ResponseUtil.error("Error al obtener selección de integrantes: " + e.getMessage());
        }
    }
}
