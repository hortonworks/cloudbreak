package com.sequenceiq.cloudbreak.reactor;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.credential.InteractiveCredentialCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.mapper.DuplicatedKeyValueExceptionMapper;
import com.sequenceiq.cloudbreak.converter.spi.ExtendedCloudCredentialToCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;

import reactor.bus.Event;

/**
 * Created by perdos on 9/23/16.
 */
@Component
public class InteractiveCredentialCreationHandler implements ClusterEventHandler<InteractiveCredentialCreationRequest> {

    @Inject
    private CredentialService credentialService;

    @Inject
    private ExtendedCloudCredentialToCredentialConverter extendedCloudCredentialToCredentialConverter;

    @Override
    public Class<InteractiveCredentialCreationRequest> type() {
        return InteractiveCredentialCreationRequest.class;
    }

    @Override
    public void accept(Event<InteractiveCredentialCreationRequest> interactiveCredentialCreationRequestEvent) {
        InteractiveCredentialCreationRequest interactiveCredentialCreationRequest = interactiveCredentialCreationRequestEvent.getData();

        ExtendedCloudCredential extendedCloudCredential = interactiveCredentialCreationRequest.getExtendedCloudCredential();
        Credential credential = extendedCloudCredentialToCredentialConverter.convert(extendedCloudCredential);
        try {
            credentialService.createWithRetry(extendedCloudCredential.getOwner(), extendedCloudCredential.getAccount(), credential);
        } catch (DuplicateKeyValueException e) {
            credentialService.sendErrorNotification(extendedCloudCredential.getOwner(), extendedCloudCredential.getAccount(),
                    extendedCloudCredential.getCloudPlatform(), DuplicatedKeyValueExceptionMapper.errorMessage(e));
        } catch (BadRequestException e) {
            credentialService.sendErrorNotification(extendedCloudCredential.getOwner(), extendedCloudCredential.getAccount(),
                    extendedCloudCredential.getCloudPlatform(), e.getMessage());
        }
    }
}
