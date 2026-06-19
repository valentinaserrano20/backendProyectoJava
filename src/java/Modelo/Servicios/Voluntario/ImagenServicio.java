package Modelo.Servicios.Voluntario;

import Modelo.DAO.ImagenDAO;
import Modelo.DTO.ImagenDTO;
import jakarta.servlet.http.Part;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

// Qué hace: Capa de servicio encargada de controlar la lógica de negocio, subida física de archivos y mapear las respuestas en JSON para las imágenes del plan familiar.
// Por qué existe: Actúa como capa de abstracción intermedia entre los controladores (servlets) y la persistencia de datos (DAO).
// Qué problema resuelve: Centraliza la validación de archivos (tamaño, tipo), orquesta la eliminación física de archivos en el disco del servidor y estructura las respuestas JSON según el contrato de la SPA.
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
        JSONObject res = new JSONObject();
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

            res.put("success", true);
            res.put("data", dataArr);
            res.put("paginate", paginate);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al listar gráficos de vivienda: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Obtiene la información detallada de una imagen específica por su ID y la formatea en JSON.
    // Por qué existe: Alimenta el modal de previsualización en grande y el formulario de edición de descripción.
    // Qué pasaría si no estuviera: El voluntario no podría ver el croquis detallado ni editar su pie de foto en el frontend.
    public String obtenerImagen(int id) {
        JSONObject res = new JSONObject();
        try {
            // Qué hace: Consulta una imagen por su ID.
            // y luego de esto pasamos a ImagenDAO.obtenerPorId, que lee los datos desde MySQL.
            ImagenDTO img = dao.obtenerPorId(id);
            if (img == null) {
                return res.put("success", false).put("message", "Gráfico no encontrado.").toString();
            }

            JSONObject obj = new JSONObject();
            obj.put("id", img.getId());
            obj.put("tipo_grafico", img.getTipoGrafico());
            obj.put("path", img.getPath());
            obj.put("description", img.getDescription());
            obj.put("family_plan_id", img.getPlanId());

            res.put("success", true);
            res.put("data", obj);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al obtener gráfico: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Obtiene la información detallada de la imagen única de entorno o georreferenciación vinculada al plan familiar.
    // Por qué existe: Permite cargar la foto actual en las vistas correspondientes de la SPA.
    // Qué pasaría si no estuviera: La SPA no podría mostrar la imagen del mapa de entorno o de evacuación de la zona al cargar la pestaña correspondiente.
    public String obtenerImagenPorPlanYTipo(int planId, String tipo) {
        JSONObject res = new JSONObject();
        try {
            // Qué hace: Consulta un gráfico específico por su tipo y plan.
            // y luego de esto pasamos a ImagenDAO.obtenerPorPlanYTipo, el cual ejecuta la consulta en base de datos.
            ImagenDTO img = dao.obtenerPorPlanYTipo(planId, tipo);
            if (img == null) {
                // Para simplificar la validación en el frontend, retornamos un éxito vacío
                res.put("success", true);
                res.put("data", JSONObject.NULL);
                return res.toString();
            }

            JSONObject obj = new JSONObject();
            obj.put("id", img.getId());
            obj.put("tipo_grafico", img.getTipoGrafico());
            obj.put("path", img.getPath());
            obj.put("description", img.getDescription());
            obj.put("family_plan_id", img.getPlanId());

            res.put("success", true);
            res.put("data", obj);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al obtener gráfico del plan: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Procesa la subida física de una nueva imagen de vivienda al disco y registra su descripción en la base de datos.
    // Por qué existe: Atiende el envío multipart del formulario para añadir múltiples croquis de la casa.
    // Qué pasaría si no estuviera: Los archivos subidos no se guardarían físicamente en el servidor ni estarían indexados en la base de datos relacional.
    public String subirImagenVivienda(int planId, String descripcion, Part filePart, String contextPath) {
        JSONObject res = new JSONObject();
        try {
            String validacion = validarArchivo(filePart);
            if (validacion != null) {
                return res.put("success", false).put("message", validacion).toString();
            }

            // Qué hace: Escribe el stream binario de la imagen subida en el almacenamiento del servidor Tomcat.
            String relativePath = guardarArchivoEnDisco(planId, filePart, contextPath);

            ImagenDTO dto = new ImagenDTO();
            dto.setTipoGrafico("vivienda");
            dto.setPath(relativePath);
            dto.setDescription(descripcion);
            dto.setPlanId(planId);

            // Qué hace: Registra el nuevo croquis en la base de datos.
            // y luego de esto pasamos a ImagenDAO.crear, que inserta la ruta y descripción en MySQL.
            int newId = dao.crear(dto);
            JSONObject data = new JSONObject();
            data.put("id", newId);
            data.put("path", relativePath);

            res.put("success", true);
            res.put("message", "Gráfico de vivienda subido correctamente.");
            res.put("data", data);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al subir gráfico de vivienda: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Guarda en el disco del servidor Tomcat y registra o reemplaza la imagen única de entorno o georreferenciación.
    // Por qué existe: Asegura que solo exista un registro de entorno y mapa a la vez por plan familiar (eliminando la foto física anterior).
    // Qué pasaría si no estuviera: Quedarían croquis antiguos huérfanos ocupando espacio en el disco duro del servidor indefinidamente.
    public String subirOReemplazarImagenUnica(int planId, String tipo, Part filePart, String contextPath) {
        JSONObject res = new JSONObject();
        try {
            String validacion = validarArchivo(filePart);
            if (validacion != null) {
                return res.put("success", false).put("message", validacion).toString();
            }

            // Qué hace: Busca si ya hay un gráfico previo de este tipo registrado en el plan familiar.
            // y luego de esto pasamos a ImagenDAO.obtenerPorPlanYTipo, que consulta la fila anterior.
            ImagenDTO anterior = dao.obtenerPorPlanYTipo(planId, tipo);

            if (anterior != null) {
                // Qué hace: Borra físicamente la foto anterior en el disco del servidor.
                eliminarArchivoFisico(anterior.getPath(), contextPath);
            }

            // Qué hace: Guarda el nuevo archivo físico en el servidor.
            String relativePath = guardarArchivoEnDisco(planId, filePart, contextPath);

            if (anterior != null) {
                // Qué hace: Actualiza la ruta del archivo en base de datos.
                // y luego de esto pasamos a ImagenDAO.actualizarRuta para guardar los cambios en MySQL.
                dao.actualizarRuta(anterior.getId(), relativePath);
                
                JSONObject data = new JSONObject();
                data.put("id", anterior.getId());
                data.put("path", relativePath);

                res.put("success", true);
                res.put("message", "Gráfico reemplazado correctamente.");
                res.put("data", data);
            } else {
                ImagenDTO dto = new ImagenDTO();
                dto.setTipoGrafico(tipo);
                dto.setPath(relativePath);
                dto.setDescription(""); // Sin descripción requerida para entorno o mapa
                dto.setPlanId(planId);

                // Qué hace: Crea el registro inicial en la base de datos.
                // y luego de esto pasamos a ImagenDAO.crear, el cual realiza el INSERT correspondiente en MySQL.
                int newId = dao.crear(dto);
                JSONObject data = new JSONObject();
                data.put("id", newId);
                data.put("path", relativePath);

                res.put("success", true);
                res.put("message", "Gráfico guardado correctamente.");
                res.put("data", data);
            }
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al procesar gráfico único: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Valida y actualiza la descripción de un gráfico de vivienda por su ID.
    // Por qué existe: Atiende la petición de edición parcial del voluntario desde el frontend.
    // Qué pasaría si no estuviera: No se podría agregar ni editar un texto descriptivo del croquis para documentar la zona segura.
    public String actualizarDescripcion(int id, String descripcion) {
        JSONObject res = new JSONObject();
        try {
            // Qué hace: Actualiza la descripción en la base de datos.
            // y luego de esto pasamos a ImagenDAO.actualizarDescripcion, que ejecuta el comando UPDATE.
            dao.actualizarDescripcion(id, descripcion);
            res.put("success", true);
            res.put("message", "Descripción del gráfico actualizada correctamente.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al actualizar descripción del gráfico: " + e.getMessage());
        }
        return res.toString();
    }

    // Qué hace: Elimina una imagen físicamente del disco y borra su registro en la base de datos por su ID.
    // Por qué existe: Permite dar de baja total a los croquis de vivienda.
    // Qué pasaría si no estuviera: Las imágenes borradas quedarían como basura en el servidor Tomcat o con referencias rotas en MySQL.
    public String eliminarImagen(int id, String contextPath) {
        JSONObject res = new JSONObject();
        try {
            // Qué hace: Recupera la información del croquis para conocer la ruta de su archivo.
            // y luego de esto pasamos a ImagenDAO.obtenerPorId para consultar la base de datos.
            ImagenDTO img = dao.obtenerPorId(id);
            if (img == null) {
                return res.put("success", false).put("message", "El gráfico que intenta eliminar no existe.").toString();
            }

            // Qué hace: Elimina la foto del disco físicamente.
            eliminarArchivoFisico(img.getPath(), contextPath);

            // Qué hace: Elimina el registro del gráfico en la base de datos.
            // y luego de esto pasamos a ImagenDAO.eliminar, que corre la sentencia DELETE.
            dao.eliminar(id);

            res.put("success", true);
            res.put("message", "Gráfico eliminado correctamente.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error al eliminar el gráfico: " + e.getMessage());
        }
        return res.toString();
    }

    // =========================================================================
    // METODOS AUXILIARES PRIVADOS PARA GESTIÓN DE ARCHIVOS FÍSICOS
    // =========================================================================

    // Qué hace: Valida que la parte multipart del archivo cumpla con los requisitos del sistema.
    // Por qué existe: Asegura que no se suban archivos de formatos no admitidos o que pesen más de 2MB.
    // Qué pasaría si no estuviera: Se podrían subir archivos maliciosos (.exe, .sh) o sumamente grandes que colapsen el almacenamiento.
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

    // Qué hace: Escribe el stream binario de la imagen subida en el almacenamiento del servidor Tomcat.
    // Por qué existe: Guarda de manera física la fotografía en el disco local dentro del contexto del despliegue del proyecto.
    // Qué pasaría si no estuviera: No tendríamos forma de almacenar archivos físicos, imposibilitando guardar los croquis.
    private String guardarArchivoEnDisco(int planId, Part part, String contextPath) throws Exception {
        String originalName = part.getSubmittedFileName();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        
        // Genera un nombre aleatorio único
        String randomFileName = UUID.randomUUID().toString() + ext;
        
        // Carpeta destino física de almacenamiento del servidor
        String subPath = "uploads" + File.separator + "planes" + File.separator + planId;
        String absoluteDirectoryPath = contextPath + File.separator + subPath;
        
        File dir = new File(absoluteDirectoryPath);
        if (!dir.exists()) {
            dir.mkdirs(); // Crea la jerarquía de carpetas si no existían previamente
        }
        
        File fileOnServer = new File(dir, randomFileName);
        
        // Escribe el archivo en disco copiando su InputStream de red
        try (InputStream input = part.getInputStream()) {
            Files.copy(input, fileOnServer.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        
        // Retorna la ruta relativa normalizada con barras inclinadas '/' para compatibilidad con navegadores y SPA
        return "uploads/planes/" + planId + "/" + randomFileName;
    }

    // Qué hace: Borra físicamente un archivo ubicado en el disco del servidor.
    // Por qué existe: Libera espacio en disco cuando se elimina un registro de croquis o se reemplaza una imagen.
    // Qué pasaría si no estuviera: Se acumularían archivos obsoletos en el disco del servidor sin que nadie pueda borrarlos.
    private void eliminarArchivoFisico(String relativePath, String contextPath) {
        try {
            if (relativePath == null || relativePath.isEmpty()) return;
            
            // Reconstruye la ruta absoluta
            String systemPath = relativePath.replace("/", File.separator);
            String absolutePath = contextPath + File.separator + systemPath;
            
            File file = new File(absolutePath);
            if (file.exists() && file.isFile()) {
                file.delete(); // Elimina el archivo
            }
        } catch (Exception e) {
            System.err.println("Advertencia al eliminar archivo físico: " + e.getMessage());
        }
    }
}
