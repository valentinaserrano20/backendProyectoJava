package Modelo.Servicios.Public;

import Modelo.DAO.CatalogoDAO;
import org.json.JSONArray;
import org.json.JSONObject;

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

                data = catalogoDAO.getGeneros();
                break;

            case "/tipos-documento":

                data = catalogoDAO.getTiposDocumento();
                break;

            case "/organizaciones":

                data = catalogoDAO.getOrganizaciones();
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

