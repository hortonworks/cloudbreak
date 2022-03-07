package com.sequenceiq.authorization.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;

@RunWith(MockitoJUnitRunner.class)
public class ResourceNameAuthorizationFactoryTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    private static final String RESOURCE_CRN = "resourceCrn";

    @Mock
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Mock
    private ResourceCrnAthorizationFactory resourceCrnAthorizationFactory;

    @InjectMocks
    private ResourceNameAuthorizationFactory underTest;

    @Mock
    private AuthorizationResourceCrnProvider resourceBasedCrnProvider;

    @Before
    public void setUp() throws IllegalAccessException {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn("resource");
        when(resourceBasedCrnProvider.getResourceCrnByResourceName(any())).thenReturn(RESOURCE_CRN);
        FieldUtils.writeField(underTest, "resourceCrnProviderMap",
                Map.of(AuthorizationResourceType.CREDENTIAL, resourceBasedCrnProvider), true);
    }

    @Test
    public void testAuthorizationWhenEnvCrnNotPresent() {
        underTest.getAuthorization(getAnnotation(), USER_CRN, null, null);

        verify(commonPermissionCheckingUtils).getParameter(any(), any(), eq(ResourceName.class), eq(String.class));
        verify(resourceCrnAthorizationFactory).calcAuthorization(RESOURCE_CRN, AuthorizationResourceAction.EDIT_CREDENTIAL);
    }

    private CheckPermissionByResourceName getAnnotation() {
        return new CheckPermissionByResourceName() {

            @Override
            public AuthorizationResourceAction action() {
                return AuthorizationResourceAction.EDIT_CREDENTIAL;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return CheckPermissionByResourceName.class;
            }
        };
    }
}
