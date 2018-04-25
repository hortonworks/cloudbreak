package com.sequenceiq.cloudbreak.blueprint.template.views.dialect;

public class OracleRdsViewDialect implements RdsViewDialect {
    @Override
    public String databaseNameSplitter() {
        return ":";
    }

    @Override
    public String jdbcPrefixSplitter() {
        return "@";
    }
}
