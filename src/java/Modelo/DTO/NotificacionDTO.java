package Modelo.DTO;

/**
 * DTO para Notificaciones
 * Representa una notificación en el sistema
 */
public class NotificacionDTO {
    private int id;
    private int usuarioId;
    private String titulo;
    private String mensaje;
    private String tipo; // 'plan_estado', 'nuevo_usuario', 'nuevo_plan'
    private boolean leida;
    private String fechaCreacion;
    private String enlace; // URL para redirección
    private int entidadId; // ID de la entidad relacionada (plan_id, user_id, etc.)
    
    // Constructor vacío
    public NotificacionDTO() {}
    
    // Constructor completo
    public NotificacionDTO(int id, int usuarioId, String titulo, String mensaje, 
                          String tipo, boolean leida, String fechaCreacion, 
                          String enlace, int entidadId) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.tipo = tipo;
        this.leida = leida;
        this.fechaCreacion = fechaCreacion;
        this.enlace = enlace;
        this.entidadId = entidadId;
    }
    
    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }
    
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    
    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }
    
    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    
    public String getEnlace() { return enlace; }
    public void setEnlace(String enlace) { this.enlace = enlace; }
    
    public int getEntidadId() { return entidadId; }
    public void setEntidadId(int entidadId) { this.entidadId = entidadId; }
}
