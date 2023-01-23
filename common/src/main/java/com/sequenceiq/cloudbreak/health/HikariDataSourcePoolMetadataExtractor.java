package com.sequenceiq.cloudbreak.health;

import org.springframework.boot.jdbc.metadata.HikariDataSourcePoolMetadata;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariDataSource;

@Component
public class HikariDataSourcePoolMetadataExtractor {

    public DataSourcePoolMetadata extract(HikariDataSource hikariDataSource) {
        HikariDataSourcePoolMetadata metadata = new HikariDataSourcePoolMetadata(hikariDataSource);
        return new DataSourcePoolMetadata(metadata.getActive(), metadata.getIdle(), metadata.getMax());
    }
}
