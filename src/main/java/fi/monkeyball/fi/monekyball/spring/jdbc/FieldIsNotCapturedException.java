package fi.monkeyball.fi.monekyball.spring.jdbc;

/**
 * Created by juhof on 15/08/15.
 */
public class FieldIsNotCapturedException extends CapturingRowMapperException {
    public <V extends Object, T> FieldIsNotCapturedException(String fieldName) {
        super("Field '" + fieldName + "' is not captured");
    }
}
