package Modelo.DTO;

// Clase DTO para capturar los datos de actualización del formulario de identificación enviados en el cuerpo del PATCH
public class ActualizarIdentificacionDTO {
    // Apellidos detallados ingresados para la familia
    private String lastNames;
    // Dirección o nomenclatura del domicilio de la familia
    private String address;
    // ID del sector relacional seleccionado
    private int sectorId;
    // Nombre aclaratorio o barrio/comuna del sector (mapea a barrio_comuna_localidad)
    private String sectorName;
    // Teléfono de contacto de la familia
    private String landlinePhone;
    // ID de la calidad de la vivienda relacional
    private int housingQualityId;

    // Sirve para: Almacenar la zona geográfica (Rural/Urbana) en la actualización
    // Qué hace: Captura el valor numérico zone_id enviado por el frontend
    // Por qué es importante: Permite guardar los cambios hechos sobre el tipo de zona del plan
    private int zoneId;

    // Constructor vacío por defecto para permitir la deserialización de peticiones JSON
    public ActualizarIdentificacionDTO() {}

    // Constructor parametrizado para instanciar rápidamente objetos DTO de actualización
    public ActualizarIdentificacionDTO(String lastNames, String address, int sectorId, String sectorName, String landlinePhone, int housingQualityId) {
        // Asigna los apellidos familiares
        this.lastNames = lastNames;
        // Asigna la dirección
        this.address = address;
        // Asigna el identificador del sector
        this.sectorId = sectorId;
        // Asigna el nombre de barrio/sector
        this.sectorName = sectorName;
        // Asigna el teléfono
        this.landlinePhone = landlinePhone;
        // Asigna el identificador de calidad de vivienda
        this.housingQualityId = housingQualityId;
    }

    // Sirve para: Obtener los apellidos familiares
    // Qué hace: Retorna la cadena de apellidos
    // Por qué es importante: Facilita el acceso a los datos nominales para persistencia
    public String getLastNames() {
        return lastNames;
    }

    // Sirve para: Establecer los apellidos familiares
    // Qué hace: Asigna el valor en lastNames
    // Por qué es importante: Inicializa los apellidos a guardar
    public void setLastNames(String lastNames) {
        this.lastNames = lastNames;
    }

    // Sirve para: Obtener la dirección
    // Qué hace: Retorna la dirección en texto
    // Por qué es importante: Provee la nomenclatura física de la vivienda
    public String getAddress() {
        return address;
    }

    // Sirve para: Establecer la dirección de la vivienda
    // Qué hace: Guarda el valor recibido en address
    // Por qué es importante: Actualiza la dirección física a persistir
    public void setAddress(String address) {
        this.address = address;
    }

    // Sirve para: Obtener el identificador del sector
    // Qué hace: Retorna el código numérico de sector
    // Por qué es importante: Permite recuperar la llave foránea de sector
    public int getSectorId() {
        return sectorId;
    }

    // Sirve para: Establecer el identificador de sector
    // Qué hace: Escribe el entero en sectorId
    // Por qué es importante: Registra el sector geográfico seleccionado
    public void setSectorId(int sectorId) {
        this.sectorId = sectorId;
    }

    // Sirve para: Obtener el nombre del barrio o comuna
    // Qué hace: Retorna el barrio ingresado manualmente
    // Por qué es importante: Da precisión de la comunidad o asentamiento
    public String getSectorName() {
        return sectorName;
    }

    // Sirve para: Establecer el nombre del barrio o comuna
    // Qué hace: Guarda el valor recibido en la propiedad sectorName
    // Por qué es importante: Permite persistir la denominación manual del barrio
    public void setSectorName(String sectorName) {
        this.sectorName = sectorName;
    }

    // Sirve para: Obtener el teléfono fijo de contacto
    // Qué hace: Retorna la cadena de caracteres telefónica
    // Por qué es importante: Permite acceder al contacto de la familia
    public String getLandlinePhone() {
        return landlinePhone;
    }

    // Sirve para: Establecer el teléfono fijo
    // Qué hace: Guarda el valor en landlinePhone
    // Por qué es importante: Actualiza el contacto telefónico de la vivienda
    public void setLandlinePhone(String landlinePhone) {
        this.landlinePhone = landlinePhone;
    }

    // Sirve para: Obtener el identificador de calidad de vivienda
    // Qué hace: Retorna el ID relacional de calidad de vivienda
    // Por qué es importante: Sirve para referenciar las condiciones de habitabilidad
    public int getHousingQualityId() {
        return housingQualityId;
    }

    // Sirve para: Establecer el identificador de calidad de vivienda
    // Qué hace: Escribe el entero en la propiedad housingQualityId
    // Por qué es importante: Registra la calidad de la vivienda en el DTO
    public void setHousingQualityId(int housingQualityId) {
        this.housingQualityId = housingQualityId;
    }

    // Sirve para: Obtener el ID de la zona geográfica en el DTO de actualización
    // Qué hace: Retorna el entero zoneId
    // Por qué es importante: Permite consultar el tipo de zona que se quiere actualizar
    public int getZoneId() {
        return zoneId;
    }

    // Sirve para: Establecer el ID de la zona geográfica en el DTO de actualización
    // Qué hace: Asigna el valor del entero zoneId
    // Por qué es importante: Permite guardar el cambio de la zona Rural/Urbana
    public void setZoneId(int zoneId) {
        this.zoneId = zoneId;
    }
}
