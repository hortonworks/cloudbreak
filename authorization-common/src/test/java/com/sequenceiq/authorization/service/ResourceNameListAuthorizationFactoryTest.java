package com.sequenceiq.authorization.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceNameList;
import com.sequenceiq.authorization.annotation.ResourceNameList;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.model.AuthorizationRule;
import com.sequenceiq.authorization.service.model.HasRightOnAll;

@RunWith(MockitoJUnitRunner.class)
public class ResourceNameListAuthorizationFactoryTest {

    private static final AuthorizationResourceAction ACTION = AuthorizationResourceAction.EDIT_CREDENTIAL;

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    private static final List<String> RESOURCES = List.of("resource1", "resource2");

    private static final List<String> RESOURCE_CRNS = List.of("resourceCrn1", "resourceCrn2");

    @Mock
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Mock
    private ResourceCrnListAuthorizationFactory resourceCrnListAuthorizationFactory;

    @InjectMocks
    private ResourceNameListAuthorizationFactory underTest;

    @Mock
    private ResourcePropertyProvider resourceBasedCrnProvider;

    @Test
    public void testAuthorrization() {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(RESOURCES);
        when(commonPermissionCheckingUtils.getResourceBasedCrnProvider(any())).thenReturn(resourceBasedCrnProvider);
        when(resourceBasedCrnProvider.getResourceCrnListByResourceNameList(anyList())).thenReturn(RESOURCE_CRNS);
        Optional<AuthorizationRule> expected = Optional.of(new HasRightOnAll(ACTION, RESOURCE_CRNS));
        when(resourceCrnListAuthorizationFactory.calcAuthorization(anyCollection(), any()))
                .thenReturn(expected);

        Optional<AuthorizationRule> authorization = underTest.getAuthorization(getAnnotation(), USER_CRN, null, null);

        verify(commonPermissionCheckingUtils).getParameter(any(), any(), eq(ResourceNameList.class), eq(Collection.class));
        verify(resourceCrnListAuthorizationFactory).calcAuthorization(RESOURCE_CRNS, ACTION);
        assertEquals(expected, authorization);
    }

    @Test
    public void testAuthorrizationWithEmptyList() {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(List.of());

        Optional<AuthorizationRule> authorization = underTest.getAuthorization(getAnnotation(), USER_CRN, null, null);

        verify(commonPermissionCheckingUtils).getParameter(any(), any(), eq(ResourceNameList.class), eq(Collection.class));
        assertEquals(Optional.empty(), authorization);
    }

    private CheckPermissionByResourceNameList getAnnotation() {
        return new CheckPermissionByResourceNameList() {

            @Override
            public AuthorizationResourceAction action() {
                return ACTION;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return CheckPermissionByResourceNameList.class;
            }
        };
    }
}
