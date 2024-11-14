package com.sequenceiq.cloudbreak.rotation;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EnforceRotationFrameworkRulesUtil.TestAppContext.class)
public class EnforceRotationFrameworkRulesTest {

    @Named("enforceRotationFrameworkRulesMessageSource")
    @Inject
    private MessageSource messageSource;

    @Test
    void enforceThereAreNoDuplicatesBetweenSecretTypeEnums() {
        EnforceRotationFrameworkRulesUtil.enforceThereAreNoDuplicatesBetweenSecretTypeEnums();
    }

    @Test
    void enforceMessagesForTypesAndSteps() {
        EnforceRotationFrameworkRulesUtil.enforceMessagesForTypesAndSteps(messageSource);
    }
}
