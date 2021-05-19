package com.sequenceiq.cloudbreak.service.blueprint;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class BlueprintConfigValidatorTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private BlueprintConfigValidator underTest;

    @Test
    public void testIfRoleConfigGroupsRefNamesMisspelledShouldDropException() {
        Blueprint request = new Blueprint();
        request.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("test/defaults/blueprints/blueprint-with-wrong-role-group-name.bp"));
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("RoleConfigGroupsRefNames is probably missing or misspelled in your Cloudera Manager template.");
        underTest.validate(request);
    }

    @Test
    public void testIfVolumeConfigPresentedShouldDropException() {
        Blueprint request = new Blueprint();
        request.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("test/defaults/blueprints/blueprint-with-volume-config.bp"));
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Volume configuration should not be part of your Cloudera Manager template.");
        underTest.validate(request);
    }

    @Test
    public void testIfRoleConfigGroupsMisspelledShouldDropException() {
        Blueprint request = new Blueprint();
        request.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("test/defaults/blueprints/blueprint-with-wrong-service-role-group-name.bp"));
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("RoleConfigGroups is probably missing or misspelled in your Cloudera Manager template.");
        underTest.validate(request);
    }

    @Test
    public void testIfInstatiatorPresentedShouldDropException() {
        Blueprint request = new Blueprint();
        request.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("test/defaults/blueprints/blueprint-with-instantiator.bp"));
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Instantiator is present in your Cloudera Manager template which is probably incorrect.");
        underTest.validate(request);
    }

    @Test
    public void testIfPasswordPresentedWithStarsShouldDropException() {
        Blueprint request = new Blueprint();
        request.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("test/defaults/blueprints/blueprint-with-password-placeholder.bp"));
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Password placeholder with **** is present in your Cloudera Manager template which is probably incorrect.");
        underTest.validate(request);
    }

    @Test
    public void testIfReposioriesPresentedShouldDropException() {
        Blueprint request = new Blueprint();
        request.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("test/defaults/blueprints/blueprint-with-repositories.bp"));
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Repositories are present in your Cloudera Manager template, this must be removed.");
        underTest.validate(request);
    }

}