package Modelo.Utilidades;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ResultSetMapper {
    
    public static <T> List<T> mapList(ResultSet rs, Function<ResultSet, T> mapper) throws SQLException {
        List<T> list = new ArrayList<>();
        while (rs.next()) {
            list.add(mapper.apply(rs));
        }
        return list;
    }
}