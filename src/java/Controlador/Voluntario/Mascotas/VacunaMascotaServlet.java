package Controlador.Voluntario.Mascotas;

/*
 * Qué hace (la acción): Importa la clase DTO de vacunas de mascotas, la capa de servicio de mascotas, utilidades de JSON, respuestas web y APIs estándares de servlets de Jakarta.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.DTO.VacunaMascotaDTO: Contenedor que modela la información sanitaria de inmunización de las mascotas (nombre de vacuna y fecha de aplicación).
 *   - Modelo.Servicios.Voluntario.MascotaServicio: Servicio de negocio que realiza operaciones CRUD en base de datos para las mascotas y sus cartillas de vacunación.
 * Para qué se usa (el propósito): Proveer las dependencias de red y negocio requeridas para gestionar las vacunas de los animales domésticos.
 * Por qué es importante (el impacto o problema que resuelve): Sin estas importaciones, no se podría registrar las fechas de vacunación de los animales, lo cual es vital en la prevención de zoonosis durante evacuaciones colectivas.
 */
import Modelo.DTO.VacunaMascotaDTO;
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
 * Qué hace (la acción): Asocia el servlet VacunaMascotaServlet con el endpoint "/api/petVaccines/*" utilizando la anotación @WebServlet.
 * Qué significa (conceptos, métodos, tipos involucrados): Mapea la ruta para que Tomcat procese el historial sanitario de las mascotas del plan familiar.
 * Para qué se usa (el propósito): Servir como el endpoint de red CRUD para gestionar las vacunas de los animales de compañía.
 * Por qué es importante (el impacto o problema que resuelve): Permite registrar de forma asíncrona qué vacunas tienen aplicadas las mascotas del hogar, facilitando el control y prevención ante emergencias.
 */
@WebServlet("/api/petVaccines/*")
public class VacunaMascotaServlet extends HttpServlet {

    /*
     * Qué hace (la acción): Instancia de manera privada y constante la variable servicio de tipo MascotaServicio.
     * Qué significa (conceptos, métodos, tipos involucrados): Instancia de la clase de servicios de negocio para mascotas y vacunas.
     * Para qué se usa (el propósito): Invocar las funciones lógicas de administración del historial de vacunación.
     */
    private final MascotaServicio servicio = new MascotaServicio();

    /*
     * Qué hace (la acción): Sobrescribe el método service para canalizar las peticiones HTTP PATCH hacia el método doPatch.
     * Qué significa (conceptos, métodos, tipos involucrados): Redirecciona el método de red PATCH en Jakarta Servlet API.
     * Para qué se usa (el propósito): Habilitar la actualización parcial de la cartilla sanitaria en el servidor.
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
     * Qué hace (la acción): Sobrescribe el método doGet para listar las vacunas aplicadas a una mascota en particular en la subruta "/pet/{petId}", o consultar el detalle de una vacuna individual por su ID.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - parts[1].equals("pet"): Identifica que se solicita listar el historial de vacunas de un animal.
     *   - parts[1]: ID numérico directo que representa la consulta unitaria de una vacuna.
     * Para qué se usa (el propósito): Mostrar las inmunizaciones registradas para cada mascota en los formularios del plan familiar.
     * Por qué es importante (el impacto o problema que resuelve): Permite que el voluntario visualice de manera inmediata el estado sanitario de sus mascotas.
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
            if (parts[1].equals("pet")) {
                if (parts.length < 3) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write(ResponseUtil.error("ID de mascota no provisto."));
                    return;
                }
                int mascotaId = Integer.parseInt(parts[2]);
                
                String resJson = servicio.listarVacunas(mascotaId);
                response.getWriter().write(resJson);
            } else {
                int id = Integer.parseInt(parts[1]);
                String resJson = servicio.obtenerVacuna(id);
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
     * Qué hace (la acción): Sobrescribe el método doPost para crear y guardar un nuevo registro de vacuna en la base de datos SQL.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - JSONUtil.leerJson(request): Parsea la petición asíncrona a un objeto JSON.
     *   - dto.setPetId: Vincula la vacuna con la clave foránea de la mascota.
     * Para qué se usa (el propósito): Insertar una vacuna nueva en el historial sanitario del animal doméstico.
     * Por qué es importante (el impacto o problema que resuelve): Previene el registro de vacunas con datos nulos o inconsistentes en MySQL validando el cuerpo JSON antes de ejecutar la inserción en base de datos.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            JSONObject json = JSONUtil.leerJson(request);
            VacunaMascotaDTO dto = new VacunaMascotaDTO();
            dto.setName(json.getString("name"));
            dto.setDate(json.getString("date"));
            dto.setPetId(json.getInt("pet_id"));
            
            String resJson = servicio.crearVacuna(dto);
            response.getWriter().write(resJson);
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("JSON mal formado: " + e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al crear vacuna: " + e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doPatch para actualizar de manera parcial el nombre de la vacuna o la fecha de aplicación de una vacuna existente por su ID.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - doPatch: Procesador de modificaciones parciales.
     *   - servicio.actualizarVacuna(id, dto): Actualiza la fila en base de datos.
     * Para qué se usa (el propósito): Modificar datos de la vacuna sin tener que eliminarla y volverla a crear.
     */
    protected void doPatch(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID de vacuna no provisto."));
            return;
        }
        
        String[] parts = pathInfo.split("/");
        
        try {
            int id = Integer.parseInt(parts[1]);
            
            JSONObject json = JSONUtil.leerJson(request);
            VacunaMascotaDTO dto = new VacunaMascotaDTO();
            dto.setName(json.getString("name"));
            dto.setDate(json.getString("date"));
            
            String resJson = servicio.actualizarVacuna(id, dto);
            response.getWriter().write(resJson);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID de vacuna debe ser numérico."));
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("JSON mal formado: " + e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al actualizar vacuna: " + e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doDelete para dar de baja física y eliminar una vacuna por su ID en la base de datos SQL.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - servicio.eliminarVacuna(id): Remueve la fila correspondiente en base de datos.
     * Para qué se usa (el propósito): Eliminar del historial sanitario una vacuna errónea de forma definitiva.
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID de vacuna no provisto."));
            return;
        }
        
        String[] parts = pathInfo.split("/");
        
        try {
            int id = Integer.parseInt(parts[1]);
            String resJson = servicio.eliminarVacuna(id);
            response.getWriter().write(resJson);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID de vacuna debe ser numérico."));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al eliminar vacuna: " + e.getMessage()));
        }
    }
}
