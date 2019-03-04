package com.sequenceiq.cloudbreak.cloud.yarn;

import static com.sequenceiq.cloudbreak.common.type.ResourceType.YARN_APPLICATION;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.AdjustmentType;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.CloudOperationNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.exception.TemplatingDoesNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.yarn.auth.YarnClientUtil;
import com.sequenceiq.cloudbreak.cloud.yarn.client.YarnClient;
import com.sequenceiq.cloudbreak.cloud.yarn.client.api.YarnResourceConstants;
import com.sequenceiq.cloudbreak.cloud.yarn.client.exception.YarnClientException;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.core.Artifact;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.core.ConfigFile;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.core.ConfigFileType;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.core.Configuration;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.core.Resource;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.core.YarnComponent;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.request.ApplicationDetailRequest;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.request.CreateApplicationRequest;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.request.DeleteApplicationRequest;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.response.ApplicationDetailResponse;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.response.ApplicationErrorResponse;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.response.ResponseContext;
import com.sequenceiq.cloudbreak.cloud.yarn.status.YarnApplicationStatus;

@Service
public class YarnResourceConnector implements ResourceConnector<Object> {
    private static final Logger LOGGER = LoggerFactory.getLogger(YarnResourceConnector.class);

    private static final String ARTIFACT_TYPE_DOCKER = "DOCKER";

    private static final int APPNAME_HYPHEN_NUM = 2;

    @Inject
    private YarnClientUtil yarnClientUtil;

    @Value("${cb.max.yarn.resource.name.length:}")
    private int maxResourceNameLength;

    @Value("${cb.yarn.defaultQueue}")
    private String defaultQueue;

    @Value("${cb.yarn.defaultLifeTime:}")
    private int defaultLifeTime;

    @Override
    public List<CloudResourceStatus> launch(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier persistenceNotifier,
            AdjustmentType adjustmentType, Long threshold) throws Exception {
        YarnClient yarnClient = yarnClientUtil.createYarnClient(authenticatedContext);
        String applicationName = createApplicationName(authenticatedContext);

        if (!checkApplicationAlreadyCreated(yarnClient, applicationName)) {
            CreateApplicationRequest createApplicationRequest = createRequest(stack, applicationName);
            createApplication(yarnClient, createApplicationRequest);
        }

        CloudResource yarnApplication = new Builder().type(YARN_APPLICATION).name(applicationName).build();
        persistenceNotifier.notifyAllocation(yarnApplication, authenticatedContext.getCloudContext());
        return check(authenticatedContext, Collections.singletonList(yarnApplication));
    }

    private void createApplication(YarnClient yarnClient, CreateApplicationRequest createApplicationRequest) throws MalformedURLException {
        ResponseContext responseContext = yarnClient.createApplication(createApplicationRequest);
        if (Objects.nonNull(responseContext.getResponseError())) {
            ApplicationErrorResponse applicationErrorResponse = responseContext.getResponseError();
            throw new CloudConnectorException(String.format("Yarn Application creation error: HTTP Return: %d Error: %s", responseContext.getStatusCode(),
                    applicationErrorResponse.getDiagnostics()));
        }
    }

    private CreateApplicationRequest createRequest(CloudStack stack, String applicationName) {
        CreateApplicationRequest createApplicationRequest = new CreateApplicationRequest();
        createApplicationRequest.setName(applicationName);
        createApplicationRequest.setQueue(stack.getParameters().getOrDefault(YarnConstants.YARN_QUEUE_PARAMETER, defaultQueue));
        String lifeTimeStr = stack.getParameters().get(YarnConstants.YARN_LIFETIME_PARAMETER);
        createApplicationRequest.setLifetime(lifeTimeStr != null ? Integer.parseInt(lifeTimeStr) : defaultLifeTime);

        Artifact artifact = new Artifact();
        artifact.setId(stack.getImage().getImageName());
        artifact.setType(ARTIFACT_TYPE_DOCKER);

        List<YarnComponent> components = stack.getGroups().stream()
            .map(group -> mapGroupToYarnComponent(group, stack, artifact))
            .collect(Collectors.toList());

        createApplicationRequest.setComponents(components);
        return createApplicationRequest;
    }

    private YarnComponent mapGroupToYarnComponent(Group group, CloudStack stack, Artifact artifact) {
        YarnComponent component = new YarnComponent();
        component.setName(group.getName());
        component.setNumberOfContainers(group.getInstancesSize());
        String userData = stack.getImage().getUserDataByType(group.getType());
        component.setLaunchCommand(String.format("/bootstrap/start-systemd '%s' '%s' '%s'", Base64.getEncoder().encodeToString(userData.getBytes()),
                stack.getLoginUserName(), stack.getPublicKey()));
        component.setArtifact(artifact);
        component.setDependencies(new ArrayList<>());
        InstanceTemplate instanceTemplate = group.getReferenceInstanceConfiguration().getTemplate();
        Resource resource = new Resource();
        resource.setCpus(instanceTemplate.getParameter(PlatformParametersConsts.CUSTOM_INSTANCETYPE_CPUS, Integer.class));
        resource.setMemory(instanceTemplate.getParameter(PlatformParametersConsts.CUSTOM_INSTANCETYPE_MEMORY, Integer.class));
        component.setResource(resource);
        component.setRunPrivilegedContainer(true);

        Configuration configuration = new Configuration();
        Map<String, String> propsMap = Maps.newHashMap();
        propsMap.put("conf.cb-conf.per.component", "true");
        propsMap.put("site.cb-conf.userData", '\'' + Base64.getEncoder().encodeToString(userData.getBytes()) + '\'');
        propsMap.put("site.cb-conf.sshUser", '\'' + stack.getLoginUserName() + '\'');
        propsMap.put("site.cb-conf.groupname", '\'' + group.getName() + '\'');
        propsMap.put("site.cb-conf.sshPubKey", '\'' + stack.getPublicKey() + '\'');
        configuration.setProperties(propsMap);
        ConfigFile configFileProps = new ConfigFile();
        configFileProps.setType(ConfigFileType.PROPERTIES.name());
        configFileProps.setSrcFile("cb-conf");
        configFileProps.setDestFile("/etc/cloudbreak-config.props");
        configuration.setFiles(Collections.singletonList(configFileProps));

        component.setConfiguration(configuration);
        return component;
    }

