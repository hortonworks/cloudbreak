package com.sequenceiq.cloudbreak.service.decorator;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_AWS_AMI_MAP;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_AZURE_IMAGE_URI;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_GCP_SOURCE_IMAGE_PATH;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_OPENSTACK_IMAGE;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.regions.Regions;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.AdjustmentType;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;
import com.sequenceiq.cloudbreak.service.network.NetworkService;
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

    @Autowired
    private NetworkService networkService;

    @Value("${cb.azure.image.uri:" + CB_AZURE_IMAGE_URI + "}")
    private String azureImage;

    @Value("${cb.aws.ami.map:" + CB_AWS_AMI_MAP + "}")
    private String awsImage;

    @Value("${cb.openstack.image:" + CB_OPENSTACK_IMAGE + "}")
    private String openStackImage;

    @Value("${cb.gcp.source.image.path:" + CB_GCP_SOURCE_IMAGE_PATH + "}")
    private String gcpImage;

    private enum DecorationData {
        CREDENTIAL_ID,
        USR_CONSUL_SERVER_COUNT,
        NETWORK_ID
    }

    @Override
    public Stack decorate(Stack subject, Object... data) {
        subject.addAllowedSubnets(securityService.getCloudbreakSubnets(subject));
        subject.setCredential(credentialRepository.findOne((Long) data[DecorationData.CREDENTIAL_ID.ordinal()]));
        int consulServers = getConsulServerCount((Integer) data[DecorationData.USR_CONSUL_SERVER_COUNT.ordinal()], subject.getFullNodeCount());
        subject.setConsulServers(consulServers);
        Network network = networkService.getById((Long) data[DecorationData.NETWORK_ID.ordinal()]);
        if (!subject.cloudPlatform().equals(network.cloudPlatform())) {
            throw new BadRequestException("The selected credential and network must relate to the same cloud platform!");
        }
        subject.setNetwork(network);

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
