package com.sequenceiq.datalake.service.sdx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.FileSystemV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.responses.FileSystemParameterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.responses.FileSystemParameterV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.common.model.CloudStorageCdpService;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.S3GuardRequestParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;

@RunWith(MockitoJUnitRunner.class)
public class CloudStorageManifesterTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:cloudera:user:bob@cloudera.com";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private FileSystemV4Endpoint fileSystemV4Endpoint;

    @InjectMocks
    private CloudStorageManifester underTest;

    private String exampleBlueprintName = "SDX HA dummy BP";

    @Test
    public void whenInvalidConfigIsProvidedThrowBadRequest() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("instance profile must be defined for S3");
        SdxCluster sdxCluster = new SdxCluster();
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        SdxCloudStorageRequest sdxCloudStorageRequest = new SdxCloudStorageRequest();
        sdxCloudStorageRequest.setBaseLocation("s3a://example-path");
        sdxClusterRequest.setCloudStorage(sdxCloudStorageRequest);
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        clusterV4Request.setBlueprintName(exampleBlueprintName);
        environment.setCloudPlatform("AWS");
        underTest.initCloudStorageRequest(environment, clusterV4Request, sdxCluster, sdxClusterRequest);
    }

    @Test
    public void whenConfigIsProvidedReturnFileSystemParameters() {
        mockFileSystemResponseForCloudbreakClient();
        SdxCluster sdxCluster = new SdxCluster();
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxCluster.setInitiatorUserCrn(USER_CRN);
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
        CloudStorageRequest cloudStorageConfigReq = underTest.initCloudStorageRequest(environment, clusterV4Request, sdxCluster, sdxClusterRequest);
        StorageLocationBase singleRequest = cloudStorageConfigReq.getLocations().iterator().next();

        assertEquals(1, cloudStorageConfigReq.getIdentities().size());
        assertEquals(CloudIdentityType.ID_BROKER, cloudStorageConfigReq.getIdentities().iterator().next().getType());
        assertEquals(1, cloudStorageConfigReq.getLocations().size());
        assertEquals(CloudStorageCdpService.RANGER_AUDIT, singleRequest.getType());
        assertEquals("ranger/example-path", singleRequest.getValue());
    }

    @Test
    public void whenEnvironmentHasLoggingEnabledThenShouldApplyAsLogIdentity() {
        mockFileSystemResponseForCloudbreakClient();
        SdxCluster sdxCluster = new SdxCluster();
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxCluster.setInitiatorUserCrn(USER_CRN);
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
        CloudStorageRequest cloudStorageConfigReq = underTest.initCloudStorageRequest(environment, clusterV4Request, sdxCluster, sdxClusterRequest);
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
        when(fileSystemV4Endpoint
                .getFileSystemParameters(anyLong(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyBoolean(), anyBoolean())).thenReturn(dummyResponses);
    }

    @Test
    public void throwErrorWhenS3LocationInvalid() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("AWS baselocation missing protocol. please specify s3a://");
        SdxCloudStorageRequest cloudStorageRequest = new SdxCloudStorageRequest();
        cloudStorageRequest.setBaseLocation("cloudbreakbucket/something");
        S3CloudStorageV1Parameters params = new S3CloudStorageV1Parameters();
        params.setInstanceProfile("instanceProfile");
        cloudStorageRequest.setS3(params);
        underTest.validateCloudStorage(CloudPlatform.AWS.toString(), cloudStorageRequest);
    }

    @Test
    public void okWhenS3LocationIsValid() {
        SdxCloudStorageRequest cloudStorageRequest = new SdxCloudStorageRequest();
        cloudStorageRequest.setBaseLocation("s3a://cloudbreakbucket/something");
        S3CloudStorageV1Parameters params = new S3CloudStorageV1Parameters();
        params.setInstanceProfile("instanceProfile");
        cloudStorageRequest.setS3(params);
        underTest.validateCloudStorage(CloudPlatform.AWS.toString(), cloudStorageRequest);
    }
}
