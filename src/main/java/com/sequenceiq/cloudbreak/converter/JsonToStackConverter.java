package com.sequenceiq.cloudbreak.converter;

import static com.sequenceiq.cloudbreak.domain.InstanceGroupType.GATEWAY;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.amazonaws.regions.Regions;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.json.InstanceGroupJson;
import com.sequenceiq.cloudbreak.controller.json.StackJson;
import com.sequenceiq.cloudbreak.domain.AdjustmentType;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.Subnet;
import com.sequenceiq.cloudbreak.domain.SubnetJson;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;
import com.sequenceiq.cloudbreak.service.network.SecurityService;
import com.sequenceiq.cloudbreak.service.stack.flow.ConsulUtils;

@Component
public class JsonToStackConverter extends AbstractConversionServiceAwareConverter<StackJson, Stack> {

    private static final double ONE_HUNDRED = 100.0;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private SecurityService securityService;

    @Value("${cb.aws.ami.map:eu-west-1:ami-f3670284,us-east-1:ami-f8477690,ap-northeast-1:ami-34cd3134,sa-east-1:ami-29e45e34,ap-southeast-1:ami-8a2c1fd8,"
            + "ap-southeast-2:ami-6132405b,us-west-2:ami-57c7ee67,us-west-1:ami-8d42a2c9}")
    private String awsImage;

    @Value("${cb.gcp.source.image.path:sequenceiqimage/sequenceiq-ambari17-consul-centos-2015-04-02-1413.image.tar.gz}")
    private String gcpImage;

    @Value("${cb.azure.image.uri:https://102589fae040d8westeurope.blob.core.windows.net/images/"
            + "packer-cloudbreak-2015-04-02-centos6-reset_2015-April-2_13-36-os-2015-04-02.vhd}")
    private String azureImage;

    @Value("${cb.openstack.image:cloudbreak-centos-amb17-2015-04-02}")
    private String openStackImage;

    @Override
    public Stack convert(StackJson source) {
        Stack stack = new Stack();
        stack.setName(source.getName());
        stack.setUserName(source.getUserName());
        stack.setPassword(source.getPassword());
        stack.setPublicInAccount(source.isPublicInAccount());
        stack.setRegion(source.getRegion());
        stack.setOnFailureActionAction(source.getOnFailureAction());
        if (source.getAllowedSubnets() != null) {
            stack.setAllowedSubnets(convertSubnets(source.getAllowedSubnets()));
        }
        stack.addAllowedSubnets(securityService.getCloudbreakSubnets(stack));
        try {
            stack.setCredential(credentialRepository.findOne(source.getCredentialId()));
        } catch (AccessDeniedException e) {
            throw new AccessDeniedException(String.format("Access to credential '%s' is denied or credential doesn't exist", source.getCredentialId()), e);
        }
        stack.setStatus(Status.REQUESTED);
        stack.setInstanceGroups(convertInstanceGroups(source.getInstanceGroups()));
        if (stack.getInstanceGroupsByType(GATEWAY).isEmpty()) {
            throw new BadRequestException("Gateway instance group not configured");
        }
        int minNodeCount = ConsulUtils.ConsulServers.NODE_COUNT_LOW.getMin();
        int fullNodeCount = stack.getFullNodeCount();
        if (fullNodeCount < minNodeCount) {
            throw new BadRequestException(String.format("At least %s nodes are required to launch the stack", minNodeCount));
        }
        if (source.getImage() != null) {
            stack.setImage(source.getImage());
        } else {
            stack.setImage(prepareImage(stack));
        }
        int consulServers = getConsulServerCount(source, fullNodeCount);
        stack.setConsulServers(consulServers);
        if (source.getFailurePolicy() != null) {
            stack.setFailurePolicy(getConversionService().convert(source.getFailurePolicy(), FailurePolicy.class));
            FailurePolicy failurePolicy = stack.getFailurePolicy();
            validatFailurePolicy(stack, consulServers, failurePolicy);
        }
        stack.setParameters(source.getParameters());
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

    private Set<Subnet> convertSubnets(List<SubnetJson> source) {
        return (Set<Subnet>) getConversionService().convert(source,
                TypeDescriptor.forObject(source),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(Subnet.class)));
    }

    private Set<InstanceGroup> convertInstanceGroups(List<InstanceGroupJson> instanceGroupJsons) {
        return (Set<InstanceGroup>) getConversionService().convert(instanceGroupJsons, TypeDescriptor.forObject(instanceGroupJsons),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(InstanceGroup.class)));
    }
}
