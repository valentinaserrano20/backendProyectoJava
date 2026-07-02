package Modelo.DAO;

/*
 * Qué hace (la acción): Importa la conexión física de base de datos MySQL, utilidades JDBC estándar, colecciones en Java y las clases JSONObject / JSONArray para parsear información en formato JSON.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - org.json.JSONObject / JSONArray: Librería para procesar y estructurar datos en formato JSON directo desde y hacia las peticiones HTTP.
 *   - java.sql.*: Clases para gestionar consultas preparadas, conexiones y lectura de datos relacionales en MySQL.
 * Para qué se usa (el propósito): Servir como base técnica para ejecutar consultas y serializar resultados de forma genérica para la capa de datos maestros.
 * Por qué es importante (el impacto o problema que resuelve): Permite que un solo DAO administre múltiples catálogos dinámicamente mediante el uso de estructuras de datos flexibles como Maps y JSON.
 */
import Modelo.Config.Conexion;
import java.sql.*;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

/*
 * Qué hace (la acción): Define la clase DatoMaestroDAO que implementa operaciones CRUD y auditoría genéricas sobre las doce tablas paramétricas del sistema.
 * Qué significa (conceptos, métodos, tipos involucrados):
 *   - CRUD genérico: Operaciones de Crear, Leer, Actualizar y Borrar estructuradas dinámicamente por nombre de entidad.
 * Para qué se usa (el propósito): Simplificar y unificar la persistencia de datos maestros evitando escribir doce clases DAO distintas.
 * Por qué es importante (el impacto o problema que resuelve): Reduce significativamente las líneas de código duplicadas y centraliza el mantenimiento de la integridad referencial y auditoría de catálogos.
 */
public class DatoMaestroDAO {

