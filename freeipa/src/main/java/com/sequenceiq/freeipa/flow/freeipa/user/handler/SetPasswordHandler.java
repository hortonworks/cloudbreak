package com.sequenceiq.freeipa.flow.freeipa.user.handler;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.freeipa.client.FreeIpaCapabilities;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.user.event.SetPasswordRequest;
import com.sequenceiq.freeipa.flow.freeipa.user.event.SetPasswordResult;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.WorkloadCredentialService;
import com.sequenceiq.freeipa.service.freeipa.user.conversion.UserMetadataConverter;
import com.sequenceiq.freeipa.service.freeipa.user.model.UserMetadata;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredentialUpdate;
import com.sequenceiq.freeipa.service.freeipa.user.ums.UmsCredentialProvider;
import com.sequenceiq.freeipa.service.stack.StackService;

import reactor.bus.Event;

@Component
public class SetPasswordHandler implements EventHandler<SetPasswordRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetPasswordHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private UmsCredentialProvider umsCredentialProvider;

    @Inject
    private WorkloadCredentialService workloadCredentialService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private UserMetadataConverter userMetadataConverter;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(SetPasswordRequest.class);
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
                LOGGER.info("IPA has password hash support. Credentials information from UMS will be used.");
                WorkloadCredential workloadCredential = umsCredentialProvider.getCredentials(request.getUserCrn());
                setPasswordHash(stack, request, freeIpaClient, workloadCredential);

                if (StringUtils.isBlank(workloadCredential.getHashedPassword())) {
                    LOGGER.info("IPA has password hash support but user does not have a password set in UMS; using the provided password directly.");
                    freeIpaClient.userSetPasswordWithExpiration(
                            request.getUsername(), request.getPassword(), request.getExpirationInstant());
                }
            } else {
                LOGGER.info("IPA does not have password hash support; using the provided password directly.");
                freeIpaClient.userSetPasswordWithExpiration(
                        request.getUsername(), request.getPassword(), request.getExpirationInstant());
            }
            SetPasswordResult result = new SetPasswordResult(request);
            request.getResult().onNext(result);
        } catch (Exception e) {
            request.getResult().onError(e);
        }
    }

    private void setPasswordHash(Stack stack, SetPasswordRequest request, FreeIpaClient freeIpaClient, WorkloadCredential workloadCredential)
            throws IOException, FreeIpaClientException {
        String accountId = Crn.fromString(stack.getEnvironmentCrn()).getAccountId();
        boolean credentialsUpdateOptimizationEnabled = entitlementService.usersyncCredentialsUpdateOptimizationEnabled(accountId);
        LOGGER.info("Credentials update optimization is{} enabled for account {}", credentialsUpdateOptimizationEnabled ? "" : " not", accountId);

        if (credentialUpdateRequired(credentialsUpdateOptimizationEnabled, request.getUsername(), freeIpaClient, workloadCredential)) {
            workloadCredentialService.setWorkloadCredential(credentialsUpdateOptimizationEnabled, freeIpaClient,
                    new WorkloadCredentialUpdate(request.getUsername(), request.getUserCrn(), workloadCredential));
        } else {
            LOGGER.debug("Not setting workload credentials for user '{}' because credentials are already up to date", request.getUsername());
        }
    }

    private boolean credentialUpdateRequired(boolean credentialsUpdateOptimizationEnabled, String username, FreeIpaClient freeIpaClient,
            WorkloadCredential workloadCredential) throws FreeIpaClientException {
        if (credentialsUpdateOptimizationEnabled) {
            Optional<User> ipaUser = freeIpaClient.userFind(username);
            if (ipaUser.isPresent()) {
                Optional<UserMetadata> userMetadata = userMetadataConverter.toUserMetadata(ipaUser.get());
                if (userMetadata.isPresent() && userMetadata.get().getWorkloadCredentialsVersion() >= workloadCredential.getVersion()) {
                    return false;
                }
            }
        }

        return true;
    }
}