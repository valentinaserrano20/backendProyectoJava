package Modelo.Servicios.Voluntario;

import Modelo.DAO.MascotaDAO;
import Modelo.DTO.MascotaDTO;
import Modelo.DTO.VacunaMascotaDTO;
import Modelo.Utilidades.ResponseUtil;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Qué hace: Clase de servicio que encapsula la lógica de negocio y procesamiento JSON para las entidades de Mascotas y Vacunas de Mascotas.
 * Por qué existe: Actúa como intermediaria entre los Servlets del controlador y el DAO de persistencia, facilitando la validación y el formato homogéneo de datos.
 * Qué problema resuelve: Centraliza la lógica de validación de campos obligatorios, conversión de fechas a edades y formateo JSON uniforme.
 * Se implementó ResponseUtil para homogeneizar las respuestas hacia el frontend y reducir el boilerplate de bloques try-catch.
 */
public class MascotaServicio {
    private final MascotaDAO dao = new MascotaDAO();

    /**
     * Qué hace: Consulta las mascotas registradas asociadas a un plan familiar, calculando sus metadatos de paginación e inyectando campos de visualización.
     * Por qué se hizo: Permite al listado de mascotas en la interfaz web cargar la información de forma diferida y paginada.
     * Qué significa: Retorna un JSON estructurado con la lista de mascotas y los metadatos de paginación o el error correspondiente.
     */
    public String listarMascotas(int planId, int page) {
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
            
            return ResponseUtil.paginate(dataArr, paginate);
        } catch (Exception e) {
            return ResponseUtil.error("Error al listar mascotas: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Recupera la información estructurada de una mascota por su ID para retornar su DTO en un objeto JSON anidado.
     * Por qué se hizo: Permite poblar los datos de la mascota en el formulario de edición o modales detallados de la UI.
     * Qué significa: Retorna el JSON de éxito estructurado con los datos de la mascota o el error si no se encuentra.
     */
    public String obtenerMascota(int id) {
        try {
            MascotaDTO m = dao.obtenerMascota(id);
            if (m == null) {
                return ResponseUtil.error("Mascota no encontrada.");
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
            
            return ResponseUtil.success(obj);
        } catch (Exception e) {
            return ResponseUtil.error("Error al obtener mascota: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Valida los campos obligatorios del animal (nombre, especie y género) y los persiste en base de datos.
     * Por qué se hizo: Asegura que no se registren mascotas incompletas en la base de datos de Defensa Civil.
     * Qué significa: Valida la información y retorna éxito con el ID autogenerado del nuevo registro.
     */
    public String crearMascota(MascotaDTO dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            return ResponseUtil.error("El nombre de la mascota es obligatorio.");
        }
        if (dto.getSpeciesId() <= 0) {
            return ResponseUtil.error("La especie de la mascota es obligatoria.");
        }
        if (dto.getAnimalGenderId() <= 0) {
            return ResponseUtil.error("El género de la mascota es obligatorio.");
        }
        
        try {
            int newId = dao.crearMascota(dto);
            JSONObject data = new JSONObject();
            data.put("id", newId);
            return ResponseUtil.success("Mascota registrada correctamente.", data);
        } catch (Exception e) {
            return ResponseUtil.error("Error al registrar la mascota: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Valida los parámetros modificados de la mascota y ejecuta la sentencia de actualización.
     * Por qué se hizo: Hace efectivas las correcciones de datos demográficos guardadas en el formulario de edición.
     * Qué significa: Actualiza el registro y retorna el JSON de éxito o error estandarizado.
     */
    public String actualizarMascota(int id, MascotaDTO dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            return ResponseUtil.error("El nombre de la mascota es obligatorio.");
        }
        if (dto.getSpeciesId() <= 0) {
            return ResponseUtil.error("La especie de la mascota es obligatoria.");
        }
        
        try {
            dao.actualizarMascota(id, dto);
            return ResponseUtil.success("Mascota actualizada correctamente.");
        } catch (Exception e) {
            return ResponseUtil.error("Error al actualizar la mascota: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Ejecuta la eliminación en cascada transaccional de una mascota y su historial sanitario.
     * Por qué se hizo: Libera recursos físicos de la base de datos eliminando de forma íntegra un animal.
     * Qué significa: Elimina la mascota y retorna un JSON con el resultado de la transacción.
     */
    public String eliminarMascota(int id) {
        try {
            dao.eliminarMascota(id);
            return ResponseUtil.success("Mascota eliminada correctamente.");
        } catch (Exception e) {
            return ResponseUtil.error("Error al eliminar la mascota: " + e.getMessage());
        }
    }

    // =========================================================================
    // SUB-MÓDULO DE VACUNAS
    // =========================================================================

    /**
     * Qué hace: Consulta todas las vacunas aplicadas a una mascota específica y las empaqueta en una respuesta wrapped de tipo JSON.
     * Por qué se hizo: Expone el historial sanitario del animal para poblar el modal y listas en el front.
     * Qué significa: Retorna una respuesta JSON exitosa con la colección de vacunas mapeada.
     */
    public String listarVacunas(int mascotaId) {
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
            return ResponseUtil.success(arr);
        } catch (Exception e) {
            return ResponseUtil.error("Error al listar vacunas de la mascota: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Recupera una única vacuna de mascota por su identificador para mostrar su detalle.
     * Por qué se hizo: Se usa en el modal interactivo de SweetAlert para poder editar un registro de vacunación singular.
     * Qué significa: Retorna el DTO de la vacuna enlazada en un JSON de éxito.
     */
    public String obtenerVacuna(int id) {
        try {
            VacunaMascotaDTO v = dao.obtenerVacuna(id);
            if (v == null) {
                return ResponseUtil.error("Vacuna no encontrada.");
            }
            
            JSONObject obj = new JSONObject();
            obj.put("id", v.getId());
            obj.put("name", v.getName());
            obj.put("date", v.getDate());
            obj.put("pet_id", v.getPetId());
            return ResponseUtil.success(obj);
        } catch (Exception e) {
            return ResponseUtil.error("Error al obtener la vacuna de la mascota: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Valida los campos obligatorios del registro sanitario (nombre de vacuna y fecha) y la guarda.
     * Por qué se hizo: Asegura que no se ingresen dosis vacías o sin fecha de aplicación válida en la ficha médica.
     * Qué significa: Inserta la dosis en la BD y retorna éxito con el ID recién asignado.
     */
    public String crearVacuna(VacunaMascotaDTO dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            return ResponseUtil.error("El nombre de la vacuna es obligatorio.");
        }
        if (dto.getDate() == null || dto.getDate().trim().isEmpty()) {
            return ResponseUtil.error("La fecha de aplicación de la vacuna es obligatoria.");
        }
        
        try {
            int newId = dao.crearVacuna(dto);
            JSONObject data = new JSONObject();
            data.put("id", newId);
            return ResponseUtil.success("Vacuna agregada correctamente.", data);
        } catch (Exception e) {
            return ResponseUtil.error("Error al registrar la vacuna: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Valida los datos actualizados de la vacuna y aplica la persistencia del cambio en base de datos.
     * Por qué se hizo: Permite modificar la dosificación, nombre o fecha del registro vacunal.
     * Qué significa: Actualiza la vacuna en la base de datos y retorna confirmación.
     */
    public String actualizarVacuna(int id, VacunaMascotaDTO dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            return ResponseUtil.error("El nombre de la vacuna es obligatorio.");
        }
        if (dto.getDate() == null || dto.getDate().trim().isEmpty()) {
            return ResponseUtil.error("La fecha de aplicación de la vacuna es obligatoria.");
        }
        
        try {
            dao.actualizarVacuna(id, dto);
            return ResponseUtil.success("Vacuna actualizada correctamente.");
        } catch (Exception e) {
            return ResponseUtil.error("Error al actualizar la vacuna: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Elimina físicamente una dosis vacunal del historial médico.
     * Por qué se hizo: Habilita la depuración de vacunas erróneas del animalito.
     * Qué significa: Elimina el registro físico y retorna confirmación.
     */
    public String eliminarVacuna(int id) {
        try {
            dao.eliminarVacuna(id);
            return ResponseUtil.success("Vacuna eliminada correctamente.");
        } catch (Exception e) {
            return ResponseUtil.error("Error al eliminar la vacuna: " + e.getMessage());
        }
    }
}
