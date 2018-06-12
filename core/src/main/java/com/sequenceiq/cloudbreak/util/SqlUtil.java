package com.sequenceiq.cloudbreak.util;

import java.util.Arrays;
import java.util.function.Function;

import org.postgresql.util.PSQLException;
import org.springframework.dao.DataIntegrityViolationException;

public class SqlUtil {

    private SqlUtil() {
    }

    public static String getProperSqlErrorMessage(DataIntegrityViolationException dive) {
        Throwable cause = dive.getMostSpecificCause();
        if (cause instanceof PSQLException) {
            PSQLException ex = (PSQLException) cause;
            MessageCleaner messageCleaner = MessageCleaner.fromSqlState(ex.getSQLState());
            return messageCleaner.cleanerFunction.apply(ex.getLocalizedMessage());
        }
        return cause.getLocalizedMessage();
    }

    private enum MessageCleaner {
        UNIQUE_CONSTRAINT("23505", (orig) -> orig.split("\\n")[1].replace("  Detail: ", "")),
        COMMON(null, (orig) -> orig.split("\\n")[0]);

        private final String sqlState;

        private final Function<String, String> cleanerFunction;

        MessageCleaner(String sqlState, Function<String, String> cleanerFunction) {
            this.sqlState = sqlState;
            this.cleanerFunction = cleanerFunction;
        }

        private static MessageCleaner fromSqlState(String sqlState) {
            return Arrays.stream(values()).filter(mc -> mc.sqlState.equals(sqlState)).findFirst().orElse(COMMON);
        }
    }
}
