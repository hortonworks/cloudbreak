package com.sequenceiq.cloudbreak.controller.validation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

import com.sequenceiq.cloudbreak.api.model.PluginExecutionType;
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
        Map<String, PluginExecutionType> plugins = new HashMap<>();
        plugins.put("http://github.com/user/consul-plugins-plugin1.git", PluginExecutionType.ALL_NODES);
        plugins.put("https://github.com/user/consul-plugins-plugin1.git", PluginExecutionType.ALL_NODES);
        plugins.put("git://github.com/user/consul-plugins-plugin1.git", PluginExecutionType.ALL_NODES);
        plugins.put("base64://" + Base64.encodeBase64String("plugin.toml:\nrecipe-pre-install:".getBytes()), PluginExecutionType.ALL_NODES);
        Assert.assertTrue(underTest.isValid(plugins, constraintValidatorContext));
    }

    @Test
    public void inValidPluginNullReturnFalse() {
        Assert.assertEquals(underTest.isValid(null, constraintValidatorContext), false);
    }

    @Test
    public void inValidPluginEmptyReturnFalse() {
        Assert.assertFalse(underTest.isValid(Collections.<String, PluginExecutionType>emptyMap(), constraintValidatorContext));
    }

    @Test
    public void inValidPluginUrlJsonWillReturnFalse() {
        Map<String, PluginExecutionType> plugins = new HashMap<>();
        plugins.put("asd://github.com/user/plugin1.git", PluginExecutionType.ALL_NODES);
        Assert.assertFalse(underTest.isValid(plugins, constraintValidatorContext));
    }

    @Test
    public void inValidBase64MissingScriptWillReturnFalse() {
        Map<String, PluginExecutionType> plugins = new HashMap<>();
        plugins.put("base64://" + Base64.encodeBase64String("plugin.toml:".getBytes()), PluginExecutionType.ALL_NODES);
        Assert.assertFalse(underTest.isValid(plugins, constraintValidatorContext));
    }

    @Test
    public void inValidBase64MissingPluginDotTomlWillReturnFalse() {
        Map<String, PluginExecutionType> plugins = new HashMap<>();
        plugins.put("base64://" + Base64.encodeBase64String("recipe-pre-install:\nrecipe-post-install:".getBytes()), PluginExecutionType.ALL_NODES);
        Assert.assertFalse(underTest.isValid(plugins, constraintValidatorContext));
    }
}