package com.sequenceiq.environment.api.v1.environment.model.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sequenceiq.common.api.backup.response.BackupResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.CcmV2TlsType;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialViewResponse;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.yarn.YarnEnvironmentParameters;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyViewResponse;

class SimpleEnvironmentResponseTest {

    @Test
    void builderTest() {
        CompactRegionResponse regions = new CompactRegionResponse();
        CredentialViewResponse credentialResponse = new CredentialViewResponse();
        LocationResponse location = new LocationResponse();
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        TelemetryResponse telemetry = new TelemetryResponse();
        BackupResponse backup = new BackupResponse();
        FreeIpaResponse freeIpa = new FreeIpaResponse();
        AwsEnvironmentParameters aws = new AwsEnvironmentParameters();
        AzureEnvironmentParameters azure = new AzureEnvironmentParameters();
        YarnEnvironmentParameters yarn = new YarnEnvironmentParameters();
        TagResponse tag = new TagResponse();
        ProxyViewResponse proxyConfig = new ProxyViewResponse();
        GcpEnvironmentParameters gcp = new GcpEnvironmentParameters();
        DataServicesResponse dataServices = new DataServicesResponse();

        SimpleEnvironmentResponse response = SimpleEnvironmentResponse.builder()
                .withCrn("crn")
                .withName("name")
                .withOriginalName("originalName")
                .withDescription("description")
                .withRegions(regions)
                .withCloudPlatform("cloudPlatform")
                .withCredentialView(credentialResponse)
                .withLocation(location)
                .withNetwork(network)
                .withTelemetry(telemetry)
                .withBackup(backup)
                .withEnvironmentStatus(EnvironmentStatus.AVAILABLE)
                .withCreateFreeIpa(true)
                .withFreeIpa(freeIpa)
                .withStatusReason("statusReason")
                .withCreated(123L)
                .withTunnel(Tunnel.CCMV2_JUMPGATE)
                .withAdminGroupName("adminGroupName")
                .withAws(aws)
                .withAzure(azure)
                .withYarn(yarn)
                .withTag(tag)
                .withParentEnvironmentName("parentEnvironmentName")
                .withProxyConfig(proxyConfig)
                .withGcp(gcp)
                .withEnvironmentServiceVersion("environmentServiceVersion")
                .withCcmV2TlsType(CcmV2TlsType.TWO_WAY_TLS)
                .withDeletionType(EnvironmentDeletionType.NONE)
                .withEnvironmentDomain("environmentDomain")
                .withDataServices(dataServices)
                .withEnableSecretEncryption(true)
                .withEnvironmentVersion("v2")
                .build();

        assertThat(response).isNotNull();
        assertThat(response.getCrn()).isEqualTo("crn");
        assertThat(response.getName()).isEqualTo("name");
        assertThat(response.getOriginalName()).isEqualTo("originalName");
        assertThat(response.getDescription()).isEqualTo("description");
        assertThat(response.getRegions()).isSameAs(regions);
        assertThat(response.getCloudPlatform()).isEqualTo("cloudPlatform");
        assertThat(response.getCredential()).isSameAs(credentialResponse);
        assertThat(response.getLocation()).isSameAs(location);
        assertThat(response.getNetwork()).isSameAs(network);
        assertThat(response.getTelemetry()).isSameAs(telemetry);
        assertThat(response.getBackup()).isSameAs(backup);
        assertThat(response.getEnvironmentStatus()).isEqualTo(EnvironmentStatus.AVAILABLE);
        assertThat(response.getCreateFreeIpa()).isTrue();
        assertThat(response.getFreeIpa()).isSameAs(freeIpa);
        assertThat(response.getStatusReason()).isEqualTo("statusReason");
        assertThat(response.getCreated()).isEqualTo(123L);
        assertThat(response.getAuthentication()).isNull();
        assertThat(response.getSecurityAccess()).isNull();
        assertThat(response.getTunnel()).isEqualTo(Tunnel.CCMV2_JUMPGATE);
        assertThat(response.getIdBrokerMappingSource()).isNull();
        assertThat(response.getCloudStorageValidation()).isNull();
        assertThat(response.getAdminGroupName()).isEqualTo("adminGroupName");
        assertThat(response.getAws()).isSameAs(aws);
        assertThat(response.getAzure()).isSameAs(azure);
        assertThat(response.getYarn()).isSameAs(yarn);
        assertThat(response.getTags()).isSameAs(tag);
        assertThat(response.getParentEnvironmentCrn()).isNull();
        assertThat(response.getParentEnvironmentName()).isEqualTo("parentEnvironmentName");
        assertThat(response.getParentEnvironmentCloudPlatform()).isNull();
        assertThat(response.getProxyConfig()).isSameAs(proxyConfig);
        assertThat(response.getGcp()).isSameAs(gcp);
        assertThat(response.getEnvironmentServiceVersion()).isEqualTo("environmentServiceVersion");
        assertThat(response.getAccountId()).isNull();
        assertThat(response.getCcmV2TlsType()).isEqualTo(CcmV2TlsType.TWO_WAY_TLS);
        assertThat(response.getDeletionType()).isEqualTo(EnvironmentDeletionType.NONE);
        assertThat(response.getEnvironmentDomain()).isEqualTo("environmentDomain");
        assertThat(response.getDataServices()).isSameAs(dataServices);
        assertThat(response.isEnableSecretEncryption()).isTrue();
        assertThat(response.getEnvironmentVersion()).isEqualTo("v2");
    }

}