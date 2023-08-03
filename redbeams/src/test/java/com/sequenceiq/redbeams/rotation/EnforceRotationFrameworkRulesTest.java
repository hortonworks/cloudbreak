package com.sequenceiq.redbeams.rotation;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.rotation.EnforceRotationFrameworkRulesUtil;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EnforceRotationFrameworkRulesUtil.TestAppContext.class)
public class EnforceRotationFrameworkRulesTest {

    @Named("enforceRotationFrameworkRulesMessageSource")
    @Inject
    private MessageSource messageSource;

    @Test
    void enforceMultiSecretTypes() {
        EnforceRotationFrameworkRulesUtil.enforceMultiSecretTypes();
    }

    @Test
    void enforceMultiSecretTypeMethodForRelatedContextProviders() {
        EnforceRotationFrameworkRulesUtil.enforceMultiSecretTypeMethodForRelatedContextProviders();
    }

    @Test
    void enforceSecretTypeBelongsOnlyOneMultiSecretType() {
        EnforceRotationFrameworkRulesUtil.enforceSecretTypeBelongsOnlyOneMultiSecretType();
    }

    @Test
    void enforceThereAreNoDuplicatesBetweenSecretTypeEnums() {
        EnforceRotationFrameworkRulesUtil.enforceThereAreNoDuplicatesBetweenSecretTypeEnums();
    }

    @Test
    void enforceThereAreNoDuplicatesBetweenMultiSecretTypeEnums() {
        EnforceRotationFrameworkRulesUtil.enforceThereAreNoDuplicatesBetweenMultiSecretTypeEnums();
    }

    @Test
    void enforceMessagesForTypesAndSteps() {
        EnforceRotationFrameworkRulesUtil.enforceMessagesForTypesAndSteps(messageSource);
    }
}
