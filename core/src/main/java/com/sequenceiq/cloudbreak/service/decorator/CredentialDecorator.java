package com.sequenceiq.cloudbreak.service.decorator;

import java.util.Random;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.Credential;

@Service
public class CredentialDecorator implements Decorator<Credential> {

    private static final String CLOUDBREAK_KEY_NAME = "cloudbreak-key";
    private static final int SUFFIX_RND = 999999;
    private Random rnd = new Random();

    @Override
    public Credential decorate(Credential subject, Object... data) {
        if (subject instanceof AwsCredential) {
            AwsCredential awsCredential = (AwsCredential) subject;
            awsCredential.setKeyPairName(generateNewKeyPairName());
            return awsCredential;
        }
        return subject;
    }

    private String generateNewKeyPairName() {
        return CLOUDBREAK_KEY_NAME + "-" + rnd.nextInt(SUFFIX_RND);
    }
}
