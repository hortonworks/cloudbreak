package com.sequenceiq.it.cloudbreak.util.resize;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer.LoadBalancerResponse;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxInstanceGroupDiskRequest;
import com.sequenceiq.sdx.api.model.SdxInstanceGroupRequest;

@Component
@Scope("prototype")
public class SdxResizeTestValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SdxResizeTestValidator.class);

    private SdxClusterShape expectedShape;

    private final AtomicReference<String> expectedCrn;

    private final AtomicReference<String> expectedName;

    private final AtomicReference<String> expectedRuntime;

    private final AtomicReference<Long> expectedCreationTimestamp;

    private final AtomicReference<List<SdxInstanceGroupRequest>> expectedCustomInstanceGroups;

    private final AtomicReference<List<SdxInstanceGroupDiskRequest>> expectedSdxInstanceGroupDiskRequest;

    private boolean expectedMultiAzDatalake;

    private boolean expectedSameShapeResize;

    private String expectedDnsEntry;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private SshJClientActions sshJClientActions;

    public SdxResizeTestValidator() {
        expectedShape = SdxClusterShape.ENTERPRISE;
        expectedCrn = new AtomicReference<>();
        expectedName = new AtomicReference<>();
        expectedRuntime = new AtomicReference<>();
        expectedCreationTimestamp = new AtomicReference<>();
        expectedCustomInstanceGroups = new AtomicReference<>();
        expectedSdxInstanceGroupDiskRequest = new AtomicReference<>();
        expectedMultiAzDatalake = false;
        expectedSameShapeResize = false;
        expectedDnsEntry = "";
        LOGGER.info("Validator instance ID: {}", System.identityHashCode(this));
    }

    public SdxResizeTestValidator(SdxClusterShape expectedShape) {
        this.expectedShape = expectedShape;
        expectedCrn = new AtomicReference<>();
        expectedName = new AtomicReference<>();
        expectedRuntime = new AtomicReference<>();
        expectedCreationTimestamp = new AtomicReference<>();
        expectedCustomInstanceGroups = new AtomicReference<>();
        expectedSdxInstanceGroupDiskRequest = new AtomicReference<>();
        expectedMultiAzDatalake = false;
        expectedSameShapeResize = false;
        expectedDnsEntry = "";
        LOGGER.info("Validator instance ID: {}", System.identityHashCode(this));
    }

    public void setExpectedShape(SdxClusterShape expectedShape) {
        this.expectedShape = expectedShape; }

    public void setExpectedCrn(String expectedCrn) {
        this.expectedCrn.set(expectedCrn);
    }

    public void setExpectedName(String expectedName) {
        this.expectedName.set(expectedName);
    }

    public void setExpectedRuntime(String expectedRuntime) {
        this.expectedRuntime.set(expectedRuntime);
    }

    public void setExpectedCreationTimestamp(Long expectedCreationTimestamp) {
        this.expectedCreationTimestamp.set(expectedCreationTimestamp);
    }

    public void setExpectedCustomInstanceGroups(List<SdxInstanceGroupRequest> expectedCustomInstanceGroups) {
        this.expectedCustomInstanceGroups.set(expectedCustomInstanceGroups);
    }

    public void setExpectedSdxInstanceGroupDiskRequest(List<SdxInstanceGroupDiskRequest> expectedSdxInstanceGroupDiskRequest) {
        this.expectedSdxInstanceGroupDiskRequest.set(expectedSdxInstanceGroupDiskRequest);
    }

    public void setExpectedMultiAzDatalake(Boolean expectedMultiAzDatalake) {
        this.expectedMultiAzDatalake = expectedMultiAzDatalake;
    }

    public void setExpectedSameShapeResize(Boolean expectedSameShapeResize) {
        this.expectedSameShapeResize = expectedSameShapeResize;
    }

    public void setExpectedDnsEntry(String expectedDnsEntry) {
        this.expectedDnsEntry = expectedDnsEntry;
    }

    public void setExpectedDnsEntry(SdxInternalTestDto testDto) {
        List<LoadBalancerResponse> loadBalancers = testDto.getResponse().getStackV4Response().getLoadBalancers();
        LoadBalancerResponse loadBalancerResponse =
                loadBalancers
                        .stream()
                        .filter(Predicate.not(lb -> lb.getType() == LoadBalancerType.OUTBOUND))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Load balancer not found"));
        this.expectedDnsEntry = loadBalancerResponse.getFqdn();
        LOGGER.info("Found expectedDnsEntry: {}", this.expectedDnsEntry);
    }

    public SdxInternalTestDto validateResizedCluster(SdxInternalTestDto dto) {
        SdxClusterDetailResponse response = dto.getResponse();
        validateClusterShape(response.getClusterShape());
        validateCrn(response.getCrn());
        validateStackCrn(response.getStackCrn());
        validateName(response.getName());
        validateRuntime(response.getRuntime());
        validateCustomInstanceGroups(response.getStackV4Response().getInstanceGroups());
        validateCustomInstanceDiskSize(response.getStackV4Response().getInstanceGroups());
        return dto;
    }

    public SdxInternalTestDto validateResizedCluster(SdxInternalTestDto dto, TestContext tc) {
        validateResizedCluster(dto);
        validateMultiAzDatalake(dto, tc);
        validateDiscoveryFqdnResizeSuffix(dto, tc);
        setExpectedDnsEntry(dto);
        validateDnsEntryInFreeIpa(tc);
        return dto;
    }

    private void validateCustomInstanceGroups(List<InstanceGroupV4Response> instanceGroupV4Responses) {
        List<SdxInstanceGroupRequest> customInstanceGroups = expectedCustomInstanceGroups.get();
        if (!CollectionUtils.isEmpty(customInstanceGroups)) {
            customInstanceGroups
                    .forEach(customInstanceGroup ->
                            getInstanceGroupByName(customInstanceGroup.getName(), instanceGroupV4Responses)
                                    .ifPresent(instanceGroup -> {
                                        if (!customInstanceGroup.getInstanceType().equals(instanceGroup.getTemplate().getInstanceType())) {
                                            fail("instanceType " + customInstanceGroup.getName(),
                                                    customInstanceGroup.getInstanceType(), instanceGroup.getTemplate().getInstanceType());
                                        }
                                    }));
        }
    }

    public void preValidateCustomInstanceGroups(SdxInternalTestDto dto) {
        List<InstanceGroupV4Response> instanceGroupV4Responses = dto.getResponse().getStackV4Response().getInstanceGroups();
        List<SdxInstanceGroupRequest> customInstanceGroups = expectedCustomInstanceGroups.get();
        if (!CollectionUtils.isEmpty(customInstanceGroups)) {
            customInstanceGroups
                    .forEach(customInstanceGroup ->
                            getInstanceGroupByName(customInstanceGroup.getName(), instanceGroupV4Responses)
                                    .ifPresent(instanceGroup -> {
                                        if (customInstanceGroup.getInstanceType().equals(instanceGroup.getTemplate().getInstanceType())) {
                                            fail("custom instanceType is already the expected instanceType before resize: " +
                                                            customInstanceGroup.getName(),
                                                    customInstanceGroup.getInstanceType(),
                                                    instanceGroup.getTemplate().getInstanceType());
                                        }
                                    }));
        }
    }

    private void validateCustomInstanceDiskSize(List<InstanceGroupV4Response> instanceGroupV4Responses) {
        List<SdxInstanceGroupDiskRequest> customInstanceGroupDisks = expectedSdxInstanceGroupDiskRequest.get();
        if (!CollectionUtils.isEmpty(customInstanceGroupDisks)) {
            customInstanceGroupDisks
                    .forEach(customInstanceGroupDisk ->
                            getInstanceGroupByName(customInstanceGroupDisk.getName(), instanceGroupV4Responses)
                                    .ifPresent(instanceGroup -> {
                                        Integer actualDiskSize =
                                                instanceGroup
                                                        .getTemplate()
                                                        .getAttachedVolumes()
                                                        .stream()
                                                        .findAny()
                                                        .get()
                                                        .getSize();
                                        if (!actualDiskSize.equals(customInstanceGroupDisk.getInstanceDiskSize())) {
                                            fail("instanceDiskSize " + customInstanceGroupDisk.getName(),
                                                    customInstanceGroupDisk.getInstanceDiskSize().toString(), actualDiskSize.toString());
                                        }
                                    }));
        }
    }

    private void validateMultiAzDatalake(SdxInternalTestDto sdxInternalTestDto, TestContext tc) {
        SdxClusterDetailResponse sdxClusterDetailResponse = sdxInternalTestDto.getResponse();
        if (!expectedMultiAzDatalake) {
            return;
        }
        for (InstanceGroupV4Response instanceGroup : sdxClusterDetailResponse.getStackV4Response().getInstanceGroups()) {
            if (!CollectionUtils.isEmpty(instanceGroup.getMetadata())) {
                Set<String> nodeAvailabilityZones = instanceGroup.getAvailabilityZones();
                LOGGER.info("availabilityZones: {} for {}", nodeAvailabilityZones, instanceGroup.getName());
                if (nodeAvailabilityZones.size() <= 1) {
                    throw new TestFailException(String.format("Node: %s's availabilityZones should have more than one zones",
                            instanceGroup.getName()));
                }
                Map<String, String> instanceZonesMap = instanceGroup.getMetadata()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        InstanceMetaDataV4Response::getInstanceId,
                                        InstanceMetaDataV4Response::getAvailabilityZone));
                if (instanceGroup.getNodeCount() <= 1) {
                    continue;
                }
                validateAzSpreadOnInstances(instanceZonesMap, instanceGroup.getName());
            }
        }
    }

    private void validateAzSpreadOnInstances(Map<String, String> instanceZonesMap, String nodeName) {
        Set<String> usedZones = new HashSet<>();
        for (Map.Entry<String, String> instanceId : instanceZonesMap.entrySet()) {
            usedZones.add(instanceId.getValue());
        }
        if (usedZones.size() <= 1) {
            throw new TestFailException(String.format("Node: %s' does not spread across multiple availability zones", nodeName));
        }
    }

    private void validateDiscoveryFqdnResizeSuffix(SdxInternalTestDto sdxInternalTestDto, TestContext tc) {
        SdxClusterDetailResponse sdxClusterDetailResponse = sdxInternalTestDto.getResponse();
        String clusterName = sdxClusterDetailResponse.getName();
        for (InstanceGroupV4Response instanceGroup : sdxClusterDetailResponse.getStackV4Response().getInstanceGroups()) {
            if (!CollectionUtils.isEmpty(instanceGroup.getMetadata())) {
                Map<String, String> instanceDiscoveryFqdnMap = instanceGroup.getMetadata().stream()
                        .collect(Collectors.toMap(
                                InstanceMetaDataV4Response::getInstanceId,
                                InstanceMetaDataV4Response::getDiscoveryFQDN));
                instanceDiscoveryFqdnMap.forEach((instanceId, discoveryFqdn) -> {
                    String expectedClusterNameWithResizeSuffix = clusterName + expectedShape.getResizeSuffix();
                    if (expectedSameShapeResize) {
                        expectedClusterNameWithResizeSuffix += "-az";
                    }
                    if (!discoveryFqdn.startsWith(expectedClusterNameWithResizeSuffix)) {
                        throw new TestFailException(String.format("InstanceId: %s's discovery fqdn %s does not starts with: %s",
                                instanceId, discoveryFqdn, expectedClusterNameWithResizeSuffix));
                    }
                });
            }
        }
    }

    private void validateDnsEntryInFreeIpa(TestContext testContext) {
        testContext
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .then((tc, testDto, client) -> {
                    validateDnsEntryInFreeIpa(testDto, tc, client);
                return testDto;
                });
    }

    public void validateDnsEntryInFreeIpa(FreeIpaTestDto freeIpaTestDto, TestContext tc, FreeIpaClient freeipaClient) {
        int firstDotIndex = this.expectedDnsEntry.indexOf('.');
        String cName = this.expectedDnsEntry.substring(0, firstDotIndex);
        String dnsZone = this.expectedDnsEntry.substring(firstDotIndex + 1);
        sshJClientActions.validateDnsZoneAndCnameEntryInFreeIpa(freeIpaTestDto, tc, freeipaClient, dnsZone, cName);
    }

    private Optional<InstanceGroupV4Response> getInstanceGroupByName(String name, List<InstanceGroupV4Response> instanceGroupV4Responses) {
        return instanceGroupV4Responses
                .stream()
                .filter(instanceGroup -> StringUtils.equals(instanceGroup.getName(), name))
                .findAny();
    }

    public SdxInternalTestDto validateRecoveredCluster(SdxInternalTestDto dto) {
        validateResizedCluster(dto);
        SdxClusterResponse response = dto.getResponse();
        validateNotDetached(response.isDetached());
        validateCreationTimestamp(response.getCreated());
        return dto;
    }

    private void validateClusterShape(SdxClusterShape shape) {
        if (!expectedShape.equals(shape)) {
            fail("cluster shape", expectedShape.name(), shape.name());
        }
    }

    private void validateCrn(String crn) {
        if (!expectedCrn.get().equals(crn)) {
            fail("crn", expectedCrn.get(), crn);
        }
    }

    private void validateStackCrn(String stackCrn) {
        if (!expectedCrn.get().equals(stackCrn)) {
            fail("stack crn", expectedCrn.get(), stackCrn);
        }
    }

    private void validateName(String name) {
        if (!expectedName.get().equals(name)) {
            fail("name", expectedName.get(), name);
        }
    }

    private void validateRuntime(String runtime) {
        if (!expectedRuntime.get().equals(runtime)) {
            fail("runtime", expectedRuntime.get(), runtime);
        }
    }

    private void validateNotDetached(boolean detached) {
        if (detached) {
            fail("detached", "false", "true");
        }
    }

    private void validateCreationTimestamp(Long creationTimestamp) {
        if (!expectedCreationTimestamp.get().equals(creationTimestamp)) {
            fail("creation timestamp", expectedCreationTimestamp.get().toString(), creationTimestamp.toString());
        }
    }

    private void fail(String testField, String expected, String actual) {
        throw new TestFailException(
                " The DL's field '" + testField + "' is '" + actual + "' instead of '" + expected + '\''
        );
    }
}
