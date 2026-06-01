package Modelo.Entidades;

// Clase de Entidad que representa la tabla preguntas_test de la base de datos
public class PreguntaTest {
    // ID autonumérico de la pregunta en la base de datos (clave primaria)
    private int id;
    // Enunciado textual de la pregunta de vulnerabilidad
    private String enunciado;
    // Indica si la pregunta es evaluable para sumar puntos de riesgo
    private boolean esEvaluable;
    // Secuencia de orden de visualización de la pregunta
    private int orden;
    // Estado de la pregunta en la base de datos (activa/inactiva)
    private boolean activo;

    // Constructor vacío por defecto requerido por convenciones JavaBeans
    public PreguntaTest() {}

    // Constructor parametrizado para facilitar la creación de instancias en el DAO
    public PreguntaTest(int id, String enunciado, boolean esEvaluable, int orden, boolean activo) {
        // Asigna el identificador de la pregunta
        this.id = id;
        // Asigna el enunciado de la pregunta
        this.enunciado = enunciado;
        // Asigna el indicador evaluable
        this.esEvaluable = esEvaluable;
        // Asigna el orden de secuencia
        this.orden = orden;
        // Asigna el estado activo
        this.activo = activo;
    }

    // Obtiene el identificador numérico de la pregunta
    public int getId() {
        return id;
    }

    // Establece el identificador de la pregunta
    public void setId(int id) {
        this.id = id;
    }

    // Obtiene el enunciado textual de la pregunta
    public String getEnunciado() {
        return enunciado;
    }

    // Establece el enunciado de la pregunta
    public void setEnunciado(String enunciado) {
        this.enunciado = enunciado;
    }

    // Obtiene el valor booleano indicando si es evaluable
    public boolean isEsEvaluable() {
        return esEvaluable;
    }

    // Establece si la pregunta es evaluable
    public void setEsEvaluable(boolean esEvaluable) {
        this.esEvaluable = esEvaluable;
    }

    // Obtiene el orden de la pregunta
    public int getOrden() {
        return orden;
    }

    // Establece el orden de la pregunta
    public void setOrden(int orden) {
        this.orden = orden;
    }

    // Obtiene el estado de actividad de la pregunta
    public boolean isActivo() {
        return activo;
    }

    // Establece el estado de actividad de la pregunta
    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
