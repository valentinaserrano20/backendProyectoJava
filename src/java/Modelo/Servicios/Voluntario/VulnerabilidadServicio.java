package Modelo.Servicios.Voluntario;

import Modelo.DAO.VulnerabilidadDAO;
import Modelo.Entidades.PreguntaTest;
import Modelo.DTO.RespuestaTestDTO;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

// Capa de Servicio para coordinar las operaciones lógicas y serialización JSON del Test de Vulnerabilidad
public class VulnerabilidadServicio {
    // Instancia el DAO correspondiente para interactuar con la base de datos
    private final VulnerabilidadDAO dao = new VulnerabilidadDAO();

    // Obtiene todas las preguntas activas como entidades y las serializa a formato DTO JSON con el nodo "data"
    public String obtenerPreguntas() {
        // Inicializa el objeto JSON de respuesta principal
        JSONObject res = new JSONObject();
        try {
            // Solicita al DAO la lista completa de preguntas activas (retorna objetos Entidad PreguntaTest)
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
            // Si ocurre algún fallo, marca éxito como false
            res.put("success", false);
            // Inserta el mensaje detallado del error
            res.put("message", "Error al obtener las preguntas: " + e.getMessage());
        }
        // Devuelve la respuesta en formato de cadena JSON
        return res.toString();
    }

    // Obtiene las preguntas paginadas como entidades y devuelve el JSON estructurado con nodos "data" y "paginate"
    public String obtenerPreguntasPaginadas(int page, int perPage) {
        // Inicializa el objeto JSON principal
        JSONObject res = new JSONObject();
        try {
            // Obtiene el conteo total de preguntas activas en la BD
            int total = dao.contarPreguntasActivas();
            // Calcula matemáticamente el número de páginas necesarias redondeando hacia arriba
            int lastPage = (int) Math.ceil((double) total / perPage);
            // Asegura que la página solicitada no sea menor a 1
            if (page < 1) page = 1;
            // Calcula la fila inicial (offset) para la consulta SQL
            int offset = (page - 1) * perPage;

            // Pide al DAO la lista de entidades PreguntaTest paginadas
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

    // Obtiene respuestas existentes para precargarlas en el test de vulnerabilidad
    public String obtenerRespuestasPlan(int planId) {
        // Inicializa el objeto JSON de respuesta
        JSONObject res = new JSONObject();
        try {
            // Solicita al DAO la lista de respuestas existentes para el plan
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

    // Procesa el lote de respuestas, ejecuta el guardado en base de datos y calcula vulnerabilidad en memoria
    public String procesarGuardadoLote(int planId, List<RespuestaTestDTO> respuestas) {
        // Inicializa el objeto de respuesta JSON
        JSONObject res = new JSONObject();
        
        // Valida que el lote contenga datos válidos para procesar
        if (respuestas == null || respuestas.isEmpty()) {
            return res.put("success", false).put("message", "El test no tiene respuestas para guardar.").toString();
        }

        try {
            // Ejecuta el DAO transaccional para el lote
            boolean exito = dao.guardarTestYActualizarPlan(planId, respuestas);
            // Si la transacción en base de datos se consolida
            if (exito) {
                // Inicializa el conteo de puntos de vulnerabilidad en memoria
                int puntos = 0;
                // Recupera todas las preguntas activas (entidades) para evaluar la peligrosidad de las respuestas SÍ
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
    
    // Cambia el estado del plan familiar e inserta un registro en la bitácora de seguimiento
    public String cambiarEstadoPlan(int planId, int estadoId, String comentario, int usuarioId) {
        // Inicializa el JSON
        JSONObject res = new JSONObject();
        try {
            // Solicita al DAO actualizar el estado del plan familiar
            dao.actualizarEstadoPlan(planId, estadoId);
            
            // Registra el seguimiento con el comentario del supervisor y el ID de usuario gestor
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
