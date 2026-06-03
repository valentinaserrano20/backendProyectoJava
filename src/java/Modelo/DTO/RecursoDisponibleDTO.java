package Modelo.DTO;

// Qué hace: Data Transfer Object (DTO) que almacena la información estructurada de un recurso comunitario disponible para la familia.
// Por qué existe: Transporta la información de los recursos comunitarios entre los controladores, la capa de servicios y el DAO, desacoplándola del modelo relacional.
// Qué problema resuelve: Agrupa múltiples campos individuales de un recurso en un único objeto, simplificando la manipulación de datos en el backend.
public class RecursoDisponibleDTO {
    private int id;
    private String placeName; // Nombre del lugar físico (Ubicación en el frontend)
    private int distance; // Distancia en metros hasta el hogar de la familia
    private String phone; // Teléfono de contacto o emergencia
    private String description; // Detalles descriptivos y observaciones (agregado por alteración de BD)
    private int planId; // ID del plan familiar al que se asocia
    private int resourceTypeId; // ID del tipo de recurso del catálogo (tipo_recurso_id)

    // Campos provenientes de JOINS de base de datos
    private String resourceTypeName; // Nombre descriptivo del tipo de recurso (ej: CAI, Hospital)
    private String serviceName; // Nombre del servicio al que pertenece (ej: Salud, Seguridad)

    // Qué hace: Constructor por defecto para inicializar un DTO vacío.
    // Por qué existe: Permite la creación del objeto sin asignación inicial para mapearse secuencialmente por setters.
    // Qué problema resuelve: Facilita la carga diferida de propiedades al leer de la base de datos o recibir del servlet.
    public RecursoDisponibleDTO() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getPlaceName() { return placeName; }
    public void setPlaceName(String placeName) { this.placeName = placeName; }

    public int getDistance() { return distance; }
    public void setDistance(int distance) { this.distance = distance; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getPlanId() { return planId; }
    public void setPlanId(int planId) { this.planId = planId; }

    public int getResourceTypeId() { return resourceTypeId; }
    public void setResourceTypeId(int resourceTypeId) { this.resourceTypeId = resourceTypeId; }

    public String getResourceTypeName() { return resourceTypeName; }
    public void setResourceTypeName(String resourceTypeName) { this.resourceTypeName = resourceTypeName; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
}
