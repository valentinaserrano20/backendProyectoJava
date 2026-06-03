package Modelo.DTO;

// Qué hace: Data Transfer Object (DTO) que representa una condición o afección médica de un integrante familiar (enfermedad, alergia o discapacidad) y su dosis de medicamento asociada.
// Por qué existe: Transporta la información de salud agregada del integrante de forma limpia entre el servlet, el servicio y la base de datos.
// Qué problema resuelve: Facilita el mapeo relacional de datos médicos que involucran más de una tabla (afecciones y medicamentos) en un objeto consolidado de transporte.
public class AfeccionDTO {
    private int id;
    private int memberId;
    private int conditionTypeId;
    private String conditionTypeName;
    private String name;
    private String dose;

    // Qué hace: Constructor por defecto para inicializar una instancia vacía del DTO de afecciones.
    // Por qué existe: Habilita el mapeo y la instanciación dinámica en los controladores y DAOs.
    // Qué problema resuelve: Permite la creación y el rellenado progresivo del DTO médico según el flujo de la aplicación.
    public AfeccionDTO() {}


    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }

    public int getConditionTypeId() { return conditionTypeId; }
    public void setConditionTypeId(int conditionTypeId) { this.conditionTypeId = conditionTypeId; }

    public String getConditionTypeName() { return conditionTypeName; }
    public void setConditionTypeName(String conditionTypeName) { this.conditionTypeName = conditionTypeName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDose() { return dose; }
    public void setDose(String dose) { this.dose = dose; }
}
