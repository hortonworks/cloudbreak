package com.sequenceiq.cloudbreak;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlStatementInspector implements StatementInspector {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlStatementInspector.class);

    private static final AtomicInteger SELECT_STATEMENTS = new AtomicInteger(0);

    public static int getSelectCountNumberAndReset() {
        return SELECT_STATEMENTS.getAndSet(0);
    }

    @Override
    public String inspect(String sql) {
        LOGGER.info(" Inspect the statement: " + sql);
        if (sql.toLowerCase(Locale.ROOT).startsWith("select")) {
            SELECT_STATEMENTS.incrementAndGet();
        }

        return null;
    }
}
