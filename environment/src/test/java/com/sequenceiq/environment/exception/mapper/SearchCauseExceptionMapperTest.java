package com.sequenceiq.environment.exception.mapper;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Providers;

import org.glassfish.jersey.internal.ExceptionMapperFactory;
import org.glassfish.jersey.internal.JaxrsProviders;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.ServiceHolder;
import org.glassfish.jersey.internal.inject.ServiceHolderImpl;
import org.glassfish.jersey.spi.ExceptionMappers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.service.GetCloudParameterException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = TestExceptionMapperConfiguration.class)
public class SearchCauseExceptionMapperTest {

    @Inject
    private GetCloudParameterExceptionMapper underTest;

    @MockBean
    private InjectionManager injectionManager;

    @Inject
    private List<ExceptionMapper> exceptionMappers;

    @BeforeEach
    public void setup() {
        List<ServiceHolder<ExceptionMapper>> ret = exceptionMappers.stream().map(em -> new ServiceHolderImpl<>(em, emptySet())).collect(toList());
        when(injectionManager.getAllServiceHolders(ExceptionMapper.class)).thenReturn(ret);
        Provider<ExceptionMappers> mappers = () -> new ExceptionMapperFactory(injectionManager);

        Providers providers = new JaxrsProviders(null, null, mappers);

        ReflectionTestUtils.setField(underTest, "providers", providers);
    }

    @Test
    public void testGetResponseStatusWhenNoCause() {
        Response.StatusType actual = underTest.getResponseStatus(new GetCloudParameterException(""));
        assertEquals(Response.Status.BAD_REQUEST, actual);
    }

    @Test
    public void testGetResponseStatusWhenHasOnlyOneCause() {
        Response.StatusType actual = underTest.getResponseStatus(new GetCloudParameterException("", new NotFoundException()));
        assertEquals(Response.Status.NOT_FOUND, actual);
    }

    @Test
    public void testGetResponseStatusWhenHasTwoDepthsCause() {
        Response.StatusType actual = underTest.getResponseStatus(new GetCloudParameterException("", new RuntimeException(new NotFoundException())));
        assertEquals(Response.Status.NOT_FOUND, actual);
    }

    @Test
    public void testGetResponseStatusWhenHasTwoDepthsCauseWithDefaultMapper() {
        Response.StatusType actual = underTest.getResponseStatus(new GetCloudParameterException("", new Exception(new Exception())));
        assertEquals(Response.Status.BAD_REQUEST, actual);
    }

    @Test
    public void testGetResponseStatusWhenHasOneDepthsCauseWithDefaultMapper() {
        Response.StatusType actual = underTest.getResponseStatus(new GetCloudParameterException("", new Exception()));
        assertEquals(Response.Status.BAD_REQUEST, actual);
    }

    @Test
    public void testGetResponseStatusWhenTheExceptionIsTheFirstCause() {
        Response.StatusType actual = underTest.getResponseStatus(new GetCloudParameterException("", new Exception(new NotFoundException())));
        assertEquals(Response.Status.NOT_FOUND, actual);
    }

    @Test
    public void testGetResponseStatusWhenTheCauseIsTheRealException() {
        Response.StatusType actual = underTest.getResponseStatus(new GetCloudParameterException("", new NotFoundException(new Exception())));
        assertEquals(Response.Status.NOT_FOUND, actual);
    }
}
