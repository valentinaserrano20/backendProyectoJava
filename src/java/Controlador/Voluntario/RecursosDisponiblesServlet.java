package Controlador.Voluntario;

import Modelo.DTO.RecursoDisponibleDTO;
import Modelo.Servicios.Voluntario.RecursoDisponibleServicio;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import org.json.JSONObject;

/**
 * Qué hace: Servlet controlador mapeado a /api/recursosDisponibles/* que recibe y procesa todas las solicitudes HTTP del CRUD de Recursos Disponibles.
 * Por qué existe: Actúa como punto de entrada de la API para las operaciones del módulo de Recursos en la interfaz SPA del voluntario.
 * Qué pasaría si no estuviera: Los voluntarios no tendrían cómo agregar, consultar, editar o borrar la lista de recursos disponibles de emergencia de las familias.
 */
@WebServlet("/api/recursosDisponibles/*")
public class RecursosDisponiblesServlet extends HttpServlet {

    // Qué hace: Instancia el servicio de lógica de negocios para los recursos de emergencia comunitarios.
    // Por qué existe: Separa la lógica JDBC y de negocio del enrutamiento de red de los servlets.
    // Qué pasaría si no estuviera: Sería obligatorio programar el acceso a datos directo con PreparedStatements dentro del servlet web.
    // Flujo: De aquí pasamos a RecursoDisponibleServicio.
    private final RecursoDisponibleServicio servicio = new RecursoDisponibleServicio();

    // Qué hace: Captura las peticiones HTTP e intercepta las de tipo PATCH para redirigirlas al método doPatch no nativo.
    // Por qué existe: Servlets nativos de Java no soportan doPatch por defecto de forma automática en la herencia de HttpServlet.
    // Qué pasaría si no estuviera: Las peticiones tipo PATCH del frontend arrojarían un error HTTP 405 (Method Not Allowed).
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

    // Qué hace: Atiende peticiones HTTP GET para listar los recursos de un plan familiar de forma paginada o cargar los datos de un único recurso.
    // Por qué existe: Permite consultar y renderizar la información de los recursos en la pantalla del voluntario y los detalles en modales.
    // Qué pasaría si no estuviera: La SPA no podría listar ni rellenar los recursos para renderizarlos en la tabla del censo.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Qué hace: Comprueba que el usuario posea una sesión activa en el servidor.
        // Por qué existe: Protege los recursos familiares de accesos no autorizados externos.
        // Qué pasaría si no estuviera: Cualquier atacante podría espiar la lista de recursos de evacuación, distancias y teléfonos de las familias registradas.
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            return;
        }

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Ruta o ID no especificado.").toString());
            return;
        }

        String[] parts = pathInfo.split("/");
        
        try {
            // Caso 1: recursosDisponibles/planFamiliar/{planId}
            if (parts[1].equals("planFamiliar")) {
                if (parts.length < 3) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de plan familiar no provisto.").toString());
                    return;
                }
                int planId = Integer.parseInt(parts[2]);
                String pageParam = request.getParameter("page");
                int page = (pageParam != null) ? Integer.parseInt(pageParam) : 1;
                
                // Qué hace: Obtiene la lista de recursos asignados a la vivienda.
                // y luego de esto pasamos a RecursoDisponibleServicio.listarRecursos, el cual lee la base de datos MySQL.
                String jsonRes = servicio.listarRecursos(planId, page);
                response.getWriter().write(jsonRes);
            } 
            // Caso 2: recursosDisponibles/{id}
            else {
                int id = Integer.parseInt(parts[1]);
                
                // Qué hace: Consulta un recurso específico por su ID único.
                // y luego de esto pasamos a RecursoDisponibleServicio.obtenerRecurso, que hace un SELECT filtrando por ID.
                String jsonRes = servicio.obtenerRecurso(id);
                response.getWriter().write(jsonRes);
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "El identificador de ruta debe ser numérico.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al procesar consulta: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Recibe peticiones HTTP POST para insertar un nuevo recurso disponible para la familia.
    // Por qué existe: Permite agregar nuevos registros geográficos comunitarios desde el formulario de creación.
    // Qué pasaría si no estuviera: Los voluntarios no podrían registrar nuevos puntos de apoyo o centros de recursos para la familia.
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            return;
        }

        StringBuilder buffer = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
        }

        try {
            JSONObject json = new JSONObject(buffer.toString());
            RecursoDisponibleDTO dto = new RecursoDisponibleDTO();
            dto.setPlaceName(json.getString("location")); 
            dto.setDistance(json.getInt("distance"));
            dto.setPhone(json.optString("phone", null));
            dto.setDescription(json.optString("description", null));
            dto.setPlanId(json.getInt("family_plan_id")); 
            dto.setResourceTypeId(json.getInt("resource_id")); 

            // Qué hace: Llama al servicio para almacenar físicamente el recurso familiar.
            // y luego de esto pasamos a RecursoDisponibleServicio.crearRecurso, el cual realiza las validaciones correspondientes y lo inserta en MySQL.
            String jsonRes = servicio.crearRecurso(dto);
            response.getWriter().write(jsonRes);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al registrar recurso: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Procesa solicitudes HTTP PATCH para actualizar la información de un recurso existente por su ID.
    // Por qué existe: Atiende la petición de guardado del formulario de edición.
    // Qué pasaría si no estuviera: No se podrían actualizar datos de distancia, descripción o tipo de recurso de forma dinámica.
    protected void doPatch(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            return;
        }

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID del recurso no provisto.").toString());
            return;
        }

        String[] parts = pathInfo.split("/");
        
        try {
            int id = Integer.parseInt(parts[1]);
            
            StringBuilder buffer = new StringBuilder();
            String line;
            try (BufferedReader reader = request.getReader()) {
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }
            }

            JSONObject json = new JSONObject(buffer.toString());
            RecursoDisponibleDTO dto = new RecursoDisponibleDTO();
            dto.setPlaceName(json.getString("location")); 
            dto.setDistance(json.getInt("distance"));
            dto.setPhone(json.optString("phone", null));
            dto.setDescription(json.optString("description", null));
            dto.setResourceTypeId(json.getInt("resource_id")); 

            // Qué hace: Modifica el recurso especificado mediante llamada delegada al servicio.
            // y luego de esto pasamos a RecursoDisponibleServicio.actualizarRecurso, que ejecuta el UPDATE en la BD.
            String jsonRes = servicio.actualizarRecurso(id, dto);
            response.getWriter().write(jsonRes);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de recurso debe ser numérico.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al actualizar recurso: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Procesa solicitudes HTTP DELETE para dar de baja física a un recurso disponible.
    // Por qué existe: Habilita el botón de eliminar de las tarjetas del listado del plan de emergencia.
    // Qué pasaría si no estuviera: Las familias acumularían recursos obsoletos sin opción de limpieza.
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            return;
        }

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de recurso no provisto.").toString());
            return;
        }

        String[] parts = pathInfo.split("/");
        
        try {
            int id = Integer.parseInt(parts[1]);
            
            // Qué hace: Elimina físicamente la fila del recurso.
            // y luego de esto pasamos a RecursoDisponibleServicio.eliminarRecurso, que ejecuta el DELETE en MySQL.
            String jsonRes = servicio.eliminarRecurso(id);
            response.getWriter().write(jsonRes);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de recurso debe ser numérico.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al eliminar recurso: " + e.getMessage()).toString());
        }
    }
}
