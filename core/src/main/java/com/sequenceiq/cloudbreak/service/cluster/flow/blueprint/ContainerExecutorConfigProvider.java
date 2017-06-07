package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

@Component
public class ContainerExecutorConfigProvider {
    @Inject
    private BlueprintProcessor blueprintProcessor;

    public String addToBlueprint(String blueprint) {
        blueprint = blueprintProcessor.addConfigEntries(blueprint, getConfigs(), true);
        return blueprint;
    }

    private List<BlueprintConfigurationEntry> getConfigs() {
        List<BlueprintConfigurationEntry> bpConfigs = new ArrayList<>();
        bpConfigs.add(new BlueprintConfigurationEntry("yarn-site", "yarn.acl.enable", "true"));
        bpConfigs.add(new BlueprintConfigurationEntry("yarn-site", "yarn.nodemanager.linux-container-executor.nonsecure-mode.local-user",
                "nobody"));
        bpConfigs.add(new BlueprintConfigurationEntry("yarn-site", "yarn.nodemanager.runtime.linux.docker.capabilities",
                "CHOWN,DAC_OVERRIDE,FSETID,FOWNER,MKNOD,NET_RAW,SETGID,SETUID,SETFCAP,SETPCAP,NET_BIND_SERVICE,SYS_CHROOT,KILL,"
                        + "AUDIT_WRITE,DAC_READ_SEARCH,SYS_PTRACE,SYS_ADMIN"));
        bpConfigs.add(new BlueprintConfigurationEntry("yarn-site", "yarn.nodemanager.runtime.linux.docker.default-container-network",
                "host"));
        bpConfigs.add(new BlueprintConfigurationEntry("yarn-site", "yarn.nodemanager.container-executor.class",
                "org.apache.hadoop.yarn.server.nodemanager.LinuxContainerExecutor"));
        bpConfigs.add(new BlueprintConfigurationEntry("yarn-site", "yarn.nodemanager.linux-container-executor.cgroups.mount",
                "false"));
        bpConfigs.add(new BlueprintConfigurationEntry("yarn-site", "yarn.nodemanager.linux-container-executor.cgroups.hierarchy",
                "hadoop-yarn"));
        bpConfigs.add(new BlueprintConfigurationEntry("yarn-site", "yarn.nodemanager.linux-container-executor.cgroups.mount-path",
                "/sys/fs/cgroup"));
        bpConfigs.add(new BlueprintConfigurationEntry("yarn-site", "yarn.nodemanager.linux-container-executor.cgroups.strict-resource-usage",
                "false"));
        bpConfigs.add(new BlueprintConfigurationEntry("yarn-site", "yarn.nodemanager.recovery.enabled", "true"));
        bpConfigs.add(new BlueprintConfigurationEntry("yarn-site", "yarn.nodemanager.recovery.dir",
                "/var/log/hadoop-yarn/nodemanager/recovery-state"));

        bpConfigs.add(new BlueprintConfigurationEntry("yarn-env", "min.user.id", "50"));

        return bpConfigs;
    }


}
