package Modelo.DTO;

/**
 * DTO (Data Transfer Object) para representar un género de mascota.
 * Contiene el identificador único y el nombre descriptivo del género.
 */
public class GeneroMascotaDTO {
    /** Identificador único del género de mascota */
    private int id;
    /** Nombre del género (por ejemplo, "Macho" o "Hembra") */
    private String nombre;

    /** Constructor vacío requerido para la reflexión */
    public GeneroMascotaDTO() {}

    /** Constructor con parámetros para mayor comodidad */
    public GeneroMascotaDTO(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    /** Obtiene el identificador del género */
    public int getId() {
        return id;
    }

    /** Asigna el identificador del género */
    public void setId(int id) {
        this.id = id;
    }

    /** Obtiene el nombre del género */
    public String getNombre() {
        return nombre;
    }

    /** Asigna el nombre del género */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
