package Modelo.Servicios.Voluntario;

import Modelo.DAO.VulnerabilidadDAO;
import Modelo.Entidades.PreguntaTest;
import Modelo.DTO.RespuestaTestDTO;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

// Qué hace: Capa de Servicio encargada de coordinar las operaciones lógicas, validaciones y serialización JSON del Test de Vulnerabilidad familiar.
// Por qué existe: Separa la lógica de control del flujo web (Servlet) y el acceso físico a base de datos (DAO), asegurando que se calcule correctamente la vulnerabilidad del hogar antes de actualizar su estado.
// Qué pasaría si no estuviera: Los controladores tendrían que realizar consultas directas y cálculos manuales de vulnerabilidad, duplicando lógica y acoplando la base de datos con la vista web.
public class VulnerabilidadServicio {
    
    // Qué hace: Instancia el objeto de acceso a datos del test de vulnerabilidad.
    // Por qué existe: Permite consultar preguntas, registrar respuestas y actualizar estados en la base de datos relacional.
    // Qué pasaría si no estuviera: No podríamos recuperar las preguntas ni guardar los resultados del test en la base de datos.
    // Flujo: De aquí pasamos a VulnerabilidadDAO.
    private final VulnerabilidadDAO dao = new VulnerabilidadDAO();

    // Qué hace: Obtiene todas las preguntas del test que se encuentren activas y las empaqueta en una estructura JSON.
    // Por qué existe: Permite al frontend listar el cuestionario completo dinámicamente en el formulario de la SPA.
    // Qué pasaría si no estuviera: El cuestionario tendría que estar hardcodeado en el frontend, impidiendo agregar o modificar preguntas desde la base de datos sin redesplegar el cliente.
    public String obtenerPreguntas() {
        // Inicializa el objeto JSON de respuesta principal
        JSONObject res = new JSONObject();
        try {
            // Qué hace: Consulta al DAO las preguntas activas.
            // y luego de esto pasamos a VulnerabilidadDAO.obtenerPreguntasActivas, que realiza la consulta SELECT.
            List<PreguntaTest> preguntas = dao.obtenerPreguntasActivas();
            // Inicializa un arreglo JSON para almacenar las preguntas convertidas a DTO
            JSONArray datos = new JSONArray();
            // Recorre las entidades de preguntas devueltas
            for (PreguntaTest p : preguntas) {
                // Crea un objeto JSON para mapear las propiedades del DTO al frontend
                JSONObject obj = new JSONObject();
                // Inserta el ID de la pregunta
                obj.put("id", p.getId());
                // Mapea la columna enunciado al campo description del frontend
                obj.put("description", p.getEnunciado());
                // Mapea la inversa de esEvaluable a question_caution (true si no es evaluable)
                obj.put("question_caution", !p.isEsEvaluable());
                // Mapea la columna activo a is_active
                obj.put("is_active", p.isActivo());
                // Inserta el orden
                obj.put("orden", p.getOrden());
                // Agrega el objeto JSON del DTO al arreglo
                datos.put(obj);
            }
            // Agrega el indicador de éxito true
            res.put("success", true);
            // Agrega el arreglo de preguntas envuelto en el nodo "data" requerido por el frontend
            res.put("data", datos);
        } catch (Exception e) {
            // Si ocurre algún fallo, marca éxito como false y retorna la descripción
            res.put("success", false);
            // Inserta el mensaje detallado del error
            res.put("message", "Error al obtener las preguntas: " + e.getMessage());
        }
        // Devuelve la respuesta en formato de cadena JSON
        return res.toString();
    }

