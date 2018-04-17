package com.sequenceiq.cloudbreak.blueprint.template.views.dialect;

public interface RdsViewDialect {

    String databaseNameSplitter();

    String jdbcPrefixSplitter();
}
