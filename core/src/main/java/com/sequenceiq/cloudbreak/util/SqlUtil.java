package com.sequenceiq.cloudbreak.util;

import java.sql.SQLException;

import org.postgresql.util.PSQLException;
import org.springframework.dao.DataIntegrityViolationException;

public class SqlUtil {

    private SqlUtil() {
    }

    public static String getProperSqlErrorMessage(DataIntegrityViolationException ex) {
        Throwable cause = ex.getCause();

        while (cause.getCause() != null || cause instanceof PSQLException) {
            if (cause instanceof PSQLException && !((SQLException) cause).getSQLState().isEmpty()) {
                PSQLException e = (PSQLException) cause;
                String[] split = e.getLocalizedMessage().split("\\n");
                if (split.length > 0) {
                    return split[0];
                }
            }
            cause = cause.getCause();
        }

        return ex.getLocalizedMessage();
    }

}
