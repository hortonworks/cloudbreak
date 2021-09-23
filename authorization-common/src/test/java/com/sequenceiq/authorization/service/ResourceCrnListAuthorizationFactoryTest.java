package com.sequenceiq.authorization.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrnList;
import com.sequenceiq.authorization.annotation.ResourceCrnList;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.model.AuthorizationRule;
import com.sequenceiq.authorization.service.model.HasRightOnAll;
import com.sequenceiq.authorization.utils.CrnAccountValidator;

@RunWith(MockitoJUnitRunner.class)
public class ResourceCrnListAuthorizationFactoryTest {

    private static final AuthorizationResourceAction ACTION = AuthorizationResourceAction.EDIT_CREDENTIAL;

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    private static final List<String> RESOURCE_CRNS = List.of("resourceCrn1", "resourceCrn2");

    @Mock
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Mock
    private DefaultResourceAuthorizationProvider defaultResourceAuthorizationProvider;

    @Mock
    private EnvironmentBasedAuthorizationProvider environmentBasedAuthorizationProvider;

    @Mock
    private CrnAccountValidator crnAccountValidator;

    @InjectMocks
    private ResourceCrnListAuthorizationFactory underTest;

    @Test
    public void testAuthorization() {
        when(defaultResourceAuthorizationProvider.authorizeDefaultOrElseCompute(eq(RESOURCE_CRNS), eq(ACTION), any()))
                .then(i -> ((Function) i.getArgument(2)).apply(RESOURCE_CRNS));
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(RESOURCE_CRNS);
        Optional<AuthorizationRule> expected = Optional.of(new HasRightOnAll(ACTION, RESOURCE_CRNS));
        when(environmentBasedAuthorizationProvider.getAuthorizations(anyCollection(), any())).thenReturn(expected);

        Optional<AuthorizationRule> authorization = underTest.getAuthorization(getAnnotation(), USER_CRN, null, null);

        verify(commonPermissionCheckingUtils).getParameter(any(), any(), eq(ResourceCrnList.class), eq(Collection.class));
        assertEquals(expected, authorization);
    }

    @Test
    public void testAuthorizationWithEmptyList() {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(List.of());

        Optional<AuthorizationRule> authorization = underTest.getAuthorization(getAnnotation(), USER_CRN, null, null);

        verify(commonPermissionCheckingUtils).getParameter(any(), any(), eq(ResourceCrnList.class), eq(Collection.class));
        assertEquals(Optional.empty(), authorization);
    }

    private CheckPermissionByResourceCrnList getAnnotation() {
        return new CheckPermissionByResourceCrnList() {

            @Override
            public AuthorizationResourceAction action() {
                return ACTION;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return CheckPermissionByResourceCrnList.class;
            }
        };
    }
}
