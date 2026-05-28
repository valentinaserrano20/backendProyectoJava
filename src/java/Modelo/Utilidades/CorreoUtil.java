package Modelo.Utilidades;

// =========================================================
// IMPORTACIONES ESTÁNDAR (Ya reconocidas por tus JARs en lib)
// =========================================================
// Importaciones explícitas de la librería Jakarta Mail
// Sirven para: Evitar la ambigüedad que producen las importaciones con comodines (*) y asegurar que el compilador resuelva cada clase de correo correctamente
// Qué hace: Importa de forma individual las clases Authenticator, Message, PasswordAuthentication, Session, Transport, InternetAddress y MimeMessage
// Por qué es importante: Garantiza la estabilidad de la compilación del backend en servidores Glassfish/Tomcat y previene errores de clase no encontrada
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Clase: CorreoUtil
 * Capa: Modelo.Utilidades
 * Responsabilidad: Conectarse de forma nativa al servidor SMTP de Mailtrap.
 */
public class CorreoUtil {

    // Configuración universal del servidor SMTP de Mailtrap
    private static final String HOST = "sandbox.smtp.mailtrap.io";
    private static final String PORT = "587"; // Puerto estándar seguro para TLS
    
    // =========================================================
    // CREDENCIALES DE MAILTRAP
    // =========================================================
    private static final String USER = "e47d9a4d9da5e4"; 
    private static final String PASS = "9cf32ec8bad56f"; 
    
    // Remitente institucional simulado
    private static final String FROM = "no-reply@SenaDC.co";

    /**
     * Envía un correo electrónico de recuperación en un hilo secundario asíncrono
     * para que la API responda al SPA de inmediato sin congelar la pantalla.
     */
    public static void enviarCorreoAsincrono(String destinatario, String asunto, String cuerpoTexto) {
        // Log de arranque: confirma que el método fue invocado antes de lanzar el hilo
        System.out.println(" [CorreoUtil] Iniciando envío asíncrono a: " + destinatario);

        new Thread(() -> {
            try {
                // PASO 1: Configurar propiedades del protocolo SMTP
                System.out.println("[CorreoUtil] Paso 1: Configurando propiedades SMTP...");
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true"); // Activar cifrado de datos TLS
                props.put("mail.smtp.host", HOST);
                props.put("mail.smtp.port", PORT);
                // Timeout de conexión de 10 segundos para detectar bloqueos de red
                props.put("mail.smtp.connectiontimeout", "10000");
                props.put("mail.smtp.timeout", "10000");

                // PASO 2: Crear sesión autenticada contra el servidor Mailtrap
                System.out.println("🔵 [CorreoUtil] Paso 2: Creando sesión SMTP con usuario: " + USER);
                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(USER, PASS);
                    }
                });
                // Activa el log detallado de JavaMail en consola para ver la negociación TLS
                session.setDebug(true);

                // PASO 3: Construir el mensaje con remitente, destinatario y contenido
                System.out.println(" [CorreoUtil] Paso 3: Construyendo mensaje de correo...");
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(FROM, "Defensa Civil Colombiana"));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
                message.setSubject(asunto);
                
                // PASO 3.5: Asignar el cuerpo del correo en formato HTML
                // Sirve para: Configurar el cuerpo del correo para que soporte etiquetas y estilos HTML en vez de texto plano
                // Qué hace: Llama a setContent indicando el tipo MIME "text/html; charset=utf-8" pasándole el contenido HTML generado
                // Por qué es importante: Permite renderizar una plantilla visualmente agradable e institucional en los gestores de correo
                message.setContent(cuerpoTexto, "text/html; charset=utf-8");

