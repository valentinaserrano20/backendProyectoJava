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

    // Obtiene los apellidos familiares
    public String getLastNames() {
        return lastNames;
    }

    // Establece los apellidos familiares
    public void setLastNames(String lastNames) {
        this.lastNames = lastNames;
    }

    // Obtiene la dirección
    public String getAddress() {
        return address;
    }

    // Establece la dirección
    public void setAddress(String address) {
        this.address = address;
    }

    // Obtiene el identificador del sector
    public int getSectorId() {
        return sectorId;
    }

    // Establece el identificador del sector
    public void setSectorId(int sectorId) {
        this.sectorId = sectorId;
    }

    // Obtiene el nombre del barrio o comuna
    public String getSectorName() {
        return sectorName;
    }

    // Establece el nombre del barrio o comuna
    public void setSectorName(String sectorName) {
        this.sectorName = sectorName;
    }

    // Obtiene el teléfono de contacto
    public String getLandlinePhone() {
        return landlinePhone;
    }

    // Establece el teléfono de contacto
    public void setLandlinePhone(String landlinePhone) {
        this.landlinePhone = landlinePhone;
    }

    // Obtiene el identificador de calidad de vivienda
    public int getHousingQualityId() {
        return housingQualityId;
    }

    // Establece el identificador de calidad de vivienda
    public void setHousingQualityId(int housingQualityId) {
        this.housingQualityId = housingQualityId;
    }
}
