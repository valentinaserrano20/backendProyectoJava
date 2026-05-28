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

        // Validación #1: Si no existe el email
        if (usuario == null) {
            throw new Exception("Credenciales incorrectas");
        }

        // Validación #2: Verificar la contraseña con el hash guardado en la BD
        if (!BCrypt.checkpw(password, usuario.getContrasena())) {
            throw new Exception("Credenciales incorrectas");
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
        // Validación obligatoria: ¿Existe el email?
        if (!usuarioDAO.existeEmail(email)) {
            throw new Exception("El correo electrónico ingresado no coincide con ningún voluntario registrado.");
        }

        // Generar token matemático aleatorio seguro de 6 dígitos numéricos
        String token = String.valueOf((int)(Math.random() * 900000) + 100000);
        
        // Calcular expiración: Hora actual + 15 minutos en milisegundos
        long tiempoExpiracion = System.currentTimeMillis() + (15 * 60 * 1000);
        java.sql.Timestamp fechaExpiracion = new java.sql.Timestamp(tiempoExpiracion);

        // Guardar persistencia en BD
        usuarioDAO.guardarTokenRecuperacion(email, token, fechaExpiracion);

        // Armar cuerpo del mensaje institucional de la Defensa Civil
        String asunto = "Código de Recuperación de Contraseña - Defensa Civil Colombiana";
        String cuerpo = "Estimado Voluntario,\n\n"
                + "Se ha solicitado un restablecimiento de contraseña para su cuenta.\n"
                + "Su código de verificación es: " + token + "\n\n"
                + "Este código es de uso único y expirará en 15 minutos.\n"
                + "Si usted no solicitó este cambio, por favor ignore este correo.";

        // Envío asíncrono vía Mailtrap
        Modelo.Utilidades.CorreoUtil.enviarCorreoAsincrono(email, asunto, cuerpo);
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
}