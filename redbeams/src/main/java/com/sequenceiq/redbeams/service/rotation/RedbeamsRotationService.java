package com.sequenceiq.redbeams.service.rotation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.SecretTypeConverter;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@Service
public class RedbeamsRotationService {

    @Inject
    private RedbeamsFlowManager redbeamsFlowManager;

    @Inject
    private DBStackService dbStackService;

    @Inject
    private List<SecretType> enabledSecretTypes;

    public FlowIdentifier rotateSecrets(String resourceCrn, List<String> secrets, RotationFlowExecutionType executionType,
            Map<String, String> additionalProperties) {
        DBStack dbStack = dbStackService.getByCrn(resourceCrn);
        List<SecretType> secretTypes = SecretTypeConverter.mapSecretTypes(secrets,
                enabledSecretTypes.stream().map(SecretType::getClass).collect(Collectors.toSet()));
        if (!dbStack.getStatus().isAvailable()) {
            throw new CloudbreakServiceException(
                    String.format("Secret rotation is not allowed because database status is not available. Current status: %s", dbStack.getStatus()));
        }
        return redbeamsFlowManager.triggerSecretRotation(dbStack.getId(), resourceCrn, secretTypes, executionType, additionalProperties);
    }
}
