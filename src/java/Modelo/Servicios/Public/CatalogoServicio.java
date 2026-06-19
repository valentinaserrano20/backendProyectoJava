package Modelo.Servicios.Public;

import Modelo.DAO.CatalogoDAO;
import org.json.JSONArray;
import org.json.JSONObject;

// Qué hace: Servicio encargado de centralizar la lógica de negocio, mapeo y serialización JSON de los catálogos públicos.
// Por qué existe: Actúa como capa intermedia de abstracción entre los controladores REST y el DAO de persistencia, asegurando un formato JSON uniforme.
// Qué pasaría si no estuviera: El controlador tendría que interactuar directo con el DAO y realizar la serialización JSON a mano, rompiendo el patrón MVC.
public class CatalogoServicio {

    // Qué hace: Instancia el DAO correspondiente para interactuar con la base de datos de catálogos.
    // Por qué existe: Permite consultar los registros paramétricos guardados en MySQL.
    // Flujo: De aquí pasamos a CatalogoDAO.
    private CatalogoDAO catalogoDAO;

    public CatalogoServicio() {
        catalogoDAO = new CatalogoDAO();
    }

    // Qué hace: Identifica la subruta de catálogo solicitada, consulta al DAO el listado correspondiente y lo encapsula en un JSONArray.
    // Por qué existe: Ofrece un enrutador unificado para que la SPA en el frontend llene múltiples combos de selección (roles, departamentos, zonas, etc.).
    // Qué pasaría si no estuviera: Habría que escribir métodos separados de consulta para cada tabla paramétrica del sistema, dificultando la escalabilidad.
    public String obtenerCatalogo(String path)
            throws Exception {

        JSONArray data;

        // Estructura condicional que mapea cada endpoint a su consulta DAO correspondiente
        switch (path) {

            case "/generos":
                // Qué hace: Obtiene y serializa los géneros.
                // y luego de esto pasamos a CatalogoDAO.getGeneros.
                data = new JSONArray(catalogoDAO.getGeneros());
                break;

            case "/tipos-documento":
                // Qué hace: Obtiene y serializa los tipos de documento de identidad.
                // y luego de esto pasamos a CatalogoDAO.getTiposDocumento.
                data = new JSONArray(catalogoDAO.getTiposDocumento());
                break;

            case "/organizaciones":
                // Qué hace: Obtiene y serializa las seccionales de la Defensa Civil.
                // y luego de esto pasamos a CatalogoDAO.getOrganizaciones.
                data = new JSONArray(catalogoDAO.getOrganizaciones());
                break;
            
            case "/zones":
                // Qué hace: Obtiene y serializa las zonas geográficas (Urbana / Rural).
                // y luego de esto pasamos a CatalogoDAO.getTiposZona.
                data = new JSONArray(catalogoDAO.getTiposZona());
                break;

            case "/sectors":
                // Qué hace: Obtiene y serializa los sectores geográficos activos.
                // y luego de esto pasamos a CatalogoDAO.getSectores.
                data = new JSONArray(catalogoDAO.getSectores());
                break;

            case "/housingQualities":
                // Qué hace: Obtiene y serializa las calidades o estados de vivienda del censo.
                // y luego de esto pasamos a CatalogoDAO.getCalidadesVivienda.
                data = new JSONArray(catalogoDAO.getCalidadesVivienda());
                break;

            case "/departments":
                // Qué hace: Retorna una estructura JSON simulada fija para departamentos (Santander).
                // Por qué existe: Cumple con el contrato esperado por la SPA del frontend sin requerir una tabla adicional en BD.
                data = new JSONArray("[{\"id\": 1, \"nombre\": \"Santander\"}]");
                break;

            case "/cities/department/1":
                // Qué hace: Mapea las ciudades colombianas consultando las organizaciones existentes.
                // y luego de esto pasamos a CatalogoDAO.getOrganizaciones.
                data = new JSONArray(catalogoDAO.getOrganizaciones());
                break;

            default:
                // Retorna error 404 si la subruta de catálogo no existe
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

