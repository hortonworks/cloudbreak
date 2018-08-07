package com.sequenceiq.cloudbreak.blueprint.template.views.dialect;

public class DefaultRdsViewDialect implements RdsViewDialect {
    @Override
    public String databaseNameSplitter() {
        return "/";
    }

    @Override
    public String jdbcPrefixSplitter() {
        return "//";
    }
}
