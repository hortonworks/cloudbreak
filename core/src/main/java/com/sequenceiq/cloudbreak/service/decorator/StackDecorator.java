package com.sequenceiq.cloudbreak.service.decorator;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.regions.Regions;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.AdjustmentType;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;
import com.sequenceiq.cloudbreak.service.network.SecurityService;
import com.sequenceiq.cloudbreak.service.stack.flow.ConsulUtils;

@Service
public class StackDecorator implements Decorator<Stack> {

    private static final double ONE_HUNDRED = 100.0;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private SecurityService securityService;

    @Value("${cb.azure.image.uri:https://102589fae040d8westeurope.blob.core.windows.net/images/"
            + "cb-centos66-amb200-2015-04-18_2015-April-18_14-11-os-2015-04-18.vhd}")
    private String azureImage;

    @Value("${cb.aws.ami.map:ap-northeast-1:ami-b8ad69b8,sa-east-1:ami-5f53d642,ap-southeast-1:ami-76350824,eu-west-1:ami-11345766,"
            + "ap-southeast-2:ami-5148356b,us-east-1:ami-fa181c92,us-west-1:ami-bd5dbff9,us-west-2:ami-fb1326cb}")
    private String awsImage;

    @Value("${cb.openstack.image:cb-centos66-amb200-2015-04-19}")
    private String openStackImage;

    @Value("${cb.gcp.source.image.path:sequenceiqimage/cb-centos66-amb200-2-2015-04-20-1027.image.tar.gz}")
    private String gcpImage;

    private enum DecorationData {
        CREDENTIAL_ID,
        USR_CONSUL_SERVER_COUNT
    }

    @Override
    public Stack decorate(Stack subject, Object... data) {
        subject.addAllowedSubnets(securityService.getCloudbreakSubnets(subject));
        subject.setCredential(credentialRepository.findOne((Long) data[DecorationData.CREDENTIAL_ID.ordinal()]));
        int consulServers = getConsulServerCount((Integer) data[DecorationData.USR_CONSUL_SERVER_COUNT.ordinal()], subject.getFullNodeCount());
        subject.setConsulServers(consulServers);

        if (subject.getFailurePolicy() != null) {
            validatFailurePolicy(subject, consulServers, subject.getFailurePolicy());
        }

        if (subject.getImage() == null) {
            subject.setImage(prepareImage(subject));
        }
        validate(subject);
        return subject;
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

    private void validate(Stack stack) {
        if (stack.getGatewayInstanceGroup() == null) {
            throw new BadRequestException("Gateway instance group not configured");
        }
        int minNodeCount = ConsulUtils.ConsulServers.NODE_COUNT_LOW.getMin();
        int fullNodeCount = stack.getFullNodeCount();
        if (fullNodeCount < minNodeCount) {
            throw new BadRequestException(String.format("At least %s nodes are required to launch the stack", minNodeCount));
        }
    }

    private int getConsulServerCount(Integer userDefinedConsulServers, int fullNodeCount) {
        int consulServers = userDefinedConsulServers == null ? ConsulUtils.getConsulServerCount(fullNodeCount) : userDefinedConsulServers;
        if (consulServers > fullNodeCount || consulServers < 1) {
            throw new BadRequestException("Invalid consul server specification: must be in range 1-" + fullNodeCount);
        }
        return consulServers;
    }

    private long calculateMinCount(Stack stack) {
        return Math.round(Double.valueOf(stack.getFullNodeCount()) * (Double.valueOf(stack.getFailurePolicy().getThreshold()) / ONE_HUNDRED));
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

}
