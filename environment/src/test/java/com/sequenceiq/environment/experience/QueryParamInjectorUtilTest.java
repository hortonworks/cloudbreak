package com.sequenceiq.environment.experience;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Map;

import javax.ws.rs.client.WebTarget;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class QueryParamInjectorUtilTest {

    @Mock
    private WebTarget webTarget;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSetQueryParamsWhenWebTargetIsNullThenIllegalArgumentExceptionShouldCome() {
        IllegalArgumentException expectedException = assertThrows(
                IllegalArgumentException.class,
                () -> QueryParamInjectorUtil.setQueryParams(null, emptyMap()));

        assertEquals("Input WebTarget should not be null!", expectedException.getMessage());
    }

    @Test
    void testSetQueryParamsWhenMapIsEmptyThenNoQueryParamShouldBeAdded() {
        QueryParamInjectorUtil.setQueryParams(webTarget, emptyMap());

        verify(webTarget, times(0)).queryParam(any(), any());
    }

    @Test
    void testSetQueryParamsWhenMapIsNullThenNoQueryParamShouldBeAdded() {
        QueryParamInjectorUtil.setQueryParams(webTarget, null);

        verify(webTarget, times(0)).queryParam(any(), any());
    }

    @Test
    void testSetQueryParamsWhenMapIsNotEmptyThenQueryParamShouldBeAdded() {
        String key = "alma";
        String value = "fa";
        QueryParamInjectorUtil.setQueryParams(webTarget, Map.of(key, value));

        verify(webTarget, times(1)).queryParam(any(), any());
        verify(webTarget, times(1)).queryParam(key, value);
    }

}