    // Qué hace: Obtiene un subconjunto de preguntas activas de forma paginada para la administración del test.
    // Por qué existe: Evita la sobrecarga de red al transferir listas masivas de preguntas en una sola petición.
    // Qué pasaría si no estuviera: Las interfaces administrativas de preguntas cargarían lento al procesar todo de golpe.
    public String obtenerPreguntasPaginadas(int page, int perPage) {
        // Inicializa el objeto JSON principal
        JSONObject res = new JSONObject();
        try {
            // Qué hace: Cuenta el total de preguntas para calcular el número de páginas.
            // y luego de esto pasamos a VulnerabilidadDAO.contarPreguntasActivas.
            int total = dao.contarPreguntasActivas();
            // Calcula matemáticamente el número de páginas necesarias redondeando hacia arriba
            int lastPage = (int) Math.ceil((double) total / perPage);
            // Asegura que la página solicitada no sea menor a 1
            if (page < 1) page = 1;
            // Calcula la fila inicial (offset) para la consulta SQL
            int offset = (page - 1) * perPage;

            // Qué hace: Obtiene la lista de entidades PreguntaTest paginadas.
            // y luego de esto pasamos a VulnerabilidadDAO.obtenerPreguntasPaginadas.
            List<PreguntaTest> preguntas = dao.obtenerPreguntasPaginadas(offset, perPage);
            // Crea el arreglo JSON para los datos DTO de la página
            JSONArray datos = new JSONArray();
            // Recorre las entidades obtenidas
            for (PreguntaTest p : preguntas) {
                // Serializa y mapea la entidad al DTO JSON del frontend
                JSONObject obj = new JSONObject();
                obj.put("id", p.getId());
                obj.put("description", p.getEnunciado());
                obj.put("question_caution", !p.isEsEvaluable());
                obj.put("is_active", p.isActivo());
                obj.put("orden", p.getOrden());
                // Agrega al arreglo
                datos.put(obj);
            }

            // Crea un objeto JSON para la metadata de la paginación
            JSONObject paginate = new JSONObject();
            // Inserta la cantidad de páginas totales
            paginate.put("last_page", lastPage);
            // Inserta la página actual
            paginate.put("current_page", page);
            // Inserta la cantidad de elementos por página
            paginate.put("per_page", perPage);
            // Inserta el total de elementos activos
            paginate.put("total", total);

            // Marca la respuesta como exitosa
            res.put("success", true);
            // Inserta el arreglo en "data"
            res.put("data", datos);
            // Inserta los metadatos de paginación en el nodo "paginate"
            res.put("paginate", paginate);
        } catch (Exception e) {
            // Captura errores e inyecta la descripción del fallo
            res.put("success", false);
            res.put("message", "Error en la paginación: " + e.getMessage());
        }
        // Retorna la cadena JSON
        return res.toString();
    }

    // Qué hace: Obtiene las respuestas guardadas previamente en un plan familiar específico.
    // Por qué existe: Permite precargar las respuestas en el cuestionario cuando el voluntario vuelve a ingresar al test.
    // Qué pasaría si no estuviera: El usuario tendría que responder todas las preguntas nuevamente cada vez que entre a la pestaña del test.
    public String obtenerRespuestasPlan(int planId) {
        // Inicializa el objeto JSON de respuesta
        JSONObject res = new JSONObject();
        try {
            // Qué hace: Recupera las respuestas guardadas para el plan.
            // y luego de esto pasamos a VulnerabilidadDAO.obtenerRespuestasPorPlan, el cual hace un SELECT de las respuestas.
            List<RespuestaTestDTO> respuestas = dao.obtenerRespuestasPorPlan(planId);
            // Crea el arreglo JSON de respuestas
            JSONArray datos = new JSONArray();
            // Recorre las respuestas DTO recuperadas
            for (RespuestaTestDTO r : respuestas) {
                // Mapea la respuesta a un objeto JSON
                JSONObject obj = new JSONObject();
                // Inserta el ID de la pregunta
                obj.put("vulnerable_question_id", r.getVulnerableQuestionId());
                // Inserta el valor booleano
                obj.put("answer", r.isAnswer());
                // Agrega al arreglo de datos
                datos.put(obj);
            }
            // Agrega el éxito
            res.put("success", true);
            // Agrega el arreglo en "data" para el mapeo del frontend
            res.put("data", datos);
        } catch (Exception e) {
            // Captura fallos e informa al cliente
            res.put("success", false);
            res.put("message", "Error al precargar las respuestas: " + e.getMessage());
        }
        // Devuelve el JSON en String
        return res.toString();
    }

