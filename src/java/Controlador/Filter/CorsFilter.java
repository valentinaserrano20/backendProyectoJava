package Controlador.Filter;

/*
 * Qué hace (la acción): Importa la clase IOException y las APIs estándares de Jakarta Servlet para procesar filtros HTTP.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - jakarta.servlet.Filter: Interfaz para interceptar y transformar peticiones o respuestas.
 *   - jakarta.servlet.FilterChain: Cadena de filtros ordenados por el servidor.
 *   - jakarta.servlet.http.HttpServletResponse / HttpServletRequest: Objetos especializados en peticiones y respuestas HTTP.
 * Para qué se usa (el propósito): Proveer las herramientas de servidor indispensables para interceptar todo el tráfico y configurar CORS.
 * Por qué es importante (el impacto o problema que resuelve): Sin estas importaciones no se podría compilar el filtro ni manipular las cabeceras HTTP de red.
 */
import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;

/*
 * Qué hace (la acción): Aplica la anotación @WebFilter("/*") para registrar esta clase como un filtro global para todas las rutas del servidor.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - @WebFilter("/*"): Comodín "/*" que indica que absolutamente todas las URLs que entren al backend pasarán primero por este filtro.
 *   - implements Filter: Implementa el ciclo de vida de los filtros de servlet en Java.
 * Para qué se usa (el propósito): Actuar como un configurador de CORS global, interceptando todas las peticiones antes de cualquier otro filtro o servlet del sistema.
 * Por qué es importante (el impacto o problema que resuelve): Centraliza y soluciona las políticas de origen cruzado para todo el backend en un único punto, evitando tener que configurarlo de forma manual o repetitiva en otros componentes.
 */
@WebFilter("/*")
public class CorsFilter implements Filter {

    /*
     * Qué hace (la acción): Sobrescribe doFilter para interceptar todas las peticiones, inyectar cabeceras CORS de origen permitido, configurar los métodos HTTP soportados y dar paso o responder de inmediato si es OPTIONS.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - ServletRequest, ServletResponse: Solicitud y respuesta genéricas del contenedor.
     *   - FilterChain chain: Siguiente paso en la secuencia de filtros.
     * Para qué se usa (el propósito): Regular la seguridad de origen cruzado (CORS) de forma dinámica para todo el proyecto.
     * Por qué es importante (el impacto o problema que resuelve): Permite que aplicaciones web externas (como el frontend de desarrollo en Vite o producción) se comuniquen fluidamente con el backend de Java sin que el navegador web bloquee la conexión.
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        /*
         * Qué hace (la acción): Castea los objetos de petición y respuesta genéricos a tipos HttpServletRequest y HttpServletResponse.
         * Qué significa (conceptos, métodos, tipos involucrados): Conversión de clases para habilitar APIs exclusivas del protocolo HTTP.
         * Para qué se usa (el propósito): Acceder a métodos como request.getHeader y response.setHeader.
         * Por qué es importante (el impacto o problema que resuelve): Sin este paso, el compilador impediría el acceso a las cabeceras HTTP de origen y configuración.
         */
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        /*
         * Qué hace (la acción): Extrae el origen de la petición del cliente y lo contrasta contra una lista blanca (allowedOrigins) de direcciones seguras del frontend.
         * Qué significa (conceptos, métodos, tipos involucrados):
         *   - request.getHeader("Origin"): Recupera el dominio de donde proviene la petición del navegador (ej: http://localhost:5173).
         *   - HashSet / Arrays.asList: Crea un conjunto de orígenes permitidos de forma estática para búsquedas O(1).
         * Para qué se usa (el propósito): Validar que el origen del cliente web esté explícitamente autorizado a conectarse con este backend.
         * Por qué es importante (el impacto o problema que resuelve): Previene ataques de origen cruzado de sitios maliciosos no listados en la lista blanca de orígenes permitidos.
         */
        String origin = request.getHeader("Origin");
        java.util.Set<String> allowedOrigins = new java.util.HashSet<>(java.util.Arrays.asList(
                "http://localhost:5173",
                "http://localhost:3000",
                "http://127.0.0.1:5173",
                "http://127.0.0.1:3000"
        ));

