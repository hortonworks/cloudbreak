package com.sequenceiq.provisioning.service.aws;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.sequenceiq.provisioning.domain.TemporaryAwsCredentials;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.repository.TemporaryAwsCredentialsRepository;
import com.sequenceiq.provisioning.repository.UserRepository;

/**
 * Provides temporary session credentials for cross account requests. The assume
 * role request is made with basic credentials found in the default provider
 * chain (e.g.: environment variables). The assume role request must be made
 * with IAM user credentials, and the user must have a policy assigned that
 * allows her to call sts:AssumeRole. To be able to make cross-account requests
 * on a specific account, an IAM Role must be created on that account and it
 * must be configured with the accountId of the trusted partner.
 */
@Component
public class CrossAccountCredentialsProvider {

    private static final int MILLISECONDS = 1000;

    private static final int FIVE_MINUTES = 300000;

    @Autowired
    private TemporaryAwsCredentialsRepository credentialsRepository;

    @Autowired
    private UserRepository userRepository;

    public BasicSessionCredentials retrieveSessionCredentials(int durationInSeconds, String externalId, User user) {

        BasicSessionCredentials cachedSessionCredentials = getCachedCredentials(user);
        if (cachedSessionCredentials != null) {
            return cachedSessionCredentials;
        }

        AWSSecurityTokenServiceClient client = new AWSSecurityTokenServiceClient();
        AssumeRoleRequest assumeRoleRequest = new AssumeRoleRequest()
                .withDurationSeconds(durationInSeconds)
                .withExternalId(externalId)
                .withRoleArn(user.getRoleArn())
                .withRoleSessionName("hadoop-provisioning");
        AssumeRoleResult result = client.assumeRole(assumeRoleRequest);

        cacheSessionCredentials(result, durationInSeconds, user);

        return new BasicSessionCredentials(
                result.getCredentials().getAccessKeyId(),
                result.getCredentials().getSecretAccessKey(),
                result.getCredentials().getSessionToken());
    }

    private BasicSessionCredentials getCachedCredentials(User user) {
        long dateNow = new Date().getTime();
        TemporaryAwsCredentials temporaryAwsCredentials = user.getTemporaryAwsCredentials();
        if (temporaryAwsCredentials != null) {
            if (temporaryAwsCredentials.getValidUntil() > dateNow + FIVE_MINUTES) {
                return new BasicSessionCredentials(
                        temporaryAwsCredentials.getAccessKeyId(),
                        temporaryAwsCredentials.getSecretAccessKey(),
                        temporaryAwsCredentials.getSessionToken());
            } else {
                user.setTemporaryAwsCredentials(null);
                userRepository.save(user);
                credentialsRepository.delete(temporaryAwsCredentials);
            }
        }
        return null;
    }

    private void cacheSessionCredentials(AssumeRoleResult result, int durationInSeconds, User user) {
        TemporaryAwsCredentials temporaryAwsCredentials = new TemporaryAwsCredentials();
        temporaryAwsCredentials.setAccessKeyId(result.getCredentials().getAccessKeyId());
        temporaryAwsCredentials.setSecretAccessKey(result.getCredentials().getSecretAccessKey());
        temporaryAwsCredentials.setSessionToken(result.getCredentials().getSessionToken());
        temporaryAwsCredentials.setValidUntil(new Date().getTime() + durationInSeconds * MILLISECONDS);
        credentialsRepository.save(temporaryAwsCredentials);
        user.setTemporaryAwsCredentials(temporaryAwsCredentials);
        userRepository.save(user);

    }
}
