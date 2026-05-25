package Modelo.Entidades;

public class Usuario {
    private int id;
    private String nombre;
    private String apellido;
    private String email;
    private String contrasena;
    private int estadoId;
    private String estado; // Para almacenar el nombre del estado (e.g. "Activo", "Pendiente") tras un JOIN
    private int rolId;
    private Integer organizacionId;
    private int generoId;

    public Usuario() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }

    public int getEstadoId() { return estadoId; }
    public void setEstadoId(int estadoId) { this.estadoId = estadoId; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public int getRolId() { return rolId; }
    public void setRolId(int rolId) { this.rolId = rolId; }

    public Integer getOrganizacionId() { return organizacionId; }
    public void setOrganizacionId(Integer organizacionId) { this.organizacionId = organizacionId; }

    public int getGeneroId() { return generoId; }
    public void setGeneroId(int generoId) { this.generoId = generoId; }
}