                // PASO 4: Enviar el mensaje a través del servidor SMTP de Mailtrap
                System.out.println(" [CorreoUtil] Paso 4: Enviando mensaje...");
                Transport.send(message);
                System.out.println("[CorreoUtil] ÉXITO - Correo enviado a: " + destinatario);

            } catch (Exception e) {
                // Imprime la causa raíz del fallo (credenciales, red, puerto bloqueado, etc.)
                System.err.println(" [CorreoUtil] ERROR al enviar correo a: " + destinatario);
                System.err.println(" [CorreoUtil] Causa: " + e.getMessage());
                e.printStackTrace(); // Traza completa visible en la consola de GlassFish/Tomcat
            }
        }).start(); // Disparar el hilo en paralelo y liberar el hilo HTTP de inmediato
    }

    /**
     * Generador de plantilla HTML institucional para la recuperación de contraseñas.
     * Sirve para: Producir un código HTML responsivo, moderno y limpio alineado con los colores corporativos de la Defensa Civil Colombiana.
     * Qué hace: Retorna una cadena de texto HTML estructurada con CSS inline que incluye el nombre del usuario y el token de seguridad.
     * Por qué es importante: Mejora considerablemente la presentación visual y la confianza del usuario final en el sistema al recibir su código.
     */
    public static String obtenerPlantillaHTML(String nombreUsuario, String token) {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <title>Recuperación de Contraseña</title>\n" +
                "  <style>\n" +
                "    body {\n" +
                "      font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;\n" +
                "      background-color: #f4f6f9;\n" +
                "      margin: 0;\n" +
                "      padding: 0;\n" +
                "      -webkit-font-smoothing: antialiased;\n" +
                "    }\n" +
                "    .container {\n" +
                "      max-width: 600px;\n" +
                "      margin: 40px auto;\n" +
                "      background: #ffffff;\n" +
                "      border-radius: 12px;\n" +
                "      overflow: hidden;\n" +
                "      box-shadow: 0 4px 15px rgba(0,0,0,0.05);\n" +
                "      border: 1px solid #e1e8ed;\n" +
                "    }\n" +
                "    .header {\n" +
                "      background-color: #003366;\n" +
                "      padding: 30px;\n" +
                "      text-align: center;\n" +
                "      border-bottom: 5px solid #FF6600;\n" +
                "    }\n" +
                "    .header h1 {\n" +
                "      color: #ffffff;\n" +
                "      margin: 0;\n" +
                "      font-size: 22px;\n" +
                "      letter-spacing: 1px;\n" +
                "      text-transform: uppercase;\n" +
                "    }\n" +
                "    .header p {\n" +
                "      color: #FF6600;\n" +
                "      margin: 5px 0 0 0;\n" +
                "      font-size: 12px;\n" +
                "      font-weight: bold;\n" +
                "      letter-spacing: 2px;\n" +
                "    }\n" +
                "    .content {\n" +
                "      padding: 40px 30px;\n" +
                "      color: #333333;\n" +
                "      line-height: 1.6;\n" +
                "    }\n" +
                "    .content h2 {\n" +
                "      font-size: 18px;\n" +
                "      margin-top: 0;\n" +
                "      color: #003366;\n" +
                "    }\n" +
                "    .code-container {\n" +
                "      text-align: center;\n" +
                "      margin: 30px 0;\n" +
                "      background-color: #fff9f5;\n" +
                "      border: 2px dashed #FF6600;\n" +
                "      border-radius: 8px;\n" +
                "      padding: 20px;\n" +
                "    }\n" +
                "    .code-label {\n" +
                "      font-size: 12px;\n" +
                "      text-transform: uppercase;\n" +
                "      letter-spacing: 1px;\n" +
                "      color: #FF6600;\n" +
                "      font-weight: bold;\n" +
                "      margin-bottom: 8px;\n" +
                "    }\n" +
                "    .code-number {\n" +
                "      font-family: 'Courier New', Courier, monospace;\n" +
                "      font-size: 36px;\n" +
                "      font-weight: bold;\n" +
                "      color: #003366;\n" +
                "      letter-spacing: 6px;\n" +
                "      margin: 0;\n" +
                "    }\n" +
                "    .info-box {\n" +
                "      background-color: #f8fafc;\n" +
                "      border-left: 4px solid #003366;\n" +
                "      padding: 15px;\n" +
                "      margin: 25px 0 10px 0;\n" +
                "      font-size: 14px;\n" +
                "      color: #555555;\n" +
                "      border-radius: 0 6px 6px 0;\n" +
                "    }\n" +
                "    .footer {\n" +
                "      background-color: #f8fafc;\n" +
                "      text-align: center;\n" +
                "      padding: 20px;\n" +
                "      font-size: 12px;\n" +
                "      color: #777777;\n" +
                "      border-top: 1px solid #ebebeb;\n" +
                "    }\n" +
                "    .footer p {\n" +
                "      margin: 5px 0;\n" +
                "    }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class=\"container\">\n" +
                "    <div class=\"header\">\n" +
                "      <h1>Defensa Civil Colombiana</h1>\n" +
                "      <p>PLAN DE EMERGENCIA FAMILIAR</p>\n" +
                "    </div>\n" +
                "    <div class=\"content\">\n" +
                "      <h2>Restablecimiento de Contraseña</h2>\n" +
                "      <p>Hola, <strong>" + nombreUsuario + "</strong>:</p>\n" +
                "      <p>Hemos recibido una solicitud para restablecer tu contraseña en el sistema de gestión de planes familiares <strong>DCPlanes</strong>.</p>\n" +
                "      <div class=\"code-container\">\n" +
                "        <div class=\"code-label\">Código de verificación</div>\n" +
                "        <div class=\"code-number\">" + token + "</div>\n" +
                "      </div>\n" +
                "      <div class=\"info-box\">\n" +
                "        <strong>Importante:</strong> Este código es de uso único y tiene una vigencia de <strong>15 minutos</strong>. Después de este tiempo, expirará automáticamente.\n" +
                "      </div>\n" +
                "      <p style=\"font-size: 14px; color: #666666; margin-top: 25px;\">\n" +
                "        Si tú no has solicitado este cambio, por favor ignora este mensaje. Tu cuenta y contraseña siguen estando seguras.\n" +
                "      </p>\n" +
                "    </div>\n" +
                "    <div class=\"footer\">\n" +
                "      <p><strong>Defensa Civil Colombiana</strong></p>\n" +
                "      <p>Sistema de Planes Familiares de Emergencia</p>\n" +
                "      <p style=\"font-size: 10px; color: #aaaaaa; margin-top: 15px;\">Este es un correo electrónico generado de manera automática, por favor no respondas a este mensaje.</p>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "</body>\n" +
                "</html>";
    }
}