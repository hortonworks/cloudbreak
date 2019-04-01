package com.sequenceiq.cloudbreak.startup;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.SecretKey;
import com.sequenceiq.cloudbreak.repository.SecretKeyRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@Component
@DependsOn("encryptionConverter")
public class SecretKeyValidator {

    @Inject
    private SecretKeyRepository secretKeyRepository;

    @Value("${cb.client.secret.test:test}")
    private String secretKeyTest;

    @PostConstruct
    public void init() throws CloudbreakException {
        Iterable<SecretKey> all = secretKeyRepository.findAll();
        if (!all.iterator().hasNext()) {
            SecretKey e = new SecretKey();
            e.setId(1L);
            e.setValue(secretKeyTest);
            secretKeyRepository.save(e);
        } else {
            SecretKey secretKey = all.iterator().next();
            if (!secretKey.getValue().equals(secretKeyTest)) {
                throw new CloudbreakException("Did you change the secret key? Cloudbreak does not support changing the following env variables: "
                        + "UAA_CLOUDBREAK_SECRET, UAA_DEFAULT_SECRET. If you've changed them please revert them to the previous value.");
            }
        }
    }
}
