package Modelo.DTO;

// AGREGADO: Clase DTO para Género, resolviendo acoplamiento MVC en CatalogoDAO
public class GeneroDTO {
    private int id;
    private String nombre;
    private int activo;

    public GeneroDTO() {}

    public GeneroDTO(int id, String nombre, int activo) {
        this.id = id;
        this.nombre = nombre;
        this.activo = activo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getActivo() { return activo; }
    public void setActivo(int activo) { this.activo = activo; }
}
