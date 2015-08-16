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

    /** A special object for marking null in result set  */
    private static final Object CAPTURED_NULL = new Object();

    /** column names which are captured  */
    private final String[] capturedFieldKeys;

    /** Captured values by object */
    private final HashMap<Integer, HashMap<String, Object>> capturedValues = new HashMap<>();

    public CapturingRowMapper(String ... capturedFieldKeys) {
        this.capturedFieldKeys = capturedFieldKeys;
    }

    @Override
    public final T mapRow(ResultSet resultSet, int i) throws SQLException {
        // Map base object
        T baseObject = mapBaseObject(resultSet, i);
        // Row mappers can't return null
        if(baseObject == null) {
            throw new CapturingRowMapperException("CapturingRowMapper mapBaseObject returned null");
        }
        // Capture fields
        captureFieldsFromResultSet(resultSet, baseObject);
        // Return base object
        return baseObject;
    }

    private void captureFieldsFromResultSet(ResultSet resultSet, T baseObject) throws SQLException {
        // Initialise container for object
        this.capturedValues.put(baseObject.hashCode(), new HashMap<>());
        // Capture fields
        for (String capturedFieldKey : capturedFieldKeys) {
            saveCapturedValue(baseObject, capturedFieldKey, getCapturedValueFromResultSet(resultSet, capturedFieldKey));
        }
    }

    private Object getCapturedValueFromResultSet(ResultSet resultSet, String capturedFieldKey) {
        try {
            return resultSet.getObject(capturedFieldKey);
        } catch (SQLException e) {
            // SQLException was thrown, wrap it as a CapturingRowMapperException
            throw new CapturingRowMapperException("Error while capturing field " + capturedFieldKey, e);
        }
    }

    private void saveCapturedValue(T t, String capturedFieldKey, Object capturedObject) throws SQLException {
        // If resultSet.getObject(...) returns null mark as special "was null in result set"
        if(capturedObject == null) {
            capturedObject = CAPTURED_NULL;
        }
        this.capturedValues.get(t.hashCode()).put(capturedFieldKey, capturedObject);
    }

    /**
     * Implement as you implement mapRow in normal row mapper
     *
     * @param resultSet
     * @param i
     * @return
     * @throws SQLException
     */
    public abstract T mapBaseObject(ResultSet resultSet, int i) throws SQLException;

    /**
     * Returns captured fieldName of object and cast to expectedClass
     *
     * @param object
     * @param fieldName
     * @param expectedClass
     * @param <V>
     * @return
     */
    public <V> V  captured(T object, String fieldName, Class<V> expectedClass) {
        V value = getCapturedValue(object, fieldName);
        // It was captured but value was null
        if(value == CAPTURED_NULL) {
            return null;
        }
        assertClassIsAsExpected(fieldName, expectedClass, value);
        return value;
    }

    private <V> V getCapturedValue(T object, String fieldName) {
        V value = (V) this.capturedValues.get(object.hashCode()).get(fieldName);
        if(value == null) {
            throw new FieldIsNotCapturedException(fieldName);
        }
        return value;
    }

    private <V> void assertClassIsAsExpected(String fieldName, Class<V> expectedClass, V value) {
        if(!value.getClass().equals(expectedClass)) {
            throw new FieldIsNotExpectedTypeException(fieldName, expectedClass, value.getClass());
        }
    }
}
