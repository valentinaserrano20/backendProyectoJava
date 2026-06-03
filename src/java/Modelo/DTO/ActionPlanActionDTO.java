package Modelo.DTO;

// Qué hace: Data Transfer Object (DTO) para representar y transportar los datos individuales de una tarea de acción (fase).
// Por qué existe: Transporta la información de micro-acciones (momento, descripción, responsable) entre los controladores y la capa de persistencia (DAO).
// Qué problema resuelve: Facilita el mapeo estructurado del modelo relacional en la base de datos (con datos anidados del integrante) al formato JSON consumido por el frontend.
public class ActionPlanActionDTO {
    private int id;
    private int memberId;
    private String description;
    private int actionTypeId; // 1='antes', 2='durante', 3='despues'
    private int actionPlanId; // Corresponde al plan_id (id de plan familiar)
    private String memberName; // Nombre concatenado del integrante coordinador

    // Datos anidados del integrante para coincidir con la estructura del modal frontend
    private MemberDTO member;

    // Qué hace: Constructor por defecto.
    // Por qué existe: Inicializa el objeto DTO y su miembro anidado en memoria para evitar excepciones NullPointerException.
    // Qué problema resuelve: Previene fallos de desreferencia al llenar el DTO desde la base de datos o solicitudes de red.
    public ActionPlanActionDTO() {
        this.member = new MemberDTO();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { 
        this.memberId = memberId; 
        if (this.member == null) {
            this.member = new MemberDTO();
        }
        this.member.setId(memberId);
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getActionTypeId() { return actionTypeId; }
    public void setActionTypeId(int actionTypeId) { this.actionTypeId = actionTypeId; }

    public int getActionPlanId() { return actionPlanId; }
    public void setActionPlanId(int actionPlanId) { this.actionPlanId = actionPlanId; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public MemberDTO getMember() { return member; }
    public void setMember(MemberDTO member) { this.member = member; }

    // Qué hace: Clase DTO anidada que representa los detalles específicos del miembro responsable.
    // Por qué existe: Mapea la información cruda del integrante requerida visualmente en los diálogos modales.
    // Qué problema resuelve: Estructura la respuesta JSON agregando de forma aislada los nombres y apellidos del miembro responsable.
    public static class MemberDTO {
        private int id;
        private String names;
        private String last_names;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getNames() { return names; }
        public void setNames(String names) { this.names = names; }

        public String getLast_names() { return last_names; }
        public void setLast_names(String last_names) { this.last_names = last_names; }
    }
}
