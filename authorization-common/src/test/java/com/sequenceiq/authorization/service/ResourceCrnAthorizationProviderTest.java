package com.sequenceiq.authorization.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.model.AuthorizationRule;
import com.sequenceiq.authorization.service.model.HasRight;
import com.sequenceiq.authorization.service.model.HasRightOnAny;
import com.sequenceiq.authorization.utils.CrnAccountValidator;

@RunWith(MockitoJUnitRunner.class)
public class ResourceCrnAthorizationProviderTest {

    private static final AuthorizationResourceAction ACTION = AuthorizationResourceAction.DELETE_DATAHUB;

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    private static final String RESOURCE_CRN = "crn:cdp:datalake:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:datahub:614a791a-a100-4f83-8c65-968fe9b06d47";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:environment:6b2b1600-8ac6-4c26-aa34-dab36f4bd243";

    @Mock
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Mock
    private EnvironmentBasedAuthorizationProvider environmentBasedAuthorizationProvider;

    @Mock
    private DefaultResourceAuthorizationProvider defaultResourceAuthorizationProvider;

    @Mock
    private CrnAccountValidator crnAccountValidator;

    @InjectMocks
    private ResourceCrnAthorizationFactory underTest;

    @Before
    public void setUp() {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), eq(ResourceCrn.class), eq(String.class)))
                .thenReturn(RESOURCE_CRN);
        when(defaultResourceAuthorizationProvider.authorizeDefaultOrElseCompute(eq(RESOURCE_CRN), eq(ACTION), any()))
                .then(i -> ((Supplier) i.getArgument(2)).get());
    }

    @Test
    public void testAuthorizationWhenEnvCrnIsPresent() {
        Optional<AuthorizationRule> expected = Optional.of(new HasRightOnAny(ACTION, List.of(ENV_CRN, RESOURCE_CRN)));
        when(environmentBasedAuthorizationProvider.getAuthorizations(RESOURCE_CRN, ACTION)).thenReturn(expected);

        Optional<AuthorizationRule> authorization = underTest.getAuthorization(getAnnotation(), USER_CRN, null, null);

        assertEquals(expected, authorization);
    }

    @Test
    public void testAuthorizationWhenEnvCrnNotPresent() {
        Optional<AuthorizationRule> expected = Optional.of(new HasRight(ACTION, RESOURCE_CRN));
        when(environmentBasedAuthorizationProvider.getAuthorizations(RESOURCE_CRN, ACTION)).thenReturn(expected);

        Optional<AuthorizationRule> authorization = underTest.getAuthorization(getAnnotation(), USER_CRN, null, null);

        assertEquals(expected, authorization);
    }

    @Test
    public void testLegacyAuthorization() {
        when(commonPermissionCheckingUtils.legacyAuthorizationNeeded()).thenReturn(Boolean.TRUE);

        Optional<AuthorizationRule> authorization = underTest.getAuthorization(getAnnotation(), USER_CRN, null, null);

        assertEquals(Optional.of(new HasRight(ACTION, RESOURCE_CRN)), authorization);
    }

    private CheckPermissionByResourceCrn getAnnotation() {
        return new CheckPermissionByResourceCrn() {

            @Override
            public AuthorizationResourceAction action() {
                return ACTION;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return CheckPermissionByResourceCrn.class;
            }
        };
    }
}