    /*
     * Qué hace (la acción): Registra de manera cronológica en la bitácora de auditoría cualquier cambio (inserción, edición, desactivación) realizado sobre los datos maestros.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - historial_datos_maestros: Tabla de auditoría interna de la base de datos.
     *   - ps.executeUpdate(): Ejecuta la inserción física del registro de auditoría.
     * Para qué se usa (el propósito): Monitorear qué supervisor u operador realizó cambios sobre la configuración paramétrica del sistema.
     * Por qué es importante (el impacto o problema que resuelve): Cumple con los requerimientos de seguridad y trazabilidad del sistema, permitiendo identificar responsables y valores anteriores en caso de fallos.
     */
    public void registrarAuditoria(String tabla, String accion, String valorAnterior, String valorNuevo, int usuarioId) throws SQLException {
        String sql = "INSERT INTO historial_datos_maestros (tabla_afectada, accion, valor_anterior, valor_nuevo, usuario_id) VALUES (?, ?, ?, ?, ?)";
        // Qué hace: Abre la conexión JDBC limpia y prepara el statement de inserción.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Enlaza secuencialmente los 5 parámetros descriptivos.
            ps.setString(1, tabla);
            ps.setString(2, accion);
            ps.setString(3, valorAnterior);
            ps.setString(4, valorNuevo);
            ps.setInt(5, usuarioId);
            // Qué hace: Ejecuta la inserción en la base de datos MySQL.
            ps.executeUpdate();
        }
    }

    /*
     * Qué hace (la acción): Recupera el historial de modificaciones realizadas exclusivamente sobre un registro específico de un catálogo particular.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - CONCAT(u.nombre, ' ', u.apellido): Junta nombre y apellido del usuario auditor en una sola columna.
     *   - SimpleDateFormat("dd/MM/yyyy HH:mm"): Formatea la fecha y hora para que sea fácilmente legible por el usuario en el frontend.
     * Para qué se usa (el propósito): Cargar el panel de trazabilidad de cambios en los formularios de edición de datos maestros.
     * Por qué es importante (el impacto o problema que resuelve): Permite ver el ciclo de vida del dato desde su creación hasta su último estado sin tener que buscar manualmente en los logs del servidor.
     */
    public List<Map<String, Object>> obtenerHistorial(String tabla, int registroId) throws SQLException {
        String sql = "SELECT h.accion, h.valor_anterior, h.valor_nuevo, h.fecha, "
                   + "CONCAT(u.nombre, ' ', u.apellido) as user_name, r.nombre as rol_nombre "
                   + "FROM historial_datos_maestros h "
                   + "JOIN usuarios u ON h.usuario_id = u.id "
                   + "JOIN roles r ON u.rol_id = r.id "
                   + "WHERE h.tabla_afectada = ? "
                   + "ORDER BY h.fecha DESC";
        
        List<Map<String, Object>> resultado = new ArrayList<>();
        // Qué hace: Abre la conexión a la base de datos MySQL y compila el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Enlaza el nombre de la tabla paramétrica en el filtro WHERE.
            ps.setString(1, tabla);
            // Qué hace: Ejecuta la consulta y lee el ResultSet.
            try (ResultSet rs = ps.executeQuery()) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
                while (rs.next()) {
                    String valAnt = rs.getString("valor_anterior");
                    String valNvo = rs.getString("valor_nuevo");
                    
                    // Filtrar por ID del registro codificado en la auditoría
                    boolean coincide = false;
                    String idBuscado = "id: " + registroId;
                    if (valAnt != null && valAnt.contains(idBuscado)) {
                        coincide = true;
                    }
                    if (valNvo != null && valNvo.contains(idBuscado)) {
                        coincide = true;
                    }
                    
                    // Si coincide con el ID buscado, se añade el log al resultado formateado
                    if (coincide) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("action_execute", rs.getString("accion"));
                        map.put("user_name", rs.getString("user_name"));
                        map.put("rol", rs.getString("rol_nombre"));
                        map.put("date_time", rs.getTimestamp("fecha") != null ? sdf.format(rs.getTimestamp("fecha")) : "");
                        map.put("status_old", valAnt);
                        map.put("status_new", valNvo);
                        map.put("name_model", tabla);
                        resultado.add(map);
                    }
                }
            }
        }
        return resultado;
    }

    /*
     * Qué hace (la acción): Extrae la lista única de seccionales geográficas (ej: Santander, Bucaramanga) basándose en las organizaciones registradas.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - SELECT DISTINCT seccional: Filtra duplicados para que cada seccional aparezca una sola vez.
     * Para qué se usa (el propósito): Alimentar la lista de seccionales disponibles sin tener una tabla exclusiva para ello.
     * Por qué es importante (el impacto o problema que resuelve): Mantiene la consistencia de datos geográficos basándose directamente en la distribución de las organizaciones vigentes.
     */
    public List<String> obtenerNombresSeccionales() throws SQLException {
        String sql = "SELECT DISTINCT seccional FROM organizaciones ORDER BY seccional ASC";
        List<String> seccionales = new ArrayList<>();
        // Qué hace: Obtiene la conexión JDBC y ejecuta la consulta directa.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                // Qué hace: Agrega el nombre de la seccional a la lista.
                seccionales.add(rs.getString("seccional"));
            }
        }
        return seccionales;
    }

    /*
     * Qué hace (la acción): Determina el ID virtual (1-based) asignado a un nombre de seccional.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - i + 1: Conversión del índice base cero a una clave numérica base uno.
     * Para qué se usa (el propósito): Mapear el texto plano de seccionales a identificadores numéricos que el cliente web pueda manejar fácilmente en el select.
     */
    public int obtenerIdSeccional(String seccional) throws SQLException {
        List<String> lista = obtenerNombresSeccionales();
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).equalsIgnoreCase(seccional)) {
                return i + 1; 
            }
        }
        return -1;
    }

    /*
     * Qué hace (la acción): Recupera el nombre de la seccional correspondiente a un ID virtual.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - lista.get(id - 1): Obtiene el elemento restando 1 al ID virtual recibido.
     * Para qué se usa (el propósito): Traducir el ID numérico enviado por el frontend al String correspondiente al persistir en base de datos.
     */
    public String obtenerNombreSeccionalPorId(int id) throws SQLException {
        List<String> lista = obtenerNombresSeccionales();
        if (id >= 1 && id <= lista.size()) {
            return lista.get(id - 1);
        }
        return null;
    }

    // =========================================================================
    // MÉTODOS CRUD GENÉRICOS
    // =========================================================================

    // Sirve para: Obtener el listado completo de registros para cualquiera de las 12 entidades paramétricas (catálogos).
    // Qué hace: Según el parámetro 'entidad', arma dinámicamente la consulta SELECT correspondiente y mapea las columnas a un HashMap flexible.
    // Explicación de consultas SQL dinámicas:
    // - Para 'departments': Retorna una lista estática (fija) con Santander (ID 1).
    // - Para 'sectionals': Invoca a obtenerNombresSeccionales() (SELECT DISTINCT) y arma la lista virtual.
    // - Para 'sectors': SELECT id, nombre, activo FROM sectores ORDER BY id DESC (obtiene sectores ordenados por ID descendente).
    // - Para 'organizations': SELECT id, nombre, seccional, activo FROM organizaciones ORDER BY id DESC.
    // - Para 'documentTypes': SELECT id, sigla, descripcion, activo FROM tipo_documentos ORDER BY id DESC (sigla descriptiva y descripción completa).
    // - Para 'housingQualities': SELECT id, nombre, activo FROM calidad_vivienda ORDER BY id DESC.
    // - Para 'vulnerableQuestions': SELECT id, enunciado, es_evaluable, orden, activo FROM preguntas_test ORDER BY orden ASC, id ASC.
    // - Para 'nationalities': SELECT id, nombre, activo FROM nacionalidades ORDER BY id DESC.
    // - Para 'threatTypes': SELECT id, nombre, activo FROM amenazas ORDER BY id DESC.
    // - Para 'species': SELECT id, nombre, activo FROM especies_mascota ORDER BY id DESC (catálogo de especies de mascotas).
    // - Para 'resources': SELECT r.id, r.nombre, s.nombre AS servicio, r.activo FROM tipos_recurso r LEFT JOIN servicios_emergencia s ON r.servicio_id = s.id ORDER BY r.id DESC.
    //   * Cruza la tabla tipos_recurso (r) con servicios_emergencia (s) mediante un LEFT JOIN relacionando r.servicio_id = s.id para traer el nombre legible de la categoría del servicio.
    // - Para 'vulnerabilities': SELECT id, nombre, activo FROM vulnerabilidades ORDER BY id DESC.
    public List<Map<String, Object>> listar(String entidad) throws SQLException {
        List<Map<String, Object>> resultado = new ArrayList<>();
        
        if (entidad.equalsIgnoreCase("departments")) {
            // Hardcodeado: Solo lectura del departamento por defecto
            Map<String, Object> dept = new HashMap<>();
            dept.put("id", 1);
            dept.put("name", "Santander");
            dept.put("is_active", true);
            resultado.add(dept);
            return resultado;
        }

        if (entidad.equalsIgnoreCase("sectionals")) {
            List<String> nombres = obtenerNombresSeccionales();
            for (int i = 0; i < nombres.size(); i++) {
                Map<String, Object> sec = new HashMap<>();
                sec.put("id", i + 1);
                sec.put("name", nombres.get(i));
                sec.put("is_active", true); // Las seccionales son activas por defecto
                resultado.add(sec);
            }
            return resultado;
        }

        String sql = "";
        if (entidad.equalsIgnoreCase("sectors")) {
            sql = "SELECT id, nombre, activo FROM sectores ORDER BY id DESC";
        } else if (entidad.equalsIgnoreCase("organizations")) {
            sql = "SELECT id, nombre, seccional, activo FROM organizaciones ORDER BY id DESC";
        } else if (entidad.equalsIgnoreCase("documentTypes")) {
            sql = "SELECT id, sigla, descripcion, activo FROM tipo_documentos ORDER BY id DESC";
        } else if (entidad.equalsIgnoreCase("housingQualities")) {
            sql = "SELECT id, nombre, activo FROM calidad_vivienda ORDER BY id DESC";
        } else if (entidad.equalsIgnoreCase("vulnerableQuestions")) {
            sql = "SELECT id, enunciado, es_evaluable, orden, activo FROM preguntas_test ORDER BY orden ASC, id ASC";
        } else if (entidad.equalsIgnoreCase("nationalities")) {
            sql = "SELECT id, nombre, activo FROM nacionalidades ORDER BY id DESC";
        } else if (entidad.equalsIgnoreCase("threatTypes")) {
            sql = "SELECT id, nombre, activo FROM amenazas ORDER BY id DESC";
        } else if (entidad.equalsIgnoreCase("species")) {
            sql = "SELECT id, nombre, activo FROM especies_mascota ORDER BY id DESC";
        } else if (entidad.equalsIgnoreCase("resources")) {
            sql = "SELECT r.id, r.nombre, s.nombre AS servicio, r.activo FROM tipos_recurso r LEFT JOIN servicios_emergencia s ON r.servicio_id = s.id ORDER BY r.id DESC";
        } else if (entidad.equalsIgnoreCase("vulnerabilities")) {
            sql = "SELECT id, nombre, activo FROM vulnerabilidades ORDER BY id DESC";
        }

        if (sql.isEmpty()) return resultado;

        // Qué hace: Abre la conexión y prepara la consulta compilada en base de datos.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                // Qué hace: Mapea cada fila a un objeto Map con las llaves que el frontend JS espera recibir.
                Map<String, Object> item = new HashMap<>();
                item.put("id", rs.getInt("id"));
                
                if (entidad.equalsIgnoreCase("sectors") || entidad.equalsIgnoreCase("housingQualities") ||
                    entidad.equalsIgnoreCase("nationalities") || entidad.equalsIgnoreCase("threatTypes") ||
                    entidad.equalsIgnoreCase("species") || entidad.equalsIgnoreCase("vulnerabilities")) {
                    item.put("name", rs.getString("nombre"));
                    item.put("is_active", rs.getBoolean("activo"));
                } 
                else if (entidad.equalsIgnoreCase("organizations")) {
                    item.put("name", rs.getString("nombre"));
                    item.put("sectional_id", obtenerIdSeccional(rs.getString("seccional")));
                    item.put("is_active", rs.getBoolean("activo"));
                }
                else if (entidad.equalsIgnoreCase("documentTypes")) {
                    item.put("acronym", rs.getString("sigla"));
                    item.put("name", rs.getString("descripcion"));
                    item.put("is_active", rs.getBoolean("activo"));
                }
                else if (entidad.equalsIgnoreCase("vulnerableQuestions")) {
                    item.put("description", rs.getString("enunciado"));
                    item.put("question_caution", !rs.getBoolean("es_evaluable"));
                    item.put("is_active", rs.getBoolean("activo"));
                }
                else if (entidad.equalsIgnoreCase("resources")) {
                    item.put("name", rs.getString("nombre"));
                    item.put("service", rs.getString("servicio") != null ? rs.getString("servicio") : "Otro");
                    item.put("is_active", rs.getBoolean("activo"));
                }
                
                resultado.add(item);
            }
        }
        return resultado;
    }

    // Sirve para: Obtener un único registro coincidente por su clave primaria (ID) para cualquiera de los catálogos.
    // Qué hace: Selecciona de forma condicional la consulta SQL de la entidad elegida, inyectando el ID como parámetro del PreparedStatement.
    // Explicación de consultas SQL con filtro ID:
    // - Para 'departments': Retorna el departamento fijo de Santander si id == 1.
    // - Para 'sectionals': Mapea el nombre a partir del índice virtual i = id - 1.
    // - Para 'sectors': SELECT id, nombre, activo FROM sectores WHERE id = ?.
    // - Para 'organizations': SELECT id, nombre, seccional, activo FROM organizaciones WHERE id = ?.
    // - Para 'documentTypes': SELECT id, sigla, descripcion, activo FROM tipo_documentos WHERE id = ?.
    // - Para 'housingQualities': SELECT id, nombre, activo FROM calidad_vivienda WHERE id = ?.
    // - Para 'vulnerableQuestions': SELECT id, enunciado, es_evaluable, orden, activo FROM preguntas_test WHERE id = ?.
    // - Para 'nationalities': SELECT id, nombre, activo FROM nacionalidades WHERE id = ?.
    // - Para 'threatTypes': SELECT id, nombre, activo FROM amenazas WHERE id = ?.
    // - Para 'species': SELECT id, nombre, activo FROM especies_mascota WHERE id = ?.
    // - Para 'resources': SELECT r.id, r.nombre, s.nombre AS servicio, r.activo FROM tipos_recurso r LEFT JOIN servicios_emergencia s ON r.servicio_id = s.id WHERE r.id = ?.
    //   * Cruza tipos_recurso y servicios_emergencia relacionando r.servicio_id = s.id mediante un LEFT JOIN filtrando por r.id = ?.
    // - Para 'vulnerabilities': SELECT id, nombre, activo FROM vulnerabilidades WHERE id = ?.
    public Map<String, Object> obtenerPorId(String entidad, int id) throws SQLException {
        if (entidad.equalsIgnoreCase("departments")) {
            if (id == 1) {
                Map<String, Object> dept = new HashMap<>();
                dept.put("id", 1);
                dept.put("name", "Santander");
                dept.put("is_active", true);
                return dept;
            }
            return null;
        }

        if (entidad.equalsIgnoreCase("sectionals")) {
            String name = obtenerNombreSeccionalPorId(id);
            if (name != null) {
                Map<String, Object> sec = new HashMap<>();
                sec.put("id", id);
                sec.put("name", name);
                sec.put("is_active", true);
                return sec;
            }
            return null;
        }

        String sql = "";
        if (entidad.equalsIgnoreCase("sectors")) {
            sql = "SELECT id, nombre, activo FROM sectores WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("organizations")) {
            sql = "SELECT id, nombre, seccional, activo FROM organizaciones WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("documentTypes")) {
            sql = "SELECT id, sigla, descripcion, activo FROM tipo_documentos WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("housingQualities")) {
            sql = "SELECT id, nombre, activo FROM calidad_vivienda WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("vulnerableQuestions")) {
            sql = "SELECT id, enunciado, es_evaluable, orden, activo FROM preguntas_test WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("nationalities")) {
            sql = "SELECT id, nombre, activo FROM nacionalidades WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("threatTypes")) {
            sql = "SELECT id, nombre, activo FROM amenazas WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("species")) {
            sql = "SELECT id, nombre, activo FROM especies_mascota WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("resources")) {
            sql = "SELECT r.id, r.nombre, s.nombre AS servicio, r.activo FROM tipos_recurso r LEFT JOIN servicios_emergencia s ON r.servicio_id = s.id WHERE r.id = ?";
        } else if (entidad.equalsIgnoreCase("vulnerabilities")) {
            sql = "SELECT id, nombre, activo FROM vulnerabilidades WHERE id = ?";
        }

        if (sql.isEmpty()) return null;

        // Qué hace: Abre la conexión a la base de datos MySQL y compila la sentencia parametrizada.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Enlaza el ID del registro en el statement de consulta.
            ps.setInt(1, id);
            // Qué hace: Ejecuta la consulta y lee el ResultSet.
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", rs.getInt("id"));
                    
                    if (entidad.equalsIgnoreCase("sectors") || entidad.equalsIgnoreCase("housingQualities") ||
                        entidad.equalsIgnoreCase("nationalities") || entidad.equalsIgnoreCase("threatTypes") ||
                        entidad.equalsIgnoreCase("species") || entidad.equalsIgnoreCase("vulnerabilities")) {
                        item.put("name", rs.getString("nombre"));
                        item.put("is_active", rs.getBoolean("activo"));
                    } 
                    else if (entidad.equalsIgnoreCase("organizations")) {
                        item.put("name", rs.getString("nombre"));
                        item.put("sectional_id", obtenerIdSeccional(rs.getString("seccional")));
                        item.put("is_active", rs.getBoolean("activo"));
                    }
                    else if (entidad.equalsIgnoreCase("documentTypes")) {
                        item.put("acronym", rs.getString("sigla"));
                        item.put("name", rs.getString("descripcion"));
                        item.put("is_active", rs.getBoolean("activo"));
                    }
                    else if (entidad.equalsIgnoreCase("vulnerableQuestions")) {
                        item.put("description", rs.getString("enunciado"));
                        item.put("question_caution", !rs.getBoolean("es_evaluable"));
                        item.put("is_active", rs.getBoolean("activo"));
                    }
                    else if (entidad.equalsIgnoreCase("resources")) {
                        item.put("name", rs.getString("nombre"));
                        item.put("service", rs.getString("servicio") != null ? rs.getString("servicio") : "Otro");
                        item.put("is_active", rs.getBoolean("activo"));
                    }
                    return item;
                }
            }
        }
        return null;
    }

    // Sirve para: Crear un nuevo registro físico en la tabla paramétrica seleccionada de forma dinámica.
    // Qué hace: Según el parámetro 'entidad', compila y ejecuta la sentencia INSERT adecuada inyectando las columnas del body JSON y retornando la clave primaria autogenerada.
    // Explicación de sentencias INSERT dinámicas:
    // - Para 'sectionals': INSERT INTO organizaciones (nombre, seccional, activo) VALUES (?, ?, TRUE) (crea una organización dummy con la seccional).
    // - Para 'sectors': INSERT INTO sectores (nombre, activo) VALUES (?, TRUE).
    // - Para 'organizations': INSERT INTO organizaciones (nombre, seccional, activo) VALUES (?, ?, TRUE).
    // - Para 'documentTypes': INSERT INTO tipo_documentos (descripcion, sigla, activo) VALUES (?, ?, TRUE).
    // - Para 'housingQualities': INSERT INTO calidad_vivienda (nombre, activo) VALUES (?, TRUE).
    // - Para 'vulnerableQuestions': INSERT INTO preguntas_test (enunciado, es_evaluable, orden, activo) VALUES (?, ?, ?, TRUE).
    //   * Consulta adicional de orden: SELECT COALESCE(MAX(orden), 0) + 1 FROM preguntas_test (obtiene el siguiente entero consecutivo de orden).
    // - Para 'nationalities': INSERT INTO nacionalidades (nombre, activo) VALUES (?, TRUE).
    // - Para 'threatTypes': INSERT INTO amenazas (nombre, activo) VALUES (?, TRUE).
    // - Para 'species': INSERT INTO especies_mascota (nombre, activo) VALUES (?, TRUE).
    // - Para 'resources': INSERT INTO tipos_recurso (nombre, servicio_id, activo) VALUES (?, ?, TRUE).
    // - Para 'vulnerabilities': INSERT INTO vulnerabilidades (nombre, activo) VALUES (?, TRUE).
    public int crear(String entidad, JSONObject body) throws SQLException {
        if (entidad.equalsIgnoreCase("sectionals")) {
            String name = body.getString("name");
            // Inserta una organización dummy para crear la seccional
            String sql = "INSERT INTO organizaciones (nombre, seccional, activo) VALUES (?, ?, TRUE)";
            try (Connection con = Conexion.obtener();
                 PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, "Seccional " + name);
                ps.setString(2, name);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        // Retornamos el id virtual de la seccional
                        return obtenerIdSeccional(name);
                    }
                }
            }
            return -1;
        }

        String sql = "";
        if (entidad.equalsIgnoreCase("sectors")) {
            sql = "INSERT INTO sectores (nombre, activo) VALUES (?, TRUE)";
        } else if (entidad.equalsIgnoreCase("organizations")) {
            sql = "INSERT INTO organizaciones (nombre, seccional, activo) VALUES (?, ?, TRUE)";
        } else if (entidad.equalsIgnoreCase("documentTypes")) {
            sql = "INSERT INTO tipo_documentos (descripcion, sigla, activo) VALUES (?, ?, TRUE)";
        } else if (entidad.equalsIgnoreCase("housingQualities")) {
            sql = "INSERT INTO calidad_vivienda (nombre, activo) VALUES (?, TRUE)";
        } else if (entidad.equalsIgnoreCase("vulnerableQuestions")) {
            sql = "INSERT INTO preguntas_test (enunciado, es_evaluable, orden, activo) VALUES (?, ?, ?, TRUE)";
        } else if (entidad.equalsIgnoreCase("nationalities")) {
            sql = "INSERT INTO nacionalidades (nombre, activo) VALUES (?, TRUE)";
        } else if (entidad.equalsIgnoreCase("threatTypes")) {
            sql = "INSERT INTO amenazas (nombre, activo) VALUES (?, TRUE)";
        } else if (entidad.equalsIgnoreCase("species")) {
            sql = "INSERT INTO especies_mascota (nombre, activo) VALUES (?, TRUE)";
        } else if (entidad.equalsIgnoreCase("resources")) {
            sql = "INSERT INTO tipos_recurso (nombre, servicio_id, activo) VALUES (?, ?, TRUE)";
        } else if (entidad.equalsIgnoreCase("vulnerabilities")) {
            sql = "INSERT INTO vulnerabilidades (nombre, activo) VALUES (?, TRUE)";
        }

        if (sql.isEmpty()) return -1;

        // Qué hace: Obtiene la conexión y prepara el statement de inserción configurado para devolver llaves autogeneradas.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            if (entidad.equalsIgnoreCase("sectors") || entidad.equalsIgnoreCase("housingQualities") ||
                entidad.equalsIgnoreCase("nationalities") || entidad.equalsIgnoreCase("threatTypes") ||
                entidad.equalsIgnoreCase("species") || entidad.equalsIgnoreCase("vulnerabilities")) {
                ps.setString(1, body.getString("name"));
            }
            else if (entidad.equalsIgnoreCase("organizations")) {
                ps.setString(1, body.getString("name"));
                int secId = body.getInt("sectional_id");
                String secName = obtenerNombreSeccionalPorId(secId);
                ps.setString(2, secName != null ? secName : "Santander");
            }
            else if (entidad.equalsIgnoreCase("documentTypes")) {
                ps.setString(1, body.getString("name"));
                ps.setString(2, body.getString("acronym"));
            }
            else if (entidad.equalsIgnoreCase("vulnerableQuestions")) {
                ps.setString(1, body.getString("description"));
                ps.setBoolean(2, !(body.optBoolean("question_caution", true) || body.optInt("question_caution", 0) == 1));
                
                // Conteo para asignar orden final
                int orden = 1;
                try (Statement st = con.createStatement();
                     ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(orden), 0) + 1 FROM preguntas_test")) {
                    if (rs.next()) orden = rs.getInt(1);
                }
                ps.setInt(3, orden);
            }
            else if (entidad.equalsIgnoreCase("resources")) {
                ps.setString(1, body.getString("name"));
                String servicioNombre = body.getString("service");
                int servicioId = obtenerOInsertarServicio(con, servicioNombre);
                ps.setInt(2, servicioId);
            }

            // Qué hace: Ejecuta la inserción en la base de datos MySQL.
            ps.executeUpdate();
            // Qué hace: Lee las llaves primarias autogeneradas.
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    // Qué hace: Retorna el ID numérico asignado de forma automática por la base de datos.
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    // Sirve para: Obtener el ID de un servicio de emergencia o insertarlo si no existe en la base de datos.
    // Qué hace: Ejecuta un SELECT para buscar el id por nombre. Si no existe, realiza un INSERT y retorna el id generado.
    // Explicación de consultas SQL:
    // - Selección: SELECT id FROM servicios_emergencia WHERE nombre = ?.
    // - Inserción: INSERT INTO servicios_emergencia (nombre) VALUES (?).
    private int obtenerOInsertarServicio(Connection con, String nombre) throws SQLException {
        String query = "SELECT id FROM servicios_emergencia WHERE nombre = ?";
        // Qué hace: Prepara y ejecuta la consulta de búsqueda.
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        String insert = "INSERT INTO servicios_emergencia (nombre) VALUES (?)";
        // Qué hace: Inserta el nuevo servicio de emergencia.
        try (PreparedStatement ps = con.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return 6; // Default a 'Otro' (id 6) si falla la inserción
    }

    // Sirve para: Modificar los valores de un registro existente por su ID en cualquiera de los catálogos.
    // Qué hace: Ejecuta una sentencia UPDATE dinámica sobre la tabla correspondiente mapeando los campos del cuerpo JSON recibido.
    // Explicación de sentencias UPDATE dinámicas:
    // - Para 'sectionals': UPDATE organizaciones SET seccional = ? WHERE seccional = ? (actualiza todas las organizaciones virtuales de esa seccional).
    // - Para 'sectors': UPDATE sectores SET nombre = ? WHERE id = ?.
    // - Para 'organizations': UPDATE organizaciones SET nombre = ?, seccional = ? WHERE id = ?.
    // - Para 'documentTypes': UPDATE tipo_documentos SET descripcion = ?, sigla = ? WHERE id = ?.
    // - Para 'housingQualities': UPDATE calidad_vivienda SET nombre = ? WHERE id = ?.
    // - Para 'vulnerableQuestions': UPDATE preguntas_test SET enunciado = ?, es_evaluable = ? WHERE id = ?.
    // - Para 'nationalities': UPDATE nacionalidades SET nombre = ? WHERE id = ?.
    // - Para 'threatTypes': UPDATE amenazas SET nombre = ? WHERE id = ?.
    // - Para 'species': UPDATE especies_mascota SET nombre = ? WHERE id = ?.
    // - Para 'resources': UPDATE tipos_recurso SET nombre = ?, servicio_id = ? WHERE id = ?.
    // - Para 'vulnerabilities': UPDATE vulnerabilidades SET nombre = ? WHERE id = ?.
    public void actualizar(String entidad, int id, JSONObject body) throws SQLException {
        if (entidad.equalsIgnoreCase("sectionals")) {
            String oldName = obtenerNombreSeccionalPorId(id);
            String newName = body.getString("name");
            if (oldName != null && !oldName.equalsIgnoreCase(newName)) {
                String sql = "UPDATE organizaciones SET seccional = ? WHERE seccional = ?";
                try (Connection con = Conexion.obtener();
                     PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, newName);
                    ps.setString(2, oldName);
                    ps.executeUpdate();
                }
            }
            return;
        }

        String sql = "";
        if (entidad.equalsIgnoreCase("sectors")) {
            sql = "UPDATE sectores SET nombre = ? WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("organizations")) {
            sql = "UPDATE organizaciones SET nombre = ?, seccional = ? WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("documentTypes")) {
            sql = "UPDATE tipo_documentos SET descripcion = ?, sigla = ? WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("housingQualities")) {
            sql = "UPDATE calidad_vivienda SET nombre = ? WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("vulnerableQuestions")) {
            sql = "UPDATE preguntas_test SET enunciado = ?, es_evaluable = ? WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("nationalities")) {
            sql = "UPDATE nacionalidades SET nombre = ? WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("threatTypes")) {
            sql = "UPDATE amenazas SET nombre = ? WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("species")) {
            sql = "UPDATE especies_mascota SET nombre = ? WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("resources")) {
            sql = "UPDATE tipos_recurso SET nombre = ?, servicio_id = ? WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("vulnerabilities")) {
            sql = "UPDATE vulnerabilidades SET nombre = ? WHERE id = ?";
        }

        if (sql.isEmpty()) return;

        // Qué hace: Abre la conexión a la base de datos MySQL y prepara el statement de actualización.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            if (entidad.equalsIgnoreCase("sectors") || entidad.equalsIgnoreCase("housingQualities") ||
                entidad.equalsIgnoreCase("nationalities") || entidad.equalsIgnoreCase("threatTypes") ||
                entidad.equalsIgnoreCase("species") || entidad.equalsIgnoreCase("vulnerabilities")) {
                ps.setString(1, body.getString("name"));
                ps.setInt(2, id);
            }
            else if (entidad.equalsIgnoreCase("organizations")) {
                ps.setString(1, body.getString("name"));
                int secId = body.getInt("sectional_id");
                String secName = obtenerNombreSeccionalPorId(secId);
                ps.setString(2, secName != null ? secName : "Santander");
                ps.setInt(3, id);
            }
            else if (entidad.equalsIgnoreCase("documentTypes")) {
                ps.setString(1, body.getString("name"));
                ps.setString(2, body.getString("acronym"));
                ps.setInt(3, id);
            }
            else if (entidad.equalsIgnoreCase("vulnerableQuestions")) {
                ps.setString(1, body.getString("description"));
                ps.setBoolean(2, !(body.optBoolean("question_caution", true) || body.optInt("question_caution", 0) == 1));
                ps.setInt(3, id);
            }
            else if (entidad.equalsIgnoreCase("resources")) {
                ps.setString(1, body.getString("name"));
                String servicioNombre = body.getString("service");
                int servicioId = obtenerOInsertarServicio(con, servicioNombre);
                ps.setInt(2, servicioId);
                ps.setInt(3, id);
            }

            // Qué hace: Ejecuta la sentencia de actualización física en MySQL.
            ps.executeUpdate();
        }
    }

    // Sirve para: Modificar el estado de activación lógica (is_active / activo) de un registro por su ID.
    // Qué hace: Ejecuta un UPDATE dinámico sobre la columna de activación (activo) de la tabla correspondiente.
    // Explicación de sentencias UPDATE de activación:
    // - Para 'sectionals': UPDATE organizaciones SET activo = ? WHERE seccional = ?.
    // - Para otras entidades ('sectors', 'organizations', etc.): UPDATE {tabla} SET activo = ? WHERE id = ?.
    public void cambiarEstado(String entidad, int id, boolean activo) throws SQLException {
        if (entidad.equalsIgnoreCase("sectionals")) {
            String name = obtenerNombreSeccionalPorId(id);
            if (name != null) {
                String sql = "UPDATE organizaciones SET activo = ? WHERE seccional = ?";
                try (Connection con = Conexion.obtener();
                     PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setBoolean(1, activo);
                    ps.setString(2, name);
                    ps.executeUpdate();
                }
            }
            return;
        }

        String sql = "";
        if (entidad.equalsIgnoreCase("sectors")) {
            sql = "UPDATE sectores SET activo = ? WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("organizations")) {
            sql = "UPDATE organizaciones SET activo = ? WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("documentTypes")) {
            sql = "UPDATE tipo_documentos SET activo = ? WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("housingQualities")) {
            sql = "UPDATE calidad_vivienda SET activo = ? WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("vulnerableQuestions")) {
            sql = "UPDATE preguntas_test SET activo = ? WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("nationalities")) {
            sql = "UPDATE nacionalidades SET activo = ? WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("threatTypes")) {
            sql = "UPDATE amenazas SET activo = ? WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("species")) {
            sql = "UPDATE especies_mascota SET activo = ? WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("resources")) {
            sql = "UPDATE tipos_recurso SET activo = ? WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("vulnerabilities")) {
            sql = "UPDATE vulnerabilidades SET activo = ? WHERE id = ?";
        }

        if (sql.isEmpty()) return;

        // Qué hace: Abre la conexión a la base de datos MySQL y prepara el statement.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, activo);
            ps.setInt(2, id);
            // Qué hace: Ejecuta la modificación física del estado de activación.
            ps.executeUpdate();
        }
    }

    /*
     * Qué hace (la acción): Elimina de manera física el registro seleccionado previa validación relacional de sus dependencias en otras tablas.
     * Qué significa (conceptos, métodos, tipos involucrados):
     *   - DELETE FROM: Elimina el registro físico de la base de datos MySQL.
     *   - validarDependenciasFK: Método que lanza excepciones si el registro cuenta con registros dependientes activos.
     * Para qué se usa (el propósito): Depurar registros agregados por error que no cuenten con relaciones pendientes en el sistema.
     * Por qué es importante (el impacto o problema que resuelve): Garantiza que no se violen llaves foráneas o restricciones físicas en MySQL, arrojando errores controlados y legibles.
     */
    public void eliminar(String entidad, int id) throws SQLException {
        if (entidad.equalsIgnoreCase("sectionals")) {
            String name = obtenerNombreSeccionalPorId(id);
            if (name != null) {
                // Verificar si hay usuarios asociados a organizaciones de esta seccional
                String checkSql = "SELECT COUNT(*) FROM usuarios u JOIN organizaciones o ON u.organizacion_id = o.id WHERE o.seccional = ?";
                try (Connection con = Conexion.obtener();
                     PreparedStatement ps = con.prepareStatement(checkSql)) {
                    ps.setString(1, name);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            throw new SQLException("No se puede eliminar la seccional porque contiene organizaciones con usuarios registrados.");
                        }
                    }
                }
                
                // Si pasa la validación, elimina las organizaciones de la seccional
                String sql = "DELETE FROM organizaciones WHERE seccional = ?";
                try (Connection con = Conexion.obtener();
                     PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, name);
                    ps.executeUpdate();
                }
            }
            return;
        }

        // Para otras tablas, realizar validación de dependencias FK antes de eliminar
        validarDependenciasFK(entidad, id);

        String sql = "";
        if (entidad.equalsIgnoreCase("sectors")) {
            sql = "DELETE FROM sectores WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("organizations")) {
            sql = "DELETE FROM organizaciones WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("documentTypes")) {
            sql = "DELETE FROM tipo_documentos WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("housingQualities")) {
            sql = "DELETE FROM calidad_vivienda WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("vulnerableQuestions")) {
            sql = "DELETE FROM preguntas_test WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("nationalities")) {
            sql = "DELETE FROM nacionalidades WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("threatTypes")) {
            sql = "DELETE FROM amenazas WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("species")) {
            sql = "DELETE FROM especies_mascota WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("resources")) {
            sql = "DELETE FROM tipos_recurso WHERE id = ?";
        } else if (entidad.equalsIgnoreCase("vulnerabilities")) {
            sql = "DELETE FROM vulnerabilidades WHERE id = ?";
        }

        if (sql.isEmpty()) return;

        // Qué hace: Obtiene la conexión JDBC y prepara el statement de eliminación física.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            // Qué hace: Ejecuta la instrucción de eliminación física en la base de datos MySQL.
            ps.executeUpdate();
        }
    }

    // Sirve para: Prevenir caídas y errores de integridad referencial (Foreign Key constraints) en MySQL al intentar borrar un registro.
    // Qué hace: Realiza una consulta SELECT COUNT(*) condicional para contar cuántas filas referencian al registro en las tablas secundarias.
    // Explicación de consultas de integridad FK:
    // - Para 'sectors': SELECT COUNT(*) FROM planes_familiares WHERE sector_id = ? (verifica si hay planes familiares asignados al sector).
    // - Para 'organizations': SELECT COUNT(*) FROM usuarios WHERE organizacion_id = ? (verifica si hay usuarios en la organización).
    // - Para 'documentTypes': SELECT COUNT(*) FROM usuarios WHERE tipo_documento_id = ? (verifica si hay usuarios con ese tipo de documento).
    // - Para 'housingQualities': SELECT COUNT(*) FROM identificacion_familiar WHERE calidad_vivienda_id = ? (verifica si hay identificaciones con esa calidad de vivienda).
    // - Para 'vulnerableQuestions': SELECT COUNT(*) FROM respuestas_test WHERE pregunta_id = ? (verifica si hay respuestas dadas a esa pregunta).
    // - Para 'species': SELECT COUNT(*) FROM mascotas WHERE especie_mascota_id = ? (verifica si hay mascotas de esa especie).
    // - Para 'resources': SELECT COUNT(*) FROM recursos_disponibles WHERE tipo_recurso_id = ? (verifica si hay recursos asignados a ese tipo).
    private void validarDependenciasFK(String entidad, int id) throws SQLException {
        String query = "";
        String msg = "";

        if (entidad.equalsIgnoreCase("sectors")) {
            query = "SELECT COUNT(*) FROM planes_familiares WHERE sector_id = ?";
            msg = "No se puede eliminar el sector porque existen planes familiares asociados a él.";
        } else if (entidad.equalsIgnoreCase("organizations")) {
            query = "SELECT COUNT(*) FROM usuarios WHERE organizacion_id = ?";
            msg = "No se puede eliminar la organización porque existen voluntarios o gestores registrados en ella.";
        } else if (entidad.equalsIgnoreCase("documentTypes")) {
            query = "SELECT COUNT(*) FROM usuarios WHERE tipo_documento_id = ?";
            msg = "No se puede eliminar este tipo de documento porque existen usuarios registrados con él.";
        } else if (entidad.equalsIgnoreCase("housingQualities")) {
            query = "SELECT COUNT(*) FROM identificacion_familiar WHERE calidad_vivienda_id = ?";
            msg = "No se puede eliminar esta calidad de vivienda porque está siendo referenciada en planes familiares.";
        } else if (entidad.equalsIgnoreCase("vulnerableQuestions")) {
            query = "SELECT COUNT(*) FROM respuestas_test WHERE pregunta_id = ?";
            msg = "No se puede eliminar la pregunta porque existen respuestas registradas a este test.";
        } else if (entidad.equalsIgnoreCase("species")) {
            query = "SELECT COUNT(*) FROM mascotas WHERE especie_mascota_id = ?";
            msg = "No se puede eliminar esta especie porque existen mascotas registradas con ella.";
        } else if (entidad.equalsIgnoreCase("resources")) {
            query = "SELECT COUNT(*) FROM recursos_disponibles WHERE tipo_recurso_id = ?";
            msg = "No se puede eliminar este tipo de recurso porque está siendo referenciado en planes de emergencia.";
        }

        if (!query.isEmpty()) {
            // Qué hace: Obtiene la conexión y compila el statement de conteo de dependencias relacionales.
            try (Connection con = Conexion.obtener();
                 PreparedStatement ps = con.prepareStatement(query)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        // Qué hace: Lanza una excepción controlada impidiendo la eliminación si el conteo es mayor que cero.
                        throw new SQLException(msg);
                    }
                }
            }
        }
    }
}
