package com.sequenceiq.cloudbreak;

import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlStatementInspector implements StatementInspector {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlStatementInspector.class);

    private static AtomicInteger selectStatements = new AtomicInteger(0);

    public static int getSelectCountNumberAndReset() {
        return selectStatements.getAndSet(0);
    }

    @Override
    public String inspect(String sql) {
        LOGGER.info(" Inspect the statement: " + sql);
        if (sql.toLowerCase().startsWith("select")) {
            selectStatements.incrementAndGet();
        }

        return null;
    }
}
