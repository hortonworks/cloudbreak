package com.sequenceiq.cloudbreak.api.model.imagecatalog;

import static com.sequenceiq.cloudbreak.validation.ImageCatalogValidator.FAILED_TO_GET_WITH_EXCEPTION;
import static com.sequenceiq.cloudbreak.validation.ImageCatalogValidator.INVALID_JSON_IN_RESPONSE;
import static com.sequenceiq.cloudbreak.validation.ImageCatalogValidator.INVALID_JSON_STRUCTURE_IN_RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.validation.Configuration;
import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.base.ImageCatalogV4Base;
import com.sequenceiq.cloudbreak.api.helper.HttpHelper;
import com.sequenceiq.cloudbreak.validation.HttpContentSizeValidator;
import com.sequenceiq.cloudbreak.validation.ImageCatalogValidator;

@RunWith(MockitoJUnitRunner.class)
public class ImageCatalogV4BaseTest {

    public static final String FAILED_TO_GET_BY_FAMILY_TYPE = "Failed to get response by the specified URL '%s' due to: '%s'!";

    private static final String INVALID_MESSAGE = "A valid image catalog must be available on the given URL";

    @Mock
    private StatusType statusType;

    @Mock
    private HttpHelper httpHelper;

    @Mock
    private HttpContentSizeValidator httpContentSizeValidator;

    private Validator validator;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        Configuration<?> cfg = Validation.byDefaultProvider().configure();
        cfg.messageInterpolator(new ParameterMessageInterpolator());
        validator = cfg.buildValidatorFactory().getValidator();

        for (Entry<String, Object> entry : Map.of("HTTP_CONTENT_SIZE_VALIDATOR", httpContentSizeValidator, "HTTP_HELPER", httpHelper).entrySet()) {
            Field field = ReflectionUtils.findField(ImageCatalogValidator.class, entry.getKey());
            field.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            field.set(null, entry.getValue());
        }

        when(httpContentSizeValidator.isValid(anyString(), any(ConstraintValidatorContext.class))).thenReturn(true);
        when(statusType.getFamily()).thenReturn(Family.SUCCESSFUL);
    }

    @Test
    public void testContentNotAvailable() {
        String url = "http://protocol.com";
        String reasonPhrase = "Invalid reason phrase";
        when(statusType.getFamily()).thenReturn(Family.OTHER);
        when(statusType.getReasonPhrase()).thenReturn(reasonPhrase);
        when(httpHelper.getContent(anyString())).thenReturn(new ImmutablePair<>(statusType, ""));

        ImageCatalogV4Base i = new ImageCatalogV4Base();
        i.setName("testname");
        i.setUrl(url);

        Set<ConstraintViolation<ImageCatalogV4Base>> violations = validator.validate(i);

        assertEquals(2L, violations.size());
        String failedToGetMessage = String.format(FAILED_TO_GET_BY_FAMILY_TYPE, url, reasonPhrase);
        assertTrue(violations.stream().allMatch(cv -> cv.getMessage().equals(INVALID_MESSAGE) || cv.getMessage().equals(failedToGetMessage)));
    }

    @Test
    public void testContentStructureNotValid() {
        when(httpHelper.getContent(anyString())).thenReturn(new ImmutablePair<>(statusType, "{}"));

        ImageCatalogV4Base i = new ImageCatalogV4Base();
        i.setName("testname");
        i.setUrl("http://protocol.com");

        Set<ConstraintViolation<ImageCatalogV4Base>> violations = validator.validate(i);

        assertEquals(2L, violations.size());
        assertTrue(violations.stream().allMatch(cv -> cv.getMessage().equals(INVALID_MESSAGE) || cv.getMessage().equals(INVALID_JSON_STRUCTURE_IN_RESPONSE)));
    }

    @Test
    public void testContentNotAValidJSON() {
        when(httpHelper.getContent(anyString())).thenReturn(new ImmutablePair<>(statusType, "{[]}"));

        ImageCatalogV4Base i = new ImageCatalogV4Base();
        i.setName("testname");
        i.setUrl("http://protocol.com");

        Set<ConstraintViolation<ImageCatalogV4Base>> violations = validator.validate(i);

        assertEquals(2L, violations.size());
        assertTrue(violations.stream().allMatch(cv -> cv.getMessage().equals(INVALID_MESSAGE) || cv.getMessage().equals(INVALID_JSON_IN_RESPONSE)));
    }

    @Test
    public void testWhenWebTargetFailesWithException() {
        when(httpHelper.getContent(anyString())).thenThrow(ProcessingException.class);

        ImageCatalogV4Base i = new ImageCatalogV4Base();
        i.setName("testname");
        i.setUrl("http://protocol.com");

        Set<ConstraintViolation<ImageCatalogV4Base>> violations = validator.validate(i);

        assertEquals(2L, violations.size());
        assertTrue(violations.stream().allMatch(cv -> cv.getMessage().equals(INVALID_MESSAGE) || cv.getMessage().equals(FAILED_TO_GET_WITH_EXCEPTION)));
    }

    @Test
    public void testWhenContentIsTooBig() {
        when(httpContentSizeValidator.isValid(anyString(), any(ConstraintValidatorContext.class))).thenReturn(false);

        ImageCatalogV4Base i = new ImageCatalogV4Base();
        i.setName("testname");
        i.setUrl("http://protocol.com");

        Set<ConstraintViolation<ImageCatalogV4Base>> violations = validator.validate(i);

        assertEquals(1L, violations.size());
        assertTrue(violations.stream().allMatch(cv -> cv.getMessage().equals(INVALID_MESSAGE)));
    }
}