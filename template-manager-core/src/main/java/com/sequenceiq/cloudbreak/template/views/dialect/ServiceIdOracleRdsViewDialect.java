package com.sequenceiq.cloudbreak.template.views.dialect;

public class ServiceIdOracleRdsViewDialect implements RdsViewDialect {
    @Override
    public String databaseNameSplitter() {
        return "/";
    }

    @Override
    public String jdbcPrefixSplitter() {
        return "@";
    }
}
