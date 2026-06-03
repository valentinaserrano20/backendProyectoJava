package Modelo.DTO;

// Qué hace: Data Transfer Object (DTO) que representa un factor de riesgo (amenaza, descripción, ubicación y distancia).
// Por qué existe: Transporta la información de riesgo de manera aislada y limpia entre la base de datos y la capa de vista o servicio.
// Qué problema resuelve: Facilita el mapeo relacional de datos de riesgos y su serialización a formato JSON para el frontend.
public class FactorRiesgoDTO {
    private int id;
    private String description;   // Mapea a descripcion
    private String ubication;     // Mapea a ubicacion
    private int distance;         // Mapea a distancia_metros
    private int familyPlanId;     // Mapea a plan_id
    private int threatTypeId;     // Mapea a amenaza_id
    private String threatTypeName; // Mapea al nombre de la amenaza (JOIN)

    // Qué hace: Constructor por defecto para inicializar una instancia vacía de FactorRiesgoDTO.
    // Por qué existe: Habilita el mapeo automático y estructurado en la capa DAO.
    // Qué problema resuelve: Permite la creación y el rellenado progresivo de los datos del factor de riesgo.
    public FactorRiesgoDTO() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUbication() { return ubication; }
    public void setUbication(String ubication) { this.ubication = ubication; }

    public int getDistance() { return distance; }
    public void setDistance(int distance) { this.distance = distance; }

    public int getFamilyPlanId() { return familyPlanId; }
    public void setFamilyPlanId(int familyPlanId) { this.familyPlanId = familyPlanId; }

    public int getThreatTypeId() { return threatTypeId; }
    public void setThreatTypeId(int threatTypeId) { this.threatTypeId = threatTypeId; }

    public String getThreatTypeName() { return threatTypeName; }
    public void setThreatTypeName(String threatTypeName) { this.threatTypeName = threatTypeName; }
}
