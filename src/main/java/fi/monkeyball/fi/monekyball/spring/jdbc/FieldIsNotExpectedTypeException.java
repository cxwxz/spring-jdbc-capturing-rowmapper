package fi.monkeyball.fi.monekyball.spring.jdbc;

/**
 * Created by juhof on 15/08/15.
 */
public class FieldIsNotExpectedTypeException extends CapturingRowMapperException {

    public FieldIsNotExpectedTypeException(String fieldName, Class<? extends Object> expectedClass, Class<? extends Object> actuallClass) {
        super("Field '" + fieldName + " is not of class " + expectedClass + " it is of class " + actuallClass);
    }
}
