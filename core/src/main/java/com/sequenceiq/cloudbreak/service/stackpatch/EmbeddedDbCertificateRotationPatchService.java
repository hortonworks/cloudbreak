package com.sequenceiq.cloudbreak.service.stackpatch;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.service.stack.flow.StackRotationService;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Component
public class EmbeddedDbCertificateRotationPatchService extends ExistingStackPatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedDbCertificateRotationPatchService.class);

    @Inject
    private StackRotationService stackRotationService;

    @Override
    public StackPatchType getStackPatchType() {
        return StackPatchType.EMBEDDED_DB_CERTIFICATE_ROTATION;
    }

    @Override
    public boolean isAffected(Stack stack) {
        return Optional.ofNullable(stack.getDatabase())
                .map(Database::getExternalDatabaseAvailabilityType)
                .map(DatabaseAvailabilityType::isEmbedded)
                .orElse(false);
    }

    @Override
    boolean doApply(Stack stack) throws ExistingStackPatchApplyException {
        try {
            FlowIdentifier flowIdentifier = stackRotationService.rotateSecrets(stack.getResourceCrn(),
                    List.of(CloudbreakSecretType.EMBEDDED_DB_SSL_CERT.value()), null, Map.of());
            LOGGER.info("Started embedded DB certificate rotation flow for stack '{}' with flow identifier '{}'.", stack.getId(), flowIdentifier);
        } catch (BadRequestException e) {
            LOGGER.error("Failed to rotate embedded DB certificate for stack '{}' because of a validation error:", stack.getId(), e);
            return false;
        } catch (Exception e) {
            throw new ExistingStackPatchApplyException(String.format("Failed to rotate embedded DB certificate for stack %s, because of an unexpected error.",
                    stack.getId()), e);
        }
        return true;
    }
}
