package com.sequenceiq.cloudbreak.api.model.imagecatalog;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import javax.validation.Configuration;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.validation.ImageCatalogValidator;
import com.sequenceiq.cloudbreak.validation.bean.ImageCatalogProvider;

@RunWith(MockitoJUnitRunner.class)
public class ImageCatalogBaseTest {

    @Mock
    private ImageCatalogProvider imageCatalogProvider;

    private Validator validator;

    @Before
    public void setUp() {
        Configuration<?> cfg = Validation.byDefaultProvider().configure();
        cfg.messageInterpolator(new ParameterMessageInterpolator());
        validator = cfg.buildValidatorFactory().getValidator();

        ReflectionTestUtils.setField(ImageCatalogValidator.class, "imageCatalogProvider", imageCatalogProvider);
    }

    @Test
    public void testUrlHttp() {
        ImageCatalogBase i = new ImageCatalogBase();
        i.setName("testname");
        i.setUrl("http://protocol.com");
        Set<ConstraintViolation<ImageCatalogBase>> violations = validator.validate(i);
        assertEquals(0L, violations.size());
    }

    @Test
    public void testUrlHttps() {
        ImageCatalogBase i = new ImageCatalogBase();
        i.setName("testname");
        i.setUrl("http://protocol.com");
        Set<ConstraintViolation<ImageCatalogBase>> violations = validator.validate(i);
        assertEquals(0L, violations.size());
    }

    @Test
    public void testUrlWrongProtocol() {
        ImageCatalogBase i = new ImageCatalogBase();
        i.setName("testname");
        i.setUrl("ftp://protocol.com");
        Set<ConstraintViolation<ImageCatalogBase>> violations = validator.validate(i);
        assertEquals(1L, violations.size());
        ConstraintViolation<ImageCatalogBase> v = violations.toArray(new ConstraintViolation[1])[0];
        assertEquals("A valid image catalog must be available on the given URL", v.getMessage());
    }

    @Test
    public void testUrlWithoutProtocol() {
        ImageCatalogBase i = new ImageCatalogBase();
        i.setName("testname");
        i.setUrl("without.protocol.com");
        Set<ConstraintViolation<ImageCatalogBase>> violations = validator.validate(i);
        assertEquals(1L, violations.size());
        ConstraintViolation<ImageCatalogBase> v = violations.toArray(new ConstraintViolation[1])[0];
        assertEquals("A valid image catalog must be available on the given URL", v.getMessage());
    }

}