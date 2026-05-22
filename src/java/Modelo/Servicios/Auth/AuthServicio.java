package Modelo.Servicios.Auth;

import Modelo.DAO.UsuarioDAO;
import Modelo.Entidades.Usuario;

public class AuthServicio {

    private UsuarioDAO usuarioDAO;

    // Constructor
    public AuthServicio() {
        usuarioDAO = new UsuarioDAO();
    }

    // Lógica de autenticación
    public Usuario login(String email, String password) throws Exception {

        // Buscar usuario en BD
        Usuario usuario = usuarioDAO.login(email, password);

        // Validación #1
        if (usuario == null) {
            throw new Exception("Credenciales incorrectas");
        }

        // Validación #2
        if (!usuario.getEstado().equalsIgnoreCase("activo")) {
            throw new Exception("Tu cuenta aún no está activa");
        }

        // Si todo sale bien
        return usuario;
    }
}