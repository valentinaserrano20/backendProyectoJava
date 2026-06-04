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
        // Explicación de consulta SQL:
        // - Información buscada: El conteo total de mascotas asociadas a un plan específico.
        // - Tablas participantes: mascotas.
        // - Filtros aplicados: plan_id = ? (filtrado por el plan familiar de interés).
        String sql = "SELECT COUNT(*) AS total FROM mascotas WHERE plan_id = ?";
        // Qué hace: Abre la conexión a la base de datos y compila el statement parametrizado.
        // Por qué existe: Previene la inyección SQL al usar PreparedStatement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Vincula el ID del plan familiar al primer marcador de la consulta.
            ps.setInt(1, planId);
            // Qué hace: Ejecuta la consulta de conteo y lee el ResultSet.
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Qué hace: Retorna la cantidad total de mascotas encontradas en la columna total.
                    return rs.getInt("total");
                }
            }
        }
        // Qué hace: Retorna 0 si la consulta no produjo resultados.
        return 0;
    }

    // Qué hace: Consulta una lista paginada de mascotas asociadas a un plan familiar, uniendo con su respectiva especie.
    // Por qué existe: Alimenta el renderizado de la cuadrícula de mascotas de la familia de forma dosificada en el cliente.
    // Qué problema resuelve: Evita la sobrecarga de memoria del servidor al recuperar grupos delimitados de registros utilizando LIMIT y OFFSET.
    public List<MascotaDTO> listarMascotas(int planId, int limit, int offset) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Identificador, nombre, raza, género, fecha de nacimiento, especie e ID de plan familiar de cada mascota.
        // - Tablas participantes: mascotas (m), especies_mascota (e), generos_mascota (g).
        // - Relaciones (JOINs): LEFT JOIN con especies_mascota en especie_id y con generos_mascota en genero_id.
        // - Filtros aplicados: m.plan_id = ?, paginado de forma segura con LIMIT ? OFFSET ?.
        String sql = "SELECT m.id, m.nombre, m.raza, m.genero_id, g.nombre AS genero_nombre, m.fecha_nacimiento, m.plan_id, m.especie_id, e.nombre AS especie_nombre "
                   + "FROM mascotas m "
                   + "LEFT JOIN especies_mascota e ON m.especie_id = e.id "
                   + "LEFT JOIN generos_mascota g ON m.genero_id = g.id "
                   + "WHERE m.plan_id = ? "
                   + "LIMIT ? OFFSET ?";
        
        // Qué hace: Abre la conexión a base de datos y compila el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Asigna los valores del ID del plan, el límite y el offset al statement.
            ps.setInt(1, planId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            
            // Qué hace: Inicializa la lista que contendrá las mascotas encontradas.
            List<MascotaDTO> lista = new ArrayList<>();
            // Qué hace: Ejecuta la consulta de lectura y procesa el ResultSet.
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Qué hace: Instancia el DTO para mapear la fila actual.
                    MascotaDTO dto = new MascotaDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setName(rs.getString("nombre"));
                    // Qué hace: Valida nulos en la columna raza.
                    dto.setBreed(rs.getString("raza") != null ? rs.getString("raza") : "Sin raza");
                    
                    // Qué hace: Mapea la información de género.
                    String gen = rs.getString("genero_nombre");
                    dto.setAnimalGender(gen);
                    dto.setAnimalGenderName(gen != null ? gen : "No especificado");
                    dto.setAnimalGenderId(rs.getInt("genero_id"));
                    
                    // Qué hace: Calcula y formatea la edad del animal basada en su fecha de nacimiento.
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
                    // Qué hace: Valida nulos en el nombre de la especie.
                    dto.setSpeciesName(rs.getString("especie_nombre") != null ? rs.getString("especie_nombre") : "Otro");
                    // Qué hace: Añade el DTO poblado a la lista de retorno.
                    lista.add(dto);
                }
            }
            // Qué hace: Retorna el listado de mascotas mapeadas.
            return lista;
        }
    }

    // Qué hace: Obtiene la información detallada de una mascota singular y su relación con especies_mascota.
    // Por qué existe: Permite precargar la información de la mascota en el formulario de edición y ventanas de detalles.
    // Qué problema resuelve: Facilita la recuperación atómica y limpia de los atributos de un animal individual por su ID único.
    public MascotaDTO obtenerMascota(int id) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Detalles del animal, incluyendo su género y especie.
        // - Tablas participantes: mascotas (m), especies_mascota (e), generos_mascota (g).
        // - Relaciones (JOINs): LEFT JOIN con especies_mascota en especie_id y con generos_mascota en genero_id.
        // - Filtros aplicados: m.id = ? (filtrado por el identificador de la mascota).
        String sql = "SELECT m.id, m.nombre, m.raza, m.genero_id, g.nombre AS genero_nombre, m.fecha_nacimiento, m.plan_id, m.especie_id, e.nombre AS especie_nombre "
                   + "FROM mascotas m "
                   + "LEFT JOIN especies_mascota e ON m.especie_id = e.id "
                   + "LEFT JOIN generos_mascota g ON m.genero_id = g.id "
                   + "WHERE m.id = ?";
        
        // Qué hace: Abre la conexión a la base de datos y compila el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Asigna el ID de la mascota al statement.
            ps.setInt(1, id);
            // Qué hace: Ejecuta el SELECT en la base de datos.
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Qué hace: Instancia el DTO y mapea cada columna del ResultSet.
                    MascotaDTO dto = new MascotaDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setName(rs.getString("nombre"));
                    dto.setBreed(rs.getString("raza"));
                    
                    String gen = rs.getString("genero_nombre");
                    dto.setAnimalGender(gen);
                    dto.setAnimalGenderName(gen);
                    dto.setAnimalGenderId(rs.getInt("genero_id"));
                    
                    // Qué hace: Calcula y setea la edad de la mascota de forma segura.
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
                    // Qué hace: Retorna la mascota detallada.
                    return dto;
                }
            }
        }
        // Qué hace: Retorna null si no se localizó la mascota por su ID.
        return null;
    }

    // Qué hace: Inserta una nueva mascota en la base de datos de MySQL y retorna el ID autogenerado asignado.
    // Por qué existe: Soporta la creación física de mascotas asociadas a un núcleo familiar voluntario evaluado.
    // Qué problema resuelve: Garantiza el almacenamiento consistente de los tipos de datos (como fecha y llaves foráneas) controlando nulos en columnas opcionales.
    public int crearMascota(MascotaDTO dto) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Creación de un registro en la tabla mascotas.
        // - Tablas participantes: mascotas.
        // - Filtros aplicados: Ninguno (INSERT INTO con placeholders).
        String sql = "INSERT INTO mascotas (nombre, raza, genero_id, fecha_nacimiento, plan_id, especie_id) VALUES (?, ?, ?, ?, ?, ?)";
        // Qué hace: Abre la conexión a base de datos y compila la consulta con retorno de llaves generadas.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            // Qué hace: Asigna el nombre y maneja el valor nulo de la raza si está vacía.
            ps.setString(1, dto.getName());
            ps.setString(2, dto.getBreed() != null && !dto.getBreed().isEmpty() ? dto.getBreed() : null);
            
            // Qué hace: Regla de negocio: Por defecto, asigna el género con ID 1 si no es válido.
            ps.setInt(3, dto.getAnimalGenderId() > 0 ? dto.getAnimalGenderId() : 1);
            
            // Qué hace: Controla si la fecha de nacimiento viene vacía o nula para insertar el valor NULL.
            if (dto.getBirthDate() != null && !dto.getBirthDate().isEmpty()) {
                ps.setDate(4, Date.valueOf(dto.getBirthDate()));
            } else {
                ps.setNull(4, Types.DATE);
            }
            
            ps.setInt(5, dto.getPlanId());
            
            // Qué hace: Controla si el ID de especie es válido o inserta NULL.
            if (dto.getSpeciesId() > 0) {
                ps.setInt(6, dto.getSpeciesId());
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            
            // Qué hace: Ejecuta la inserción.
            ps.executeUpdate();
            
            // Qué hace: Recupera las llaves primarias autogeneradas generadas por MySQL.
            try (ResultSet rsKeys = ps.getGeneratedKeys()) {
                if (rsKeys.next()) {
                    return rsKeys.getInt(1);
                }
            }
        }
        // Qué hace: Lanza excepción si falla la recuperación de la llave autogenerada.
        throw new SQLException("Error al recuperar el ID autogenerado de la mascota registrada.");
    }

    // Qué hace: Modifica los atributos base de una mascota existente en la base de datos.
    // Por qué existe: Posibilita que el voluntario guarde correcciones del nombre, raza, género o edad de la mascota.
    // Qué problema resuelve: Actualiza los campos específicos de la mascota sin alterar su relación estructurada con el plan familiar.
    public void actualizarMascota(int id, MascotaDTO dto) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Actualizar campos básicos de la mascota.
        // - Tablas participantes: mascotas.
        // - Filtros aplicados: WHERE id = ? (se actualiza el registro con el ID correspondiente).
        String sql = "UPDATE mascotas SET nombre = ?, raza = ?, genero_id = ?, fecha_nacimiento = ?, especie_id = ? WHERE id = ?";
        // Qué hace: Abre la conexión y prepara la consulta SQL.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            // Qué hace: Vincula los parámetros actualizados de la mascota.
            ps.setString(1, dto.getName());
            ps.setString(2, dto.getBreed() != null && !dto.getBreed().isEmpty() ? dto.getBreed() : null);
            
            // Qué hace: Asigna el ID de género.
            ps.setInt(3, dto.getAnimalGenderId() > 0 ? dto.getAnimalGenderId() : 1);
            
            // Qué hace: Maneja la fecha de nacimiento nula.
            if (dto.getBirthDate() != null && !dto.getBirthDate().isEmpty()) {
                ps.setDate(4, Date.valueOf(dto.getBirthDate()));
            } else {
                ps.setNull(4, Types.DATE);
            }
            
            // Qué hace: Asigna especie_id o setea NULL.
            if (dto.getSpeciesId() > 0) {
                ps.setInt(5, dto.getSpeciesId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            
            // Qué hace: Vincula el id de la mascota para el WHERE.
            ps.setInt(6, id);
            // Qué hace: Ejecuta la consulta de actualización en MySQL.
            ps.executeUpdate();
        }
    }

    // Qué hace: Elimina una mascota de forma transaccional, borrando primero todas sus vacunas asociadas.
    // Por qué existe: Evita violaciones de restricciones de claves foráneas de MySQL durante el borrado físico de la mascota.
    // Qué problema resuelve: Garantiza la atomicidad y la limpieza del historial sanitario evitando registros huérfanos en la base de datos.
    public void eliminarMascota(int id) throws SQLException {
        Connection con = null;
        try {
            // Qué hace: Obtiene la conexión JDBC.
            con = Conexion.obtener();
            // Qué hace: Desactiva el auto-commit automático para controlar de manera manual la transacción.
            con.setAutoCommit(false);
            
            // Explicación de consulta SQL (Eliminar vacunas):
            // - Información buscada: Borrar las vacunas del animal.
            // - Tablas participantes: vacunas.
            // - Filtros aplicados: mascota_id = ?.
            String sqlDelVacunas = "DELETE FROM vacunas WHERE mascota_id = ?";
            try (PreparedStatement psV = con.prepareStatement(sqlDelVacunas)) {
                // Qué hace: Vincula el ID de la mascota y ejecuta el delete.
                psV.setInt(1, id);
                psV.executeUpdate();
            }
            
            // Explicación de consulta SQL (Eliminar mascota):
            // - Información buscada: Eliminar el registro de la mascota.
            // - Tablas participantes: mascotas.
            // - Filtros aplicados: id = ?.
            String sqlDelMascota = "DELETE FROM mascotas WHERE id = ?";
            try (PreparedStatement psM = con.prepareStatement(sqlDelMascota)) {
                // Qué hace: Vincula el ID de la mascota y ejecuta el delete.
                psM.setInt(1, id);
                psM.executeUpdate();
            }
            
            // Qué hace: Confirma la transacción en base de datos de manera definitiva.
            con.commit();
        } catch (SQLException e) {
            // Qué hace: Si algo falla durante el borrado, revierte la transacción para prevenir la pérdida de integridad.
            if (con != null) {
                con.rollback();
            }
            throw e;
        } finally {
            // Qué hace: Cierra la conexión JDBC.
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
        // Explicación de consulta SQL:
        // - Información buscada: Columnas de vacunas asociadas a la mascota.
        // - Tablas participantes: vacunas.
        // - Filtros aplicados: mascota_id = ?, ordenadas descendente por fecha de aplicación.
        String sql = "SELECT id, nombre_vacuna, fecha_aplicacion, mascota_id FROM vacunas WHERE mascota_id = ? ORDER BY fecha_aplicacion DESC";
        // Qué hace: Abre la conexión a base de datos y compila el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Asigna el ID de la mascota al statement.
            ps.setInt(1, mascotaId);
            
            // Qué hace: Inicializa la lista que almacenará las vacunas mapeadas.
            List<VacunaMascotaDTO> lista = new ArrayList<>();
            // Qué hace: Ejecuta la consulta de lectura y procesa el ResultSet.
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    VacunaMascotaDTO dto = new VacunaMascotaDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setName(rs.getString("nombre_vacuna"));
                    
                    // Qué hace: Formatea y mapea de forma segura la fecha de aplicación.
                    Date date = rs.getDate("fecha_aplicacion");
                    dto.setDate(date != null ? date.toString() : "");
                    
                    dto.setPetId(rs.getInt("mascota_id"));
                    // Qué hace: Agrega la vacuna a la lista de retorno.
                    lista.add(dto);
                }
            }
            // Qué hace: Retorna el listado de vacunas del animal.
            return lista;
        }
    }

    // Qué hace: Obtiene los detalles de una vacuna singular registrada por su ID.
    // Por qué existe: Se utiliza para alimentar el modal flotante de edición de vacuna específica.
    // Qué problema resuelve: Recupera de forma limpia y exacta la información e historial de una vacuna particular.
    public VacunaMascotaDTO obtenerVacuna(int id) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Datos de la vacuna específica.
        // - Tablas participantes: vacunas.
        // - Filtros aplicados: id = ? (id de la vacuna).
        String sql = "SELECT id, nombre_vacuna, fecha_aplicacion, mascota_id FROM vacunas WHERE id = ?";
        // Qué hace: Abre la conexión a la base de datos y compila la consulta SQL.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Asigna el ID de la vacuna al statement.
            ps.setInt(1, id);
            // Qué hace: Ejecuta el query y lee la fila única resultante.
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Qué hace: Instancia el DTO y realiza el mapeo.
                    VacunaMascotaDTO dto = new VacunaMascotaDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setName(rs.getString("nombre_vacuna"));
                    
                    Date date = rs.getDate("fecha_aplicacion");
                    dto.setDate(date != null ? date.toString() : "");
                    
                    dto.setPetId(rs.getInt("mascota_id"));
                    // Qué hace: Retorna el DTO de la vacuna encontrada.
                    return dto;
                }
            }
        }
        // Qué hace: Retorna null si la vacuna no existe.
        return null;
    }

    // Qué hace: Inserta un registro de vacuna en la base de datos y retorna el ID autogenerado.
    // Por qué existe: Habilita el registro de una nueva inmunización al animal dentro del flujo del voluntario.
    // Qué problema resuelve: Asegura la correcta escritura e integridad de la fecha y de la clave foránea a la mascota.
    public int crearVacuna(VacunaMascotaDTO dto) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Creación de un registro en la tabla vacunas.
        // - Tablas participantes: vacunas.
        String sql = "INSERT INTO vacunas (nombre_vacuna, fecha_aplicacion, mascota_id) VALUES (?, ?, ?)";
        // Qué hace: Abre la conexión a base de datos y compila la consulta con retorno de llaves generadas.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            // Qué hace: Vincula los parámetros del DTO al statement JDBC.
            ps.setString(1, dto.getName());
            ps.setDate(2, Date.valueOf(dto.getDate()));
            ps.setInt(3, dto.getPetId());
            
            // Qué hace: Ejecuta la inserción.
            ps.executeUpdate();
            // Qué hace: Recupera la llave autoincremental de la vacuna insertada.
            try (ResultSet rsKeys = ps.getGeneratedKeys()) {
                if (rsKeys.next()) {
                    return rsKeys.getInt(1);
                }
            }
        }
        // Qué hace: Lanza una excepción si falla la inserción.
        throw new SQLException("Error al recuperar el ID autogenerado de la vacuna registrada.");
    }

    // Qué hace: Actualiza el nombre o la fecha de aplicación de una vacuna específica.
    // Por qué existe: Permite al voluntario editar la dosis o corregir la fecha de inmunización del animal.
    // Qué problema resuelve: Realiza modificaciones sobre el registro particular de vacunas sin afectar las relaciones del animal.
    public void actualizarVacuna(int id, VacunaMascotaDTO dto) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Modificar los atributos de una vacuna.
        // - Tablas participantes: vacunas.
        // - Filtros aplicados: WHERE id = ? (id de la vacuna).
        String sql = "UPDATE vacunas SET nombre_vacuna = ?, fecha_aplicacion = ? WHERE id = ?";
        // Qué hace: Abre la conexión a base de datos y compila el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            // Qué hace: Vincula los nuevos valores al statement.
            ps.setString(1, dto.getName());
            ps.setDate(2, Date.valueOf(dto.getDate()));
            ps.setInt(3, id);
            
            // Qué hace: Ejecuta la consulta de actualización.
            ps.executeUpdate();
        }
    }

    // Qué hace: Elimina una vacuna específica de la base de datos por su ID único.
    // Por qué existe: Permite dar de baja o quitar vacunas registradas incorrectamente.
    // Qué problema resuelve: Ejecuta la remoción directa del registro de vacunas sin afectar al registro padre de la mascota.
    public void eliminarVacuna(int id) throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Borrado del registro de vacuna.
        // - Tablas participantes: vacunas.
        // - Filtros aplicados: WHERE id = ?.
        String sql = "DELETE FROM vacunas WHERE id = ?";
        // Qué hace: Abre la conexión a la base de datos y compila el statement parametrizado.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // Qué hace: Vincula el ID de vacuna y ejecuta la eliminación.
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // Qué hace: Obtiene la lista completa de todas las mascotas registradas en el sistema.
    // Por qué existe: Soporta el filtrado de mascotas del supervisor en el panel de revisión de planes familiares.
    // Qué problema resuelve: Recupera de forma masiva y estructurada todas las mascotas para que el supervisor filtre localmente.
    public List<MascotaDTO> obtenerTodasMascotas() throws SQLException {
        // Explicación de consulta SQL:
        // - Información buscada: Listado de todas las mascotas con su identificador, nombre, raza, género, fecha de nacimiento, especie y ID de plan familiar.
        // - Tablas participantes: mascotas (m), especies_mascota (e), generos_mascota (g).
        // - Relaciones (JOINs): LEFT JOIN con especies_mascota en especie_id y con generos_mascota en genero_id.
        // - Filtros aplicados: Ninguno (consulta global).
        String sql = "SELECT m.id, m.nombre, m.raza, m.genero_id, g.nombre AS genero_nombre, m.fecha_nacimiento, m.plan_id, m.especie_id, e.nombre AS especie_nombre "
                   + "FROM mascotas m "
                   + "LEFT JOIN especies_mascota e ON m.especie_id = e.id "
                   + "LEFT JOIN generos_mascota g ON m.genero_id = g.id";
        
        // Qué hace: Abre la conexión JDBC y prepara la consulta.
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            // Qué hace: Inicializa la lista dinámica para almacenar las mascotas.
            List<MascotaDTO> lista = new ArrayList<>();
            // Qué hace: Recorre cada fila del ResultSet de la base de datos.
            while (rs.next()) {
                // Qué hace: Instancia el DTO de mascota para almacenar los datos de la fila actual.
                MascotaDTO dto = new MascotaDTO();
                dto.setId(rs.getInt("id"));
                dto.setName(rs.getString("nombre"));
                dto.setBreed(rs.getString("raza") != null ? rs.getString("raza") : "Sin raza");
                
                // Qué hace: Mapea la información de género.
                String gen = rs.getString("genero_nombre");
                dto.setAnimalGender(gen);
                dto.setAnimalGenderName(gen != null ? gen : "No especificado");
                dto.setAnimalGenderId(rs.getInt("genero_id"));
                
                // Qué hace: Calcula y formatea la edad del animal basada en su fecha de nacimiento.
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
                // Qué hace: Agrega el DTO a la lista de retorno.
                lista.add(dto);
            }
            // Qué hace: Retorna la lista resultante de todas las mascotas registradas.
            return lista;
        }
    }
}