    private boolean checkApplicationAlreadyCreated(YarnClient yarnClient, String applicationName) throws MalformedURLException {
        ApplicationDetailRequest applicationDetailRequest = new ApplicationDetailRequest();
        applicationDetailRequest.setName(applicationName);
        return yarnClient.getApplicationDetail(applicationDetailRequest).getStatusCode() == YarnResourceConstants.HTTP_SUCCESS;
    }

    @Override
    public List<CloudResourceStatus> check(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        YarnClient yarnClient = yarnClientUtil.createYarnClient(authenticatedContext);
        List<CloudResourceStatus> result = new ArrayList<>();
        for (CloudResource resource : resources) {
            if (resource.getType() == YARN_APPLICATION) {
                LOGGER.debug("Checking Yarn application status of: {}", resource.getName());
                try {
                    ApplicationDetailRequest applicationDetailRequest = new ApplicationDetailRequest();
                    applicationDetailRequest.setName(resource.getName());
                    ResponseContext responseContext = yarnClient.getApplicationDetail(applicationDetailRequest);
                    if (responseContext.getStatusCode() == YarnResourceConstants.HTTP_SUCCESS) {
                        ApplicationDetailResponse applicationDetailResponse = (ApplicationDetailResponse) responseContext.getResponseObject();
                        result.add(new CloudResourceStatus(resource, YarnApplicationStatus.mapResourceStatus(applicationDetailResponse.getState())));
                    } else if (responseContext.getStatusCode() == YarnResourceConstants.HTTP_NOT_FOUND) {
                        result.add(new CloudResourceStatus(resource, ResourceStatus.DELETED, "Yarn application has been killed."));
                    } else if (responseContext.getResponseError() != null) {
                        throw new CloudConnectorException(String.format("Yarn Application status check failed: HttpStatusCode: %d, Error: %s",
                                responseContext.getStatusCode(), responseContext.getResponseError().getDiagnostics()));
                    } else {
                        throw new CloudConnectorException(String.format("Yarn Application status check failed: Invalid HttpStatusCode: %d",
                                responseContext.getStatusCode()));
                    }
                } catch (MalformedURLException | RuntimeException e) {
                    throw new CloudConnectorException(String.format("Invalid resource exception: %s", e.getMessage()), e);
                }
            } else {
                throw new CloudConnectorException(String.format("Invalid resource type: %s", resource.getType()));
            }
        }
        return result;
    }

    @Override
    public List<CloudResourceStatus> terminate(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> cloudResources) {
        for (CloudResource resource : cloudResources) {
            if (resource.getType() == YARN_APPLICATION) {
                YarnClient yarnClient = yarnClientUtil.createYarnClient(authenticatedContext);
                String yarnApplicationName = resource.getName();
                String stackName = authenticatedContext.getCloudContext().getName();
                LOGGER.debug("Terminate stack: {}", stackName);
                try {
                    DeleteApplicationRequest deleteApplicationRequest = new DeleteApplicationRequest();
                    deleteApplicationRequest.setName(yarnApplicationName);
                    yarnClient.deleteApplication(deleteApplicationRequest);
                    LOGGER.debug("Yarn Applicatin has been deleted");
                } catch (MalformedURLException | YarnClientException e) {
                    throw new CloudConnectorException("Stack cannot be deleted", e);
                }
            } else {
                throw new CloudConnectorException(String.format("Invalid resource type: %s", resource.getType()));
            }
        }
        return check(authenticatedContext, cloudResources);
    }

    @Override
    public List<CloudResourceStatus> update(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) {
        return null;
    }

    @Override
    public List<CloudResourceStatus> upscale(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) {
        throw new CloudOperationNotSupportedException("Upscale stack operation is not supported on YARN");
    }

    @Override
    public List<CloudResourceStatus> downscale(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources,
            List<CloudInstance> vms, Object resourcesToRemove) {
        throw new CloudOperationNotSupportedException("Downscale stack operation is not supported on YARN");
    }

    @Override
    public Object collectResourcesToRemove(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources, List<CloudInstance> vms) {
        throw new CloudOperationNotSupportedException("Downscale resources collection operation is not supported on YARN");
    }

    @Override
    public TlsInfo getTlsInfo(AuthenticatedContext authenticatedContext, CloudStack cloudStack) {
        return new TlsInfo(false);
    }

    @Override
    public String getStackTemplate() throws TemplatingDoesNotSupportedException {
        throw new TemplatingDoesNotSupportedException();
    }

    private String createApplicationName(AuthenticatedContext ac) {
        CloudContext context = ac.getCloudContext();
        String name = context.getName();
        String id = context.getId().toString();
        String user = context.getUserId().split("@")[0].replaceAll("[^a-z0-9-_]", "");
        int nameLength = Math.max(maxResourceNameLength - (id.length() + user.length() + APPNAME_HYPHEN_NUM), 1);
        String appName = String.format("%s-%s-%s", Splitter.fixedLength(nameLength).splitToList(name).get(0), id, user);
        if (appName.length() > maxResourceNameLength) {
            appName = appName.substring(0, maxResourceNameLength);
        }
        return appName;
    }
}
