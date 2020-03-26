package com.sequenceiq.cloudbreak.controller.validation.stack;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRequest;
import com.sequenceiq.cloudbreak.cloud.model.component.AmbariInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.AmbariRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDFEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDPEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDPInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultStackRepoDetails;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.DefaultAmbariRepoService;
import com.sequenceiq.cloudbreak.service.credential.PaywallCredentialService;

@RunWith(MockitoJUnitRunner.class)
public class PaywallCredentialValidatorTest {

    private static final String HDP_STACK = "HDP";

    private static final String HDP_315 = "3.1.5";

    private static final String PROTECTED_URL = "/protected";

    private static final String AMBARI_275 = "2.7.5";

    @InjectMocks
    private PaywallCredentialValidator underTest;

    @Mock
    private PaywallCredentialService paywallCredentialService;

    @Mock
    private DefaultHDPEntries defaultHDPEntries;

    @Mock
    private DefaultHDFEntries defaultHDFEntries;

    @Mock
    private DefaultAmbariRepoService defaultAmbariRepoService;

    @Test
    public void testValidateCredentialShouldNotThrowExceptionWhenTheClusterProtectedAndCredentialsArePresent() {
        AmbariStackDetailsJson stackDetails = createStackDetails(HDP_315, PROTECTED_URL);
        AmbariRepoDetailsJson ambariRepoDetails = createAmbariRepoDetails(AMBARI_275, PROTECTED_URL);
        ClusterRequest clusterRequest = createClusterRequest(stackDetails, ambariRepoDetails);

        when(defaultHDPEntries.getEntries()).thenReturn(Map.of(HDP_315, createStackInfo(HDP_315, PROTECTED_URL, true)));
        when(paywallCredentialService.paywallCredentialAvailable()).thenReturn(true);
        when(defaultAmbariRepoService.getEntries()).thenReturn(Map.of(AMBARI_275, createAmbariInfo(AMBARI_275, PROTECTED_URL, true)));

        underTest.validateCredential(clusterRequest, HDP_STACK, HDP_315);

        verify(defaultHDPEntries).getEntries();
        verify(paywallCredentialService).paywallCredentialAvailable();
        verify(defaultAmbariRepoService).getEntries();
        verifyZeroInteractions(defaultHDFEntries);
    }

    @Test
    public void testValidateCredentialShouldNotThrowExceptionWhenTheClusterProtectedAndCredentialsArePresentAndOnlyTheStackInfoAvailable() {
        ClusterRequest clusterRequest = createClusterRequest(null, null);

        when(defaultHDPEntries.getEntries()).thenReturn(Map.of(HDP_315, createStackInfo(HDP_315, PROTECTED_URL, true)));
        when(paywallCredentialService.paywallCredentialAvailable()).thenReturn(true);

        underTest.validateCredential(clusterRequest, HDP_STACK, HDP_315);

        verify(defaultHDPEntries).getEntries();
        verify(paywallCredentialService).paywallCredentialAvailable();
        verifyZeroInteractions(defaultAmbariRepoService);
        verifyZeroInteractions(defaultHDFEntries);
    }

    @Test(expected = BadRequestException.class)
    public void testValidateCredentialShouldThrowExceptionWhenTheClusterProtectedAndCredentialsAreNotPresent() {
        AmbariStackDetailsJson stackDetails = createStackDetails(HDP_315, PROTECTED_URL);
        AmbariRepoDetailsJson ambariRepoDetails = createAmbariRepoDetails(AMBARI_275, PROTECTED_URL);
        ClusterRequest clusterRequest = createClusterRequest(stackDetails, ambariRepoDetails);

        when(defaultHDPEntries.getEntries()).thenReturn(Map.of(HDP_315, createStackInfo(HDP_315, PROTECTED_URL, true)));
        when(paywallCredentialService.paywallCredentialAvailable()).thenReturn(false);
        when(defaultAmbariRepoService.getEntries()).thenReturn(Map.of(AMBARI_275, createAmbariInfo(AMBARI_275, PROTECTED_URL, true)));

        underTest.validateCredential(clusterRequest, HDP_STACK, HDP_315);

        verify(defaultHDPEntries).getEntries();
        verify(paywallCredentialService).paywallCredentialAvailable();
        verify(defaultAmbariRepoService).getEntries();
        verifyZeroInteractions(defaultHDFEntries);
    }

