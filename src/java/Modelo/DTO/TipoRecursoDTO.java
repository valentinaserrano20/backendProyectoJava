package Modelo.DTO;

// Qué hace: Data Transfer Object (DTO) que representa un tipo de recurso comunitario de emergencia (CAI, Hospital, etc.).
// Por qué existe: Transporta la información de clasificación de recursos y su respectivo servicio de emergencia padre desde el DAO.
// Qué problema resuelve: Agrupa el tipo de recurso y el nombre de su servicio asociado en un único objeto de catálogo desacoplado.
public class TipoRecursoDTO {
    private int id;
    private String nombre;
    private String servicio; // Nombre del servicio padre (ej: Salud, Seguridad)
    private int activo;

    public TipoRecursoDTO() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getServicio() { return servicio; }
    public void setServicio(String servicio) { this.servicio = servicio; }

    public int getActivo() { return activo; }
    public void setActivo(int activo) { this.activo = activo; }
}
