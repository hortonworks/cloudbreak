package com.sequenceiq.cloudbreak.controller.validation.template;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.VolumeParameterConstants;

class InstanceTemplateValidatorTest {

    private InstanceTemplateValidator underTest;

    @BeforeEach
    void setUp() {
        underTest = new InstanceTemplateValidator();
    }

    @Test
    void testValidateShouldNotAddValidationErrorWhenTheNumberOfVolumesIsNull() {
        Template instanceTemplate = new Template();

        ValidationResult result = underTest.validate(instanceTemplate);

        assertNotNull(result);
        assertFalse(result.hasError());
    }

    @Test
    void testValidateShouldNotAddValidationErrorWhenTheNumberOfVolumesIsEmpty() {
        Template instanceTemplate = new Template();
        instanceTemplate.setVolumeTemplates(Set.of());

        ValidationResult result = underTest.validate(instanceTemplate);

        assertNotNull(result);
        assertFalse(result.hasError());
    }

    @Test
    void testValidateShouldNotAddValidationErrorWhenTheNumberOfVolumesIsLessThanTheMaximumSupportedOneVolumeTemplateExists() {
        VolumeTemplate volumeTemplate = new VolumeTemplate();
        volumeTemplate.setVolumeCount(20);
        Template instanceTemplate = new Template();
        instanceTemplate.setVolumeTemplates(Set.of(volumeTemplate));

        ValidationResult result = underTest.validate(instanceTemplate);

        assertNotNull(result);
        assertFalse(result.hasError());
    }

    @Test
    void testValidateShouldNotAddValidationErrorWhenTheNumberOfVolumesIsLessThanTheMaximumSupportedAndMultipleVolumeTemplatesExist() {
        VolumeTemplate aVolumeTemplate = new VolumeTemplate();
        aVolumeTemplate.setVolumeCount(10);
        VolumeTemplate bVolumeTemplate = new VolumeTemplate();
        bVolumeTemplate.setVolumeCount(11);
        Template instanceTemplate = new Template();
        instanceTemplate.setVolumeTemplates(Set.of(aVolumeTemplate, bVolumeTemplate));

        ValidationResult result = underTest.validate(instanceTemplate);

        assertNotNull(result);
        assertFalse(result.hasError());
    }

    @Test
    void testValidateShouldAddValidationErrorWhenTheNumberOfVolumesIsMoreThanTheSupportedAndOnlyOneVolumeTemplateExists() {
        String instanceTemplateName = "anInstanceTemplate";
        int volumeCount = 200;
        VolumeTemplate volumeTemplate = new VolumeTemplate();
        volumeTemplate.setVolumeCount(volumeCount);
        Template instanceTemplate = new Template();
        instanceTemplate.setName(instanceTemplateName);
        instanceTemplate.setVolumeTemplates(Set.of(volumeTemplate));

        ValidationResult result = underTest.validate(instanceTemplate);

        assertNotNull(result);
        assertTrue(result.hasError());
        String expectedMessage = String.format(InstanceTemplateValidator.INVALID_NUMBER_OF_VOLUMES_MESSAGE_PATTERN,
                VolumeParameterConstants.MAXIMUM_NUMBER_OF_VOLUMES,
                instanceTemplateName,
                volumeCount
        );
        assertTrue(result.getErrors().contains(expectedMessage));
    }

    @Test
    void testValidateShouldAddValidationErrorWhenTheNumberOfVolumesIsMoreThanTheSupportedAndMultipleVolumeTemplatesExist() {
        String instanceTemplateName = "anInstanceTemplate";
        int aVolumeCount = 1;
        int bVolumeCount = VolumeParameterConstants.MAXIMUM_NUMBER_OF_VOLUMES;
        VolumeTemplate aVolumeTemplate = new VolumeTemplate();
        aVolumeTemplate.setVolumeCount(aVolumeCount);
        VolumeTemplate bVolumeTemplate = new VolumeTemplate();
        bVolumeTemplate.setVolumeCount(bVolumeCount);
        Template instanceTemplate = new Template();
        instanceTemplate.setName(instanceTemplateName);
        instanceTemplate.setVolumeTemplates(Set.of(aVolumeTemplate, bVolumeTemplate));

        ValidationResult result = underTest.validate(instanceTemplate);

        assertNotNull(result);
        assertTrue(result.hasError());
        String expectedMessage = String.format(InstanceTemplateValidator.INVALID_NUMBER_OF_VOLUMES_MESSAGE_PATTERN,
                VolumeParameterConstants.MAXIMUM_NUMBER_OF_VOLUMES,
                instanceTemplateName,
                aVolumeCount + bVolumeCount
        );
        assertTrue(result.getErrors().contains(expectedMessage));
    }
}