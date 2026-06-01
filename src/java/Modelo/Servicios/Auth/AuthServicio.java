package Modelo.Servicios.Auth;

// =========================================================
// IMPORTACIONES OBLIGATORIAS
// =========================================================
import Modelo.DAO.UsuarioDAO;
import Modelo.Entidades.Usuario;
import Modelo.Utilidades.BCrypt;
import org.json.JSONObject;

/**
 * Clase: AuthServicio
 * Capa: Modelo.Servicios.Auth
 * Responsabilidad:
 * - Gestionar la lógica de autenticación segura (BCrypt).
 * - Mapear y adaptar datos del usuario en sesión según el contrato del SPA.
 */
public class AuthServicio {

    private final UsuarioDAO usuarioDAO;

    /**
     * Constructor que inicializa el acceso a datos.
     */
    public AuthServicio() {
        this.usuarioDAO = new UsuarioDAO();
    }

    /**
     * Lógica de autenticación segura utilizando verificación de hash BCrypt.
     * (Conserva intacta tu lógica original)
     */
    public Usuario login(String email, String password) throws Exception {

        // Buscar usuario en BD por su email únicamente
        Usuario usuario = usuarioDAO.obtenerPorEmail(email);

        // Sirve para: Verificar si el correo ingresado existe en la base de datos
        // Qué hace: Lanza una excepción específica si no se encuentra ningún registro
        // Por qué es importante: Provee feedback claro al usuario indicando que el correo no está registrado
        if (usuario == null) {
            throw new Exception("El correo electrónico no se encuentra registrado.");
        }

        // Sirve para: Validar la contraseña contra el hash de BCrypt almacenado
        // Qué hace: Lanza una excepción específica si la contraseña no coincide
        // Por qué es importante: Indica de forma precisa que la contraseña ingresada para el correo es incorrecta
        if (!BCrypt.checkpw(password, usuario.getContrasena())) {
            throw new Exception("La contraseña ingresada es incorrecta.");
        }

        // Validación #3: Verificar estado de cuenta
        if (usuario.getEstado() == null || !usuario.getEstado().equalsIgnoreCase("activo")) {
            throw new Exception("Tu cuenta aún no está activa");
        }

        // Si la validación es correcta, limpia el hash antes de retornarlo por seguridad
        usuario.setContrasena(null);
        return usuario;
    }

    /**
     * NUEVO MÉTODO ADICIONADO:
     * Recupera el usuario por su ID (desde la sesión) y formatea los campos para el Home del SPA.
     * @param userId Identificador extraído de la HttpSession.
     * @return JSONObject mapeado en inglés / snake_case o null si no se encuentra el registro.
     * @throws Exception Si ocurre un fallo en cascada desde la base de datos.
     */
    public JSONObject obtenerPerfilMapeado(int userId) throws Exception {
        // Consultar el POJO a la capa de datos
        Usuario usuario = usuarioDAO.obtenerPorId(userId);
        if (usuario == null) {
            return null;
        }

        // Instanciar el objeto JSON requerido por los estándares contractuales del SPA
        JSONObject dataSpa = new JSONObject();
        
        // Mapeo 1: full_name <- nombre + ' ' + apellido
        String nombreCompleto = usuario.getNombre() + " " + usuario.getApellido();
        dataSpa.put("full_name", nombreCompleto);
        
        // Mapeo 2: gender_id <- generoId obtenido numéricamente desde la entidad
        dataSpa.put("gender_id", usuario.getGeneroId());
        
        // Campos de control adicionales útiles para el ecosistema del Frontend
        dataSpa.put("email", usuario.getEmail());
        dataSpa.put("role_id", usuario.getRolId());
        dataSpa.put("state_user_id", usuario.getEstadoId());
        dataSpa.put("organization_id", usuario.getOrganizacionId() != null ? usuario.getOrganizacionId() : JSONObject.NULL);

        return dataSpa;
    }
    
