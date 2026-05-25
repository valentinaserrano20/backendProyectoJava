package Modelo.DTO;

// AGREGADO: Clase DTO para Tipo de Documento, resolviendo acoplamiento MVC en CatalogoDAO
public class TipoDocumentoDTO {
    private int id;
    private String sigla;
    private String nombre;
    private int activo;

    public TipoDocumentoDTO() {}

    public TipoDocumentoDTO(int id, String sigla, String nombre, int activo) {
        this.id = id;
        this.sigla = sigla;
        this.nombre = nombre;
        this.activo = activo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getSigla() { return sigla; }
    public void setSigla(String sigla) { this.sigla = sigla; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getActivo() { return activo; }
    public void setActivo(int activo) { this.activo = activo; }
}
