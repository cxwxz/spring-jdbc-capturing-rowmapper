package fi.monkeyball.fi.monekyball.spring.jdbc;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * It's a abstract RowMapper which "captures" fields in result set for later use. Implement
 * T mapBaseObject(ResultSet resultSet, int i) which should return actual object from query.
 *
 *
 * Created by juhof on 15/08/15.
 */
public abstract class CapturingRowMapper<T> implements RowMapper<T> {

    private final String[] capturedFieldKeys;
    private final HashMap<Integer, HashMap<String, Object>> capturedValues = new HashMap<>();

    public CapturingRowMapper(String ... capturedFieldKeys) {
        this.capturedFieldKeys = capturedFieldKeys;
    }

    @Override
    public T mapRow(ResultSet resultSet, int i) throws SQLException {
        T t = mapBaseObject(resultSet, i);
        if(t == null) {
            throw new CapturingRowMapperException("CapturingRowMapper mapBaseObject returned null");
        }
        if(!this.capturedValues.containsKey(t.hashCode())) {
            this.capturedValues.put(t.hashCode(), new HashMap<>());
        }
        for (String capturedFieldKey : capturedFieldKeys) {
            this.capturedValues.get(t.hashCode()).put(capturedFieldKey, resultSet.getObject(capturedFieldKey));
        }

        return t;
    }

    public abstract T mapBaseObject(ResultSet resultSet, int i) throws SQLException;

    public <V> V  captured(T object, String fieldName, Class<V> expectedClass) {

        V value = (V) this.capturedValues.get(object.hashCode()).get(fieldName);
        if(value == null) {
            throw new FieldIsNotCapturedException(fieldName);
        }
        if(!value.getClass().equals(expectedClass)) {
            throw new FieldIsNotExpectedTypeException(fieldName, expectedClass, value.getClass());
        }

        return value;
    }
}
