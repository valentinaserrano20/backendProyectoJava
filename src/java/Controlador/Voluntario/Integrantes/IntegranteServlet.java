package Controlador.Voluntario.Integrantes;

/*
 * Qué hace (la acción): Importa la clase DTO de integrantes, la capa de servicio de integrantes, utilidades de JSON, respuestas web y APIs estándares de servlets de Jakarta.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.DTO.IntegranteDTO: Clase de transferencia que encapsula los datos personales de un familiar (nombre, nacimiento, documento, EPS, teléfono, grupo sanguíneo, etc.).
 *   - Modelo.Servicios.Voluntario.IntegranteServicio: Servicio de negocio que procesa las reglas del censo familiar en la base de datos SQL.
 * Para qué se usa (el propósito): Proveer al servlet de las herramientas lógicas necesarias para procesar peticiones CRUD sobre el grupo familiar del voluntario.
 * Por qué es importante (el impacto o problema que resuelve): Sin estas importaciones, no se podrían registrar las personas que viven en la vivienda del plan de emergencia familiar.
 */
import Modelo.DTO.IntegranteDTO;
import Modelo.Servicios.Voluntario.IntegranteServicio;
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
 * Qué hace (la acción): Asocia el servlet IntegranteServlet con el endpoint "/api/members/*" utilizando la anotación @WebServlet.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - @WebServlet: Anotación de Jakarta para registrar dinámicamente el enrutador en Tomcat.
 * Para qué se usa (el propósito): Servir como el endpoint de red CRUD para gestionar la lista de personas del censo de evacuación familiar.
 * Por qué es importante (el impacto o problema que resuelve): Mapea las llamadas RESTful para que el voluntario registre, edite o dé de baja a integrantes del hogar de forma asíncrona.
 */
@WebServlet("/api/members/*")
public class IntegranteServlet extends HttpServlet {

    /*
     * Qué hace (la acción): Instancia de manera privada y constante la variable servicio de tipo IntegranteServicio.
     * Qué significa (conceptos, métodos, tipos involucrados): Instancia de la clase de servicios de negocio para los integrantes.
     * Para qué se usa (el propósito): Invocar los procesos de creación, lectura, actualización y eliminación.
     */
    private final IntegranteServicio servicio = new IntegranteServicio();

