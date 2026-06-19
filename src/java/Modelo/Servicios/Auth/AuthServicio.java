package Modelo.Servicios.Auth;

// =========================================================
// IMPORTACIONES OBLIGATORIAS
// =========================================================
import Modelo.DAO.UsuarioDAO;
import Modelo.Entidades.Usuario;
import Modelo.Utilidades.BCrypt;
import org.json.JSONObject;

/**
 * Qué hace: Define la clase AuthServicio encargada de coordinar toda la lógica de negocio del módulo de autenticación y seguridad.
 * Por qué existe: Separa la lógica de seguridad y el formateo de datos de usuario de los controladores web (Servlets) y del acceso a datos directo (DAOs).
 * Qué pasaría si no estuviera: Los controladores tendrían que manejar directamente hashes BCrypt, consultas SQL adicionales, transacciones y plantillas de correo, sobrecargándose de responsabilidades.
 */
public class AuthServicio {

    // Qué hace: Declara el objeto de acceso a datos para usuarios.
    // Por qué existe: Permite realizar operaciones CRUD y de búsqueda relacional sobre la tabla de usuarios en la base de datos MySQL.
    // Qué pasaría si no estuviera: Este servicio no podría comunicarse con la persistencia de datos.
    private final UsuarioDAO usuarioDAO;

    /**
     * Qué hace: Inicializa una nueva instancia de la clase de acceso a datos UsuarioDAO.
     * Por qué existe: Provee al servicio la infraestructura necesaria para consultar y actualizar registros de usuarios en la base de datos.
     * Qué pasaría si no estuviera: Al intentar consumir cualquier método que requiera usuarioDAO se arrojaría un NullPointerException.
     */
    public AuthServicio() {
        this.usuarioDAO = new UsuarioDAO();
    }

    /**
     * Qué hace: Realiza la autenticación lógica comparando la contraseña en texto plano recibida con el hash almacenado en base de datos.
     * Por qué existe: Valida de manera segura que las credenciales de correo y contraseña correspondan a un voluntario habilitado en la plataforma.
     * Qué pasaría si no estuviera: La aplicación no contaría con un proceso seguro para verificar credenciales e iniciar sesiones de usuario.
     * 
     * @param email Correo electrónico ingresado.
     * @param password Contraseña en texto plano ingresada.
     * @return El objeto de la entidad Usuario con contraseña oculta.
     * @throws Exception Si el correo no existe, la contraseña es inválida o la cuenta está inactiva.
     */
    public Usuario login(String email, String password) throws Exception {

        // Qué hace: Busca en la base de datos el registro del usuario según su correo electrónico único.
        // y luego de esto pasamos a UsuarioDAO.obtenerPorEmail, el cual se encarga de realizar la consulta SQL para mapear la fila a un objeto Usuario.
        // Qué pasaría si no estuviera: No podríamos comprobar si la cuenta de correo existe en el sistema ni comparar contraseñas.
        Usuario usuario = usuarioDAO.obtenerPorEmail(email);

        // Qué hace: Evalúa defensivamente si el resultado del DAO es nulo.
        // Por qué existe: Previene errores si el correo digitado no corresponde a ninguna cuenta y detiene el flujo temprano.
        // Qué pasaría si no estuviera: Pasar un objeto nulo a las validaciones siguientes causaría fallos críticos de NullPointerException en el servidor.
        if (usuario == null) {
            throw new Exception("El correo electrónico no se encuentra registrado.");
        }

        // Qué hace: Compara la contraseña en texto plano ingresada por el usuario con el hash seguro almacenado (BCrypt).
        // Por qué existe: Evita almacenar contraseñas legibles y permite comprobar su validez sin descifrarlas.
        // Qué pasaría si no estuviera: No habría seguridad en las contraseñas, o no se verificaría si la clave ingresada es la correcta.
        if (!BCrypt.checkpw(password, usuario.getContrasena())) {
            throw new Exception("La contraseña ingresada es incorrecta.");
        }

        // Qué hace: Comprueba el estado de activación asignado por el supervisor del sistema.
        // Por qué existe: Bloquea el acceso a usuarios registrados cuyo perfil aún no ha sido auditado y aprobado.
        // Qué pasaría si no estuviera: Cualquier usuario recién registrado podría ingresar al sistema inmediatamente sin autorización previa.
        if (usuario.getEstado() == null || !usuario.getEstado().equalsIgnoreCase("activo")) {
            throw new Exception("Tu cuenta aún no está activa");
        }

        // Qué hace: Remueve de la memoria el hash de la contraseña asignándole nulo al atributo.
        // Por qué existe: Protege el hash de contraseñas de viajar en flujos JSON y ser interceptado en la red.
        // Qué pasaría si no estuviera: El hash criptográfico de la contraseña del usuario sería transmitido al frontend, facilitando ataques de fuerza bruta si el tráfico es capturado.
        usuario.setContrasena(null);
        
        // Qué hace: Retorna la entidad del voluntario autenticado.
        return usuario;
    }

