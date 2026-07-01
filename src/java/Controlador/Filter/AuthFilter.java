package Controlador.Filter;

/*
 * Qué hace (la acción): Importa las interfaces, clases y anotaciones necesarias de la API de Jakarta Servlet, Java Collections y procesamiento JSON.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - jakarta.servlet.*: Clases del motor de servlets para interceptar peticiones web.
 *   - java.util.*: Clases para colecciones de datos únicas (Set, HashSet).
 *   - org.json.JSONObject: Biblioteca externa para estructurar respuestas en formato JSON.
 * Para qué se usa (el propósito): Permite utilizar estas clases y tipos de datos por su nombre simple sin escribir su ruta completa en el código.
 * Por qué es importante (el impacto o problema que resuelve): Sin estas importaciones, el compilador arrojará errores de sintaxis al no reconocer tipos como Filter, HttpServletRequest, o JSONObject.
 */
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONObject;

/*
 * Qué hace (la acción): Aplica el decorador @WebFilter para que el servidor web asocie la clase AuthFilter con las URLs que empiezan por "/api/*".
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - @WebFilter("/api/*"): Anotación que indica al contenedor de servlets que registre esta clase como un filtro interceptor para ese patrón de URL.
 *   - implements Filter: Indica que esta clase implementa la interfaz Filter de Jakarta EE, obligándose a definir sus métodos init, doFilter y destroy.
 * Para qué se usa (el propósito): Define a AuthFilter como el interceptor global perimetral para asegurar y validar todas las peticiones a la API.
 * Por qué es importante (el impacto o problema que resuelve): Centraliza la seguridad. Si no estuviera, cada controlador tendría que validar la sesión individualmente, lo que causaría duplicación de código y riesgos de seguridad si se olvida algún endpoint.
 */
@WebFilter("/api/*")
public class AuthFilter implements Filter {

    /*
     * Qué hace (la acción): Declara e inicializa la colección privada y constante (final) 'publicPaths' como un HashSet.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - Set<String>: Interfaz que representa una colección ordenada o desordenada de elementos únicos.
     *   - HashSet: Implementación de Set basada en una tabla hash que provee búsquedas extremadamente rápidas de tiempo constante O(1).
     * Para qué se usa (el propósito): Almacenar las URLs públicas que están autorizadas a ser accedidas sin iniciar sesión.
     * Por qué es importante (el impacto o problema que resuelve): Permite validar en tiempo récord si una ruta es pública sin necesidad de recorrer una lista completa paso por paso.
     */
    private final Set<String> publicPaths = new HashSet<>();

