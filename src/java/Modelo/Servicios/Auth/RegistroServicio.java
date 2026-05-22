package Modelo.Servicios.Auth;

import Modelo.DAO.UsuarioDAO;

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

        // REGISTRAR USUARIO
        usuarioDAO.registrar(
                nombres,
                apellidos,
                email,
                password,
                numDocumento,
                fechaNac,
                telefono,
                tipoDocumentoId,
                generoId,
                organizacionId
        );
    }
}