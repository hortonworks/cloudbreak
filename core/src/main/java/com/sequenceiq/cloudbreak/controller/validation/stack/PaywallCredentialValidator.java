package com.sequenceiq.cloudbreak.controller.validation.stack;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRequest;
import com.sequenceiq.cloudbreak.cloud.model.component.AmbariInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDFEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDPEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.StackInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.DefaultAmbariRepoService;
import com.sequenceiq.cloudbreak.service.credential.PaywallCredentialService;

@Component
public class PaywallCredentialValidator {

    @Inject
    private PaywallCredentialService paywallCredentialService;

    @Inject
    private DefaultHDPEntries defaultHDPEntries;

    @Inject
    private DefaultHDFEntries defaultHDFEntries;

    @Inject
    private DefaultAmbariRepoService defaultAmbariRepoService;

    public void validateCredential(ClusterRequest clusterRequest, String stackType, String stackVersion) {
        StackInfo stackInfo = getStackInfo(stackType, stackVersion);
        boolean noPaywallCredentialAvailable = noPaywallCredentialAvailable();
        if (clusterRequest.getAmbariStackDetails() != null && clusterRequest.getAmbariRepoDetailsJson() != null) {
            if ((isStackPaywallProtected(clusterRequest, stackInfo) || isAmbariPaywallProtected(clusterRequest)) && noPaywallCredentialAvailable) {
                throwException();
            }
        } else if (isStackPaywallProtected(clusterRequest, stackInfo) && noPaywallCredentialAvailable) {
            throwException();
        }
    }

    private StackInfo getStackInfo(String stackType, String stackVersion) {
        return StackType.HDP.name().equals(stackType) ? defaultHDPEntries.getEntries().get(stackVersion) : defaultHDFEntries.getEntries().get(stackVersion);
    }

    private boolean noPaywallCredentialAvailable() {
        return !paywallCredentialService.paywallCredentialAvailable();
    }

    private void throwException() {
        throw new BadRequestException("Paywall user name and password is required in order to create the cluster.");
    }

    private boolean isStackPaywallProtected(ClusterRequest clusterRequest, StackInfo stackInfo) {
        AmbariStackDetailsJson stackDetails = clusterRequest.getAmbariStackDetails();
        return stackInfo != null && isDefaultStackRepository(stackInfo, stackDetails) && stackInfo.isPaywallProtected();
    }

    private boolean isDefaultStackRepository(StackInfo hdpInfo, AmbariStackDetailsJson stackDetails) {
        Map<String, String> stack = hdpInfo.getRepo().getStack();
        return stackDetails == null
                || stack.containsValue(stackDetails.getRepositoryVersion()) && stack.containsValue(stackDetails.getVersionDefinitionFileUrl());
    }

    private boolean isAmbariPaywallProtected(ClusterRequest clusterRequest) {
        AmbariInfo ambariRepo = getAmbariRepo(clusterRequest.getAmbariRepoDetailsJson());
        return ambariRepo != null && isDefaultAmbariRepository(ambariRepo, clusterRequest) && ambariRepo.isPaywallProtected();
    }

    private AmbariInfo getAmbariRepo(AmbariRepoDetailsJson repoDetails) {
        return defaultAmbariRepoService.getEntries().values().stream()
                .filter(ambariInfo -> repoDetails.getVersion().equals(ambariInfo.getVersion()))
                .findFirst()
                .orElse(null);
    }

    private boolean isDefaultAmbariRepository(AmbariInfo ambariRepo, ClusterRequest clusterRequest) {
        AmbariRepoDetailsJson repoDetails = clusterRequest.getAmbariRepoDetailsJson();
        return ambariRepo.getRepo().values().stream()
                .anyMatch(ambariRepoDetails -> ambariRepoDetails.getBaseurl().equals(repoDetails.getBaseUrl()));
    }
}
