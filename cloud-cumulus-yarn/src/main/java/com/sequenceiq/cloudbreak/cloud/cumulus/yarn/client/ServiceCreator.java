package com.sequenceiq.cloudbreak.cloud.cumulus.yarn.client;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cb.yarn.service.api.records.Artifact;
import org.apache.cb.yarn.service.api.records.Component;
import org.apache.cb.yarn.service.api.records.Component.RestartPolicyEnum;
import org.apache.cb.yarn.service.api.records.ConfigFile;
import org.apache.cb.yarn.service.api.records.ConfigFile.TypeEnum;
import org.apache.cb.yarn.service.api.records.ModelConfiguration;
import org.apache.cb.yarn.service.api.records.Resource;
import org.apache.cb.yarn.service.api.records.Service;
import org.springframework.beans.factory.annotation.Value;

import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.cumulus.yarn.CumulusYarnConstants;
import com.sequenceiq.cloudbreak.cloud.cumulus.yarn.util.CumulusYarnResourceNameHelper;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;

@org.springframework.stereotype.Component
public class ServiceCreator {
    private static final int DEFAULT_CPU_COUNT = 4;

    private static final int DEFAULT_MEM_SIZE_IN_MB = 8192;

    private static final int MB_TO_KB = 1024;

    @Value("${cb.yarn.defaultQueue}")
    private String defaultQueue;

    @Value("${cb.yarn.defaultLifeTime:}")
    private Integer defaultLifeTime;

    @Inject
    private CumulusYarnResourceNameHelper cumulusYarnResourceNameHelper;

    public Service create(AuthenticatedContext ac, CloudStack stack) {
        Service service = new Service();
        service.setName(cumulusYarnResourceNameHelper.createApplicationName(ac));
        service.setQueue(stack.getParameters().getOrDefault(CumulusYarnConstants.CUMULUS_YARN_QUEUE_PARAMETER, defaultQueue));
        service.setVersion("1.0.0");

        String lifeTimeStr = stack.getParameters().get(CumulusYarnConstants.CUMULUS_YARN_LIFETIME_PARAMETER);
        service.setLifetime(lifeTimeStr != null ? Long.parseLong(lifeTimeStr) : defaultLifeTime.longValue());

        Artifact artifact = createArtifact(stack);

        List<Component> components = stack.getGroups().stream().map(group -> mapGroupToYarnComponent(group, stack, artifact)).collect(Collectors.toList());
        service.setComponents(components);

        return service;
    }

    public Artifact createArtifact(CloudStack stack) {
        Artifact artifact = new Artifact();
        artifact.setId(stack.getImage().getImageName());
        artifact.setType(Artifact.TypeEnum.DOCKER);
        return artifact;
    }

    public Component mapGroupToYarnComponent(Group group, CloudStack stack, Artifact artifact) {
        Component component = new Component();
        String userData = stack.getImage().getUserDataByType(group.getType());
        setupComponentProperties(group, stack, artifact, component, userData);
        decorateComponentWithresource(group, component);
        decorateComponentWithConfiguration(group, stack, component, userData);
        return component;
    }

    private void decorateComponentWithConfiguration(Group group, CloudStack stack, Component component, String userData) {
        ModelConfiguration configuration = new ModelConfiguration();
        configuration.putEnvItem("YARN_CONTAINER_RUNTIME_DOCKER_MOUNTS", "/sys/fs/cgroup:/sys/fs/cgroup:ro");
        configuration.putEnvItem("YARN_CONTAINER_RUNTIME_DOCKER_RUN_OVERRIDE_DISABLE", "true");

        addCloudbreakSpecificConfiguration(group, stack, userData, configuration);

        addResourceOverrideConfiguration(group, configuration);

        component.setConfiguration(configuration);
    }

    private void addCloudbreakSpecificConfiguration(Group group, CloudStack stack, String userData, ModelConfiguration configuration) {
        ConfigFile cloudbreakConfig = new ConfigFile();
        cloudbreakConfig.setType(TypeEnum.TEMPLATE);
        cloudbreakConfig.setDestFile("/etc/cloudbreak-config.props");
        cloudbreakConfig.putPropertiesItem("content",
                "userData=" + '\'' + Base64.getEncoder().encodeToString(userData.getBytes()) + '\'' + '\n'
                        + "sshUser=" + '\'' + stack.getLoginUserName() + '\'' + '\n'
                        + "groupname=" + '\'' + group.getName() + '\'' + '\n'
                        + "sshPubKey=" + '\'' + stack.getPublicKey() + '\'' + '\n');
        configuration.addFilesItem(cloudbreakConfig);
    }

    private void addResourceOverrideConfiguration(Group group, ModelConfiguration configuration) {
        Optional<CloudInstance> cloudInstance = group.getInstances().stream().findAny();
        if (cloudInstance.isPresent()) {
            InstanceTemplate template = cloudInstance.get().getTemplate();
            Integer cpuCount = Optional.ofNullable(template.getParameter(PlatformParametersConsts.CUSTOM_INSTANCETYPE_CPUS, Integer.class))
                    .orElse(DEFAULT_CPU_COUNT);
            Integer memoryInMb = Optional.ofNullable(template.getParameter(PlatformParametersConsts.CUSTOM_INSTANCETYPE_MEMORY, Integer.class))
                    .orElse(DEFAULT_MEM_SIZE_IN_MB);
            int memoryInKb = memoryInMb * MB_TO_KB;
            ConfigFile resourcesOverrides = new ConfigFile();
            resourcesOverrides.setType(TypeEnum.TEMPLATE);
            resourcesOverrides.setDestFile("/etc/resource_overrides/yarn.json");
            resourcesOverrides.putPropertiesItem("content",
                    "{\n"
                            + "    \"processorcount\": \"" + cpuCount + "\",\n"
                            + "    \"physicalprocessorcount\": \"" + cpuCount + "\",\n"
                            + "    \"memorysize\": \"" + memoryInKb + "\",\n"
                            + "    \"memoryfree\": \"" + memoryInKb + "\",\n"
                            + "    \"memorytotal\": \"" + memoryInKb + "\"\n"
                            + '}');
            configuration.addFilesItem(resourcesOverrides);
        }
    }

    private String setupComponentProperties(Group group, CloudStack stack, Artifact artifact, Component component, String userData) {
        component.setName(cumulusYarnResourceNameHelper.getComponentNameFromGroupName(group.getName()));
        component.setNumberOfContainers(group.getInstancesSize().longValue());


        component.setLaunchCommand(String.format("'%s','%s','%s'", Base64.getEncoder().encodeToString(userData.getBytes()),
                stack.getLoginUserName(), stack.getPublicKey()));
        component.setArtifact(artifact);
        component.setDependencies(new ArrayList<>());
        component.setRunPrivilegedContainer(true);
        component.setRestartPolicy(RestartPolicyEnum.ALWAYS);
        return userData;
    }

    private void decorateComponentWithresource(Group group, Component component) {
        InstanceTemplate instanceTemplate = group.getReferenceInstanceConfiguration().getTemplate();
        Resource resource = new Resource();
        resource.setCpus(instanceTemplate.getParameter(PlatformParametersConsts.CUSTOM_INSTANCETYPE_CPUS, Integer.class));
        resource.setMemory(instanceTemplate.getParameter(PlatformParametersConsts.CUSTOM_INSTANCETYPE_MEMORY, Integer.class).toString());
        component.setResource(resource);
    }

}
