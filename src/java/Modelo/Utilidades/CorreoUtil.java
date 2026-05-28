package Modelo.Utilidades;

// =========================================================
// IMPORTACIONES OBLIGATORIAS
// =========================================================
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

/**
 * Clase: CorreoUtil
 * Capa: Modelo.Utilidades
 * Responsabilidad: Gestionar el canal SMTP seguro con Mailtrap de forma asíncrona.
 */
public class CorreoUtil {

    // Configuración estricta de Mailtrap (Debes reemplazarlos con tus valores de Mailtrap.io)
    private static final String HOST = "sandbox.smtp.mailtrap.io";
    private static final String PORT = "587";
    private static final String USER = "TU_USER_REAL_DE_MAILTRAP"; // <-- Reemplaza aquí
    private static final String PASS = "TU_PASS_REAL_DE_MAILTRAP"; // <-- Reemplaza aquí
    private static final String FROM = "no-reply@defensacivil.co";

    /**
     * Envía un correo electrónico en un hilo de ejecución secundario para no bloquear al usuario.
     */
    public static void enviarCorreoAsincrono(String destinatario, String asunto, String cuerpoTexto) {
        new Thread(() -> {
            // 1. Establecer las propiedades de conexión SMTP
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true"); // TLS Obligatorio en Mailtrap port 587
            props.put("mail.smtp.host", HOST);
            props.put("mail.smtp.port", PORT);

            // 2. Crear la sesión autenticada con las credenciales básicas
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(USER, PASS);
                }
            });

            try {
                // 3. Construir la estructura del mensaje institucional
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(FROM));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
                message.setSubject(asunto);
                message.setText(cuerpoTexto);

                // 4. Disparar el envío a través de la red
                Transport.send(message);
                System.out.println("📧 [Mailtrap] Código enviado exitosamente a: " + destinatario);

            } catch (MessagingException e) {
                System.err.println("❌ [Error Correo] Error de infraestructura SMTP en Mailtrap: " + e.getMessage());
            }
        }).start(); // Inicia el hilo en paralelo
    }
}