package com.sequenceiq.it.cloudbreak.util.ssh;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.dto.AbstractFreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;

@Component
public class SshJUtil {
    @Inject
    private SshJClientActions sshJClientActions;

    private SshJUtil() {
    }

    public <T extends AbstractSdxTestDto> T checkFilesOnHostByNameAndPath(T testDto, List<InstanceGroupV4Response> instanceGroups,
            List<String> hostGroupNames, String filePath, String fileName, long requiredNumberOfFiles, String user, String password) {
        return sshJClientActions.checkFilesByNameAndPath(testDto, instanceGroups, hostGroupNames, filePath, fileName, requiredNumberOfFiles, user,
                password);
    }

    public <T extends AbstractFreeIpaTestDto> T checkFilesOnFreeIpaByNameAndPath(T testDto, String environmentCrn, FreeIpaClient freeipaClient,
            InstanceMetadataType istanceMetadataType, String filePath, String fileName, long requiredNumberOfFiles, String user, String password) {
        return sshJClientActions.checkFilesByNameAndPath(testDto, environmentCrn, freeipaClient, istanceMetadataType, filePath, fileName,
                requiredNumberOfFiles, user, password);
    }

    public void checkAwsMountedDisks(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames) {
        sshJClientActions.checkAwsEphemeralDisksMounted(instanceGroups, hostGroupNames, "ephfs");
    }

    public void checkAzureMountedDisks(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames) {
        sshJClientActions.checkAzureTemporalDisksMounted(instanceGroups, hostGroupNames, "ephfs1");
    }

    public Set<String> getAwsVolumeMountPoints(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames) {
        return sshJClientActions.getAwsEphemeralVolumeMountPoints(instanceGroups, hostGroupNames);
    }

    public DistroXTestDto checkMeteringStatus(DistroXTestDto testDto, List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames) {
        return sshJClientActions.checkMeteringStatus(testDto, instanceGroups, hostGroupNames);
    }

    public <T extends CloudbreakTestDto> T checkCommonMonitoringStatus(T testDto, List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames,
            List<String> verifyMetricNames, List<String> acceptableNokNames) {
        return sshJClientActions.checkMonitoringStatus(testDto, instanceGroups, hostGroupNames, verifyMetricNames, acceptableNokNames);
    }

    public FreeIpaTestDto checkCommonMonitoringStatus(FreeIpaTestDto testDto, String environmentCrn, FreeIpaClient freeipaClient,
            List<String> verifyMetricNames, List<String> acceptableNokNames) {
        return sshJClientActions.checkMonitoringStatus(testDto, environmentCrn, freeipaClient, verifyMetricNames, acceptableNokNames);
    }

    public <T extends CloudbreakTestDto> T checkFilesystemFreeBytesGeneratedMetric(T testDto, List<InstanceGroupV4Response> instanceGroups,
            List<String> hostGroupNames) {
        return sshJClientActions.checkFilesystemFreeBytesGeneratedMetric(testDto, instanceGroups, hostGroupNames);
    }

    public FreeIpaTestDto checkFilesystemFreeBytesGeneratedMetric(FreeIpaTestDto testDto, String environmentCrn, FreeIpaClient freeipaClient) {
        return sshJClientActions.checkFilesystemFreeBytesGeneratedMetric(testDto, environmentCrn, freeipaClient);
    }
}
