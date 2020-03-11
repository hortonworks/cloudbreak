package com.sequenceiq.freeipa.flow.freeipa.user.handler;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.security.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.logger.MDCUtils;
import com.sequenceiq.freeipa.client.FreeIpaCapabilities;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.service.freeipa.user.UmsCredentialProvider;
import com.sequenceiq.freeipa.service.freeipa.user.kerberos.KrbKeySetEncoder;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.user.event.SetPasswordRequest;
import com.sequenceiq.freeipa.flow.freeipa.user.event.SetPasswordResult;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.stack.StackService;

import reactor.bus.Event;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

@Component
public class SetPasswordHandler implements EventHandler<SetPasswordRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetPasswordHandler.class);

    private static final String IAM_INTERNAL_ACTOR_CRN = new InternalCrnBuilder(Crn.Service.IAM).getInternalCrnForServiceAsString();

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private UmsCredentialProvider umsCredentialProvider;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(SetPasswordRequest.class);
    }

    private void setPasswordHashFromUms(FreeIpaClient freeIpaClient, String username, String userCrn,
                                        Optional<Instant> passwordExpirationInstant) throws IOException, FreeIpaClientException {
        WorkloadCredential workloadCredential = umsCredentialProvider.getCredentials(userCrn, MDCUtils.getRequestId());
        String ansEncodedKrbPrincipalKey = KrbKeySetEncoder.getASNEncodedKrbPrincipalKey(workloadCredential.getKeys());
        freeIpaClient.userSetPasswordHash(username, workloadCredential.getHashedPassword(), ansEncodedKrbPrincipalKey,
                passwordExpirationInstant);
    }

    @Override
    public void accept(Event<SetPasswordRequest> setPasswordRequestEvent) {
        SetPasswordRequest request = setPasswordRequestEvent.getData();
        LOGGER.info("SetPasswordHandler accepting request {}", request);
        try {
            Stack stack = stackService.getStackById(request.getResourceId());
            MDCBuilder.buildMdcContext(stack);

            FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack);
            if (FreeIpaCapabilities.hasSetPasswordHashSupport(freeIpaClient.getConfig())) {
                LOGGER.info("IPA has password hash support, credentials information from UMS will be used.");
                setPasswordHashFromUms(freeIpaClient, request.getUsername(), request.getUserCrn(), request.getExpirationInstant());
            } else {
                LOGGER.info("IPA does not have password hash support, using the provided password directly.");
                freeIpaClient.userSetPasswordWithExpiration(
                        request.getUsername(), request.getPassword(), request.getExpirationInstant());
            }
            SetPasswordResult result = new SetPasswordResult(request);
            request.getResult().onNext(result);
        } catch (Exception e) {
            request.getResult().onError(e);
        }
    }
}