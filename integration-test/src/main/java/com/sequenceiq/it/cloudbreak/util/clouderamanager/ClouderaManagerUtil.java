package com.sequenceiq.it.cloudbreak.util.clouderamanager;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.util.clouderamanager.action.ClouderaManagerClientActions;

@Component
public class ClouderaManagerUtil {

    @Inject
    private ClouderaManagerClientActions clouderaManagerClientActions;

    private ClouderaManagerUtil() {
    }

    public SdxInternalTestDto checkClouderaManagerKnoxIDBrokerRoleConfigGroups(SdxInternalTestDto testDto, TestContext testContext) {
        return clouderaManagerClientActions.checkCmKnoxIDBrokerRoleConfigGroups(testDto, testContext);
    }

    public SdxInternalTestDto checkConfig(SdxInternalTestDto testDto, TestContext testContext, Map<String, String> expectedConfig) {
        return clouderaManagerClientActions.checkConfig(testDto, testContext, expectedConfig);
    }

    public DistroXTestDto checkConfig(DistroXTestDto testDto, TestContext testContext, Map<String, String> expectedConfig) {
        return clouderaManagerClientActions.checkConfig(testDto, testContext, expectedConfig);
    }

    public DistroXTestDto checkClouderaManagerYarnNodemanagerRoleConfigGroups(DistroXTestDto testDto, TestContext testContext) {
        return clouderaManagerClientActions.checkCmYarnNodemanagerRoleConfigGroups(testDto, testContext);
    }

    public DistroXTestDto checkClouderaManagerYarnNodemanagerRoleConfigGroupsDirect(DistroXTestDto testDto, TestContext testContext) {
        return clouderaManagerClientActions.checkCmYarnNodemanagerRoleConfigGroupsDirect(testDto, testContext);
    }

    public DistroXTestDto checkClouderaManagerHdfsNamenodeRoleConfigGroups(DistroXTestDto testDto, TestContext testContext, Set<String> mountPoints) {
        return clouderaManagerClientActions.checkCmHdfsNamenodeRoleConfigGroups(testDto, testContext, mountPoints);
    }

    public DistroXTestDto checkClouderaManagerHdfsDatanodeRoleConfigGroups(DistroXTestDto testDto, TestContext testContext, Set<String> mountPoints) {
        return clouderaManagerClientActions.checkCmHdfsDatanodeRoleConfigGroups(testDto, testContext, mountPoints);
    }

    public DistroXTestDto checkCmServicesStartedSuccessfully(DistroXTestDto testDto, TestContext testContext) {
        return clouderaManagerClientActions.checkCmServicesStartedSuccessfully(testDto, testContext);
    }
}
