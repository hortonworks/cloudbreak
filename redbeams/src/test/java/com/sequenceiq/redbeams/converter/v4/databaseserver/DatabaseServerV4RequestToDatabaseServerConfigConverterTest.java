package com.sequenceiq.redbeams.converter.v4.databaseserver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerV4Request;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;

public class DatabaseServerV4RequestToDatabaseServerConfigConverterTest {

    private DatabaseServerV4RequestToDatabaseServerConfigConverter converter;

    @BeforeEach
    public void setUp() {
        converter = new DatabaseServerV4RequestToDatabaseServerConfigConverter();
    }

    @Test
    public void testConversion() {
        DatabaseServerV4Request request = new DatabaseServerV4Request();
        request.setName("myserver");
        request.setDescription("mine not yours");
        request.setHost("myserver.db.example.com");
        request.setPort(5432);
        request.setDatabaseVendor("postgres");
        request.setConnectionUserName("root");
        request.setConnectionPassword("cloudera");
        request.setConnectionDriver("org.postgresql.Driver");
        request.setEnvironmentCrn("myenvironment");

        DatabaseServerConfig server = converter.convert(request);

        assertThat(server).isNotNull();
        assertThat(server.getName()).isEqualTo(request.getName());
        assertThat(server.getDescription()).isEqualTo(request.getDescription());
        assertThat(server.getHost()).isEqualTo(request.getHost());
        assertThat(server.getPort()).isEqualTo(request.getPort());
        assertThat(server.getDatabaseVendor().databaseType()).isEqualTo(request.getDatabaseVendor());
        assertThat(server.getConnectionUserName()).isEqualTo(request.getConnectionUserName());
        assertThat(server.getConnectionPassword()).isEqualTo(request.getConnectionPassword());
        assertThat(server.getConnectionDriver()).isEqualTo(request.getConnectionDriver());
        assertThat(server.getEnvironmentId()).isEqualTo(request.getEnvironmentCrn());
        assertThat(server.getResourceStatus()).isEqualTo(ResourceStatus.USER_MANAGED);
    }

}
