package com.sequenceiq.it.util.cleanup;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.CloudbreakApiKeyClient;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceUserCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakUserCrnClientBuilder;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.environment.client.EnvironmentClient;
import com.sequenceiq.environment.client.EnvironmentServiceApiKeyClient;
import com.sequenceiq.environment.client.EnvironmentServiceUserCrnClient;
import com.sequenceiq.environment.client.EnvironmentServiceUserCrnClientBuilder;
import com.sequenceiq.sdx.client.SdxClient;
import com.sequenceiq.sdx.client.SdxServiceApiKeyClient;
import com.sequenceiq.sdx.client.SdxServiceUserCrnClient;
import com.sequenceiq.sdx.client.SdxServiceUserCrnClientBuilder;

@Service
public class CleanupClientUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(CleanupClientUtil.class);

    private EnvironmentClient environmentClient;

    private SdxClient sdxClient;

    private CloudbreakClient cloudbreakClient;

    // Per-service servers. In prod all three resolve to the same reverse-proxied host,
    // but locally each service listens on its own port — so read them independently.
    @Value("${integrationtest.cloudbreak.server}")
    private String cloudbreakServer;

    @Value("${integrationtest.environment.server}")
    private String environmentServer;

    @Value("${integrationtest.sdx.server}")
    private String sdxServer;

    @Value("${integrationtest.cloudbreak.server.contextPath:/cb}")
    private String cbRootContextPath;

    @Value("${integrationtest.environment.server.contextPath:/environmentservice}")
    private String envRootContextPath;

    @Value("${integrationtest.sdx.server.contextPath:/dl}")
    private String sdxRootContextPath;

    @Value("${integrationtest.user.accesskey:}")
    private String accesskey;

    @Value("${integrationtest.user.secretkey:}")
    private String secretkey;

    // When true, skip API-key + ECDSA signing and stamp the request with `x-cdp-actor-crn` directly.
    // Meant for runs where there is no Knox/IAM proxy in front of the services to validate the
    // signature and inject the CRN header on behalf of the caller (e.g. talking straight to a
    // locally-running environmentservice / cloudbreak / sdx). The CRN sent is a real (non-internal)
    // user CRN, so PermissionCheckService takes the normal-user path instead of the
    // "service-to-service" one which rejects internal actors on endpoints that need an account id.
    //
    // The CRN is base64-decoded from `integrationtest.user.accesskey`. v3 access keys ARE the
    // base64-encoded user CRN — the decoded payload starts with "crn:".
    @Value("${integrationtest.cleanup.directCrnHeader:false}")
    private boolean directCrnHeader;

    public CloudbreakClient createCloudbreakClient() {
        String base = cloudbreakServer + cbRootContextPath;
        if (directCrnHeader) {
            String crn = resolveActorCrn();
            LOGGER.info("directCrnHeader mode: building CloudbreakServiceUserCrnClient against {} with actor {}", base, crn);
            CloudbreakServiceUserCrnClient userCrnClient = new CloudbreakUserCrnClientBuilder(base)
                    .withCertificateValidation(false)
                    .withIgnorePreValidation(true)
                    .withDebug(true)
                    .build();
            cloudbreakClient = userCrnClient.withCrn(crn);
        } else {
            cloudbreakClient = new CloudbreakApiKeyClient(base, new ConfigKey(false, true, true))
                    .withKeys(accesskey, secretkey);
        }
        return cloudbreakClient;
    }

    public EnvironmentClient createEnvironmentClient() {
        String base = environmentServer + envRootContextPath;
        if (directCrnHeader) {
            String crn = resolveActorCrn();
            LOGGER.info("directCrnHeader mode: building EnvironmentServiceUserCrnClient against {} with actor {}", base, crn);
            EnvironmentServiceUserCrnClient userCrnClient = new EnvironmentServiceUserCrnClientBuilder(base)
                    .withCertificateValidation(false)
                    .withIgnorePreValidation(true)
                    .withDebug(true)
                    .build();
            environmentClient = userCrnClient.withCrn(crn);
        } else {
            environmentClient = new EnvironmentServiceApiKeyClient(base, new ConfigKey(false, true, true))
                    .withKeys(accesskey, secretkey);
        }
        return environmentClient;
    }

    public SdxClient createSdxClient() {
        String base = sdxServer + sdxRootContextPath;
        if (directCrnHeader) {
            String crn = resolveActorCrn();
            LOGGER.info("directCrnHeader mode: building SdxServiceUserCrnClient against {} with actor {}", base, crn);
            SdxServiceUserCrnClient userCrnClient = new SdxServiceUserCrnClientBuilder(base)
                    .withCertificateValidation(false)
                    .withIgnorePreValidation(true)
                    .withDebug(true)
                    .build();
            sdxClient = userCrnClient.withCrn(crn);
        } else {
            sdxClient = new SdxServiceApiKeyClient(base, new ConfigKey(false, true, true))
                    .withKeys(accesskey, secretkey);
        }
        return sdxClient;
    }

    private String resolveActorCrn() {
        if (StringUtils.isBlank(accesskey)) {
            throw new IllegalStateException(
                    "integrationtest.user.accesskey (env INTEGRATIONTEST_USER_ACCESSKEY) must be set when integrationtest.cleanup.directCrnHeader=true");
        }
        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(accesskey), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "integrationtest.user.accesskey is not valid base64 — expected a v3 access key whose payload decodes to a CRN.", e);
        }
        if (!decoded.startsWith("crn:")) {
            throw new IllegalStateException(
                    "integrationtest.user.accesskey does not decode to a CRN (got: '" + decoded
                            + "') — expected a v3 access key whose base64 payload starts with 'crn:'.");
        }
        LOGGER.info("Derived actor CRN from access key: {}", decoded);
        return decoded;
    }

    public CloudbreakClient getCloudbreakClient() {
        return cloudbreakClient; }

    public EnvironmentClient getEnvironmentClient() {
        return environmentClient; }

    public SdxClient getSdxClient() {
        return sdxClient; }

    public void setCloudbreakClient(CloudbreakClient c) {
        this.cloudbreakClient = c; }

    public void setEnvironmentClient(EnvironmentClient c) {
        this.environmentClient = c; }

    public void setSdxClient(SdxClient c) {
        this.sdxClient = c; }
}
