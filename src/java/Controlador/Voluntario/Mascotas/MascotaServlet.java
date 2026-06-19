package Controlador.Voluntario.Mascotas;

import Modelo.DTO.MascotaDTO;
import Modelo.Servicios.Voluntario.MascotaServicio;
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
 * Qué hace: Servlet encargado de mapear las peticiones HTTP CRUD (GET, POST, PATCH, DELETE) sobre la entidad mascotas.
 * Por qué existe: Actúa como el controlador de entrada para gestionar el flujo de datos de los animales de compañía de la familia evaluada.
 * Qué pasaría si no estuviera: Las familias no tendrían cómo reportar perros, gatos o ganado de tracción en su plan familiar, lo cual es crítico en evacuaciones.
 */
@WebServlet("/api/pets/*")
public class MascotaServlet extends HttpServlet {

    // Qué hace: Instancia el servicio de lógica de negocios para las mascotas familiares.
    // Por qué existe: Mantiene aislada la lógica de acceso a base de datos del enrutador de red Servlet.
    // Qué pasaría si no estuviera: El controlador web tendría que orquestar las sentencias PreparedStatements directas en MySQL.
    // Flujo: De aquí pasamos a MascotaServicio.
    private final MascotaServicio servicio = new MascotaServicio();

    // Qué hace: Intercepta todas las peticiones entrantes, capturando el método PATCH para redirigirlo a doPatch, y delegando otros verbos a super.service.
    // Por qué existe: Java Servlet API (HttpServlet) estándar no provee soporte nativo directo para el método doPatch.
    // Qué pasaría si no estuviera: Las llamadas de actualización parcial PATCH arrojarían un error HTTP 405 (Method Not Allowed).
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

    // Qué hace: Atiende peticiones GET para obtener la lista paginada de mascotas (bajo /familyPlan/{planId}) o el detalle individual (bajo /{id}).
    // Por qué existe: Provee al cliente los datos de las mascotas para pintar las tarjetas de listado y rellenar formularios.
    // Qué pasaría si no estuviera: El voluntario no podría ver el listado de mascotas en la SPA.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Qué hace: Comprueba que la sesión de usuario se encuentre activa.
        // Por qué existe: Resguarda los datos demográficos y la existencia de mascotas de las viviendas de accesos externos sin autorización.
        // Qué pasaría si no estuviera: Cualquier persona podría auditar qué animales y de qué razas existen en cada vivienda sin iniciar sesión.
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            return;
        }
        
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Recurso no especificado.").toString());
            return;
        }
        
        String[] parts = pathInfo.split("/");
        
        try {
            if (parts[1].equals("familyPlan")) {
                if (parts.length < 3) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de plan familiar no provisto.").toString());
                    return;
                }
                int planId = Integer.parseInt(parts[2]);
                String pageParam = request.getParameter("page");
                int page = 1;
                if (pageParam != null && !pageParam.isEmpty()) {
                    page = Integer.parseInt(pageParam);
                }
                
                // Qué hace: Recupera las mascotas del plan de emergencia familiar de forma paginada.
                // y luego de esto pasamos a MascotaServicio.listarMascotas, que ejecuta la consulta en la BD MySQL.
                String resJson = servicio.listarMascotas(planId, page);
                response.getWriter().write(resJson);
            } else {
                int id = Integer.parseInt(parts[1]);
                
                // Qué hace: Obtiene la información detallada de una mascota por su ID.
                // y luego de esto pasamos a MascotaServicio.obtenerMascota, que hace SELECT por ID en la BD.
                String resJson = servicio.obtenerMascota(id);
                response.getWriter().write(resJson);
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "El identificador debe ser numérico.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error interno: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Atiende peticiones POST para registrar una nueva mascota.
    // Por qué existe: Permite procesar el formulario de creación de mascotas del frontend.
    // Qué pasaría si no estuviera: No podríamos registrar animales en la ficha familiar de la SPA.
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
        try {
            StringBuilder buffer = new StringBuilder();
            String line;
            
            try (BufferedReader reader = request.getReader()) {
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }
            }
            
            JSONObject json = new JSONObject(buffer.toString());
            MascotaDTO dto = new MascotaDTO();
            dto.setName(json.getString("name"));
            dto.setBreed(json.optString("breed", null));
            dto.setBirthDate(json.optString("birth_date", null));
            dto.setSpeciesId(json.optInt("species_id", 0));
            dto.setAnimalGenderId(json.optInt("animal_gender_id", 0));
            dto.setPlanId(json.getInt("family_plan_id"));
            
            // Qué hace: Inserta una mascota nueva.
            // y luego de esto pasamos a MascotaServicio.crearMascota, que valida la información en la capa de negocio.
            String resJson = servicio.crearMascota(dto);
            response.getWriter().write(resJson);
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al crear mascota: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Atiende peticiones PATCH para actualizar la información de una mascota existente por su ID.
    // Por qué existe: Canaliza las actualizaciones parciales del formulario de edición.
    // Qué pasaría si no estuviera: No podríamos corregir el nombre, raza, especie o género de una mascota ya registrada.
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
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de mascota no provisto.").toString());
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
            MascotaDTO dto = new MascotaDTO();
            dto.setName(json.getString("name"));
            dto.setBreed(json.optString("breed", null));
            dto.setBirthDate(json.optString("birth_date", null));
            dto.setSpeciesId(json.optInt("species_id", 0));
            dto.setAnimalGenderId(json.optInt("animal_gender_id", 0));
            
            // Qué hace: Actualiza los detalles de la mascota.
            // y luego de esto pasamos a MascotaServicio.actualizarMascota, que actualiza la fila en base de datos.
            String resJson = servicio.actualizarMascota(id, dto);
            response.getWriter().write(resJson);
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de mascota debe ser numérico.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al actualizar mascota: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Atiende peticiones DELETE para eliminar físicamente una mascota del plan familiar.
    // Por qué existe: Permite dar de baja animales cargados por error o inactivos de la familia.
    // Qué pasaría si no estuviera: Las mascotas registradas por error quedarían asociadas permanentemente.
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
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de mascota no provisto.").toString());
            return;
        }
        
        String[] parts = pathInfo.split("/");
        
        try {
            int id = Integer.parseInt(parts[1]);
            
            // Qué hace: Remueve la mascota.
            // y luego de esto pasamos a MascotaServicio.eliminarMascota, que borra el registro en MySQL.
            String resJson = servicio.eliminarMascota(id);
            response.getWriter().write(resJson);
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de mascota debe ser numérico.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al eliminar mascota: " + e.getMessage()).toString());
        }
    }
}
