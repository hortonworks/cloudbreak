package com.sequenceiq.cloudbreak.cloud.yarn.loadbalancer.service.component;

import static com.sequenceiq.cloudbreak.cloud.yarn.YarnApplicationCreationService.ARTIFACT_TYPE_DOCKER;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.yarn.ApplicationNameUtil;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.core.Artifact;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.core.ConfigFile;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.core.ConfigFileType;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.core.Configuration;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.core.Resource;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.core.YarnComponent;
import com.sequenceiq.common.api.type.InstanceGroupType;

@Service
public class YarnLoadBalancerComponentFactoryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(YarnLoadBalancerComponentFactoryService.class);

    private final ApplicationNameUtil applicationNameUtil;

    /**
     * Must match an existing Docker image in the primary Cloudbreak repository.
     */
    private final String loadBalancerImageName = "docker-sandbox.infra.cloudera.com/cloudbreak/yarn-loadbalancer:2021-03-23-12-02-09";

    private final int loadBalancerNumCPUs = 1;

    /**
     * In megabytes.
     */
    private final int loadBalancerMemorySize = 1024;

    YarnLoadBalancerComponentFactoryService(ApplicationNameUtil applicationNameUtil) {
        Objects.requireNonNull(applicationNameUtil);
        this.applicationNameUtil = applicationNameUtil;
    }

    /**
     * Creates a Yarn component for each of the loadbalancers that are to be created, using a set of pre-defined
     * constants for the various component parameters.
     *
     * Each container is pointed at a specific docker image which contains the loadbalancing service and logic.
     */
    public List<YarnComponent> create(CloudStack cloudStack, List<String> gatewayIPs, String applicationName) {
        LOGGER.debug("Creating the loadbalancer components for application " + applicationName + " with gatewayIPs: " + gatewayIPs.toString() + ".");
        Artifact artifact = createLoadBalancerArtifact();
        Resource resource = createLoadBalancerResource();
        String launchCommand = createLoadBalancerLaunchCommand(cloudStack);
        Configuration configuration = createLoadBalancerConfiguration(gatewayIPs);

        List<YarnComponent> loadBalancerComponents = Lists.newArrayList();
        for (CloudLoadBalancer loadBalancer : cloudStack.getLoadBalancers()) {
            String componentName = applicationNameUtil.createLoadBalancerComponentName(applicationName, loadBalancer.getType());
            LOGGER.debug("Creating the load balancer Yarn component object for " + componentName + ".");
            loadBalancerComponents.add(createLoadBalancerComponent(componentName, artifact, resource, launchCommand, configuration));
        }

        LOGGER.debug("Finished creating the Yarn load balancer components for application " + applicationName + ".");
        return loadBalancerComponents;
    }

    /**
     * Creates a YarnComponent, using specific parameters to set up the load balancer service on the designated
     * docker image pointed to.
     */
    private YarnComponent createLoadBalancerComponent(String name, Artifact artifact, Resource resource, String launchCommand, Configuration configuration) {
        YarnComponent component = new YarnComponent();
        component.setName(name);
        component.setArtifact(artifact);
        component.setResource(resource);
        component.setNumberOfContainers(1);
        component.setDependencies(Collections.emptyList());
        component.setRunPrivilegedContainer(true);
        component.setLaunchCommand(launchCommand);
        component.setConfiguration(configuration);
        LOGGER.debug("Created Yarn load balancer component: " + component);
        return component;
    }

    /**
     * Creates the artifact object for the loadbalancer Yarn component, which points at the specific Docker image
     * that will set up the actual service which will do the loadbalancing.
     */
    private Artifact createLoadBalancerArtifact() {
        Artifact artifact = new Artifact();
        artifact.setId(loadBalancerImageName);
        artifact.setType(ARTIFACT_TYPE_DOCKER);
        return artifact;
    }

    /**
     * Creates the resource object for the loadbalancer Yarn component, which specifies the amount of
     * CPUs to use and the amount of memory to use for the container.
     */
    private Resource createLoadBalancerResource() {
        Resource resource = new Resource();
        resource.setCpus(loadBalancerNumCPUs);
        resource.setMemory(loadBalancerMemorySize);
        return resource;
    }

    /**
     * Creates the launch command for the loadbalancer Yarn component, which uses a custom start script.
     */
    private String createLoadBalancerLaunchCommand(CloudStack cloudStack) {
        return String.format("/bootstrap/start-systemd '%s' '%s' '%s'",
                Base64.getEncoder().encodeToString(cloudStack.getImage().getUserDataByType(InstanceGroupType.CORE).getBytes()),
                cloudStack.getLoginUserName(), cloudStack.getPublicKey());
    }

    /**
     * Creates the configuration object for the loadbalancer Yarn component, which specifies the backend servers
     * the loadbalancer container will balance against, as well as any other custom properties for the
     * Docker image to use.
     *
     * The name of the destination file must match the name of the properties file used for the Docker image.
     */
    private Configuration createLoadBalancerConfiguration(List<String> gatewayIPs) {
        Map<String, String> propsMap = Maps.newHashMap();
        propsMap.put("conf.cb-conf.per.component", "true");
        propsMap.put("site.cb-conf.groupname", "'loadbalancer'");
        propsMap.put("site.cb-conf.servers", '\'' + String.join(" ", gatewayIPs) + '\'');

        ConfigFile configFileProps = new ConfigFile();
        configFileProps.setType(ConfigFileType.PROPERTIES.name());
        configFileProps.setSrcFile("cb-conf");
        configFileProps.setDestFile("/etc/cloudbreak-loadbalancer.props");

        Configuration configuration = new Configuration();
        configuration.setProperties(propsMap);
        configuration.setFiles(Collections.singletonList(configFileProps));
        return configuration;
    }
}
