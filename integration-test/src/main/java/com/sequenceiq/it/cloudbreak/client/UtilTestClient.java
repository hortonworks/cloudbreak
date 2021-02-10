package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.util.CheckResourceRightAction;
import com.sequenceiq.it.cloudbreak.action.v4.util.CheckRightAction;
import com.sequenceiq.it.cloudbreak.action.v4.util.CheckRightRawAction;
import com.sequenceiq.it.cloudbreak.action.v4.util.CloudStorageMatrixAction;
import com.sequenceiq.it.cloudbreak.action.v4.util.DeploymentPreferencesAction;
import com.sequenceiq.it.cloudbreak.action.v4.util.RepoConfigValidationAction;
import com.sequenceiq.it.cloudbreak.action.v4.util.SecurityRulesAction;
import com.sequenceiq.it.cloudbreak.action.v4.util.StackMatrixAction;
import com.sequenceiq.it.cloudbreak.action.v4.util.UsedImagesAction;
import com.sequenceiq.it.cloudbreak.action.v4.util.VersionCheckAction;
import com.sequenceiq.it.cloudbreak.dto.RawCloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.securityrule.SecurityRulesTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.CheckResourceRightTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.CheckRightTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.CloudStorageMatrixTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.DeploymentPreferencesTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.RepoConfigValidationTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.StackMatrixTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.UsedImagesTestDto;
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

    public Action<CheckRightTestDto, CloudbreakClient> checkRight() {
        return new CheckRightAction();
    }

    public Action<CheckResourceRightTestDto, CloudbreakClient> checkResourceRight() {
        return new CheckResourceRightAction();
    }

    public Action<RawCloudbreakTestDto, CloudbreakClient> checkRightRaw() {
        return new CheckRightRawAction();
    }

    public Action<UsedImagesTestDto, CloudbreakClient> usedImages() {
        return new UsedImagesAction();
    }

}