    @Test(expected = BadRequestException.class)
    public void testValidateCredentialShouldThrowExceptionWhenTheClusterProtectedAndCredentialsAreNotPresentAndOnlyTheStackInfoAvailable() {
        ClusterRequest clusterRequest = createClusterRequest(null, null);

        when(defaultHDPEntries.getEntries()).thenReturn(Map.of(HDP_315, createStackInfo(HDP_315, PROTECTED_URL, true)));
        when(paywallCredentialService.paywallCredentialAvailable()).thenReturn(false);

        underTest.validateCredential(clusterRequest, HDP_STACK, HDP_315);

        verify(defaultHDPEntries).getEntries();
        verify(paywallCredentialService).paywallCredentialAvailable();
        verifyZeroInteractions(defaultAmbariRepoService);
        verifyZeroInteractions(defaultHDFEntries);
    }

    @Test
    public void testValidateCredentialShouldNotThrowExceptionWhenTheClusterNotProtectedAndCredentialsAreNotPresent() {
        AmbariStackDetailsJson stackDetails = createStackDetails(HDP_315, PROTECTED_URL);
        AmbariRepoDetailsJson ambariRepoDetails = createAmbariRepoDetails(AMBARI_275, PROTECTED_URL);
        ClusterRequest clusterRequest = createClusterRequest(stackDetails, ambariRepoDetails);

        when(defaultHDPEntries.getEntries()).thenReturn(Map.of(HDP_315, createStackInfo(HDP_315, PROTECTED_URL, false)));
        when(paywallCredentialService.paywallCredentialAvailable()).thenReturn(false);
        when(defaultAmbariRepoService.getEntries()).thenReturn(Map.of(AMBARI_275, createAmbariInfo(AMBARI_275, PROTECTED_URL, false)));

        underTest.validateCredential(clusterRequest, HDP_STACK, HDP_315);

        verify(defaultHDPEntries).getEntries();
        verify(paywallCredentialService).paywallCredentialAvailable();
        verify(defaultAmbariRepoService).getEntries();
        verifyZeroInteractions(defaultHDFEntries);
    }

    @Test
    public void testValidateCredentialShouldNotThrowExceptionWhenTheClusterNotProtectedAndCredentialsAreNotPresentAndOnlyTheStackInfoAvailable() {
        ClusterRequest clusterRequest = createClusterRequest(null, null);

        when(defaultHDPEntries.getEntries()).thenReturn(Map.of(HDP_315, createStackInfo(HDP_315, PROTECTED_URL, false)));
        when(paywallCredentialService.paywallCredentialAvailable()).thenReturn(false);

        underTest.validateCredential(clusterRequest, HDP_STACK, HDP_315);

        verify(defaultHDPEntries).getEntries();
        verify(paywallCredentialService).paywallCredentialAvailable();
        verifyZeroInteractions(defaultAmbariRepoService);
        verifyZeroInteractions(defaultHDFEntries);
    }

    private AmbariStackDetailsJson createStackDetails(String version, String repoUrl) {
        AmbariStackDetailsJson stackDetails = new AmbariStackDetailsJson();
        stackDetails.setRepositoryVersion(version);
        stackDetails.setVersionDefinitionFileUrl(repoUrl);
        return stackDetails;
    }

    private AmbariRepoDetailsJson createAmbariRepoDetails(String version, String repoUrl) {
        AmbariRepoDetailsJson ambariRepoDetails = new AmbariRepoDetailsJson();
        ambariRepoDetails.setVersion(version);
        ambariRepoDetails.setBaseUrl(repoUrl);
        return ambariRepoDetails;
    }

    private ClusterRequest createClusterRequest(AmbariStackDetailsJson ambariStackDetails, AmbariRepoDetailsJson ambariRepoDetails) {
        ClusterRequest clusterRequest = new ClusterRequest();
        clusterRequest.setAmbariStackDetails(ambariStackDetails);
        clusterRequest.setAmbariRepoDetailsJson(ambariRepoDetails);
        return clusterRequest;
    }

    private DefaultHDPInfo createStackInfo(String version, String url, boolean paywallProtected) {
        DefaultHDPInfo stackInfo = new DefaultHDPInfo();
        stackInfo.setVersion(version);
        stackInfo.setRepo(createRepoDetails(url));
        stackInfo.setPaywallProtected(paywallProtected);
        return stackInfo;
    }

    private AmbariInfo createAmbariInfo(String version, String url, boolean paywallProtected) {
        AmbariInfo stackInfo = new AmbariInfo();
        stackInfo.setVersion(version);
        AmbariRepoDetails ambariRepoDetails = new AmbariRepoDetails();
        ambariRepoDetails.setBaseurl(url);
        stackInfo.setRepo(Map.of("centos7", ambariRepoDetails));
        stackInfo.setPaywallProtected(paywallProtected);
        return stackInfo;
    }

    private DefaultStackRepoDetails createRepoDetails(String url) {
        DefaultStackRepoDetails repoDetails = new DefaultStackRepoDetails();
        repoDetails.setStack(Map.of("centos7", url));
        return repoDetails;
    }
}