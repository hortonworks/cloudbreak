package com.sequenceiq.it.cloudbreak.dto.freeipa;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.testng.util.Strings;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4SpotParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.placement.PlacementSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.VolumeRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.aws.AwsInstanceTemplateParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.aws.AwsInstanceTemplateSpotParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.AwsNetworkParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.AzureNetworkParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.GcpNetworkParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.MockNetworkParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.region.PlacementRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityGroupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityRuleRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.StackAuthenticationRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.list.ListFreeIpaResponse;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.context.Clue;
import com.sequenceiq.it.cloudbreak.context.Investigable;
import com.sequenceiq.it.cloudbreak.context.Purgable;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractFreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.NetworkV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.PlacementSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.StackAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.search.Searchable;
import com.sequenceiq.it.cloudbreak.util.FreeIpaInstanceUtil;

@Prototype
public class FreeIpaTestDto extends AbstractFreeIpaTestDto<CreateFreeIpaRequest, DescribeFreeIpaResponse, FreeIpaTestDto>
        implements Purgable<ListFreeIpaResponse, FreeIpaClient>, Searchable, Investigable {

    private static final String FREEIPA_RESOURCE_NAME = "freeipaName";

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    public FreeIpaTestDto(TestContext testContext) {
        super(new CreateFreeIpaRequest(), testContext);
    }

    @Override
    public FreeIpaTestDto valid() {
        return withName(getResourcePropertyProvider().getName(getCloudPlatform()))
                .withEnvironment(getTestContext().given(EnvironmentTestDto.class))
                .withPlacement(getTestContext().given(PlacementSettingsTestDto.class))
                .withInstanceGroupsEntity(InstanceGroupTestDto.defaultHostGroup(getTestContext()), OptionalInt.empty(), OptionalInt.empty())
                .withNetwork(getTestContext().given(NetworkV4TestDto.class))
                .withGatewayPort(getCloudProvider().gatewayPort(this))
                .withAuthentication(getCloudProvider().stackAuthentication(given(StackAuthenticationTestDto.class)))
                .withFreeIpa("ipatest.local", "ipaserver", "admin1234", "admins")
                .withCatalog(getCloudProvider().getFreeIpaImageCatalogUrl());
    }

    public FreeIpaTestDto withFreeIpaHa(int instanceGroupCount, int instanceCountByGroup) {
        withInstanceGroupsEntity(InstanceGroupTestDto.defaultHostGroup(getTestContext()),
                OptionalInt.of(instanceGroupCount),
                OptionalInt.of(instanceCountByGroup));
        return this;
    }

    private FreeIpaTestDto withFreeIpa(String domain, String hostname, String adminPassword, String adminGroupName) {
        FreeIpaServerRequest request = new FreeIpaServerRequest();
        request.setDomain(domain);
        request.setHostname(hostname);
        request.setAdminPassword(adminPassword);
        request.setAdminGroupName(adminGroupName);
        getRequest().setFreeIpa(request);
        return this;
    }

    private FreeIpaTestDto withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    @Override
    public String getResourceNameType() {
        return FREEIPA_RESOURCE_NAME;
    }

    @Override
    public String getCrn() {
        return getResponse().getCrn();
    }

    public FreeIpaTestDto withTelemetry(String telemetry) {
        TelemetryTestDto telemetryTestDto = getTestContext().get(telemetry);
        getRequest().setTelemetry(telemetryTestDto.getRequest());
        return this;
    }

    public FreeIpaTestDto withSpotPercentage(int spotPercentage) {
        getRequest().getInstanceGroups().forEach(instanceGroupRequest -> {
            AwsInstanceTemplateParameters aws = instanceGroupRequest.getInstanceTemplate().getAws();
            if (Objects.isNull(aws)) {
                aws = new AwsInstanceTemplateParameters();
                instanceGroupRequest.getInstanceTemplate().setAws(aws);
            }
            AwsInstanceTemplateSpotParameters spot = new AwsInstanceTemplateSpotParameters();
            spot.setPercentage(spotPercentage);
            aws.setSpot(spot);
        });
        return this;
    }

    private FreeIpaTestDto withPlacement(PlacementSettingsTestDto placementSettings) {
        PlacementSettingsV4Request request = placementSettings.getRequest();
        getRequest().setPlacement(new PlacementRequest()
                .withAvailabilityZone(request.getAvailabilityZone())
                .withRegion(request.getRegion()));
        return this;
    }

    private FreeIpaTestDto withInstanceGroupsEntity(Collection<InstanceGroupTestDto> instanceGroups,
            OptionalInt instanceGroupCount, OptionalInt instanceCountByGroup) {
        List<InstanceGroupRequest> instanceGroupRequests = instanceGroups.stream()
                .filter(instanceGroupTestDto -> "master".equals(instanceGroupTestDto.getRequest().getName()))
                .limit(1)
                .map(InstanceGroupTestDto::getRequest)
                .map(mapInstanceGroupRequest(instanceCountByGroup))
                .collect(Collectors.toList());
        if (instanceGroupCount.isPresent() && instanceGroupRequests.size() == 1) {
            InstanceGroupRequest reqToCopyDataFrom = instanceGroupRequests.get(0);
            instanceGroupRequests.clear();
            for (int i = 0; i < instanceGroupCount.getAsInt(); ++i) {
                InstanceGroupRequest req = new InstanceGroupRequest();
                req.setNodeCount(reqToCopyDataFrom.getNodeCount());
                req.setName(reqToCopyDataFrom.getName() + i);
                req.setType(reqToCopyDataFrom.getType());
                req.setInstanceTemplateRequest(reqToCopyDataFrom.getInstanceTemplate());
                req.setSecurityGroup(reqToCopyDataFrom.getSecurityGroup());
                instanceGroupRequests.add(req);
            }
        }
        getRequest().setInstanceGroups(instanceGroupRequests);
        return this;
    }

    private Function<InstanceGroupV4Request, InstanceGroupRequest> mapInstanceGroupRequest(OptionalInt instanceCountByGroup) {
        return request -> {
            InstanceGroupRequest instanceGroupRequest = new InstanceGroupRequest();
            instanceGroupRequest.setNodeCount(instanceCountByGroup.orElse(request.getNodeCount()));
            instanceGroupRequest.setName("master");
            instanceGroupRequest.setType(InstanceGroupType.MASTER);
            instanceGroupRequest.setInstanceTemplateRequest(mapInstanceTemplateRequest(request));
            instanceGroupRequest.setSecurityGroup(mapSecurityGroupRequest(request));
            return instanceGroupRequest;
        };
    }

    private SecurityGroupRequest mapSecurityGroupRequest(InstanceGroupV4Request request) {
        SecurityGroupRequest securityGroup = new SecurityGroupRequest();
        securityGroup.setSecurityRules(request.getSecurityGroup().getSecurityRules()
                .stream()
                .map(sgreq -> {
                    SecurityRuleRequest rule = new SecurityRuleRequest();
                    rule.setModifiable(sgreq.isModifiable());
                    rule.setPorts(sgreq.getPorts());
                    rule.setProtocol(sgreq.getProtocol());
                    rule.setSubnet(sgreq.getSubnet());
                    return rule;
                })
                .collect(Collectors.toList()));
        securityGroup.setSecurityGroupIds(request.getSecurityGroup().getSecurityGroupIds());
        return securityGroup;
    }

    private InstanceTemplateRequest mapInstanceTemplateRequest(InstanceGroupV4Request request) {
        InstanceTemplateRequest template = new InstanceTemplateRequest();
        template.setInstanceType(request.getTemplate().getInstanceType());
        template.setAttachedVolumes(request.getTemplate().getAttachedVolumes()
                .stream()
                .map(volreq -> {
                    VolumeRequest volumeRequest = new VolumeRequest();
                    volumeRequest.setCount(volreq.getCount());
                    volumeRequest.setSize(volreq.getSize());
                    volumeRequest.setType(volreq.getType());
                    return volumeRequest;
                })
                .collect(Collectors.toSet()));
        Optional.ofNullable(request.getTemplate().getAws())
                .map(AwsInstanceTemplateV4Parameters::getSpot)
                .map(AwsInstanceTemplateV4SpotParameters::getPercentage)
                .ifPresent(spotPercentage -> {
                    AwsInstanceTemplateParameters awsInstanceTemplateParameters = new AwsInstanceTemplateParameters();
                    AwsInstanceTemplateSpotParameters awsInstanceTemplateSpotParameters = new AwsInstanceTemplateSpotParameters();
                    awsInstanceTemplateSpotParameters.setPercentage(spotPercentage);
                    awsInstanceTemplateParameters.setSpot(awsInstanceTemplateSpotParameters);
                    template.setAws(awsInstanceTemplateParameters);
                });
        return template;
    }

    public FreeIpaTestDto withNetwork() {
        getRequest().setNetwork(getCloudProvider().networkRequest(this));
        return this;
    }

    private FreeIpaTestDto withNetwork(NetworkV4TestDto network) {
        NetworkV4Request request = network.getRequest();
        NetworkRequest networkRequest = new NetworkRequest();
        if (request.getAws() != null) {
            AwsNetworkParameters params = new AwsNetworkParameters();
            params.setSubnetId(request.getAws().getSubnetId());
            params.setVpcId(request.getAws().getVpcId());
            networkRequest.setAws(params);
        } else if (request.getMock() != null) {
            MockNetworkParameters parameters = new MockNetworkParameters();
            parameters.setSubnetId(request.getMock().getSubnetId());
            parameters.setVpcId(request.getMock().getVpcId());
            parameters.setInternetGatewayId(request.getMock().getInternetGatewayId());
            networkRequest.setMock(parameters);
        } else if (request.getGcp() != null) {
            GcpNetworkParameters gcp = new GcpNetworkParameters();
            gcp.setNetworkId(request.getGcp().getNetworkId());
            gcp.setSubnetId(request.getGcp().getSubnetId());
            gcp.setNoFirewallRules(request.getGcp().getNoFirewallRules());
            gcp.setNoPublicIp(request.getGcp().getNoPublicIp());
            gcp.setSharedProjectId(request.getGcp().getSharedProjectId());
            networkRequest.setGcp(gcp);
        } else if (request.getAzure() != null) {
            AzureNetworkParameters azure = new AzureNetworkParameters();
            azure.setNetworkId(request.getAzure().getNetworkId());
            azure.setNoPublicIp(request.getAzure().getNoPublicIp());
            azure.setSubnetId(request.getAzure().getSubnetId());
            azure.setResourceGroupName(request.getAzure().getResourceGroupName());
            networkRequest.setAzure(azure);
        }
        getRequest().setNetwork(networkRequest);
        return this;
    }

    private FreeIpaTestDto withAuthentication(StackAuthenticationTestDto stackAuthentication) {
        StackAuthenticationV4Request request = stackAuthentication.getRequest();
        StackAuthenticationRequest authReq = new StackAuthenticationRequest();
        authReq.setLoginUserName(request.getLoginUserName());
        authReq.setPublicKey(request.getPublicKey());
        authReq.setPublicKeyId(request.getPublicKeyId());
        getRequest().setAuthentication(authReq);
        return this;
    }

    public FreeIpaTestDto withEnvironment(EnvironmentTestDto environment) {
        getRequest().setEnvironmentCrn(environment.getResponse().getCrn());
        return this;
    }

    public FreeIpaTestDto withEnvironment(String key) {
        EnvironmentTestDto environment = getTestContext().get(key);
        return withEnvironment(environment);
    }

    public FreeIpaTestDto withEnvironment() {
        EnvironmentTestDto environment = getTestContext().get(EnvironmentTestDto.class);
        return withEnvironment(environment);
    }

    public FreeIpaTestDto withCatalog(String catalog) {
        ImageSettingsRequest imageSettingsRequest = new ImageSettingsRequest();
        imageSettingsRequest.setCatalog(catalog);
        getRequest().setImage(imageSettingsRequest);
        return this;
    }

    public FreeIpaTestDto withCatalog(String imageCatalog, String imageUuid) {
        if (!Strings.isNullOrEmpty(imageCatalog) && !Strings.isNullOrEmpty(imageUuid)) {
            LOGGER.info("Using catalog [{}] and image [{}] for creating FreeIPA", imageCatalog, imageUuid);
            ImageSettingsRequest imageSettingsRequest = new ImageSettingsRequest();
            imageSettingsRequest.setCatalog(imageCatalog);
            imageSettingsRequest.setId(imageUuid);

            getRequest().setImage(imageSettingsRequest);
        } else {
            LOGGER.warn("Catalog [{}] or image [{}] is null or empty", imageCatalog, imageUuid);
        }
        return this;
    }

    private boolean checkResponseHasInstanceGroups() {
        return getResponse() != null && getResponse().getInstanceGroups() != null;
    }

    private Map<List<String>, com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus> getInstanceStatusMapIfAvailableInResponse(
            Supplier<Map<List<String>, com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus>> instanceStatusMapSupplier) {
        if (checkResponseHasInstanceGroups()) {
            return instanceStatusMapSupplier.get();
        } else {
            LOGGER.info("Response doesn't has instance groups");
            return Collections.emptyMap();
        }
    }

    public FreeIpaTestDto awaitForHealthyInstances() {
        Map<List<String>, com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus> instanceStatusMap =
                getInstanceStatusMapIfAvailableInResponse(() -> FreeIpaInstanceUtil.getInstanceStatusMap(getResponse()));
        return awaitForFreeIpaInstance(instanceStatusMap);
    }

    public FreeIpaTestDto awaitForFreeIpaInstance(Map<List<String>,
            com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus> statuses) {
        return awaitForFreeIpaInstance(statuses, emptyRunningParameter());
    }

    public FreeIpaTestDto awaitForFreeIpaInstance(FreeIpaTestDto entity, Map<List<String>,
            com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus> statuses, RunningParameter runningParameter) {
        return getTestContext().awaitForInstance(entity, statuses, runningParameter);
    }

    public FreeIpaTestDto awaitForFreeIpaInstance(Map<List<String>, com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus> statuses,
            RunningParameter runningParameter) {
        return getTestContext().awaitForInstance(this, statuses, runningParameter);
    }

    public FreeIpaTestDto withUpgradeCatalogAndImage() {
        return withCatalog(getCloudProvider().getFreeIpaUpgradeImageCatalog(), getCloudProvider().getFreeIpaUpgradeImageId());
    }

    public FreeIpaTestDto await(Status status) {
        return await(status, emptyRunningParameter());
    }

    public FreeIpaTestDto await(Status status, RunningParameter runningParameter) {
        return getTestContext().await(this, Map.of("status", status), runningParameter);
    }

    public FreeIpaTestDto withGatewayPort(Integer port) {
        getRequest().setGatewayPort(port);
        return this;
    }

    @Override
    public CloudbreakTestDto refresh() {
        LOGGER.info("Refresh FreeIPA with name: {}", getName());
        return when(freeIpaTestClient.refresh(), key("refresh-freeipa-" + getName()));
    }

    @Override
    public Collection<ListFreeIpaResponse> getAll(FreeIpaClient client) {
        return client.getDefaultClient().getFreeIpaV1Endpoint().list();
    }

    @Override
    public boolean deletable(ListFreeIpaResponse entity) {
        return entity.getName().startsWith(getResourcePropertyProvider().prefix(getCloudPlatform()));
    }

    @Override
    public void delete(TestContext testContext, ListFreeIpaResponse entity, FreeIpaClient client) {
        client.getDefaultClient().getFreeIpaV1Endpoint().delete(entity.getEnvironmentCrn(), false);
    }

    @Override
    public Class<FreeIpaClient> client() {
        return FreeIpaClient.class;
    }

    @Override
    public String getSearchId() {
        return getName();
    }

    @Override
    public void cleanUp(TestContext context, MicroserviceClient cloudbreakClient) {
        LOGGER.info("Cleaning up freeIpa with name: {}", getName());
        if (getResponse() != null) {
            when(freeIpaTestClient.delete(), key("delete-freeipa-" + getName()).withSkipOnFail(false));
            await(DELETE_COMPLETED, new RunningParameter().withSkipOnFail(true));
        } else {
            LOGGER.info("FreeIpa: {} response is null!", getName());
        }
    }

    @Override
    public Clue investigate() {
        if (getResponse() == null) {
            return null;
        }
        boolean hasSpotTermination = (getResponse().getInstanceGroups() == null) ? false : getResponse().getInstanceGroups().stream()
                .flatMap(ig -> ig.getMetaData().stream())
                .anyMatch(metadata -> InstanceStatus.DELETED_BY_PROVIDER == metadata.getInstanceStatus());
        return new Clue("FreeIpa", null, getResponse(), hasSpotTermination);
    }
}
