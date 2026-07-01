package Controlador.Voluntario.Mascotas;

/*
 * Qué hace (la acción): Importa la clase DTO de mascotas, la capa de servicio de mascotas, utilidades de JSON, respuestas web y APIs estándares de servlets de Jakarta.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.DTO.MascotaDTO: Contenedor que modela la información de un animal de compañía (nombre, raza, fecha de nacimiento, especie y género).
 *   - Modelo.Servicios.Voluntario.MascotaServicio: Servicio de negocio que procesa las reglas del censo de mascotas en la base de datos SQL.
 * Para qué se usa (el propósito): Proveer al servlet de las dependencias requeridas para procesar peticiones CRUD sobre las mascotas de la familia.
 * Por qué es importante (el impacto o problema que resuelve): Sin estas importaciones, no se podrían registrar las mascotas asociadas a la vivienda familiar para su respectiva evacuación.
 */
import Modelo.DTO.MascotaDTO;
import Modelo.Servicios.Voluntario.MascotaServicio;
import Modelo.Utilidades.JSONUtil;
import Modelo.Utilidades.ResponseUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.json.JSONObject;

/*
 * Qué hace (la acción): Asocia el servlet MascotaServlet con el endpoint "/api/pets/*" utilizando la anotación @WebServlet.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - @WebServlet: Anotación de Jakarta para registrar el enrutador en Tomcat.
 * Para qué se usa (el propósito): Servir como el endpoint de red CRUD para gestionar las mascotas en el Plan de Emergencia Familiar.
 * Por qué es importante (el impacto o problema que resuelve): Permite registrar de forma asíncrona la información de los animales de compañía, lo cual es de gran importancia en desastres naturales para coordinar refugios compatibles.
 */
@WebServlet("/api/pets/*")
public class MascotaServlet extends HttpServlet {

    /*
     * Qué hace (la acción): Instancia de manera privada y constante la variable servicio de tipo MascotaServicio.
     * Qué significa (conceptos, métodos, tipos involucrados): Instancia de la clase de servicios de negocio para mascotas.
     * Para qué se usa (el propósito): Invocar los métodos lógicos para administrar el censo de mascotas.
     */
    private final MascotaServicio servicio = new MascotaServicio();

    /*
     * Qué hace (la acción): Sobrescribe el método service para desviar las peticiones que utilizan el verbo HTTP PATCH hacia el método doPatch.
     * Qué significa (conceptos, métodos, tipos involucrados): Redirección manual del método PATCH en Jakarta Servlet API.
     * Para qué se usa (el propósito): Habilitar la edición parcial de los datos de la mascota en Tomcat.
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        String method = req.getMethod();
        if (method.equalsIgnoreCase("PATCH")) {
            doPatch(req, resp);
        } else {
            super.service(req, resp);
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doGet para listar las mascotas asociadas a un plan familiar en la subruta "/familyPlan/{planId}" de manera paginada, o consultar la información detallada de una mascota individual.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - parts[1].equals("familyPlan"): Detecta si se solicita listar todas las mascotas vinculadas al censo familiar.
     *   - pageParam: Parámetro que especifica la página actual para pintar la tabla en la UI.
     * Para qué se usa (el propósito): Alimentar la tabla de censo de mascotas en la interfaz del voluntario.
     * Por qué es importante (el impacto o problema que resuelve): Permite mostrar e interactuar de forma ordenada y paginada con el padrón de animales domésticos en el hogar de forma robusta.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Recurso no especificado."));
            return;
        }
        
        String[] parts = pathInfo.split("/");
        
        try {
            if (parts[1].equals("familyPlan")) {
                if (parts.length < 3) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write(ResponseUtil.error("ID de plan familiar no provisto."));
                    return;
                }
                int planId = Integer.parseInt(parts[2]);
                String pageParam = request.getParameter("page");
                int page = 1;
                if (pageParam != null && !pageParam.isEmpty()) {
                    page = Integer.parseInt(pageParam);
                }
                
                String resJson = servicio.listarMascotas(planId, page);
                response.getWriter().write(resJson);
            } else {
                int id = Integer.parseInt(parts[1]);
                String resJson = servicio.obtenerMascota(id);
                response.getWriter().write(resJson);
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("El identificador debe ser numérico."));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(ResponseUtil.error("Error interno: " + e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doPost para recibir el JSON de una nueva mascota (nombre, raza, fecha de nacimiento, especie y género) y guardarla en la base de datos SQL vinculada a su plan familiar.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - JSONUtil.leerJson(request): Parsea la petición asíncrona a un objeto JSON.
     *   - dto.setPlanId: Vincula a la mascota con la clave foránea del plan de emergencia familiar.
     * Para qué se usa (el propósito): Insertar un nuevo animal de compañía en el censo familiar de emergencia de la vivienda.
     * Por qué es importante (el impacto o problema que resuelve): Previene el registro de mascotas con datos nulos o inconsistentes en MySQL validando el cuerpo JSON antes de ejecutar la inserción en base de datos.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            JSONObject json = JSONUtil.leerJson(request);
            MascotaDTO dto = new MascotaDTO();
            dto.setName(json.getString("name"));
            dto.setBreed(json.optString("breed", null));
            dto.setBirthDate(json.optString("birth_date", null));
            dto.setSpeciesId(json.optInt("species_id", 0));
            dto.setAnimalGenderId(json.optInt("animal_gender_id", 0));
            dto.setPlanId(json.getInt("family_plan_id"));
            
            String resJson = servicio.crearMascota(dto);
            response.getWriter().write(resJson);
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("JSON mal formado: " + e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al crear mascota: " + e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doPatch para actualizar de manera parcial el nombre, raza, fecha de nacimiento, especie o género de una mascota existente por su ID.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - doPatch: Procesador de modificaciones parciales.
     *   - servicio.actualizarMascota(id, dto): Actualiza en base de datos el registro seleccionado.
     * Para qué se usa (el propósito): Modificar datos de la mascota sin tener que eliminarla y volverla a crear.
     */
    protected void doPatch(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID de mascota no provisto."));
            return;
        }
        
        String[] parts = pathInfo.split("/");
        
        try {
            int id = Integer.parseInt(parts[1]);
            JSONObject json = JSONUtil.leerJson(request);
            MascotaDTO dto = new MascotaDTO();
            dto.setName(json.getString("name"));
            dto.setBreed(json.optString("breed", null));
            dto.setBirthDate(json.optString("birth_date", null));
            dto.setSpeciesId(json.optInt("species_id", 0));
            dto.setAnimalGenderId(json.optInt("animal_gender_id", 0));
            
            String resJson = servicio.actualizarMascota(id, dto);
            response.getWriter().write(resJson);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID de mascota debe ser numérico."));
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("JSON mal formado: " + e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al actualizar mascota: " + e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doDelete para dar de baja física y eliminar una mascota por su ID en la base de datos SQL.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - servicio.eliminarMascota(id): Remueve la fila correspondiente en base de datos.
     * Para qué se usa (el propósito): Eliminar del censo familiar a una mascota del hogar de forma definitiva.
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID de mascota no provisto."));
            return;
        }
        
        String[] parts = pathInfo.split("/");
        
        try {
            int id = Integer.parseInt(parts[1]);
            String resJson = servicio.eliminarMascota(id);
            response.getWriter().write(resJson);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID de mascota debe ser numérico."));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al eliminar mascota: " + e.getMessage()));
        }
    }
}
