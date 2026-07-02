package Modelo.Utilidades;

// =========================================
// IMPORTACIONES
// =========================================

import jakarta.servlet.http.HttpServletRequest;

import java.io.BufferedReader;
import java.io.IOException;

import org.json.JSONObject;

// =========================================
// UTILIDAD PARA MANEJO DE JSON
// =========================================

/**
 * Qué hace: Clase utilitaria dedicada al procesamiento y lectura de datos en formato JSON provenientes del cliente.
 * Por qué existe: Los Servlets Java estándar no poseen un mecanismo nativo para parsear cuerpos de petición con Content-Type "application/json" a objetos manejables, por lo que esta clase unifica esa lectura.
 * Qué pasaría si no estuviera: Cada Servlet que reciba JSON (como los controladores de registro, test de vulnerabilidad o mascotas) tendría que implementar manualmente la lectura del BufferedReader, duplicando código.
 */
public class JSONUtil {

    // =========================================
    // LEER JSON DEL BODY DEL REQUEST
    // =========================================

    /**
     * Qué hace: Lee secuencialmente el cuerpo (Body) de una petición HttpServletRequest y lo transforma en un objeto de tipo JSONObject.
     * Qué significa: Lee el flujo de entrada de caracteres a través del lector de la petición, acumula el texto completo en memoria y lo parsea sintácticamente.
     * Para qué se usa: Permite que los servlets obtengan de forma estructurada los parámetros enviados por Axios o Fetch desde el frontend de la SPA.
     * Por qué es importante: Soporta el intercambio asíncrono de datos moderno (REST/JSON) en lugar del envío clásico de formularios multiparte.
     * 
     * @param request La petición HTTP entrante.
     * @return Un JSONObject que contiene las propiedades enviadas en el body.
     * @throws IOException Si ocurre un fallo de lectura del flujo de datos de red.
     */
    public static JSONObject leerJson(HttpServletRequest request) throws IOException {
        // Instanciamos un StringBuilder para acumular de forma eficiente todo el texto del cuerpo (body) de la petición HTTP recibida
        StringBuilder sb = new StringBuilder();
        // Declaramos una variable String temporal que almacenará cada línea del texto leída del flujo de datos
        String line;
        
        // Invocamos request.getReader() que obtiene el flujo de entrada del cuerpo de la petición HTTP en formato de caracteres.
        // Asignamos dicho flujo a un objeto BufferedReader, que es una clase de Java diseñada para leer texto de manera eficiente utilizando un búfer intermedio en memoria.
        BufferedReader reader = request.getReader();
        
        // Ejecutamos un bucle while que asigna a la variable 'line' el resultado del método reader.readLine() (que lee una línea completa de texto).
        // El bucle continuará ejecutándose hasta que reader.readLine() devuelva null, lo cual indica que hemos alcanzado el final del cuerpo de la petición.
        while ((line = reader.readLine()) != null) {
            // Añadimos cada línea leída a nuestro acumulador StringBuilder
            sb.append(line);
        }
        
        // Convertimos el acumulador a String y usamos .trim() para limpiar los espacios en blanco accidentales de los extremos
        String jsonString = sb.toString().trim();
        // Validamos si la cadena resultante está completamente vacía
        if (jsonString.isEmpty()) {
            // Lanzamos una excepción de argumento ilegal si la petición no contiene datos en su cuerpo
            throw new IllegalArgumentException("El cuerpo de la solicitud está vacío");
        }
        
        try {
            // Instanciamos y retornamos un JSONObject analizando y parseando la cadena de texto JSON
            return new JSONObject(jsonString);
        } catch (Exception e) {
            // Capturamos cualquier error de parseo y lanzamos una excepción con el detalle del formato JSON inválido
            throw new IllegalArgumentException("JSON inválido: " + e.getMessage());
        }
    }
}