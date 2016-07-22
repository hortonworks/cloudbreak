package com.sequenceiq.cloudbreak.controller.validation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.validation.ConstraintValidatorContext;

import org.apache.commons.codec.binary.Base64;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.validation.PluginValidator;
import com.sequenceiq.cloudbreak.validation.ValidPlugin;

@RunWith(MockitoJUnitRunner.class)
public class PluginValidatorTest extends AbstractValidatorTest {

    @InjectMocks
    private PluginValidator underTest;

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Mock
    private ValidPlugin validPlugin;

    @Before
    public void setUp() {
        underTest.initialize(validPlugin);
        BDDMockito.given(constraintValidatorContext.buildConstraintViolationWithTemplate(Matchers.anyString())).willReturn(getConstraintViolationBuilder());
    }

    @Test
    public void validPluginJsonWillReturnTrue() {
        Set<String> plugins = new HashSet<>();
        plugins.add("base64://" + Base64.encodeBase64String("plugin.toml:\nrecipe-pre-install:".getBytes()));
        Assert.assertTrue(underTest.isValid(plugins, constraintValidatorContext));
    }

    @Test
    public void inValidPluginNullReturnFalse() {
        Assert.assertEquals(underTest.isValid(null, constraintValidatorContext), false);
    }

    @Test
    public void inValidPluginEmptyReturnFalse() {
        Assert.assertFalse(underTest.isValid(Collections.emptySet(), constraintValidatorContext));
    }

    @Test
    public void inValidPluginUrlJsonWillReturnFalse() {
        Set<String> plugins = new HashSet<>();
        plugins.add("asd://github.com/user/plugin1.git");
        Assert.assertFalse(underTest.isValid(plugins, constraintValidatorContext));
    }

    @Test
    public void inValidBase64MissingScriptWillReturnFalse() {
        Set<String> plugins = new HashSet<>();
        plugins.add("base64://" + Base64.encodeBase64String("plugin.toml:".getBytes()));
        Assert.assertFalse(underTest.isValid(plugins, constraintValidatorContext));
    }

    @Test
    public void inValidBase64MissingPluginDotTomlWillReturnFalse() {
        Set<String> plugins = new HashSet<>();
        plugins.add("base64://" + Base64.encodeBase64String("recipe-pre-install:\nrecipe-post-install:".getBytes()));
        Assert.assertFalse(underTest.isValid(plugins, constraintValidatorContext));
    }
}