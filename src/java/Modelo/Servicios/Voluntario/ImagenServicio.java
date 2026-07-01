package Modelo.Servicios.Voluntario;

import Modelo.DAO.ImagenDAO;
import Modelo.DTO.ImagenDTO;
import Modelo.Utilidades.ResponseUtil;
import jakarta.servlet.http.Part;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Qué hace: Capa de servicio encargada de controlar la lógica de negocio, subida física de archivos y mapear las respuestas en JSON para las imágenes del plan familiar.
 * Por qué existe: Actúa como capa de abstracción intermedia entre los controladores (servlets) y la persistencia de datos (DAO).
 */
public class ImagenServicio {
    // Qué hace: Instancia el objeto de acceso a datos para las imágenes o croquis.
    // Por qué existe: Permite almacenar en la base de datos la ubicación relativa (ruta) de las imágenes subidas por el voluntario.
    // Qué pasaría si no estuviera: No podríamos registrar los croquis de vivienda, mapas de entorno o planos de evacuación en MySQL.
    // Flujo: De aquí pasamos a ImagenDAO.
    private final ImagenDAO dao = new ImagenDAO();

    // Qué hace: Lista croquis de la vivienda de forma paginada y estructura el JSON para el frontend.
    // Por qué existe: Suministra los datos para renderizar la galería de gráficos de la vivienda en la vista de la SPA.
    // Qué pasaría si no estuviera: La SPA no podría listar ni mostrar las miniaturas de los croquis de evacuación de la casa.
    public String listarImagenesVivienda(int planId, int page) {
        try {
            int limit = 6; // Límite de croquis por página en el listado
            int offset = (page - 1) * limit;
            
            // Qué hace: Cuenta el total de croquis registrados para este plan.
            // y luego de esto pasamos a ImagenDAO.contarPorPlanYTipo, el cual hace un SELECT COUNT en MySQL.
            int total = dao.contarPorPlanYTipo(planId, "vivienda");
            
            // Qué hace: Obtiene la lista paginada de croquis.
            // y luego de esto pasamos a ImagenDAO.listarPorPlanYTipo, que ejecuta el query de consulta.
            List<ImagenDTO> list = dao.listarPorPlanYTipo(planId, "vivienda", limit, offset);

            JSONArray dataArr = new JSONArray();
            for (ImagenDTO img : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", img.getId());
                obj.put("tipo_grafico", img.getTipoGrafico());
                obj.put("path", img.getPath());
                obj.put("description", img.getDescription());
                obj.put("family_plan_id", img.getPlanId());
                dataArr.put(obj);
            }

            JSONObject paginate = new JSONObject();
            paginate.put("total", total);
            paginate.put("per_page", limit);
            paginate.put("current_page", page);
            paginate.put("last_page", (int) Math.ceil((double) total / limit));

            return ResponseUtil.paginate(dataArr, paginate);
        } catch (Exception e) {
            return ResponseUtil.error("Error al listar gráficos de vivienda: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Obtiene la información detallada de una imagen específica por su ID y la formatea en JSON.
     */
    public String obtenerImagen(int id) {
        try {
            ImagenDTO img = dao.obtenerPorId(id);
            if (img == null) {
                return ResponseUtil.error("Gráfico no encontrado.");
            }

            JSONObject obj = new JSONObject();
            obj.put("id", img.getId());
            obj.put("tipo_grafico", img.getTipoGrafico());
            obj.put("path", img.getPath());
            obj.put("description", img.getDescription());
            obj.put("family_plan_id", img.getPlanId());

            return ResponseUtil.success(obj);
        } catch (Exception e) {
            return ResponseUtil.error("Error al obtener gráfico: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Obtiene la información detallada de la imagen única de entorno o georreferenciación vinculada al plan familiar.
     */
    public String obtenerImagenPorPlanYTipo(int planId, String tipo) {
        try {
            ImagenDTO img = dao.obtenerPorPlanYTipo(planId, tipo);
            if (img == null) {
                // Para simplificar la validación en el frontend, retornamos un éxito vacío
                return ResponseUtil.success(JSONObject.NULL);
            }

            JSONObject obj = new JSONObject();
            obj.put("id", img.getId());
            obj.put("tipo_grafico", img.getTipoGrafico());
            obj.put("path", img.getPath());
            obj.put("description", img.getDescription());
            obj.put("family_plan_id", img.getPlanId());

            return ResponseUtil.success(obj);
        } catch (Exception e) {
            return ResponseUtil.error("Error al obtener gráfico del plan: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Procesa la subida física de una nueva imagen de vivienda al disco y registra su descripción en la base de datos.
     */
    public String subirImagenVivienda(int planId, String descripcion, Part filePart, String contextPath) {
        try {
            String validacion = validarArchivo(filePart);
            if (validacion != null) {
                return ResponseUtil.error(validacion);
            }

            String relativePath = guardarArchivoEnDisco(planId, filePart, contextPath);

            ImagenDTO dto = new ImagenDTO();
            dto.setTipoGrafico("vivienda");
            dto.setPath(relativePath);
            dto.setDescription(descripcion);
            dto.setPlanId(planId);

            int newId = dao.crear(dto);
            JSONObject data = new JSONObject();
            data.put("id", newId);
            data.put("path", relativePath);

            return ResponseUtil.success("Gráfico de vivienda subido correctamente.", data);
        } catch (Exception e) {
            return ResponseUtil.error("Error al subir gráfico de vivienda: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Guarda en el disco del servidor Tomcat y registra o reemplaza la imagen única de entorno o georreferenciación.
     */
    public String subirOReemplazarImagenUnica(int planId, String tipo, Part filePart, String contextPath) {
        try {
            String validacion = validarArchivo(filePart);
            if (validacion != null) {
                return ResponseUtil.error(validacion);
            }

            ImagenDTO anterior = dao.obtenerPorPlanYTipo(planId, tipo);

            if (anterior != null) {
                eliminarArchivoFisico(anterior.getPath(), contextPath);
            }

            String relativePath = guardarArchivoEnDisco(planId, filePart, contextPath);

            if (anterior != null) {
                dao.actualizarRuta(anterior.getId(), relativePath);
                
                JSONObject data = new JSONObject();
                data.put("id", anterior.getId());
                data.put("path", relativePath);

                return ResponseUtil.success("Gráfico reemplazado correctamente.", data);
            } else {
                ImagenDTO dto = new ImagenDTO();
                dto.setTipoGrafico(tipo);
                dto.setPath(relativePath);
                dto.setDescription(""); // Sin descripción requerida para entorno o mapa
                dto.setPlanId(planId);

                int newId = dao.crear(dto);
                JSONObject data = new JSONObject();
                data.put("id", newId);
                data.put("path", relativePath);

                return ResponseUtil.success("Gráfico guardado correctamente.", data);
            }
        } catch (Exception e) {
            return ResponseUtil.error("Error al procesar gráfico único: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Valida y actualiza la descripción de un gráfico de vivienda por su ID.
     */
    public String actualizarDescripcion(int id, String descripcion) {
        try {
            dao.actualizarDescripcion(id, descripcion);
            return ResponseUtil.success("Descripción del gráfico actualizada correctamente.");
        } catch (Exception e) {
            return ResponseUtil.error("Error al actualizar descripción del gráfico: " + e.getMessage());
        }
    }

    /**
     * Qué hace: Elimina una imagen físicamente del disco y borra su registro en la base de datos por su ID.
     */
    public String eliminarImagen(int id, String contextPath) {
        try {
            ImagenDTO img = dao.obtenerPorId(id);
            if (img == null) {
                return ResponseUtil.error("El gráfico que intenta eliminar no existe.");
            }

            eliminarArchivoFisico(img.getPath(), contextPath);
            dao.eliminar(id);

            return ResponseUtil.success("Gráfico eliminado correctamente.");
        } catch (Exception e) {
            return ResponseUtil.error("Error al eliminar el gráfico: " + e.getMessage());
        }
    }

    // =========================================================================
    // METODOS AUXILIARES PRIVADOS PARA GESTIÓN DE ARCHIVOS FÍSICOS
    // =========================================================================

    private String validarArchivo(Part part) {
        if (part == null || part.getSize() == 0) {
            return "No se ha seleccionado ningún archivo de imagen.";
        }

        // Límite de tamaño estricto de 2 MB
        long maxBytes = 2 * 1024 * 1024;
        if (part.getSize() > maxBytes) {
            return "El archivo supera el tamaño máximo permitido de 2MB.";
        }

        String contentType = part.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && 
                                    !contentType.equals("image/png") && 
                                    !contentType.equals("image/webp"))) {
            return "El formato de archivo no está permitido. Solo se admite JPG, PNG y WEBP.";
        }

        return null;
    }

    private String guardarArchivoEnDisco(int planId, Part part, String contextPath) throws Exception {
        String originalName = part.getSubmittedFileName();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        
        String randomFileName = UUID.randomUUID().toString() + ext;
        
        String subPath = "uploads" + File.separator + "planes" + File.separator + planId;
        String absoluteDirectoryPath = contextPath + File.separator + subPath;
        
        File dir = new File(absoluteDirectoryPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        File fileOnServer = new File(dir, randomFileName);
        
        try (InputStream input = part.getInputStream()) {
            Files.copy(input, fileOnServer.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        
        return "uploads/planes/" + planId + "/" + randomFileName;
    }

    private void eliminarArchivoFisico(String relativePath, String contextPath) {
        try {
            if (relativePath == null || relativePath.isEmpty()) return;
            
            String systemPath = relativePath.replace("/", File.separator);
            String absolutePath = contextPath + File.separator + systemPath;
            
            File file = new File(absolutePath);
            if (file.exists() && file.isFile()) {
                file.delete();
            }
        } catch (Exception e) {
            System.err.println("Advertencia al eliminar archivo físico: " + e.getMessage());
        }
    }
}
