package com.sequenceiq.freeipa.service.freeipa.trust.setup;

import static com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil.ignoreNotFoundException;

import java.util.Locale;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class CancelTrustService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CancelTrustService.class);

    @Inject
    private StackService stackService;

    @Inject
    private CrossRealmTrustService crossRealmTrustService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    public void cancelTrust(Long stackId) throws FreeIpaClientException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        CrossRealmTrust crossRealmTrust = crossRealmTrustService.getByStackId(stackId);
        FreeIpaClient client = freeIpaClientFactory.getFreeIpaClientForStack(stack);
        String realm = crossRealmTrust.getRealm().toUpperCase(Locale.ROOT);
        ignoreNotFoundException(() -> client.deleteTrust(realm),
                "Deleting trust for [{}] but it was not found", realm);
        LOGGER.debug("Deleting trust for crossRealm [{}]", crossRealmTrust);

        ignoreNotFoundException(() -> client.deleteForwardDnsZone("in-addr.arpa."),
                "Deleting DNS forward zone for [in-addr.arpa.] but it was not found", realm);
        ignoreNotFoundException(() -> client.deleteForwardDnsZone(realm),
                "Deleting DNS forward zone for [{}] but it was not found", realm);
        LOGGER.debug("Deleting DNS forward zone for crossRealm [{}]", crossRealmTrust);
    }
}
