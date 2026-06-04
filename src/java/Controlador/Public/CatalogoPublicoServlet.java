package Controlador.Public;

import Modelo.DAO.CatalogoDAO;
import Modelo.DTO.TipoDocumentoDTO;
import Modelo.DTO.GeneroDTO;
import Modelo.DTO.UbicacionDTO;
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
import Modelo.DAO.MascotaDAO;
import Modelo.DTO.MascotaDTO;
import Modelo.DTO.TipoRecursoDTO;

// Qué hace: Servlet centralizado para exponer endpoints de catálogos públicos como tipos de documento, géneros, parentescos, grupos sanguíneos, nacionalidades y tipos de afección.
// Por qué existe: Concentra todas las consultas paramétricas solicitadas por la interfaz del voluntario en la creación y edición de integrantes.
// Qué problema resuelve: Evita la proliferación de múltiples servlets individuales pequeños, estructurando las respuestas en JSON compatibles con el frontend.
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
    private final CatalogoDAO catalogoDAO = new CatalogoDAO();

    // Qué hace: Recibe peticiones HTTP GET, identifica el recurso solicitado según la ruta de servicio y devuelve el catálogo serializado a formato JSON.
    // Por qué existe: Permite a los dropdowns de la interfaz poblar sus opciones dinámicamente con los registros activos de la base de datos.
    // Qué problema resuelve: Evita respuestas estáticas harcodeadas en el frontend, garantizando la sincronización con los datos paramétricos de la base de datos.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        String path = request.getServletPath();
        
        try {
            JSONArray arr = new JSONArray();
            
            if (path.equals("/api/documentTypes")) {
                List<TipoDocumentoDTO> list = catalogoDAO.getTiposDocumento();
                for (TipoDocumentoDTO doc : list) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", doc.getId());
                    obj.put("nombre", doc.getNombre());
                    obj.put("activo", doc.getActivo());
                    obj.put("acronym", doc.getSigla());
                    arr.put(obj);
                }
            } else if (path.equals("/api/genders")) {
                List<GeneroDTO> list = catalogoDAO.getGeneros();
                for (GeneroDTO gen : list) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", gen.getId());
                    obj.put("nombre", gen.getNombre());
                    obj.put("activo", gen.getActivo());
                    arr.put(obj);
                }
            } else if (path.equals("/api/kinships")) {
                // Qué hace: Verifica si la solicitud contiene un ID específico en el pathInfo para retornar un parentesco único.
                // Por qué existe: El RevisionPlanController.js del supervisor consume /api/kinships/{id} para ver detalles del integrante.
                // Qué problema resuelve: Devuelve un objeto individual con soporte de localización para evitar errores 404 e indefinidos.
                String pathInfo = request.getPathInfo();
                if (pathInfo != null && pathInfo.length() > 1) {
                    try {
                        int id = Integer.parseInt(pathInfo.substring(1));
                        UbicacionDTO kin = null;
                        List<UbicacionDTO> list = catalogoDAO.getParentescos();
                        for (UbicacionDTO k : list) {
                            if (k.getId() == id) {
                                kin = k;
                                break;
                            }
                        }
                        if (kin != null) {
                            JSONObject obj = new JSONObject();
                            obj.put("id", kin.getId());
                            obj.put("nombre", kin.getNombre());
                            obj.put("name", kin.getNombre()); // Inyecta name para soporte de frontend
                            obj.put("activo", 1);
                            
                            JSONObject res = new JSONObject();
                            res.put("success", true);
                            res.put("data", obj);
                            out.print(res.toString());
                            return;
                        }
                    } catch (NumberFormatException e) {
                        // Ignora errores de parsing de URL no numéricos
                    }
                }

                // Qué hace: Retorna la lista completa de parentescos con soporte dual de claves nombre/name.
                // Por qué existe: Asegura que tanto los dropdowns de creación como la vista de lectura obtengan los parentescos.
                // Qué problema resuelve: Homologa los campos de respuesta con la SPA.
                List<UbicacionDTO> list = catalogoDAO.getParentescos();
                for (UbicacionDTO kin : list) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", kin.getId());
                    obj.put("nombre", kin.getNombre());
                    obj.put("name", kin.getNombre()); // Inyecta name para compatibilidad de la SPA
                    obj.put("activo", 1);
                    arr.put(obj);
                }
            } else if (path.equals("/api/bloodGroups")) {
                List<UbicacionDTO> list = catalogoDAO.getGruposSanguineos();
                for (UbicacionDTO bg : list) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", bg.getId());
                    obj.put("nombre", bg.getNombre());
                    obj.put("activo", 1);
                    arr.put(obj);
                }
            } else if (path.equals("/api/nationalities")) {
                List<UbicacionDTO> list = catalogoDAO.getNacionalidades();
                for (UbicacionDTO nat : list) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", nat.getId());
                    obj.put("nombre", nat.getNombre());
                    obj.put("activo", 1);
                    arr.put(obj);
                }
            } else if (path.equals("/api/conditionTypes")) {
                JSONObject type1 = new JSONObject().put("id", 1).put("name", "Enfermedad").put("nombre", "Enfermedad").put("activo", 1);
                JSONObject type2 = new JSONObject().put("id", 2).put("name", "Discapacidad").put("nombre", "Discapacidad").put("activo", 1);
                JSONObject type3 = new JSONObject().put("id", 3).put("name", "Alergia").put("nombre", "Alergia").put("activo", 1);
                arr.put(type1);
                arr.put(type2);
                arr.put(type3);
            } else if (path.equals("/api/tiposRecurso")) {
                List<TipoRecursoDTO> list = catalogoDAO.getTiposRecurso();
                for (TipoRecursoDTO tr : list) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", tr.getId());
                    obj.put("nombre", tr.getNombre());
                    obj.put("activo", tr.getActivo());
                    obj.put("service", tr.getServicio()); // Autocompleta el servicio en la lista doble
                    arr.put(obj);
                }
            } else if (path.equals("/api/species")) {
                // Qué hace: Verifica si la solicitud contiene un ID específico en el pathInfo para retornar una especie única.
                // Por qué existe: El RevisionPlanController.js del supervisor consume /api/species/{id} al evaluar las mascotas.
                // Qué problema resuelve: Devuelve un objeto individual con soporte de localización para evitar fallos 404.
                String pathInfo = request.getPathInfo();
                if (pathInfo != null && pathInfo.length() > 1) {
                    try {
                        int id = Integer.parseInt(pathInfo.substring(1));
                        UbicacionDTO sp = null;
                        List<UbicacionDTO> list = catalogoDAO.getEspeciesMascota();
                        for (UbicacionDTO s : list) {
                            if (s.getId() == id) {
                                sp = s;
                                break;
                            }
                        }
                        if (sp != null) {
                            JSONObject obj = new JSONObject();
                            obj.put("id", sp.getId());
                            obj.put("nombre", sp.getNombre());
                            obj.put("name", sp.getNombre()); // Inyecta name para soporte de frontend
                            obj.put("activo", 1);
                            
                            JSONObject res = new JSONObject();
                            res.put("success", true);
                            res.put("data", obj);
                            out.print(res.toString());
                            return;
                        }
                    } catch (NumberFormatException e) {
                        // Ignora errores de parsing de URL no numéricos
                    }
                }

                // Qué hace: Retorna la lista completa de especies con soporte dual de claves nombre/name.
                // Por qué existe: Alimenta el dropdown de especies de mascotas en la SPA.
                // Qué problema resuelve: Unifica las propiedades de la respuesta.
                List<UbicacionDTO> list = catalogoDAO.getEspeciesMascota();
                for (UbicacionDTO sp : list) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", sp.getId());
                    obj.put("nombre", sp.getNombre());
                    obj.put("name", sp.getNombre()); // Inyecta name para compatibilidad de la SPA
                    obj.put("activo", 1);
                    arr.put(obj);
                }
            } else if (path.equals("/api/animalGenders")) {
                // Qué hace: Verifica si la petición incluye un identificador específico de mascota para retornar su género.
                // Por qué existe: Satisface la ruta /api/animalGenders/pet/{id} consumida por la vista supervisor.
                // Qué problema resuelve: Evita llamadas fallidas retornando el género de la mascota de forma estructurada.
                String pathInfo = request.getPathInfo();
                if (pathInfo != null && pathInfo.startsWith("/pet/")) {
                    try {
                        int petId = Integer.parseInt(pathInfo.substring(5));
                        MascotaDAO mascotaDAO = new MascotaDAO();
                        MascotaDTO pet = mascotaDAO.obtenerMascota(petId);
                        if (pet != null) {
                            JSONObject obj = new JSONObject();
                            obj.put("id", pet.getAnimalGenderId());
                            obj.put("name", pet.getAnimalGender());
                            obj.put("nombre", pet.getAnimalGender());
                            
                            JSONObject res = new JSONObject();
                            res.put("success", true);
                            res.put("data", obj);
                            out.print(res.toString());
                            return;
                        }
                    } catch (Exception e) {
                        // Qué hace: Captura errores de base de datos o conversión y los registra.
                        e.printStackTrace();
                    }
                }

                // Qué hace: Retorna el catálogo estático de géneros de mascota.
                // Por qué existe: Evita consultar la tabla inexistente generos_mascota previniendo un error 500.
                // Qué problema resuelve: Suministra los géneros Macho y Hembra para poblar el select en la UI.
                JSONObject m = new JSONObject().put("id", 1).put("nombre", "Macho").put("name", "Macho").put("activo", 1);
                JSONObject h = new JSONObject().put("id", 2).put("nombre", "Hembra").put("name", "Hembra").put("activo", 1);
                arr.put(m);
                arr.put(h);
            } else if (path.equals("/api/tiposAmenaza")) {
                // Qué hace: Obtiene la información adicional de la ruta para ver si se pide un ID específico.
                // Por qué existe: Permite consultar una amenaza individual de forma independiente a la lista.
                // Qué problema resuelve: Satisface los consumos puntuales del frontend del supervisor al mostrar detalles de riesgos.
                String pathInfo = request.getPathInfo();
                if (pathInfo != null && pathInfo.length() > 1) {
                    try {
                        int id = Integer.parseInt(pathInfo.substring(1));
                        UbicacionDTO amen = null;
                        List<UbicacionDTO> list = catalogoDAO.getAmenazas();
                        for (UbicacionDTO a : list) {
                            if (a.getId() == id) {
                                amen = a;
                                break;
                            }
                        }
                        if (amen != null) {
                            JSONObject obj = new JSONObject();
                            obj.put("id", amen.getId());
                            obj.put("nombre", amen.getNombre());
                            obj.put("name", amen.getNombre());
                            obj.put("activo", 1);
                            
                            JSONObject res = new JSONObject();
                            res.put("success", true);
                            res.put("data", obj);
                            out.print(res.toString());
                            return;
                        }
                    } catch (NumberFormatException e) {
                        // Qué hace: Ignora la conversión fallida si no es un número y prosigue con la lista general.
                        // Por qué existe: Previene caídas en tiempo de ejecución por URLs corruptas o mal formadas.
                    }
                }
                
                // Catálogo de amenazas desde MySQL
                List<UbicacionDTO> list = catalogoDAO.getAmenazas();
                for (UbicacionDTO amen : list) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", amen.getId());
                    obj.put("nombre", amen.getNombre());
                    obj.put("name", amen.getNombre());
                    obj.put("activo", 1);
                    arr.put(obj);
                }
            } else if (path.equals("/api/vulnerabilidades")) {
                // Catálogo de vulnerabilidades desde MySQL
                List<UbicacionDTO> list = catalogoDAO.getVulnerabilidades();
                for (UbicacionDTO vuln : list) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", vuln.getId());
                    obj.put("nombre", vuln.getNombre());
                    obj.put("activo", 1);
                    arr.put(obj);
                }
            } else if (path.equals("/api/gradosVulnerabilidad")) {
                // Catálogo estático de grados de vulnerabilidad (Muy Alta, Alta, Media, Baja)
                JSONObject g1 = new JSONObject().put("id", 1).put("nombre", "Muy Alta").put("activo", 1);
                JSONObject g2 = new JSONObject().put("id", 2).put("nombre", "Alta").put("activo", 1);
                JSONObject g3 = new JSONObject().put("id", 3).put("nombre", "Media").put("activo", 1);
                JSONObject g4 = new JSONObject().put("id", 4).put("nombre", "Baja").put("activo", 1);
                arr.put(g1);
                arr.put(g2);
                arr.put(g3);
                arr.put(g4);
            }
            
            JSONObject res = new JSONObject();
            res.put("success", true);
            res.put("data", arr);
            
            out.print(res.toString());
            
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(new JSONObject()
                    .put("success", false)
                    .put("message", "Error al procesar el catálogo: " + e.getMessage())
                    .toString());
        }
    }
}
