package com.sequenceiq.cloudbreak.common.tx;

import com.sequenceiq.cloudbreak.common.metrics.type.Metric;

public enum HibernateMetricType implements Metric {

    TRANSACTION_DURATION("hibernate.transaction.duration"),
    TRANSACTION_WARNING("hibernate.transaction.warning"),
    JDBC_EXECUTE_DURATION("hibernate.jdbc.execute.duration"),
    STATEMENT_COUNT_WARNING("hibernate.statement.count.warning");

    private final String metricName;

    HibernateMetricType(String metricName) {
        this.metricName = metricName;
    }

    @Override
    public String getMetricName() {
        return metricName;
    }
}
