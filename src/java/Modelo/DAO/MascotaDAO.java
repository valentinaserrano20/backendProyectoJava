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
        // Explicación detallada de la consulta SQL:
        // - Comando SELECT COUNT(*) AS total: Cuenta la cantidad total de registros (filas) que cumplen la condición y le asigna el alias "total" a la columna resultante para recuperarla fácilmente en Java.
        // - Tabla FROM mascotas: Especifica la tabla física de donde se extraerán y contarán los datos.
        // - Filtro WHERE plan_id = ?: Condición que restringe el conteo únicamente a las mascotas asociadas al plan familiar del ID provisto en el marcador posicional.
        // - Qué retorna: Una única fila con la columna "total" conteniendo el número entero de registros coincidentes.
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
        // Explicación detallada de la consulta SQL:
        // - Columnas consultadas (m.id, m.nombre, etc.): Selecciona campos clave de la mascota (m), el nombre descriptivo de su género (g.nombre) y su especie (e.nombre) para mapear el DTO completo.
        // - Tabla principal FROM mascotas m: Indica que la consulta base se realiza sobre la tabla de mascotas.
        // - Relación LEFT JOIN especies_mascota e ON m.especie_id = e.id: Une la tabla mascotas con especies_mascota. Se usa LEFT JOIN (unión izquierda) en lugar de INNER JOIN para garantizar que si una mascota no tiene especie asignada (especie_id es null o no existe), el animal de todos modos aparezca en la lista final con el campo de especie en nulo, en vez de ser excluido del resultado.
        // - Relación LEFT JOIN generos_mascota g ON m.genero_id = g.id: Une mascotas con el catálogo de géneros de forma opcional (LEFT JOIN) para incluir al animal incluso si no tiene un género especificado.
        // - Filtro WHERE m.plan_id = ?: Limita los resultados a las mascotas pertenecientes al ID de plan familiar ingresado en el primer marcador de posición.
        // - Cláusula LIMIT ?: Restringe el número máximo de filas que el motor de base de datos devolverá (por ejemplo, 10 filas para no sobrecargar el navegador).
        // - Cláusula OFFSET ?: Indica cuántos registros iniciales debe saltarse MySQL antes de comenzar a leer (por ejemplo, en la página 2 saltará los primeros 10 registros). Sirve para implementar la paginación de datos.
        // - Qué retorna: Un listado de filas que representan cada mascota del plan familiar con sus respectivas descripciones de género y especie.
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
        // Explicación detallada de la consulta SQL:
        // - Columnas consultadas (m.id, m.nombre, etc.): Selecciona todos los campos de información atómica del animal.
        // - Relaciones LEFT JOIN e y g: Conecta la tabla mascotas con especies_mascota y generos_mascota. Se usa LEFT JOIN para asegurar que si el animal no tiene género o especie configurados en la base de datos, la consulta siga teniendo éxito y devuelva los datos básicos del animal, rellenando con null los campos ausentes en lugar de ignorar la fila completa.
        // - Filtro WHERE m.id = ?: Filtra por la clave primaria única del animal, asegurando que se recupere a lo sumo un solo registro coincidente.
        // - Qué retorna: Una única fila con la información completa de la mascota, que se mapea directamente a un objeto MascotaDTO en Java.
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
        // Definición de la sentencia SQL parametrizada
        String sql = "INSERT INTO mascotas (nombre, raza, genero_id, fecha_nacimiento, plan_id, especie_id) VALUES (?, ?, ?, ?, ?, ?)";
        // Usamos la estructura try-with-resources para garantizar que la conexión JDBC y el PreparedStatement se cierren automáticamente al finalizar
        try (Connection con = Conexion.obtener();
             // El PreparedStatement sirve para compilar de forma segura la consulta SQL con placeholders '?', anulando la posibilidad de inyecciones SQL
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            // Usamos ps.setString para vincular de forma sanitizada el nombre de la mascota al parámetro 1
            ps.setString(1, dto.getName());
            // Controlamos si la raza fue provista o pasamos un valor nulo seguro a MySQL
            ps.setString(2, dto.getBreed() != null && !dto.getBreed().isEmpty() ? dto.getBreed() : null);
            
            // Usamos ps.setInt para vincular el ID de género al parámetro 3
            ps.setInt(3, dto.getAnimalGenderId() > 0 ? dto.getAnimalGenderId() : 1);
            
            // Validamos la presencia de la fecha de nacimiento del animal
            if (dto.getBirthDate() != null && !dto.getBirthDate().isEmpty()) {
                // Usamos ps.setDate para transformar el String en un objeto java.sql.Date y vincularlo
                ps.setDate(4, Date.valueOf(dto.getBirthDate()));
            } else {
                // Usamos ps.setNull para insertar un valor nulo en la columna de fecha de nacimiento si esta no fue provista
                ps.setNull(4, Types.DATE);
            }
            
            // Usamos ps.setInt para vincular el ID del plan de emergencia familiar al parámetro 5
            ps.setInt(5, dto.getPlanId());
            
            // Validamos la especie y la vinculamos
            if (dto.getSpeciesId() > 0) {
                ps.setInt(6, dto.getSpeciesId());
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            
            // Ejecutamos la consulta física de inserción en la base de datos
            ps.executeUpdate();
            
            // El ResultSet rsKeys sirve para recuperar las llaves autogeneradas en MySQL tras la inserción exitosa
            try (ResultSet rsKeys = ps.getGeneratedKeys()) {
                if (rsKeys.next()) {
                    // Extraemos e identificamos el ID numérico asignado por MySQL
                    return rsKeys.getInt(1);
                }
            }
        }
        // Lanzamos una excepción en caso de que ocurran fallas no controladas al obtener la llave autogenerada
        throw new SQLException("Error al recuperar el ID autogenerado de la mascota registrada.");
    }

    // Qué hace: Modifica los atributos base de una mascota existente en la base de datos.
    // Por qué existe: Posibilita que el voluntario guarde correcciones del nombre, raza, género o edad de la mascota.
    // Qué problema resuelve: Actualiza los campos específicos de la mascota sin alterar su relación estructurada con el plan familiar.
    public void actualizarMascota(int id, MascotaDTO dto) throws SQLException {
        // Explicación detallada de la consulta SQL:
        // - Comando UPDATE mascotas: Ordena al motor MySQL modificar los valores de registros existentes en la tabla mascotas.
        // - Cláusulas SET nombre = ?, raza = ?, etc.: Especifica qué columnas sufrirán modificaciones y las vincula a marcadores de posición posicionales.
        // - Filtro WHERE id = ?: Cláusula crítica de seguridad que limita la modificación exclusivamente a la fila cuyo identificador único coincida con el ID provisto, previniendo la actualización accidental de toda la tabla.
        // - Qué retorna: No retorna datos (filas), sino que altera de forma persistente el registro modificado en MySQL.
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
            
            // Explicación detallada de la consulta SQL de eliminación de vacunas:
            // - Comando DELETE FROM vacunas: Ordena la eliminación física de registros de la tabla vacunas.
            // - Filtro WHERE mascota_id = ?: Condición que restringe el borrado únicamente a las vacunas que pertenezcan a la mascota seleccionada, limpiando registros dependientes para mantener la integridad referencial.
            String sqlDelVacunas = "DELETE FROM vacunas WHERE mascota_id = ?";
            try (PreparedStatement psV = con.prepareStatement(sqlDelVacunas)) {
                // Qué hace: Vincula el ID de la mascota y ejecuta el delete.
                psV.setInt(1, id);
                psV.executeUpdate();
            }
            
            // Explicación detallada de la consulta SQL de eliminación de la mascota:
            // - Comando DELETE FROM mascotas: Ordena la eliminación física del registro en la tabla mascotas.
            // - Filtro WHERE id = ?: Limita el borrado exclusivamente a la mascota cuyo identificador único coincida con el parámetro inyectado, garantizando que no se eliminen otros animales.
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
        // Explicación detallada de la consulta SQL:
        // - Comando SELECT id, nombre_vacuna, fecha_aplicacion, mascota_id: Recupera los datos necesarios del historial de inmunizaciones.
        // - Tabla FROM vacunas: Especifica que consultamos sobre la tabla de vacunas.
        // - Filtro WHERE mascota_id = ?: Restringe los registros exclusivamente a las vacunas asociadas al ID de la mascota provisto.
        // - Cláusula ORDER BY fecha_aplicacion DESC: Ordena cronológicamente los registros de forma descendente (del más reciente al más antiguo) basándose en la fecha.
        // - Qué retorna: Un listado de filas que representan las vacunas aplicadas a esa mascota.
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
        // Explicación detallada de la consulta SQL:
        // - Comando SELECT: Recupera las columnas descriptivas de una sola dosis vacunal.
        // - Filtro WHERE id = ?: Filtra la consulta por la clave primaria de la vacuna, asegurando recuperar un único registro exacto.
        // - Qué retorna: Una fila con la información de la vacuna seleccionada, mapeada en Java a VacunaMascotaDTO.
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
        // Explicación detallada de la consulta SQL:
        // - Comando INSERT INTO vacunas: Ordena al motor MySQL crear un nuevo registro físico en la tabla vacunas.
        // - Columnas (nombre_vacuna, fecha_aplicacion, mascota_id): Especifica el orden de inserción de las columnas.
        // - Cláusula VALUES (?, ?, ?): Marcadores de posición que recibirán los valores sanitizados a insertar (nombre, fecha de aplicación e ID de la mascota asociada).
        // - Qué retorna: No retorna filas, pero la base de datos genera un nuevo ID numérico autoincremental para este registro.
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
        // Explicación detallada de la consulta SQL:
        // - Comando UPDATE vacunas: Modifica valores de un registro de vacuna existente en MySQL.
        // - Filtro WHERE id = ?: Restringe de forma precisa la actualización a la vacuna seleccionada por su ID único, evitando alterar otras filas.
        // - Qué retorna: Altera de forma persistente el registro de vacuna seleccionado en la base de datos.
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
        // Explicación detallada de la consulta SQL:
        // - Comando DELETE FROM vacunas: Elimina de forma permanente el registro físico de la dosis de la base de datos.
        // - Filtro WHERE id = ?: Restringe la eliminación exclusivamente a la vacuna identificada por su ID.
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
        // Explicación detallada de la consulta SQL:
        // - Columnas consultadas (m.id, m.nombre, etc.): Selecciona la información clave de todas las mascotas guardadas en el sistema.
        // - Relaciones LEFT JOIN e y g: Une la tabla principal mascotas con los catálogos especies_mascota y generos_mascota. Se usa LEFT JOIN para asegurar que todas las mascotas del censo se incluyan en el listado devuelto, rellenando con valores nulos (null) la especie o el género si estos no han sido asignados al animal, impidiendo que filas válidas sean excluidas de la lista.
        // - Filtros aplicados: Ninguno (consulta global del supervisor).
        // - Qué retorna: El universo total de registros de mascotas que serán convertidos en objetos MascotaDTO.
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
