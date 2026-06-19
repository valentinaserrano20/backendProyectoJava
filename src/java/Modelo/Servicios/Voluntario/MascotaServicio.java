package Modelo.Servicios.Voluntario;

import Modelo.DAO.MascotaDAO;
import Modelo.DTO.MascotaDTO;
import Modelo.DTO.VacunaMascotaDTO;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

// Qué hace: Clase de servicio que encapsula la lógica de negocio y procesamiento JSON para las entidades de Mascotas y Vacunas de Mascotas.
// Por qué existe: Actúa como intermediaria entre los Servlets del controlador y el DAO de persistencia, facilitando la validación y el formato homogéneo de datos.
// Qué problema resuelve: Centraliza la lógica de validación de campos obligatorios, conversión de fechas a edades y formateo JSON uniforme conforme a los estándares esperados por el cliente web.
public class MascotaServicio {
    private final MascotaDAO dao = new MascotaDAO();

    // Qué hace: Consulta las mascotas registradas asociadas a un plan familiar, calculando sus metadatos de paginación e inyectando campos de visualización.
    // Por qué existe: Permite al listado de mascotas en la interfaz web cargar la información de forma diferida y paginada.
    // Qué problema resuelve: Controla la volumetría de datos de red retornando de forma unificada el arreglo de mascotas (`data`) y los indicadores de paginación (`paginate`).
    public String listarMascotas(int planId, int page) {
        JSONObject res = new JSONObject();
        try {
            int limit = 10;
            int offset = (page - 1) * limit;
            
            int total = dao.obtenerTotalMascotas(planId);
            List<MascotaDTO> list = dao.listarMascotas(planId, limit, offset);
            
            JSONArray dataArr = new JSONArray();
            for (MascotaDTO m : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", m.getId());
                obj.put("name", m.getName());
                obj.put("breed", m.getBreed());
                obj.put("species_name", m.getSpeciesName());
                obj.put("age", m.getAge());
                obj.put("animal_gender_id", m.getAnimalGenderId());
                obj.put("animal_gender_name", m.getAnimalGenderName());
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
            res.put("message", "Error al listar mascotas: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Recupera la información estructurada de una mascota por su ID para retornar su DTO en un objeto JSON anidado.
    // Por qué existe: Permite poblar los datos de la mascota en el formulario de edición o modales detallados de la UI.
    // Qué problema resuelve: Resuelve la representación jerárquica de la especie (bajo la propiedad nested `species.name`) requerida por la lógica del modal del cliente.
    public String obtenerMascota(int id) {
        JSONObject res = new JSONObject();
        try {
            MascotaDTO m = dao.obtenerMascota(id);
            if (m == null) {
                return res.put("success", false).put("message", "Mascota no encontrada.").toString();
            }
            
            JSONObject obj = new JSONObject();
            obj.put("id", m.getId());
            obj.put("name", m.getName());
            obj.put("breed", m.getBreed() != null ? m.getBreed() : "");
            obj.put("birth_date", m.getBirthDate() != null ? m.getBirthDate() : "");
            obj.put("species_id", m.getSpeciesId());
            obj.put("animal_gender_id", m.getAnimalGenderId());
            
            // Nested object para coincidir con la llamada frontend de modalMascota: datos.species.name
            JSONObject speciesObj = new JSONObject();
            speciesObj.put("name", m.getSpeciesName() != null ? m.getSpeciesName() : "");
            obj.put("species", speciesObj);
            
            res.put("success", true);
            res.put("data", obj);
            
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al obtener mascota: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Valida los campos obligatorios del animal (nombre, especie y género) y los persiste en base de datos.
    // Por qué existe: Asegura que no se registren mascotas incompletas en la base de datos de Defensa Civil.
    // Qué problema resuelve: Previene la inserción de registros erróneos o nulos mediante validaciones previas del lado del servidor.
    public String crearMascota(MascotaDTO dto) {
        // Instanciamos un objeto JSON para compilar el retorno de la petición
        JSONObject res = new JSONObject();
        // Usamos el método .trim() y .isEmpty() para validar que el nombre de la mascota no sea nulo ni consista de puros espacios vacíos
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            return res.put("success", false).put("message", "El nombre de la mascota es obligatorio.").toString();
        }
        // Validamos que se haya provisto una especie numérica de catálogo válida
        if (dto.getSpeciesId() <= 0) {
            return res.put("success", false).put("message", "La especie de la mascota es obligatoria.").toString();
        }
        // Validamos que se haya provisto un género de animal de catálogo válido
        if (dto.getAnimalGenderId() <= 0) {
            return res.put("success", false).put("message", "El género de la mascota es obligatorio.").toString();
        }
        
        try {
            // Llamamos al método crearMascota de la clase MascotaDAO para realizar la inserción física en la base de datos
            int newId = dao.crearMascota(dto);
            // Creamos un JSONObject para empaquetar el ID autogenerado devuelto por MySQL
            JSONObject data = new JSONObject();
            data.put("id", newId);
            
            // Estructuramos la confirmación de éxito en el JSON de respuesta
            res.put("success", true);
            res.put("message", "Mascota registrada correctamente.");
            res.put("data", data);
        } catch (Exception e) {
            // Capturamos cualquier excepción de base de datos e inyectamos el mensaje del fallo
            res.put("success", false);
            res.put("message", "Error al registrar la mascota: " + e.getMessage());
        }
        // Retornamos el JSON formateado como cadena de texto
        return res.toString();
    }

    // Qué hace: Valida los parámetros modificados de la mascota y ejecuta la sentencia de actualización.
    // Por qué existe: Hace efectivas las correcciones de datos demográficos guardadas en el formulario de edición.
    // Qué problema resuelve: Protege la consistencia de los datos base del animal (nombre, especie, raza) tras la edición.
    public String actualizarMascota(int id, MascotaDTO dto) {
        JSONObject res = new JSONObject();
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            return res.put("success", false).put("message", "El nombre de la mascota es obligatorio.").toString();
        }
        if (dto.getSpeciesId() <= 0) {
            return res.put("success", false).put("message", "La especie de la mascota es obligatoria.").toString();
        }
        
        try {
            dao.actualizarMascota(id, dto);
            res.put("success", true);
            res.put("message", "Mascota actualizada correctamente.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al actualizar la mascota: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Ejecuta la eliminación en cascada transaccional de una mascota y su historial sanitario.
    // Por qué existe: Libera recursos físicos de la base de datos eliminando de forma íntegra un animal.
    // Qué problema resuelve: Elimina la mascota sin incurrir en violaciones de clave foránea en la tabla vacunas gracias a la transacción del DAO.
    public String eliminarMascota(int id) {
        JSONObject res = new JSONObject();
        try {
            dao.eliminarMascota(id);
            res.put("success", true);
            res.put("message", "Mascota eliminada correctamente.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al eliminar la mascota: " + e.getMessage());
        }
        return res.toString();
    }

    // =========================================================================
    // SUB-MÓDULO DE VACUNAS
    // =========================================================================

    // Qué hace: Consulta todas las vacunas aplicadas a una mascota específica y las empaqueta en una respuesta wrapped de tipo JSON.
    // Por qué existe: Expone el historial sanitario del animal para poblar el modal y listas en el front.
    // Qué problema resuelve: Suministra de forma estructurada los campos `name` y `date` que la UI recorre recursivamente.
    public String listarVacunas(int mascotaId) {
        JSONObject res = new JSONObject();
        try {
            List<VacunaMascotaDTO> list = dao.listarVacunasMascota(mascotaId);
            JSONArray arr = new JSONArray();
            for (VacunaMascotaDTO v : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", v.getId());
                obj.put("name", v.getName());
                obj.put("date", v.getDate());
                obj.put("pet_id", v.getPetId());
                arr.put(obj);
            }
            res.put("success", true);
            res.put("data", arr);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al listar vacunas de la mascota: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Recupera una única vacuna de mascota por su identificador para mostrar su detalle.
    // Por qué existe: Se usa en el modal interactivo de SweetAlert para poder editar un registro de vacunación singular.
    // Qué problema resuelve: Facilita que el frontend obtenga los valores exactos cargados previamente en el formulario.
    public String obtenerVacuna(int id) {
        JSONObject res = new JSONObject();
        try {
            VacunaMascotaDTO v = dao.obtenerVacuna(id);
            if (v == null) {
                return res.put("success", false).put("message", "Vacuna no encontrada.").toString();
            }
            
            JSONObject obj = new JSONObject();
            obj.put("id", v.getId());
            obj.put("name", v.getName());
            obj.put("date", v.getDate());
            obj.put("pet_id", v.getPetId());
            
            res.put("success", true);
            res.put("data", obj);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al obtener la vacuna de la mascota: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Valida los campos obligatorios del registro sanitario (nombre de vacuna y fecha) y la guarda.
    // Por qué existe: Asegura que no se ingresen dosis vacías o sin fecha de aplicación válida en la ficha médica.
    // Qué problema resuelve: Valida la obligatoriedad de la fecha y del nombre del medicamento previniendo inconsistencias en la base de datos.
    public String crearVacuna(VacunaMascotaDTO dto) {
        JSONObject res = new JSONObject();
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            return res.put("success", false).put("message", "El nombre de la vacuna es obligatorio.").toString();
        }
        if (dto.getDate() == null || dto.getDate().trim().isEmpty()) {
            return res.put("success", false).put("message", "La fecha de aplicación de la vacuna es obligatoria.").toString();
        }
        
        try {
            int newId = dao.crearVacuna(dto);
            JSONObject data = new JSONObject();
            data.put("id", newId);
            
            res.put("success", true);
            res.put("message", "Vacuna agregada correctamente.");
            res.put("data", data);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al registrar la vacuna: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Valida los datos actualizados de la vacuna y aplica la persistencia del cambio en base de datos.
    // Por qué existe: Permite modificar la dosificación, nombre o fecha del registro vacunal.
    // Qué problema resuelve: Modifica la vacuna singular sin alterar otras vacunas ni desvincular a la mascota asociada.
    public String actualizarVacuna(int id, VacunaMascotaDTO dto) {
        JSONObject res = new JSONObject();
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            return res.put("success", false).put("message", "El nombre de la vacuna es obligatorio.").toString();
        }
        if (dto.getDate() == null || dto.getDate().trim().isEmpty()) {
            return res.put("success", false).put("message", "La fecha de aplicación de la vacuna es obligatoria.").toString();
        }
        
        try {
            dao.actualizarVacuna(id, dto);
            res.put("success", true);
            res.put("message", "Vacuna actualizada correctamente.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al actualizar la vacuna: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Elimina físicamente una dosis vacunal del historial médico.
    // Por qué existe: Habilita la depuración de vacunas erróneas del animalito.
    // Qué problema resuelve: Borra de forma directa la fila de la tabla `vacunas` liberando recursos físicos en MySQL.
    public String eliminarVacuna(int id) {
        JSONObject res = new JSONObject();
        try {
            dao.eliminarVacuna(id);
            res.put("success", true);
            res.put("message", "Vacuna eliminada correctamente.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al eliminar la vacuna: " + e.getMessage());
        }
        return res.toString();
    }
}

