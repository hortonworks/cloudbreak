package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.util.CloudStorageMatrixAction;
import com.sequenceiq.it.cloudbreak.action.v4.util.DeploymentPreferencesAction;
import com.sequenceiq.it.cloudbreak.action.v4.util.RepoConfigValidationAction;
import com.sequenceiq.it.cloudbreak.action.v4.util.SecurityRulesAction;
import com.sequenceiq.it.cloudbreak.action.v4.util.StackMatrixAction;
import com.sequenceiq.it.cloudbreak.action.v4.util.VersionCheckAction;
import com.sequenceiq.it.cloudbreak.dto.securityrule.SecurityRulesTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.CloudStorageMatrixTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.DeploymentPreferencesTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.RepoConfigValidationTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.StackMatrixTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.VersionCheckTestDto;

@Service
public class UtilTestClient {

    public Action<CloudStorageMatrixTestDto, CloudbreakClient> cloudStorageMatrix() {
        return new CloudStorageMatrixAction();
    }

    public Action<DeploymentPreferencesTestDto, CloudbreakClient> deploymentPreferencesV4() {
        return new DeploymentPreferencesAction();
    }

    public Action<RepoConfigValidationTestDto, CloudbreakClient> repoConfigValidationV4() {
        return new RepoConfigValidationAction();
    }

    public Action<SecurityRulesTestDto, CloudbreakClient> securityRulesV4() {
        return new SecurityRulesAction();
    }

    public Action<StackMatrixTestDto, CloudbreakClient> stackMatrixV4() {
        return new StackMatrixAction();
    }

    public Action<VersionCheckTestDto, CloudbreakClient> versionChecker() {
        return new VersionCheckAction();
    }

}