    /*
     * Qué hace (la acción): Sobrescribe el método init del ciclo de vida del filtro para inicializar la lista blanca de rutas públicas.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - @Override: Anotación que valida ante el compilador que este método reemplaza a uno heredado de la interfaz.
     *   - FilterConfig: Objeto con parámetros de configuración del filtro provistos por el servidor.
     *   - ServletException: Excepción que se puede lanzar si el servlet o filtro falla al inicializarse.
     * Para qué se usa (el propósito): Cargar todas las rutas exentas de inicio de sesión antes de que la aplicación empiece a recibir peticiones de clientes.
     * Por qué es importante (el impacto o problema que resuelve): Configura el filtro al iniciar el servidor una sola vez en lugar de construir la lista con cada petición entrante, ahorrando procesamiento y memoria.
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Rutas de autenticación pública
        publicPaths.add("/api/login");
        publicPaths.add("/api/register");
        publicPaths.add("/api/forgotPassword");
        publicPaths.add("/api/catalogoPublico");

        // Endpoints de catálogos paramétricos requeridos antes de iniciar sesión
        publicPaths.add("/api/documentTypes");
        publicPaths.add("/api/genders");
        publicPaths.add("/api/kinships");
        publicPaths.add("/api/bloodGroups");
        publicPaths.add("/api/nationalities");
        publicPaths.add("/api/conditionTypes");
        publicPaths.add("/api/species");
        publicPaths.add("/api/animalGenders");
        publicPaths.add("/api/tiposAmenaza");
        publicPaths.add("/api/vulnerabilidades");
        publicPaths.add("/api/gradosVulnerabilidad");
        publicPaths.add("/api/tiposRecurso");
        publicPaths.add("/api/statusPlans");
    }

    /*
     * Qué hace (la acción): Sobrescribe el método principal doFilter para interceptar, evaluar y procesar la seguridad de cada petición.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - ServletRequest req: Objeto genérico que contiene los datos de la petición entrante.
     *   - ServletResponse res: Objeto genérico que contiene la respuesta que se enviará al cliente.
     *   - FilterChain chain: Objeto para delegar la petición al siguiente filtro o servlet en la cadena de ejecución.
     *   - IOException, ServletException: Excepciones lanzadas por fallos de red o errores internos del contenedor de servlets.
     * Para qué se usa (el propósito): Analizar los permisos de cada petición entrante a la API para dar paso o bloquear la solicitud.
     * Por qué es importante (el impacto o problema que resuelve): Es el núcleo de seguridad. Sin este método, no habría forma de inspeccionar las peticiones entrantes para restringir los accesos.
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        /*
         * Qué hace (la acción): Convierte (castea) los objetos genéricos ServletRequest y ServletResponse a clases HTTP especializadas.
         * Qué significa (conceptos, métodos, tipos involucrados): Casting explícito a HttpServletRequest y HttpServletResponse.
         * Para qué se usa (el propósito): Obtener acceso a la sesión, cabeceras HTTP, métodos HTTP (GET, POST) y manipulación de respuestas.
         * Por qué es importante (el impacto o problema que resuelve): Sin este casteo no podríamos leer cabeceras como "Origin" ni obtener la sesión del cliente, ya que la interfaz base ServletRequest no las tiene definidas.
         */
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        /*
         * Qué hace (la acción): Detecta si la petición utiliza el método HTTP OPTIONS para inyectar cabeceras CORS y responder inmediatamente con un código 200 (OK).
         * Qué significa (conceptos, métodos, tipos involucrados):
         *   - OPTIONS: Petición de "preflight" que envían los navegadores antes de una petición real a dominios cruzados para validar permisos.
         *   - Access-Control-Allow-Origin: Cabecera que permite al cliente externo leer la respuesta.
         *   - Access-Control-Allow-Credentials: Indica si la API acepta cookies de sesión del cliente.
         *   - Access-Control-Max-Age: Tiempo de caché para los permisos CORS.
         * Para qué se usa (el propósito): Evitar que las peticiones CORS hechas desde el frontend del navegador sean bloqueadas por políticas de seguridad de origen cruzado.
         * Por qué es importante (el impacto o problema que resuelve): Si no estuviera, el navegador bloquearía la comunicación del frontend hacia el backend, haciendo imposible que el cliente se comunique con la API.
         */
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            String origin = request.getHeader("Origin");
            if (origin != null) {
                response.setHeader("Access-Control-Allow-Origin", origin);
                response.setHeader("Access-Control-Allow-Credentials", "true");
            }
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
            response.setHeader("Access-Control-Max-Age", "3600");
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        /*
         * Qué hace (la acción): Obtiene la URI de la petición relativa a la aplicación eliminando el contexto de la URL y limpiando la barra inclinada '/' final.
         * Qué significa (conceptos, métodos, tipos involucrados):
         *   - request.getRequestURI(): Ruta completa de la petición.
         *   - request.getContextPath(): Ruta base de despliegue de la aplicación.
         *   - substring(): Extrae una porción del texto.
         *   - endsWith(): Evalúa si la cadena finaliza con un carácter específico.
         * Para qué se usa (el propósito): Estandarizar la URL para que no haya diferencias entre "/api/recurso/" y "/api/recurso" al momento de compararla.
         * Por qué es importante (el impacto o problema que resuelve): Previene bypasses de seguridad donde un usuario podría saltarse el filtro agregando barras inclinadas adicionales al final de la URL.
         */
        String path = request.getRequestURI().substring(request.getContextPath().length());

        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }

        /*
         * Qué hace (la acción): Evalúa si el endpoint solicitado es de libre acceso público (coincide con publicPaths o inicia con prefijos públicos dinámicos).
         * Qué significa (conceptos, métodos, tipos involucrados):
         *   - contains(path): Verifica coincidencia exacta en el conjunto.
         *   - startsWith(prefijo): Verifica si la URL inicia con un determinado prefijo (ej: sub-recursos con IDs variables).
         * Para qué se usa (el propósito): Determinar si la petición puede saltarse el flujo de autenticación obligatoria.
         * Por qué es importante (el impacto o problema que resuelve): Permite que catálogos y llamadas esenciales iniciales (como cargar géneros o tipos de sangre para registrarse) estén exentos de login.
         */
        boolean isPublic = publicPaths.contains(path) 
                || path.startsWith("/api/public")
                || path.startsWith("/api/kinships/")
                || path.startsWith("/api/animalGenders/")
                || path.startsWith("/api/species/")
                || path.startsWith("/api/tiposAmenaza/")
                || path.startsWith("/api/statusPlans/");

        /*
         * Qué hace (la acción): Si el recurso es público, configura la cabecera de la respuesta como JSON codificado en UTF-8 (excepto para PDFs) y permite el paso al servlet correspondiente.
         * Qué significa (conceptos, métodos, tipos involucrados):
         *   - setContentType("application/json"): Define que los datos retornados serán JSON.
         *   - setCharacterEncoding("UTF-8"): Establece la codificación de caracteres.
         *   - chain.doFilter(req, res): Permite continuar al siguiente eslabón en la cadena de ejecución.
         * Para qué se usa (el propósito): Procesar de forma inmediata llamadas públicas sin bloquear al cliente.
         * Por qué es importante (el impacto o problema que resuelve): Da paso al tráfico público e impide que se arruinen las descargas binarias de reportes PDF (/api/pdf/) al no forzar cabeceras de texto/JSON en ellas.
         */
        if (isPublic) {
            if (!path.startsWith("/api/pdf/")) {
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
            }
            chain.doFilter(req, res);
            return;
        }

        /*
         * Qué hace (la acción): Intenta recuperar la sesión HTTP del cliente si ya existe, sin crear una nueva en memoria en caso contrario.
         * Qué significa (conceptos, métodos, tipos involucrados):
         *   - request.getSession(false): Si se le pasa false, retorna la sesión existente o null si no la hay.
         * Para qué se usa (el propósito): Comprobar de forma segura si existe una sesión activa vinculada a la cookie JSESSIONID del cliente.
         * Por qué es importante (el impacto o problema que resuelve): Previene el consumo excesivo de memoria del servidor, ya que pasarle true crearía una sesión en memoria por cada ataque o bot que llame a la API sin estar autenticado.
         */
        HttpSession session = request.getSession(false);

        /*
         * Qué hace (la acción): Evalúa si la sesión no es nula y cuenta con el atributo 'usuarioId' o 'user_id' registrado con algún valor válido.
         * Qué significa (conceptos, métodos, tipos involucrados):
         *   - session.getAttribute(nombre): Recupera variables persistentes almacenadas del lado del servidor para ese cliente.
         * Para qué se usa (el propósito): Validar técnicamente si el usuario inició sesión correctamente y su sesión sigue vigente.
         * Por qué es importante (el impacto o problema que resuelve): Evita suplantaciones de identidad. No permite que usuarios no autenticados accedan a recursos privados.
         */
        boolean isAuthenticated = session != null 
                && (session.getAttribute("usuarioId") != null || session.getAttribute("user_id") != null);

        /*
         * Qué hace (la acción): Si el usuario no está autenticado, inyecta las cabeceras CORS en la respuesta, configura el estado como HTTP 401 Unauthorized, arma un JSONObject de error y finaliza la llamada.
         * Qué significa (conceptos, métodos, tipos involucrados):
         *   - SC_UNAUTHORIZED: Constante de HttpServletResponse que equivale al código HTTP 401.
         *   - errorJson.toString(): Serializa el objeto JSON de error en una cadena de texto.
         *   - response.getWriter().write(): Escribe los caracteres de respuesta al cliente.
         * Para qué se usa (el propósito): Detener e informar de manera estructurada al cliente la falta de privilegios de acceso.
         * Por qué es importante (el impacto o problema que resuelve): Impide el procesamiento del endpoint protegido y le da un mensaje claro en JSON al frontend para que redirija al usuario al login, evitando a la vez bloqueos del navegador gracias a CORS.
         */
        if (!isAuthenticated) {
            String origin = request.getHeader("Origin");
            if (origin != null) {
                response.setHeader("Access-Control-Allow-Origin", origin);
                response.setHeader("Access-Control-Allow-Credentials", "true");
            }

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            
            JSONObject errorJson = new JSONObject();
            errorJson.put("success", false);
            errorJson.put("message", "Acceso denegado. Inicie sesión para continuar.");
            
            response.getWriter().write(errorJson.toString());
            return;
        }

        /*
         * Qué hace (la acción): Configura las cabeceras de contenido JSON y la codificación UTF-8 para las peticiones de usuarios autenticados (que no correspondan a PDFs).
         * Qué significa (conceptos, métodos, tipos involucrados): Inyección de cabeceras HTTP en respuestas aprobadas.
         * Para qué se usa (el propósito): Asegurar que los servlets autenticados envíen datos de tipo JSON por defecto.
         * Por qué es importante (el impacto o problema que resuelve): Ahorra tener que configurar estas cabeceras repetitivamente en cada servlet autenticado y evita corromper descargas binarias de archivos de reporte PDF.
         */
        if (!path.startsWith("/api/pdf/")) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
        }

        /*
         * Qué hace (la acción): Permite continuar con el procesamiento normal del request pasándolo al siguiente filtro o servlet correspondiente.
         * Qué significa (conceptos, métodos, tipos involucrados): Ejecución del método chain.doFilter(req, res).
         * Para qué se usa (el propósito): Delegar la solicitud al controlador web final que posee la lógica del negocio.
         * Por qué es importante (el impacto o problema que resuelve): Si esta línea no estuviera, la petición de los usuarios autenticados se quedaría colgada en el filtro y nunca obtendrían la respuesta solicitada.
         */
        chain.doFilter(req, res);
    }

    /*
     * Qué hace (la acción): Sobrescribe el método destroy del ciclo de vida del filtro para la limpieza de recursos.
     * Qué significa (conceptos, métodos, tipos involucrados): destroy() es invocado por el contenedor de servlets al apagar o retirar el filtro.
     * Para qué se usa (el propósito): Liberar recursos persistentes o conexiones si existieran (aquí se mantiene vacío al no requerirse).
     * Por qué es importante (el impacto o problema que resuelve): Cumple el contrato estructural de la interfaz Filter y ofrece un punto limpio de liberación de memoria de ser requerido a futuro.
     */
    @Override
    public void destroy() {
    }
}
