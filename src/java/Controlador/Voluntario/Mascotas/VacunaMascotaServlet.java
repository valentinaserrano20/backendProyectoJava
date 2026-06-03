package Controlador.Voluntario.Mascotas;

import Modelo.DTO.VacunaMascotaDTO;
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

// Qué hace: Servlet encargado de mapear las peticiones HTTP CRUD (GET, POST, PATCH, DELETE) sobre el historial sanitario de vacunas de mascotas.
// Por qué existe: Actúa como el controlador de entrada para gestionar la información de inmunizaciones de los animales del hogar evaluado.
// Qué problema resuelve: Enruta y valida los payloads de vacunas de mascotas mediante la sesión activa y delega su procesamiento en la capa de servicios.
@WebServlet("/api/petVaccines/*")
public class VacunaMascotaServlet extends HttpServlet {
    private final MascotaServicio servicio = new MascotaServicio();

    // Qué hace: Intercepta peticiones HTTP entrantes redirigiendo el método PATCH a doPatch y enviando los demás a la rutina de super.service.
    // Por qué existe: Habilita el soporte para peticiones parciales PATCH que la especificación de HttpServlet base de Java EE/Jakarta EE no cubre por defecto.
    // Qué problema resuelve: Permite implementar actualizaciones parciales de campos en la interfaz web de vacunas de mascotas.
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

    // Qué hace: Atiende peticiones GET para listar las vacunas de una mascota (bajo /pet/{mascotaId}) o consultar los detalles de una vacuna singular (bajo /{id}).
    // Por qué existe: Permite a la interfaz web desplegar las vacunas aplicadas en la tarjeta de la mascota o precargar datos en el modal de edición.
    // Qué problema resuelve: Provee acceso estructurado a los padecimientos o inmunizaciones de la mascota filtrando por IDs relacionales con sesión activa.
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
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Recurso no especificado.").toString());
            return;
        }
        
        String[] parts = pathInfo.split("/");
        
        try {
            if (parts[1].equals("pet")) {
                if (parts.length < 3) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de mascota no provisto.").toString());
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
            response.getWriter().write(new JSONObject().put("success", false).put("message", "El identificador debe ser numérico.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error interno: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Atiende peticiones POST para registrar una vacuna nueva a una mascota.
    // Por qué existe: Habilita la inserción de nuevos registros sanitarios en el historial médico de las mascotas.
    // Qué problema resuelve: Recibe el payload JSON, mapea las propiedades al DTO sanitario y delega su creación al servicio de negocio.
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
            VacunaMascotaDTO dto = new VacunaMascotaDTO();
            dto.setName(json.getString("name"));
            dto.setDate(json.getString("date"));
            dto.setPetId(json.getInt("pet_id"));
            
            String resJson = servicio.crearVacuna(dto);
            response.getWriter().write(resJson);
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al crear vacuna: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Atiende peticiones PATCH para actualizar la información de una vacuna específica.
    // Por qué existe: Soporta la modificación de dosis o fechas del historial sanitario del animal.
    // Qué problema resuelve: Recibe el cuerpo JSON de modificación y actualiza de manera exacta la vacuna identificada por su ID único.
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
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de vacuna no provisto.").toString());
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
            VacunaMascotaDTO dto = new VacunaMascotaDTO();
            dto.setName(json.getString("name"));
            dto.setDate(json.getString("date"));
            
            String resJson = servicio.actualizarVacuna(id, dto);
            response.getWriter().write(resJson);
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de vacuna debe ser numérico.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al actualizar vacuna: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Atiende peticiones DELETE para eliminar de la base de datos una vacuna por su ID.
    // Por qué existe: Habilita al usuario del sistema a quitar registros de vacunación obsoletos o erróneos.
    // Qué problema resuelve: Remueve directamente la dosis vacunal sin alterar otras tablas ni desvincular al animal de compañía.
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
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de vacuna no provisto.").toString());
            return;
        }
        
        String[] parts = pathInfo.split("/");
        
        try {
            int id = Integer.parseInt(parts[1]);
            String resJson = servicio.eliminarVacuna(id);
            response.getWriter().write(resJson);
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de vacuna debe ser numérico.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al eliminar vacuna: " + e.getMessage()).toString());
        }
    }
}
