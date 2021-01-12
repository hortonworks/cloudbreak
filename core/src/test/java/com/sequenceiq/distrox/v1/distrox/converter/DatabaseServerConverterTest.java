package com.sequenceiq.distrox.v1.distrox.converter;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(result.getCrn()).isEqualTo(source.getCrn());
        assertThat(result.getName()).isEqualTo(source.getName());
        assertThat(result.getDescription()).isEqualTo(source.getDescription());
        assertThat(result.getEnvironmentCrn()).isEqualTo(source.getEnvironmentCrn());
        assertThat(result.getHost()).isEqualTo(source.getHost());
        assertThat(result.getPort()).isEqualTo(source.getPort());
        assertThat(result.getDatabaseVendor()).isEqualTo(source.getDatabaseVendor());
        assertThat(result.getDatabaseVendorDisplayName()).isEqualTo(source.getDatabaseVendorDisplayName());
        assertThat(result.getCreationDate()).isEqualTo(source.getCreationDate());
        assertThat(result.getResourceStatus()).isEqualTo(DatabaseServerResourceStatus.SERVICE_MANAGED);
        assertThat(result.getStatus()).isEqualTo(DatabaseServerStatus.AVAILABLE);
        assertThat(result.getStatusReason()).isEqualTo(source.getStatusReason());
        assertThat(result.getClusterCrn()).isEqualTo(source.getClusterCrn());
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