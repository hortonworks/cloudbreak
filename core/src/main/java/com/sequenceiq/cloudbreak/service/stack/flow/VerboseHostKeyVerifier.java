package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.VERBOSEDCLOUDPLATFORMS;

import java.security.PublicKey;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;

import net.schmizz.sshj.common.SecurityUtils;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;

public class VerboseHostKeyVerifier implements HostKeyVerifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(VerboseHostKeyVerifier.class);


    private Set<String> expectedFingerprints;
    private CloudPlatform cloudPlatform;

    public VerboseHostKeyVerifier(Set<String> expectedFingerprints, CloudPlatform cloudPlatform) {
        this.expectedFingerprints = expectedFingerprints;
        this.cloudPlatform = cloudPlatform;
    }

    @Override
    public boolean verify(String hostname, int port, PublicKey key) {
        if (!VERBOSEDCLOUDPLATFORMS.contains(cloudPlatform.name())) {
            String receivedFingerprint = SecurityUtils.getFingerprint(key);
            boolean matches = false;
            for (String expectedFingerprint : expectedFingerprints) {
                matches = receivedFingerprint.equals(expectedFingerprint);
                if (matches) {
                    break;
                }
            }

            if (matches) {
                LOGGER.info("HostKey has been successfully  verified. hostname: {}, port: {}, fingerprint: {}", hostname, port, receivedFingerprint);
            } else {
                LOGGER.error("HostKey verification failed. hostname: {}, port: {}, expectedFingerprint: {}, receivedFingerprint: {}", hostname, port,
                        expectedFingerprints, receivedFingerprint);
            }
            return matches;
        } else {
            return true;
        }
    }
}

