package Modelo.Servicios.Public;

import Modelo.DAO.CatalogoDAO;
import org.json.JSONArray;
import org.json.JSONObject;

// MODIFICADO: Serializa los DTOs de CatalogoDAO a JSON en la capa de Servicio, aislando al DAO de la capa de presentación
public class CatalogoServicio {

    private CatalogoDAO catalogoDAO;

    public CatalogoServicio() {
        catalogoDAO = new CatalogoDAO();
    }

    public String obtenerCatalogo(String path)
            throws Exception {

        JSONArray data;

        switch (path) {

            case "/generos":
                // AGREGADO: Serializa la lista de GeneroDTO a JSONArray
                data = new JSONArray(catalogoDAO.getGeneros());
                break;

            case "/tipos-documento":
                // AGREGADO: Serializa la lista de TipoDocumentoDTO a JSONArray
                data = new JSONArray(catalogoDAO.getTiposDocumento());
                break;

            case "/organizaciones":
                // AGREGADO: Serializa la lista de OrganizacionDTO a JSONArray
                data = new JSONArray(catalogoDAO.getOrganizaciones());
                break;

            default:

                return new JSONObject()
                        .put("success", false)
                        .put("message", "Ruta no encontrada")
                        .toString();
        }

        return new JSONObject()
                .put("success", true)
                .put("data", data)
                .toString();
    }
}

