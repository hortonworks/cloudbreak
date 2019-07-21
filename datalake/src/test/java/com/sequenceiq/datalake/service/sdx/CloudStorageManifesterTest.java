package com.sequenceiq.datalake.service.sdx;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

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
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.location.StorageLocationV4Request;
import com.sequenceiq.cloudbreak.client.CloudbreakUserCrnClient;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceCrnEndpoints;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceUserCrnClient;
import com.sequenceiq.common.api.cloudstorage.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.filesystem.FileSystemType;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;

@RunWith(MockitoJUnitRunner.class)
public class CloudStorageManifesterTest {

    private static final String USER_CRN = "crn:altus:iam:us-west-1:cloudera:user:bob@cloudera.com";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private CloudbreakServiceUserCrnClient cloudbreakClient;

    @InjectMocks
    private CloudStorageManifester underTest;

    private String exampleBlueprintName = "SDX HA dummy BP";

    @Test
    public void whenInvalidConfigIsProvidedThrowBadRequest() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("instance profile must be defined for S3");
        SdxCluster sdxCluster = new SdxCluster();
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setCloudStorage(new SdxCloudStorageRequest());
        underTest.getCloudStorageConfig("AWS", exampleBlueprintName, sdxCluster, sdxClusterRequest);
    }

    @Test
    public void whenConfigIsProvidedReturnFileSystemParameters() {
        mockFileSystemResponseForCloudbreakClient();
        SdxCluster sdxCluster = new SdxCluster();
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxCluster.setInitiatorUserCrn(USER_CRN);
        sdxCluster.setClusterName("sdx-cluster");
        SdxCloudStorageRequest cloudStorageRequest = new SdxCloudStorageRequest();
        cloudStorageRequest.setBaseLocation("example-path");
        cloudStorageRequest.setFileSystemType(FileSystemType.S3);
        S3CloudStorageV1Parameters s3Params = new S3CloudStorageV1Parameters();
        s3Params.setInstanceProfile("instance:profile");
        cloudStorageRequest.setS3(s3Params);
        sdxClusterRequest.setCloudStorage(cloudStorageRequest);
        CloudStorageV4Request cloudStorageConfigReq = underTest.getCloudStorageConfig("AWS", exampleBlueprintName, sdxCluster, sdxClusterRequest);
        assertEquals(1, cloudStorageConfigReq.getLocations().size());
        StorageLocationV4Request singleRequest = cloudStorageConfigReq.getLocations().iterator().next();
        assertEquals("dummyFile", singleRequest.getPropertyFile());
        assertEquals("dummyPropertyName", singleRequest.getPropertyName());

    }

    private void mockFileSystemResponseForCloudbreakClient() {
        CloudbreakServiceCrnEndpoints mockedEndpoint = mock(CloudbreakServiceCrnEndpoints.class);
        FileSystemV4Endpoint mockedFileSystemV4Endpoint = mock(FileSystemV4Endpoint.class);
        when(mockedEndpoint.filesystemV4Endpoint()).thenReturn(mockedFileSystemV4Endpoint);
        when(cloudbreakClient.withCrn(anyString())).thenReturn(mockedEndpoint);
        FileSystemParameterV4Responses dummyResponses = new FileSystemParameterV4Responses();
        List<FileSystemParameterV4Response> responses = new ArrayList<>();
        FileSystemParameterV4Response resp = new FileSystemParameterV4Response();
        resp.setDefaultPath("ranger/example-path");
        resp.setDescription("Rangerpath");
        resp.setPropertyFile("dummyFile");
        resp.setPropertyName("dummyPropertyName");
        responses.add(resp);
        dummyResponses.setResponses(responses);
        when(mockedFileSystemV4Endpoint
                .getFileSystemParameters(anyLong(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyBoolean(), anyBoolean())).thenReturn(dummyResponses);
    }
}