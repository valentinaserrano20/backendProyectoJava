package Modelo.DAO;

import Modelo.Config.Conexion;
import Modelo.Entidades.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UsuarioDAO {

    // Sirve para: Obtener la información completa de un usuario buscando por su dirección de correo electrónico.
    // Qué hace: Realiza una consulta SELECT a la base de datos uniendo la tabla usuarios con estado_usuarios.
    // Explicación de consulta SQL:
    // - Información buscada: Los campos id, nombre, apellido, email, contraseña (hash), rol_id, organizacion_id y estado_id del usuario, junto con el nombre descriptivo del estado (estado_nombre).
    // - Tablas participantes: usuarios (u) y estado_usuarios (e).
    // - Filtros aplicados: u.email = ? (el correo electrónico proporcionado por parámetro).
    // - Relación (JOIN): Se realiza un INNER JOIN entre la tabla usuarios (u) y estado_usuarios (e) a través de la clave foránea u.estado_id y la clave primaria e.id.
    public Usuario obtenerPorEmail(String email) throws SQLException {
        String sql = "SELECT u.id, u.nombre, u.apellido, u.email, u.contraseña, "
                + "u.rol_id, u.organizacion_id, u.estado_id, e.nombre AS estado_nombre "
                + "FROM usuarios u "
                + "INNER JOIN estado_usuarios e ON u.estado_id = e.id "
                + "WHERE u.email = ?";
                
        // Qué hace: Obtiene una conexión activa a la base de datos y prepara la consulta SQL.
        // Por qué existe: Habilita la ejecución segura de la consulta mediante PreparedStatement, previniendo inyección SQL.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Asigna el valor del parámetro de entrada 'email' al primer marcador (?) de la consulta.
            ps.setString(1, email);
            // Qué hace: Ejecuta la consulta SELECT y almacena los resultados en un ResultSet.
            try (ResultSet rs = ps.executeQuery()) {
                // Qué hace: Verifica si la consulta retornó alguna fila coincidente.
                if (rs.next()) {
                    // Qué hace: Instancia un nuevo objeto de la entidad Usuario.
                    Usuario u = new Usuario();
                    // Qué hace: Asigna al objeto Usuario los valores correspondientes recuperados de las columnas del ResultSet.
                    u.setId(rs.getInt("id"));
                    u.setNombre(rs.getString("nombre"));
                    u.setApellido(rs.getString("apellido"));
                    u.setEmail(rs.getString("email"));
                    u.setContrasena(rs.getString("contraseña"));
                    u.setRolId(rs.getInt("rol_id"));
                    // Qué hace: Mapea la columna organizacion_id manejando valores nulos en la base de datos.
                    u.setOrganizacionId(rs.getObject("organizacion_id") != null
                            ? rs.getInt("organizacion_id")
                            : null);
                    u.setEstadoId(rs.getInt("estado_id"));
                    // Qué hace: Asigna el nombre legible del estado obtenido mediante el INNER JOIN.
                    u.setEstado(rs.getString("estado_nombre"));
                    // Qué hace: Retorna el usuario completamente mapeado.
                    return u;
                }
                // Qué hace: Retorna null si no se encontró ningún usuario con ese correo electrónico.
                return null;
            }
        }
    }

    // Sirve para: Verificar la existencia de una cuenta registrada con un correo electrónico específico.
    // Qué hace: Realiza una consulta SELECT para buscar el ID de un usuario por su email.
    // Explicación de consulta SQL:
    // - Información buscada: El campo id del usuario.
    // - Tablas participantes: usuarios.
    // - Filtros aplicados: email = ? (el email que se desea validar).
    public boolean existeEmail(String email) throws SQLException {
        String sql = "SELECT id FROM usuarios WHERE email = ?";
        // Qué hace: Obtiene la conexión y compila el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Asigna el email a validar al primer marcador de la consulta.
            ps.setString(1, email);
            // Qué hace: Ejecuta la consulta y lee el ResultSet resultante.
            try (ResultSet rs = ps.executeQuery()) {
                // Qué hace: Retorna true si hay al menos un registro que coincida con el email, de lo contrario false.
                return rs.next();
            }
        }
    }

    // Sirve para: Validar si un número de documento de identificación ya se encuentra registrado en el sistema.
    // Qué hace: Realiza una consulta SELECT buscando el ID de un usuario con el número de documento proporcionado.
    // Explicación de consulta SQL:
    // - Información buscada: El campo id del usuario.
    // - Tablas participantes: usuarios.
    // - Filtros aplicados: numero_documento = ? (el número de documento a comprobar).
    public boolean existeDocumento(String numDocumento) throws SQLException {
        String sql = "SELECT id FROM usuarios WHERE numero_documento = ?";
        // Qué hace: Obtiene la conexión y compila el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Asigna el número de documento a comprobar en el statement.
            ps.setString(1, numDocumento);
            // Qué hace: Ejecuta la consulta de selección.
            try (ResultSet rs = ps.executeQuery()) {
                // Qué hace: Retorna true si ya existe un usuario con ese número de documento, de lo contrario false.
                return rs.next();
            }
        }
    }

    // Sirve para: Registrar un nuevo usuario (inicialmente con rol de Voluntario y estado Pendiente) en el sistema.
    // Qué hace: Ejecuta una sentencia INSERT parametrizada en la tabla usuarios.
    // Explicación de consulta SQL:
    // - Operación: Inserción de un nuevo registro.
    // - Tabla afectada: usuarios.
    // - Columnas insertadas: nombre, apellido, email, contraseña, numero_documento, fecha_nacimiento, celular, tipo_documento_id, genero_id, organizacion_id, rol_id, estado_id.
    // - Valores insertados: Los valores parametrizados recibidos, forzando por defecto rol_id = 1 (Voluntario) y estado_id = 3 (Pendiente de aprobación).
    // Método encargado de realizar la inserción física del registro en la tabla de base de datos relacional
    public void registrar(String nombres, String apellidos, String email, String passwordHashed,
            String numDocumento, String fechaNac, String telefono,
            int tipoDocumentoId, int generoId, int organizacionId) throws SQLException {

        // Construye la sentencia SQL parametrizada. 
        // Define de manera fija el rol_id en 1 (Voluntario) y el estado_id en 3 (Pendiente).
        String sql = "INSERT INTO usuarios (nombre, apellido, email, contraseña, numero_documento, "
                + "fecha_nacimiento, celular, tipo_documento_id, genero_id, organizacion_id, rol_id, estado_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 3)";

        // Obtiene una conexión activa del pool de conexiones JDBC e inicializa el PreparedStatement.
        // El bloque try-with-resources garantiza el cierre automático de la conexión y el statement al terminar.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Asocia el primer parámetro "?" de la sentencia SQL al nombre del usuario
            ps.setString(1, nombres);
            // Asocia el segundo parámetro "?" de la sentencia SQL al apellido del usuario
            ps.setString(2, apellidos);
            // Asocia el tercer parámetro "?" de la sentencia SQL al correo electrónico
            ps.setString(3, email);
            // Asocia el cuarto parámetro "?" de la sentencia SQL a la contraseña cifrada
            ps.setString(4, passwordHashed); 
            // Asocia el quinto parámetro "?" de la sentencia SQL al número de documento
            ps.setString(5, numDocumento);
            // Asocia el sexto parámetro "?" de la sentencia SQL a la fecha de nacimiento (YYYY-MM-DD)
            ps.setString(6, fechaNac);
            // Asocia el séptimo parámetro "?" de la sentencia SQL al número de celular
            ps.setString(7, telefono);
            // Asocia el octavo parámetro "?" de la sentencia SQL al identificador del tipo de documento
            ps.setInt(8, tipoDocumentoId);
            // Asocia el noveno parámetro "?" de la sentencia SQL al identificador del género
            ps.setInt(9, generoId);
            // Asocia el décimo parámetro "?" de la sentencia SQL al identificador de la organización
            ps.setInt(10, organizacionId);
            
            // Envía la consulta preparada para ser compilada y ejecutada en el motor de base de datos MySQL.
            // Sirve para persistir el nuevo registro de voluntario en el disco.
            ps.executeUpdate();
        }
    }
    
    // Sirve para: Obtener los datos básicos de un usuario por su identificador único (ID).
    // Qué hace: Realiza una consulta SELECT simple filtrando por la clave primaria 'id' del usuario.
    // Explicación de consulta SQL:
    // - Información buscada: id, nombre, apellido, email, genero_id, rol_id, organizacion_id, estado_id.
    // - Tablas participantes: usuarios.
    // - Filtros aplicados: id = ? (clave primaria del usuario).
    public Usuario obtenerPorId(int id) throws SQLException {
        String sql = "SELECT id, nombre, apellido, email, genero_id, rol_id, organizacion_id, estado_id "
                   + "FROM usuarios WHERE id = ?";
                   
        // Qué hace: Obtiene la conexión y prepara el statement de consulta.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Configura el ID del usuario en el filtro WHERE.
            ps.setInt(1, id);
            // Qué hace: Ejecuta el query en la base de datos.
            try (ResultSet rs = ps.executeQuery()) {
                // Qué hace: Verifica si se encontró el registro.
                if (rs.next()) {
                    // Qué hace: Instancia el objeto Usuario y mapea todos los atributos correspondientes desde las columnas resultantes.
                    Usuario u = new Usuario();
                    u.setId(rs.getInt("id"));
                    u.setNombre(rs.getString("nombre"));
                    u.setApellido(rs.getString("apellido"));
                    u.setEmail(rs.getString("email"));
                    u.setRolId(rs.getInt("rol_id"));
                    u.setEstadoId(rs.getInt("estado_id"));
                    u.setOrganizacionId(rs.getObject("organizacion_id") != null
                            ? rs.getInt("organizacion_id")
                            : null);
                    u.setGeneroId(rs.getInt("genero_id"));
                    // Qué hace: Retorna la entidad mapeada.
                    return u;
                }
                // Qué hace: Retorna null si el ID no corresponde a ningún usuario registrado.
                return null;
            }
        }
    }

    // =========================================================================
    // MÓDULO: RECUPERACIÓN DE CONTRASEÑA
    // =========================================================================

    // Sirve para: Guardar de forma temporal el token de recuperación de contraseña y su fecha de vencimiento.
    // Qué hace: Ejecuta una sentencia UPDATE en la tabla usuarios asignando el reset_token y reset_token_expiry.
    // Explicación de consulta SQL:
    // - Operación: Actualización de campos en base de datos.
    // - Tabla afectada: usuarios.
    // - Filtros aplicados: email = ? (el email del usuario que solicitó el restablecimiento).
    // - Columnas modificadas: reset_token = ? (el hash del token temporal), reset_token_expiry = ? (la fecha y hora de expiración).
    public void guardarTokenRecuperacion(String email, String token, java.sql.Timestamp expiracion) throws SQLException {
        String sql = "UPDATE usuarios SET reset_token = ?, reset_token_expiry = ? WHERE email = ?";
        // Qué hace: Obtiene la conexión a la base de datos y compila el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Enlaza los parámetros del token, la fecha de expiración y el correo electrónico.
            ps.setString(1, token);
            ps.setTimestamp(2, expiracion);
            ps.setString(3, email);
            // Qué hace: Ejecuta la actualización física en la base de datos.
            ps.executeUpdate();
        }
    }

    // Sirve para: Buscar un usuario en el sistema que tenga un token de recuperación válido y no haya expirado.
    // Qué hace: Realiza una consulta SELECT filtrando por el token y comparando que reset_token_expiry sea mayor que la hora actual del servidor.
    // Explicación de consulta SQL:
    // - Información buscada: id y email de la cuenta del usuario.
    // - Tablas participantes: usuarios.
    // - Filtros aplicados: reset_token = ? (el token proporcionado) AND reset_token_expiry > NOW() (la marca de tiempo de expiración debe ser mayor al instante actual).
    public Usuario obtenerPorTokenValido(String token) throws SQLException {
        String sql = "SELECT id, email FROM usuarios WHERE reset_token = ? AND reset_token_expiry > NOW()";
        // Qué hace: Abre la conexión JDBC y prepara la sentencia de selección.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Enlaza el token a validar como parámetro.
            ps.setString(1, token);
            // Qué hace: Ejecuta la consulta y lee el ResultSet resultante.
            try (ResultSet rs = ps.executeQuery()) {
                // Qué hace: Si hay un usuario que posea este token y esté vigente.
                if (rs.next()) {
                    Usuario u = new Usuario();
                    u.setId(rs.getInt("id"));
                    u.setEmail(rs.getString("email"));
                    // Qué hace: Retorna la instancia de Usuario con su ID y email cargados.
                    return u;
                }
            }
        }
        // Qué hace: Retorna null si el token es inválido o ya ha expirado.
        return null;
    }

    // Sirve para: Establecer la nueva contraseña del usuario y limpiar las columnas del token de restablecimiento.
    // Qué hace: Ejecuta un UPDATE en la tabla usuarios asignando la contraseña encriptada y estableciendo en NULL las columnas de recuperación.
    // Explicación de consulta SQL:
    // - Operación: Actualización de columnas.
    // - Tabla afectada: usuarios.
    // - Columnas modificadas: contraseña = ? (la nueva contraseña hasheada), reset_token = NULL, reset_token_expiry = NULL.
    // - Filtros aplicados: id = ? (identificador único del usuario).
    public void actualizarContrasenaYLimpiarToken(int usuarioId, String nuevaContrasenaHashed) throws SQLException {
        String sql = "UPDATE usuarios SET contraseña = ?, reset_token = NULL, reset_token_expiry = NULL WHERE id = ?";
        // Qué hace: Obtiene la conexión JDBC y prepara la actualización parametrizada.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Enlaza la contraseña hasheada y el ID de usuario correspondiente.
            ps.setString(1, nuevaContrasenaHashed);
            ps.setInt(2, usuarioId);
            // Qué hace: Aplica la actualización en la base de datos MySQL.
            ps.executeUpdate();
        }
    }
    
    // =========================================================================
    // MÓDULO: MI PERFIL (INTERACCIONES PROTEGIDAS)
    // =========================================================================

    // Sirve para: Obtener el perfil detallado del usuario cruzando sus relaciones de catálogos maestros.
    // Qué hace: Ejecuta un SELECT con múltiples LEFT JOINs hacia las tablas relacionales para obtener etiquetas descriptivas.
    // Explicación de consulta SQL:
    // - Información buscada: id, nombre, apellido, email, numero_documento, fecha_nacimiento, celular, contraseña (hash), la descripción del tipo de documento (tipo_documento_nombre), el nombre del estado (estado_nombre), la organización (organizacion_nombre) y la seccional (seccional_nombre).
    // - Tablas participantes: usuarios u (principal), tipo_documentos td (relación tipo doc), estado_usuarios eu (relación estado), organizaciones o (relación organización).
    // - Relaciones (JOINs):
    //   1. LEFT JOIN td ON u.tipo_documento_id = td.id (cruza llave foránea de documento con catálogo).
    //   2. LEFT JOIN eu ON u.estado_id = eu.id (cruza llave foránea de estado con catálogo de estado de usuario).
    //   3. LEFT JOIN o ON u.organizacion_id = o.id (cruza llave foránea de organización con catálogo de organizaciones).
    // - Filtros aplicados: u.id = ? (el identificador del usuario solicitante).
    public Usuario obtenerPerfilDetallado(int id) throws SQLException {
        String sql = "SELECT u.id, u.nombre, u.apellido, u.email, u.numero_documento, u.fecha_nacimiento, u.celular, u.contraseña, "
                   + "td.descripcion AS tipo_documento_nombre, "
                   + "eu.nombre AS estado_nombre, "
                   + "o.nombre AS organizacion_nombre, "
                   + "o.seccional AS seccional_nombre "
                   + "FROM usuarios u "
                   + "LEFT JOIN tipo_documentos td ON u.tipo_documento_id = td.id "
                   + "LEFT JOIN estado_usuarios eu ON u.estado_id = eu.id "
                   + "LEFT JOIN organizaciones o ON u.organizacion_id = o.id "
                   + "WHERE u.id = ?";
                   
        // Qué hace: Obtiene la conexión y compila el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Asigna el ID de usuario al marcador del filtro WHERE.
            ps.setInt(1, id);
            // Qué hace: Ejecuta la consulta y lee el ResultSet.
            try (ResultSet rs = ps.executeQuery()) {
                // Qué hace: Si se encuentra el usuario con el ID especificado.
                if (rs.next()) {
                    Usuario u = new Usuario();
                    // Qué hace: Mapea detalladamente los campos básicos y relacionales del ResultSet al objeto Usuario de retorno.
                    u.setId(rs.getInt("id"));
                    u.setNombre(rs.getString("nombre"));
                    u.setApellido(rs.getString("apellido"));
                    u.setEmail(rs.getString("email"));
                    u.setContrasena(rs.getString("contraseña")); // Requerido para verificar contraseña actual antes de cambios
                    u.setEstado(rs.getString("estado_nombre"));
                    // Se pueden almacenar u obtener nombres cruzados si la entidad lo soporta.
                    return u;
                }
            }
        }
        // Qué hace: Retorna null si el ID de usuario no existe.
        return null;
    }

    // Sirve para: Modificar el número de teléfono celular de un usuario.
    // Qué hace: Ejecuta una sentencia UPDATE en la tabla usuarios para cambiar la columna celular filtrando por ID.
    // Explicación de consulta SQL:
    // - Operación: Actualización de columnas.
    // - Tabla afectada: usuarios.
    // - Columnas modificadas: celular = ? (el nuevo número telefónico).
    // - Filtros aplicados: id = ? (clave primaria del usuario).
    public void actualizarTelefono(int id, String nuevoTelefono) throws SQLException {
        String sql = "UPDATE usuarios SET celular = ? WHERE id = ?";
        // Qué hace: Abre la conexión a la base de datos y compila el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Enlaza los parámetros de celular y ID del usuario.
            ps.setString(1, nuevoTelefono);
            ps.setInt(2, id);
            // Qué hace: Ejecuta la modificación física en el motor MySQL.
            ps.executeUpdate();
        }
    }

    // Sirve para: Modificar la dirección de correo electrónico registrada por un usuario.
    // Qué hace: Ejecuta una sentencia UPDATE en la tabla usuarios para cambiar la columna email por ID.
    // Explicación de consulta SQL:
    // - Operación: Actualización de columnas.
    // - Tabla afectada: usuarios.
    // - Columnas modificadas: email = ? (el nuevo correo electrónico).
    // - Filtros aplicados: id = ? (identificador único).
    public void actualizarEmail(int id, String nuevoEmail) throws SQLException {
        String sql = "UPDATE usuarios SET email = ? WHERE id = ?";
        // Qué hace: Obtiene la conexión JDBC y prepara el statement.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Enlaza el nuevo correo y el ID en el statement.
            ps.setString(1, nuevoEmail);
            ps.setInt(2, id);
            // Qué hace: Ejecuta el update en el motor de base de datos.
            ps.executeUpdate();
        }
    }

    // Sirve para: Modificar la contraseña de inicio de sesión de un usuario.
    // Qué hace: Ejecuta una sentencia UPDATE en la tabla usuarios cambiando la contraseña por ID.
    // Explicación de consulta SQL:
    // - Operación: Actualización de columnas.
    // - Tabla afectada: usuarios.
    // - Columnas modificadas: contraseña = ? (la nueva contraseña ya encriptada con BCrypt).
    // - Filtros aplicados: id = ? (identificador único).
    public void actualizarContrasena(int id, String nuevaContrasenaHashed) throws SQLException {
        String sql = "UPDATE usuarios SET contraseña = ? WHERE id = ?";
        // Qué hace: Obtiene la conexión y compila el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Enlaza la contraseña hasheada y el ID del usuario.
            ps.setString(1, nuevaContrasenaHashed);
            ps.setInt(2, id);
            // Qué hace: Aplica la actualización en la base de datos.
            ps.executeUpdate();
        }
    }

    /**
     * Qué hace: Consulta los permisos asociados a un rol específico desde la base de datos.
     * Por qué existe: Habilita el control dinámico de permisos relacionando la tabla 'roles_permisos' con 'permisos'.
     * Qué problema resuelve: Carga dinámicamente las facultades del rol del usuario al iniciar sesión.
     */
    public java.util.List<String> obtenerPermisosPorRol(int rolId) throws SQLException {
        // Qué hace: Inicializa una lista dinámica para almacenar las claves de los permisos.
        // Por qué existe: Sirve como estructura contenedora de los resultados de la base de datos.
        // Qué problema resuelve: Evita retornar valores nulos o estructuras no tipadas.
        java.util.List<String> permisos = new java.util.ArrayList<>();

        // Qué hace: Define la consulta SQL cruzando roles_permisos con permisos para obtener las llaves de acceso.
        // Por qué existe: Explicita la relación lógica entre el rol y sus capacidades asignadas.
        // Qué problema resuelve: Evita consultas manuales pesadas recuperando solo la columna requerida 'key_name'.
        String sql = "SELECT p.key_name FROM roles_permisos rp "
                   + "INNER JOIN permisos p ON rp.permission_id = p.id "
                   + "WHERE rp.role_id = ?";

        // Qué hace: Abre la conexión a la base de datos y prepara la consulta con el PreparedStatement.
        // Por qué existe: Asegura el cierre seguro de los recursos JDBC al finalizar la ejecución del bloque.
        // Qué problema resuelve: Previene fugas de memoria o conexiones activas huérfanas en el pool.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {

            // Qué hace: Enlaza el parámetro posicional del rol del usuario al PreparedStatement.
            // Por qué existe: Pasa de forma segura el ID del rol evitando inyecciones SQL.
            // Qué problema resuelve: Garantiza la seguridad y robustez de la consulta de base de datos.
            ps.setInt(1, rolId);

            // Qué hace: Ejecuta la consulta select en la base de datos MySQL.
            // Por qué existe: Abre el ResultSet para iterar sobre las filas encontradas.
            // Qué problema resuelve: Recupera físicamente las tuplas asociadas al rol.
            try (ResultSet rs = ps.executeQuery()) {
                // Qué hace: Recorre las filas de ResultSet mientras existan registros.
                // Por qué existe: Permite leer cada uno de los permisos del rol del usuario.
                // Qué problema resuelve: Consolida todos los permisos individuales en la lista temporal.
                while (rs.next()) {
                    // Qué hace: Agrega la clave de texto del permiso obtenido a la lista de salida.
                    // Por qué existe: Transfiere el valor de la columna 'key_name' al contenedor tipado de memoria.
                    // Qué problema resuelve: Convierte datos de bajo nivel relacionales en objetos String legibles.
                    permisos.add(rs.getString("key_name"));
                }
            }
        }
        
        // Qué hace: Retorna la lista con todas las llaves de permisos cargadas.
        // Por qué existe: Entrega el resultado final a la capa de servicios o controlador solicitante.
        // Qué problema resuelve: Expone los permisos del usuario para su evaluación de autorización.
        return permisos;
    }

    // =========================================================================
    // MÓDULO: GESTIÓN DE USUARIOS (SUPERVISOR Y ADMINISTRADOR)
    // =========================================================================

    // Sirve para: Contar el número total de usuarios registrados con rol de Voluntario (rol_id = 1) y que estén Activos (estado_id = 1) o Inactivos (estado_id = 2).
    // Qué hace: Realiza una consulta SELECT COUNT(*) filtrando por rol_id y estado_id.
    // Explicación de consulta SQL:
    // - Información buscada: El conteo total de registros en la tabla.
    // - Tablas participantes: usuarios.
    // - Filtros aplicados: rol_id = 1 AND estado_id IN (1, 2) (excluye pendientes y eliminados).
    public int contarVoluntarios() throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE rol_id = 1 AND estado_id IN (1, 2)";
        // Qué hace: Obtiene la conexión a la base de datos y ejecuta la consulta de conteo.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            // Qué hace: Retorna la cuenta si hay resultados disponibles.
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    // Sirve para: Obtener una lista paginada de voluntarios registrados para el panel de gestión de supervisores.
    // Qué hace: Ejecuta un SELECT con LEFT JOIN a roles, estado_usuarios y organizaciones con paginación LIMIT/OFFSET.
    // Explicación de consulta SQL:
    // - Información buscada: id, nombre completo (full_name), nombre, apellido, email, la seccional de la organización, el nombre de la organización, el nombre legible del estado del usuario (status_name) y el nombre del rol (rol_name).
    // - Tablas participantes: usuarios u, roles r, estado_usuarios eu, organizaciones o.
    // - Relaciones (JOINs):
    //   1. LEFT JOIN roles r ON u.rol_id = r.id (obtiene el nombre legible del rol).
    //   2. LEFT JOIN estado_usuarios eu ON u.estado_id = eu.id (obtiene el nombre legible del estado de cuenta).
    //   3. LEFT JOIN organizaciones o ON u.organizacion_id = o.id (obtiene la seccional y nombre de la organización).
    // - Filtros aplicados: u.rol_id = 1 AND u.estado_id IN (1, 2) (sólo voluntarios activos o inactivos).
    // - Ordenamiento y paginación: Ordenados por id descendente, limitados y desplazados por parámetros.
    public java.util.List<java.util.Map<String, Object>> listarVoluntariosPaginados(int offset, int limit) throws SQLException {
        String sql = "SELECT u.id, CONCAT(u.nombre, ' ', u.apellido) AS full_name, u.nombre, u.apellido, u.email, "
                   + "o.seccional AS sectional, o.nombre AS organization, eu.nombre AS status_name, r.nombre AS rol_name "
                   + "FROM usuarios u "
                   + "LEFT JOIN roles r ON u.rol_id = r.id "
                   + "LEFT JOIN estado_usuarios eu ON u.estado_id = eu.id "
                   + "LEFT JOIN organizaciones o ON u.organizacion_id = o.id "
                   + "WHERE u.rol_id = 1 AND u.estado_id IN (1, 2) "
                   + "ORDER BY u.id DESC LIMIT ? OFFSET ?";
        // Qué hace: Inicializa la lista que contendrá el resultado mapeado.
        java.util.List<java.util.Map<String, Object>> resultado = new java.util.ArrayList<>();
        // Qué hace: Obtiene la conexión JDBC y prepara la consulta parametrizada.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Configura los parámetros de límite y desplazamiento para la paginación.
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            // Qué hace: Ejecuta la consulta y recorre el ResultSet.
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Qué hace: Mapea cada fila del ResultSet a un HashMap para su posterior conversión a JSON.
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", rs.getInt("id"));
                    map.put("full_name", rs.getString("full_name"));
                    map.put("names", rs.getString("nombre"));
                    map.put("last_names", rs.getString("apellido"));
                    map.put("email", rs.getString("email"));
                    map.put("sectional", rs.getString("sectional") != null ? rs.getString("sectional") : "No asignado");
                    map.put("organization", rs.getString("organization") != null ? rs.getString("organization") : "No asignado");
                    map.put("status", rs.getString("status_name"));
                    map.put("rol", rs.getString("rol_name"));
                    // Qué hace: Agrega el mapa del voluntario a la lista de resultados.
                    resultado.add(map);
                }
            }
        }
        return resultado;
    }

    // Sirve para: Obtener el listado global y completo de voluntarios sin paginación.
    // Qué hace: Ejecuta la misma consulta SELECT con LEFT JOINs hacia roles, estado_usuarios y organizaciones omitiendo LIMIT/OFFSET.
    // Explicación de consulta SQL:
    // - Información buscada: id, nombre completo, nombre, apellido, email, seccional, organización, estado y rol.
    // - Tablas participantes: usuarios u, roles r, estado_usuarios eu, organizaciones o.
    // - Relaciones (JOINs):
    //   1. LEFT JOIN roles r ON u.rol_id = r.id.
    //   2. LEFT JOIN estado_usuarios eu ON u.estado_id = eu.id.
    //   3. LEFT JOIN organizaciones o ON u.organizacion_id = o.id.
    // - Filtros aplicados: u.rol_id = 1 AND u.estado_id IN (1, 2).
    public java.util.List<java.util.Map<String, Object>> listarVoluntariosTodos() throws SQLException {
        String sql = "SELECT u.id, CONCAT(u.nombre, ' ', u.apellido) AS full_name, u.nombre, u.apellido, u.email, "
                   + "o.seccional AS sectional, o.nombre AS organization, eu.nombre AS status_name, r.nombre AS rol_name "
                   + "FROM usuarios u "
                   + "LEFT JOIN roles r ON u.rol_id = r.id "
                   + "LEFT JOIN estado_usuarios eu ON u.estado_id = eu.id "
                   + "LEFT JOIN organizaciones o ON u.organizacion_id = o.id "
                   + "WHERE u.rol_id = 1 AND u.estado_id IN (1, 2) "
                   + "ORDER BY u.id DESC";
        // Qué hace: Inicializa la lista que contendrá el resultado.
        java.util.List<java.util.Map<String, Object>> resultado = new java.util.ArrayList<>();
        // Qué hace: Obtiene la conexión JDBC y prepara el statement.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             // Qué hace: Ejecuta la consulta directa a MySQL.
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                // Qué hace: Mapea los campos de cada registro de voluntario.
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", rs.getInt("id"));
                map.put("full_name", rs.getString("full_name"));
                map.put("names", rs.getString("nombre"));
                map.put("last_names", rs.getString("apellido"));
                map.put("email", rs.getString("email"));
                map.put("sectional", rs.getString("sectional") != null ? rs.getString("sectional") : "No asignado");
                map.put("organization", rs.getString("organization") != null ? rs.getString("organization") : "No asignado");
                map.put("status", rs.getString("status_name"));
                map.put("rol", rs.getString("rol_name"));
                resultado.add(map);
            }
        }
        return resultado;
    }

    // Sirve para: Contar el número de peticiones de registro de voluntarios que se encuentran en estado Pendiente (estado_id = 3).
    // Qué hace: Realiza una consulta SELECT COUNT(*) filtrando por rol_id de Voluntario y estado de Pendiente.
    // Explicación de consulta SQL:
    // - Información buscada: El total de peticiones pendientes.
    // - Tablas participantes: usuarios.
    // - Filtros aplicados: rol_id = 1 AND estado_id = 3.
    public int contarPeticionesVoluntarios() throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE rol_id = 1 AND estado_id = 3";
        // Qué hace: Abre la conexión y ejecuta la consulta.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    // Sirve para: Obtener una lista paginada de peticiones de registro pendientes (estado_id = 3) para la bandeja del supervisor.
    // Qué hace: Ejecuta una consulta SELECT con LEFT JOINs hacia roles, estado_usuarios y organizaciones filtrada por estado_id = 3 con LIMIT/OFFSET.
    // Explicación de consulta SQL:
    // - Información buscada: id, nombre completo, nombre, apellido, email, seccional, organización, estado y rol de las cuentas.
    // - Tablas participantes: usuarios u, roles r, estado_usuarios eu, organizaciones o.
    // - Relaciones (JOINs):
    //   1. LEFT JOIN roles r ON u.rol_id = r.id.
    //   2. LEFT JOIN estado_usuarios eu ON u.estado_id = eu.id.
    //   3. LEFT JOIN organizaciones o ON u.organizacion_id = o.id.
    // - Filtros aplicados: u.rol_id = 1 AND u.estado_id = 3.
    // - Paginación: ORDER BY u.id DESC LIMIT ? OFFSET ?.
    public java.util.List<java.util.Map<String, Object>> listarPeticionesVoluntariosPaginados(int offset, int limit) throws SQLException {
        String sql = "SELECT u.id, CONCAT(u.nombre, ' ', u.apellido) AS full_name, u.nombre, u.apellido, u.email, "
                   + "o.seccional AS sectional, o.nombre AS organization, eu.nombre AS status_name, r.nombre AS rol_name "
                   + "FROM usuarios u "
                   + "LEFT JOIN roles r ON u.rol_id = r.id "
                   + "LEFT JOIN estado_usuarios eu ON u.estado_id = eu.id "
                   + "LEFT JOIN organizaciones o ON u.organizacion_id = o.id "
                   + "WHERE u.rol_id = 1 AND u.estado_id = 3 "
                   + "ORDER BY u.id DESC LIMIT ? OFFSET ?";
        java.util.List<java.util.Map<String, Object>> resultado = new java.util.ArrayList<>();
        // Qué hace: Abre la conexión a la base de datos y compila el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Enlaza los parámetros de paginación.
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            // Qué hace: Ejecuta la consulta y recorre el ResultSet.
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", rs.getInt("id"));
                    map.put("full_name", rs.getString("full_name"));
                    map.put("names", rs.getString("nombre"));
                    map.put("last_names", rs.getString("apellido"));
                    map.put("email", rs.getString("email"));
                    map.put("sectional", rs.getString("sectional") != null ? rs.getString("sectional") : "No asignado");
                    map.put("organization", rs.getString("organization") != null ? rs.getString("organization") : "No asignado");
                    map.put("status", rs.getString("status_name"));
                    map.put("rol", rs.getString("rol_name"));
                    resultado.add(map);
                }
            }
        }
        return resultado;
    }

    // Sirve para: Contar el número total de usuarios registrados en el sistema (excluyendo los que están en estado Pendiente = 3).
    // Qué hace: Realiza una consulta SELECT COUNT(*) filtrando por estados Activos o Inactivos.
    // Explicación de consulta SQL:
    // - Información buscada: Conteo total de cuentas de usuarios procesadas.
    // - Tablas participantes: usuarios.
    // - Filtros aplicados: estado_id IN (1, 2).
    public int contarUsuariosAdmin() throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE estado_id IN (1, 2)";
        // Qué hace: Obtiene la conexión JDBC y ejecuta el conteo directo.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    // Sirve para: Obtener una lista paginada de todos los usuarios registrados (para la vista de superadministrador).
    // Qué hace: Ejecuta un SELECT con LEFT JOIN a roles, estado_usuarios y organizaciones con paginación LIMIT/OFFSET.
    // Explicación de consulta SQL:
    // - Información buscada: id, nombre completo, nombre, apellido, email, número de documento, seccional, organización, estado (status_name), rol (rol_name), estado_id y rol_id.
    // - Tablas participantes: usuarios u, roles r, estado_usuarios eu, organizaciones o.
    // - Relaciones (JOINs):
    //   1. LEFT JOIN roles r ON u.rol_id = r.id.
    //   2. LEFT JOIN estado_usuarios eu ON u.estado_id = eu.id.
    //   3. LEFT JOIN organizaciones o ON u.organizacion_id = o.id.
    // - Filtros aplicados: u.estado_id IN (1, 2) (excluye pendientes).
    // - Paginación: ORDER BY u.id DESC LIMIT ? OFFSET ?.
    public java.util.List<java.util.Map<String, Object>> listarUsuariosAdminPaginados(int offset, int limit) throws SQLException {
        String sql = "SELECT u.id, CONCAT(u.nombre, ' ', u.apellido) AS full_name, u.nombre, u.apellido, u.email, u.numero_documento, "
                   + "o.seccional AS sectional, o.nombre AS organization, eu.nombre AS status_name, r.nombre AS rol_name, u.estado_id, u.rol_id "
                   + "FROM usuarios u "
                   + "LEFT JOIN roles r ON u.rol_id = r.id "
                   + "LEFT JOIN estado_usuarios eu ON u.estado_id = eu.id "
                   + "LEFT JOIN organizaciones o ON u.organizacion_id = o.id "
                   + "WHERE u.estado_id IN (1, 2) "
                   + "ORDER BY u.id DESC LIMIT ? OFFSET ?";
        java.util.List<java.util.Map<String, Object>> resultado = new java.util.ArrayList<>();
        // Qué hace: Abre la conexión a la base de datos y compila el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Enlaza los parámetros de paginación.
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            // Qué hace: Ejecuta la consulta y lee el ResultSet.
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", rs.getInt("id"));
                    map.put("full_name", rs.getString("full_name"));
                    map.put("names", rs.getString("nombre"));
                    map.put("last_names", rs.getString("apellido"));
                    map.put("email", rs.getString("email"));
                    map.put("document_number", rs.getString("numero_documento"));
                    map.put("sectional", rs.getString("sectional") != null ? rs.getString("sectional") : "No asignado");
                    map.put("organization", rs.getString("organization") != null ? rs.getString("organization") : "No asignado");
                    map.put("state_user", rs.getString("status_name"));
                    map.put("state_user_id", rs.getInt("estado_id"));
                    map.put("rol", rs.getString("rol_name"));
                    map.put("rol_id", rs.getInt("rol_id"));
                    resultado.add(map);
                }
            }
        }
        return resultado;
    }

    // Sirve para: Obtener la ficha técnica detallada y todos los IDs relacionales de un usuario específico para el modal de edición.
    // Qué hace: Realiza una consulta SELECT con múltiples LEFT JOINs para traer los identificadores numéricos y los nombres descriptivos.
    // Explicación de consulta SQL:
    // - Información buscada: id, nombre, apellido, email, numero_documento, fecha_nacimiento, celular, tipo de documento (y su id), género (y su id), seccional, organización (y su id), estado (y su id) y rol (y su id).
    // - Tablas participantes: usuarios u, tipo_documentos td, generos g, organizaciones o, estado_usuarios eu, roles r.
    // - Relaciones (JOINs):
    //   1. LEFT JOIN tipo_documentos td ON u.tipo_documento_id = td.id (catálogo tipo doc).
    //   2. LEFT JOIN generos g ON u.genero_id = g.id (catálogo géneros).
    //   3. LEFT JOIN organizaciones o ON u.organizacion_id = o.id (catálogo organizaciones).
    //   4. LEFT JOIN estado_usuarios eu ON u.estado_id = eu.id (catálogo estado usuarios).
    //   5. LEFT JOIN roles r ON u.rol_id = r.id (catálogo roles).
    // - Filtros aplicados: u.id = ? (clave primaria del usuario).
    public java.util.Map<String, Object> obtenerUsuarioDetalleGestion(int id) throws SQLException {
        String sql = "SELECT u.id, u.nombre, u.apellido, u.email, u.numero_documento, u.fecha_nacimiento, u.celular, "
                   + "td.descripcion AS document_type, td.id AS document_type_id, "
                   + "g.nombre AS gender, g.id AS gender_id, " 
                   + "o.seccional AS sectional, o.nombre AS organization, o.id AS organization_id, "
                   + "eu.nombre AS status_name, eu.id AS status_id, "
                   + "r.nombre AS rol_name, r.id AS rol_id "
                   + "FROM usuarios u "
                   + "LEFT JOIN tipo_documentos td ON u.tipo_documento_id = td.id "
                   + "LEFT JOIN generos g ON u.genero_id = g.id "
                   + "LEFT JOIN organizaciones o ON u.organizacion_id = o.id "
                   + "LEFT JOIN estado_usuarios eu ON u.estado_id = eu.id "
                   + "LEFT JOIN roles r ON u.rol_id = r.id "
                   + "WHERE u.id = ?";
                   
        // Qué hace: Abre la conexión a la base de datos y compila el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Configura el ID del usuario en el filtro WHERE.
            ps.setInt(1, id);
            // Qué hace: Ejecuta el query y lee las columnas mapeándolas a un Map.
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", rs.getInt("id"));
                    map.put("names", rs.getString("nombre"));
                    map.put("last_names", rs.getString("apellido"));
                    map.put("email", rs.getString("email"));
                    map.put("document_type", rs.getString("document_type") != null ? rs.getString("document_type") : "No especificado");
                    map.put("document_type_id", rs.getInt("document_type_id"));
                    map.put("document_number", rs.getString("numero_documento"));
                    map.put("birth_date", rs.getDate("fecha_nacimiento") != null ? rs.getDate("fecha_nacimiento").toString() : "");
                    map.put("gender", rs.getString("gender") != null ? rs.getString("gender") : "No especificado");
                    map.put("gender_id", rs.getInt("gender_id"));
                    map.put("phone", rs.getString("celular"));
                    map.put("sectional", rs.getString("sectional") != null ? rs.getString("sectional") : "No asignado");
                    map.put("organization", rs.getString("organization") != null ? rs.getString("organization") : "No asignado");
                    map.put("organization_id", rs.getInt("organization_id"));
                    map.put("state_user_id", rs.getInt("status_id"));
                    map.put("status", rs.getString("status_name"));
                    map.put("rol_id", rs.getInt("rol_id"));
                    
                    // Qué hace: Crea una estructura anidada de mapa para el rol, cumpliendo con la estructura esperada en el JS.
                    java.util.Map<String, Object> rolMap = new java.util.HashMap<>();
                    rolMap.put("id", rs.getInt("rol_id"));
                    rolMap.put("name", rs.getString("rol_name"));
                    map.put("rol", rolMap);
                    
                    return map;
                }
            }
        }
        return null;
    }

    // Sirve para: Actualizar el rol asignado a un usuario en el sistema.
    // Qué hace: Ejecuta una sentencia UPDATE modificando la columna rol_id por ID de usuario.
    // Explicación de consulta SQL:
    // - Operación: Actualización de columnas.
    // - Tabla afectada: usuarios.
    // - Columnas modificadas: rol_id = ? (el nuevo identificador del rol).
    // - Filtros aplicados: id = ? (clave primaria del usuario).
    public void actualizarRol(int id, int rolId) throws SQLException {
        String sql = "UPDATE usuarios SET rol_id = ? WHERE id = ?";
        // Qué hace: Obtiene la conexión y compila el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Enlaza el rolId y el ID del usuario en la sentencia.
            ps.setInt(1, rolId);
            ps.setInt(2, id);
            // Qué hace: Ejecuta el update en MySQL.
            ps.executeUpdate();
        }
    }

    // Sirve para: Modificar el estado lógico de activación de un usuario en el sistema.
    // Qué hace: Ejecuta una sentencia UPDATE modificando la columna estado_id por ID.
    // Explicación de consulta SQL:
    // - Operación: Actualización de columnas.
    // - Tabla afectada: usuarios.
    // - Columnas modificadas: estado_id = ? (el nuevo identificador de estado).
    // - Filtros aplicados: id = ? (clave primaria del usuario).
    public void actualizarEstado(int id, int estadoId) throws SQLException {
        String sql = "UPDATE usuarios SET estado_id = ? WHERE id = ?";
        // Qué hace: Obtiene la conexión y compila el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Enlaza el estadoId y el ID del usuario.
            ps.setInt(1, estadoId);
            ps.setInt(2, id);
            // Qué hace: Ejecuta la actualización física.
            ps.executeUpdate();
        }
    }

    // Sirve para: Actualizar la ficha completa de datos personales de un usuario en el sistema.
    // Qué hace: Ejecuta una sentencia UPDATE en la tabla usuarios modificando todos los campos personales y relacionales.
    // Explicación de consulta SQL:
    // - Operación: Actualización de múltiples columnas.
    // - Tabla afectada: usuarios.
    // - Columnas modificadas: nombre, apellido, tipo_documento_id, numero_documento, fecha_nacimiento, genero_id, celular, organizacion_id, email.
    // - Filtros aplicados: id = ? (identificador único del usuario).
    public void actualizarDatosPersonales(int id, String nombres, String apellidos, int tipoDocId, String numDoc, String fechaNac, int generoId, String telefono, int orgId, String email) throws SQLException {
        String sql = "UPDATE usuarios SET nombre = ?, apellido = ?, tipo_documento_id = ?, numero_documento = ?, fecha_nacimiento = ?, genero_id = ?, celular = ?, organizacion_id = ?, email = ? WHERE id = ?";
        // Qué hace: Abre la conexión a la base de datos y compila el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Enlaza secuencialmente los 10 parámetros de actualización.
            ps.setString(1, nombres);
            ps.setString(2, apellidos);
            ps.setInt(3, tipoDocId);
            ps.setString(4, numDoc);
            ps.setString(5, fechaNac);
            ps.setInt(6, generoId);
            ps.setString(7, telefono);
            ps.setInt(8, orgId);
            ps.setString(9, email);
            ps.setInt(10, id);
            // Qué hace: Aplica la actualización física en la base de datos MySQL.
            ps.executeUpdate();
        }
    }

    // Sirve para: Registrar una operación de cambio o modificación en la bitácora de auditoría de datos maestros.
    // Qué hace: Inserta una fila en la tabla historial_datos_maestros con el detalle del cambio realizado.
    // Explicación de consulta SQL:
    // - Operación: Inserción de un registro.
    // - Tabla afectada: historial_datos_maestros.
    // - Columnas insertadas: tabla_afectada, accion, valor_anterior, valor_nuevo, usuario_id.
    // - Valores insertados: tabla, tipo de acción (INSERT, UPDATE, DELETE), valores viejos y nuevos formateados en texto, e ID del operador.
    public void registrarAuditoria(String tablaAfectada, String accion, String valorAnterior, String valorNuevo, int usuarioId) throws SQLException {
        String sql = "INSERT INTO historial_datos_maestros (tabla_afectada, accion, valor_anterior, valor_nuevo, usuario_id) VALUES (?, ?, ?, ?, ?)";
        // Qué hace: Abre la conexión JDBC y prepara el statement de inserción.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Enlaza los parámetros de auditoría.
            ps.setString(1, tablaAfectada);
            ps.setString(2, accion);
            ps.setString(3, valorAnterior);
            ps.setString(4, valorNuevo);
            ps.setInt(5, usuarioId);
            // Qué hace: Ejecuta la inserción física.
            ps.executeUpdate();
        }
    }

    // Sirve para: Obtener el historial cronológico detallado de cambios aplicados a la entidad 'usuarios' para un ID determinado.
    // Qué hace: Realiza una consulta SELECT a historial_datos_maestros uniendo con la tabla usuarios y roles para recuperar el operador que ejecutó el cambio.
    // Explicación de consulta SQL:
    // - Información buscada: tabla_afectada, accion, valor_anterior, valor_nuevo, fecha de la auditoría, nombre completo del operador (user_name) y nombre de su rol (rol_nombre).
    // - Tablas participantes: historial_datos_maestros h, usuarios u, roles r.
    // - Relaciones (JOINs):
    //   1. JOIN usuarios u ON h.usuario_id = u.id (obtiene los datos del operador).
    //   2. JOIN roles r ON u.rol_id = r.id (obtiene el rol del operador).
    // - Filtros aplicados: h.tabla_afectada = 'usuarios' AND h.usuario_id = ? (el usuario auditado).
    // - Ordenamiento: ORDER BY h.fecha DESC (cronológico inverso).
    public java.util.List<java.util.Map<String, Object>> obtenerHistorialUsuario(int usuarioId) throws SQLException {
        String sql = "SELECT h.tabla_afectada, h.accion, h.valor_anterior, h.valor_nuevo, h.fecha, "
                   + "CONCAT(u.nombre, ' ', u.apellido) as user_name, r.nombre as rol_nombre "
                   + "FROM historial_datos_maestros h "
                   + "JOIN usuarios u ON h.usuario_id = u.id "
                   + "JOIN roles r ON u.rol_id = r.id "
                   + "WHERE h.tabla_afectada = 'usuarios' AND h.usuario_id = ? "
                   + "ORDER BY h.fecha DESC";
        java.util.List<java.util.Map<String, Object>> resultado = new java.util.ArrayList<>();
        // Qué hace: Obtiene la conexión y compila el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Enlaza el ID del usuario auditado.
            ps.setInt(1, usuarioId);
            // Qué hace: Ejecuta la consulta y lee el ResultSet.
            try (ResultSet rs = ps.executeQuery()) {
                // Qué hace: Instancia un formateador de fechas para presentar la marca de tiempo de forma amigable (DD/MM/AAAA HH:MM).
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
                while (rs.next()) {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("name_model", rs.getString("tabla_afectada"));
                    map.put("action_execute", rs.getString("accion"));
                    map.put("user_name", rs.getString("user_name"));
                    map.put("rol", rs.getString("rol_nombre"));
                    map.put("date_time", rs.getTimestamp("fecha") != null ? sdf.format(rs.getTimestamp("fecha")) : "");
                    map.put("status_old", rs.getString("valor_anterior"));
                    map.put("status_new", rs.getString("valor_nuevo"));
                    resultado.add(map);
                }
            }
        }
        return resultado;
    }

    // Sirve para: Aprobar la petición de activación de un usuario pendiente en el sistema.
    // Qué hace: Modifica el estado del usuario a Activo (estado_id = 1) y le asigna el rol definitivo seleccionado (rolId).
    // Explicación de consulta SQL:
    // - Operación: Actualización de columnas.
    // - Tabla afectada: usuarios.
    // - Columnas modificadas: estado_id = 1 (Activo), rol_id = ? (el rol asignado).
    // - Filtros aplicados: id = ? (clave primaria del usuario).
    public void aprobarUsuarioConRol(int id, int rolId) throws SQLException {
        String sql = "UPDATE usuarios SET estado_id = 1, rol_id = ? WHERE id = ?";
        // Qué hace: Abre la conexión a la base de datos y compila la sentencia parametrizada.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Enlaza el rol y el ID de usuario correspondientes.
            ps.setInt(1, rolId);
            ps.setInt(2, id);
            // Qué hace: Aplica la actualización física.
            ps.executeUpdate();
        }
    }

    // Sirve para: Rechazar o eliminar permanentemente una petición o registro de usuario del sistema.
    // Qué hace: Ejecuta una instrucción DELETE física sobre la tabla usuarios.
    // Explicación de consulta SQL:
    // - Operación: Eliminación física de registros.
    // - Tabla afectada: usuarios.
    // - Filtros aplicados: id = ? (clave primaria del usuario a eliminar).
    public void eliminarUsuario(int id) throws SQLException {
        String sql = "DELETE FROM usuarios WHERE id = ?";
        // Qué hace: Obtiene la conexión JDBC y prepara el statement de eliminación.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Configura el ID del usuario en el statement.
            ps.setInt(1, id);
            // Qué hace: Ejecuta la eliminación física en MySQL.
            ps.executeUpdate();
        }
    }
}
