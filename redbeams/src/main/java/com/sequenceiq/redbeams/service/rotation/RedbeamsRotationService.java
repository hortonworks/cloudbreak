package com.sequenceiq.redbeams.service.rotation;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.type.RedbeamsSecretType;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.rotation.service.SecretRotationValidator;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@Service
public class RedbeamsRotationService {

    @Inject
    private RedbeamsFlowManager redbeamsFlowManager;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private SecretRotationValidator secretRotationValidator;

    @Inject
    private DBStackService dbStackService;

    public FlowIdentifier rotateSecrets(String resourceCrn, List<String> secrets, RotationFlowExecutionType executionType) {
        if (entitlementService.isSecretRotationEnabled(Crn.fromString(resourceCrn).getAccountId())) {
            Long resourceIdByResourceCrn = dbStackService.getResourceIdByResourceCrn(resourceCrn);
            if (resourceIdByResourceCrn != null) {
                List<SecretType> secretTypes = secretRotationValidator.mapSecretTypes(secrets, RedbeamsSecretType.class);
                return redbeamsFlowManager.triggerSecretRotation(resourceIdByResourceCrn, resourceCrn, secretTypes, executionType);
            } else {
                throw new CloudbreakServiceException("No db stack found with crn: " + resourceCrn);
            }
        } else {
            throw new CloudbreakServiceException("Account is not entitled to execute any secret rotation!");
        }
    }
}
