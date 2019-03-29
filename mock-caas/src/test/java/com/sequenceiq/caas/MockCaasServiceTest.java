package com.sequenceiq.caas;

import static javax.servlet.http.HttpServletResponse.SC_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.InvalidParameterException;
import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.NotFoundException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.caas.model.TokenResponse;
import com.sequenceiq.caas.service.MockCaasService;
import com.sequenceiq.caas.util.JsonUtil;

public class MockCaasServiceTest {

    private static final String LOCATION_HEADER_KEY = "Location";

    private static final String TENANT = "tenantValue";

    private static final String USERNAME = "usernameValue";

    private static final String OTHER_PAGE = "authorize.html";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private JsonUtil jsonUtil;

    @InjectMocks
    private MockCaasService underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(jsonUtil.toJsonString(any())).thenReturn("{}");
    }

    @Test
    public void testGetUserInfoWhenRequestDoesNotContainCookieThenExceptionComes() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new Cookie[0]);

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage("Can not retrieve user from token");

        underTest.getUserInfo(request);
    }

    @Test
    public void testGetUserInfoWhenRequestDoesNotContainTheRequiredTokenThenExceptionComes() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie cookie = new Cookie("someInvalidId", "someOtherValue");
        when(request.getCookies()).thenReturn(new Cookie[] {cookie});

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage("Can not retrieve user from token");

        underTest.getUserInfo(request);
    }

    @Test
    public void testGetTokenResponseWhenAuthorizationCodeIsNotNullThenItsValueShouldReturnAsBothAccessAndRefreshTokenValue() {
        String expected = "someAuthorizationValue";
        TokenResponse result = underTest.getTokenResponse(expected, null);

        Assert.assertEquals(expected, result.getAccessToken());
        Assert.assertEquals(expected, result.getRefreshToken());
    }

    @Test
    public void testGetTokenResponseWhenRefreshTokenIsNotNullThenItsValueShouldReturnAsBothAccessAndRefreshTokenValue() {
        String expected = "someRefreshTokenValue";
        TokenResponse result = underTest.getTokenResponse(null, expected);

        Assert.assertEquals(expected, result.getAccessToken());
        Assert.assertEquals(expected, result.getRefreshToken());
    }

    @Test
    public void testGetTokenResponseWhenBothAuthorizationCodeAndRefreshTokenIsNullThenExceptionComes() {
        expectedException.expect(InvalidParameterException.class);

        underTest.getTokenResponse(null, null);
    }

    @Test
    public void testAuthorizeWhenNeitherTenantAndUsernameGivenAndActiveTrueThenRedirectingToAnotherPageAndNullReturns() {
        HttpServletResponse response = mock(HttpServletResponse.class);

        String result = underTest.authorize(response, Optional.empty(), Optional.empty(), Optional.empty(), true);

        Assert.assertNull(result);
        verify(response, times(1)).setHeader(anyString(), anyString());
        verify(response, times(1)).setHeader(LOCATION_HEADER_KEY, OTHER_PAGE);
        verify(response, times(1)).setStatus(anyInt());
        verify(response, times(1)).setStatus(SC_FOUND);
    }

    @Test
    public void testAuthorizeWhenNeitherTenantAndUsernameGivenAndActiveFalseThenRedirectingToAnotherPageAndNullReturns() {
        HttpServletResponse response = mock(HttpServletResponse.class);

        String result = underTest.authorize(response, Optional.empty(), Optional.empty(), Optional.empty(), false);

        Assert.assertNull(result);
        verify(response, times(1)).setHeader(anyString(), anyString());
        verify(response, times(1)).setHeader(LOCATION_HEADER_KEY, OTHER_PAGE);
        verify(response, times(1)).setStatus(anyInt());
        verify(response, times(1)).setStatus(SC_FOUND);
    }

    @Test
    public void testAuthorizeWhenOnlyTenantGivenAndActiveFalseThenRedirectingToAnotherPageAndNullReturns() {
        HttpServletResponse response = mock(HttpServletResponse.class);

        String result = underTest.authorize(response, Optional.of(TENANT), Optional.empty(), Optional.empty(), false);

        Assert.assertNull(result);
        verify(response, times(1)).setHeader(anyString(), anyString());
        verify(response, times(1)).setHeader(LOCATION_HEADER_KEY, OTHER_PAGE);
        verify(response, times(1)).setStatus(anyInt());
        verify(response, times(1)).setStatus(SC_FOUND);
    }

    @Test
    public void testAuthorizeWhenOnlyTenantGivenAndActiveTrueThenRedirectingToAnotherPageAndNullReturns() {
        HttpServletResponse response = mock(HttpServletResponse.class);

        String result = underTest.authorize(response, Optional.of(TENANT), Optional.empty(), Optional.empty(), true);

        Assert.assertNull(result);
        verify(response, times(1)).setHeader(anyString(), anyString());
        verify(response, times(1)).setHeader(LOCATION_HEADER_KEY, OTHER_PAGE);
        verify(response, times(1)).setStatus(anyInt());
        verify(response, times(1)).setStatus(SC_FOUND);
    }

    @Test
    public void testAuthorizeWhenOnlyUsernameGivenAndActiveFalseThenRedirectingToAnotherPageAndNullReturns() {
        HttpServletResponse response = mock(HttpServletResponse.class);

        String result = underTest.authorize(response, Optional.empty(), Optional.of(USERNAME), Optional.empty(), false);

        Assert.assertNull(result);
        verify(response, times(1)).setHeader(anyString(), anyString());
        verify(response, times(1)).setHeader(LOCATION_HEADER_KEY, OTHER_PAGE);
        verify(response, times(1)).setStatus(anyInt());
        verify(response, times(1)).setStatus(SC_FOUND);
    }

    @Test
    public void testAuthorizeWhenOnlyUsernameGivenAndActiveTrueThenRedirectingToAnotherPageAndNullReturns() {
        HttpServletResponse response = mock(HttpServletResponse.class);

        String result = underTest.authorize(response, Optional.empty(), Optional.of(USERNAME), Optional.empty(), true);

        Assert.assertNull(result);
        verify(response, times(1)).setHeader(anyString(), anyString());
        verify(response, times(1)).setHeader(LOCATION_HEADER_KEY, OTHER_PAGE);
        verify(response, times(1)).setStatus(anyInt());
        verify(response, times(1)).setStatus(SC_FOUND);
    }

    @Test
    public void testAuthorizeWhenBothUsernameAndTenantGivenAndActiveFalseButThereIsNoRedirectUriThenTokenReturns() {
        HttpServletResponse response = mock(HttpServletResponse.class);

        String result = underTest.authorize(response, Optional.of(TENANT), Optional.of(USERNAME), Optional.empty(), false);

        Assert.assertNotNull(result);
        verify(response, times(0)).setHeader(anyString(), anyString());
        verify(response, times(0)).setStatus(anyInt());
    }

    @Test
    public void testAuthorizeWhenBothUsernameAndTenantGivenAndActiveTrueButThereIsNoRedirectUriThenTokenReturns() {
        HttpServletResponse response = mock(HttpServletResponse.class);

        String result = underTest.authorize(response, Optional.of(TENANT), Optional.of(USERNAME), Optional.empty(), true);

        Assert.assertNotNull(result);
        verify(response, times(0)).setHeader(anyString(), anyString());
        verify(response, times(0)).setStatus(anyInt());
    }

    @Test
    public void testAuthorizeWhenBothUsernameAndTenantGivenAndActiveFalseAndThereIsARedirectUriThenRedirectionHasBeenSetAsHeaderWithAuthCodeAndNullReturns() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        String redirectUri = "someUriValue";

        String result = underTest.authorize(response, Optional.of(TENANT), Optional.of(USERNAME), Optional.of(redirectUri), false);

        Assert.assertNull(result);
        verify(response, times(1)).setHeader(anyString(), anyString());
        verify(response, times(1)).setHeader(eq(LOCATION_HEADER_KEY), anyString());
        verify(response, times(1)).setStatus(anyInt());
        verify(response, times(1)).setStatus(SC_FOUND);
    }

    @Test
    public void testAuthorizeWhenBothUsernameAndTenantGivenAndActiveTrueAndThereIsARedirectUriThenRedirectionHasBeenSetAsHeaderWithAuthCodeAndNullReturns() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        String redirectUri = "someUriValue";

        String result = underTest.authorize(response, Optional.of(TENANT), Optional.of(USERNAME), Optional.of(redirectUri), true);

        Assert.assertNull(result);
        verify(response, times(1)).setHeader(anyString(), anyString());
        verify(response, times(1)).setHeader(eq(LOCATION_HEADER_KEY), anyString());
        verify(response, times(1)).setStatus(anyInt());
        verify(response, times(1)).setStatus(SC_FOUND);
    }

}
