package com.sequenceiq.cloudbreak.converter;

import static com.sequenceiq.cloudbreak.domain.InstanceGroupType.GATEWAY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.amazonaws.regions.Regions;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.json.ClusterResponse;
import com.sequenceiq.cloudbreak.controller.json.InstanceGroupJson;
import com.sequenceiq.cloudbreak.controller.json.StackJson;
import com.sequenceiq.cloudbreak.domain.AdjustmentType;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;
import com.sequenceiq.cloudbreak.service.network.SecurityService;
import com.sequenceiq.cloudbreak.service.stack.flow.ConsulUtils;

@Component
public class StackConverter extends AbstractConverter<StackJson, Stack> {

    private static final double ONE_HUNDRED = 100.0;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private ClusterConverter clusterConverter;

    @Autowired
    private InstanceGroupConverter instanceGroupConverter;

    @Autowired
    private MetaDataConverter metaDataConverter;

    @Autowired
    private FailurePolicyConverter failurePolicyConverter;

    @Autowired
    private SubnetConverter subnetConverter;

    @Autowired
    private SecurityService securityService;

    @Value("${cb.aws.ami.map}")
    private String awsImage;

    @Value("${cb.gcp.source.image.path}")
    private String gcpImage;

    @Value("${cb.azure.image.uri}")
    private String azureImage;

    @Value("${cb.openstack.image}")
    private String openStackImage;

    @Override
    public StackJson convert(Stack entity) {
        StackJson stackJson = new StackJson();
        stackJson.setName(entity.getName());
        stackJson.setOwner(entity.getOwner());
        stackJson.setAccount(entity.getAccount());
        stackJson.setPublicInAccount(entity.isPublicInAccount());
        stackJson.setId(entity.getId());
        if (entity.getCredential() == null) {
            stackJson.setCloudPlatform(null);
            stackJson.setCredentialId(null);
        } else {
            stackJson.setCloudPlatform(entity.cloudPlatform());
            stackJson.setCredentialId(entity.getCredential().getId());
        }
        stackJson.setStatus(entity.getStatus());
        stackJson.setStatusReason(entity.getStatusReason());
        stackJson.setAmbariServerIp(entity.getAmbariIp());
        stackJson.setUserName(entity.getUserName());
        stackJson.setPassword(entity.getPassword());
        stackJson.setHash(entity.getHash());
        stackJson.setConsulServerCount(entity.getConsulServers());
        stackJson.setRegion(entity.getRegion());
        stackJson.setOnFailureAction(entity.getOnFailureActionAction());
        stackJson.setAllowedSubnets(new ArrayList<>(subnetConverter.convertAllEntityToJson(entity.getAllowedSubnets())));
        List<InstanceGroupJson> templateGroups = new ArrayList<>();
        templateGroups.addAll(instanceGroupConverter.convertAllEntityToJson(entity.getInstanceGroups()));
        stackJson.setInstanceGroups(templateGroups);
        if (entity.getCluster() != null) {
            stackJson.setCluster(clusterConverter.convert(entity.getCluster(), "{}"));
        } else {
            stackJson.setCluster(new ClusterResponse());
        }
        if (entity.getFailurePolicy() != null) {
            stackJson.setFailurePolicy(failurePolicyConverter.convert(entity.getFailurePolicy()));
        }
        stackJson.setImage(entity.getImage());
        stackJson.setParameters(entity.getParameters());
        return stackJson;
    }

    @Override
    public Stack convert(StackJson json) {
        Stack stack = new Stack();
        stack.setName(json.getName());
        stack.setUserName(json.getUserName());
        stack.setPassword(json.getPassword());
        stack.setPublicInAccount(json.isPublicInAccount());
        stack.setRegion(json.getRegion());
        stack.setOnFailureActionAction(json.getOnFailureAction());
        if (json.getAllowedSubnets() != null) {
            stack.setAllowedSubnets(subnetConverter.convertAllJsonToEntity(json.getAllowedSubnets(), stack));
        }
        stack.addAllowedSubnets(securityService.getCloudbreakSubnets(stack));
        try {
            stack.setCredential(credentialRepository.findOne(json.getCredentialId()));
        } catch (AccessDeniedException e) {
            throw new AccessDeniedException(String.format("Access to credential '%s' is denied or credential doesn't exist", json.getCredentialId()), e);
        }
        stack.setStatus(Status.REQUESTED);
        stack.setInstanceGroups(instanceGroupConverter.convertAllJsonToEntity(json.getInstanceGroups(), stack));
        if (stack.getInstanceGroupsByType(GATEWAY).isEmpty()) {
            throw new BadRequestException("Gateway instance group not configured");
        }
        int minNodeCount = ConsulUtils.ConsulServers.NODE_COUNT_LOW.getMin();
        int fullNodeCount = stack.getFullNodeCount();
        if (fullNodeCount < minNodeCount) {
            throw new BadRequestException(String.format("At least %s nodes are required to launch the stack", minNodeCount));
        }
        if (json.getImage() != null) {
            stack.setImage(json.getImage());
        } else {
            stack.setImage(prepareImage(stack));
        }
        int consulServers = getConsulServerCount(json, fullNodeCount);
        stack.setConsulServers(consulServers);
        if (json.getFailurePolicy() != null) {
            stack.setFailurePolicy(failurePolicyConverter.convert(json.getFailurePolicy()));
            FailurePolicy failurePolicy = stack.getFailurePolicy();
            validatFailurePolicy(stack, consulServers, failurePolicy);
        }
        stack.setParameters(json.getParameters());
        return stack;
    }

