package com.sequenceiq.cloudbreak.util;

import java.util.Arrays;

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
            return messageCleaner.clean(ex.getLocalizedMessage());
        }
        return cause.getLocalizedMessage();
    }

    private enum MessageCleaner {
        UNIQUE_CONSTRAINT("23505") {
            @Override
            public String clean(String orig) {
                return orig.split("\\n")[1].replace("  Detail: ", "");
            }
        },
        COMMON(null) {
            @Override
            public String clean(String orig) {
                return orig.split("\\n")[0];
            }
        };

        private final String sqlState;

        MessageCleaner(String sqlState) {
            this.sqlState = sqlState;
        }

        public abstract String clean(String orig);

        private static MessageCleaner fromSqlState(String sqlState) {
            return Arrays.stream(values()).filter(mc -> mc.sqlState.equals(sqlState)).findFirst().orElse(COMMON);
        }
    }
}
