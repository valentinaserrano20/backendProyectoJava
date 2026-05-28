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
    StringBuilder sb = new StringBuilder();
    String line;
    BufferedReader reader = request.getReader();
    
    while ((line = reader.readLine()) != null) {
        sb.append(line);
    }
    
    String jsonString = sb.toString().trim();
    if (jsonString.isEmpty()) {
        throw new IllegalArgumentException("El cuerpo de la solicitud está vacío");
    }
    
    try {
        return new JSONObject(jsonString);
    } catch (Exception e) {
        throw new IllegalArgumentException("JSON inválido: " + e.getMessage());
    }
}
}