package com.sequenceiq.freeipa.service.rotation.cacert.contextprovider;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.freeipa.entity.RootCert;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.config.RootCertRegisterService;
import com.sequenceiq.freeipa.service.freeipa.cert.root.RootCertService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class FreeipaCacertRenewalContextProvider implements RotationContextProvider {

    private static final String FREEIPA_ROOT_CERT_RENEW_COMMAND = "ipa-cacert-manage renew";

    @Inject
    private StackService stackService;

    @Inject
    private HostOrchestrator orchestrator;

    @Inject
    private RootCertService rootCertService;

    @Inject
    private RootCertRegisterService rootCertRegisterService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        return Map.of(CUSTOM_JOB, CustomJobRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withRotationJob(() -> renewRootCert(resourceCrn))
                .withFinalizeJob(() -> clearOldCert(resourceCrn))
                .build());
    }

    private void renewRootCert(String resourceCrn) {
        try {
            Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(resourceCrn, ThreadBasedUserCrnProvider.getAccountId());
            List<GatewayConfig> notDeletedGatewayConfigs = gatewayConfigService.getNotDeletedGatewayConfigs(stack);
            orchestrator.runCommandOnHosts(notDeletedGatewayConfigs, Set.of(stack.getPrimaryGateway().orElseThrow().getDiscoveryFQDN()),
                    FREEIPA_ROOT_CERT_RENEW_COMMAND);
            String newRootCertFromServer = RootCertRegisterService.convertToPemFormat(rootCertRegisterService.getRootCertFromFreeIpa(stack));
            Optional<RootCert> originalRootCertInDb = rootCertService.findByStackId(stack.getId());
            originalRootCertInDb.ifPresentOrElse(rootCert -> {
                String extendedRootCert = String.join("\n", rootCert.getCert(), newRootCertFromServer);
                rootCert.setCert(extendedRootCert);
                rootCertService.save(rootCert);
            }, () -> {
                throw new CloudbreakServiceException("Root certificate cannot be found for stack.");
            });
        } catch (Exception e) {
            throw new SecretRotationException("Renewal of FreeIPA root cert has been failed.", e);
        }
    }

    private void clearOldCert(String resourceCrn) {
        try {
            Stack stack = stackService.getByEnvironmentCrnAndAccountId(resourceCrn, ThreadBasedUserCrnProvider.getAccountId());
            String newRootCertFromServer = RootCertRegisterService.convertToPemFormat(rootCertRegisterService.getRootCertFromFreeIpa(stack));
            Optional<RootCert> originalRootCertInDb = rootCertService.findByStackId(stack.getId());
            originalRootCertInDb.ifPresentOrElse(rootCert -> {
                rootCert.setCert(newRootCertFromServer);
                rootCertService.save(rootCert);
            }, () -> {
                throw new CloudbreakServiceException("Root certificate cannot be found for stack.");
            });
        } catch (Exception e) {
            throw new SecretRotationException("Cleaning up of FreeIPA old root cert has been failed.", e);
        }
    }

    @Override
    public SecretType getSecret() {
        return FreeIpaSecretType.FREEIPA_CA_CERT_RENEWAL;
    }
}
