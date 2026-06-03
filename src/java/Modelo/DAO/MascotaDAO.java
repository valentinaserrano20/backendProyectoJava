package Modelo.DAO;

import Modelo.Config.Conexion;
import Modelo.DTO.MascotaDTO;
import Modelo.DTO.VacunaMascotaDTO;
import java.sql.*;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

// Qué hace: Data Access Object (DAO) que encapsula el acceso físico y la persistencia de datos para las entidades de Mascotas y Vacunas de Mascotas.
// Por qué existe: Concentra la ejecución de sentencias JDBC y consultas SQL parametrizadas directas, independizando la base de datos de la lógica empresarial.
// Qué problema resuelve: Separa la gestión directa de tablas MySQL de las capas superiores del backend, evitando SQL injection y facilitando transacciones seguras.
public class MascotaDAO {

    // Qué hace: Obtiene la cantidad total de mascotas registradas asociadas a un plan familiar específico.
    // Por qué existe: Es requerido para los cálculos y lógica de paginación infinita en las vistas del voluntario.
    // Qué problema resuelve: Permite contar rápidamente las mascotas de la base de datos sin necesidad de transferir todas las filas en memoria.
    public int obtenerTotalMascotas(int planId) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM mascotas WHERE plan_id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        }
        return 0;
    }

    // Qué hace: Consulta una lista paginada de mascotas asociadas a un plan familiar, uniendo con su respectiva especie.
    // Por qué existe: Alimenta el renderizado de la cuadrícula de mascotas de la familia de forma dosificada en el cliente.
    // Qué problema resuelve: Evita la sobrecarga de memoria del servidor al recuperar grupos delimitados de registros utilizando LIMIT y OFFSET.
    public List<MascotaDTO> listarMascotas(int planId, int limit, int offset) throws SQLException {
        String sql = "SELECT m.id, m.nombre, m.raza, m.genero_animal, m.fecha_nacimiento, m.plan_id, m.especie_id, e.nombre AS especie_nombre "
                   + "FROM mascotas m "
                   + "LEFT JOIN especies_mascota e ON m.especie_id = e.id "
                   + "WHERE m.plan_id = ? "
                   + "LIMIT ? OFFSET ?";
        
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            
            List<MascotaDTO> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MascotaDTO dto = new MascotaDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setName(rs.getString("nombre"));
                    dto.setBreed(rs.getString("raza") != null ? rs.getString("raza") : "Sin raza");
                    
                    String gen = rs.getString("genero_animal");
                    dto.setAnimalGender(gen);
                    dto.setAnimalGenderName(gen != null ? gen : "No especificado");
                    dto.setAnimalGenderId("Macho".equals(gen) ? 1 : 2);
                    
                    Date birth = rs.getDate("fecha_nacimiento");
                    if (birth != null) {
                        dto.setBirthDate(birth.toString());
                        dto.setAge(Period.between(birth.toLocalDate(), LocalDate.now()).getYears());
                    } else {
                        dto.setBirthDate("");
                        dto.setAge(0);
                    }
                    
                    dto.setPlanId(rs.getInt("plan_id"));
                    dto.setSpeciesId(rs.getInt("especie_id"));
                    dto.setSpeciesName(rs.getString("especie_nombre") != null ? rs.getString("especie_nombre") : "Otro");
                    lista.add(dto);
                }
            }
            return lista;
        }
    }

    // Qué hace: Obtiene la información detallada de una mascota singular y su relación con especies_mascota.
    // Por qué existe: Permite precargar la información de la mascota en el formulario de edición y ventanas de detalles.
    // Qué problema resuelve: Facilita la recuperación atómica y limpia de los atributos de un animal individual por su ID único.
    public MascotaDTO obtenerMascota(int id) throws SQLException {
        String sql = "SELECT m.id, m.nombre, m.raza, m.genero_animal, m.fecha_nacimiento, m.plan_id, m.especie_id, e.nombre AS especie_nombre "
                   + "FROM mascotas m "
                   + "LEFT JOIN especies_mascota e ON m.especie_id = e.id "
                   + "WHERE m.id = ?";
        
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    MascotaDTO dto = new MascotaDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setName(rs.getString("nombre"));
                    dto.setBreed(rs.getString("raza"));
                    
                    String gen = rs.getString("genero_animal");
                    dto.setAnimalGender(gen);
                    dto.setAnimalGenderName(gen);
                    dto.setAnimalGenderId("Macho".equals(gen) ? 1 : 2);
                    
                    Date birth = rs.getDate("fecha_nacimiento");
                    if (birth != null) {
                        dto.setBirthDate(birth.toString());
                        dto.setAge(Period.between(birth.toLocalDate(), LocalDate.now()).getYears());
                    } else {
                        dto.setBirthDate("");
                        dto.setAge(0);
                    }
                    
                    dto.setPlanId(rs.getInt("plan_id"));
                    dto.setSpeciesId(rs.getInt("especie_id"));
                    dto.setSpeciesName(rs.getString("especie_nombre"));
                    return dto;
                }
            }
        }
        return null;
    }

    // Qué hace: Inserta una nueva mascota en la base de datos de MySQL y retorna el ID autogenerado asignado.
    // Por qué existe: Soporta la creación física de mascotas asociadas a un núcleo familiar voluntario evaluado.
    // Qué problema resuelve: Garantiza el almacenamiento consistente de los tipos de datos (como fecha y llaves foráneas) controlando nulos en columnas opcionales.
    public int crearMascota(MascotaDTO dto) throws SQLException {
        String sql = "INSERT INTO mascotas (nombre, raza, genero_animal, fecha_nacimiento, plan_id, especie_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setString(1, dto.getName());
            ps.setString(2, dto.getBreed() != null && !dto.getBreed().isEmpty() ? dto.getBreed() : null);
            
            String genEnum = dto.getAnimalGenderId() == 1 ? "Macho" : "Hembra";
            ps.setString(3, genEnum);
            
            if (dto.getBirthDate() != null && !dto.getBirthDate().isEmpty()) {
                ps.setDate(4, Date.valueOf(dto.getBirthDate()));
            } else {
                ps.setNull(4, Types.DATE);
            }
            
            ps.setInt(5, dto.getPlanId());
            
            if (dto.getSpeciesId() > 0) {
                ps.setInt(6, dto.getSpeciesId());
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            
            ps.executeUpdate();
            
            try (ResultSet rsKeys = ps.getGeneratedKeys()) {
                if (rsKeys.next()) {
                    return rsKeys.getInt(1);
                }
            }
        }
        throw new SQLException("Error al recuperar el ID autogenerado de la mascota registrada.");
    }

    // Qué hace: Modifica los atributos base de una mascota existente en la base de datos.
    // Por qué existe: Posibilita que el voluntario guarde correcciones del nombre, raza, género o edad de la mascota.
    // Qué problema resuelve: Actualiza los campos específicos de la mascota sin alterar su relación estructurada con el plan familiar.
    public void actualizarMascota(int id, MascotaDTO dto) throws SQLException {
        String sql = "UPDATE mascotas SET nombre = ?, raza = ?, genero_animal = ?, fecha_nacimiento = ?, especie_id = ? WHERE id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, dto.getName());
            ps.setString(2, dto.getBreed() != null && !dto.getBreed().isEmpty() ? dto.getBreed() : null);
            
            String genEnum = dto.getAnimalGenderId() == 1 ? "Macho" : "Hembra";
            ps.setString(3, genEnum);
            
            if (dto.getBirthDate() != null && !dto.getBirthDate().isEmpty()) {
                ps.setDate(4, Date.valueOf(dto.getBirthDate()));
            } else {
                ps.setNull(4, Types.DATE);
            }
            
            if (dto.getSpeciesId() > 0) {
                ps.setInt(5, dto.getSpeciesId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            
            ps.setInt(6, id);
            ps.executeUpdate();
        }
    }

    // Qué hace: Elimina una mascota de forma transaccional, borrando primero todas sus vacunas asociadas.
    // Por qué existe: Evita violaciones de restricciones de claves foráneas de MySQL durante el borrado físico de la mascota.
    // Qué problema resuelve: Garantiza la atomicidad y la limpieza del historial sanitario evitando registros huérfanos en la base de datos.
    public void eliminarMascota(int id) throws SQLException {
        Connection con = null;
        try {
            con = Conexion.obtener();
            con.setAutoCommit(false);
            
            // 1. Eliminar vacunas dependientes de esta mascota
            String sqlDelVacunas = "DELETE FROM vacunas WHERE mascota_id = ?";
            try (PreparedStatement psV = con.prepareStatement(sqlDelVacunas)) {
                psV.setInt(1, id);
                psV.executeUpdate();
            }
            
            // 2. Eliminar la mascota
            String sqlDelMascota = "DELETE FROM mascotas WHERE id = ?";
            try (PreparedStatement psM = con.prepareStatement(sqlDelMascota)) {
                psM.setInt(1, id);
                psM.executeUpdate();
            }
            
            con.commit();
        } catch (SQLException e) {
            if (con != null) {
                con.rollback();
            }
            throw e;
        } finally {
            if (con != null) {
                con.close();
            }
        }
    }

    // =========================================================================
    // SUB-MÓDULO DE VACUNAS
    // =========================================================================

    // Qué hace: Obtiene la lista completa de vacunas registradas para una mascota en particular.
    // Por qué existe: Permite listar el historial de vacunas en el perfil sanitario de la mascota de la UI.
    // Qué problema resuelve: Recupera del backend de forma consolidada todos los registros de inmunización del animal.
    public List<VacunaMascotaDTO> listarVacunasMascota(int mascotaId) throws SQLException {
        String sql = "SELECT id, nombre_vacuna, fecha_aplicacion, mascota_id FROM vacunas WHERE mascota_id = ? ORDER BY fecha_aplicacion DESC";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, mascotaId);
            
            List<VacunaMascotaDTO> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    VacunaMascotaDTO dto = new VacunaMascotaDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setName(rs.getString("nombre_vacuna"));
                    
                    Date date = rs.getDate("fecha_aplicacion");
                    dto.setDate(date != null ? date.toString() : "");
                    
                    dto.setPetId(rs.getInt("mascota_id"));
                    lista.add(dto);
                }
            }
            return lista;
        }
    }

    // Qué hace: Obtiene los detalles de una vacuna singular registrada por su ID.
    // Por qué existe: Se utiliza para alimentar el modal flotante de edición de vacuna específica.
    // Qué problema resuelve: Recupera de forma limpia y exacta la información e historial de una vacuna particular.
    public VacunaMascotaDTO obtenerVacuna(int id) throws SQLException {
        String sql = "SELECT id, nombre_vacuna, fecha_aplicacion, mascota_id FROM vacunas WHERE id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    VacunaMascotaDTO dto = new VacunaMascotaDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setName(rs.getString("nombre_vacuna"));
                    
                    Date date = rs.getDate("fecha_aplicacion");
                    dto.setDate(date != null ? date.toString() : "");
                    
                    dto.setPetId(rs.getInt("mascota_id"));
                    return dto;
                }
            }
        }
        return null;
    }

    // Qué hace: Inserta un registro de vacuna en la base de datos y retorna el ID autogenerado.
    // Por qué existe: Habilita el registro de una nueva inmunización al animal dentro del flujo del voluntario.
    // Qué problema resuelve: Asegura la correcta escritura e integridad de la fecha y de la clave foránea a la mascota.
    public int crearVacuna(VacunaMascotaDTO dto) throws SQLException {
        String sql = "INSERT INTO vacunas (nombre_vacuna, fecha_aplicacion, mascota_id) VALUES (?, ?, ?)";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setString(1, dto.getName());
            ps.setDate(2, Date.valueOf(dto.getDate()));
            ps.setInt(3, dto.getPetId());
            
            ps.executeUpdate();
            try (ResultSet rsKeys = ps.getGeneratedKeys()) {
                if (rsKeys.next()) {
                    return rsKeys.getInt(1);
                }
            }
        }
        throw new SQLException("Error al recuperar el ID autogenerado de la vacuna registrada.");
    }

    // Qué hace: Actualiza el nombre o la fecha de aplicación de una vacuna específica.
    // Por qué existe: Permite al voluntario editar la dosis o corregir la fecha de inmunización del animal.
    // Qué problema resuelve: Realiza modificaciones sobre el registro particular de vacunas sin afectar las relaciones del animal.
    public void actualizarVacuna(int id, VacunaMascotaDTO dto) throws SQLException {
        String sql = "UPDATE vacunas SET nombre_vacuna = ?, fecha_aplicacion = ? WHERE id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, dto.getName());
            ps.setDate(2, Date.valueOf(dto.getDate()));
            ps.setInt(3, id);
            
            ps.executeUpdate();
        }
    }

    // Qué hace: Elimina una vacuna específica de la base de datos por su ID único.
    // Por qué existe: Permite dar de baja o quitar vacunas registradas incorrectamente.
    // Qué problema resuelve: Ejecuta la remoción directa del registro de vacunas sin afectar al registro padre de la mascota.
    public void eliminarVacuna(int id) throws SQLException {
        String sql = "DELETE FROM vacunas WHERE id = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
