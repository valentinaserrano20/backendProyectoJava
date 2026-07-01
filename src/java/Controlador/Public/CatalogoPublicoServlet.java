package Controlador.Public;

/*
 * Qué hace (la acción): Importa la capa DAO para consulta de catálogos y mascotas, clases DTO de datos maestros y utilidades de respuesta web y JSON.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - Modelo.DAO.CatalogoDAO / MascotaDAO: Clases que ejecutan sentencias SQL en base de datos para cargar opciones paramétricas y datos de mascotas.
 *   - Modelo.DTO.*: Data Transfer Objects que representan registros lógicos de catálogos en memoria.
 *   - Modelo.Utilidades.ResponseUtil: Clase auxiliar para formatear la respuesta del servidor en un objeto JSON estándar de éxito o error.
 * Para qué se usa (el propósito): Proveer las dependencias necesarias para que el servlet recupere información paramétrica y la prepare para el cliente web.
 * Por qué es importante (el impacto o problema que resuelve): Permite desvincular la capa de controlador de la interacción directa con SQL, estructurando los catálogos y formateando las respuestas JSON homogéneamente.
 */
import Modelo.DAO.CatalogoDAO;
import Modelo.DAO.MascotaDAO;
import Modelo.DTO.TipoDocumentoDTO;
import Modelo.DTO.GeneroDTO;
import Modelo.DTO.UbicacionDTO;
import Modelo.DTO.MascotaDTO;
import Modelo.DTO.TipoRecursoDTO;
import Modelo.Utilidades.ResponseUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/*
 * Qué hace (la acción): Asocia el servlet CatalogoPublicoServlet con una amplia lista de URLs de catálogos paramétricos del sistema.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - urlPatterns = {...}: Configura el enrutador de servlets para capturar múltiples llamadas relativas a tipos de documentos, géneros, amenazas, etc.
 * Para qué se usa (el propósito): Servir como el único controlador unificado para obtener todas las listas paramétricas estáticas y dinámicas que requiere la interfaz del voluntario.
 * Por qué es importante (el impacto o problema que resuelve): Evita crear un servlet por cada tabla de catálogo paramétrico (ej. un servlet para tipos de sangre, otro para géneros, etc.), reduciendo drásticamente la cantidad de archivos y simplificando el mantenimiento del backend.
 */
@WebServlet(urlPatterns = {
    "/api/documentTypes",
    "/api/genders",
    "/api/kinships",
    "/api/kinships/*",
    "/api/bloodGroups",
    "/api/nationalities",
    "/api/conditionTypes",
    "/api/species",
    "/api/animalGenders",
    "/api/animalGenders/*",
    "/api/tiposAmenaza",
    "/api/tiposAmenaza/*",
    "/api/vulnerabilidades",
    "/api/gradosVulnerabilidad",
    "/api/tiposRecurso"
})
public class CatalogoPublicoServlet extends HttpServlet {

    /*
     * Qué hace (la acción): Instancia de manera privada y constante la variable catalogoDAO.
     * Qué significa (conceptos, métodos, tipos involucrados): Instancia de CatalogoDAO para realizar consultas a la base de datos SQL.
     * Para qué se usa (el propósito): Llamar a los métodos de carga de registros dinámicos de base de datos de manera centralizada.
     */
    private final CatalogoDAO catalogoDAO = new CatalogoDAO();

