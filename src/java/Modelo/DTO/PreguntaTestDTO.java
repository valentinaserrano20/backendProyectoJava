package Modelo.DTO;

// Clase DTO para encapsular los datos de una pregunta del test de vulnerabilidad y enviarlos al frontend
public class PreguntaTestDTO {
    // ID único de la pregunta en la base de datos
    private int id;
    // Enunciado textual de la pregunta (corresponde a la columna enunciado)
    private String description;
    // Indica si la pregunta es de precaución/no evaluable para puntos de riesgo (mapea a !es_evaluable)
    private boolean questionCaution;
    // Estado de la pregunta en la plataforma (activo o inactivo)
    private boolean isActive;
    // Posición u orden de visualización de la pregunta
    private int orden;

    // Constructor vacío por defecto para permitir deserialización automática
    public PreguntaTestDTO() {}

    // Constructor con parámetros para instanciar rápidamente objetos DTO desde el DAO
    public PreguntaTestDTO(int id, String description, boolean questionCaution, boolean isActive, int orden) {
        // Asigna el identificador numérico de la pregunta
        this.id = id;
        // Asigna el texto de la pregunta (description)
        this.description = description;
        // Asigna el indicador de precaución
        this.questionCaution = questionCaution;
        // Asigna el estado activo
        this.isActive = isActive;
        // Asigna el orden establecido
        this.orden = orden;
    }

    // Retorna el ID único de la pregunta
    public int getId() {
        return id;
    }

    // Establece el ID único de la pregunta
    public void setId(int id) {
        this.id = id;
    }

    // Retorna la descripción textual de la pregunta
    public String getDescription() {
        return description;
    }

    // Establece la descripción de la pregunta
    public void setDescription(String description) {
        this.description = description;
    }

    // Retorna true si la pregunta es de precaución (no evaluable para vulnerabilidad)
    public boolean isQuestionCaution() {
        return questionCaution;
    }

    // Establece si la pregunta es de precaución
    public void setQuestionCaution(boolean questionCaution) {
        this.questionCaution = questionCaution;
    }

    // Retorna true si la pregunta está activa en la base de datos
    public boolean isIsActive() {
        return isActive;
    }

    // Establece el estado de actividad de la pregunta
    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    // Retorna la posición de orden de la pregunta
    public int getOrden() {
        return orden;
    }

    // Establece el orden de visualización de la pregunta
    public void setOrden(int orden) {
        this.orden = orden;
    }
}