    /*
     * Qué hace (la acción): Sobrescribe el método doGet para listar los integrantes de un plan familiar en la subruta "/familyPlan/{planId}" de manera paginada, o en "/familyPlan/select/{planId}" de manera plana sin paginar para rellenar controles select (ej. para asignar un coordinador), o consultar un integrante unitario.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - select: Sub-segmento que indica la recuperación de la lista simplificada de integrantes para los dropdowns.
     *   - pageParam: Parámetro de paginación que indica la página actual de la tabla en el frontend.
     * Para qué se usa (el propósito): Alimentar la tabla de integrantes de la familia y los dropdowns de selección de responsables en el Plan de Acción familiar.
     * Por qué es importante (el impacto o problema que resuelve): Permite que la UI del voluntario renderice dinámicamente a los integrantes de su hogar aplicando paginación para agilizar la navegación de red.
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
                
                // Caso A: members/familyPlan/select/{planId} (Para combos de coordinadores)
                if (parts.length > 3 && parts[2].equals("select")) {
                    int planId = Integer.parseInt(parts[3]);
                    String resJson = servicio.obtenerIntegrantesSeleccion(planId);
                    response.getWriter().write(resJson);
                } 
                // Caso B: members/familyPlan/{planId} (Paginado para la tabla de integrantes)
                else {
                    int planId = Integer.parseInt(parts[2]);
                    String pageParam = request.getParameter("page");
                    int page = 1;
                    if (pageParam != null && !pageParam.isEmpty()) {
                        page = Integer.parseInt(pageParam);
                    }
                    
                    String resJson = servicio.listarIntegrantes(planId, page);
                    response.getWriter().write(resJson);
                }
            } else {
                int id = Integer.parseInt(parts[1]);
                String resJson = servicio.obtenerIntegrante(id);
                response.getWriter().write(resJson);
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("El identificador debe ser numérico."));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(ResponseUtil.error("Error del servidor: " + e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doPost para recibir el JSON de un nuevo integrante (nombres, apellidos, nacimiento, tipo de documento, EPS, teléfono, grupo sanguíneo, nacionalidad y género) y guardarlo en la base de datos SQL atado a su plan familiar.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - JSONUtil.leerJson(request): Parsea la petición asíncrona a un objeto JSON.
     *   - dto.setPlanId(planId): Vincula al integrante con la clave foránea del plan de emergencia familiar.
     * Para qué se usa (el propósito): Insertar un nuevo miembro del hogar en el censo familiar de emergencia de la vivienda.
     * Por qué es importante (el impacto o problema que resuelve): Previene el registro de integrantes con datos nulos o inconsistentes en MySQL validando el cuerpo JSON antes de ejecutar la inserción en base de datos.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID de plan familiar no provisto."));
            return;
        }
        
        String[] parts = pathInfo.split("/");
        
        try {
            int planId = Integer.parseInt(parts[1]);
            JSONObject json = JSONUtil.leerJson(request);
            IntegranteDTO dto = new IntegranteDTO();
            dto.setPlanId(planId);
            dto.setNames(json.getString("names"));
            dto.setLastNames(json.getString("last_names"));
            dto.setBirthDate(json.getString("birth_date"));
            dto.setDocumentNumber(json.optString("document_number", null));
            dto.setEps(json.optString("eps", null));
            dto.setPhone(json.optString("phone", null));
            
            dto.setDocumentTypeId(json.optInt("document_type_id", 0));
            dto.setKinshipId(json.optInt("kinship_id", 0));
            dto.setBloodGroupId(json.optInt("blood_group_id", 0));
            dto.setNationalityId(json.optInt("nationality_id", 0));
            dto.setGenderId(json.optInt("gender_id", 0));
            
            String resJson = servicio.crearIntegrante(dto);
            response.getWriter().write(resJson);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID de plan familiar debe ser numérico."));
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("JSON mal formado: " + e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al registrar integrante: " + e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doPut para actualizar de manera parcial todos los datos personales o de contacto de un integrante familiar existente por su ID.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - doPut: Procesador de modificaciones completas de recursos.
     *   - servicio.actualizarIntegrante(id, dto): Actualiza en base de datos el integrante seleccionado.
     * Para qué se usa (el propósito): Modificar datos personales del miembro familiar sin tener que eliminarlo y volverlo a crear.
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID de integrante no provisto."));
            return;
        }
        
        String[] parts = pathInfo.split("/");
        
        try {
            int id = Integer.parseInt(parts[1]);
            JSONObject json = JSONUtil.leerJson(request);
            IntegranteDTO dto = new IntegranteDTO();
            dto.setNames(json.getString("names"));
            dto.setLastNames(json.getString("last_names"));
            dto.setBirthDate(json.getString("birth_date"));
            dto.setDocumentNumber(json.optString("document_number", null));
            dto.setEps(json.optString("eps", null));
            dto.setPhone(json.optString("phone", null));
            
            dto.setDocumentTypeId(json.optInt("document_type_id", 0));
            dto.setKinshipId(json.optInt("kinship_id", 0));
            dto.setBloodGroupId(json.optInt("blood_group_id", 0));
            dto.setNationalityId(json.optInt("nationality_id", 0));
            dto.setGenderId(json.optInt("gender_id", 0));
            
            String resJson = servicio.actualizarIntegrante(id, dto);
            response.getWriter().write(resJson);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID de integrante debe ser numérico."));
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("JSON mal formado: " + e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al actualizar integrante: " + e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doDelete para dar de baja física y eliminar un integrante familiar por su ID en la base de datos SQL.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - servicio.eliminarIntegrante(id): Remueve la fila correspondiente en base de datos.
     * Para qué se usa (el propósito): Eliminar del censo familiar a un integrante del hogar de forma definitiva.
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID de integrante no provisto."));
            return;
        }
        
        String[] parts = pathInfo.split("/");
        
        try {
            int id = Integer.parseInt(parts[1]);
            String resJson = servicio.eliminarIntegrante(id);
            response.getWriter().write(resJson);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("ID de integrante debe ser numérico."));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al eliminar integrante: " + e.getMessage()));
        }
    }
}
