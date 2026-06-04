package Modelo.Servicios.Supervisor;

import Modelo.DAO.IntegranteDAO;
import Modelo.DAO.MascotaDAO;
import Modelo.DAO.CatalogoDAO;
import Modelo.DTO.MascotaDTO;
import Modelo.DTO.UbicacionDTO;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

// Qué hace: Clase de servicio exclusiva del rol Supervisor que agrupa las consultas globales necesarias para la revisión de planes familiares.
// Por qué existe: Centraliza la lógica de negocio del supervisor separándola de los servicios del voluntario, respetando la arquitectura de separación por roles.
// Qué problema resuelve: Evita que los servicios y servlets del voluntario se contaminen con consultas globales que solo corresponden al flujo de revisión del supervisor.
public class SupervisorServicio {

    // Instancia del DAO de integrantes para reutilizar el método obtenerTodosFamilyMembers()
    private final IntegranteDAO integranteDAO = new IntegranteDAO();
    // Instancia del DAO de mascotas para reutilizar el método obtenerTodasMascotas()
    private final MascotaDAO mascotaDAO = new MascotaDAO();
    // Instancia del DAO de catálogos para reutilizar el método getSectores()
    private final CatalogoDAO catalogoDAO = new CatalogoDAO();

    // Qué hace: Obtiene la correspondencia global de integrantes y sus planes familiares asociados.
    // Por qué existe: El supervisor necesita filtrar en memoria qué integrantes pertenecen al plan que está revisando.
    // Qué problema resuelve: Retorna un JSONArray con los pares {member_id, family_plan_id} para que el frontend filtre localmente.
    public String obtenerTodosFamilyMembers() {
        JSONObject res = new JSONObject();
        try {
            // Invoca el método existente en IntegranteDAO que consulta todos los integrantes sin filtro de plan
            List<Map<String, Object>> lista = integranteDAO.obtenerTodosFamilyMembers();
            // Inicializa el arreglo JSON que contendrá los mapeos de integrantes
            JSONArray arr = new JSONArray();
            // Recorre cada mapa de la lista y lo convierte a un objeto JSON
            for (Map<String, Object> map : lista) {
                JSONObject obj = new JSONObject();
                // Asigna el ID del integrante al campo member_id
                obj.put("member_id", map.get("member_id"));
                // Asigna el ID del plan familiar al campo family_plan_id
                obj.put("family_plan_id", map.get("family_plan_id"));
                arr.put(obj);
            }
            // Marca la respuesta como exitosa
            res.put("success", true);
            // Adjunta el arreglo de datos al campo data
            res.put("data", arr);
        } catch (Exception e) {
            // Si ocurre un error, marca la respuesta como fallida con el mensaje descriptivo
            res.put("success", false);
            res.put("message", "Error al obtener integrantes globales: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Obtiene la lista global de todas las mascotas registradas en el sistema.
    // Por qué existe: El supervisor necesita filtrar en memoria qué mascotas pertenecen al plan que está revisando.
    // Qué problema resuelve: Retorna un JSONArray con los datos básicos de cada mascota incluyendo su plan familiar para el filtrado local.
    public String obtenerTodasMascotas() {
        JSONObject res = new JSONObject();
        try {
            // Invoca el método existente en MascotaDAO que consulta todas las mascotas sin filtro de plan
            List<MascotaDTO> lista = mascotaDAO.obtenerTodasMascotas();
            // Inicializa el arreglo JSON que contendrá las mascotas
            JSONArray arr = new JSONArray();
            // Recorre cada mascota y la convierte a un objeto JSON con los campos esperados por el frontend
            for (MascotaDTO m : lista) {
                JSONObject obj = new JSONObject();
                // Identificador único de la mascota
                obj.put("id", m.getId());
                // Nombre de la mascota
                obj.put("name", m.getName());
                // Raza de la mascota
                obj.put("breed", m.getBreed());
                // Nombre de la especie de la mascota
                obj.put("species_name", m.getSpeciesName());
                // Identificador de la especie para consultas posteriores del frontend
                obj.put("species_id", m.getSpeciesId());
                // Edad calculada de la mascota
                obj.put("age", m.getAge());
                // Identificador del género animal
                obj.put("animal_gender_id", m.getAnimalGenderId());
                // Nombre del género animal
                obj.put("animal_gender_name", m.getAnimalGenderName());
                // Identificador del plan familiar al que pertenece la mascota
                obj.put("family_plan_id", m.getPlanId());
                arr.put(obj);
            }
            // Marca la respuesta como exitosa
            res.put("success", true);
            // Adjunta el arreglo de datos al campo data
            res.put("data", arr);
        } catch (Exception e) {
            // Si ocurre un error, marca la respuesta como fallida con el mensaje descriptivo
            res.put("success", false);
            res.put("message", "Error al obtener mascotas globales: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Obtiene la lista de todos los sectores activos registrados en el sistema.
    // Por qué existe: El supervisor necesita el catálogo de sectores para mostrar el nombre del sector en la cabecera del plan familiar.
    // Qué problema resuelve: Retorna un JSONArray con los datos de cada sector (id, name) para que el frontend los busque localmente.
    public String obtenerTodosSectores() {
        JSONObject res = new JSONObject();
        try {
            // Invoca el método existente en CatalogoDAO que consulta todos los sectores activos
            List<UbicacionDTO> lista = catalogoDAO.getSectores();
            // Inicializa el arreglo JSON que contendrá los sectores
            JSONArray arr = new JSONArray();
            // Recorre cada sector y lo convierte a un objeto JSON
            for (UbicacionDTO s : lista) {
                JSONObject obj = new JSONObject();
                // Identificador único del sector
                obj.put("id", s.getId());
                // Nombre del sector en español
                obj.put("name", s.getNombre());
                arr.put(obj);
            }
            // Marca la respuesta como exitosa
            res.put("success", true);
            // Adjunta el arreglo de datos al campo data
            res.put("data", arr);
        } catch (Exception e) {
            // Si ocurre un error, marca la respuesta como fallida con el mensaje descriptivo
            res.put("success", false);
            res.put("message", "Error al obtener sectores: " + e.getMessage());
        }
        return res.toString();
    }
}
