package com.sequenceiq.environment.platformresource.v1.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTable;
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTables;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformNoSqlTableResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformNoSqlTablesResponse;

class CloudNoSqlTablesToPlatformNoSqlTablesV1ResponseConverterTest {

    private CloudNoSqlTablesToPlatformNoSqlTablesV1ResponseConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new CloudNoSqlTablesToPlatformNoSqlTablesV1ResponseConverter();
    }

    @Test
    void convert() {
        List<CloudNoSqlTable> tables = List.of(new CloudNoSqlTable("a"), new CloudNoSqlTable("b"));
        CloudNoSqlTables source = new CloudNoSqlTables(tables);
        PlatformNoSqlTablesResponse result = underTest.convert(source);
        List<PlatformNoSqlTableResponse> noSqlTables = result.getNoSqlTables();
        assertNotNull(noSqlTables);
        assertEquals(tables.size(), noSqlTables.size());
        tables.forEach(t -> {
            assertTrue(noSqlTables.stream().anyMatch(s -> t.getName().equals(s.getName())));
        });
    }
}
