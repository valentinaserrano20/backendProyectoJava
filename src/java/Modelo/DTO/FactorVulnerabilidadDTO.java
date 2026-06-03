package Modelo.DTO;

// Qué hace: Data Transfer Object (DTO) que representa la vulnerabilidad asociada a un factor de riesgo (tipo de vulnerabilidad y grado).
// Por qué existe: Transporta la información relacional de debilidades familiares entre la base de datos y la capa de vista o servicio.
// Qué problema resuelve: Facilita el mapeo relacional de la tabla intermedia y su estructuración en objetos anidados para el frontend.
public class FactorVulnerabilidadDTO {
    private int id;
    private int vulnerabilityId;       // Mapea a vulnerabilidad_id
    private int vulnerabilityGradeId;  // Mapea al ID numérico del grado (1=Muy Alta, 2=Alta, etc.)
    private int riskFactorId;          // Mapea a riesgo_id
    private String vulnerabilityName;   // Nombre de la vulnerabilidad (JOIN)
    private String vulnerabilityGradeName; // Nombre del grado (ENUM texto: Muy Alta, Alta, etc.)

    // Qué hace: Constructor por defecto para inicializar una instancia vacía de FactorVulnerabilidadDTO.
    // Por qué existe: Habilita el mapeo automático y estructurado en la capa DAO.
    // Qué problema resuelve: Permite la creación y el rellenado progresivo de los datos de la vulnerabilidad asociada.
    public FactorVulnerabilidadDTO() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getVulnerabilityId() { return vulnerabilityId; }
    public void setVulnerabilityId(int vulnerabilityId) { this.vulnerabilityId = vulnerabilityId; }

    public int getVulnerabilityGradeId() { return vulnerabilityGradeId; }
    public void setVulnerabilityGradeId(int vulnerabilityGradeId) { this.vulnerabilityGradeId = vulnerabilityGradeId; }

    public int getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(int riskFactorId) { this.riskFactorId = riskFactorId; }

    public String getVulnerabilityName() { return vulnerabilityName; }
    public void setVulnerabilityName(String vulnerabilityName) { this.vulnerabilityName = vulnerabilityName; }

    public String getVulnerabilityGradeName() { return vulnerabilityGradeName; }
    public void setVulnerabilityGradeName(String vulnerabilityGradeName) { this.vulnerabilityGradeName = vulnerabilityGradeName; }
}