    /**
     * Qué hace: Busca al usuario por su identificador primario y genera un JSON con campos compatibles con la SPA.
     * Por qué existe: Mapea la estructura interna del backend a las llaves específicas y formato esperado por el frontend.
     * Qué pasaría si no estuviera: El frontend recibiría nombres de campos incorrectos o nulos, impidiendo la inicialización correcta del menú de la SPA.
     * 
     * @param userId Identificador primario del usuario en sesión.
     * @return JSONObject estructurado para el front.
     * @throws Exception Si ocurre un fallo en la consulta SQL.
     */
    public JSONObject obtenerPerfilMapeado(int userId) throws Exception {
        // Qué hace: Solicita a la base de datos la entidad del usuario filtrando por su identificador único.
        // y luego de esto pasamos a UsuarioDAO.obtenerPorId, el cual realiza una consulta SQL SELECT para rellenar la entidad Usuario.
        // Qué pasaría si no estuviera: No sabríamos qué usuario está intentando renderizar la SPA.
        Usuario usuario = usuarioDAO.obtenerPorId(userId);
        
        // Qué hace: Valida si el registro existe en la base de datos para evitar procesamientos sobre nulos.
        if (usuario == null) {
            return null;
        }

        // Qué hace: Inicializa un JSONObject para formatear la respuesta.
        // Por qué existe: Provee la estructura JSON fluida requerida por axios/fetch en el frontend.
        JSONObject dataSpa = new JSONObject();
        
        // Qué hace: Concatena los nombres y apellidos en una sola propiedad llamada full_name.
        // Por qué existe: Cumple con el contrato visual de visualización de nombres en la cabecera del dashboard.
        String nombreCompleto = usuario.getNombre() + " " + usuario.getApellido();
        dataSpa.put("full_name", nombreCompleto);
        
        // Qué hace: Asigna las llaves idénticas del contrato del front.
        dataSpa.put("gender_id", usuario.getGeneroId());
        dataSpa.put("email", usuario.getEmail());
        dataSpa.put("role_id", usuario.getRolId());
        dataSpa.put("state_user_id", usuario.getEstadoId());
        
        // Qué hace: Evalúa de manera segura el identificador de la organización para no serializar valores nulos erróneos.
        // Por qué existe: Evita enviar un valor nulo que rompa los selectores dropdown del front, colocando un valor por defecto seguro.
        dataSpa.put("organization_id", usuario.getOrganizacionId() != null ? usuario.getOrganizacionId() : JSONObject.NULL);

        return dataSpa;
    }
    
