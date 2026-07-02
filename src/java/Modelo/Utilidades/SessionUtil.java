package Modelo.Utilidades;

import jakarta.servlet.http.HttpSession;

/**
 * Qué hace: Clase utilitaria encargada de la extracción y mapeo seguro de atributos de sesión de usuario en la capa web.
 * Por qué existe: Proporciona métodos defensivos y seguros para recuperar variables de sesión de Tomcat, previniendo excepciones NullPointerException durante el autounboxing de tipos primitivos.
 * Qué pasaría si no estuviera: Cada Servlet tendría que obtener la sesión, verificar si es nula, extraer el objeto de atributo, comprobar su tipo y convertirlo a entero de forma individual, resultando en código altamente propenso a caídas por puntero nulo.
 */
public class SessionUtil {

    /**
     * Qué hace: Recupera de forma segura el ID de usuario activo de la sesión (HttpSession).
     * Qué significa: Evalúa secuencialmente tanto el atributo tradicional "usuarioId" como el atributo "user_id", realizando un cast defensivo a través de la clase abstracta Number.
     * Para qué se usa: Permite que los controladores y filtros identifiquen de forma inequívoca qué usuario de la Defensa Civil está realizando la petición HTTP.
     * Por qué es importante: El autounboxing implícito de Java (de Integer a int) arroja un NullPointerException si la sesión ha expirado o el atributo no existe; este método actúa de intermediario seguro retornando null en lugar de lanzar una excepción fatal.
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
