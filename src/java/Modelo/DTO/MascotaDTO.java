package Modelo.DTO;

// Qué hace: Data Transfer Object (DTO) que almacena de manera temporal y estructurada los datos demográficos y de clasificación de una mascota familiar.
// Por qué existe: Transporta la información de la mascota entre los controladores, la capa de servicios y la de acceso a datos (DAO), desacoplándola de la base de datos.
// Qué problema resuelve: Agrupa múltiples campos individuales relacionados con la mascota en un único objeto, evitando la ineficiencia de pasar múltiples parámetros.
public class MascotaDTO {
    private int id;
    private String name;
    private String breed;
    private String animalGender; // ENUM 'Macho' o 'Hembra'
    private String birthDate; // YYYY-MM-DD
    private int planId;
    private int speciesId;

    // Campos calculados y provenientes de JOINS
    private String speciesName;
    private int animalGenderId; // 1 = Macho, 2 = Hembra
    private String animalGenderName;
    private int age;

    // Qué hace: Constructor por defecto para inicializar el DTO de mascotas vacío.
    // Por qué existe: Facilita la instanciación dinámica en los mapeadores DAO y controladores.
    // Qué problema resuelve: Permite asignar propiedades de manera progresiva mediante métodos setters según los datos recibidos.
    public MascotaDTO() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }

    public String getAnimalGender() { return animalGender; }
    public void setAnimalGender(String animalGender) { this.animalGender = animalGender; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public int getPlanId() { return planId; }
    public void setPlanId(int planId) { this.planId = planId; }

    public int getSpeciesId() { return speciesId; }
    public void setSpeciesId(int speciesId) { this.speciesId = speciesId; }

    public String getSpeciesName() { return speciesName; }
    public void setSpeciesName(String speciesName) { this.speciesName = speciesName; }

    public int getAnimalGenderId() { return animalGenderId; }
    public void setAnimalGenderId(int animalGenderId) { this.animalGenderId = animalGenderId; }

    public String getAnimalGenderName() { return animalGenderName; }
    public void setAnimalGenderName(String animalGenderName) { this.animalGenderName = animalGenderName; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
}
