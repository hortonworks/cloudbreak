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
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getByAccountIdAndEnvName() {
        SdxCluster sdxCluser = new SdxCluster();
        sdxCluser.setStatus(SdxClusterStatus.REQUESTED);
        List<SdxCluster> sdxList = new ArrayList<>();
        sdxList.add(sdxCluser);
        when(sdxClusterRepository.findByAccountIdAndEnvName(anyString(), anyString())).thenReturn(sdxList);
        SdxCluster returnedSdxCluster = sdxService.getByAccountIdAndEnvName(CRN, "env");
        Assertions.assertEquals(sdxCluser, returnedSdxCluster);
    }

    @Test
    public void getByAccountIdAndEnvNameNotFound() {
        when(sdxClusterRepository.findByAccountIdAndEnvName(anyString(), anyString())).thenReturn(new ArrayList<>());
        Assertions.assertThrows(NotFoundException.class, () -> sdxService.getByAccountIdAndEnvName(CRN, "env"), "Sdx cluster not found");
    }

    @Test
    public void updateSdxStatus() {
        long id = 1L;
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(id);
        when(sdxClusterRepository.findById(id)).thenReturn(Optional.of(sdxCluster));
        sdxService.updateSdxStatus(id, SdxClusterStatus.PROVISIONING_FAILED);
        verify(sdxClusterRepository, times(1)).save(sdxCluster);
    }

    @Test
    public void createSdxIfExists() {
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setAccessCidr("0.0.0.0/0");
        sdxClusterRequest.setClusterShape("big");
        Map<String, String> tags = new HashMap<>();
        tags.put("mytag", "tagecske");
        sdxClusterRequest.setTags(tags);
        when(sdxClusterRepository.findByAccountIdAndEnvName(anyString(), anyString())).thenReturn(Collections.singletonList(new SdxCluster()));
        Assertions.assertThrows(BadRequestException.class,
                () -> sdxService.createSdx(CRN, "envir", sdxClusterRequest), "SDX cluster exists for environment name");
    }

    @Test
    public void createSdx() {
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setAccessCidr("0.0.0.0/0");
        sdxClusterRequest.setClusterShape("big");
        Map<String, String> tags = new HashMap<>();
        tags.put("mytag", "tagecske");
        sdxClusterRequest.setTags(tags);
        when(sdxClusterRepository.findByAccountIdAndEnvName(anyString(), anyString())).thenReturn(new ArrayList<>());
        long id = 10L;
        when(sdxClusterRepository.save(any(SdxCluster.class))).thenAnswer(invocation -> {
            SdxCluster sdxWithId = invocation.getArgument(0, SdxCluster.class);
            sdxWithId.setId(id);
            return sdxWithId;
        });
        SdxCluster createdSdxCluster = sdxService.createSdx(CRN, "envir", sdxClusterRequest);
        Assertions.assertEquals(id, createdSdxCluster.getId());
        final ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        SdxCluster capturedSdx = captor.getValue();
        Assertions.assertEquals("0.0.0.0/0", capturedSdx.getAccessCidr());
        Assertions.assertEquals("tagecske", capturedSdx.getTags().getValue("mytag"));
        Assertions.assertEquals("envir-sdx-cluster", capturedSdx.getClusterName());
        Assertions.assertEquals("big", capturedSdx.getClusterShape());
        Assertions.assertEquals("envir", capturedSdx.getEnvName());
        Assertions.assertEquals("hortonworks", capturedSdx.getAccountId());
        Assertions.assertEquals(CRN, capturedSdx.getInitiatorUserCrn());
        Assertions.assertEquals(SdxClusterStatus.REQUESTED, capturedSdx.getStatus());
        verify(sdxReactorFlowManager).triggerSdxCreation(id);
    }

    @Test
    public void listSdx() {
        ArrayList<SdxCluster> sdxClusters = new ArrayList<>();
        sdxClusters.add(new SdxCluster());
        sdxClusters.add(new SdxCluster());
        when(sdxClusterRepository.findByAccountIdAndEnvName(eq("hortonworks"), eq("envir"))).thenReturn(sdxClusters);
        List<SdxCluster> sdxList = sdxService.listSdx(CRN, "envir");
        Assertions.assertEquals(2, sdxList.size());
    }

    @Test
    public void deleteSdxNotFound() {
        Assertions.assertThrows(BadRequestException.class, () -> sdxService.deleteSdx(CRN, "envir"), "Can not find sdx cluster");
        verify(sdxClusterRepository, times(1))
                .findByAccountIdAndClusterNameAndEnvName(eq("hortonworks"), eq("envir-sdx-cluster"), eq("envir"));
    }
}
