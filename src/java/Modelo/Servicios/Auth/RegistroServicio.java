package Modelo.Servicios.Auth;

import Modelo.DAO.UsuarioDAO;
import Modelo.Utilidades.BCrypt;

/**
 * Qué hace: Define e inicializa la clase de servicio RegistroServicio.
 * Por qué existe: Aloja y coordina la lógica de validación de negocio necesaria antes de permitir la inserción de un nuevo usuario en la base de datos.
 * Qué pasaría si no estuviera: Las validaciones de negocio tendrían que hacerse en los controladores web, o peor, se registrarían datos directamente sin saneamiento ni validación de unicidad.
 */
public class RegistroServicio {

    // Qué hace: Declara la variable de acceso a datos para usuarios.
    // Por qué existe: Provee el puente hacia la tabla de usuarios en la base de datos para realizar búsquedas de duplicados e inserciones.
    // Qué pasaría si no estuviera: El servicio no podría realizar consultas de verificación ni persistir al usuario en la base de datos.
    private UsuarioDAO usuarioDAO;

    /**
     * Qué hace: Constructor que inicializa el acceso a la base de datos a través de UsuarioDAO.
     * Por qué existe: Instancia el objeto necesario para interactuar con la capa de datos.
     * Qué pasaría si no estuviera: Al intentar consumir el servicio obtendríamos un error NullPointerException al acceder a la variable usuarioDAO.
     */
    public RegistroServicio() {
        usuarioDAO = new UsuarioDAO();
    }

    /**
     * Qué hace: Ejecuta la validación de correos y documentos repetidos, hashea la contraseña del voluntario y la registra formalmente en el sistema.
     * Por qué existe: Centraliza las reglas de negocio para la creación de cuentas del voluntariado en un único punto del backend.
     * Qué pasaría si no estuviera: No existiría una lógica coherente que garantice la integridad de los datos de los usuarios registrados ni el almacenamiento seguro de sus contraseñas.
     * 
     * @param nombres Nombres del voluntario.
     * @param apellidos Apellidos del voluntario.
     * @param email Correo electrónico único.
     * @param password Contraseña elegida en texto plano.
     * @param numDocumento Número de identificación.
     * @param fechaNac Fecha de nacimiento.
     * @param telefono Celular de contacto.
     * @param tipoDocumentoId Identificador del tipo de documento.
     * @param generoId Identificador del género.
     * @param organizacionId Identificador de la seccional a la que pertenece.
     * @throws Exception Si el correo o el documento ya existen.
     */
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

        // =========================================
        // 1. VALIDAR EMAIL REPETIDO
        // =========================================
        // Qué hace: Consulta en la base de datos si el email provisto ya pertenece a algún voluntario registrado en el sistema.
        // y luego de esto pasamos a UsuarioDAO.existeEmail, el cual realiza una consulta SELECT en MySQL buscando coincidencias.
        // Por qué existe: Previene colisiones de identidad en el inicio de sesión, donde el correo actúa como la credencial principal única de acceso.
        // Qué pasaría si no estuviera: Dos usuarios podrían registrarse con el mismo correo, impidiendo que el motor de login diferencie a qué perfil ingresar.
        if (usuarioDAO.existeEmail(email)) {
            // Lanza una excepción interrumpiendo el flujo si el correo ya está registrado en el sistema
            throw new Exception("El correo ya está registrado");
        }

        // =========================================
        // 2. VALIDAR DOCUMENTO REPETIDO
        // =========================================
        // Qué hace: Consulta en la base de datos si el número de documento de identidad ingresado ya existe.
        // y luego de esto pasamos a UsuarioDAO.existeDocumento, el cual realiza la consulta SELECT correspondiente.
        // Por qué existe: Protege contra fraudes y duplicidad de perfiles, garantizando que una persona física solo tenga una cuenta activa en el sistema.
        // Qué pasaría si no estuviera: Una misma persona física podría crearse múltiples cuentas voluntarias con nombres ficticios pero usando su misma cédula.
        if (usuarioDAO.existeDocumento(numDocumento)) {
            // Lanza una excepción interrumpiendo el flujo si el documento ya está registrado en el sistema
            throw new Exception("El número de documento ya está registrado");
        }

        // =========================================
        // 3. CIFRADO DE CONTRASEÑA
        // =========================================
        // Qué hace: Genera la sal de cifrado y hashea la contraseña en texto plano con el algoritmo BCrypt.
        // Por qué existe: Asegura la confidencialidad de la clave del usuario ante eventuales filtraciones o robos de la base de datos física.
        // Qué pasaría si no estuviera: La contraseña se guardaría en texto plano (lectura abierta), violando estándares básicos de protección de datos personales.
        String passwordHashed = BCrypt.hashpw(password, BCrypt.gensalt());

        // =========================================
        // 4. REGISTRAR USUARIO (PERSISTENCIA)
        // =========================================
        // Qué hace: Invoca al método de persistencia física en el DAO enviando la información validada y la contraseña hasheada.
        // y luego de esto pasamos a UsuarioDAO.registrar para insertar la fila a través de un INSERT Prepared Statement en la base de datos relacional.
        // Qué pasaría si no estuviera: Las validaciones de negocio se completarían satisfactoriamente pero los datos del voluntario nunca se guardarían físicamente, perdiéndose la solicitud.
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
