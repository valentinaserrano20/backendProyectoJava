package Modelo.DTO;

// Clase DTO para transferir los datos detallados del plan familiar al frontend en la fase de identificación
public class IdentificacionPlanDTO {
    // ID único identificador del plan familiar
    private int id;
    // Apellidos de la familia asociados al plan familiar
    private String lastNames;
    // Clasificación del tipo de familia (ej. Vulnerable, No Vulnerable, Por definir)
    private String familyType;

    // Sirve para: Almacenar la zona geográfica (Rural/Urbana) asociada al plan familiar
    // Qué hace: Guarda el identificador numérico de la zona (tipo_zona_id)
    // Por qué es importante: Permite al frontend precargar y validar el tipo de zona de la vivienda
    private int zoneId;

    // Sirve para: Almacenar el identificador de departamento del plan familiar
    // Qué hace: Retorna siempre el valor predeterminado (ID 1 para Santander)
    // Por qué es importante: Mantiene la compatibilidad y consistencia de los datos geográficos en el frontend
    private int departmentId;

    // Sirve para: Almacenar la organización o ciudad asociada al plan familiar
    // Qué hace: Guarda el identificador numérico de la organización (organizacion_id del voluntario creador)
    // Por qué es importante: Vincula el plan a la Defensa Civil de su respectiva ciudad
    private int cityId;

    // Sirve para: Almacenar la dirección de residencia de la familia
    // Qué hace: Guarda la nomenclatura o dirección física
    // Por qué es importante: Permite localizar la vivienda familiar para la respuesta ante emergencias
    private String address;

    // Sirve para: Almacenar el sector de la ciudad o localidad
    // Qué hace: Guarda el identificador numérico del sector de la lista paramétrica
    // Por qué es importante: Clasifica el área geográfica del plan dentro del municipio
    private int sectorId;

    // Sirve para: Almacenar el nombre detallado del barrio, comuna o localidad
    // Qué hace: Guarda una cadena con la descripción manual ingresada por el usuario
    // Por qué es importante: Ofrece precisión exacta del barrio cuando no está predefinido en sectores
    private String sectorName;

    // Sirve para: Almacenar el número de teléfono fijo de contacto familiar
    // Qué hace: Guarda la cadena numérica telefónica si la familia dispone de una
    // Por qué es importante: Facilita canales alternativos de comunicación en emergencias
    private String landlinePhone;

    // Sirve para: Almacenar el identificador de calidad de la vivienda (Propia, Arrendada, etc.)
    // Qué hace: Guarda el ID relacional de la tabla calidad_vivienda
    // Por qué es importante: Aporta datos censo-vulnerabilidad para el análisis socioeconómico
    private int housingQualityId;

    // Constructor vacío por defecto para permitir la deserialización de JSON
    public IdentificacionPlanDTO() {}

    // Constructor parametrizado para facilitar la instanciación de este DTO en el DAO
    public IdentificacionPlanDTO(int id, String lastNames, String familyType) {
        // Asigna el identificador numérico
        this.id = id;
        // Asigna los apellidos de la familia
        this.lastNames = lastNames;
        // Asigna la descripción textual del tipo de familia
        this.familyType = familyType;
    }

    // Sirve para: Obtener el identificador del plan familiar
    // Qué hace: Retorna la variable entera id
    // Por qué es importante: Es la llave primaria usada en todas las consultas del plan
    public int getId() {
        return id;
    }

    // Sirve para: Asignar el identificador del plan familiar
    // Qué hace: Escribe el valor entero id en la propiedad de la clase
    // Por qué es importante: Inicializa la clave de referencia del plan
    public void setId(int id) {
        this.id = id;
    }

    // Sirve para: Obtener los apellidos de la familia
    // Qué hace: Retorna la cadena lastNames
    // Por qué es importante: Identifica nominalmente al grupo familiar
    public String getLastNames() {
        return lastNames;
    }

    // Sirve para: Asignar los apellidos de la familia
    // Qué hace: Escribe la cadena de caracteres en la variable lastNames
    // Por qué es importante: Permite actualizar el nombre con el que se reconoce la familia
    public void setLastNames(String lastNames) {
        this.lastNames = lastNames;
    }

    // Sirve para: Obtener el tipo de familia
    // Qué hace: Retorna la clasificación familiar en cadena
    // Por qué es importante: Es de lectura para determinar la vulnerabilidad inicial
    public String getFamilyType() {
        return familyType;
    }

    // Sirve para: Asignar el tipo de familia
    // Qué hace: Escribe la descripción de tipo de familia
    // Por qué es importante: Refleja el estado calculado de la clasificación familiar
    public void setFamilyType(String familyType) {
        this.familyType = familyType;
    }

    // Sirve para: Obtener el ID de la zona geográfica
    // Qué hace: Retorna el identificador numérico de la zona
    // Por qué es importante: Indica si la familia reside en zona rural o urbana
    public int getZoneId() {
        return zoneId;
    }

    // Sirve para: Establecer el ID de la zona geográfica
    // Qué hace: Guarda el valor entero en la propiedad zoneId
    // Por qué es importante: Permite precargar la zona correcta en los controles del frontend
    public void setZoneId(int zoneId) {
        this.zoneId = zoneId;
    }

    // Sirve para: Obtener el ID del departamento
    // Qué hace: Retorna el código numérico del departamento
    // Por qué es importante: Mapea la división administrativa superior para el frontend
    public int getDepartmentId() {
        return departmentId;
    }

