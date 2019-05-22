package com.sequenceiq.datalake.service.sdx;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.datalake.api.endpoint.sdx.SdxClusterRequest;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxClusterStatus;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.repository.SdxClusterRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SDX service tests")
public class SdxServiceTest {

    public static final String CRN = "crn:altus:iam:us-west-1:hortonworks:user:perdos@hortonworks.com";

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @InjectMocks
    private SdxService sdxService;

    @BeforeEach
    void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void getByAccountIdAndEnvName() {
        String clusterName = "test-sdx-cluster";
        SdxCluster sdxCluser = new SdxCluster();
        sdxCluser.setStatus(SdxClusterStatus.REQUESTED);
        sdxCluser.setEnvName("env");
        sdxCluser.setClusterName(clusterName);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(eq("hortonworks"), eq(clusterName))).thenReturn(Optional.of(sdxCluser));
        SdxCluster returnedSdxCluster = sdxService.getByAccountIdAndSdxName(CRN, clusterName);
        Assertions.assertEquals(sdxCluser, returnedSdxCluster);
    }

    @Test
    void getByAccountIdAndEnvNameNotFound() {
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(anyString(), anyString())).thenReturn(Optional.empty());
        Assertions.assertThrows(NotFoundException.class, () -> sdxService.getByAccountIdAndSdxName(CRN, "env"), "Sdx cluster not found");
    }

    @Test
    void updateSdxStatus() {
        long id = 1L;
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(id);
        when(sdxClusterRepository.findById(id)).thenReturn(Optional.of(sdxCluster));
        sdxService.updateSdxStatus(id, SdxClusterStatus.PROVISIONING_FAILED);
        verify(sdxClusterRepository, times(1)).save(sdxCluster);
    }

    @Test
    void createSdxIfExists() {
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setAccessCidr("0.0.0.0/0");
        sdxClusterRequest.setClusterShape("big");
        sdxClusterRequest.setEnvironment("envir");
        Map<String, String> tags = new HashMap<>();
        tags.put("mytag", "tagecske");
        sdxClusterRequest.setTags(tags);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(anyString(), anyString())).thenReturn(Collections.singletonList(new SdxCluster()));
        Assertions.assertThrows(BadRequestException.class,
                () -> sdxService.createSdx(CRN, "test-sdx-cluster", sdxClusterRequest), "SDX cluster exists for environment name");
    }

    @Test
    void createSdx() {
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setAccessCidr("0.0.0.0/0");
        sdxClusterRequest.setClusterShape("big");
        Map<String, String> tags = new HashMap<>();
        tags.put("mytag", "tagecske");
        sdxClusterRequest.setTags(tags);
        sdxClusterRequest.setEnvironment("envir");
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(anyString(), anyString())).thenReturn(new ArrayList<>());
        long id = 10L;
        when(sdxClusterRepository.save(any(SdxCluster.class))).thenAnswer(invocation -> {
            SdxCluster sdxWithId = invocation.getArgument(0, SdxCluster.class);
            sdxWithId.setId(id);
            return sdxWithId;
        });
        String sdxName = "test-sdx-cluster";
        SdxCluster createdSdxCluster = sdxService.createSdx(CRN, sdxName, sdxClusterRequest);
        Assertions.assertEquals(id, createdSdxCluster.getId());
        final ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        SdxCluster capturedSdx = captor.getValue();
        Assertions.assertEquals("0.0.0.0/0", capturedSdx.getAccessCidr());
        Assertions.assertEquals("tagecske", capturedSdx.getTags().getValue("mytag"));
        Assertions.assertEquals(sdxName, capturedSdx.getClusterName());
        Assertions.assertEquals("big", capturedSdx.getClusterShape());
        Assertions.assertEquals("envir", capturedSdx.getEnvName());
        Assertions.assertEquals("hortonworks", capturedSdx.getAccountId());
        Assertions.assertEquals(CRN, capturedSdx.getInitiatorUserCrn());
        Assertions.assertEquals(SdxClusterStatus.REQUESTED, capturedSdx.getStatus());
        verify(sdxReactorFlowManager).triggerSdxCreation(id);
    }

    @Test
    void listSdx() {
        ArrayList<SdxCluster> sdxClusters = new ArrayList<>();
        sdxClusters.add(new SdxCluster());
        sdxClusters.add(new SdxCluster());
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(eq("hortonworks"), eq("envir"))).thenReturn(sdxClusters);
        List<SdxCluster> sdxList = sdxService.listSdx(CRN, "envir");
        Assertions.assertEquals(2, sdxList.size());
    }

    @Test
    void listSdxInvalidCrn() {
        String crn = "crsdfadsfdsf sadasf3-df81ae585e10";
        Assertions.assertThrows(BadRequestException.class, () -> sdxService.listSdx(crn, "envir"));
    }

    @Test
    void deleteSdxNotFound() {
        Assertions.assertThrows(BadRequestException.class, () -> sdxService.deleteSdx(CRN, "test-sdx-cluster"), "Can not find sdx cluster");
        verify(sdxClusterRepository, times(1))
                .findByAccountIdAndClusterNameAndDeletedIsNull(eq("hortonworks"), eq("test-sdx-cluster"));
    }

    @Test
    void deleteSdx() {
        SdxCluster sdxCluster = new SdxCluster();
        long sdxId = 2L;
        sdxCluster.setId(sdxId);
        sdxCluster.setClusterName("sdx-cluster-name");
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        sdxService.deleteSdx(CRN, "envir");
        verify(sdxReactorFlowManager, times(1)).triggerSdxDeletion(sdxId);
        final ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        SdxClusterStatus sdxClusterStatus = captor.getValue().getStatus();
        Assertions.assertEquals(SdxClusterStatus.DELETE_REQUESTED, sdxClusterStatus);
    }
}
