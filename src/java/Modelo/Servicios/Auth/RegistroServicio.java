package Modelo.Servicios.Auth;

import Modelo.DAO.UsuarioDAO;
import Modelo.Utilidades.BCrypt;

public class RegistroServicio {

    private UsuarioDAO usuarioDAO;

    public RegistroServicio() {
        usuarioDAO = new UsuarioDAO();
    }

    public void registrarUsuario(
            String nombres,
            String apellidos,
            String email,
            String password,
            String numDocumento,
            String fechaNac,
            String telefono,
            int tipoDocumentoId,
            int generoId,
            int organizacionId
    ) throws Exception {

        // VALIDAR EMAIL REPETIDO
        if (usuarioDAO.existeEmail(email)) {
            throw new Exception("El correo ya está registrado");
        }

        // VALIDAR DOCUMENTO REPETIDO
        if (usuarioDAO.existeDocumento(numDocumento)) {
            throw new Exception("El número de documento ya está registrado");
        }

        // CORREGIDO: Hashear la contraseña con BCrypt antes de persistirla
        String passwordHashed = BCrypt.hashpw(password, BCrypt.gensalt());

        // REGISTRAR USUARIO
        usuarioDAO.registrar(
                nombres,
                apellidos,
                email,
                passwordHashed,
                numDocumento,
                fechaNac,
                telefono,
                tipoDocumentoId,
                generoId,
                organizacionId
        );
    }
}
