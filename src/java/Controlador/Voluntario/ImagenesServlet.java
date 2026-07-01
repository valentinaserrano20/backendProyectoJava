package Controlador.Voluntario;

/*
 * Qué hace (la acción): Importa la clase del servicio de imágenes, utilidades de JSON y respuestas web, APIs de servlets de Jakarta y la clase Part para procesar archivos multipart.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.Servicios.Voluntario.ImagenServicio: Servicio de negocio que realiza validaciones físicas de archivos, subidas al disco, reemplazo y borrado de croquis.
 *   - jakarta.servlet.http.Part: Interfaz que representa una parte de datos o archivo recibido en una petición multipart/form-data.
 * Para qué se usa (el propósito): Proveer al servlet las dependencias requeridas para procesar y almacenar imágenes en el servidor.
 * Por qué es importante (el impacto o problema que resuelve): Sin estas importaciones, no se podrían recuperar los bytes del croquis de vivienda ni enrutar la lógica de almacenamiento en el sistema de archivos del servidor.
 */
import Modelo.Servicios.Voluntario.ImagenServicio;
import Modelo.Utilidades.JSONUtil;
import Modelo.Utilidades.ResponseUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.IOException;
import org.json.JSONObject;

/*
 * Qué hace (la acción): Mapea el servlet a "/api/imagenes/*" y configura los límites de carga de archivos binarios mediante las anotaciones @WebServlet y @MultipartConfig.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - @MultipartConfig: Anotación obligatoria para indicar que el servlet espera recibir formularios de tipo multipart (archivos adjuntos).
 *   - maxFileSize: Límite máximo de tamaño para un archivo individual (ej. 2 MB).
 *   - maxRequestSize: Límite máximo del cuerpo completo de la petición (ej. 4 MB).
 * Para qué se usa (el propósito): Controlar la subida y almacenamiento seguro de imágenes de la vivienda, croquis de entorno y puntos de georreferenciación.
 * Por qué es importante (el impacto o problema que resuelve): Permite que el contenedor Tomcat procese los flujos binarios de imágenes previniendo la inyección de archivos gigantescos que puedan desbordar o saturar el disco del servidor.
 */
@WebServlet("/api/imagenes/*")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 1, // 1 MB
    maxFileSize = 1024 * 1024 * 2,      // 2 MB
    maxRequestSize = 1024 * 1024 * 4    // 4 MB
)
public class ImagenesServlet extends HttpServlet {

    /*
     * Qué hace (la acción): Instancia de manera privada y constante la variable servicio de tipo ImagenServicio.
     * Qué significa (conceptos, métodos, tipos involucrados): Instancia de la clase de servicios de negocio para croquis e imágenes.
     * Para qué se usa (el propósito): Invocar los procesos de subida, obtención y eliminación física de croquis.
     */
    private final ImagenServicio servicio = new ImagenServicio();