    /*
     * Qué hace (la acción): Sobrescribe el método doGet para procesar solicitudes HTTP de tipo GET, evaluando el endpoint solicitado y respondiendo con la lista paramétrica correspondiente en formato JSON.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - request.getServletPath(): Retorna la ruta específica que disparó la llamada (ej. "/api/genders").
     *   - obtenerIdDesdePath(request.getPathInfo()): Método auxiliar que lee si se adjuntó un ID numérico a la URL (ej: "/api/species/5").
     * Para qué se usa (el propósito): Recuperar de la base de datos o generar de forma estática los conjuntos paramétricos que completan los selects y radio buttons en la UI del frontend.
     * Por qué es importante (el impacto o problema que resuelve): Resuelve la carga dinámica e inmediata de catálogos necesarios para los formularios (como registrar integrantes, afecciones, mascotas, etc.), manejando tanto la recuperación de listas completas como la consulta puntual de un elemento por ID.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        PrintWriter out = response.getWriter();
        String path = request.getServletPath();
        
        try {
            // Manejo de peticiones específicas por ID (GET /api/recurso/{id})
            Integer idFiltro = obtenerIdDesdePath(request.getPathInfo());
            if (idFiltro != null) {
                UbicacionDTO item = null;
                if (path.equals("/api/kinships")) {
                    item = buscarPorId(catalogoDAO.getParentescos(), idFiltro);
                } else if (path.equals("/api/species")) {
                    item = buscarPorId(catalogoDAO.getEspeciesMascota(), idFiltro);
                } else if (path.equals("/api/tiposAmenaza")) {
                    item = buscarPorId(catalogoDAO.getAmenazas(), idFiltro);
                }

                if (item != null) {
                    out.print(ResponseUtil.success(mapearUbicacion(item, true)));
                    return;
                }
            }

            // Manejo de peticiones especiales o colecciones
            JSONArray arr = new JSONArray();
            
            if (path.equals("/api/documentTypes")) {
                for (TipoDocumentoDTO doc : catalogoDAO.getTiposDocumento()) {
                    arr.put(new JSONObject()
                        .put("id", doc.getId())
                        .put("nombre", doc.getNombre())
                        .put("activo", doc.getActivo())
                        .put("acronym", doc.getSigla()));
                }
            } else if (path.equals("/api/genders")) {
                for (GeneroDTO gen : catalogoDAO.getGeneros()) {
                    arr.put(new JSONObject()
                        .put("id", gen.getId())
                        .put("nombre", gen.getNombre())
                        .put("activo", gen.getActivo()));
                }
            } else if (path.equals("/api/kinships")) {
                convertirUbicacionAJson(catalogoDAO.getParentescos(), arr, true);
            } else if (path.equals("/api/bloodGroups")) {
                convertirUbicacionAJson(catalogoDAO.getGruposSanguineos(), arr, false);
            } else if (path.equals("/api/nationalities")) {
                convertirUbicacionAJson(catalogoDAO.getNacionalidades(), arr, false);
            } else if (path.equals("/api/vulnerabilidades")) {
                convertirUbicacionAJson(catalogoDAO.getVulnerabilidades(), arr, false);
            } else if (path.equals("/api/species")) {
                convertirUbicacionAJson(catalogoDAO.getEspeciesMascota(), arr, true);
            } else if (path.equals("/api/tiposAmenaza")) {
                convertirUbicacionAJson(catalogoDAO.getAmenazas(), arr, true);
            } else if (path.equals("/api/tiposRecurso")) {
                for (TipoRecursoDTO tr : catalogoDAO.getTiposRecurso()) {
                    arr.put(new JSONObject()
                        .put("id", tr.getId())
                        .put("nombre", tr.getNombre())
                        .put("activo", tr.getActivo())
                        .put("service", tr.getServicio()));
                }
            } else if (path.equals("/api/conditionTypes")) {
                arr.put(crearCatalogoEstatico(1, "Enfermedad"));
                arr.put(crearCatalogoEstatico(2, "Discapacidad"));
                arr.put(crearCatalogoEstatico(3, "Alergia"));
            } else if (path.equals("/api/gradosVulnerabilidad")) {
                arr.put(crearCatalogoEstatico(1, "Muy Alta"));
                arr.put(crearCatalogoEstatico(2, "Alta"));
                arr.put(crearCatalogoEstatico(3, "Media"));
                arr.put(crearCatalogoEstatico(4, "Baja"));
            } else if (path.equals("/api/animalGenders")) {
                /*
                 * Qué hace (la acción): Evalúa si la subruta contiene "/pet/{id}" para retornar el género de una mascota específica directamente de la base de datos; de lo contrario, retorna la lista estática (Macho, Hembra).
                 * Qué significa (conceptos, métodos, tipos involucrados):
                 *   - new MascotaDAO().obtenerMascota(petId): Consulta a la base de datos la información de la mascota seleccionada.
                 * Para qué se usa (el propósito): Recuperar los detalles del género del animal para editar registros o mostrar su género biológico.
                 */
                String pathInfo = request.getPathInfo();
                if (pathInfo != null && pathInfo.startsWith("/pet/")) {
                    int petId = Integer.parseInt(pathInfo.substring(5));
                    MascotaDTO pet = new MascotaDAO().obtenerMascota(petId);
                    if (pet != null) {
                        out.print(ResponseUtil.success(new JSONObject()
                            .put("id", pet.getAnimalGenderId())
                            .put("name", pet.getAnimalGender())
                            .put("nombre", pet.getAnimalGender())));
                        return;
                    }
                }
                arr.put(crearCatalogoEstatico(1, "Macho"));
                arr.put(crearCatalogoEstatico(2, "Hembra"));
            }

            out.print(ResponseUtil.success(arr));
            
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(ResponseUtil.error("Error al procesar el catálogo: " + e.getMessage()));
        }
    }

    // =========================================================================
    // METODOS AUXILIARES DE SIMPLIFICACIÓN Y LEGIBILIDAD
    // =========================================================================

    /*
     * Qué hace (la acción): Lee e interpreta el parámetro PathInfo de la URL para extraer la llave numérica (ID) si estuviera presente.
     * Qué significa (conceptos, métodos, tipos involucrados): Parsea una subcadena de texto saltando la primera barra inclinada ("/").
     * Para qué se usa (el propósito): Identificar si se llamó a un recurso unitario (ej: "/5" de un total).
     * Por qué es importante (el impacto o problema que resuelve): Previene errores de parseo numérico capturando la excepción NumberFormatException si la ruta contuviera letras.
     */
    private Integer obtenerIdDesdePath(String pathInfo) {
        if (pathInfo != null && pathInfo.length() > 1 && !pathInfo.startsWith("/pet/")) {
            try {
                return Integer.parseInt(pathInfo.substring(1));
            } catch (NumberFormatException e) {
                // Ignorar si no es numérico
            }
        }
        return null;
    }

    /*
     * Qué hace (la acción): Busca un objeto de tipo UbicacionDTO dentro de una lista de elementos comparando el identificador numérico.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - lista.stream(): Convierte la lista en un flujo de elementos.
     *   - filter: Filtra los elementos según la coincidencia de ID.
     *   - findFirst().orElse(null): Retorna el primer elemento coincidente, o null si no existe.
     * Para qué se usa (el propósito): Recuperar rápidamente el DTO del elemento correspondiente al ID solicitado.
     */
    private UbicacionDTO buscarPorId(List<UbicacionDTO> lista, int id) {
        return lista.stream()
                .filter(item -> item.getId() == id)
                .findFirst()
                .orElse(null);
    }

    /*
     * Qué hace (la acción): Convierte un objeto UbicacionDTO a un JSONObject estructurado.
     * Qué significa (conceptos, métodos, tipos involucrados): Mapea campos como id, nombre y activo al formato JSON.
     * Para qué se usa (el propósito): Adaptar el modelo interno DTO de Java en un objeto compatible para el frontend.
     */
    private JSONObject mapearUbicacion(UbicacionDTO dto, boolean incluirName) {
        JSONObject obj = new JSONObject()
                .put("id", dto.getId())
                .put("nombre", dto.getNombre())
                .put("activo", 1);
        if (incluirName) {
            obj.put("name", dto.getNombre());
        }
        return obj;
    }

    /*
     * Qué hace (la acción): Itera una lista de objetos UbicacionDTO, los convierte a JSON y los inserta en un JSONArray de destino.
     * Qué significa (conceptos, métodos, tipos involucrados): Bucle que procesa la conversión en masa de una lista.
     * Para qué se usa (el propósito): Compilar colecciones completas para retornarlas al frontend.
     */
    private void convertirUbicacionAJson(List<UbicacionDTO> lista, JSONArray destino, boolean incluirName) {
        for (UbicacionDTO item : lista) {
            destino.put(mapearUbicacion(item, incluirName));
        }
    }

    /*
     * Qué hace (la acción): Instancia y retorna un JSONObject representando una opción de catálogo estático de forma sencilla.
     * Qué significa (conceptos, métodos, tipos involucrados): JSONObject personalizado con llaves estáticas básicas.
     * Para qué se usa (el propósito): Generar conjuntos de datos fijos que no requieren almacenamiento en base de datos (como Masculino/Femenino o Grados de vulnerabilidad).
     */
    private JSONObject crearCatalogoEstatico(int id, String nombre) {
        return new JSONObject()
                .put("id", id)
                .put("nombre", nombre)
                .put("name", nombre)
                .put("activo", 1);
    }
}
