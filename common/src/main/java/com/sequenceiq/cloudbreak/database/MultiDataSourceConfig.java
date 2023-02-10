package com.sequenceiq.cloudbreak.database;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.zaxxer.hikari.HikariDataSource;

public abstract class MultiDataSourceConfig {

    @Bean(name = "defaultDataSource")
    public DataSource defaultDataSource() throws SQLException {
        return getDataSource("hikari-app-pool");
    }

    @Bean(name = "quartzDataSource")
    public DataSource quartzDataSource() throws SQLException {
        return getDataSource("hikari-quartz-pool");
    }

    @Primary
    @Bean(name = "dataSource")
    public DataSource dataSource() {
        return new RoutingDataSource();
    }

    protected abstract HikariDataSource getDataSource(String poolName) throws SQLException;

}
