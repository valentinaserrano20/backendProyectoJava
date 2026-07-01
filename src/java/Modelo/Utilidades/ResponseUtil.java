package Modelo.Utilidades;

import org.json.JSONObject;

/**
 * Qué hace: Clase utilitaria encargada de estandarizar e intermediar en la creación de respuestas JSON de la capa de Servicios.
 * Por qué existe: Antes, cada método de servicio duplicaba bloques try-catch con inicializaciones manuales de 'new JSONObject()'
 * y mapeos manuales de claves 'success', 'message' y 'data'. Esto centraliza el formato y reduce el boilerplate del proyecto.
 * Qué problema resuelve: Elimina la redundancia y asegura que el frontend reciba siempre la misma estructura JSON predecible,
 * facilitando la mantenibilidad al concentrar el contrato de la API en un solo punto.
 */
public class ResponseUtil {

    /**
     * Qué hace: Crea una respuesta JSON exitosa simple sin datos adicionales.
     * Por qué se hizo: Para responder a operaciones de guardado, actualización o eliminación simples que solo requieren indicar éxito.
     * Qué significa: Retorna un String JSON con {"success": true}.
     */
    public static String success() {
        return new JSONObject().put("success", true).toString();
    }

    /**
     * Qué hace: Crea una respuesta JSON exitosa que contiene un mensaje descriptivo.
     * Por qué se hizo: Permite notificar al frontend un mensaje de confirmación amigable para mostrar en SweetAlert.
     * Qué significa: Retorna un String JSON con {"success": true, "message": message}.
     */
    public static String success(String message) {
        return new JSONObject()
                .put("success", true)
                .put("message", message)
                .toString();
    }

    /**
     * Qué hace: Crea una respuesta JSON exitosa con datos (data) asociados.
     * Por qué se hizo: Utilizado para consultas GET o posts que retornan una entidad o ID autogenerado.
     * Qué significa: Retorna un String JSON con {"success": true, "data": data}.
     */
    public static String success(Object data) {
        return new JSONObject()
                .put("success", true)
                .put("data", data)
                .toString();
    }

    /**
     * Qué hace: Crea una respuesta JSON exitosa con un mensaje descriptivo y un objeto de datos adjunto.
     * Por qué se hizo: Permite combinar la confirmación de éxito de SweetAlert con el retorno de información (como ID del registro creado).
     * Qué significa: Retorna un String JSON con {"success": true, "message": message, "data": data}.
     */
    public static String success(String message, Object data) {
        return new JSONObject()
                .put("success", true)
                .put("message", message)
                .put("data", data)
                .toString();
    }

    /**
     * Qué hace: Crea una respuesta JSON de error con un mensaje de fallo descriptivo.
     * Por qué se hizo: Para estandarizar cómo se reportan las fallas de negocio o excepciones del sistema al frontend.
     * Qué significa: Retorna un String JSON con {"success": false, "message": message}.
     */
    public static String error(String message) {
        return new JSONObject()
                .put("success", false)
                .put("message", message)
                .toString();
    }

    /**
     * Qué hace: Crea una respuesta JSON exitosa con datos (data) y metadatos de paginación (paginate).
     * Por qué se hizo: Para estandarizar cómo se retornan listados paginados de forma consistente al frontend.
     * Qué significa: Retorna un String JSON con {"success": true, "data": data, "paginate": paginate}.
     */
    public static String paginate(Object data, Object paginate) {
        return new JSONObject()
                .put("success", true)
                .put("data", data)
                .put("paginate", paginate)
                .toString();
    }
}

