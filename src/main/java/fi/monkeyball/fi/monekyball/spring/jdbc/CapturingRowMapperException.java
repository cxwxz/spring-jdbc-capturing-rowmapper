package fi.monkeyball.fi.monekyball.spring.jdbc;

import java.sql.SQLException;

/**
 * Created by juhof on 15/08/15.
 */
public class CapturingRowMapperException extends RuntimeException {
    public CapturingRowMapperException(String message) {
        super(message);
    }

    public CapturingRowMapperException(String message, Throwable cause) {
        super(message, cause);
    }
}
