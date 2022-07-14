package com.sequenceiq.cloudbreak.service.secret.domain;

import static org.slf4j.LoggerFactory.getLogger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.persistence.AttributeConverter;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.secret.service.SecretService;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component
public class SecretToString implements AttributeConverter<Secret, String> {

    private static final Logger LOGGER = getLogger(SecretToString.class);

    private static SecretService secretService;

    @Inject
    private SecretService secretServiceComponent;

    @PostConstruct
    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    public void init() {
        secretService = secretServiceComponent;
    }

    @Override
    public String convertToDatabaseColumn(Secret attribute) {
        return attribute != null ? attribute.getSecret() : null;
    }

    @Override
    public Secret convertToEntityAttribute(String dbData) {
        return new SecretProxy(secretService, dbData);
    }
}