    /*
     * Qué hace (la acción): Sobrescribe el método service para desviar las peticiones que utilizan el verbo HTTP PATCH hacia el método doPatch.
     * Qué significa (conceptos, métodos, tipos involucrados): Redirección manual de peticiones PATCH en servlets de Jakarta.
     * Para qué se usa (el propósito): Habilitar la edición parcial de la descripción de las imágenes de vivienda.
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
     * Qué hace (la acción): Sobrescribe el método doGet para procesar consultas de imágenes, permitiendo listar los croquis de vivienda de un plan familiar, obtener los detalles de una imagen de vivienda en específico o recuperar los croquis únicos de entorno y georreferenciación.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - parts[1].equals("vivienda") && parts[2].equals("planFamiliar"): Identifica la solicitud para listar las imágenes de vivienda adjuntas a un plan familiar.
     *   - parts[1].equals("entorno") / parts[1].equals("georeferenciacion"): Recupera las imágenes únicas del plan según la categoría de croquis de la familia.
     * Para qué se usa (el propósito): Cargar y renderizar los croquis y mapas en las tarjetas informativas del panel del voluntario o supervisor.
     * Por qué es importante (el impacto o problema que resuelve): Permite que el frontend cargue dinámicamente las rutas físicas de los croquis adjuntos al plan de emergencia familiar en tiempo real.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Ruta no especificada."));
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
            else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(ResponseUtil.error("Ruta de consulta no válida."));
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("El identificador de ruta debe ser numérico."));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al procesar la consulta: " + e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doPost para recibir el stream multipart de un croquis (vivienda, entorno o georreferenciación) y guardarlo en el disco físico del servidor vinculándolo en base de datos.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - request.getPart("path"): Recupera el fragmento binario de la petición que contiene el archivo de imagen.
     *   - request.getServletContext().getRealPath("/"): Obtiene la ruta física absoluta de la carpeta raíz de despliegue del servidor para escribir el archivo en disco.
     *   - subirOReemplazarImagenUnica(...): Lógica del servicio que comprueba si ya existía una imagen de entorno/georreferenciación previa, la borra físicamente y guarda la nueva para no duplicar espacio.
     * Para qué se usa (el propósito): Almacenar y catalogar en el backend los mapas de rutas de evacuación o fotos de riesgos del hogar que sube el voluntario.
     * Por qué es importante (el impacto o problema que resuelve): Resuelve la subida y descarte seguro de archivos físicos previniendo fugas de archivos huérfanos sin asociar en el servidor.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Ruta de envío no especificada."));
            return;
        }

        String[] parts = pathInfo.split("/");
        String tipo = parts[1]; // "vivienda", "entorno" o "georeferenciacion"

        try {
            String planIdStr = request.getParameter("family_plan_id");
            if (planIdStr == null || planIdStr.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(ResponseUtil.error("El ID del plan familiar es obligatorio."));
                return;
            }
            int planId = Integer.parseInt(planIdStr);
            
            Part filePart = request.getPart("path"); 
            String contextPath = request.getServletContext().getRealPath("/");

            String jsonRes;
            if (tipo.equals("vivienda")) {
                String descripcion = request.getParameter("description");
                jsonRes = servicio.subirImagenVivienda(planId, descripcion, filePart, contextPath);
            } else if (tipo.equals("entorno") || tipo.equals("georeferenciacion")) {
                jsonRes = servicio.subirOReemplazarImagenUnica(planId, tipo, filePart, contextPath);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(ResponseUtil.error("Tipo de gráfico no admitido."));
                return;
            }

            response.getWriter().write(jsonRes);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al registrar la imagen: " + e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Define la lógica doPatch para actualizar la descripción explicativa de un gráfico de vivienda existente por su ID.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - parts[3].equals("description"): Valida que el endpointPATCH apunte a la edición de descripción del croquis.
     *   - servicio.actualizarDescripcion(id, descripcion): Actualiza el campo descripción en la base de datos SQL.
     * Para qué se usa (el propósito): Cambiar el texto descriptivo del gráfico (ej. "Croquis de la habitación principal").
     */
    protected void doPatch(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Ruta no válida."));
            return;
        }

        String[] parts = pathInfo.split("/");
        
        try {
            // Espera: /vivienda/{id}/description
            if (parts[1].equals("vivienda") && parts.length > 3 && parts[3].equals("description")) {
                int id = Integer.parseInt(parts[2]);
                JSONObject json = JSONUtil.leerJson(request);
                String descripcion = json.optString("description", "");

                String jsonRes = servicio.actualizarDescripcion(id, descripcion);
                response.getWriter().write(jsonRes);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(ResponseUtil.error("Operación PATCH no permitida en esta ruta."));
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("El ID de la imagen debe ser numérico."));
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("JSON mal formado: " + e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al actualizar descripción de imagen: " + e.getMessage()));
        }
    }

    /*
     * Qué hace (la acción): Sobrescribe el método doDelete para dar de baja y eliminar de manera física el archivo de croquis del disco del servidor y borrar su registro de base de datos.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - servicio.eliminarImagen(id, contextPath): Borra la fila de base de datos, localiza la ruta física del archivo subido en el disco del servidor Tomcat y lo borra usando java.io.File.delete().
     * Para qué se usa (el propósito): Permitir al voluntario borrar un croquis de vivienda obsoleto.
     * Por qué es importante (el impacto o problema que resuelve): Previene la acumulación de archivos inútiles o basura en el disco del servidor cuando el usuario descarta o rehace las imágenes de su vivienda.
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Ruta de eliminación no válida."));
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
                response.getWriter().write(ResponseUtil.error("Operación DELETE no permitida en esta ruta."));
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("El ID de la imagen debe ser numérico."));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(ResponseUtil.error("Error al eliminar gráfico: " + e.getMessage()));
        }
    }
}
