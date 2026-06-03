package Modelo.DTO;

// Qué hace: Data Transfer Object (DTO) que modela y almacena temporalmente los datos demográficos y médicos de un integrante familiar.
// Por qué existe: Permite desacoplar y transportar la información del integrante de manera estructurada entre los controladores, la capa de servicios y la capa de acceso a datos (DAO) sin arrastrar lógica de comportamiento.
// Qué problema resuelve: Evita la transferencia ineficiente de parámetros sueltos en las firmas de los métodos y previene el acoplamiento directo entre la base de datos y la vista.
public class IntegranteDTO {
    private int id;
    private String names;
    private String lastNames;
    private String documentNumber;
    private String birthDate;
    private String eps;
    private String phone;
    private boolean esJefeHogar;
    private int planId;
    private int documentTypeId;
    private int kinshipId;
    private int bloodGroupId;
    private int nationalityId;
    private int genderId;

    // Campos relacionados de joins
    private String documentTypeAcronym;
    private String genderName;
    private String kinshipName;
    private String bloodGroupName;
    private String nationalityName;
    private int statusId;

    // Qué hace: Constructor por defecto sin argumentos para inicializar la clase DTO vacía.
    // Por qué existe: Permite que librerías de serialización/deserialización o la capa DAO instancien el objeto para luego asignarle datos mediante métodos setter.
    // Qué problema resuelve: Facilita la creación flexible del objeto en diferentes contextos del flujo sin exigir todos los campos en un constructor rígido.
    public IntegranteDTO() {}


    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNames() { return names; }
    public void setNames(String names) { this.names = names; }

    public String getLastNames() { return lastNames; }
    public void setLastNames(String lastNames) { this.lastNames = lastNames; }

    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public String getEps() { return eps; }
    public void setEps(String eps) { this.eps = eps; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public boolean isEsJefeHogar() { return esJefeHogar; }
    public void setEsJefeHogar(boolean esJefeHogar) { this.esJefeHogar = esJefeHogar; }

    public int getPlanId() { return planId; }
    public void setPlanId(int planId) { this.planId = planId; }

    public int getDocumentTypeId() { return documentTypeId; }
    public void setDocumentTypeId(int documentTypeId) { this.documentTypeId = documentTypeId; }

    public int getKinshipId() { return kinshipId; }
    public void setKinshipId(int kinshipId) { this.kinshipId = kinshipId; }

    public int getBloodGroupId() { return bloodGroupId; }
    public void setBloodGroupId(int bloodGroupId) { this.bloodGroupId = bloodGroupId; }

    public int getNationalityId() { return nationalityId; }
    public void setNationalityId(int nationalityId) { this.nationalityId = nationalityId; }

    public int getGenderId() { return genderId; }
    public void setGenderId(int genderId) { this.genderId = genderId; }

    public String getDocumentTypeAcronym() { return documentTypeAcronym; }
    public void setDocumentTypeAcronym(String documentTypeAcronym) { this.documentTypeAcronym = documentTypeAcronym; }

    public String getGenderName() { return genderName; }
    public void setGenderName(String genderName) { this.genderName = genderName; }

    public String getKinshipName() { return kinshipName; }
    public void setKinshipName(String kinshipName) { this.kinshipName = kinshipName; }

    public String getBloodGroupName() { return bloodGroupName; }
    public void setBloodGroupName(String bloodGroupName) { this.bloodGroupName = bloodGroupName; }

    public String getNationalityName() { return nationalityName; }
    public void setNationalityName(String nationalityName) { this.nationalityName = nationalityName; }

    public int getStatusId() { return statusId; }
    public void setStatusId(int statusId) { this.statusId = statusId; }
}
