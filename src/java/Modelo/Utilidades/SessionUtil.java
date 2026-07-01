package Modelo.Utilidades;

import jakarta.servlet.http.HttpSession;

/**
 * Clase utilitaria encargada de la extracción y mapeo seguro de atributos de sesión de usuario.
 * Proporciona métodos defensivos para prevenir NullPointerException durante el autounboxing de tipos primitivos.
 */
public class SessionUtil {

    /**
     * Recupera de forma segura el ID de usuario activo de la sesión.
     * Evalúa tanto el atributo "usuarioId" como "user_id" para mantener compatibilidad,
     * previniendo errores de autounboxing si la sesión ha expirado o el valor es nulo.
     * 
     * @param session Instancia de HttpSession a evaluar.
     * @return El ID de usuario como Integer, o null si la sesión no posee un ID activo.
     */
    public static Integer getUsuarioId(HttpSession session) {
        if (session == null) {
            return null;
        }
        
        Object val = session.getAttribute("usuarioId");
        if (val == null) {
            val = session.getAttribute("user_id");
        }
        
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        
        return null;
    }
}
