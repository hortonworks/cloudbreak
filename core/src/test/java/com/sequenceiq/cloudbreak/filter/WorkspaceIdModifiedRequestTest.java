package com.sequenceiq.cloudbreak.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WorkspaceIdModifiedRequestTest {

    private static final String URL_WITHOUT_WORKSPACE_ID = "http://localhost/v4/user_profiles";

    private static final String URL_WITH_DIFFERENT_WORKSPACE_ID = "http://localhost/v4/2/databases";

    private static final String URL_WITH_TENANT_DEFAULT_WORKSPACE_ID = "http://localhost/v4/1/databases";

    @Mock
    private HttpServletRequest request;

    private WorkspaceIdModifiedRequest underTest;

    @Before
    public void before() {
        underTest = new WorkspaceIdModifiedRequest(request, 1L);
    }

    @Test
    public void testGetUriWithoutWorkspaceId() {
        when(request.getRequestURI()).thenReturn(URL_WITHOUT_WORKSPACE_ID);
        assertEquals(URL_WITHOUT_WORKSPACE_ID, underTest.getRequestURI());
    }

    @Test
    public void testGetUriWithWorkspace() {
        when(request.getRequestURI()).thenReturn(URL_WITH_DIFFERENT_WORKSPACE_ID);
        assertEquals(URL_WITH_TENANT_DEFAULT_WORKSPACE_ID, underTest.getRequestURI());
    }

    @Test
    public void testGetURLWithoutWorkspaceId() {
        when(request.getRequestURL()).thenReturn(new StringBuffer(URL_WITHOUT_WORKSPACE_ID));
        assertTrue(StringUtils.equals(URL_WITHOUT_WORKSPACE_ID, underTest.getRequestURL()));
    }

    @Test
    public void testGetURLWithWorkspace() {
        when(request.getRequestURL()).thenReturn(new StringBuffer(URL_WITH_DIFFERENT_WORKSPACE_ID));
        assertTrue(StringUtils.equals(URL_WITH_TENANT_DEFAULT_WORKSPACE_ID, underTest.getRequestURL()));
    }
}
