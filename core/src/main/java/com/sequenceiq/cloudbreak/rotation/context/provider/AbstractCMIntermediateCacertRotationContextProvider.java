package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_PILLAR;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_STATE_APPLY;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.Lists;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.context.SaltPillarRotationContext;
import com.sequenceiq.cloudbreak.rotation.context.SaltStateApplyRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.freeipa.FreeipaClientService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.PasswordUtil;

public abstract class AbstractCMIntermediateCacertRotationContextProvider implements RotationContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCMIntermediateCacertRotationContextProvider.class);

    private static final Integer SALT_STATE_MAX_RETRY = 5;

    @Inject
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private ExitCriteriaProvider exitCriteriaProvider;

    @Inject
    private StackDtoService stackService;

    @Inject
    private FreeipaClientService freeipaClientService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        StackDto stackDto = stackService.getByCrn(resourceCrn);
        return Map.of(VAULT, getVaultRotationContext(stackDto),
                SALT_STATE_APPLY, getSaltStateRotationContext(stackDto),
                SALT_PILLAR, getSaltPillarRotationContext(stackDto),
                CUSTOM_JOB, getCustomJobRotationContext(stackDto));
    }

    private SaltPillarRotationContext getSaltPillarRotationContext(StackDto stackDto) {
        return new SaltPillarRotationContext(stackDto.getResourceCrn(), stackDtoParam ->
                Map.of("cloudera-manager-autotls", clusterHostServiceRunner.getClouderaManagerAutoTlsPillarProperties(stackDtoParam.getCluster())));
    }

    private VaultRotationContext getVaultRotationContext(StackDto stackDto) {
        return VaultRotationContext.builder()
                .withResourceCrn(stackDto.getResourceCrn())
                .withVaultPathSecretMap(Map.of(
                        stackDto.getCluster().getKeyStorePwdSecret().getSecret(), PasswordUtil.generatePassword(),
                        stackDto.getCluster().getTrustStorePwdSecret().getSecret(), PasswordUtil.generatePassword()))
                .build();
    }

    private SaltStateApplyRotationContext getSaltStateRotationContext(StackDto stack) {
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        List<String> states = Lists.newArrayList();
        List<String> cleanupStates = Lists.newArrayList();
        if (stack.getCluster().getAutoTlsEnabled()) {
            states.addAll(List.of("cloudera.manager.server-stop", "cloudera.manager.rotate.cmca-renewal", "cloudera.manager.server-start"));
            cleanupStates.add("cloudera.manager.rotate.cmca-renewal-cleanup");
        }
        return SaltStateApplyRotationContext.builder()
                .withResourceCrn(stack.getResourceCrn())
                .withStates(states)
                .withRollbackStates(states)
                .withCleanupStates(cleanupStates)
                .withGatewayConfig(primaryGatewayConfig)
                .withTargets(Set.of(primaryGatewayConfig.getHostname()))
                .withExitCriteriaModel(exitCriteriaProvider.get(stack))
                .withMaxRetry(SALT_STATE_MAX_RETRY)
                .withMaxRetryOnError(SALT_STATE_MAX_RETRY)
                .build();
    }

    private CustomJobRotationContext getCustomJobRotationContext(StackDto stackDto) {
        return CustomJobRotationContext.builder()
                .withResourceCrn(stackDto.getResourceCrn())
                .withRotationJob(() -> waitForClouderaManagerToStartup(stackDto))
                .withPostValidateJob(() -> checkCMCAWithRootCert(stackDto))
                .build();
    }

    private void checkCMCAWithRootCert(StackDto stack) {
        try {
            ClusterApi connector = clusterApiConnectors.getConnector(stack);
            String trustStoreFromCM = connector.clusterSecurityService().getTrustStore();
            String rootCertFromFMS = freeipaClientService.getRootCertificateByEnvironmentCrn(stack.getEnvironmentCrn());
            List<X509Certificate> x509CertificatesFromCM = readPEMCertificatesFromString(trustStoreFromCM);
            List<X509Certificate> x509CertificatesFromFMS = readPEMCertificatesFromString(rootCertFromFMS);
            X509Certificate latestRootCertFromFMS = x509CertificatesFromFMS.stream()
                    .min(Comparator.comparing(X509Certificate::getNotAfter, Comparator.nullsLast(Comparator.reverseOrder())))
                    .orElseThrow(() -> new SecretRotationException("FreeIPA root cert cannot be found!"));
            X509Certificate cmcaCertificate = x509CertificatesFromCM.stream()
                    .filter(cert -> cert.getSubjectX500Principal().getName().contains(stack.getResourceName()))
                    .findFirst()
                    .orElseThrow(() ->
                            new SecretRotationException(String.format("CM intermediate certificate cannot be found for stack %S", stack.getResourceName())));
            cmcaCertificate.verify(latestRootCertFromFMS.getPublicKey());
        } catch (Exception e) {
            throw new SecretRotationException(e);
        }
    }

    private static List<X509Certificate> readPEMCertificatesFromString(String pemData) {
        List<X509Certificate> certificates = new ArrayList<>();

        // Use regex to find individual PEM certificates
        Pattern pattern = Pattern.compile("-----BEGIN CERTIFICATE-----(.*?)-----END CERTIFICATE-----", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(pemData);

        while (matcher.find()) {
            String pemCertificate = matcher.group(1).trim().replace("\n", "").replace("\r", "");
            try {
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(
                        new ByteArrayInputStream(Base64.getDecoder().decode(pemCertificate)));
                certificates.add(certificate);
            } catch (CertificateException e) {
                LOGGER.error("cannot read certificate.");
            }
        }

        return certificates;
    }

    private void waitForClouderaManagerToStartup(StackDto stack) {
        try {
            ClusterApi connector = clusterApiConnectors.getConnector(stack);
            connector.clusterSetupService().waitForServer(false);
        } catch (Exception e) {
            throw new SecretRotationException(e);
        }
    }
}
