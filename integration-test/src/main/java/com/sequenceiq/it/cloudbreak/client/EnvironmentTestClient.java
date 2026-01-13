package com.sequenceiq.it.cloudbreak.client;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentAddUserManagedIdentityAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentCascadingDeleteAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentCcmUpgrade;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentChangeAuthenticationAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentChangeCredentialAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentChangeSecurityAccessAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentCreateAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentCreateDefaultExternalizedComputeClusterAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentDeleteAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentDeleteByNameAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentDeleteMultipleByCrnsAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentDeleteMultipleByNamesAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentFinishTrustSetupAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentForceDeleteAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentGetAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentGetByCrnAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentInternalGetAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentListAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentModifyProxyConfigAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentReInitializeDefaultExternalizedComputeClusterAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentRefreshAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentStartAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentStartWithoutDatahubAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentStopAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentTrustSetupAction;
import com.sequenceiq.it.cloudbreak.action.v4.environment.EnvironmentVerticalScaleAction;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTrustSetupDto;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

@Service
public class EnvironmentTestClient {

    public Action<EnvironmentTestDto, EnvironmentClient> create() {
        return new EnvironmentCreateAction();
    }

    public Action<EnvironmentTestDto, EnvironmentClient> describe() {
        return new EnvironmentGetAction();
    }

    public Action<EnvironmentTestDto, EnvironmentClient> describeByCrn() {
        return new EnvironmentGetByCrnAction();
    }

    public Action<EnvironmentTestDto, EnvironmentClient> refresh() {
        return new EnvironmentRefreshAction();
    }

    public Action<EnvironmentTestDto, EnvironmentClient> list() {
        return new EnvironmentListAction();
    }

    public Action<EnvironmentTestDto, EnvironmentClient> delete() {
        return new EnvironmentDeleteAction();
    }

    public Action<EnvironmentTestDto, EnvironmentClient> forceDelete() {
        return new EnvironmentForceDeleteAction();
    }

    public Action<EnvironmentTestDto, EnvironmentClient> cascadingDelete() {
        return new EnvironmentCascadingDeleteAction();
    }

    public Action<EnvironmentTestDto, EnvironmentClient> deleteByName() {
        return new EnvironmentDeleteByNameAction();
    }

    public Action<EnvironmentTestDto, EnvironmentClient> deleteByName(boolean cascading) {
        return new EnvironmentDeleteByNameAction(cascading);
    }

    public Action<EnvironmentTestDto, EnvironmentClient> deleteMultipleByNames(String... envNames) {
        return new EnvironmentDeleteMultipleByNamesAction(Set.of(envNames));
    }

    public Action<EnvironmentTestDto, EnvironmentClient> deleteMultipleByCrns(String... crns) {
        return new EnvironmentDeleteMultipleByCrnsAction(Set.of(crns));
    }

    public Action<EnvironmentTestDto, EnvironmentClient> changeCredential() {
        return new EnvironmentChangeCredentialAction();
    }

    public Action<EnvironmentTestDto, EnvironmentClient> get() {
        return new EnvironmentInternalGetAction();
    }

    public Action<EnvironmentTestDto, EnvironmentClient> stop() {
        return new EnvironmentStopAction();
    }

    public Action<EnvironmentTestDto, EnvironmentClient> verticalScale(String verticalScaleKey) {
        return new EnvironmentVerticalScaleAction(verticalScaleKey);
    }

    public Action<EnvironmentTestDto, EnvironmentClient> start() {
        return new EnvironmentStartAction();
    }

    public Action<EnvironmentTestDto, EnvironmentClient> startWithoutDatahub() {
        return new EnvironmentStartWithoutDatahubAction();
    }

    public Action<EnvironmentTestDto, EnvironmentClient> changeAuthentication() {
        return new EnvironmentChangeAuthenticationAction();
    }

    public Action<EnvironmentTestDto, EnvironmentClient> changeSecurityAccess() {
        return new EnvironmentChangeSecurityAccessAction();
    }

    public Action<EnvironmentTestDto, EnvironmentClient> upgradeCcm() {
        return new EnvironmentCcmUpgrade();
    }

    public Action<EnvironmentTestDto, EnvironmentClient> modifyProxyConfig(String proxyConfigName) {
        return new EnvironmentModifyProxyConfigAction(proxyConfigName);
    }

    public Action<EnvironmentTestDto, EnvironmentClient> createDefaultExternalizedComputeCluster() {
        return new EnvironmentCreateDefaultExternalizedComputeClusterAction();
    }

    public Action<EnvironmentTestDto, EnvironmentClient> reInitializeDefaultExternalizedComputeCluster(boolean privateCluster) {
        return new EnvironmentReInitializeDefaultExternalizedComputeClusterAction(privateCluster);
    }

    public Action<EnvironmentTestDto, EnvironmentClient> addUserManagedIdentity() {
        return new EnvironmentAddUserManagedIdentityAction();
    }

    public Action<EnvironmentTrustSetupDto, EnvironmentClient> setupTrust() {
        return new EnvironmentTrustSetupAction();
    }

    public Action<EnvironmentTestDto, EnvironmentClient> finishTrustSetup() {
        return new EnvironmentFinishTrustSetupAction();
    }
}
