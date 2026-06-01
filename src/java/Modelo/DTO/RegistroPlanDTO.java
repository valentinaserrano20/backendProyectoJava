package Modelo.DTO;

public class RegistroPlanDTO {
    private String lastNames;
    private int zoneId;
    private int organizacionId; // Mapeado desde el selector "ciudades"
    private int userId;

    public RegistroPlanDTO() {}

    public String getLastNames() { return lastNames; }
    public void setLastNames(String lastNames) { this.lastNames = lastNames; }

    public int getZoneId() { return zoneId; }
    public void setZoneId(int zoneId) { this.zoneId = zoneId; }

    public int getOrganizacionId() { return organizacionId; }
    public void setOrganizacionId(int organizacionId) { this.organizacionId = organizacionId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
}