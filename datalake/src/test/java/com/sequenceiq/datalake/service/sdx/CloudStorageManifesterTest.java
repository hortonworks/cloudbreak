package com.sequenceiq.datalake.service.sdx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.FileSystemV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.responses.FileSystemParameterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.responses.FileSystemParameterV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.common.model.CloudStorageCdpService;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.S3GuardRequestParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;

@ExtendWith(MockitoExtension.class)
public class CloudStorageManifesterTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:cloudera:user:bob@cloudera.com";

    private static final String EMAIL = "blah@blah.blah";

    @Mock
    private StorageValidationService storageValidationService;

    @Mock
    private FileSystemV4Endpoint fileSystemV4Endpoint;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @InjectMocks
    private CloudStorageManifester underTest;

    private String exampleBlueprintName = "SDX HA dummy BP";

    @Test
    public void whenConfigIsProvidedReturnFileSystemParameters() {
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        mockFileSystemResponseForCloudbreakClient();
        SdxCluster sdxCluster = new SdxCluster();
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxCluster.setClusterName("sdx-cluster");
        SdxCloudStorageRequest cloudStorageRequest = new SdxCloudStorageRequest();
        cloudStorageRequest.setBaseLocation("s3a://example-path");
        cloudStorageRequest.setFileSystemType(FileSystemType.S3);
        S3CloudStorageV1Parameters s3Params = new S3CloudStorageV1Parameters();
        s3Params.setInstanceProfile("instance:profile");
        cloudStorageRequest.setS3(s3Params);
        sdxClusterRequest.setCloudStorage(cloudStorageRequest);
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("AWS");
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        clusterV4Request.setBlueprintName(exampleBlueprintName);
        CloudStorageRequest cloudStorageConfigReq = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.initCloudStorageRequest(environment, clusterV4Request, sdxCluster, sdxClusterRequest));
        StorageLocationBase singleRequest = cloudStorageConfigReq.getLocations().iterator().next();

        assertEquals(1, cloudStorageConfigReq.getIdentities().size());
        assertEquals(CloudIdentityType.ID_BROKER, cloudStorageConfigReq.getIdentities().iterator().next().getType());
        assertEquals(1, cloudStorageConfigReq.getLocations().size());
        assertEquals(CloudStorageCdpService.RANGER_AUDIT, singleRequest.getType());
        assertEquals("ranger/example-path", singleRequest.getValue());
        verify(storageValidationService).validateCloudStorage("AWS", cloudStorageRequest);
    }

    @Test
    public void whenEnvironmentHasLoggingEnabledThenShouldApplyAsLogIdentity() {
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        mockFileSystemResponseForCloudbreakClient();
        SdxCluster sdxCluster = new SdxCluster();
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxCluster.setClusterName("sdx-cluster");
        SdxCloudStorageRequest cloudStorageRequest = new SdxCloudStorageRequest();
        cloudStorageRequest.setBaseLocation("s3a://example-path");
        cloudStorageRequest.setFileSystemType(FileSystemType.S3);
        S3CloudStorageV1Parameters s3Params = new S3CloudStorageV1Parameters();
        s3Params.setInstanceProfile("instance:profile");
        cloudStorageRequest.setS3(s3Params);
        sdxClusterRequest.setCloudStorage(cloudStorageRequest);
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("AWS");
        TelemetryResponse telemetryResponse = new TelemetryResponse();
        LoggingResponse loggingResponse = new LoggingResponse();
        S3CloudStorageV1Parameters s3CloudStorageV1Parameters = new S3CloudStorageV1Parameters();
        s3CloudStorageV1Parameters.setInstanceProfile("logprofile");
        loggingResponse.setS3(s3CloudStorageV1Parameters);
        telemetryResponse.setLogging(loggingResponse);
        AwsEnvironmentParameters awsEnvironmentParameters = new AwsEnvironmentParameters();
        S3GuardRequestParameters s3GuardRequestParameters = new S3GuardRequestParameters();
        s3GuardRequestParameters.setDynamoDbTableName("table");
        awsEnvironmentParameters.setS3guard(s3GuardRequestParameters);
        environment.setAws(awsEnvironmentParameters);
        environment.setTelemetry(telemetryResponse);
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        clusterV4Request.setBlueprintName(exampleBlueprintName);
        CloudStorageRequest cloudStorageConfigReq = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.initCloudStorageRequest(environment, clusterV4Request, sdxCluster, sdxClusterRequest));
        StorageLocationBase singleRequest = cloudStorageConfigReq.getLocations().iterator().next();

        assertEquals(2, cloudStorageConfigReq.getIdentities().size());
        assertEquals(1, cloudStorageConfigReq.getIdentities()
                .stream()
                .filter(r -> r.getType().equals(CloudIdentityType.ID_BROKER))
                .collect(Collectors.toSet()).size());
        assertEquals(1, cloudStorageConfigReq.getIdentities()
                .stream()
                .filter(r -> r.getType().equals(CloudIdentityType.LOG))
                .collect(Collectors.toSet()).size());
        assertEquals("table", cloudStorageConfigReq.getAws().getS3Guard().getDynamoTableName());
        assertEquals(1, cloudStorageConfigReq.getLocations().size());
        assertEquals(CloudStorageCdpService.RANGER_AUDIT, singleRequest.getType());
        assertEquals("ranger/example-path", singleRequest.getValue());
        verify(storageValidationService).validateCloudStorage("AWS", cloudStorageRequest);
    }

    @Test
    public void whenEnvironmentHasOnlyLoggingEnabledThenShouldApplyAsLogIdentity() {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("AWS");
        TelemetryResponse telemetryResponse = new TelemetryResponse();
        LoggingResponse loggingResponse = new LoggingResponse();
        S3CloudStorageV1Parameters s3CloudStorageV1Parameters = new S3CloudStorageV1Parameters();
        s3CloudStorageV1Parameters.setInstanceProfile("logprofile");
        loggingResponse.setS3(s3CloudStorageV1Parameters);
        telemetryResponse.setLogging(loggingResponse);
        environment.setTelemetry(telemetryResponse);
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        clusterV4Request.setBlueprintName(exampleBlueprintName);
        CloudStorageRequest cloudStorageConfigReq = underTest.initCloudStorageRequest(environment, clusterV4Request, null, new SdxClusterRequest());

        assertEquals(1, cloudStorageConfigReq.getIdentities().size());
        assertEquals(1, cloudStorageConfigReq.getIdentities()
                .stream()
                .filter(r -> r.getType().equals(CloudIdentityType.LOG))
                .collect(Collectors.toSet()).size());
        assertEquals("logprofile", cloudStorageConfigReq.getIdentities().get(0).getS3().getInstanceProfile());
    }

    @Test
    public void whenEnvironmentHasLoggingEnabledThenShouldApplyAsLogIdentityForGCS() {
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        mockFileSystemResponseForCloudbreakClient();
        SdxCluster sdxCluster = new SdxCluster();
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxCluster.setClusterName("sdx-cluster");
        SdxCloudStorageRequest cloudStorageRequest = new SdxCloudStorageRequest();
        cloudStorageRequest.setBaseLocation("gs://example-path");
        cloudStorageRequest.setFileSystemType(FileSystemType.GCS);
        GcsCloudStorageV1Parameters gcsCloudStorageV1Parameters = new GcsCloudStorageV1Parameters();
        gcsCloudStorageV1Parameters.setServiceAccountEmail(EMAIL);
        cloudStorageRequest.setGcs(gcsCloudStorageV1Parameters);
        sdxClusterRequest.setCloudStorage(cloudStorageRequest);
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("GCP");
        TelemetryResponse telemetryResponse = new TelemetryResponse();
        LoggingResponse loggingResponse = new LoggingResponse();
        loggingResponse.setGcs(gcsCloudStorageV1Parameters);
        telemetryResponse.setLogging(loggingResponse);
        GcpEnvironmentParameters gcpEnvironmentParameters = GcpEnvironmentParameters.builder().build();
        environment.setGcp(gcpEnvironmentParameters);
        environment.setTelemetry(telemetryResponse);
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        clusterV4Request.setBlueprintName(exampleBlueprintName);
        CloudStorageRequest cloudStorageConfigReq = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.initCloudStorageRequest(environment, clusterV4Request, sdxCluster, sdxClusterRequest));
        StorageLocationBase singleRequest = cloudStorageConfigReq.getLocations().iterator().next();

        assertEquals(2, cloudStorageConfigReq.getIdentities().size());
        assertEquals(1, cloudStorageConfigReq.getIdentities()
                .stream()
                .filter(r -> r.getType().equals(CloudIdentityType.ID_BROKER))
                .collect(Collectors.toSet()).size());
        assertEquals(1, cloudStorageConfigReq.getIdentities()
                .stream()
                .filter(r -> r.getType().equals(CloudIdentityType.LOG))
                .collect(Collectors.toSet()).size());
        assertEquals(2, cloudStorageConfigReq.getIdentities()
                .stream()
                .filter(r -> r.getGcs().getServiceAccountEmail().equals(EMAIL))
                .collect(Collectors.toSet()).size());
        assertEquals(1, cloudStorageConfigReq.getLocations().size());
        assertEquals(CloudStorageCdpService.RANGER_AUDIT, singleRequest.getType());
        assertEquals("ranger/example-path", singleRequest.getValue());
        verify(storageValidationService).validateCloudStorage("GCP", cloudStorageRequest);
    }

    @Test
    public void whenEnvironmentHasOnlyLoggingEnabledThenShouldApplyAsLogIdentityForGCS() {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("GCP");
        TelemetryResponse telemetryResponse = new TelemetryResponse();
        LoggingResponse loggingResponse = new LoggingResponse();
        GcsCloudStorageV1Parameters gcsCloudStorageV1Parameters = new GcsCloudStorageV1Parameters();
        gcsCloudStorageV1Parameters.setServiceAccountEmail(EMAIL);
        loggingResponse.setGcs(gcsCloudStorageV1Parameters);
        telemetryResponse.setLogging(loggingResponse);
        environment.setTelemetry(telemetryResponse);
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        clusterV4Request.setBlueprintName(exampleBlueprintName);
        CloudStorageRequest cloudStorageConfigReq = underTest.initCloudStorageRequest(environment, clusterV4Request, null, new SdxClusterRequest());

        assertEquals(1, cloudStorageConfigReq.getIdentities().size());
        assertEquals(1, cloudStorageConfigReq.getIdentities()
                .stream()
                .filter(r -> r.getType().equals(CloudIdentityType.LOG))
                .collect(Collectors.toSet()).size());
        assertEquals(EMAIL, cloudStorageConfigReq.getIdentities().get(0).getGcs().getServiceAccountEmail());
    }

    @Test
    public void whenCloudStorageAndLoggingDisabled() {
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        clusterV4Request.setBlueprintName(exampleBlueprintName);
        CloudStorageRequest cloudStorageConfigReq = underTest.initCloudStorageRequest(
                new DetailedEnvironmentResponse(), clusterV4Request, null, new SdxClusterRequest());
        assertNull(cloudStorageConfigReq);
    }

    @Test
    public void whenCloudStorageEnabledFromInternalRequest() {
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        clusterV4Request.setBlueprintName(exampleBlueprintName);
        CloudStorageRequest cloudStorageRequest = new CloudStorageRequest();
        StorageLocationBase storageLocationBase = new StorageLocationBase();
        storageLocationBase.setType(CloudStorageCdpService.RANGER_AUDIT);
        storageLocationBase.setValue("s3a://ranger-audit");
        cloudStorageRequest.setLocations(List.of(storageLocationBase));
        clusterV4Request.setCloudStorage(cloudStorageRequest);
        CloudStorageRequest cloudStorageConfigReq = underTest.initCloudStorageRequest(
                new DetailedEnvironmentResponse(), clusterV4Request, null, new SdxClusterRequest());
        assertEquals(CloudStorageCdpService.RANGER_AUDIT, cloudStorageConfigReq.getLocations().get(0).getType());
    }

    @Test
    public void whenCloudStorageEnabledFromInternalRequestWithLogging() {
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        clusterV4Request.setBlueprintName(exampleBlueprintName);
        CloudStorageRequest cloudStorageRequest = new CloudStorageRequest();
        StorageLocationBase storageLocationBase = new StorageLocationBase();
        storageLocationBase.setType(CloudStorageCdpService.RANGER_AUDIT);
        storageLocationBase.setValue("s3a://ranger-audit");
        cloudStorageRequest.setLocations(List.of(storageLocationBase));
        clusterV4Request.setCloudStorage(cloudStorageRequest);
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("AWS");
        TelemetryResponse telemetryResponse = new TelemetryResponse();
        LoggingResponse loggingResponse = new LoggingResponse();
        S3CloudStorageV1Parameters s3CloudStorageV1Parameters = new S3CloudStorageV1Parameters();
        s3CloudStorageV1Parameters.setInstanceProfile("logprofile");
        loggingResponse.setS3(s3CloudStorageV1Parameters);
        telemetryResponse.setLogging(loggingResponse);
        environment.setTelemetry(telemetryResponse);
        CloudStorageRequest cloudStorageConfigReq = underTest.initCloudStorageRequest(
                environment, clusterV4Request, null, new SdxClusterRequest());
        assertEquals(CloudStorageCdpService.RANGER_AUDIT, cloudStorageConfigReq.getLocations().get(0).getType());
        assertEquals(CloudIdentityType.LOG, cloudStorageConfigReq.getIdentities().get(0).getType());
    }

    private void mockFileSystemResponseForCloudbreakClient() {
        FileSystemParameterV4Responses dummyResponses = new FileSystemParameterV4Responses();
        List<FileSystemParameterV4Response> responses = new ArrayList<>();
        FileSystemParameterV4Response resp = new FileSystemParameterV4Response();
        resp.setType(CloudStorageCdpService.RANGER_AUDIT.name());
        resp.setDefaultPath("ranger/example-path");
        resp.setDescription("Rangerpath");
        resp.setPropertyFile("dummyFile");
        resp.setPropertyName("dummyPropertyName");
        responses.add(resp);
        dummyResponses.setResponses(responses);
        when(fileSystemV4Endpoint.getFileSystemParametersInternal(anyLong(), anyString(), anyString(), anyString(),
                        anyString(), anyString(), anyBoolean(), anyBoolean(), anyString())).thenReturn(dummyResponses);
    }

    @Test
    public void stripWhitespacesWhenBaseLocationHasWhiteSpaces() {
        String baseLocationWithWhiteSpaces = "s3a://cloudbreak bucket/something ";
        SdxCloudStorageRequest cloudStorageRequest = new SdxCloudStorageRequest();
        cloudStorageRequest.setBaseLocation(baseLocationWithWhiteSpaces);
        S3CloudStorageV1Parameters params = new S3CloudStorageV1Parameters();
        params.setInstanceProfile("instanceProfile");
        cloudStorageRequest.setS3(params);

        underTest.normalizeCloudStorageRequest(cloudStorageRequest);
        assertEquals("s3a://cloudbreak bucket/something", cloudStorageRequest.getBaseLocation());
    }
}
