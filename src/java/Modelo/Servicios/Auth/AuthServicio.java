package Modelo.Servicios.Auth;

import Modelo.DAO.UsuarioDAO;
import Modelo.Entidades.Usuario;
import Modelo.Utilidades.BCrypt;

public class AuthServicio {

    private UsuarioDAO usuarioDAO;

    public AuthServicio() {
        usuarioDAO = new UsuarioDAO();
    }

    // CORREGIDO: Lógica de autenticación segura utilizando verificación de hash BCrypt
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
}
