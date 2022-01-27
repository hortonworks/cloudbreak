package com.sequenceiq.it.cloudbreak.util.clouderamanager;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.util.clouderamanager.action.ClouderaManagerClientActions;

@Component
public class ClouderaManagerUtil {

    @Inject
    private ClouderaManagerClientActions clouderaManagerClientActions;

    private ClouderaManagerUtil() {
    }

    public SdxInternalTestDto checkClouderaManagerKnoxIDBrokerRoleConfigGroups(SdxInternalTestDto testDto, String user, String password) {
        return clouderaManagerClientActions.checkCmKnoxIDBrokerRoleConfigGroups(testDto, user, password);
    }

    public DistroXTestDto checkClouderaManagerYarnNodemanagerRoleConfigGroups(DistroXTestDto testDto, String user, String password) {
        return clouderaManagerClientActions.checkCmYarnNodemanagerRoleConfigGroups(testDto, user, password);
    }

    public DistroXTestDto checkClouderaManagerYarnNodemanagerRoleConfigGroupsDirect(DistroXTestDto testDto, String user, String password) {
        return clouderaManagerClientActions.checkCmYarnNodemanagerRoleConfigGroupsDirect(testDto, user, password);
    }

    public DistroXTestDto checkClouderaManagerHdfsNamenodeRoleConfigGroups(DistroXTestDto testDto, String user, String password, Set<String> mountPoints) {
        return clouderaManagerClientActions.checkCmHdfsNamenodeRoleConfigGroups(testDto, user, password, mountPoints);
    }

    public DistroXTestDto checkClouderaManagerHdfsDatanodeRoleConfigGroups(DistroXTestDto testDto, String user, String password, Set<String> mountPoints) {
        return clouderaManagerClientActions.checkCmHdfsDatanodeRoleConfigGroups(testDto, user, password, mountPoints);
    }

    public DistroXTestDto checkCmServicesStartedSuccessfully(DistroXTestDto testDto, String user, String password) {
        return clouderaManagerClientActions.checkCmServicesStartedSuccessfully(testDto, user, password);
    }
}
