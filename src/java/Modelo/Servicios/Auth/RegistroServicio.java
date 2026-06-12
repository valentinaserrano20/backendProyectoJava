package Modelo.Servicios.Auth;

import Modelo.DAO.UsuarioDAO;
import Modelo.Utilidades.BCrypt;

public class RegistroServicio {

    private UsuarioDAO usuarioDAO;

    public RegistroServicio() {
        usuarioDAO = new UsuarioDAO();
    }

    // Método que implementa la lógica y condiciones de negocio para el registro de una cuenta
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
        // Llama al DAO para comprobar si el correo electrónico ingresado ya pertenece a un usuario en base de datos
        if (usuarioDAO.existeEmail(email)) {
            // Lanza una excepción interrumpiendo el flujo si el correo ya está registrado en el sistema
            throw new Exception("El correo ya está registrado");
        }

        // VALIDAR DOCUMENTO REPETIDO
        // Llama al DAO para comprobar si el número de documento de identificación ya se encuentra registrado
        if (usuarioDAO.existeDocumento(numDocumento)) {
            // Lanza una excepción interrumpiendo el flujo si el documento ya está registrado en el sistema
            throw new Exception("El número de documento ya está registrado");
        }

        // CORREGIDO: Hashear la contraseña con BCrypt antes de persistirla
        // Genera la sal de cifrado y hashea la contraseña en texto plano para proteger la seguridad del usuario
        String passwordHashed = BCrypt.hashpw(password, BCrypt.gensalt());

        // REGISTRAR USUARIO
        // Invoca al método de persistencia física en el DAO enviando la información validada y la contraseña hasheada.
        // Esta línea es la que despierta la ejecución de base de datos en UsuarioDAO.java
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
