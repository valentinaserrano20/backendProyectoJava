package Modelo.Utilidades;

/**
 * Qué hace: Clase utilitaria ejecutable que genera múltiples hashes criptográficos BCrypt a partir de una contraseña en texto plano.
 * Por qué existe: Permite generar cadenas de hash de prueba listas para ser insertadas mediante scripts SQL en la tabla de usuarios, asegurando la consistencia en el proceso de siembra de datos.
 * Qué pasaría si no estuviera: El administrador tendría que registrar usuarios a través del navegador o escribir código Java temporal dentro del servidor web solo para generar un hash seguro.
 */
public class GenerarHash {
    
    /**
     * Qué hace: Genera tres hashes distintos usando la misma clave y comprueba su validez a través de la consola estándar.
     * Qué significa: Invoca secuencialmente BCrypt.hashpw() y BCrypt.checkpw() para demostrar la unicidad del hash por cada sal generada.
     * Para qué se usa: Herramienta rápida de testing local en la terminal para validar la implementación de BCrypt del proyecto.
     * Por qué es importante: Corrobora que la sal aleatoria funcione correctamente y demuestra que una misma contraseña no genera hashes idénticos.
     * 
     * @param args Argumentos de la línea de comandos (no utilizados).
     */
    public static void main(String[] args) {
        String password = "Password.123";
        
        // Generar 3 hashes diferentes para simular diferentes usuarios
        String hash1 = BCrypt.hashpw(password, BCrypt.gensalt(10));
        String hash2 = BCrypt.hashpw(password, BCrypt.gensalt(10));
        String hash3 = BCrypt.hashpw(password, BCrypt.gensalt(10));
        
        System.out.println("Hash 1: " + hash1);
        System.out.println("Hash 2: " + hash2);
        System.out.println("Hash 3: " + hash3);
        
        // Verificar que funcionan
        System.out.println("\nVerificación:");
        System.out.println("Hash 1 válido: " + BCrypt.checkpw(password, hash1));
        System.out.println("Hash 2 válido: " + BCrypt.checkpw(password, hash2));
        System.out.println("Hash 3 válido: " + BCrypt.checkpw(password, hash3));
    }
}