    /**
     * Lógica de Negocio: Valida la existencia del email, genera el token de 6 dígitos y dispara el correo.
     */
    public void procesarSolicitudRecuperacion(String email) throws Exception {
        // Validación obligatoria y recuperación de datos del voluntario
        // Sirve para: Comprobar si el email está registrado en el sistema y obtener su información de perfil
        // Qué hace: Llama a usuarioDAO.obtenerPorEmail para recuperar la entidad Usuario
        // Por qué es importante: Permite personalizar la plantilla del correo con el nombre del usuario y valida si existe
        Usuario u = usuarioDAO.obtenerPorEmail(email);
        if (u == null) {
            throw new Exception("El correo electrónico ingresado no coincide con ningún voluntario registrado.");
        }

        // Generar token matemático aleatorio seguro de 6 dígitos numéricos
        String token = String.valueOf((int)(Math.random() * 900000) + 100000);
        
        // Calcular expiración: Hora actual + 15 minutos en milisegundos
        long tiempoExpiracion = System.currentTimeMillis() + (15 * 60 * 1000);
        java.sql.Timestamp fechaExpiracion = new java.sql.Timestamp(tiempoExpiracion);

        // Guardar persistencia en BD
        usuarioDAO.guardarTokenRecuperacion(email, token, fechaExpiracion);

        // Armar el asunto institucional del correo de recuperación
        String asunto = "Código de Recuperación de Contraseña - Defensa Civil Colombiana";

        // Obtener el nombre completo del voluntario para personalizar el saludo
        String nombreUsuario = u.getNombre() + " " + u.getApellido();

        // Generar la plantilla de diseño HTML corporativa
        // Sirve para: Generar la estructura del cuerpo del correo electrónico con diseño y estilos enriquecidos
        // Qué hace: Llama a la función estática CorreoUtil.obtenerPlantillaHTML para procesar los datos del voluntario
        // Por qué es importante: Reemplaza el texto plano básico por una plantilla institucional atractiva e interactiva con logos y badges
        String cuerpoHTML = Modelo.Utilidades.CorreoUtil.obtenerPlantillaHTML(nombreUsuario, token);

        // Envío asíncrono del correo electrónico con la plantilla HTML
        // Sirve para: Transferir la plantilla de correo compilada en segundo plano al servidor SMTP de Mailtrap
        // Qué hace: Dispara el método asíncrono enviarCorreoAsincrono para evitar congelar el flujo de la aplicación web
        // Por qué es importante: El usuario recibe el código rápidamente y de manera atractiva sin experimentar tiempos de espera en el navegador
        Modelo.Utilidades.CorreoUtil.enviarCorreoAsincrono(email, asunto, cuerpoHTML);
    }

    /**
     * Lógica de Negocio: Comprueba de manera estricta si el token digitado por el usuario es correcto y vigente.
     */
    public boolean verificarTokenValido(String token) throws Exception {
        Usuario u = usuarioDAO.obtenerPorTokenValido(token);
        return u != null;
    }

    /**
     * Lógica de Negocio: Procesa el cambio final encriptando la nueva clave con el BCrypt interno del proyecto.
     */
    public void restablecerContrasenaFinal(String token, String nuevaContrasena) throws Exception {
        Usuario u = usuarioDAO.obtenerPorTokenValido(token);
        if (u == null) {
            throw new Exception("El código de verificación ha expirado o es inválido. Inicie el proceso de nuevo.");
        }

        // Hasheo estricto con el BCrypt local
        String contrasenaHasheada = Modelo.Utilidades.BCrypt.hashpw(nuevaContrasena, Modelo.Utilidades.BCrypt.gensalt());

        // Actualización definitiva de la base de datos
        usuarioDAO.actualizarContrasenaYLimpiarToken(u.getId(), contrasenaHasheada);
    }
    
    // ... Tus métodos de login(), obtenerPerfilMapeado() y recuperación se quedan intactos ...

