package com.sequenceiq.cloudbreak.template.views.dialect;

public interface RdsViewDialect {

    String databaseNameSplitter();

    String jdbcPrefixSplitter();
}
