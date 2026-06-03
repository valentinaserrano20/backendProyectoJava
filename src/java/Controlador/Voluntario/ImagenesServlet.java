package Controlador.Voluntario;

import Modelo.Servicios.Voluntario.ImagenServicio;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import java.io.BufferedReader;
import java.io.IOException;
import org.json.JSONObject;

// Qué hace: Servlet controlador mapeado a /api/imagenes/* que procesa todas las solicitudes HTTP del CRUD de Imágenes y croquis.
// Por qué existe: Actúa como punto de entrada de la API para las operaciones de carga y visualización de imágenes del plan familiar en la SPA.
// Qué problema resuelve: Enruta las peticiones HTTP (GET, POST, PATCH, DELETE), valida la sesión activa y delega el procesamiento multipart o de texto al servicio.
@WebServlet("/api/imagenes/*")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 1, // 1 MB
    maxFileSize = 1024 * 1024 * 2,      // 2 MB
    maxRequestSize = 1024 * 1024 * 4    // 4 MB
)
public class ImagenesServlet extends HttpServlet {
    private final ImagenServicio servicio = new ImagenServicio();

    // Qué hace: Captura las peticiones HTTP e intercepta las de tipo PATCH para redirigirlas al método doPatch no nativo.
    // Por qué existe: Servlets nativos de Java no soportan doPatch por defecto de forma automática en la herencia de HttpServlet.
    // Qué problema resuelve: Habilita el soporte completo para peticiones tipo PATCH utilizadas por la SPA para actualizar la descripción del croquis.
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

    // Qué hace: Atiende peticiones HTTP GET para obtener las imágenes de vivienda, entorno o georreferenciación.
    // Por qué existe: Permite consultar y renderizar la información de los gráficos en las respectivas pantallas del voluntario y supervisor.
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
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Ruta no especificada.").toString());
            return;
        }

        String[] parts = pathInfo.split("/");
        
        try {
            // Caso 1: imagenes/vivienda/planFamiliar/{planId}
            if (parts[1].equals("vivienda") && parts.length > 2 && parts[2].equals("planFamiliar")) {
                int planId = Integer.parseInt(parts[3]);
                String pageParam = request.getParameter("page");
                int page = (pageParam != null) ? Integer.parseInt(pageParam) : 1;
                
                String jsonRes = servicio.listarImagenesVivienda(planId, page);
                response.getWriter().write(jsonRes);
            } 
            // Caso 2: imagenes/vivienda/{id}
            else if (parts[1].equals("vivienda") && parts.length == 3) {
                int id = Integer.parseInt(parts[2]);
                String jsonRes = servicio.obtenerImagen(id);
                response.getWriter().write(jsonRes);
            }
            // Caso 3: imagenes/entorno/planFamiliar/{planId}
            else if (parts[1].equals("entorno") && parts.length > 2 && parts[2].equals("planFamiliar")) {
                int planId = Integer.parseInt(parts[3]);
                String jsonRes = servicio.obtenerImagenPorPlanYTipo(planId, "entorno");
                response.getWriter().write(jsonRes);
            }
            // Caso 4: imagenes/georeferenciacion/planFamiliar/{planId}
            else if (parts[1].equals("georeferenciacion") && parts.length > 2 && parts[2].equals("planFamiliar")) {
                int planId = Integer.parseInt(parts[3]);
                String jsonRes = servicio.obtenerImagenPorPlanYTipo(planId, "georeferenciacion");
                response.getWriter().write(jsonRes);
            }
            // Ruta inválida
            else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(new JSONObject().put("success", false).put("message", "Ruta de consulta no válida.").toString());
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "El identificador de ruta debe ser numérico.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al procesar la consulta: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Recibe peticiones HTTP POST para procesar y almacenar archivos de imágenes multipart.
    // Por qué existe: Permite agregar nuevos croquis de vivienda o subir/sobrescribir imágenes de entorno y mapa.
    // Qué problema resuelve: Extrae los parámetros multipart (id del plan, descripción, archivo binario) y delega la subida al servicio.
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

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Ruta de envío no especificada.").toString());
            return;
        }

        String[] parts = pathInfo.split("/");
        String tipo = parts[1]; // "vivienda", "entorno" o "georeferenciacion"

        try {
            // Lee parámetros del formulario Multipart
            String planIdStr = request.getParameter("family_plan_id");
            if (planIdStr == null || planIdStr.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(new JSONObject().put("success", false).put("message", "El ID del plan familiar es obligatorio.").toString());
                return;
            }
            int planId = Integer.parseInt(planIdStr);
            Part filePart = request.getPart("path"); // Campo binario de la imagen
            String contextPath = request.getServletContext().getRealPath("/");

            String jsonRes;
            if (tipo.equals("vivienda")) {
                String descripcion = request.getParameter("description");
                jsonRes = servicio.subirImagenVivienda(planId, descripcion, filePart, contextPath);
            } else if (tipo.equals("entorno") || tipo.equals("georeferenciacion")) {
                jsonRes = servicio.subirOReemplazarImagenUnica(planId, tipo, filePart, contextPath);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(new JSONObject().put("success", false).put("message", "Tipo de gráfico no admitido.").toString());
                return;
            }

            response.getWriter().write(jsonRes);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al registrar la imagen: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Procesa solicitudes HTTP PATCH para actualizar la descripción de un gráfico de vivienda existente por su ID.
    // Por qué existe: Atiende la edición de la descripción del croquis.
    // Qué problema resuelve: Lee el cuerpo de la solicitud JSON, extrae la descripción nueva y ejecuta la actualización.
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
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Ruta no válida.").toString());
            return;
        }

        String[] parts = pathInfo.split("/");
        
        try {
            // Espera: /vivienda/{id}/description
            if (parts[1].equals("vivienda") && parts.length > 3 && parts[3].equals("description")) {
                int id = Integer.parseInt(parts[2]);
                
                StringBuilder buffer = new StringBuilder();
                String line;
                try (BufferedReader reader = request.getReader()) {
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line);
                    }
                }

                JSONObject json = new JSONObject(buffer.toString());
                String descripcion = json.optString("description", "");

                String jsonRes = servicio.actualizarDescripcion(id, descripcion);
                response.getWriter().write(jsonRes);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(new JSONObject().put("success", false).put("message", "Operación PATCH no permitida en esta ruta.").toString());
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "El ID de la imagen debe ser numérico.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al actualizar descripción de imagen: " + e.getMessage()).toString());
        }
    }

    // Qué hace: Procesa solicitudes HTTP DELETE para dar de baja un croquis de vivienda por su ID.
    // Por qué existe: Habilita el botón de eliminar del listado de fotos de la vivienda.
    // Qué problema resuelve: Elimina tanto el archivo físico del disco como la referencia en base de datos.
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
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Ruta de eliminación no válida.").toString());
            return;
        }

        String[] parts = pathInfo.split("/");
        
        try {
            // Espera: /vivienda/{id}
            if (parts[1].equals("vivienda") && parts.length == 3) {
                int id = Integer.parseInt(parts[2]);
                String contextPath = request.getServletContext().getRealPath("/");
                
                String jsonRes = servicio.eliminarImagen(id, contextPath);
                response.getWriter().write(jsonRes);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(new JSONObject().put("success", false).put("message", "Operación DELETE no permitida en esta ruta.").toString());
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "El ID de la imagen debe ser numérico.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al eliminar gráfico: " + e.getMessage()).toString());
        }
    }
}