    /**
     * Recupera y empaqueta el perfil completo con JOINs en el contrato exacto de campos del JS.
     */
    public JSONObject obtenerPerfilDetalladoMapeado(int userId) throws Exception {
        // Ejecutar consulta SQL extendida desde el DAO
        // Para asegurar precisión y no alterar tu POJO Usuario básico con campos de catálogos cruzados,
        // ejecutaremos una extracción limpia mapeada a las llaves que pide tu helper front.
        // Sirve para: Definir la consulta SQL que recuperará los detalles del perfil del usuario (nombre, apellido, email, documento, fecha de nacimiento, celular, tipo de documento, género, seccional y organización).
        // Qué hace: Consulta los datos de la tabla usuarios y realiza LEFT JOINs con las tablas tipo_documentos, generos y organizaciones, obteniendo la columna seccional directamente de la tabla organizaciones.
        // Por qué es importante: Evita hacer JOINs con las tablas municipios y departamentos que no existen en la base de datos física actual, previniendo así un error 500 al cargar el perfil del usuario.
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
                   
        try (java.sql.Connection con = Modelo.Config.Conexion.obtener();
             java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    JSONObject d = new JSONObject();
                    // Mapeo milimétrico para el helper cargarDatos.js del SPA
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
     * Regla de Negocio: Modifica el teléfono confirmando la contraseña actual de seguridad.
     */
    public void modificarTelefonoPerfil(int userId, String nuevoTelefono, String passwordConfirmacion) throws Exception {
        Usuario u = usuarioDAO.obtenerPorId(userId);
        if (u == null) throw new Exception("Usuario inexistente.");
        
        // Volver a consultar la clave hash real de la BD para validar
        String hashReal = obtenerHashPasswordDirecto(userId);
        
        if (!Modelo.Utilidades.BCrypt.checkpw(passwordConfirmacion, hashReal)) {
            throw new Exception("La contraseña de confirmación ingresada es incorrecta.");
        }
        
        usuarioDAO.actualizarTelefono(userId, nuevoTelefono);
    }

    /**
     * Regla de Negocio: Modifica el correo validando la contraseña y que el nuevo correo no esté duplicado.
     */
    public void modificarEmailPerfil(int userId, String nuevoEmail, String passwordConfirmacion) throws Exception {
        Usuario u = usuarioDAO.obtenerPorId(userId);
        if (u == null) throw new Exception("Usuario inexistente.");
        
        String hashReal = obtenerHashPasswordDirecto(userId);
        if (!Modelo.Utilidades.BCrypt.checkpw(passwordConfirmacion, hashReal)) {
            throw new Exception("La contraseña de confirmación ingresada es incorrecta.");
        }
        
        // Evitar que colisione el email único si cambió por uno ya existente en otro usuario
        if (!u.getEmail().equalsIgnoreCase(nuevoEmail) && usuarioDAO.existeEmail(nuevoEmail)) {
            throw new Exception("El nuevo correo electrónico ya está registrado en el sistema por otro usuario.");
        }
        
        usuarioDAO.actualizarEmail(userId, nuevoEmail);
    }

    /**
     * Regla de Negocio: Cambia la contraseña actual por una nueva con encriptación BCrypt institucional.
     */
    public void modificarContrasenaPerfil(int userId, String passwordActual, String passwordNueva) throws Exception {
        String hashReal = obtenerHashPasswordDirecto(userId);
        
        if (!Modelo.Utilidades.BCrypt.checkpw(passwordActual, hashReal)) {
            throw new Exception("Su contraseña actual ingresada es incorrecta.");
        }
        
        // Cifrar la nueva contraseña
        String nuevoHash = Modelo.Utilidades.BCrypt.hashpw(passwordNueva, Modelo.Utilidades.BCrypt.gensalt());
        usuarioDAO.actualizarContrasena(userId, nuevoHash);
    }

    /**
     * Helper privado para extraer la clave encriptada de la tabla usuarios.
     */
    private String obtenerHashPasswordDirecto(int id) throws Exception {
        String sql = "SELECT contraseña FROM usuarios WHERE id = ?";
        try (java.sql.Connection con = Modelo.Config.Conexion.obtener();
             java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("contraseña");
            }
        }
        throw new Exception("Error al recuperar las credenciales de validación.");
    }
}