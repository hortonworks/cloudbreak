package com.sequenceiq.it.cloudbreak.assertion.hybrid;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.freeipa.api.v1.freeipa.user.UserV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.EnvironmentUserSyncState;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.UserAuthException;

@Component
public class HybridCloudAssertions {

    private static final Logger LOGGER = LoggerFactory.getLogger(HybridCloudAssertions.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String MOCK_UMS_PASSWORD_INVALID = "Invalid password";

    private static final int SSH_PORT = 22;

    private static final int SSH_CONNECT_TIMEOUT = 120000;

    public Assertion<SdxInternalTestDto, SdxClient> validateDatalakeSshAuthentication() {
        return (testContext, testDto, client) -> {
            String environmentCrn = testDto.getResponse().getEnvironmentCrn();
            com.sequenceiq.freeipa.api.client.FreeIpaClient freeIpaClient = testContext.getMicroserviceClient(FreeIpaClient.class)
                    .getDefaultClient(testContext);
            checkUserSyncState(environmentCrn, freeIpaClient);

            for (InstanceGroupV4Response ig : testDto.getResponse().getStackV4Response().getInstanceGroups()) {
                for (InstanceMetaDataV4Response i : ig.getMetadata()) {
                    String ip = i.getPublicIp();

                    LOGGER.info("Trying to ssh with user {} into instance: {}", testContext.getWorkloadUserName(), OBJECT_MAPPER.writeValueAsString(i));
                    testShhAuthenticationSuccessful(testContext, ip);
                    testShhAuthenticationFailure(testContext, ip);
                }
            }
            return testDto;
        };
    }

    private void checkUserSyncState(String environmentCrn, com.sequenceiq.freeipa.api.client.FreeIpaClient freeIpaClient) throws JsonProcessingException {
        UserV1Endpoint userV1Endpoint = freeIpaClient.getUserV1Endpoint();
        EnvironmentUserSyncState userSyncState = userV1Endpoint.getUserSyncState(environmentCrn);
        SyncOperationStatus syncOperationStatus = userV1Endpoint.getSyncOperationStatus(userSyncState.getLastUserSyncOperationId());
        LOGGER.info("Last user sync is in state {}, last operation: {}", userSyncState.getState(), OBJECT_MAPPER.writeValueAsString(syncOperationStatus));
    }

    private void testShhAuthenticationSuccessful(TestContext testContext, String host) {
        try (SSHClient client = getSshClient(host)) {
            client.authPassword(testContext.getWorkloadUserName(), testContext.getWorkloadPassword());
        } catch (IOException e) {
            throw new TestFailException(String.format("Failed to ssh into host %s with workload user %s", host, testContext.getWorkloadUserName()), e);
        }
    }

    private void testShhAuthenticationFailure(TestContext testContext, String host) throws IOException {
        try (SSHClient client = getSshClient(host)) {
            client.authPassword(testContext.getWorkloadUserName(), MOCK_UMS_PASSWORD_INVALID);
            throw new TestFailException(String.format("SSH authentication passed with invalid password on host %s.", host));
        } catch (UserAuthException ex) {
            LOGGER.info("Expected: SSH authentication failure has been happend!");
        }
    }

    private SSHClient getSshClient(String host) throws IOException {
        SSHClient client = new SSHClient();
        client.addHostKeyVerifier(new PromiscuousVerifier());
        client.setConnectTimeout(SSH_CONNECT_TIMEOUT);
        client.connect(host, SSH_PORT);
        return client;
    }
}
