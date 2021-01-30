package com.sequenceiq.environment.exception.mapper;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Providers;

import org.glassfish.jersey.internal.ExceptionMapperFactory;
import org.glassfish.jersey.internal.JaxrsProviders;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.ServiceHolder;
import org.glassfish.jersey.internal.inject.ServiceHolderImpl;
import org.glassfish.jersey.spi.ExceptionMappers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.environment.credential.exception.CredentialVerificationException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = TestExceptionMapperConfiguration.class)
public class SearchCauseExceptionMapperTest {

    @Inject
    private CredentialVerificationExceptionMapper underTest;

    @MockBean
    private InjectionManager injectionManager;

    @Inject
    private List<ExceptionMapper> exceptionMappers;

    @BeforeEach
    public void setup() {
        List<ServiceHolder<ExceptionMapper>> ret = exceptionMappers.stream().map(em -> new ServiceHolderImpl<>(em, emptySet())).collect(toList());
        Mockito.when(injectionManager.getAllServiceHolders(ExceptionMapper.class)).thenReturn(ret);
        Provider<ExceptionMappers> mappers = () -> new ExceptionMapperFactory(injectionManager);

        Providers providers = new JaxrsProviders();

        ReflectionTestUtils.setField(providers, "mappers", mappers);
        ReflectionTestUtils.setField(underTest, "providers", providers);
    }

    @Test
    public void testGetResponseStatusWhenNoCause() {
        Response.Status actual = underTest.getResponseStatus(new CredentialVerificationException(""));
        Assertions.assertEquals(Response.Status.BAD_REQUEST, actual);
    }

    @Test
    public void testGetResponseStatusWhenHasOnlyOneCause() {
        Response.Status actual = underTest.getResponseStatus(new CredentialVerificationException("", new NotFoundException()));
        Assertions.assertEquals(Response.Status.NOT_FOUND, actual);
    }

    @Test
    public void testGetResponseStatusWhenHasTwoDepthsCause() {
        Response.Status actual = underTest.getResponseStatus(new CredentialVerificationException("", new RuntimeException(new NotFoundException())));
        Assertions.assertEquals(Response.Status.NOT_FOUND, actual);
    }

    @Test
    public void testGetResponseStatusWhenHasTwoDepthsCauseWithDefaultMapper() {
        Response.Status actual = underTest.getResponseStatus(new CredentialVerificationException("", new Exception(new Exception())));
        Assertions.assertEquals(Response.Status.BAD_REQUEST, actual);
    }

    @Test
    public void testGetResponseStatusWhenHasOneDepthsCauseWithDefaultMapper() {
        Response.Status actual = underTest.getResponseStatus(new CredentialVerificationException("", new Exception()));
        Assertions.assertEquals(Response.Status.BAD_REQUEST, actual);
    }

    @Test
    public void testGetResponseStatusWhenTheExceptionIsTheFirstCause() {
        Response.Status actual = underTest.getResponseStatus(new CredentialVerificationException("", new Exception(new NotFoundException())));
        Assertions.assertEquals(Response.Status.NOT_FOUND, actual);
    }

    @Test
    public void testGetResponseStatusWhenTheCauseIsTheRealException() {
        Response.Status actual = underTest.getResponseStatus(new CredentialVerificationException("", new NotFoundException(new Exception())));
        Assertions.assertEquals(Response.Status.NOT_FOUND, actual);
    }
}
