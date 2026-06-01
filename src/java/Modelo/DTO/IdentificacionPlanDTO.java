package Modelo.DTO;

// Clase DTO para transferir los datos básicos del plan familiar al frontend en la fase de identificación
public class IdentificacionPlanDTO {
    // ID único identificador del plan familiar
    private int id;
    // Apellidos de la familia asociados al plan familiar
    private String lastNames;
    // Clasificación del tipo de familia (ej. Vulnerable, No Vulnerable, Por definir)
    private String familyType;

    // Constructor vacío por defecto para permitir la deserialización de JSON
    public IdentificacionPlanDTO() {}

    // Constructor parametrizado para facilitar la instanciación de este DTO en el DAO
    public IdentificacionPlanDTO(int id, String lastNames, String familyType) {
        // Asigna el identificador numérico
        this.id = id;
        // Asigna los apellidos de la familia
        this.lastNames = lastNames;
        // Asigna la descripción textual del tipo de familia
        this.familyType = familyType;
    }

    // Obtiene el identificador del plan familiar
    public int getId() {
        return id;
    }

    // Establece el identificador del plan familiar
    public void setId(int id) {
        this.id = id;
    }

    // Obtiene los apellidos de la familia
    public String getLastNames() {
        return lastNames;
    }

    // Establece los apellidos de la familia
    public void setLastNames(String lastNames) {
        this.lastNames = lastNames;
    }

    // Obtiene el tipo de familia
    public String getFamilyType() {
        return familyType;
    }

    // Establece el tipo de familia
    public void setFamilyType(String familyType) {
        this.familyType = familyType;
    }
}
