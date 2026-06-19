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

/**
 * Qué hace: Servlet controlador mapeado a /api/imagenes/* que procesa todas las solicitudes HTTP del CRUD de Imágenes y croquis.
 * Por qué existe: Actúa como punto de entrada de la API para las operaciones de carga y visualización de imágenes del plan familiar en la SPA.
 * Qué pasaría si no estuviera: Los voluntarios no tendrían un endpoint para cargar mapas, croquis de evacuación o fotografías del estado de las viviendas.
 */
@WebServlet("/api/imagenes/*")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 1, // 1 MB
    maxFileSize = 1024 * 1024 * 2,      // 2 MB
    maxRequestSize = 1024 * 1024 * 4    // 4 MB
)
public class ImagenesServlet extends HttpServlet {

    // Qué hace: Instancia el servicio de lógica de negocios para el control de archivos y gráficos.
    // Por qué existe: Permite desacoplar el almacenamiento de imágenes del flujo directo de peticiones HTTP.
    // Qué pasaría si no estuviera: El controlador tendría que encargarse de escribir streams de bytes en el disco duro y consultar la BD.
    // Flujo: De aquí pasamos a ImagenServicio.
    private final ImagenServicio servicio = new ImagenServicio();

    // Qué hace: Captura las peticiones HTTP e intercepta las de tipo PATCH para redirigirlas al método doPatch no nativo.
    // Por qué existe: Servlets nativos de Java no soportan doPatch por defecto de forma automática en la herencia de HttpServlet.
    // Qué pasaría si no estuviera: Las peticiones tipo PATCH del frontend fallarían con código 405 (Method Not Allowed).
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
    // Qué pasaría si no estuviera: La SPA no podría cargar ni mostrar los croquis cargados previamente en el sistema.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Qué hace: Valida la sesión activa del voluntario.
        // Por qué existe: Bloquea accesos no autorizados a archivos geográficos y croquis internos de viviendas privadas.
        // Qué pasaría si no estuviera: Cualquiera en internet podría consultar y descargar mapas e imágenes de viviendas sin credenciales.
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
                
                // Qué hace: Obtiene la lista paginada de imágenes de la vivienda.
                // y luego de esto pasamos a ImagenServicio.listarImagenesVivienda, el cual lee la tabla de la BD.
                String jsonRes = servicio.listarImagenesVivienda(planId, page);
                response.getWriter().write(jsonRes);
            } 
            // Caso 2: imagenes/vivienda/{id}
            else if (parts[1].equals("vivienda") && parts.length == 3) {
                int id = Integer.parseInt(parts[2]);
                
                // Qué hace: Recupera una imagen de vivienda individual por su ID.
                // y luego de esto pasamos a ImagenServicio.obtenerImagen, el cual realiza el SELECT por clave primaria.
                String jsonRes = servicio.obtenerImagen(id);
                response.getWriter().write(jsonRes);
            }
            // Caso 3: imagenes/entorno/planFamiliar/{planId}
            else if (parts[1].equals("entorno") && parts.length > 2 && parts[2].equals("planFamiliar")) {
                int planId = Integer.parseInt(parts[3]);
                
                // Qué hace: Recupera la imagen única del croquis de entorno del plan.
                // y luego de esto pasamos a ImagenServicio.obtenerImagenPorPlanYTipo, el cual consulta la BD.
                String jsonRes = servicio.obtenerImagenPorPlanYTipo(planId, "entorno");
                response.getWriter().write(jsonRes);
            }
            // Caso 4: imagenes/georeferenciacion/planFamiliar/{planId}
            else if (parts[1].equals("georeferenciacion") && parts.length > 2 && parts[2].equals("planFamiliar")) {
                int planId = Integer.parseInt(parts[3]);
                
                // Qué hace: Recupera la imagen del croquis de georreferenciación.
                // y luego de esto pasamos a ImagenServicio.obtenerImagenPorPlanYTipo, el cual consulta la BD.
                String jsonRes = servicio.obtenerImagenPorPlanYTipo(planId, "georeferenciacion");
                response.getWriter().write(jsonRes);
            }
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
    // Qué pasaría si no estuviera: Sería imposible recibir archivos de imagen cargados desde los formularios de la UI.
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
            String planIdStr = request.getParameter("family_plan_id");
            if (planIdStr == null || planIdStr.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(new JSONObject().put("success", false).put("message", "El ID del plan familiar es obligatorio.").toString());
                return;
            }
            int planId = Integer.parseInt(planIdStr);
            
            // Qué hace: Extrae el archivo binario del request multipart mapeado al campo "path".
            Part filePart = request.getPart("path"); 
            // Qué hace: Resuelve la ruta absoluta del contexto web en el servidor Tomcat.
            String contextPath = request.getServletContext().getRealPath("/");

            String jsonRes;
            if (tipo.equals("vivienda")) {
                String descripcion = request.getParameter("description");
                
                // Qué hace: Llama al servicio para procesar y almacenar la imagen de croquis de vivienda.
                // y luego de esto pasamos a ImagenServicio.subirImagenVivienda, el cual escribe el archivo en el disco y guarda la fila en la BD.
                jsonRes = servicio.subirImagenVivienda(planId, descripcion, filePart, contextPath);
            } else if (tipo.equals("entorno") || tipo.equals("georeferenciacion")) {
                
                // Qué hace: Sube o sobrescribe la imagen única de entorno/mapa en el servidor.
                // y luego de esto pasamos a ImagenServicio.subirOReemplazarImagenUnica, el cual elimina la imagen anterior si existe y guarda la nueva.
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
    // Qué pasaría si no estuviera: No podríamos corregir o modificar la descripción o título asignado a un croquis de evacuación ya cargado.
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

                // Qué hace: Modifica la descripción asociada a la imagen.
                // y luego de esto pasamos a ImagenServicio.actualizarDescripcion, el cual ejecuta el UPDATE SQL por ID.
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
    // Qué pasaría si no estuviera: Las fotos y croquis obsoletos o erróneos seguirían almacenados permanentemente en el servidor, llenando el disco.
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
                
                // Qué hace: Llama a la remoción lógica y física del croquis.
                // y luego de esto pasamos a ImagenServicio.eliminarImagen, que borra el archivo del disco y elimina la fila de la BD.
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
