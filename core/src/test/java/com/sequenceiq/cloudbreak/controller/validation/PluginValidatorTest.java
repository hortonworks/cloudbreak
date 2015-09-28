package com.sequenceiq.cloudbreak.controller.validation;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintTarget;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import javax.validation.metadata.ConstraintDescriptor;

import org.apache.commons.codec.binary.Base64;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.common.type.PluginExecutionType;

@RunWith(MockitoJUnitRunner.class)
public class PluginValidatorTest {

    @InjectMocks
    private PluginValidator underTest;

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Mock
    private ValidPlugin validPlugin;

    @Before
    public void setUp() {
        underTest.initialize(validPlugin);
        given(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString())).willReturn(
                new ConstraintValidatorContextImpl(
                        new ArrayList<String>(),
                        PathImpl.createRootPath(),
                        new DummyConstraintDescriptor()
                ).buildConstraintViolationWithTemplate("dummytemplate")
        );
    }

    @Test
    public void validPluginJsonWillReturnTrue() {
        Map<String, PluginExecutionType> plugins = new HashMap<>();
        plugins.put("http://github.com/user/consul-plugins-plugin1.git", PluginExecutionType.ALL_NODES);
        plugins.put("https://github.com/user/consul-plugins-plugin1.git", PluginExecutionType.ALL_NODES);
        plugins.put("git://github.com/user/consul-plugins-plugin1.git", PluginExecutionType.ALL_NODES);
        plugins.put("base64://" + Base64.encodeBase64String("plugin.toml:\nrecipe-pre-install:".getBytes()), PluginExecutionType.ALL_NODES);
        assertEquals(underTest.isValid(plugins, constraintValidatorContext), true);
    }

    @Test
    public void inValidPluginNullReturnFalse() {
        assertEquals(underTest.isValid(null, constraintValidatorContext), false);
    }

    @Test
    public void inValidPluginEmptyReturnFalse() {
        assertEquals(underTest.isValid(Collections.<String, PluginExecutionType>emptyMap(), constraintValidatorContext), false);
    }

    @Test
    public void inValidPluginUrlJsonWillReturnFalse() {
        Map<String, PluginExecutionType> plugins = new HashMap<>();
        plugins.put("asd://github.com/user/plugin1.git", PluginExecutionType.ALL_NODES);
        assertEquals(underTest.isValid(plugins, constraintValidatorContext), false);
    }

    @Test
    public void inValidBase64MissingScriptWillReturnFalse() {
        Map<String, PluginExecutionType> plugins = new HashMap<>();
        plugins.put("base64://" + Base64.encodeBase64String("plugin.toml:".getBytes()), PluginExecutionType.ALL_NODES);
        assertEquals(underTest.isValid(plugins, constraintValidatorContext), false);
    }

    @Test
    public void inValidBase64MissingPluginDotTomlWillReturnFalse() {
        Map<String, PluginExecutionType> plugins = new HashMap<>();
        plugins.put("base64://" + Base64.encodeBase64String("recipe-pre-install:\nrecipe-post-install:".getBytes()), PluginExecutionType.ALL_NODES);
        assertEquals(underTest.isValid(plugins, constraintValidatorContext), false);
    }

    private class DummyAnnotation implements Annotation {

        @Override
        public boolean equals(Object obj) {
            return false;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String toString() {
            return "dummy";
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return getClass();
        }
    }

    private class DummyConstraintDescriptor implements ConstraintDescriptor<DummyAnnotation>, Serializable {

        @Override
        public DummyAnnotation getAnnotation() {
            return null;
        }

        @Override
        public String getMessageTemplate() {
            return "";
        }

        @Override
        public Set<Class<?>> getGroups() {
            return new HashSet<>();
        }

        @Override
        public Set<Class<? extends Payload>> getPayload() {
            return new HashSet<>();
        }

        @Override
        public ConstraintTarget getValidationAppliesTo() {
            return ConstraintTarget.PARAMETERS;
        }

        @Override
        public List<Class<? extends ConstraintValidator<DummyAnnotation, ?>>> getConstraintValidatorClasses() {
            return new ArrayList<>();
        }

        @Override
        public Map<String, Object> getAttributes() {
            return new HashMap<>();
        }

        @Override
        public Set<ConstraintDescriptor<?>> getComposingConstraints() {
            return new HashSet<>();
        }

        @Override
        public boolean isReportAsSingleViolation() {
            return false;
        }
    }
}