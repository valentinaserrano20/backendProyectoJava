package Modelo.Servicios.Voluntario;

import Modelo.DAO.IntegranteDAO;
import Modelo.DTO.IntegranteDTO;
import Modelo.DTO.AfeccionDTO;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

// Qué hace: Lógica de negocio y servicio de control para la gestión de integrantes de planes familiares y sus afecciones médicas.
// Por qué existe: Actúa como capa intermedia (Service) que orquesta las llamadas al DAO, procesa validaciones de negocio y formatea las respuestas JSON para el consumo del frontend.
// Qué problema resuelve: Separa la lógica de procesamiento JSON, paginación y reglas de negocio de los servlets de presentación y del acceso directo a base de datos.
public class IntegranteServicio {
    private final IntegranteDAO dao = new IntegranteDAO();

    // Qué hace: Consulta un listado paginado de integrantes familiares y formatea la respuesta en una estructura JSON con paginación clásica/infinita.
    // Por qué existe: Es invocado por los controladores cuando el frontend solicita listar a los familiares del plan de emergencia de forma dosificada.
    // Qué problema resuelve: Encapsula el cálculo de límites y offsets de la base de datos, así como la construcción de metadatos de paginación para evitar inconsistencias en el renderizado de la UI.
    public String listarIntegrantes(int planId, int page) {
        JSONObject res = new JSONObject();
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
            
            res.put("success", true);
            res.put("data", dataArr);
            res.put("paginate", paginate);
            
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al listar integrantes: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Obtiene la información detallada de un integrante por su identificador único y la empaqueta en un objeto JSON con sus relaciones completas.
    // Por qué existe: Permite alimentar la ventana de detalle o el formulario de edición en la interfaz de usuario.
    // Qué problema resuelve: Transforma el modelo relacional complejo del DTO hacia una estructura anidada JSON limpia que el frontend entiende directamente.
    public String obtenerIntegrante(int id) {
        JSONObject res = new JSONObject();
        try {
            IntegranteDTO m = dao.obtenerIntegrante(id);
            if (m == null) {
                return res.put("success", false).put("message", "Integrante no encontrado.").toString();
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
            
            res.put("success", true);
            res.put("data", obj);
            
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al obtener integrante: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Valida los campos obligatorios del integrante y delega su persistencia al DAO.
    // Por qué existe: Previene que registros incompletos o con formatos incorrectos sean guardados en el sistema.
    // Qué problema resuelve: Asegura el cumplimiento de reglas de negocio en el backend antes de realizar la inserción física en MySQL.
    public String crearIntegrante(IntegranteDTO dto) {
        JSONObject res = new JSONObject();
        
        // Validaciones de negocio
        if (dto.getNames() == null || dto.getNames().trim().isEmpty()) {
            return res.put("success", false).put("message", "El nombre es obligatorio.").toString();
        }
        if (dto.getLastNames() == null || dto.getLastNames().trim().isEmpty()) {
            return res.put("success", false).put("message", "El apellido es obligatorio.").toString();
        }
        if (dto.getBirthDate() == null || dto.getBirthDate().trim().isEmpty()) {
            return res.put("success", false).put("message", "La fecha de nacimiento es obligatoria.").toString();
        }
        // Qué hace: Valida que la fecha de nacimiento no sea futura.
        // Por qué existe: Evita el registro de fechas absurdas posteriores a la fecha actual del sistema.
        // Qué problema resuelve: Impide inconsistencias temporales en el registro de integrantes.
        try {
            java.time.LocalDate birth = java.time.LocalDate.parse(dto.getBirthDate());
            java.time.LocalDate now = java.time.LocalDate.now();
            if (birth.isAfter(now)) {
                return res.put("success", false).put("message", "La fecha de nacimiento no puede ser una fecha futura.").toString();
            }
        } catch (Exception e) {
            return res.put("success", false).put("message", "El formato de la fecha de nacimiento no es válido.").toString();
        }

        try {
            int total = dao.obtenerTotalIntegrantes(dto.getPlanId());
            
            // Qué hace: Si es el primer integrante del plan, exige que sea el Jefe de hogar.
            // Por qué existe: Asegura que el núcleo familiar empiece obligatoriamente con una cabeza de hogar registrada.
            // Qué problema resuelve: Evita registrar hijos, cónyuges u otros parentescos en planes vacíos sin jefe.
            if (total == 0 && dto.getKinshipId() != 1) {
                return res.put("success", false).put("message", "El primer integrante registrado en el plan familiar debe ser el Jefe de hogar.").toString();
            }

            // Qué hace: Si el parentesco es "Jefe de hogar" (id = 1), valida que no exista otro ya registrado y que sea mayor de edad.
            // Por qué existe: Garantiza la regla de negocio de un único jefe mayor de edad por familia.
            // Qué problema resuelve: Impide menores de edad de jefes de hogar y la duplicación de cabezas de familia.
            if (dto.getKinshipId() == 1) {
                if (dao.tieneJefeHogar(dto.getPlanId(), 0)) {
                    return res.put("success", false).put("message", "El plan familiar ya cuenta con un Jefe de hogar.").toString();
                }
                
                java.time.LocalDate birth = java.time.LocalDate.parse(dto.getBirthDate());
                java.time.LocalDate now = java.time.LocalDate.now();
                int age = java.time.Period.between(birth, now).getYears();
                if (age < 18) {
                    return res.put("success", false).put("message", "El Jefe de hogar debe ser mayor de edad (mínimo 18 años).").toString();
                }
            }
        } catch (Exception e) {
            return res.put("success", false).put("message", "Error al procesar reglas de negocio del integrante: " + e.getMessage()).toString();
        }
        
        try {
            int newId = dao.crearIntegrante(dto);
            JSONObject data = new JSONObject();
            data.put("id", newId);
            
            res.put("success", true);
            res.put("message", "Integrante agregado correctamente.");
            res.put("data", data);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al crear integrante: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Realiza validaciones previas a la modificación del integrante y delega el UPDATE en el DAO.
    // Por qué existe: Permite modificar de forma segura los datos demográficos y de salud del miembro del hogar.
    // Qué problema resuelve: Centraliza la lógica de actualización en un solo punto, controlando la coherencia de datos demográficos obligatorios.
    public String actualizarIntegrante(int id, IntegranteDTO dto) {
        JSONObject res = new JSONObject();
        
        // Validaciones de negocio
        if (dto.getNames() == null || dto.getNames().trim().isEmpty()) {
            return res.put("success", false).put("message", "El nombre es obligatorio.").toString();
        }
        if (dto.getLastNames() == null || dto.getLastNames().trim().isEmpty()) {
            return res.put("success", false).put("message", "El apellido es obligatorio.").toString();
        }
        if (dto.getBirthDate() == null || dto.getBirthDate().trim().isEmpty()) {
            return res.put("success", false).put("message", "La fecha de nacimiento es obligatoria.").toString();
        }
        // Qué hace: Valida que la fecha de nacimiento no sea futura.
        // Por qué existe: Evita el registro de fechas absurdas posteriores a la fecha actual del sistema.
        // Qué problema resuelve: Impide inconsistencias temporales en el registro de integrantes.
        try {
            java.time.LocalDate birth = java.time.LocalDate.parse(dto.getBirthDate());
            java.time.LocalDate now = java.time.LocalDate.now();
            if (birth.isAfter(now)) {
                return res.put("success", false).put("message", "La fecha de nacimiento no puede ser una fecha futura.").toString();
            }
        } catch (Exception e) {
            return res.put("success", false).put("message", "El formato de la fecha de nacimiento no es válido.").toString();
        }

        try {
            IntegranteDTO existente = dao.obtenerIntegrante(id);
            if (existente == null) {
                return res.put("success", false).put("message", "Integrante no encontrado para actualizar.").toString();
            }
            
            // Qué hace: Si el parentesco modificado pasa a ser "Jefe de hogar" (id = 1), valida exclusividad y mayoría de edad.
            // Por qué existe: Mantiene la consistencia de las reglas familiares en operaciones de edición.
            // Qué problema resuelve: Evita promover a jefe de hogar a un integrante menor de edad o duplicar el rol.
            if (dto.getKinshipId() == 1) {
                if (dao.tieneJefeHogar(existente.getPlanId(), id)) {
                    return res.put("success", false).put("message", "El plan familiar ya cuenta con un Jefe de hogar.").toString();
                }
                
                java.time.LocalDate birth = java.time.LocalDate.parse(dto.getBirthDate());
                java.time.LocalDate now = java.time.LocalDate.now();
                int age = java.time.Period.between(birth, now).getYears();
                if (age < 18) {
                    return res.put("success", false).put("message", "El Jefe de hogar debe ser mayor de edad (mínimo 18 años).").toString();
                }
            } else {
                // Si el integrante era jefe de hogar y ahora se le está cambiando a otro parentesco,
                // debemos alertar que no se puede dejar el plan sin jefe de hogar si hay integrantes registrados.
                if (existente.getKinshipId() == 1) {
                    // Verificamos si hay otros integrantes en la familia
                    int total = dao.obtenerTotalIntegrantes(existente.getPlanId());
                    if (total > 1) {
                        // Si hay más familiares, no podemos cambiar el jefe de hogar a otro parentesco
                        // a menos que primero definamos a otra persona como jefe. Para simplificar y no bloquear,
                        // exigimos que siempre deba haber un jefe de hogar.
                        return res.put("success", false).put("message", "No se puede cambiar el parentesco del Jefe de hogar actual si no se asigna previamente otra cabeza de familia.").toString();
                    }
                }
            }
        } catch (Exception e) {
            return res.put("success", false).put("message", "Error al procesar reglas de negocio en actualización: " + e.getMessage()).toString();
        }
        
        try {
            dao.actualizarIntegrante(id, dto);
            res.put("success", true);
            res.put("message", "Integrante actualizado correctamente.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al actualizar integrante: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Invoca el proceso transaccional de borrado físico del integrante y sus padecimientos satélite.
    // Por qué existe: Permite dar de baja familiares inactivos o cargados por error del voluntario.
    // Qué problema resuelve: Asegura que el borrado cascada ocurra de manera segura y controlada mediante rollbacks ante cualquier falla en base de datos.
    public String eliminarIntegrante(int id) {
        JSONObject res = new JSONObject();
        try {
            dao.eliminarIntegrante(id);
            res.put("success", true);
            res.put("message", "Integrante eliminado correctamente.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al eliminar integrante: " + e.getMessage());
        }
        return res.toString();
    }

    // =========================================================================
    // SUB-MÓDULO DE AFECCIONES
    // =========================================================================

    // Qué hace: Recupera todas las afecciones médicas registradas para un integrante específico del plan familiar.
    // Por qué existe: Permite listar los padecimientos de un miembro (enfermedad/alergia/discapacidad) en su pantalla de gestión.
    // Qué problema resuelve: Mapea la lista física a un arreglo JSON limpio e inyecta descripciones textuales para mejorar el entendimiento de la UI.
    public String listarAfecciones(int memberId) {
        JSONObject res = new JSONObject();
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
            res.put("success", true);
            res.put("data", arr);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al listar afecciones: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Obtiene la información estructurada de un diagnóstico médico singular por su identificador.
    // Por qué existe: Permite rellenar los datos de dosis y nombre de la afección en el modal de SweetAlert de edición.
    // Qué problema resuelve: Mapea el registro individual de base de datos a un formato JSON adecuado para el precargado del formulario.
    public String obtenerAfeccion(int id) {
        JSONObject res = new JSONObject();
        try {
            AfeccionDTO a = dao.obtenerAfeccion(id);
            if (a == null) {
                return res.put("success", false).put("message", "Afección no encontrada.").toString();
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
            
            res.put("success", true);
            res.put("data", obj);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al obtener afección: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Valida los campos requeridos para un diagnóstico de salud y delega el registro al DAO.
    // Por qué existe: Asegura que cada afección declarada posea un nombre y un tipo válido del catálogo médico.
    // Qué problema resuelve: Controla los campos nulos y vacíos antes de gatillar la transacción compuesta en la base de datos.
    public String crearAfeccion(AfeccionDTO dto) {
        JSONObject res = new JSONObject();
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            return res.put("success", false).put("message", "El nombre de la afección es obligatorio.").toString();
        }
        if (dto.getConditionTypeId() <= 0) {
            return res.put("success", false).put("message", "El tipo de afección es obligatorio.").toString();
        }
        
        try {
            int newId = dao.crearAfeccion(dto);
            JSONObject data = new JSONObject();
            data.put("id", newId);
            
            res.put("success", true);
            res.put("message", "Afección agregada correctamente.");
            res.put("data", data);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al crear afección: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Valida y actualiza los parámetros médicos del diagnóstico de un integrante, administrando sus dosis.
    // Por qué existe: Permite modificar el tratamiento, tipo o denominación de la condición de salud.
    // Qué problema resuelve: Gestiona en cascada si debe insertar, actualizar o eliminar la dosis de medicamento asociada al diagnóstico modificado.
    public String actualizarAfeccion(int id, AfeccionDTO dto) {
        JSONObject res = new JSONObject();
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            return res.put("success", false).put("message", "El nombre de la afección es obligatorio.").toString();
        }
        if (dto.getConditionTypeId() <= 0) {
            return res.put("success", false).put("message", "El tipo de afección es obligatorio.").toString();
        }
        
        try {
            dao.actualizarAfeccion(id, dto);
            res.put("success", true);
            res.put("message", "Afección actualizada correctamente.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al actualizar afección: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Ejecuta la baja de un diagnóstico médico y su medicamento satélite por su identificador.
    // Por qué existe: Permite quitar afecciones cargadas erróneamente o que han sido superadas por el integrante.
    // Qué problema resuelve: Elimina la afección previniendo fallas de integridad relacional en la tabla de medicamentos.
    public String eliminarAfeccion(int id) {
        JSONObject res = new JSONObject();
        try {
            dao.eliminarAfeccion(id);
            res.put("success", true);
            res.put("message", "Afección eliminada correctamente.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al eliminar afección: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Obtiene la lista completa de integrantes de un plan y la formatea en un JSONArray simplificado para los selectores HTML.
    // Por qué existe: Es invocado por el servlet de integrantes para rellenar listados desplegables del frontend (como en el Plan de Acción).
    // Qué problema resuelve: Estructura una respuesta JSON compacta con nombres completos y datos mínimos requeridos.
    public String obtenerIntegrantesSeleccion(int planId) {
        JSONObject res = new JSONObject();
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
            res.put("success", true);
            res.put("data", dataArr);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al obtener selección de integrantes: " + e.getMessage());
        }
        return res.toString();
    }
}

