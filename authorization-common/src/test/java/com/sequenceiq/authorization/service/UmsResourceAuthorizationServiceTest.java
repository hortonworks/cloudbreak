package com.sequenceiq.authorization.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;

@RunWith(MockitoJUnitRunner.class)
public class UmsResourceAuthorizationServiceTest {

    private static final String USER_ID = "userId";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:" + USER_ID;

    private static final String RESOURCE_CRN = "crn:cdp:datalake:us-west-1:1234:resource:1";

    private static final String RESOURCE_CRN2 = "crn:cdp:datalake:us-west-1:1234:resource:2";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private GrpcUmsClient umsClient;

    @InjectMocks
    private UmsResourceAuthorizationService underTest;

    @Test
    public void testCheckReadRightOnResource() {
        when(umsClient.checkRight(anyString(), anyString(), anyString(), anyString(), any())).thenReturn(false);

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("You have no right to perform datalake/write on resource " + RESOURCE_CRN);

        underTest.checkRightOfUserOnResource(USER_CRN, AuthorizationResourceType.DATALAKE, AuthorizationResourceAction.WRITE, RESOURCE_CRN);
    }

    @Test
    public void testCheckWriteRightOnResource() {
        when(umsClient.checkRight(anyString(), anyString(), anyString(), anyString(), any())).thenReturn(false);

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("You have no right to perform datalake/read on resource " + RESOURCE_CRN);

        underTest.checkRightOfUserOnResource(USER_CRN, AuthorizationResourceType.DATALAKE, AuthorizationResourceAction.READ, RESOURCE_CRN);
    }

    @Test
    public void testCheckRightOnResourcesFailure() {
        when(umsClient.hasRights(anyString(), anyString(), anyList(), anyString(), any())).thenReturn(hasRightsResultMap());

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("You have no right to perform datalake/write on resources [" + RESOURCE_CRN + "," + RESOURCE_CRN2 + "]");

        underTest.checkRightOfUserOnResources(USER_CRN, AuthorizationResourceType.DATALAKE, AuthorizationResourceAction.WRITE,
                Lists.newArrayList(RESOURCE_CRN, RESOURCE_CRN2));
    }

    @Test
    public void testCheckRightOnResources() {
        Map<String, Boolean> resultMap = hasRightsResultMap();
        resultMap.put(RESOURCE_CRN2, Boolean.TRUE);
        when(umsClient.hasRights(anyString(), anyString(), anyList(), anyString(), any())).thenReturn(resultMap);

        underTest.checkRightOfUserOnResources(USER_CRN, AuthorizationResourceType.DATALAKE, AuthorizationResourceAction.WRITE,
                Lists.newArrayList(RESOURCE_CRN, RESOURCE_CRN2));
    }

    private Map<String, Boolean> hasRightsResultMap() {
        Map<String, Boolean> result = Maps.newHashMap();
        result.put(RESOURCE_CRN, Boolean.TRUE);
        result.put(RESOURCE_CRN2, Boolean.FALSE);
        return result;
    }
}