        /*
         * Qué hace (la acción): Si el origen de la petición pertenece a la lista blanca, le autoriza el acceso a las credenciales; de lo contrario, setea un origen vacío.
         * Qué significa (conceptos, métodos, tipos involucrados):
         *   - Access-Control-Allow-Origin: Define quién puede leer la respuesta.
         *   - Access-Control-Allow-Credentials: Seteado en true para permitir cookies de sesión (JSESSIONID) de origen cruzado.
         * Para qué se usa (el propósito): Negociar los permisos CORS específicos de origen y sesión de manera selectiva.
         * Por qué es importante (el impacto o problema que resuelve): Permite que el cliente web mande cookies de sesión al servidor web a pesar de no estar en el mismo puerto o dominio, sin descuidar la seguridad de otros orígenes.
         */
        if (origin != null && allowedOrigins.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        } else {
            response.setHeader("Access-Control-Allow-Origin", "");
        }

        /*
         * Qué hace (la acción): Inyecta las cabeceras CORS globales que autorizan los métodos HTTP (GET, POST, PUT, DELETE, PATCH, OPTIONS) y las cabeceras (Content-Type, Authorization).
         * Qué significa (conceptos, métodos, tipos involucrados):
         *   - Access-Control-Allow-Methods: Métodos HTTP permitidos para interactuar.
         *   - Access-Control-Allow-Headers: Cabeceras permitidas en las llamadas.
         *   - Access-Control-Max-Age: Tiempo de caché para los permisos CORS.
         * Para qué se usa (el propósito): Indicar de forma general al navegador qué métodos y cabeceras son aceptados por la API.
         * Por qué es importante (el impacto o problema que resuelve): Evita que el navegador bloquee peticiones de actualización parcial (PATCH) u operaciones con cabeceras de autorización personalizadas.
         */
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setHeader("Access-Control-Max-Age", "3600");

        /*
         * Qué hace (la acción): Detecta si la petición utiliza el método HTTP OPTIONS (comprobación preflight) y responde inmediatamente con un estado 200 (OK), terminando el flujo de esa petición.
         * Qué significa (conceptos, métodos, tipos involucrados): SC_OK representa el código 200 de ejecución exitosa en red.
         * Para qué se usa (el propósito): Responder de forma ágil a las solicitudes preflight sin procesar lógica en los controladores.
         * Por qué es importante (el impacto o problema que resuelve): Si no se finalizara la petición de inmediato con return, la llamada OPTIONS procedería erróneamente hacia los servlets, los cuales no están diseñados para responder a este método de red.
         */
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        /*
         * Qué hace (la acción): Envía la petición hacia el siguiente filtro o servlet correspondiente.
         * Qué significa (conceptos, métodos, tipos involucrados): chain.doFilter(req, res).
         * Para qué se usa (el propósito): Permitir que el flujo continúe una vez inyectadas las cabeceras CORS necesarias.
         * Por qué es importante (el impacto o problema que resuelve): Imprescindible para que las peticiones reales (GET, POST, etc.) alcancen los controladores de negocio del backend.
         */
        chain.doFilter(req, res);
    }

    /*
     * Qué hace (la acción): Cumple con el ciclo de vida de inicialización de la interfaz Filter.
     * Qué significa (conceptos, métodos, tipos involucrados): init(fc) del ciclo de vida.
     * Para qué se usa (el propósito): Realizar ajustes de arranque si existieran (aquí se mantiene vacío).
     */
    @Override
    public void init(FilterConfig fc) {
    }

    /*
     * Qué hace (la acción): Cumple con el ciclo de vida de destrucción de la interfaz Filter.
     * Qué significa (conceptos, métodos, tipos involucrados): destroy() del ciclo de vida.
     * Para qué se usa (el propósito): Liberar recursos del filtro (aquí se mantiene vacío).
     */
    @Override
    public void destroy() {
    }
}