    private void validatFailurePolicy(Stack stack, int consulServers, FailurePolicy failurePolicy) {
        if (failurePolicy.getThreshold() == 0L && !AdjustmentType.BEST_EFFORT.equals(failurePolicy.getAdjustmentType())) {
            throw new BadRequestException("The threshold can not be 0");
        }
        String errorMsg = String.format("At least %s live nodes are required after rollback", consulServers);
        if (AdjustmentType.EXACT.equals(failurePolicy.getAdjustmentType())) {
            validateExactCount(stack, consulServers, failurePolicy, errorMsg);
        } else if (AdjustmentType.PERCENTAGE.equals(failurePolicy.getAdjustmentType())) {
            validatePercentageCount(stack, consulServers, failurePolicy, errorMsg);
        }
    }

    private void validatePercentageCount(Stack stack, int consulServers, FailurePolicy failurePolicy, String errorMsg) {
        if (calculateMinCount(stack) < consulServers) {
            throw new BadRequestException(errorMsg);
        } else if (failurePolicy.getThreshold() < 0L || failurePolicy.getThreshold() > ONE_HUNDRED) {
            throw new BadRequestException("The percentage of the threshold has to be between 0 an 100.");
        }
    }

    private void validateExactCount(Stack stack, int consulServers, FailurePolicy failurePolicy, String errorMsg) {
        if (failurePolicy.getThreshold() < consulServers) {
            throw new BadRequestException(errorMsg);
        } else if (failurePolicy.getThreshold() > stack.getFullNodeCount()) {
            throw new BadRequestException("Threshold can not be higher than the nodecount of the stack.");
        }
    }

    private int getConsulServerCount(StackJson json, int fullNodeCount) {
        Integer userDefinedConsulServers = json.getConsulServerCount();
        int consulServers = userDefinedConsulServers == null ? ConsulUtils.getConsulServerCount(fullNodeCount) : userDefinedConsulServers;
        if (consulServers > fullNodeCount || consulServers < 1) {
            throw new BadRequestException("Invalid consul server specification: must be in range 1-" + fullNodeCount);
        }
        return consulServers;
    }

    private long calculateMinCount(Stack stack) {
        return Math.round(Double.valueOf(stack.getFullNodeCount()) * (Double.valueOf(stack.getFailurePolicy().getThreshold()) / ONE_HUNDRED));
    }

    private String prepareImage(Stack stack) {
        switch (stack.cloudPlatform()) {
            case AWS:
                return prepareAmis().get(Regions.valueOf(stack.getRegion()).getName());
            case AZURE:
                return azureImage;
            case GCC:
                return gcpImage;
            case OPENSTACK:
                return openStackImage;
            default:
                throw new BadRequestException(String.format("Not supported cloud platform: %s", stack.cloudPlatform()));
        }
    }

    private Map<String, String> prepareAmis() {
        Map<String, String> amisMap = new HashMap<>();
        for (String s : awsImage.split(",")) {
            amisMap.put(s.split(":")[0], s.split(":")[1]);
        }
        return amisMap;
    }

    public Stack convert(StackJson json, boolean publicInAccount) {
        Stack stack = convert(json);
        stack.setPublicInAccount(publicInAccount);
        return stack;
    }

    public Map<String, Object> convertStackStatus(Stack stack) {
        Map<String, Object> stackStatus = new HashMap<>();
        stackStatus.put("id", stack.getId());
        stackStatus.put("status", stack.getStatus().name());
        Cluster cluster = stack.getCluster();
        if (cluster != null) {
            stackStatus.put("clusterStatus", cluster.getStatus().name());
        }
        return stackStatus;
    }
}