    // Qué hace: Guarda transaccionalmente el lote de respuestas del test y actualiza en caliente el tipo de vulnerabilidad familiar calculado.
    // Por qué existe: Consolida las respuestas del test y determina si la familia califica en condición vulnerable (5 o más respuestas 'Sí' evaluables).
    // Qué pasaría si no estuviera: No se podrían registrar las respuestas del test en lote ni se actualizaría el estado de vulnerabilidad de la familia de forma automática.
    public String procesarGuardadoLote(int planId, List<RespuestaTestDTO> respuestas) {
        // Inicializa el objeto de respuesta JSON
        JSONObject res = new JSONObject();
        
        // Valida que el lote contenga datos válidos para procesar
        if (respuestas == null || respuestas.isEmpty()) {
            return res.put("success", false).put("message", "El test no tiene respuestas para guardar.").toString();
        }

        try {
            // Qué hace: Ejecuta la transacción de borrado de respuestas anteriores e inserción de las nuevas en lote.
            // y luego de esto pasamos a VulnerabilidadDAO.guardarTestYActualizarPlan.
            boolean exito = dao.guardarTestYActualizarPlan(planId, respuestas);
            // Si la transacción en base de datos se consolida
            if (exito) {
                // Inicializa el conteo de puntos de vulnerabilidad en memoria
                int puntos = 0;
                // Qué hace: Obtiene de nuevo las preguntas activas para evaluar cuáles de las marcadas como 'SÍ' son evaluables.
                // y luego de esto pasamos a VulnerabilidadDAO.obtenerPreguntasActivas.
                List<PreguntaTest> todas = dao.obtenerPreguntasActivas();
                // Itera sobre las respuestas recibidas en el lote
                for (RespuestaTestDTO r : respuestas) {
                    // Si el voluntario marcó que SÍ
                    if (r.isAnswer()) {
                        // Verifica si la pregunta es evaluable (es decir, esEvaluable = true)
                        boolean esEvaluable = true;
                        for (PreguntaTest p : todas) {
                            if (p.getId() == r.getVulnerableQuestionId()) {
                                // Mapea directamente desde la columna esEvaluable de la entidad
                                esEvaluable = p.isEsEvaluable();
                                break;
                            }
                        }
                        // Si la pregunta es evaluable en la entidad, suma 1 punto de riesgo
                        if (esEvaluable) {
                            puntos++;
                        }
                    }
                }

                // Genera el mensaje dinámico para el voluntario en base a la vulnerabilidad calificada
                String msg = "El test de vulnerabilidad se ha guardado correctamente. La familia ha sido catalogada como " + 
                             (puntos >= 5 ? "VULNERABLE." : "NO VULNERABLE.");
                
                // Agrega el éxito de la petición
                res.put("success", true);
                // Agrega el mensaje final
                res.put("message", msg);
                // Devuelve los puntos obtenidos
                res.put("puntos", puntos);
                // Indica el tipo de familia id (1 = Vulnerable, 2 = No vulnerable)
                res.put("family_type_id", puntos >= 5 ? 1 : 2);
            } else {
                // Informa si falló
                res.put("success", false);
                res.put("message", "No se pudo guardar el test.");
            }
        } catch (Exception e) {
            // Captura y propaga la descripción del error técnico en base de datos
            res.put("success", false);
            res.put("message", "Error al persistir respuestas: " + e.getMessage());
        }
        // Devuelve el JSON
        return res.toString();
    }
    
    // Qué hace: Actualiza el estado de revisión del plan familiar (Ej: de 'En revisión' a 'Aprobado') y deja constancia en la bitácora de seguimiento.
    // Por qué existe: Permite a los supervisores de la Defensa Civil auditar, aprobar o rechazar planes con comentarios específicos.
    // Qué pasaría si no estuviera: Los planes se quedarían en un solo estado indefinidamente y no habría historial de quién aprobó o rechazó qué cosa.
    public String cambiarEstadoPlan(int planId, int estadoId, String comentario, int usuarioId) {
        // Inicializa el JSON
        JSONObject res = new JSONObject();
        try {
            // Qué hace: Ejecuta la sentencia de actualización del estado del plan.
            // y luego de esto pasamos a VulnerabilidadDAO.actualizarEstadoPlan.
            dao.actualizarEstadoPlan(planId, estadoId);
            
            // Qué hace: Inserta el registro de seguimiento en la bitácora.
            // y luego de esto pasamos a VulnerabilidadDAO.registrarSeguimiento.
            dao.registrarSeguimiento(planId, usuarioId, estadoId, comentario);
            
            // Agrega éxito y el mensaje informativo
            res.put("success", true);
            res.put("message", "Estado del plan familiar actualizado con éxito.");
        } catch (Exception e) {
            // Captura errores y responde false
            res.put("success", false);
            res.put("message", "Error al actualizar estado: " + e.getMessage());
        }
        // Devuelve la respuesta en formato de cadena JSON
        return res.toString();
    }
}
