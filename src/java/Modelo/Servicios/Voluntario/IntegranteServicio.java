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
    // Qué hace: Instancia el objeto de acceso a datos para integrantes.
    // Por qué existe: Habilita la ejecución de operaciones de persistencia demográficas y de salud en la base de datos MySQL.
    // Qué pasaría si no estuviera: El servicio no podría conectarse con la base de datos para almacenar a los familiares evaluados.
    // Flujo: De aquí pasamos a IntegranteDAO.
    private final IntegranteDAO dao = new IntegranteDAO();

    // Qué hace: Consulta un listado paginado de integrantes familiares y formatea la respuesta en una estructura JSON con paginación clásica/infinita.
    // Por qué existe: Es invocado por los controladores cuando el frontend solicita listar a los familiares del plan de emergencia de forma dosificada.
    // Qué pasaría si no estuviera: El frontend no tendría cómo mostrar la grilla de personas que habitan en la vivienda de forma estructurada o paginada.
    public String listarIntegrantes(int planId, int page) {
        JSONObject res = new JSONObject();
        try {
            int limit = 10;
            int offset = (page - 1) * limit;
            
            // Qué hace: Obtiene el conteo total de integrantes registrados en el plan.
            // y luego de esto pasamos a IntegranteDAO.obtenerTotalIntegrantes, que ejecuta un SELECT COUNT.
            int total = dao.obtenerTotalIntegrantes(planId);
            
            // Qué hace: Obtiene la lista parcial de integrantes para la página seleccionada.
            // y luego de esto pasamos a IntegranteDAO.listarIntegrantes, que realiza el query correspondiente.
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
    // Qué pasaría si no estuviera: No se podrían ver los detalles de contacto, EPS, tipo de sangre o parentesco al intentar editar o consultar la ficha de un familiar.
    public String obtenerIntegrante(int id) {
        JSONObject res = new JSONObject();
        try {
            // Qué hace: Obtiene el DTO completo del integrante filtrado por su ID.
            // y luego de esto pasamos a IntegranteDAO.obtenerIntegrante, que corre un SELECT relacional con JOINs.
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
    // Qué pasaría si no estuviera: Se registrarían integrantes sin nombres, apellidos, con fechas futuras de nacimiento o familias sin una cabeza de hogar asignada.
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
            // Qué hace: Obtiene la cantidad actual de integrantes del plan familiar.
            // y luego de esto pasamos a IntegranteDAO.obtenerTotalIntegrantes.
            int total = dao.obtenerTotalIntegrantes(dto.getPlanId());
            
            // Qué hace: Si es el primer integrante del plan, exige que sea el Jefe de hogar.
            if (total == 0 && dto.getKinshipId() != 1) {
                return res.put("success", false).put("message", "El primer integrante registrado en el plan familiar debe ser el Jefe de hogar.").toString();
            }

            // Qué hace: Si el parentesco es "Jefe de hogar" (id = 1), valida que no exista otro ya registrado y que sea mayor de edad.
            if (dto.getKinshipId() == 1) {
                // y luego de esto pasamos a IntegranteDAO.tieneJefeHogar para verificar exclusividad en MySQL.
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
            // Qué hace: Registra el nuevo familiar.
            // y luego de esto pasamos a IntegranteDAO.crearIntegrante, que corre el comando INSERT.
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
    // Qué pasaría si no estuviera: Se guardarían nombres vacíos o fechas inválidas, y se podría cambiar el parentesco del único jefe de hogar dejando a la familia descabezada.
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
            // Qué hace: Obtiene el integrante existente por ID.
            // y luego de esto pasamos a IntegranteDAO.obtenerIntegrante.
            IntegranteDTO existente = dao.obtenerIntegrante(id);
            if (existente == null) {
                return res.put("success", false).put("message", "Integrante no encontrado para actualizar.").toString();
            }
            
            // Qué hace: Si el parentesco modificado pasa a ser "Jefe de hogar" (id = 1), valida exclusividad y mayoría de edad.
            if (dto.getKinshipId() == 1) {
                // y luego de esto pasamos a IntegranteDAO.tieneJefeHogar.
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
                // Qué hace: Si el integrante era jefe de hogar y se intenta cambiar, verifica que no se deje sin jefe a un plan con múltiples miembros.
                if (existente.getKinshipId() == 1) {
                    // y luego de esto pasamos a IntegranteDAO.obtenerTotalIntegrantes.
                    int total = dao.obtenerTotalIntegrantes(existente.getPlanId());
                    if (total > 1) {
                        return res.put("success", false).put("message", "No se puede cambiar el parentesco del Jefe de hogar actual si no se asigna previamente otra cabeza de familia.").toString();
                    }
                }
            }
        } catch (Exception e) {
            return res.put("success", false).put("message", "Error al procesar reglas de negocio en actualización: " + e.getMessage()).toString();
        }
        
        try {
            // Qué hace: Actualiza la información del integrante.
            // y luego de esto pasamos a IntegranteDAO.actualizarIntegrante, que corre el UPDATE.
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
    // Qué pasaría si no estuviera: Las afecciones médicas quedarían huérfanas en la base de datos (con llaves foráneas rotas) o el familiar eliminado seguiría figurando en el censo.
    public String eliminarIntegrante(int id) {
        JSONObject res = new JSONObject();
        try {
            // Qué hace: Elimina al integrante y limpia sus afecciones en cascada.
            // y luego de esto pasamos a IntegranteDAO.eliminarIntegrante, que ejecuta la transacción SQL delete.
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
    // Qué pasaría si no estuviera: El voluntario no podría ver qué padecimientos o requerimientos médicos de medicamentos posee el miembro de la familia.
    public String listarAfecciones(int memberId) {
        JSONObject res = new JSONObject();
        try {
            // Qué hace: Consulta las afecciones del integrante.
            // y luego de esto pasamos a IntegranteDAO.listarAfeccionesPorIntegrante, el cual ejecuta el query SELECT.
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
    // Qué pasaría si no estuviera: Al intentar editar un diagnóstico médico, no se cargarían en pantalla el nombre del padecimiento ni la dosis.
    public String obtenerAfeccion(int id) {
        JSONObject res = new JSONObject();
        try {
            // Qué hace: Obtiene la afección por su ID.
            // y luego de esto pasamos a IntegranteDAO.obtenerAfeccion, que ejecuta el SELECT relacional.
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
    // Qué pasaría si no estuviera: Se registrarían afecciones vacías o sin especificar la categoría (discapacidad, enfermedad crónica, etc.), arruinando los reportes de evacuación.
    public String crearAfeccion(AfeccionDTO dto) {
        JSONObject res = new JSONObject();
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            return res.put("success", false).put("message", "El nombre de la afección es obligatorio.").toString();
        }
        if (dto.getConditionTypeId() <= 0) {
            return res.put("success", false).put("message", "El tipo de afección es obligatorio.").toString();
        }
        
        try {
            // Qué hace: Guarda el registro de la afección médica.
            // y luego de esto pasamos a IntegranteDAO.crearAfeccion, que realiza la inserción JDBC.
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
    // Qué pasaría si no estuviera: El voluntario no podría actualizar la dosis de medicamentos requerida por un miembro con enfermedad crónica.
    public String actualizarAfeccion(int id, AfeccionDTO dto) {
        JSONObject res = new JSONObject();
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            return res.put("success", false).put("message", "El nombre de la afección es obligatorio.").toString();
        }
        if (dto.getConditionTypeId() <= 0) {
            return res.put("success", false).put("message", "El tipo de afección es obligatorio.").toString();
        }
        
        try {
            // Qué hace: Actualiza la afección en la base de datos.
            // y luego de esto pasamos a IntegranteDAO.actualizarAfeccion, que corre el UPDATE.
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
    // Qué pasaría si no estuviera: No se podrían remover diagnósticos erróneos, manteniendo datos de salud incorrectos en el censo.
    public String eliminarAfeccion(int id) {
        JSONObject res = new JSONObject();
        try {
            // Qué hace: Remueve la afección médica.
            // y luego de esto pasamos a IntegranteDAO.eliminarAfeccion, que ejecuta la sentencia DELETE.
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
    // Qué pasaría si no estuviera: No podríamos poblar los selects en la UI del voluntario para designar responsabilidades de tareas o croquis.
    public String obtenerIntegrantesSeleccion(int planId) {
        JSONObject res = new JSONObject();
        try {
            // Qué hace: Consulta la lista de todos los integrantes del plan familiar.
            // y luego de esto pasamos a IntegranteDAO.listarTodosIntegrantes, el cual realiza el query a la base de datos.
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

