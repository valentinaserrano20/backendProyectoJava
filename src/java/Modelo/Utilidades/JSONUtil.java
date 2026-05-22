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

    public static JSONObject leerJson(
            HttpServletRequest request
    ) throws IOException {

        // LECTOR DEL BODY HTTP
        BufferedReader reader =
                request.getReader();

        // ACUMULADOR DEL JSON
        StringBuilder sb =
                new StringBuilder();

        // VARIABLE TEMPORAL
        String line;

        // LEER TODAS LAS LÍNEAS
        while ((line = reader.readLine()) != null) {

            sb.append(line);
        }

        // CONVERTIR STRING A JSON
        return new JSONObject(sb.toString());
    }
}