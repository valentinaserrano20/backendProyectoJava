package Controlador.Voluntario;

import Modelo.DTO.RegistroPlanDTO;
import Modelo.DTO.ActualizarIdentificacionDTO;
import Modelo.Servicios.Voluntario.PlanFamiliarServicio;
import Modelo.Servicios.Voluntario.VulnerabilidadServicio;
import Modelo.DAO.NotificacionDAO;
import Modelo.DTO.NotificacionDTO;
import Modelo.DAO.PlanFamiliarDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse; 
import jakarta.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import org.json.JSONObject;

// Servlet mapeado a /api/familyPlans/* para gestionar el registro inicial y actualizaciones de planes familiares
@WebServlet("/api/familyPlans/*")
public class PlanFamiliarServlet extends HttpServlet {
    // Instancia el servicio de registro y consulta de planes familiares
    private final PlanFamiliarServicio planServicio = new PlanFamiliarServicio();
    // Instancia el servicio de vulnerabilidades para el cambio de estado de planes
    private final VulnerabilidadServicio vulServicio = new VulnerabilidadServicio();

    // Sobrescribe service para capturar peticiones PATCH y redirigirlas a doPatch
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        // Obtiene el verbo HTTP de la solicitud
        String method = req.getMethod();
        // Si es una petición PATCH, delega a nuestro manejador personalizado
        if (method.equalsIgnoreCase("PATCH")) {
            doPatch(req, resp);
        } else {
            // Si es otro método (GET/POST), continúa el ciclo de vida estándar
            super.service(req, resp);
        }
    }    // Intercepta peticiones HTTP GET para consultar el detalle de un plan familiar específico o acciones de validación.
    // Por qué existe: Sirve de enrutador para obtener datos de un plan familiar en el sistema.
    // Qué problema resuelve: Centraliza la obtención de datos según el pathInfo en español.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Qué hace: Establece el tipo de contenido de la respuesta HTTP a formato JSON.
        // Por qué existe: Indica al cliente frontend que recibirá una respuesta estructurada en JSON.
        // Qué problema resuelve: Asegura la correcta decodificación en la UI del SPA.
        response.setContentType("application/json");
        // Qué hace: Configura la codificación de caracteres en UTF-8.
        // Por qué existe: Garantiza que acentos y caracteres especiales no se corrompan en la transmisión.
        // Qué problema resuelve: Evita fallos de caracteres especiales en la UI del SPA.
        response.setCharacterEncoding("UTF-8");

        // Qué hace: Comprueba la existencia de una sesión de usuario válida en el contenedor.
        // Por qué existe: Protege el acceso a información familiar confidencial ante accesos anónimos.
        // Qué problema resuelve: Retorna un error HTTP 401 si no hay una sesión activa de usuario.
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            return;
        }

        // Qué hace: Obtiene la información de la subruta de la petición URL (pathInfo).
        // Por qué existe: Permite enrutar dinámicamente según si se pide listado, validaciones o ficha.
        // Qué problema resuelve: Extrae el ID del plan o la subacción solicitada de forma limpia.
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            // Sirve para: Resolver el listado paginado de planes cuando no hay un pathInfo específico
            // Qué hace: Obtiene el ID del voluntario de la sesión, extrae la página de los parámetros de consulta y llama al servicio
            // Por qué es importante: El frontend llama a /api/familyPlans pasándole el parámetro de página para pintar el listado principal
            try {
                // Qué hace: Recupera de la sesión el ID del voluntario autenticado.
                // Por qué existe: Se utiliza para filtrar en MySQL únicamente los planes asociados a este voluntario.
                // Qué problema resuelve: Asegura que el voluntario solo acceda a sus propios planes asignados.
                int usuarioId = (int) session.getAttribute("usuarioId");
                // Qué hace: Recupera el parámetro 'page' enviado por el frontend.
                // Por qué existe: Permite paginar el listado de planes en la pantalla principal.
                // Qué problema resuelve: Evita transferir datos de forma masiva dividiendo las respuestas.
                String pageParam = request.getParameter("page");
                int page = 1;
                if (pageParam != null && !pageParam.trim().isEmpty()) {
                    page = Integer.parseInt(pageParam);
                }
                // Qué hace: Invoca al servicio para obtener el listado paginado como JSON.
                // Por qué existe: Delegación de la lógica de negocio y mapeo JDBC en el servicio de planes.
                // Qué problema resuelve: Retorna el listado listo para renderizar.
                String jsonRespuesta = planServicio.listarPlanesPaginado(usuarioId, page);
                response.getWriter().write(jsonRespuesta);
            } catch (Exception e) {
                // Qué hace: Captura errores y responde con HTTP 500.
                // Por qué existe: Evita que el servidor colapse y responde estructuradamente ante fallos de conexión.
                // Qué problema resuelve: Informa adecuadamente del fallo en el backend.
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al procesar listado de planes: " + e.getMessage()).toString());
            }
            return;
        }

        // Qué hace: Divide la ruta del pathInfo utilizando barras como separadores.
        // Por qué existe: Permite aislar los parámetros de subrutas y subrecursos (ej: [/check-access, 11]).
        // Qué problema resuelve: Facilita el análisis de rutas de tipo REST.
        String[] partes = pathInfo.split("/");
        
        // Sirve para: Enrutar dinámicamente las solicitudes GET de consulta y validación del plan familiar
        // Qué hace: Evalúa si el primer segmento solicita 'check-access' o 'has-members' para procesarlas de manera especializada; de lo contrario, asume que es el ID del plan familiar directo
        // Por qué es importante: Evita errores de parseo numérico cuando el frontend consulta sub-recursos bajo /api/familyPlans
        try {
            if (partes[1].equals("check-access")) {
                // Sirve para: Validar si la petición de acceso trae un ID de plan
                // Qué hace: Comprueba la longitud de los segmentos y devuelve HTTP 400 si no está el ID del plan
                // Por qué es importante: Previene fallas por índices fuera de rango al acceder a partes[2]
                if (partes.length < 3) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de plan no provisto para verificación.").toString());
                    return;
                }
                // Qué hace: Parsea el ID numérico del plan familiar.
                // Por qué existe: Parámetro para filtrar en MySQL la propiedad del plan.
                // Qué problema resuelve: Identifica el plan a verificar.
                int planId = Integer.parseInt(partes[2]);
                // Qué hace: Obtiene el ID del voluntario activo.
                // Por qué existe: Valida la relación de propiedad de este voluntario sobre el plan.
                // Qué problema resuelve: Evita que voluntarios accedan a datos de planes de otras personas.
                int usuarioId = (int) session.getAttribute("usuarioId");
                // Qué hace: Consulta al servicio si el voluntario tiene acceso al plan.
                // Por qué existe: Ejecuta la consulta de validación de permisos del plan en base de datos.
                // Qué problema resuelve: Retorna JSON indicando si tiene permisos o no.
                String jsonRespuesta = planServicio.verificarAccesoAPlan(planId, usuarioId);
                response.getWriter().write(jsonRespuesta);
                
            } else if (partes[1].equals("has-members")) {
                // Sirve para: Validar si la petición de conteo de integrantes trae un ID de plan
                // Qué hace: Comprueba la longitud de los segmentos y devuelve HTTP 400 si falta el ID
                // Por qué es importante: Garantiza que se envíe el ID de plan necesario para la consulta en base de datos
                if (partes.length < 3) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write(new JSONObject().put("success", false).put("message", "ID de plan no provisto para validar integrantes.").toString());
                    return;
                }
                // Qué hace: Parsea el ID numérico del plan.
                // Por qué existe: Identificador para contar filas en la tabla integrantes_familiares.
                // Qué problema resuelve: Determina qué plan se evaluará.
                int planId = Integer.parseInt(partes[2]);
                // Qué hace: Ejecuta consulta SQL para validar si existen integrantes en el plan familiar.
                // Por qué existe: Impide finalizar o enviar planes sin integrantes asociados.
                // Qué problema resuelve: Valida la consistencia de los datos del censo familiar.
                String jsonRespuesta = planServicio.verificarTieneIntegrantes(planId);
                response.getWriter().write(jsonRespuesta);
                
            } else {
                // Intenta extraer el ID del plan familiar desde la primera posición
                // Qué hace: Parsea el ID del plan directamente de la ruta principal de la API.
                // Por qué existe: Mapea la ruta por defecto /api/familyPlans/{id} al detalle de la ficha.
                // Qué problema resuelve: Permite recuperar la información detallada del censo de la vivienda y miembros.
                int planId = Integer.parseInt(partes[1]);
                // Qué hace: Obtiene la información estructurada del plan detallado en JSON.
                // Por qué existe: Devuelve las secciones del censo cargadas en la UI para la edición paso a paso.
                // Qué problema resuelve: Expone todos los subdocumentos del plan en formato JSON.
                String json = planServicio.obtenerPlanDetallado(planId);
                response.getWriter().write(json);
            }
        } catch (NumberFormatException e) {
            // Qué hace: Captura errores de conversión numérica e informa error 400.
            // Por qué existe: El ID especificado en la ruta no es un valor entero procesable.
            // Qué problema resuelve: Evita fallos de parsing no controlados.
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID del plan debe ser numérico.").toString());
        } catch (Exception e) {
            // Qué hace: Captura excepciones generales e informa error 400.
            // Por qué existe: Muestra el error de base de datos o lógica que haya ocurrido.
            // Qué problema resuelve: Provee retroalimentación estructurada de fallos en GET.
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al procesar la petición GET: " + e.getMessage()).toString());
        }
    }

    // Intercepta peticiones HTTP POST para crear e inicializar un nuevo plan familiar (Paso 1).
    // Por qué existe: Recibe las solicitudes para crear el censo familiar en el sistema por parte del voluntario.
    // Qué problema resuelve: Inserta el registro básico inicial del plan en MySQL asociando el voluntario a cargo.
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Qué hace: Establece el tipo de contenido de respuesta a formato JSON.
        // Por qué existe: Asegura que el cliente reciba y decodifique correctamente el JSON de respuesta.
        // Qué problema resuelve: Previene que la respuesta sea interpretada de forma incorrecta.
        response.setContentType("application/json");
        // Qué hace: Configura la codificación de caracteres a UTF-8.
        // Por qué existe: Habilita el soporte de caracteres en español (acentos, eñes).
        // Qué problema resuelve: Evita la corrupción visual de texto en la interfaz.
        response.setCharacterEncoding("UTF-8");

        // Qué hace: Valida si el voluntario tiene una sesión activa iniciada.
        // Por qué existe: Protege la creación de planes familiares contra peticiones no autenticadas.
        // Qué problema resuelve: Retorna error HTTP 401 si no está autenticado.
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            return;
        }

        // Qué hace: Inicializa un StringBuilder para leer secuencialmente el cuerpo JSON de la petición.
        // Por qué existe: Permite reconstruir los datos enviados como stream por el protocolo HTTP.
        // Qué problema resuelve: Acumula las líneas de texto recibidas del cuerpo del request.
        StringBuilder buffer = new StringBuilder();
        String linea;
        try (BufferedReader reader = request.getReader()) {
            while ((linea = reader.readLine()) != null) {
                buffer.append(linea);
            }
        }

        try {
            // Qué hace: Convierte la cadena del buffer a un objeto JSONObject.
            // Por qué existe: Permite acceder a las propiedades enviadas por el frontend.
            // Qué problema resuelve: Transforma texto plano en un mapa estructurado de datos.
            JSONObject json = new JSONObject(buffer.toString());
            
            // Qué hace: Instancia el DTO para registrar el plan familiar.
            // Por qué existe: Encapsula los campos de entrada de forma tipada para la capa de servicios.
            // Qué problema resuelve: Pasa los datos de forma limpia y desacoplada del servlet.
            RegistroPlanDTO dto = new RegistroPlanDTO();
            // Qué hace: Asigna al DTO los campos de apellidos de la familia.
            dto.setLastNames(json.optString("last_names"));
            // Qué hace: Asigna el ID numérico de la zona (ej: Urbana, Rural).
            dto.setZoneId(json.optInt("zone_id"));
            // Qué hace: Asigna el ID numérico de la organización (ciudad/municipio).
            dto.setOrganizacionId(json.optInt("city_id"));
            // Qué hace: Recupera el ID del voluntario desde los atributos de sesión y lo inyecta en el DTO.
            // Por qué existe: Evita suplantaciones al no permitir enviar el ID de voluntario en el JSON.
            // Qué problema resuelve: Garantiza la seguridad de la propiedad del plan en MySQL.
            dto.setUserId((int) session.getAttribute("usuarioId"));

            // Qué hace: Invoca al servicio para registrar el plan en la base de datos.
            // Por qué existe: Inserta en la tabla planes_familiares e inicializa los bloques del censo.
            // Qué problema resuelve: Retorna la respuesta en formato JSON incluyendo el nuevo ID autogenerado.
            String jsonRespuesta = planServicio.registrarNuevoPlan(dto);
            response.getWriter().write(jsonRespuesta);

        } catch (Exception e) {
            // Qué hace: Responde con HTTP 400 si el payload es inválido o no parseable.
            // Por qué existe: Controla errores de sintaxis del JSON de entrada.
            // Qué problema resuelve: Retorna un JSON estándar reportando el fallo.
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "JSON mal formado.").toString());
        }
    }

    // Manejador personalizado para procesar peticiones HTTP PATCH (ej. cambiar estado o guardar identificación)
    // Por qué existe: Habilita la actualización parcial del estado del plan o los campos del censo (identificación).
    // Qué problema resuelve: Permite dividir la actualización del censo en pasos lógicos e independientes de forma RESTful.
    protected void doPatch(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Qué hace: Configura el tipo de respuesta a formato JSON.
        // Por qué existe: Indica al frontend que recibirá la respuesta en un JSON legible.
        // Qué problema resuelve: Estandariza la respuesta de salida.
        response.setContentType("application/json");
        // Qué hace: Configura la codificación de caracteres en UTF-8.
        // Por qué existe: Asegura que las eñes y acentos no se corrompan.
        // Qué problema resuelve: Previene fallos de caracteres especiales en la UI.
        response.setCharacterEncoding("UTF-8");

        // Qué hace: Obtiene la sesión actual del cliente.
        // Por qué existe: Protege la modificación de datos del plan contra accesos anónimos.
        // Qué problema resuelve: Retorna error HTTP 401 si no hay una sesión de usuario válida activa.
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acceso denegado. Inicie sesión.").toString());
            return;
        }

        // Qué hace: Obtiene la información de la subruta del URL (pathInfo).
        // Por qué existe: Permite analizar qué plan y qué acción se están solicitando.
        // Qué problema resuelve: Extrae los parámetros embebidos en el URL.
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Acción no especificada.").toString());
            return;
        }

        // Qué hace: Segmenta la subruta usando la barra diagonal como separador.
        // Por qué existe: Aísla el ID del plan de la acción en la ruta REST.
        // Qué problema resuelve: Convierte la cadena en un array indexable de partes.
        String[] partes = pathInfo.split("/");
        if (partes.length < 3) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "URL mal estructurada.").toString());
            return;
        }

        try {
            int planId;
            String accion;

            // Qué hace: Identifica si la ruta corresponde al formato abreviado /status/{id} enviado por el rechazo definitivo.
            // Por qué existe: Corrige el choque de parsing numérico que genera NumberFormatException al intentar convertir "status" a entero.
            // Qué problema resuelve: Asigna correctamente el ID del plan desde partes[2] y sobreescribe la acción a "change-status".
            if (partes[1].equals("status")) {
                planId = Integer.parseInt(partes[2]);
                accion = "change-status";
            } else {
                // Obtiene el ID del plan familiar desde la ruta de la URL (formato estándar)
                planId = Integer.parseInt(partes[1]);
                // Obtiene el nombre de la acción (ej. identify / change-status)
                accion = partes[2];
            }

            // Escenario 1: Cambiar el estado del plan
            // Qué hace: Verifica si la acción es el cambio de estado del plan (ej. enviar censo, aprobar, rechazar).
            // Por qué existe: Controla la progresión del ciclo de vida del plan familiar.
            // Qué problema resuelve: Modifica la columna estado_plan_id del plan en base de datos.
            if (accion.equals("change-status")) {
                // Qué hace: Lee el JSON enviado en el cuerpo de la petición HTTP.
                // Por qué existe: Recupera el payload con el ID del nuevo estado y el comentario.
                // Qué problema resuelve: Acumula las líneas de texto en un StringBuilder.
                StringBuilder buffer = new StringBuilder();
                String linea;
                try (BufferedReader reader = request.getReader()) {
                    while ((linea = reader.readLine()) != null) {
                        buffer.append(linea);
                    }
                }
                
                // Qué hace: Convierte el texto acumulado a un objeto JSON.
                // Por qué existe: Permite acceder a las propiedades del cuerpo de la petición.
                // Qué problema resuelve: Transforma el texto plano en variables de negocio.
                JSONObject json = new JSONObject(buffer.toString());
                int statusPlanId = json.getInt("status_plan_id");
                
                // Qué hace: Extrae el comentario opcional si existe en el JSON.
                // Por qué existe: Permite que el supervisor registre el motivo de un rechazo.
                // Qué problema resuelve: Evita excepciones NullPointerException controlando nulos.
                String comentary = json.has("comentary") && !json.isNull("comentary") ? json.getString("comentary") : null;
                // Qué hace: Obtiene de forma segura el ID del operador en sesión.
                // Por qué existe: Registra quién realiza el cambio de estado en la bitácora de seguimiento.
                // Qué problema resuelve: Vincula la autoría del cambio al usuario autenticado.
                int usuarioId = (int) session.getAttribute("usuarioId");

                // Qué hace: Ejecuta el servicio para cambiar el estado del plan familiar.
                // Por qué existe: Actualiza el estado en MySQL e inserta una fila en la tabla seguimiento_planes.
                // Qué problema resuelve: Modifica el estado del plan de censo.
                String resJson = vulServicio.cambiarEstadoPlan(planId, statusPlanId, comentary, usuarioId);
                
                // =========================================
                // CREAR NOTIFICACIÓN PARA VOLUNTARIO
                // =========================================
                // Qué hace: Abre un bloque de código para alertar al voluntario dueño del plan sobre cambios.
                // Por qué existe: Mantiene informado al voluntario sobre la aprobación o rechazo de su censo.
                // Qué problema resuelve: Emplea JDBC para consultar la base de datos y crear una notificación estructurada.
                try {
                    PlanFamiliarDAO planDAO = new PlanFamiliarDAO();
                    NotificacionDAO notificacionDAO = new NotificacionDAO();
                    
                    // Obtener el voluntario dueño del plan
                    // Qué hace: Prepara la consulta SQL para seleccionar el ID del voluntario asignado al plan.
                    // Por qué existe: Identifica a qué usuario se debe dirigir la notificación.
                    // Qué problema resuelve: Busca la llave foránea voluntario_id en la tabla planes_familiares filtrada por id.
                    String sql = "SELECT voluntario_id FROM planes_familiares WHERE id = ?";
                    // Qué hace: Obtiene la conexión JDBC activa del pool de conexiones.
                    java.sql.Connection con = Modelo.Config.Conexion.obtener();
                    // Qué hace: Compila la sentencia preparada para evitar inyecciones SQL.
                    java.sql.PreparedStatement ps = con.prepareStatement(sql);
                    // Qué hace: Vincula el ID del plan familiar en el marcador de posición de la consulta.
                    ps.setInt(1, planId);
                    // Qué hace: Ejecuta la consulta select en MySQL.
                    java.sql.ResultSet rs = ps.executeQuery();
                    
                    // Qué hace: Evalúa si la consulta devolvió una fila de resultados.
                    // Por qué existe: Recupera el valor del campo si el plan existe en base de datos.
                    // Qué problema resuelve: Obtiene el ID del destinatario de la notificación.
                    if (rs.next()) {
                        int voluntarioId = rs.getInt("voluntario_id");
                        
                        // Crear notificación solo si el estado cambió a aprobado (6) o rechazado (7)
                        // Qué hace: Verifica si el nuevo estado es aprobado o rechazado.
                        // Por qué existe: Solo estas transiciones de estado ameritan notificar al voluntario.
                        // Qué problema resuelve: Evita notificaciones ruidosas en estados intermedios.
                        if (statusPlanId == 6 || statusPlanId == 7) {
                            NotificacionDTO notificacion = new NotificacionDTO();
                            notificacion.setUsuarioId(voluntarioId);
                            notificacion.setTitulo(statusPlanId == 6 ? "Plan Aprobado" : "Plan Rechazado");
                            notificacion.setMensaje(statusPlanId == 6 
                                ? "Tu plan familiar ha sido aprobado exitosamente" 
                                : "Tu plan familiar ha sido rechazado. " + (comentary != null ? comentary : ""));
                            notificacion.setTipo("plan_estado");
                            notificacion.setLeida(false);
                            notificacion.setEnlace("#/voluntario/plan_familiar");
                            notificacion.setEntidadId(planId);
                            // Qué hace: Inserta el registro de notificación en la tabla de base de datos.
                            // Por qué existe: Persiste la alerta en el buzón del voluntario.
                            // Qué problema resuelve: Guarda la alerta en MySQL.
                            notificacionDAO.crear(notificacion);
                        }
                    }
                    
                    // Qué hace: Cierra los recursos JDBC utilizados para liberar memoria y conexiones en el servidor.
                    // Por qué existe: Previene fugas de recursos y saturación de la base de datos MySQL.
                    // Qué problema resuelve: Libera de forma segura el ResultSet, PreparedStatement y Connection.
                    rs.close();
                    ps.close();
                    con.close();
                } catch (Exception e) {
                    // No fallar el cambio de estado si la notificación falla
                    System.err.println("Error al crear notificación: " + e.getMessage());
                }
                
                // Qué hace: Envía la respuesta JSON al cliente.
                response.getWriter().write(resJson);
            } 
            // Escenario 2: Guardar los datos de identificación detallados de la vivienda (PATCH de identificación)
            // Sirve para: Interceptar la petición de actualización de datos de la vivienda y del sector
            // Qué hace: Lee el stream del JSON body, parsea dirección, sector, barrio, zona, teléfono y calidad, y llama al servicio
            // Por qué es importante: Permite persistir los cambios hechos en el formulario del frontend de forma estructurada
            else if (accion.equals("identify")) {
                // Qué hace: Lee el cuerpo JSON de la petición HTTP.
                // Por qué existe: Recupera la información del formulario de la vivienda.
                // Qué problema resuelve: Convierte el flujo de bytes a un buffer de texto.
                StringBuilder buffer = new StringBuilder();
                String linea;
                try (BufferedReader reader = request.getReader()) {
                    while ((linea = reader.readLine()) != null) {
                        buffer.append(linea);
                    }
                }

                // Parsea el JSON del cuerpo
                JSONObject json = new JSONObject(buffer.toString());
                
                // Instancia el DTO para capturar los datos de la vivienda
                ActualizarIdentificacionDTO dto = new ActualizarIdentificacionDTO();
                // Qué hace: Asigna los apellidos familiares modificados al DTO.
                dto.setLastNames(json.getString("last_names"));
                // Qué hace: Asigna la dirección de la vivienda.
                dto.setAddress(json.getString("address"));
                // Qué hace: Asigna el identificador numérico de la calidad de vivienda.
                dto.setHousingQualityId(json.getInt("housing_quality_id"));
                // Qué hace: Asigna el identificador numérico de sector.
                dto.setSectorId(json.getInt("sector_id"));
                // Qué hace: Asigna el nombre de barrio/sector alternativo.
                dto.setSectorName(json.getString("sector_name"));
                // Qué hace: Asigna el teléfono fijo de la vivienda.
                dto.setLandlinePhone(json.optString("landline_phone"));
                // Qué hace: Asigna la zona de la vivienda.
                dto.setZoneId(json.optInt("zone_id", 0));

                // Qué hace: Llama al servicio para guardar los datos de identificación de la vivienda.
                // Por qué existe: Realiza la consulta SQL UPDATE en la tabla planes_familiares.
                // Qué problema resuelve: Modifica de forma permanente los datos del censo familiar en MySQL.
                String resJson = planServicio.guardarIdentificacion(planId, dto);
                response.getWriter().write(resJson);
            }
            // Si la acción solicitada no está implementada
            else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write(new JSONObject().put("success", false).put("message", "Acción no reconocida.").toString());
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "ID del plan debe ser un número entero.").toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("success", false).put("message", "Error al procesar la petición PATCH: " + e.getMessage()).toString());
        }
    }
}