    /**
     * Qué hace: Genera un token numérico temporal de recuperación, lo guarda en BD y despacha un correo HTML institucional.
     * Por qué existe: Coordina el flujo seguro de generación de credenciales temporales para usuarios que olvidaron su clave.
     * Qué pasaría si no estuviera: Los voluntarios no tendrían un mecanismo automatizado y seguro para restablecer su acceso a la plataforma.
     * 
     * @param email Correo electrónico al cual enviar el token.
     * @throws Exception Si el correo no existe en la base de datos.
     */
    public void procesarSolicitudRecuperacion(String email) throws Exception {
        // Qué hace: Recupera el usuario asociado al correo ingresado.
        // y luego de esto pasamos a UsuarioDAO.obtenerPorEmail para comprobar la existencia del registro en la tabla de usuarios.
        // Qué pasaría si no estuviera: Se enviarían correos o generarían tokens para correos inexistentes, abriendo vectores de ataques de denegación de servicio.
        Usuario u = usuarioDAO.obtenerPorEmail(email);
        if (u == null) {
            throw new Exception("El correo electrónico ingresado no coincide con ningún voluntario registrado.");
        }

        // Qué hace: Genera un número aleatorio de 6 dígitos convirtiendo la función matemática Math.random() a string.
        // Por qué existe: Proporciona un código temporal fácil de memorizar e ingresar por el usuario en la interfaz.
        // Qué pasaría si no estuviera: No habría un código único para enlazar y verificar la legitimidad de la solicitud.
        String token = String.valueOf((int)(Math.random() * 900000) + 100000);
        
        // Qué hace: Calcula la fecha y hora exacta de expiración sumando 15 minutos (900,000 ms) al instante de tiempo actual.
        // Por qué existe: Establece una ventana de tiempo estricta para mitigar ataques donde un atacante intercepte el token horas después.
        // Qué pasaría si no estuviera: El token sería válido indefinidamente, lo cual es una vulnerabilidad crítica de seguridad.
        long tiempoExpiracion = System.currentTimeMillis() + (15 * 60 * 1000);
        java.sql.Timestamp fechaExpiracion = new java.sql.Timestamp(tiempoExpiracion);

        // Qué hace: Registra el token temporal y su hora límite de validez en el registro del usuario en la base de datos.
        // y luego de esto pasamos a UsuarioDAO.guardarTokenRecuperacion para persistir de forma física estas llaves.
        // Qué pasaría si no estuviera: El servidor olvidaría cuál es el token generado y no podría autenticar el siguiente paso.
        usuarioDAO.guardarTokenRecuperacion(email, token, fechaExpiracion);

        // Qué hace: Define el asunto del correo institucional.
        String asunto = "Código de Recuperación de Contraseña - Defensa Civil Colombiana";

        // Qué hace: Concatena los nombres para personalizar la correspondencia.
        String nombreUsuario = u.getNombre() + " " + u.getApellido();

        // Qué hace: Construye la plantilla HTML del correo inyectando dinámicamente el nombre y el código numérico.
        // y luego de esto pasamos a CorreoUtil.obtenerPlantillaHTML, el cual arma el diseño CSS adaptativo del cuerpo del mensaje.
        // Qué pasaría si no estuviera: Tendríamos que enviar un correo en texto plano, lo que desmejora la experiencia visual corporativa.
        String cuerpoHTML = Modelo.Utilidades.CorreoUtil.obtenerPlantillaHTML(nombreUsuario, token);

        // Qué hace: Lanza el hilo asíncrono para enviar el email a través del host de mensajería SMTP configurado.
        // y luego de esto pasamos a CorreoUtil.enviarCorreoAsincrono, el cual abre la conexión con el servidor de correo en segundo plano.
        // Qué pasaría si no estuviera: La petición HTTP del controlador se bloquearía durante varios segundos esperando la respuesta del servidor de correo, congelando la interfaz.
        Modelo.Utilidades.CorreoUtil.enviarCorreoAsincrono(email, asunto, cuerpoHTML);
    }

    /**
     * Qué hace: Consulta si existe un usuario activo que posea el token de verificación y que no haya expirado.
     * Por qué existe: Sirve de middleware para autorizar el paso al formulario final de cambio de contraseña.
     * Qué pasaría si no estuviera: Un atacante podría saltarse la validación y enviar contraseñas de cambio aleatorias sin poseer un código válido.
     * 
     * @param token Código de 6 dígitos.
     * @return true si el token es válido y vigente, false de lo contrario.
     * @throws Exception Si hay errores en la consulta de base de datos.
     */
    public boolean verificarTokenValido(String token) throws Exception {
        // Qué hace: Consulta al DAO si el token existe y está vigente.
        // y luego de esto pasamos a UsuarioDAO.obtenerPorTokenValido para realizar la consulta SQL comparando con el timestamp actual de MySQL.
        Usuario u = usuarioDAO.obtenerPorTokenValido(token);
        return u != null;
    }

