package Modelo.Utilidades;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Qué hace: Clase utilitaria dedicada al mapeo genérico y funcional de conjuntos de resultados (ResultSet) de JDBC a listas de objetos.
 * Por qué existe: Abstrae y automatiza la iteración repetitiva de filas SQL del cursor de la base de datos, reduciendo el boilerplate en los DAOs.
 * Qué pasaría si no estuviera: Cada método en cada DAO que consulte colecciones de filas tendría que implementar manualmente el bucle while(rs.next()) para rellenar la lista del DTO.
 */
public class ResultSetMapper {
    
    /**
     * Qué hace: Recorre todas las filas de un ResultSet y aplica una función mapeadora para convertirlas a instancias de tipo T.
     * Qué significa: Ejecuta un bucle while llamando a rs.next(), aplica la lambda o referencia de método provista (Function) para cada fila y acumula los resultados en una List.
     * Para qué se usa: Simplifica la lectura y mapeado de registros SQL en los DAOs reduciendo líneas de código repetitivo de extracción.
     * Por qué es importante: Aumenta la legibilidad del código al separar el control del bucle de cursor SQL de la lógica particular de rellenar las propiedades del DTO.
     * 
     * @param <T> Tipo de clase de destino (habitualmente un DTO).
     * @param rs El ResultSet de JDBC activo que contiene las filas devueltas.
     * @param mapper Función de mapeo funcional (lambda) que procesa una fila y devuelve un objeto de tipo T.
     * @return Una List rellenada con los objetos mapeados.
     * @throws SQLException Si ocurre algún error al interactuar con el ResultSet de la base de datos.
     */
    public static <T> List<T> mapList(ResultSet rs, Function<ResultSet, T> mapper) throws SQLException {
        List<T> list = new ArrayList<>();
        while (rs.next()) {
            list.add(mapper.apply(rs));
        }
        return list;
    }
}