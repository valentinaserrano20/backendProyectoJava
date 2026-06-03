package Modelo.DTO;

// Qué hace: Data Transfer Object (DTO) que representa la información de configuración general del Plan de Acción familiar.
// Por qué existe: Transporta la información de la cabecera del plan de acción (coordinador y riesgo asociado) entre los controladores y el DAO.
// Qué problema resuelve: Agrupa el ID del plan de emergencia, el coordinador seleccionado y el factor de riesgo en una estructura única simplificando el pasaje de parámetros.
public class ActionPlanDTO {
    private int id;
    private int familyPlanId; // ID del plan familiar (plan_id en la base de datos)
    private int memberId; // ID del integrante coordinador general (coordinador_id en la base de datos)
    private int riskFactorId; // ID del factor de riesgo que atiende el plan (riesgo_id en la base de datos)

    // Qué hace: Constructor por defecto para inicializar un objeto vacío.
    // Por qué existe: Habilita la creación del DTO en memoria para ser rellenado de manera diferida mediante setters.
    // Qué problema resuelve: Facilita el mapeo estructurado desde respuestas de base de datos o peticiones JSON.
    public ActionPlanDTO() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getFamilyPlanId() { return familyPlanId; }
    public void setFamilyPlanId(int familyPlanId) { this.familyPlanId = familyPlanId; }

    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }

    public int getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(int riskFactorId) { this.riskFactorId = riskFactorId; }
}
