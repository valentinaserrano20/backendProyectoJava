package Modelo.DTO;

public class UbicacionDTO {
    private int id;
    private String nombre;

    public UbicacionDTO() {}

    public UbicacionDTO(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}