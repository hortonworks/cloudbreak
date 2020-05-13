package com.sequenceiq.it.cloudbreak.dto.freeipa;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.placement.PlacementSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.VolumeRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.aws.AwsInstanceTemplateParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.aws.AwsInstanceTemplateSpotParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.AwsNetworkParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.MockNetworkParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.region.PlacementRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityGroupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityRuleRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.StackAuthenticationRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.list.ListFreeIpaResponse;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.FreeIPAClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.client.FreeIPATestClient;
import com.sequenceiq.it.cloudbreak.context.Purgable;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractFreeIPATestDto;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.NetworkV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.PlacementSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.StackAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.search.Searchable;

@Prototype
public class FreeIPATestDto extends AbstractFreeIPATestDto<CreateFreeIpaRequest, DescribeFreeIpaResponse, FreeIPATestDto>
        implements Purgable<ListFreeIpaResponse, FreeIPAClient>, Searchable {

    @Inject
    private FreeIPATestClient freeIPATestClient;

    public FreeIPATestDto(TestContext testContext) {
        super(new CreateFreeIpaRequest(), testContext);
    }

    @Override
    public FreeIPATestDto valid() {
        return withName(getResourcePropertyProvider().getName(getCloudPlatform()))
                .withEnvironment(getTestContext().given(EnvironmentTestDto.class))
                .withPlacement(getTestContext().given(PlacementSettingsTestDto.class))
                .withInstanceGroupsEntity(InstanceGroupTestDto.defaultHostGroup(getTestContext()), OptionalInt.empty(), OptionalInt.empty())
                .withNetwork(getTestContext().given(NetworkV4TestDto.class))
                .withGatewayPort(getCloudProvider().gatewayPort(this))

                .withAuthentication(getCloudProvider().stackAuthentication(given(StackAuthenticationTestDto.class)))
                .withFreeIPA("ipatest.local", "ipaserver", "admin1234", "admins");
    }

    public FreeIPATestDto withFreeIpaHa(int instanceGroupCount, int instanceCountByGroup) {
        withInstanceGroupsEntity(InstanceGroupTestDto.defaultHostGroup(getTestContext()),
                OptionalInt.of(instanceGroupCount),
                OptionalInt.of(instanceCountByGroup));
        return this;
    }

    private FreeIPATestDto withFreeIPA(String domain, String hostname, String adminPassword, String adminGroupName) {
        FreeIpaServerRequest request = new FreeIpaServerRequest();
        request.setDomain(domain);
        request.setHostname(hostname);
        request.setAdminPassword(adminPassword);
        request.setAdminGroupName(adminGroupName);
        getRequest().setFreeIpa(request);
        return this;
    }

    private FreeIPATestDto withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public FreeIPATestDto withTelemetry(String telemetry) {
        TelemetryTestDto telemetryTestDto = getTestContext().get(telemetry);
        getRequest().setTelemetry(telemetryTestDto.getRequest());
        return this;
    }

    public FreeIPATestDto withSpotPercentage(int spotPercentage) {
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

    private FreeIPATestDto withPlacement(PlacementSettingsTestDto placementSettings) {
        PlacementSettingsV4Request request = placementSettings.getRequest();
        getRequest().setPlacement(new PlacementRequest()
                .withAvailabilityZone(request.getAvailabilityZone())
                .withRegion(request.getRegion()));
        return this;
    }

    private FreeIPATestDto withInstanceGroupsEntity(Collection<InstanceGroupTestDto> instanceGroups,
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
        return template;
    }

    private FreeIPATestDto withNetwork(NetworkV4TestDto network) {
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
        }
        getRequest().setNetwork(networkRequest);
        return this;
    }

    private FreeIPATestDto withAuthentication(StackAuthenticationTestDto stackAuthentication) {
        StackAuthenticationV4Request request = stackAuthentication.getRequest();
        StackAuthenticationRequest authReq = new StackAuthenticationRequest();
        authReq.setLoginUserName(request.getLoginUserName());
        authReq.setPublicKey(request.getPublicKey());
        authReq.setPublicKeyId(request.getPublicKeyId());
        getRequest().setAuthentication(authReq);
        return this;
    }

    private FreeIPATestDto withEnvironment(EnvironmentTestDto environment) {
        getRequest().setEnvironmentCrn(environment.getResponse().getCrn());
        return this;
    }

    public FreeIPATestDto withCatalog(String catalog) {
        ImageSettingsRequest imageSettingsRequest = new ImageSettingsRequest();
        imageSettingsRequest.setCatalog(catalog);
        getRequest().setImage(imageSettingsRequest);
        return this;
    }

    public FreeIPATestDto await(Status status) {
        return await(status, emptyRunningParameter());
    }

    public FreeIPATestDto await(Status status, RunningParameter runningParameter) {
        return getTestContext().await(this, status, runningParameter);
    }

    public FreeIPATestDto withGatewayPort(Integer port) {
        getRequest().setGatewayPort(port);
        return this;
    }

    @Override
    public CloudbreakTestDto refresh(TestContext context, CloudbreakClient cloudbreakClient) {
        return when(freeIPATestClient.describe(), key("refresh-freeipa-" + getName()));
    }

    @Override
    public Collection<ListFreeIpaResponse> getAll(FreeIPAClient client) {
        return client.getFreeIpaClient().getFreeIpaV1Endpoint().list();
    }

    @Override
    public boolean deletable(ListFreeIpaResponse entity) {
        return entity.getName().startsWith(getResourcePropertyProvider().prefix(getCloudPlatform()));
    }

    @Override
    public void delete(TestContext testContext, ListFreeIpaResponse entity, FreeIPAClient client) {
        client.getFreeIpaClient().getFreeIpaV1Endpoint().delete(entity.getEnvironmentCrn());
    }

    @Override
    public Class<FreeIPAClient> client() {
        return FreeIPAClient.class;
    }

    @Override
    public String getSearchId() {
        return getName();
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up resource with name: {}", getName());
        when(freeIPATestClient.delete(), key("delete-freeipa-" + getName()).withSkipOnFail(false));
    }
}