    /**
     * Qué hace: Realiza la encriptación final de la nueva contraseña con sal y actualiza el registro en la BD, destruyendo el token.
     * Por qué existe: Completa de forma definitiva la recuperación de contraseña de forma segura.
     * Qué pasaría si no estuviera: El usuario no podría persistir su nueva contraseña o el token seguiría activo permitiendo más cambios.
     * 
     * @param token Código temporal que autoriza la operación.
     * @param nuevaContrasena Nueva clave en texto plano.
     * @throws Exception Si el token es inválido o ya expiró.
     */
    public void restablecerContrasenaFinal(String token, String nuevaContrasena) throws Exception {
        // Qué hace: Verifica por última vez si el token de autorización sigue vigente.
        // y luego de esto pasamos a UsuarioDAO.obtenerPorTokenValido para consultar el estado en la base de datos relacional.
        Usuario u = usuarioDAO.obtenerPorTokenValido(token);
        if (u == null) {
            throw new Exception("El código de verificación ha expirado o es inválido. Inicie el proceso de nuevo.");
        }

        // Qué hace: Hashea con BCrypt la contraseña en texto plano para asegurar que nunca se almacene de forma legible.
        // Por qué existe: Cumple con el estándar de seguridad OWASP para la protección de secretos de autenticación.
        // Qué pasaría si no estuviera: Almacenaríamos la clave en texto plano en la BD, lo cual es una falla de seguridad muy grave.
        String contrasenaHasheada = Modelo.Utilidades.BCrypt.hashpw(nuevaContrasena, Modelo.Utilidades.BCrypt.gensalt());

        // Qué hace: Escribe la nueva clave hasheada y remueve (asigna NULL) al token de recuperación en la base de datos.
        // y luego de esto pasamos a UsuarioDAO.actualizarContrasenaYLimpiarToken, el cual ejecuta el UPDATE SQL en MySQL.
        // Qué pasaría si no estuviera: La contraseña no se actualizaría en el sistema y el token podría reutilizarse maliciosamente.
        usuarioDAO.actualizarContrasenaYLimpiarToken(u.getId(), contrasenaHasheada);
    }

