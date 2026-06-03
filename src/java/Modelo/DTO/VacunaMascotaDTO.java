package Modelo.DTO;

// Qué hace: Data Transfer Object (DTO) que almacena la información de una vacuna aplicada a una mascota (nombre, fecha de aplicación y relación de mascota).
// Por qué existe: Transporta la información de vacunación de manera aislada y limpia entre la base de datos y la capa de vista o servicio.
// Qué problema resuelve: Facilita el mapeo de registros sanitarios de mascotas hacia estructuras JSON legibles por la interfaz.
public class VacunaMascotaDTO {
    private int id;
    private String name; // Mapea a nombre_vacuna
    private String date; // Mapea a fecha_aplicacion (YYYY-MM-DD)
    private int petId; // Mapea a mascota_id

    // Qué hace: Constructor por defecto para inicializar la vacuna de mascota de manera vacía.
    // Por qué existe: Habilita el mapeo automático y estructurado en la capa DAO.
    // Qué problema resuelve: Permite rellenar de forma progresiva las propiedades de la vacuna recibidas desde el cliente o la base de datos.
    public VacunaMascotaDTO() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public int getPetId() { return petId; }
    public void setPetId(int petId) { this.petId = petId; }
}
