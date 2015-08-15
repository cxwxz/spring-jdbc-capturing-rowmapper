package fi.monkeyball.fi.monekyball.spring.jdbc;

/**
 * Created by juhof on 15/08/15.
 */
public class CapturingRowMapperException extends RuntimeException {
    public CapturingRowMapperException(String message) {
        super(message);
    }
}
