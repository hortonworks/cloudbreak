package com.sequenceiq.it.cloudbreak.dto.distrox;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest.STACK_DELETED;
import static java.lang.String.format;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSet;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.SecurityV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AwsInstanceTemplateV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AwsInstanceTemplateV1SpotParameters;
import com.sequenceiq.distrox.api.v1.distrox.model.security.SecurityV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Request;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.assertion.util.InstanceIPCollectorUtil;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.cloud.v4.CloudProviderProxy;
import com.sequenceiq.it.cloudbreak.context.Clue;
import com.sequenceiq.it.cloudbreak.context.Investigable;
import com.sequenceiq.it.cloudbreak.context.Purgable;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.clustertemplate.ClusterTemplateTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXUpgradeTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceTemplateTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.search.ClusterLogsStorageUrl;
import com.sequenceiq.it.cloudbreak.search.Searchable;
import com.sequenceiq.it.cloudbreak.search.StorageUrl;
import com.sequenceiq.it.cloudbreak.util.AuditUtil;
import com.sequenceiq.it.cloudbreak.util.InstanceUtil;
import com.sequenceiq.it.cloudbreak.util.LogCollectorUtil;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;
import com.sequenceiq.it.cloudbreak.util.yarn.YarnCloudFunctionality;

@Prototype
public class DistroXTestDto extends DistroXTestDtoBase<DistroXTestDto> implements Purgable<StackV4Response, CloudbreakClient>, Investigable, Searchable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXTestDto.class);

    private final Map<HostGroupType, String> privateIps = new EnumMap<>(HostGroupType.class);

    private GeneratedBlueprintV4Response generatedBlueprint;

    private StackViewV4Response internalStackResponse;

    private List<String> actionableInstanceIds;

    private List<OrderedOSUpgradeSet> osUpgradeByUpgradeSets;

    private Optional<List<String>> repairableInstanceIds = Optional.empty();

    private String initiatorUserCrn;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private YarnCloudFunctionality yarnCloudFunctionality;

    @Inject
    private LogCollectorUtil logCollectorUtil;

    private CloudPlatform cloudPlatformFromStack;

    private Set<String> entries = new HashSet<>();

    public DistroXTestDto(TestContext testContext) {
        super(new DistroXV1Request(), testContext);
    }

    @Override
    public DistroXTestDtoBase<DistroXTestDto> valid() {
        DistroXTestDtoBase<DistroXTestDto> valid = super.valid();
        EnvironmentTestDto environmentTestDto = getTestContext().get(EnvironmentTestDto.class);
        if (environmentTestDto != null && environmentTestDto.getResponse() != null) {
            valid.withEnvironmentName(environmentTestDto.getName());
        }
        return valid;
    }

    @Override
    public int order() {
        return 400;
    }

    @Override
    public void deleteForCleanup() {
        try {
            CloudbreakClient clientForCleanup = getClientForCleanup();
            LOGGER.info("Deleting DataHub with crn: {}", getCrn());
            clientForCleanup.getDefaultClient(getTestContext()).distroXV1Endpoint().deleteByCrn(getCrn(), true);
            awaitWithClient(STACK_DELETED, clientForCleanup);
        } catch (NotFoundException nfe) {
            LOGGER.info("resource not found, thus cleanup not needed.");
        }
    }

    @Override
    public List<StackV4Response> getAll(CloudbreakClient client) {
        DistroXV1Endpoint distroXV1Endpoint = client.getDefaultClient(getTestContext()).distroXV1Endpoint();
        return distroXV1Endpoint.list(null, null).getResponses().stream()
                .filter(s -> s.getName() != null)
                .map(s -> {
                    StackV4Response stackResponse = new StackV4Response();
                    stackResponse.setName(s.getName());
                    return stackResponse;
                }).collect(Collectors.toList());
    }

    @Override
    public boolean deletable(StackV4Response entity) {
        return entity.getName().startsWith(getResourcePropertyProvider().prefix(getCloudPlatform()));
    }

    @Override
    public void delete(TestContext testContext, StackV4Response entity, CloudbreakClient client) {
        try {
            client.getDefaultClient(getTestContext()).distroXV1Endpoint().deleteByName(entity.getName(), true);
            testContext.await(this, STACK_DELETED, key("wait-purge-distrox-" + entity.getName()));
        } catch (Exception e) {
            LOGGER.warn("Something went wrong on {} purge. {}", entity.getName(), ResponseUtil.getErrorMessage(e), e);
        }
    }

    @Override
    public Class<CloudbreakClient> client() {
        return CloudbreakClient.class;
    }

    @Override
    public CloudbreakTestDto refresh() {
        return when(distroXTestClient.refresh(), key("refresh-distrox-" + getName()).withSkipOnFail(false).withLogError(false));
    }

    @Override
    public CloudbreakTestDto wait(Map<String, Status> desiredStatuses, RunningParameter runningParameter) {
        return await(desiredStatuses, runningParameter);
    }

    @Override
    public DistroXTestDto await(Map<String, Status> statuses) {
        return getTestContext().await(this, statuses);
    }

    public DistroXTestDto awaitAndIgnoreFlows(Map<String, Status> statuses) {
        super.await(statuses, emptyRunningParameter());
        return this;
    }

    @Override
    public DistroXTestDto await(Class<DistroXTestDto> entityClass, Map<String, Status> statuses) {
        return getTestContext().await(this, statuses, emptyRunningParameter());
    }

    @Override
    public DistroXTestDto await(Class<DistroXTestDto> entityClass, Map<String, Status> statuses, RunningParameter runningParameter) {
        return getTestContext().await(this, statuses, runningParameter);
    }

    @Override
    public DistroXTestDto await(Map<String, Status> statuses, RunningParameter runningParameter) {
        return getTestContext().await(this, statuses, runningParameter);
    }

    private Map<List<String>, InstanceStatus> getInstanceStatusMapIfAvailableInResponse(Supplier<Map<List<String>, InstanceStatus>> instanceStatusMapSupplier) {
        if (checkResponseHasInstanceGroups()) {
            return instanceStatusMapSupplier.get();
        } else {
            LOGGER.info("Response doesn't has instance groups");
            return Collections.emptyMap();
        }
    }

    private boolean checkResponseHasInstanceGroups() {
        return getResponse() != null && getResponse().getInstanceGroups() != null;
    }

    public DistroXTestDto awaitForHealthyInstances() {
        Map<List<String>, InstanceStatus> instanceStatusMap = getInstanceStatusMapIfAvailableInResponse(() ->
                InstanceUtil.getInstanceStatusMapForStatus(getResponse(), InstanceStatus.SERVICES_HEALTHY));
        return awaitForInstance(instanceStatusMap);
    }

    public DistroXTestDto awaitForActionedInstances(InstanceStatus instanceStatus) {
        if (!getInstanceIdsForAction().isEmpty()) {
            return awaitForInstance(Map.of(getInstanceIdsForAction(), instanceStatus));
        } else {
            throw new IllegalStateException(format("There is no '%s' instance to wait!", instanceStatus));
        }
    }

    public DistroXTestDto awaitForHostGroup(String hostGroup, InstanceStatus instanceStatus) {
        if (!getTestContext().getExceptionMap().isEmpty()) {
            Log.await(LOGGER, format("Await for host group should be skipped because of previous error. awaitForHostGroup [%s] - [%s]",
                    hostGroup, instanceStatus));
            return this;
        }
        Optional<InstanceGroupV4Response> instanceGroup = getResponse().getInstanceGroups().stream()
                .filter(instanceGroupV4Response -> hostGroup.equals(instanceGroupV4Response.getName()))
                .filter(instanceGroupV4Response -> instanceGroupV4Response.getMetadata().stream()
                        .anyMatch(instanceMetaDataV4Response -> Objects.nonNull(instanceMetaDataV4Response.getInstanceId())))
                .findAny();
        if (instanceGroup.isPresent()) {
            List<String> instanceIds = instanceGroup.get().getMetadata().stream()
                    .map(InstanceMetaDataV4Response::getInstanceId)
                    .filter(Objects::nonNull)
                    .toList();
            return awaitForInstance(Map.of(instanceIds, instanceStatus));
        } else {
            throw new IllegalStateException("Can't find valid instance group with this name: " + hostGroup);
        }
    }

    public DistroXTestDto awaitForInstance(Map<List<String>, InstanceStatus> statuses) {
        return awaitForInstance(statuses, emptyRunningParameter());
    }

    public DistroXTestDto awaitForInstance(Map<List<String>, InstanceStatus> statuses, RunningParameter runningParameter) {
        return getTestContext().awaitForInstance(this, statuses, runningParameter);
    }

    public DistroXTestDto awaitForFlow() {
        return awaitForFlow(emptyRunningParameter());
    }

    public DistroXTestDto awaitForFlowFail() {
        return awaitForFlow(emptyRunningParameter().withWaitForFlowFail());
    }

    @Override
    public DistroXTestDto awaitForFlow(RunningParameter runningParameter) {
        return getTestContext().awaitForFlow(this, runningParameter);
    }

    public DistroXTestDto withGeneratedBlueprint(GeneratedBlueprintV4Response generatedBlueprint) {
        this.generatedBlueprint = generatedBlueprint;
        return this;
    }

    public GeneratedBlueprintV4Response getGeneratedBlueprint() {
        return generatedBlueprint;
    }

    public DistroXTestDto fromClusterDefinition(String key) {
        Optional<ClusterTemplateTestDto> template = Optional.ofNullable(getTestContext().get(key));
        setRequest(template.orElseThrow(() -> new TestFailException("Unable to find DistroXV1Request")).getRequest().getDistroXTemplate());
        return this;
    }

    public StackViewV4Response getInternalStackResponse() {
        return internalStackResponse;
    }

    public DistroXTestDto withAutoTls() {
        getRequest().getCluster().getCm().setEnableAutoTls(true);
        return this;
    }

    public DistroXTestDto withInternalStackResponse(StackViewV4Response internalStackResponse) {
        this.internalStackResponse = internalStackResponse;
        return this;
    }

    public DistroXTestDto withGeneratedBlueprintV4Response(GeneratedBlueprintV4Response response) {
        this.generatedBlueprint = response;
        return this;
    }

    public DistroXTestDto withTemplate(String template) {
        getRequest().getCluster().setBlueprintName(template);
        return this;

    }

    public DistroXTestDto withSpotPercentage(int spotPercentage) {
        getRequest().getInstanceGroups().stream()
                .map(InstanceGroupV1Request::getTemplate)
                .forEach(instanceTemplateV1Request -> {
                    AwsInstanceTemplateV1Parameters aws = instanceTemplateV1Request.getAws();
                    if (Objects.isNull(aws)) {
                        aws = new AwsInstanceTemplateV1Parameters();
                        instanceTemplateV1Request.setAws(aws);
                    }
                    AwsInstanceTemplateV1SpotParameters spot = new AwsInstanceTemplateV1SpotParameters();
                    spot.setPercentage(spotPercentage);
                    aws.setSpot(spot);
                });
        return this;
    }

    public DistroXTestDto withRecipe(String recipeName) {
        InstanceGroupV1Request instanceGroupV1Request = getRequest().getInstanceGroups().iterator().next();
        instanceGroupV1Request.setRecipeNames(Sets.newHashSet());
        instanceGroupV1Request.getRecipeNames().add(recipeName);
        return this;
    }

    public DistroXTestDto withRecipes(String... recipeNames) {
        InstanceGroupV1Request instanceGroupV1Request = getRequest().getInstanceGroups().iterator().next();
        instanceGroupV1Request.setRecipeNames(Sets.newHashSet());
        instanceGroupV1Request.getRecipeNames().addAll(Arrays.stream(recipeNames).toList());
        return this;
    }

    public DistroXTestDto withRecipe(String recipeName, String groupName) {
        InstanceGroupV1Request instanceGroupV1Request = getRequest().getInstanceGroups().stream()
                .filter(group -> StringUtils.equalsIgnoreCase(group.getName(), groupName))
                .findFirst()
                .orElseThrow();
        instanceGroupV1Request.setRecipeNames(Sets.newHashSet());
        instanceGroupV1Request.getRecipeNames().add(recipeName);
        return this;
    }

    public DistroXTestDto withInitiatorUserCrn(String initiatorUserCrn) {
        this.initiatorUserCrn = initiatorUserCrn;
        return this;
    }

    public DistroXTestDto withExternalDatabaseOnAws(DistroXDatabaseRequest database) {
        if (CloudPlatform.AWS.equals(getCloudPlatform())) {
            getRequest().setExternalDatabase(database);
        }
        return this;
    }

    public DistroXTestDto withJavaVersion(Integer javaVersion) {
        getRequest().setJavaVersion(javaVersion);
        return this;
    }

    public DistroXTestDto withArchitecture(Architecture architecture) {
        getRequest().setArchitecture(architecture.getName());
        getCloudProvider().template(getTestContext().given(DistroXInstanceTemplateTestDto.class), architecture);
        return this;
    }

    public DistroXTestDto withEntries(Set<String> entries) {
        this.entries = entries;
        return this;
    }

    public Set<String> getEntries() {
        return entries;
    }

    public String getInitiatorUserCrn() {
        return initiatorUserCrn;
    }

    public DistroXTestDto addTags(Map<String, String> tags) {
        getRequest().initAndGetTags().getUserDefined().putAll(tags);
        return this;
    }

    public DistroXUpgradeV1Request getDistroXUpgradeRequest() {
        DistroXUpgradeTestDto upgradeTestDto = given(DistroXUpgradeTestDto.class);
        if (upgradeTestDto == null) {
            throw new IllegalArgumentException("DistroX upgrade dto does not exist!");
        }
        return upgradeTestDto.getRequest();
    }

    @Override
    public Clue investigate() {
        if (getResponse() == null || getResponse().getId() == null) {
            return null;
        }
        String resourceName = getResponse().getName();
        String resourceCrn = getResponse().getCrn();
        setCloudPlatformFromStack(getResponse());
        collectLogFiles();
        AuditEventV4Responses auditEvents = AuditUtil.getAuditEvents(
                getTestContext().getMicroserviceClient(CloudbreakClient.class),
                CloudbreakEventService.DATAHUB_RESOURCE_TYPE,
                getResponse().getId(),
                null,
                getTestContext());
        boolean hasSpotTermination = (getResponse().getInstanceGroups() == null) ? false : getResponse().getInstanceGroups().stream()
                .flatMap(ig -> ig.getMetadata().stream())
                .anyMatch(metadata -> InstanceStatus.DELETED_BY_PROVIDER == metadata.getInstanceStatus());
        List<Searchable> listOfSearchables = List.of(this);
        return new Clue(
                resourceName,
                resourceCrn,
                getCloudStorageUrl(resourceName, resourceCrn),
                getLogSearchUrl(listOfSearchables),
                auditEvents,
                List.of(),
                getResponse(),
                hasSpotTermination);
    }

    private void collectLogFiles() {
        try {
            List<String> ipAddresses = getResponse().getInstanceGroups().stream().flatMap(ig -> ig.getMetadata().stream())
                    .map(imd -> imd.getPublicIp() != null && !Objects.equals(imd.getPublicIp(), "N/A") ? imd.getPublicIp() : imd.getPrivateIp()).toList();
            logCollectorUtil.collectLogFiles(getResponse().getStatusReason(), ipAddresses);
        } catch (Exception e) {
            LOGGER.warn("Failed to collect datahub log files for investigation.", e);
        }
    }

    @Override
    public String getCloudStorageUrl(String resourceName, String resourceCrn) {
        if (CloudPlatform.YARN.equalsIgnoreCase(getCloudPlatform().name())) {
            LOGGER.info("Special case for AWS-YCloud Hybrid tests. " +
                    "Here the defined Cloud Provider is AWS and the Cluster Logs are stored at AWS. " +
                    "However the Datahub has been created at YCloud. So the Base Location for logs are also different.");
            return yarnCloudFunctionality.getDataHubS3LogsUrl(resourceCrn);
        } else {
            CloudProviderProxy cloudProviderProxy = getTestContext().getCloudProvider();
            String baseLocation = getResponse().getTelemetry() != null && getResponse().getTelemetry().getLogging() != null
                    ? getResponse().getTelemetry().getLogging().getStorageLocation()
                    : null;
            StorageUrl storageUrl = new ClusterLogsStorageUrl();
            return (isCloudProvider(cloudProviderProxy) && StringUtils.isNotBlank(baseLocation))
                    ? storageUrl.getDataHubStorageUrl(resourceName, resourceCrn, baseLocation, cloudProviderProxy)
                    : null;
        }
    }

    public InstanceGroupV4Response findInstanceGroupByName(String name) {
        return getResponse().getInstanceGroups().stream().filter(ig -> name.equals(ig.getName())).findFirst()
                .orElseThrow(() -> new TestFailException("Unable to find Data Hub instance group based on the following name: " + name));
    }

    @Override
    public String getSearchId() {
        return getName();
    }

    @Override
    public String getCrn() {
        return getResponse().getCrn();
    }

    public void setInstanceIdsForActions(List<String> actionableInstanceIds) {
        this.actionableInstanceIds = actionableInstanceIds;
    }

    public List<String> getInstanceIdsForAction() {
        return ListUtils.emptyIfNull(actionableInstanceIds);
    }

    public Optional<List<String>> getRepairableInstanceIds() {
        return repairableInstanceIds;
    }

    public void setRepairableInstanceIds(List<String> repairableInstanceIds) {
        this.repairableInstanceIds = Optional.of(repairableInstanceIds);
    }

    public List<OrderedOSUpgradeSet> getOsUpgradeByUpgradeSets() {
        return osUpgradeByUpgradeSets;
    }

    public void setOsUpgradeByUpgradeSets(List<OrderedOSUpgradeSet> osUpgradeByUpgradeSets) {
        this.osUpgradeByUpgradeSets = osUpgradeByUpgradeSets;
    }

    public DistroXTestDto withEnableMultiAz(boolean enableMultiAz) {
        getRequest().setEnableMultiAz(enableMultiAz);
        return this;
    }

    private void setCloudPlatformFromStack(StackV4Response stackResponse) {
        if (stackResponse != null) {
            cloudPlatformFromStack = stackResponse.getCloudPlatform();
        }
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return (cloudPlatformFromStack != null) ? cloudPlatformFromStack : super.getCloudPlatform();
    }

    @Override
    public Map<HostGroupType, String> getPrivateIpsForLogCollection() {
        return privateIps;
    }

    @Override
    public void setPrivateIpsForLogCollection(TestContext testContext) {
        refreshResponse(testContext, testContext.getCloudbreakClient());
        String hostGroupName = MASTER.getName();
        InstanceMetaDataV4Response instanceMetaData = getInstanceMetaData(hostGroupName).stream()
                .findFirst()
                .orElseThrow(() -> new TestFailException("Cannot find valid instance group with this name: " + hostGroupName));
        String distroxMasterPrivateIp = instanceMetaData.getPrivateIp();
        if (StringUtils.isNotBlank(distroxMasterPrivateIp)) {
            LOGGER.info("Found {} private IP for {} host group!", distroxMasterPrivateIp, hostGroupName);
            privateIps.put(MASTER, distroxMasterPrivateIp);
        } else {
            LOGGER.info("No private IP for {} host group, current instance status is: {}", hostGroupName, instanceMetaData.getInstanceStatus());
        }
    }

    private void refreshResponse(TestContext testContext, CloudbreakClient client) {
        setResponse(client.getDefaultClient(testContext)
                .distroXV1Endpoint()
                .getByName(getName(), new HashSet<>()));
    }

    private Set<InstanceMetaDataV4Response> getInstanceMetaData(String hostGroupName) {
        return getResponse()
                .getInstanceGroups()
                .stream()
                .filter(ig -> ig.getName().equals(hostGroupName))
                .findFirst()
                .orElseThrow(() -> new TestFailException(format("The expected '%s' host group is NOT present at DistroX!", hostGroupName)))
                .getMetadata();
    }

    public DistroXTestDto withSeLinuxSecurity(String seLinux) {
        SecurityV1Request securityRequest = new SecurityV1Request();
        securityRequest.setSeLinux(seLinux);
        getRequest().setSecurity(securityRequest);
        return this;
    }

    public SeLinux getSelinuxMode() {
        return Optional.ofNullable(getResponse())
                .map(StackV4Response::getSecurity)
                .map(SecurityV4Response::getSeLinux)
                .map(s -> SeLinux.fromStringWithFallback(s))
                .orElse(SeLinux.PERMISSIVE);
    }

    public List<String> getAllInstanceIps() {
        refresh();
        return InstanceIPCollectorUtil.getAllInstanceIps(this, false);
    }
}