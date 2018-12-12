package com.sequenceiq.cloudbreak.api.model.imagecatalog;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.validation.Configuration;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;

import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.validation.ImageCatalogValidator;

@RunWith(MockitoJUnitRunner.class)
public class ImageCatalogBaseTest {

    private static final String VALID_CATALOG = "{\"images\": null, \"versions\": null}";

    private static final String INVALID_MESSAGE = "A valid image catalog must be available on the given URL";

    @Mock
    private Client client;

    @Mock
    private WebTarget webTarget;

    @Mock
    private Builder builder;

    @Mock
    private StatusType statusType;

    private Validator validator;

    @Before
    public void setUp() throws IllegalAccessException, NoSuchFieldException {
        Configuration<?> cfg = Validation.byDefaultProvider().configure();
        cfg.messageInterpolator(new ParameterMessageInterpolator());
        validator = cfg.buildValidatorFactory().getValidator();

        Field field = ReflectionUtils.findField(ImageCatalogValidator.class, "CLIENT");
        field.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, client);

        when(client.target(anyString())).thenReturn(webTarget);
        when(webTarget.request()).thenReturn(builder);
        when(builder.get()).thenReturn(this.new DummyResponse() {
        });
        when(statusType.getFamily()).thenReturn(Family.SUCCESSFUL);
    }

    @Test
    public void testUrlWrongProtocol() {
        ImageCatalogBase i = new ImageCatalogBase();
        i.setName("testname");
        i.setUrl("ftp://protocol.com");
        validateAndAssertInvalidCatalog(i);
    }

    @Test
    public void testUrlWithoutProtocol() {
        ImageCatalogBase i = new ImageCatalogBase();
        i.setName("testname");
        i.setUrl("without.protocol.com");
        validateAndAssertInvalidCatalog(i);
    }

    @Test
    public void testContentNotAvailable() {
        when(statusType.getFamily()).thenReturn(Family.OTHER);

        ImageCatalogBase i = new ImageCatalogBase();
        i.setName("testname");
        i.setUrl("http://protocol.com");
        validateAndAssertInvalidCatalog(i);
    }

    @Test
    public void testContentNotValid() {
        when(builder.get()).thenReturn(this.new DummyResponse() {
            @Override
            public <T> T readEntity(Class<T> entityType) {
                return (T) "{}";
            }
        });

        ImageCatalogBase i = new ImageCatalogBase();
        i.setName("testname");
        i.setUrl("http://protocol.com");
        validateAndAssertInvalidCatalog(i);
    }

    @Test
    public void testUrlHttp() {
        ImageCatalogBase i = new ImageCatalogBase();
        i.setName("testname");
        i.setUrl("http://protocol.com");
        validateAndAssertValidCatalog(i);
    }

    @Test
    public void testUrlHttps() {
        ImageCatalogBase i = new ImageCatalogBase();
        i.setName("testname");
        i.setUrl("http://protocol.com");
        validateAndAssertValidCatalog(i);
    }

    private void validateAndAssertInvalidCatalog(ImageCatalogBase i) {
        Set<ConstraintViolation<ImageCatalogBase>> violations = validator.validate(i);
        assertEquals(1L, violations.size());
        ConstraintViolation<ImageCatalogBase> v = violations.toArray(new ConstraintViolation[1])[0];
        assertEquals(INVALID_MESSAGE, v.getMessage());
    }

    private void validateAndAssertValidCatalog(ImageCatalogBase i) {
        Set<ConstraintViolation<ImageCatalogBase>> violations = validator.validate(i);
        assertEquals(0L, violations.size());
    }

    private abstract class DummyResponse extends Response {

        @Override
        public int getStatus() {
            return 200;
        }

        @Override
        public StatusType getStatusInfo() {
            return statusType;
        }

        @Override
        public Object getEntity() {
            return null;
        }

        @Override
        public <T> T readEntity(Class<T> entityType) {
            return (T) VALID_CATALOG;
        }

        @Override
        public <T> T readEntity(GenericType<T> entityType) {
            return null;
        }

        @Override
        public <T> T readEntity(Class<T> entityType, Annotation[] annotations) {
            return null;
        }

        @Override
        public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations) {
            return null;
        }

        @Override
        public boolean hasEntity() {
            return false;
        }

        @Override
        public boolean bufferEntity() {
            return false;
        }

        @Override
        public void close() {

        }

        @Override
        public MediaType getMediaType() {
            return null;
        }

        @Override
        public Locale getLanguage() {
            return null;
        }

        @Override
        public int getLength() {
            return 0;
        }

        @Override
        public Set<String> getAllowedMethods() {
            return null;
        }

        @Override
        public Map<String, NewCookie> getCookies() {
            return null;
        }

        @Override
        public EntityTag getEntityTag() {
            return null;
        }

        @Override
        public Date getDate() {
            return null;
        }

        @Override
        public Date getLastModified() {
            return null;
        }

        @Override
        public URI getLocation() {
            return null;
        }

        @Override
        public Set<Link> getLinks() {
            return null;
        }

        @Override
        public boolean hasLink(String relation) {
            return false;
        }

        @Override
        public Link getLink(String relation) {
            return null;
        }

        @Override
        public Link.Builder getLinkBuilder(String relation) {
            return null;
        }

        @Override
        public MultivaluedMap<String, Object> getMetadata() {
            return null;
        }

        @Override
        public MultivaluedMap<String, String> getStringHeaders() {
            return null;
        }

        @Override
        public String getHeaderString(String name) {
            return null;
        }
    }
}