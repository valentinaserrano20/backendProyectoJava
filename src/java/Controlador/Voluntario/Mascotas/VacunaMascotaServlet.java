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
// Qué pasaría si no estuviera: Los voluntarios no tendrían un endpoint específico para registrar el control de vacunas de las mascotas, imposibilitando el seguimiento sanitario en refugios.
@WebServlet("/api/petVaccines/*")
public class VacunaMascotaServlet extends HttpServlet {
    // Qué hace: Instancia el servicio de lógica de negocios de mascotas.
    // Por qué existe: Separa la lógica de control de HTTP de la capa de acceso y negocio de mascotas.
    // Qué pasaría si no estuviera: El servlet tendría que interactuar directamente con los DAOs y la base de datos MySQL.
    // Flujo: De aquí pasamos a MascotaServicio.
    private final MascotaServicio servicio = new MascotaServicio();

    // Qué hace: Intercepta peticiones HTTP entrantes redirigiendo el método PATCH a doPatch y enviando los demás a la rutina de super.service.
    // Por qué existe: Habilita el soporte para peticiones parciales PATCH que la especificación de HttpServlet base de Java EE/Jakarta EE no cubre por defecto.
    // Qué pasaría si no estuviera: Las llamadas de tipo PATCH realizadas por el frontend arrojarían un error HTTP 405 (Method Not Allowed).
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
    // Qué pasaría si no estuviera: El voluntario no podría visualizar el historial médico de las vacunas en el frontend.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Qué hace: Valida que el usuario tenga una sesión de servidor activa.
        // Por qué existe: Protege la información de las mascotas familiares contra accesos de usuarios no autenticados.
        // Qué pasaría si no estuviera: Cualquier atacante externo podría consultar los datos y vacunas de las mascotas de la comunidad.
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
                
                // Qué hace: Obtiene la lista de vacunas asociadas a la mascota.
                // y luego de esto pasamos a MascotaServicio.listarVacunas, que realiza la consulta SQL en la base de datos.
                String resJson = servicio.listarVacunas(mascotaId);
                response.getWriter().write(resJson);
            } else {
                int id = Integer.parseInt(parts[1]);
                
                // Qué hace: Obtiene los detalles de una vacuna específica por su ID.
                // y luego de esto pasamos a MascotaServicio.obtenerVacuna, que ejecuta el SELECT correspondiente.
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
    // Qué pasaría si no estuviera: No se podrían registrar nuevas vacunas aplicadas a los animales de compañía.
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Qué hace: Valida la sesión del usuario.
        // Por qué existe: Evita que usuarios sin credenciales envíen peticiones de creación.
        // Qué pasaría si no estuviera: Cualquier persona podría agregar datos erróneos al historial sanitario de las mascotas.
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
            
            // Qué hace: Guarda el DTO de la vacuna mediante el servicio de mascotas.
            // y luego de esto pasamos a MascotaServicio.crearVacuna, que persiste la vacuna en MySQL.
            String resJson = servicio.crearVacuna(dto);
            response.getWriter().write(resJson);
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al crear vacuna: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Atiende peticiones PATCH para actualizar la información de una vacuna específica.
    // Por qué existe: Soporta la modificación de dosis, nombres o fechas del historial sanitario del animal.
    // Qué pasaría si no estuviera: Si el voluntario comete un error al escribir el nombre o la fecha, no podría editarlo y tendría que borrar la vacuna por completo.
    protected void doPatch(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Qué hace: Valida la sesión del usuario.
        // Por qué existe: Asegura que solo usuarios logueados alteren los registros.
        // Qué pasaría si no estuviera: Usuarios anónimos podrían desordenar los datos del historial vacunal de las mascotas.
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
            
            // Qué hace: Actualiza la vacuna en el servicio de negocio.
            // y luego de esto pasamos a MascotaServicio.actualizarVacuna, el cual modifica el registro correspondiente en la base de datos.
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
    // Qué pasaría si no estuviera: Las vacunas mal ingresadas o duplicadas quedarían guardadas para siempre, ensuciando la base de datos.
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Qué hace: Valida la sesión del usuario.
        // Por qué existe: Protege contra eliminaciones malintencionadas o no autorizadas.
        // Qué pasaría si no estuviera: Cualquiera podría invocar DELETE en los endpoints sanitarios y borrar registros médicos reales.
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
            
            // Qué hace: Elimina la vacuna de la base de datos a través del servicio.
            // y luego de esto pasamos a MascotaServicio.eliminarVacuna, que remueve el registro físico de la base de datos.
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
