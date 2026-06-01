package Modelo.DTO;

// Clase DTO para encapsular la respuesta a una pregunta del test enviada desde el cliente
public class RespuestaTestDTO {
    // ID de la pregunta asociada en la base de datos (vulnerable_question_id)
    private int vulnerableQuestionId;
    // Valor booleano de la respuesta marcada (true para SÍ, false para NO)
    private boolean answer;

    // Constructor vacío por defecto para permitir deserialización de JSON
    public RespuestaTestDTO() {}

    // Constructor con parámetros para instanciar rápidamente objetos DTO de respuesta
    public RespuestaTestDTO(int vulnerableQuestionId, boolean answer) {
        // Asigna el ID de la pregunta de vulnerabilidad correspondida
        this.vulnerableQuestionId = vulnerableQuestionId;
        // Asigna el valor booleano (SÍ/NO) de la respuesta
        this.answer = answer;
    }

    // Retorna el ID de la pregunta respondida
    public int getVulnerableQuestionId() {
        return vulnerableQuestionId;
    }

    // Establece el ID de la pregunta respondida
    public void setVulnerableQuestionId(int vulnerableQuestionId) {
        this.vulnerableQuestionId = vulnerableQuestionId;
    }

    // Retorna el valor booleano de la respuesta (true = SÍ, false = NO)
    public boolean isAnswer() {
        return answer;
    }

    // Establece el valor booleano de la respuesta
    public void setAnswer(boolean answer) {
        this.answer = answer;
    }
}
