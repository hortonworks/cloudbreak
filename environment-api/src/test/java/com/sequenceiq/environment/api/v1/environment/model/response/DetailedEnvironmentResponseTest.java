package com.sequenceiq.environment.api.v1.environment.model.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sequenceiq.common.api.backup.response.BackupResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.CcmV2TlsType;
import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.yarn.YarnEnvironmentParameters;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponse;

class DetailedEnvironmentResponseTest {

    @Test
    void builderTest() {
        CompactRegionResponse regions = new CompactRegionResponse();
        CredentialResponse credential = new CredentialResponse();
        LocationResponse location = new LocationResponse();
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        TelemetryResponse telemetry = new TelemetryResponse();
        BackupResponse backup = new BackupResponse();
        FreeIpaResponse freeIpa = new FreeIpaResponse();
        EnvironmentAuthenticationResponse authentication = new EnvironmentAuthenticationResponse();
        SecurityAccessResponse securityAccess = new SecurityAccessResponse();
        AwsEnvironmentParameters aws = new AwsEnvironmentParameters();
        AzureEnvironmentParameters azure = new AzureEnvironmentParameters();
        YarnEnvironmentParameters yarn = new YarnEnvironmentParameters();
        TagResponse tag = new TagResponse();
        ProxyResponse proxyConfig = new ProxyResponse();
        GcpEnvironmentParameters gcp = new GcpEnvironmentParameters();
        DataServicesResponse dataServices = new DataServicesResponse();
        String environmentType = EnvironmentType.HYBRID.toString();
        String remoteEnvironmentCrn = "remoteEnvironmentCrn";
        EncryptionProfileResponse encryptionProfile = new EncryptionProfileResponse();

        DetailedEnvironmentResponse response = DetailedEnvironmentResponse.builder()
                .withCrn("crn")
                .withName("name")
                .withOriginalName("originalName")
                .withDescription("description")
                .withRegions(regions)
                .withCloudPlatform("cloudPlatform")
                .withCredential(credential)
                .withLocation(location)
                .withNetwork(network)
                .withTelemetry(telemetry)
                .withBackup(backup)
                .withEnvironmentStatus(EnvironmentStatus.AVAILABLE)
                .withCreateFreeIpa(true)
                .withFreeIpa(freeIpa)
                .withStatusReason("statusReason")
                .withCreated(123L)
                .withAuthentication(authentication)
                .withSecurityAccess(securityAccess)
                .withTunnel(Tunnel.CCMV2_JUMPGATE)
                .withIdBrokerMappingSource(IdBrokerMappingSource.IDBMMS)
                .withCloudStorageValidation(CloudStorageValidation.ENABLED)
                .withAdminGroupName("adminGroupName")
                .withAws(aws)
                .withAzure(azure)
                .withYarn(yarn)
                .withTag(tag)
                .withParentEnvironmentCrn("parentEnvironmentCrn")
                .withParentEnvironmentName("parentEnvironmentName")
                .withParentEnvironmentCloudPlatform("parentEnvironmentCloudPlatform")
                .withProxyConfig(proxyConfig)
                .withGcp(gcp)
                .withEnvironmentServiceVersion("environmentServiceVersion")
                .withAccountId("accountId")
                .withCcmV2TlsType(CcmV2TlsType.TWO_WAY_TLS)
                .withDeletionType(EnvironmentDeletionType.NONE)
                .withEnvironmentDomain("environmentDomain")
                .withDataServices(dataServices)
                .withEnableSecretEncryption(true)
                .withEnableComputeCluster(true)
                .withEnvironmentType(environmentType)
                .withRemoteEnvironmentCrn(remoteEnvironmentCrn)
                .withEncryptionProfile(encryptionProfile)
                .build();

        assertThat(response).isNotNull();
        assertThat(response.getCrn()).isEqualTo("crn");
        assertThat(response.getName()).isEqualTo("name");
        assertThat(response.getOriginalName()).isEqualTo("originalName");
        assertThat(response.getDescription()).isEqualTo("description");
        assertThat(response.getRegions()).isSameAs(regions);
        assertThat(response.getCloudPlatform()).isEqualTo("cloudPlatform");
        assertThat(response.getCredential()).isSameAs(credential);
        assertThat(response.getLocation()).isSameAs(location);
        assertThat(response.getNetwork()).isSameAs(network);
        assertThat(response.getTelemetry()).isSameAs(telemetry);
        assertThat(response.getBackup()).isSameAs(backup);
        assertThat(response.getEnvironmentStatus()).isEqualTo(EnvironmentStatus.AVAILABLE);
        assertThat(response.getCreateFreeIpa()).isTrue();
        assertThat(response.getFreeIpa()).isSameAs(freeIpa);
        assertThat(response.getStatusReason()).isEqualTo("statusReason");
        assertThat(response.getCreated()).isEqualTo(123L);
        assertThat(response.getAuthentication()).isSameAs(authentication);
        assertThat(response.getSecurityAccess()).isSameAs(securityAccess);
        assertThat(response.getTunnel()).isEqualTo(Tunnel.CCMV2_JUMPGATE);
        assertThat(response.getIdBrokerMappingSource()).isEqualTo(IdBrokerMappingSource.IDBMMS);
        assertThat(response.getCloudStorageValidation()).isEqualTo(CloudStorageValidation.ENABLED);
        assertThat(response.getAdminGroupName()).isEqualTo("adminGroupName");
        assertThat(response.getAws()).isSameAs(aws);
        assertThat(response.getAzure()).isSameAs(azure);
        assertThat(response.getYarn()).isSameAs(yarn);
        assertThat(response.getTags()).isSameAs(tag);
        assertThat(response.getParentEnvironmentCrn()).isEqualTo("parentEnvironmentCrn");
        assertThat(response.getParentEnvironmentName()).isEqualTo("parentEnvironmentName");
        assertThat(response.getParentEnvironmentCloudPlatform()).isEqualTo("parentEnvironmentCloudPlatform");
        assertThat(response.getProxyConfig()).isSameAs(proxyConfig);
        assertThat(response.getGcp()).isSameAs(gcp);
        assertThat(response.getEnvironmentServiceVersion()).isEqualTo("environmentServiceVersion");
        assertThat(response.getAccountId()).isEqualTo("accountId");
        assertThat(response.getCcmV2TlsType()).isEqualTo(CcmV2TlsType.TWO_WAY_TLS);
        assertThat(response.getDeletionType()).isEqualTo(EnvironmentDeletionType.NONE);
        assertThat(response.getEnvironmentDomain()).isEqualTo("environmentDomain");
        assertThat(response.getDataServices()).isSameAs(dataServices);
        assertThat(response.isEnableSecretEncryption()).isTrue();
        assertThat(response.isEnableComputeCluster()).isTrue();
        assertThat(response.getEnvironmentType()).isEqualTo(environmentType);
        assertThat(response.getRemoteEnvironmentCrn()).isEqualTo(remoteEnvironmentCrn);
        assertThat(response.getEncryptionProfile()).isEqualTo(encryptionProfile);
    }

}