    // Sirve para: Establecer el ID del departamento
    // Qué hace: Asigna el valor del departamento (usualmente fijo a 1)
    // Por qué es importante: Garantiza consistencia en el selector geográfico de departamentos
    public void setDepartmentId(int departmentId) {
        this.departmentId = departmentId;
    }

    // Sirve para: Obtener el ID de la ciudad u organización
    // Qué hace: Retorna la clave numérica asociada a la Defensa Civil local
    // Por qué es importante: Identifica la base de operaciones a la cual pertenece la familia
    public int getCityId() {
        return cityId;
    }

    // Sirve para: Establecer el ID de la ciudad u organización
    // Qué hace: Asigna el identificador numérico cityId
    // Por qué es importante: Permite preseleccionar la sede o ciudad correspondiente de la lista
    public void setCityId(int cityId) {
        this.cityId = cityId;
    }

    // Sirve para: Obtener la dirección física de la vivienda
    // Qué hace: Retorna la cadena con la dirección
    // Por qué es importante: Ofrece la localización básica en formato texto
    public String getAddress() {
        return address;
    }

    // Sirve para: Establecer la dirección física de la vivienda
    // Qué hace: Copia la cadena provista en la propiedad address
    // Por qué es importante: Evita pérdidas al actualizar los datos en el formulario
    public void setAddress(String address) {
        this.address = address;
    }

    // Sirve para: Obtener la clave numérica del sector
    // Qué hace: Devuelve el identificador de sector
    // Por qué es importante: Asocia la vivienda a un sector catalogado (Norte, Sur, Lagos, etc.)
    public int getSectorId() {
        return sectorId;
    }

    // Sirve para: Establecer la clave del sector
    // Qué hace: Escribe el entero en sectorId
    // Por qué es importante: Facilita la vinculación de listas en cascada en la interfaz
    public void setSectorId(int sectorId) {
        this.sectorId = sectorId;
    }

    // Sirve para: Obtener la descripción libre del barrio o comuna
    // Qué hace: Retorna la cadena de texto con el barrio
    // Por qué es importante: Completa la información de dirección cuando el sector es genérico
    public String getSectorName() {
        return sectorName;
    }

    // Sirve para: Establecer la descripción libre del barrio o comuna
    // Qué hace: Asigna el valor en la propiedad sectorName
    // Por qué es importante: Permite guardar y desplegar correctamente el nombre del barrio
    public void setSectorName(String sectorName) {
        this.sectorName = sectorName;
    }

    // Sirve para: Obtener el teléfono de la residencia
    // Qué hace: Retorna la cadena con el número telefónico fijo
    // Por qué es importante: Ofrece un dato de contacto directo
    public String getLandlinePhone() {
        return landlinePhone;
    }

    // Sirve para: Establecer el teléfono fijo
    // Qué hace: Escribe el valor en la propiedad landlinePhone
    // Por qué es importante: Evita que el campo aparezca vacío al recargar la vista
    public void setLandlinePhone(String landlinePhone) {
        this.landlinePhone = landlinePhone;
    }

    // Sirve para: Obtener el ID del régimen de calidad de vivienda
    // Qué hace: Retorna el identificador numérico calidad_vivienda_id
    // Por qué es importante: Describe las condiciones de ocupación de la vivienda (Propia/Arrendada)
    public int getHousingQualityId() {
        return housingQualityId;
    }

    // Sirve para: Establecer el ID de calidad de vivienda
    // Qué hace: Asigna el entero en la propiedad housingQualityId
    // Por qué es importante: Precarga la opción correcta seleccionada por el usuario
    public void setHousingQualityId(int housingQualityId) {
        this.housingQualityId = housingQualityId;
    }

    // Sirve para: Almacenar el ID del estado actual del plan familiar en progreso
    // Qué hace: Guarda el entero que representa el estado (estado_id en la BD)
    // Por qué es importante: Permite al frontend validar si el plan es editable o de solo lectura
    private int statusPlanId;

    // Sirve para: Almacenar la última observación de rechazo o comentario del supervisor
    // Qué hace: Guarda la cadena de texto con las observaciones del seguimiento
    // Por qué es importante: Informa al voluntario por qué fue devuelto el plan familiar para su corrección
    private String comentary;

    // Sirve para: Obtener el ID del estado del plan
    // Qué hace: Retorna el entero statusPlanId
    // Por qué es importante: Determina los permisos de edición basados en el estado del plan
    public int getStatusPlanId() {
        return statusPlanId;
    }

    // Sirve para: Asignar el ID del estado del plan
    // Qué hace: Escribe el valor entero en statusPlanId
    // Por qué es importante: Permite sincronizar el estado del plan desde la base de datos
    public void setStatusPlanId(int statusPlanId) {
        this.statusPlanId = statusPlanId;
    }

    // Sirve para: Obtener el comentario de observaciones
    // Qué hace: Retorna la cadena comentary
    // Por qué es importante: Permite desplegar el texto explicativo de las observaciones
    public String getComentary() {
        return comentary;
    }

    // Sirve para: Asignar el comentario de observaciones
    // Qué hace: Escribe la cadena de texto en comentary
    // Por qué es importante: Almacena la nota descriptiva asociada al rechazo/cambio de estado
    public void setComentary(String comentary) {
        this.comentary = comentary;
    }
}
