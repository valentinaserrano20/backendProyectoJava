package Modelo.DTO;

// Qué hace: Data Transfer Object (DTO) que representa una acción de reducción de riesgo (tarea, fecha límite, responsable).
// Por qué existe: Transporta la información de acciones preventivas entre la base de datos y la capa de vista o servicio.
// Qué problema resuelve: Facilita el mapeo relacional de acciones asociadas a un factor de riesgo y su serialización a formato JSON.
public class AccionReduccionDTO {
    private int id;
    private String action;        // Mapea a descripcion_tarea
    private String endDate;       // Mapea a fecha_termino (YYYY-MM-DD)
    private int riskFactorId;     // Mapea a riesgo_id
    private int memberId;         // Mapea a responsable_id
    private String memberName;     // Nombre del integrante responsable (JOIN)
    private String memberLastNames; // Apellidos del integrante responsable (JOIN)
    private String createdAt;     // Fallback para mapear la fecha de creación en la UI

    // Qué hace: Constructor por defecto para inicializar una instancia vacía de AccionReduccionDTO.
    // Por qué existe: Habilita el mapeo automático y estructurado en la capa DAO.
    // Qué problema resuelve: Permite la creación y el rellenado progresivo de los datos de la acción.
    public AccionReduccionDTO() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public int getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(int riskFactorId) { this.riskFactorId = riskFactorId; }

    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public String getMemberLastNames() { return memberLastNames; }
    public void setMemberLastNames(String memberLastNames) { this.memberLastNames = memberLastNames; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
