package Modelo.Utilidades;

public class GenerarHash {
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
