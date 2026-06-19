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

public class JSONUtil {

    // =========================================
    // LEER JSON DEL BODY DEL REQUEST
    // =========================================

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