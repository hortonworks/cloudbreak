package com.sequenceiq.datalake.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerSslCertificateType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerSslConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerSslMode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateType;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslConfigV4Response;
import com.sequenceiq.redbeams.api.model.common.Status;

class DatabaseServerConverterTest {

    private final DatabaseServerConverter underTest = new DatabaseServerConverter();

    @Test
    public void testConvertResponse() {
        DatabaseServerV4Response source = new DatabaseServerV4Response();
        source.setCrn("databaseCrn");
        source.setName("databaseName");
        source.setDescription("description");
        source.setEnvironmentCrn("envCrn");
        source.setHost("host");
        source.setPort(1234);
        source.setDatabaseVendor("vendor");
        source.setDatabaseVendorDisplayName("vendorName");
        source.setCreationDate(new Date().getTime());
        source.setResourceStatus(ResourceStatus.SERVICE_MANAGED);
        source.setStatus(Status.AVAILABLE);
        source.setStatusReason("Everything is great");
        source.setClusterCrn("clusterCrn");
        SslConfigV4Response sourceSslConfig = new SslConfigV4Response();
        sourceSslConfig.setSslCertificates(Set.of("cert1", "cert2"));
        sourceSslConfig.setSslMode(SslMode.ENABLED);
        sourceSslConfig.setSslCertificateType(SslCertificateType.CLOUD_PROVIDER_OWNED);
        source.setSslConfig(sourceSslConfig);


        StackDatabaseServerResponse result = underTest.convert(source);
        assertEquals(source.getCrn(), result.getCrn());
        assertEquals(source.getName(), result.getName());
        assertEquals(source.getDescription(), result.getDescription());
        assertEquals(source.getEnvironmentCrn(), result.getEnvironmentCrn());
        assertEquals(source.getHost(), result.getHost());
        assertEquals(source.getPort(), result.getPort());
        assertEquals(source.getDatabaseVendor(), result.getDatabaseVendor());
        assertEquals(source.getDatabaseVendorDisplayName(), result.getDatabaseVendorDisplayName());
        assertEquals(source.getCreationDate(), result.getCreationDate());
        assertEquals(DatabaseServerResourceStatus.SERVICE_MANAGED, result.getResourceStatus());
        assertEquals(DatabaseServerStatus.AVAILABLE, result.getStatus());
        assertEquals(source.getStatusReason(), result.getStatusReason());
        assertEquals(source.getClusterCrn(), result.getClusterCrn());
        DatabaseServerSslConfig resultSslConfig = result.getSslConfig();
        assertThat(resultSslConfig.getSslMode()).isEqualTo(DatabaseServerSslMode.ENABLED);
        assertThat(resultSslConfig.getSslCertificateType()).isEqualTo(DatabaseServerSslCertificateType.CLOUD_PROVIDER_OWNED);
        assertThat(resultSslConfig.getSslCertificates()).isEqualTo(sourceSslConfig.getSslCertificates());
    }

    @ParameterizedTest
    @EnumSource(ResourceStatus.class)
    public void testConvertResourceStatus(ResourceStatus resourceStatus) {
        DatabaseServerResourceStatus.valueOf(resourceStatus.name());
    }

    @ParameterizedTest
    @EnumSource(Status.class)
    public void testConvertStatus(Status status) {
        DatabaseServerStatus.valueOf(status.name());
    }

    @ParameterizedTest
    @EnumSource(SslMode.class)
    public void testConvertSslMode(SslMode sslMode) {
        DatabaseServerSslMode.valueOf(sslMode.name());
    }

    @ParameterizedTest
    @EnumSource(SslCertificateType.class)
    public void testConvertSslCertificateType(SslCertificateType sslCertificateType) {
        DatabaseServerSslCertificateType.valueOf(sslCertificateType.name());
    }
}