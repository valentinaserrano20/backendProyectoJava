package Modelo.DTO;

// Qué hace: Data Transfer Object (DTO) que almacena la información estructurada de una imagen del plan familiar (vivienda, entorno o georeferenciación).
// Por qué existe: Transporta la información de las imágenes entre los controladores, la capa de servicios y el DAO, desacoplándola del modelo relacional de la base de datos.
// Qué problema resuelve: Agrupa múltiples campos individuales de una imagen en un único objeto, simplificando la manipulación de datos y la conversión a formato JSON para el frontend.
public class ImagenDTO {
    private int id;
    private String tipoGrafico; // Tipo de gráfico: "vivienda", "entorno" o "georeferenciacion"
    private String path; // Ruta relativa del archivo guardado físicamente en el servidor
    private String description; // Descripción del gráfico (usado principalmente en vivienda)
    private int planId; // ID del plan familiar al que está asociada la imagen

    // Qué hace: Constructor por defecto para inicializar un DTO vacío.
    // Por qué existe: Permite instanciar el objeto sin datos de entrada para luego asignar sus atributos mediante setters.
    // Qué problema resuelve: Facilita la carga de datos campo a campo al leer registros de la base de datos o al parsear peticiones HTTP.
    public ImagenDTO() {}

    // Qué hace: Obtiene el identificador único del registro de la imagen.
    // Por qué existe: Proporciona acceso al campo id de la imagen para búsquedas y operaciones de edición/eliminación.
    // Qué problema resuelve: Permite leer el ID numérico asignado en la base de datos.
    public int getId() { return id; }
    
    // Qué hace: Asigna el identificador único del registro de la imagen.
    // Por qué existe: Permite configurar el id de la imagen al leer desde la base de datos.
    // Qué problema resuelve: Posibilita la actualización de la propiedad id en memoria.
    public void setId(int id) { this.id = id; }

    // Qué hace: Obtiene el tipo de gráfico ("vivienda", "entorno", "georeferenciacion").
    // Por qué existe: Proporciona acceso a la clasificación del gráfico.
    // Qué problema resuelve: Permite identificar la categoría de la imagen para determinar el flujo de negocio correcto.
    public String getTipoGrafico() { return tipoGrafico; }

    // Qué hace: Asigna el tipo de gráfico ("vivienda", "entorno", "georeferenciacion").
    // Por qué existe: Configura la clasificación del gráfico.
    // Qué problema resuelve: Define el tipo de imagen asociado al registro.
    public void setTipoGrafico(String tipoGrafico) { this.tipoGrafico = tipoGrafico; }

    // Qué hace: Obtiene la ruta física relativa de almacenamiento de la imagen.
    // Por qué existe: Expone la ubicación relativa donde el archivo está almacenado para que el frontend pueda construir la URL final.
    // Qué problema resuelve: Permite acceder a la dirección del recurso de forma estandarizada.
    public String getPath() { return path; }

    // Qué hace: Asigna la ruta física relativa de almacenamiento de la imagen.
    // Por qué existe: Configura la ubicación relativa del archivo de imagen.
    // Qué problema resuelve: Guarda la referencia de ubicación física de la foto.
    public void setPath(String path) { this.path = path; }

    // Qué hace: Obtiene la descripción o anotación del gráfico.
    // Por qué existe: Proporciona detalles textuales explicativos sobre lo que muestra la imagen.
    // Qué problema resuelve: Permite acceder a los comentarios descriptivos del voluntario.
    public String getDescription() { return description; }

    // Qué hace: Asigna la descripción o anotación del gráfico.
    // Por qué existe: Guarda la explicación textual redactada por el voluntario.
    // Qué problema resuelve: Actualiza o configura las observaciones y metadatos de texto del croquis.
    public void setDescription(String description) { this.description = description; }

    // Qué hace: Obtiene el identificador del plan familiar asociado.
    // Por qué existe: Proporciona la referencia al plan de emergencia familiar al cual pertenece la imagen.
    // Qué problema resuelve: Permite identificar la relación foránea de propiedad del plan.
    public int getPlanId() { return planId; }

    // Qué hace: Asigna el identificador del plan familiar asociado.
    // Por qué existe: Vincula el registro de la imagen con el plan de emergencia correspondiente en la base de datos.
    // Qué problema resuelve: Establece la llave foránea de relación con el plan familiar.
    public void setPlanId(int planId) { this.planId = planId; }
}
