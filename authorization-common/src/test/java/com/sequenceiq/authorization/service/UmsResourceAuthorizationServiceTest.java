package com.sequenceiq.authorization.service;

import static com.sequenceiq.authorization.utils.AuthorizationMessageUtilsService.INSUFFICIENT_RIGHTS;
import static com.sequenceiq.authorization.utils.AuthorizationMessageUtilsService.INSUFFICIENT_RIGHTS_TEMPLATE;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.utils.AuthorizationMessageUtilsService;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.Crn;

@ExtendWith(MockitoExtension.class)
public class UmsResourceAuthorizationServiceTest {

    private static final String USER_ID = "userId";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:" + USER_ID;

    private static final String RESOURCE_CRN = "crn:cdp:datalake:us-west-1:1234:environment:1";

    private static final String RESOURCE_CRN2 = "crn:cdp:datalake:us-west-1:1234:environment:2";

    @Mock
    private GrpcUmsClient umsClient;

    @Mock
    private UmsRightProvider umsRightProvider;

    @InjectMocks
    private UmsResourceAuthorizationService underTest;

    private AuthorizationMessageUtilsService authorizationMessageUtilsService;

    @Mock
    private ResourceNameFactoryService resourceNameFactoryService;

    @BeforeEach
    public void init() throws IllegalAccessException {
        when(resourceNameFactoryService.getNames(any())).thenReturn(Collections.EMPTY_MAP);
        this.authorizationMessageUtilsService = spy(new AuthorizationMessageUtilsService(resourceNameFactoryService));
        FieldUtils.writeField(underTest, "authorizationMessageUtilsService", authorizationMessageUtilsService, true);
        when(umsRightProvider.getRight(any())).thenAnswer(invocation -> {
            AuthorizationResourceAction action = invocation.getArgument(0);
            return action.getRight();
        });
    }

    @Test
    public void testCheckRightOnResource() {
        when(umsClient.checkResourceRight(anyString(), anyString(), anyString(), any())).thenReturn(false);

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.
                checkRightOfUserOnResource(USER_CRN, AuthorizationResourceAction.DESCRIBE_ENVIRONMENT, RESOURCE_CRN)));
        assertTrue(exception.getMessage().contains(INSUFFICIENT_RIGHTS));
        assertTrue(exception.getMessage().contains(formatTemplate("environments/describeEnvironment", RESOURCE_CRN)));
    }

    @Test
    public void testCheckRightOnResourcesFailure() {
        when(umsClient.hasRights(anyString(), anyList(), anyString(), any())).thenReturn(hasRightsResultMap());

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.checkRightOfUserOnResources(USER_CRN,
                        AuthorizationResourceAction.DESCRIBE_ENVIRONMENT,
                        Lists.newArrayList(RESOURCE_CRN, RESOURCE_CRN2))));
        assertTrue(exception.getMessage().contains(INSUFFICIENT_RIGHTS));
        assertTrue(exception.getMessage().contains(formatTemplate("environments/describeEnvironment", RESOURCE_CRN)));
        assertTrue(exception.getMessage().contains(formatTemplate("environments/describeEnvironment", RESOURCE_CRN2)));


    }

    @Test
    public void testCheckRightOnResources() {
        Map<String, Boolean> resultMap = hasRightsResultMap();
        resultMap.put(RESOURCE_CRN2, TRUE);
        when(umsClient.hasRights(anyString(), anyList(), anyString(), any())).thenReturn(resultMap);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.
                checkRightOfUserOnResources(USER_CRN, AuthorizationResourceAction.DESCRIBE_ENVIRONMENT, Lists.newArrayList(RESOURCE_CRN, RESOURCE_CRN2)));
    }

    private Map<String, Boolean> hasRightsResultMap() {
        Map<String, Boolean> result = Maps.newHashMap();
        result.put(RESOURCE_CRN, TRUE);
        result.put(RESOURCE_CRN2, FALSE);
        return result;
    }

    private String formatTemplate(String right, String resourceCrn) {
        return String.format(INSUFFICIENT_RIGHTS_TEMPLATE, right, Crn.fromString(resourceCrn).getResourceType().getName(), "Crn: '" + resourceCrn + "'");
    }
}
