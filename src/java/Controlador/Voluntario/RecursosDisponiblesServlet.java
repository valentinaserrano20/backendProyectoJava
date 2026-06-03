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

// Qué hace: Servlet controlador mapeado a /api/recursosDisponibles/* que recibe y procesa todas las solicitudes HTTP del CRUD de Recursos Disponibles.
// Por qué existe: Actúa como punto de entrada de la API para las operaciones del módulo de Recursos en la interfaz SPA del voluntario.
// Qué problema resuelve: Enruta las solicitudes HTTP (GET, POST, PATCH, DELETE) validando los permisos de sesión y deserializando los payloads JSON.
@WebServlet("/api/recursosDisponibles/*")
public class RecursosDisponiblesServlet extends HttpServlet {
    private final RecursoDisponibleServicio servicio = new RecursoDisponibleServicio();

    // Qué hace: Captura las peticiones HTTP e intercepta las de tipo PATCH para redirigirlas al método doPatch no nativo.
    // Por qué existe: Servlets nativos de Java no soportan doPatch por defecto de forma automática en la herencia de HttpServlet.
    // Qué problema resuelve: Habilita el soporte completo para peticiones tipo PATCH utilizadas por la SPA en la edición de recursos.
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
    // Qué problema resuelve: Parsea los parámetros de ruta y de consulta (page) y mapea las peticiones a los métodos correspondientes del servicio.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
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
                
                String jsonRes = servicio.listarRecursos(planId, page);
                response.getWriter().write(jsonRes);
            } 
            // Caso 2: recursosDisponibles/{id}
            else {
                int id = Integer.parseInt(parts[1]);
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
    // Qué problema resuelve: Lee el cuerpo de la solicitud en bytes, parsea el JSON a un DTO y gatilla la inserción física.
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
            dto.setPlaceName(json.getString("location")); // Mapeado desde 'location' en la SPA
            dto.setDistance(json.getInt("distance"));
            dto.setPhone(json.optString("phone", null));
            dto.setDescription(json.optString("description", null));
            dto.setPlanId(json.getInt("family_plan_id")); // Mapeado desde 'family_plan_id'
            dto.setResourceTypeId(json.getInt("resource_id")); // Mapeado desde 'resource_id'

            String jsonRes = servicio.crearRecurso(dto);
            response.getWriter().write(jsonRes);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al registrar recurso: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Procesa solicitudes HTTP PATCH para actualizar la información de un recurso existente por su ID.
    // Por qué existe: Atiende la petición de guardado del formulario de edición.
    // Qué problema resuelve: Extrae el ID de la URL, procesa los campos del DTO actualizados y ejecuta el UPDATE.
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
            dto.setPlaceName(json.getString("location")); // location
            dto.setDistance(json.getInt("distance"));
            dto.setPhone(json.optString("phone", null));
            dto.setDescription(json.optString("description", null));
            dto.setResourceTypeId(json.getInt("resource_id")); // resource_id

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
    // Qué problema resuelve: Elimina de forma directa el registro en base de datos.
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
