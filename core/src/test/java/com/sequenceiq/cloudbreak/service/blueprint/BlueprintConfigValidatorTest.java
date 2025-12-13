package com.sequenceiq.cloudbreak.service.blueprint;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

class BlueprintConfigValidatorTest {

    private BlueprintConfigValidator underTest = new BlueprintConfigValidator();

    @Test
    void testIfRoleConfigGroupsRefNamesMisspelledShouldDropException() {
        Blueprint request = new Blueprint();
        request.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("test/defaults/blueprints/blueprint-with-wrong-role-group-name.bp"));
        assertThrows(BadRequestException.class, () -> underTest.validate(request),
                "RoleConfigGroupsRefNames is probably missing or misspelled in your Cloudera Manager template.");
    }

    @Test
    void testIfVolumeConfigPresentedShouldDropException() {
        Blueprint request = new Blueprint();
        request.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("test/defaults/blueprints/blueprint-with-volume-config.bp"));
        assertThrows(BadRequestException.class, () -> underTest.validate(request), "Volume configuration should not be part of your Cloudera Manager template.");
    }

    @Test
    void testIfRoleConfigGroupsMisspelledShouldDropException() {
        Blueprint request = new Blueprint();
        request.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("test/defaults/blueprints/blueprint-with-wrong-service-role-group-name.bp"));
        assertThrows(BadRequestException.class, () -> underTest.validate(request),
                "RoleConfigGroups is probably missing or misspelled in your Cloudera Manager template.");
    }

    @Test
    void testIfInstatiatorPresentedShouldDropException() {
        Blueprint request = new Blueprint();
        request.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("test/defaults/blueprints/blueprint-with-instantiator.bp"));
        assertThrows(BadRequestException.class, () -> underTest.validate(request),
                "Instantiator is present in your Cloudera Manager template which is probably incorrect.");
    }

    @Test
    void testIfPasswordPresentedWithStarsShouldDropException() {
        Blueprint request = new Blueprint();
        request.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("test/defaults/blueprints/blueprint-with-password-placeholder.bp"));
        assertThrows(BadRequestException.class, () -> underTest.validate(request),
                "Password placeholder with **** is present in your Cloudera Manager template which is probably incorrect.");
    }

    @Test
    void testIfReposioriesPresentedShouldDropException() {
        Blueprint request = new Blueprint();
        request.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("test/defaults/blueprints/blueprint-with-repositories.bp"));
        assertThrows(BadRequestException.class, () -> underTest.validate(request),
                "Repositories are present in your Cloudera Manager template, this must be removed.");
    }

}