    /**
     * Qué hace: Consulta la base de datos mediante JOINs explícitos y empaqueta el perfil detallado requerido por el front.
     * Por qué existe: Obtiene la información enriquecida con descripciones textuales de roles, géneros y organizaciones en un solo viaje a la base de datos.
     * Qué pasaría si no estuviera: Tendríamos que hacer múltiples consultas individuales a diferentes tablas desde el servlet, incrementando el tiempo de respuesta.
     * 
     * @param userId ID único del usuario en sesión.
     * @return JSONObject detallado del usuario.
     * @throws Exception Si ocurre un fallo en la conexión o ejecución SQL.
     */
    public JSONObject obtenerPerfilDetalladoMapeado(int userId) throws Exception {
        // Qué hace: Define la consulta SQL relacional limpia con LEFT JOINs.
        // Por qué existe: Trae la descripción del tipo de documento, género y organización/seccional asociada al usuario sin requerir tablas de municipios inexistentes.
        String sql = "SELECT u.nombre, u.apellido, u.email, u.numero_documento, u.fecha_nacimiento, u.celular, "
                   + "td.descripcion AS document_type, "
                   + "g.nombre AS gender, " 
                   + "o.seccional AS sectional, "
                   + "o.nombre AS organization "
                   + "FROM usuarios u "
                   + "LEFT JOIN tipo_documentos td ON u.tipo_documento_id = td.id "
                   + "LEFT JOIN generos g ON u.genero_id = g.id "
                   + "LEFT JOIN organizaciones o ON u.organizacion_id = o.id "
                   + "WHERE u.id = ?";
                   
        // Qué hace: Abre la conexión a la base de datos y compila el prepared statement dentro de un bloque try-with-resources.
        // Por qué existe: Asegura que la conexión y el statement se liberen e invaliden automáticamente al concluir el bloque, previniendo fugas de sockets.
        // Qué pasaría si no estuviera: La base de datos colapsaría al cabo de unas horas por exceso de conexiones abiertas inactivas.
        try (java.sql.Connection con = Modelo.Config.Conexion.obtener();
             java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            
            // Qué hace: Enlaza el parámetro dinámico ID en el marcador posicional.
            // Por qué existe: Previene ataques de inyección SQL sanitizando el parámetro.
            // Qué pasaría si no estuviera: La consulta no tendría filtro para buscar al usuario específico y daría un error sintáctico.
            ps.setInt(1, userId);
            
            // Qué hace: Ejecuta el query y abre el ResultSet.
            // Por qué existe: Permite iterar sobre las filas de respuesta devueltas por MySQL.
            // Qué pasaría si no estuviera: No podríamos capturar los datos seleccionados por el motor SQL.
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    JSONObject d = new JSONObject();
                    // Qué hace: Extrae las columnas del ResultSet y las añade al JSON de respuesta.
                    // Por qué existe: Asigna cada valor en el formato exacto de variables camelCase que espera el Javascript de la vista.
                    d.put("names", rs.getString("nombre"));
                    d.put("last_names", rs.getString("apellido"));
                    d.put("document_type", rs.getString("document_type") != null ? rs.getString("document_type") : "No especificado");
                    d.put("document_number", rs.getString("numero_documento"));
                    d.put("birth_date", rs.getDate("fecha_nacimiento") != null ? rs.getDate("fecha_nacimiento").toString() : "");
                    d.put("gender", rs.getString("gender") != null ? rs.getString("gender") : "No especificado");
                    d.put("sectional", rs.getString("sectional") != null ? rs.getString("sectional") : "No asignado");
                    d.put("organization", rs.getString("organization") != null ? rs.getString("organization") : "No asignado");
                    d.put("phone", rs.getString("celular"));
                    d.put("email", rs.getString("email"));
                    return d;
                }
            }
        }
        return null;
    }

    /**
     * Qué hace: Valida las credenciales de seguridad vigentes y actualiza el teléfono del perfil del usuario.
     * Por qué existe: Protege los cambios de datos sensibles validando que el usuario en sesión realmente conoce su clave actual.
     * Qué pasaría si no estuviera: Cualquier persona con acceso físico temporal al computador de un voluntario logueado podría cambiar sus datos sin autorización.
     * 
     * @param userId Identificador primario del voluntario.
     * @param nuevoTelefono Nuevo número celular.
     * @param passwordConfirmacion Contraseña actual ingresada para confirmar.
     * @throws Exception Si el usuario no existe, la clave es incorrecta o falla la escritura SQL.
     */
    public void modificarTelefonoPerfil(int userId, String nuevoTelefono, String passwordConfirmacion) throws Exception {
        // Qué hace: Recupera el objeto del usuario a validar.
        // y luego de esto pasamos a UsuarioDAO.obtenerPorId para consultar la existencia de la cuenta en MySQL.
        Usuario u = usuarioDAO.obtenerPorId(userId);
        if (u == null) throw new Exception("Usuario inexistente.");
        
        // Qué hace: Obtiene de forma directa la contraseña hasheada actual del registro.
        // y luego de esto pasamos a obtenerHashPasswordDirecto, el cual hace una consulta optimizada a la BD.
        String hashReal = obtenerHashPasswordDirecto(userId);
        
        // Qué hace: Verifica la firma criptográfica de confirmación.
        // Qué pasaría si no estuviera: Permitiríamos cambios de datos de perfil sin validar la identidad del solicitante.
        if (!Modelo.Utilidades.BCrypt.checkpw(passwordConfirmacion, hashReal)) {
            throw new Exception("La contraseña de confirmación ingresada es incorrecta.");
        }
        
        // Qué hace: Envía la actualización del campo teléfono a la base de datos.
        // y luego de esto pasamos a UsuarioDAO.actualizarTelefono para aplicar el UPDATE SQL físico.
        usuarioDAO.actualizarTelefono(userId, nuevoTelefono);
    }

    /**
     * Qué hace: Valida las credenciales actuales, comprueba disponibilidad del nuevo correo y actualiza el registro en la BD.
     * Por qué existe: Evita colisiones de correos electrónicos únicos y protege la actualización de credenciales.
     * Qué pasaría si no estuviera: Dos usuarios podrían acabar con el mismo correo registrado, rompiendo la restricción única del sistema.
     * 
     * @param userId Identificador único del usuario.
     * @param nuevoEmail Nuevo correo electrónico a registrar.
     * @param passwordConfirmacion Contraseña de seguridad de confirmación.
     * @throws Exception Si la clave de confirmación es incorrecta o el nuevo email ya está en uso.
     */
    public void modificarEmailPerfil(int userId, String nuevoEmail, String passwordConfirmacion) throws Exception {
        Usuario u = usuarioDAO.obtenerPorId(userId);
        if (u == null) throw new Exception("Usuario inexistente.");
        
        // Qué hace: Obtiene la clave encriptada de la base de datos.
        String hashReal = obtenerHashPasswordDirecto(userId);
        if (!Modelo.Utilidades.BCrypt.checkpw(passwordConfirmacion, hashReal)) {
            throw new Exception("La contraseña de confirmación ingresada es incorrecta.");
        }
        
        // Qué hace: Comprueba que el correo nuevo no esté en uso por otro registro.
        // y luego de esto pasamos a UsuarioDAO.existeEmail para realizar la comprobación de existencia.
        // Qué pasaría si no estuviera: El motor de base de datos podría lanzar una excepción de clave duplicada cruda que tumbaría la llamada HTTP sin un mensaje descriptivo.
        if (!u.getEmail().equalsIgnoreCase(nuevoEmail) && usuarioDAO.existeEmail(nuevoEmail)) {
            throw new Exception("El nuevo correo electrónico ya está registrado en el sistema por otro usuario.");
        }
        
        // Qué hace: Envía la actualización física del email a la capa DAO.
        // y luego de esto pasamos a UsuarioDAO.actualizarEmail para ejecutar el query en MySQL.
        usuarioDAO.actualizarEmail(userId, nuevoEmail);
    }

    /**
     * Qué hace: Compara la contraseña actual del voluntario y actualiza su credencial cifrando la nueva contraseña.
     * Por qué existe: Habilita el cambio regular de contraseñas desde el módulo de configuración de perfil del voluntario.
     * Qué pasaría si no estuviera: Los usuarios no podrían cambiar sus contraseñas de acceso periódicamente desde la interfaz.
     * 
     * @param userId ID del voluntario.
     * @param passwordActual Contraseña previa ingresada.
     * @param passwordNueva Nueva contraseña de reemplazo.
     * @throws Exception Si la contraseña actual es errónea.
     */
    public void modificarContrasenaPerfil(int userId, String passwordActual, String passwordNueva) throws Exception {
        // Qué hace: Obtiene la clave hasheada vigente de base de datos.
        String hashReal = obtenerHashPasswordDirecto(userId);
        
        // Qué hace: Valida la firma del password actual ingresado.
        if (!Modelo.Utilidades.BCrypt.checkpw(passwordActual, hashReal)) {
            throw new Exception("Su contraseña actual ingresada es incorrecta.");
        }
        
        // Qué hace: Genera un nuevo hash irreversible para la nueva contraseña.
        String nuevoHash = Modelo.Utilidades.BCrypt.hashpw(passwordNueva, Modelo.Utilidades.BCrypt.gensalt());
        
        // Qué hace: Persiste el nuevo hash en el registro del voluntario en la BD.
        // y luego de esto pasamos a UsuarioDAO.actualizarContrasena para ejecutar la actualización física.
        usuarioDAO.actualizarContrasena(userId, nuevoHash);
    }

    /**
     * Qué hace: Ejecuta una consulta de proyección optimizada para recuperar únicamente la columna 'contraseña' de un usuario.
     * Por qué existe: Minimiza el tráfico de red y la memoria del servidor evitando consultar todo el registro completo del usuario cuando solo se requiere verificar la clave.
     * Qué pasaría si no estuviera: Tendríamos que consultar siempre la entidad completa con todas sus columnas y catálogos, ralentizando las comprobaciones de perfil.
     * 
     * @param id ID del usuario.
     * @return El string del hash de la contraseña.
     * @throws Exception Si ocurre un fallo en el motor de base de datos.
     */
    private String obtenerHashPasswordDirecto(int id) throws Exception {
        String sql = "SELECT contraseña FROM usuarios WHERE id = ?";
        
        // Qué hace: Crea de manera robusta la conexión y prepara la sentencia SQL dentro de un try-with-resources.
        // Por qué existe: Libera los recursos de red del pool de conexiones al terminar la ejecución.
        try (java.sql.Connection con = Modelo.Config.Conexion.obtener();
             java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, id);
            
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("contraseña");
                }
            }
        }
        throw new Exception("Error al recuperar las credenciales de validación.");
    }
}