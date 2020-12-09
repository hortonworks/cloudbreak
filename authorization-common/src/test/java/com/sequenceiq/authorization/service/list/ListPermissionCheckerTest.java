package com.sequenceiq.authorization.service.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Maps;
import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.resource.AuthorizationFilterableResponseCollection;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.ResourceCrnAwareApiModel;
import com.sequenceiq.authorization.service.CommonPermissionCheckingUtils;
import com.sequenceiq.authorization.service.ResourceBasedCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@ExtendWith(MockitoExtension.class)
public class ListPermissionCheckerTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String RESOURCE_CRN1 = "crn:cdp:datalake:us-west-1:1234:resource:1";

    private static final String RESOURCE_CRN2 = "crn:cdp:datalake:us-west-1:1234:resource:2";

    private static final String RESOURCE_CRN3 = "crn:cdp:datalake:us-west-1:1234:resource:3";

    @Mock
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private ListPermissionChecker underTest;

    @Mock
    private ResourceBasedCrnProvider resourceBasedCrnProvider;

    @BeforeEach
    public void setup() {
        when(resourceBasedCrnProvider.getResourceCrnsInAccount()).thenReturn(List.of(RESOURCE_CRN1, RESOURCE_CRN2, RESOURCE_CRN3));
        Map<String, Boolean> rightCheckResult = Maps.newHashMap();
        rightCheckResult.put(RESOURCE_CRN1, Boolean.TRUE);
        rightCheckResult.put(RESOURCE_CRN2, Boolean.TRUE);
        rightCheckResult.put(RESOURCE_CRN3, Boolean.FALSE);
        when(commonPermissionCheckingUtils.getPermissionsForUserOnResources(any(), any(), anyList())).thenReturn(rightCheckResult);
        when(commonPermissionCheckingUtils.getResourceBasedCrnProvider(any())).thenReturn(resourceBasedCrnProvider);
        lenient().when(entitlementService.listFilteringEnabled(anyString())).thenReturn(true);
    }

    @Test
    public void testListFiltering() {
        when(commonPermissionCheckingUtils.proceed(any(), any(), anyLong())).thenReturn(
                List.of((ResourceCrnAwareApiModel) () -> RESOURCE_CRN1, () -> RESOURCE_CRN2, () -> RESOURCE_CRN3));

        Object result = underTest.checkPermissions(getAnnotation(), USER_CRN, null, null, 0L);

        assertTrue(result instanceof List);
        assertEquals(2, ((List) result).size());
        assertTrue(((List) result).stream().allMatch(item -> item instanceof ResourceCrnAwareApiModel));
        List<String> filteredResourceCrns = ((List<ResourceCrnAwareApiModel>) result).stream()
                .map(ResourceCrnAwareApiModel::getResourceCrn)
                .collect(Collectors.toList());
        assertTrue(filteredResourceCrns.contains(RESOURCE_CRN1));
        assertFalse(filteredResourceCrns.contains(RESOURCE_CRN3));
    }

    @Test
    public void testIncompatibleListFiltering() {
        when(commonPermissionCheckingUtils.proceed(any(), any(), anyLong())).thenReturn(List.of(RESOURCE_CRN1, RESOURCE_CRN2, RESOURCE_CRN3));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> underTest.checkPermissions(getAnnotation(), USER_CRN, null, null, 0L));
        assertEquals("Items of your response list or set should implement ResourceCrnAwareApiModel interface", exception.getMessage());
    }

    @Test
    public void testSetFiltering() {
        when(commonPermissionCheckingUtils.proceed(any(), any(), anyLong())).thenReturn(
                Set.of((ResourceCrnAwareApiModel) () -> RESOURCE_CRN1, () -> RESOURCE_CRN2, () -> RESOURCE_CRN3));

        Object result = underTest.checkPermissions(getAnnotation(), USER_CRN, null, null, 0L);

        assertTrue(result instanceof Set);
        assertEquals(2, ((Set) result).size());
        assertTrue(((Set) result).stream().allMatch(item -> item instanceof ResourceCrnAwareApiModel));
        Set<String> filteredResourceCrns = ((Set<ResourceCrnAwareApiModel>) result).stream()
                .map(ResourceCrnAwareApiModel::getResourceCrn)
                .collect(Collectors.toSet());
        assertTrue(filteredResourceCrns.contains(RESOURCE_CRN1));
        assertFalse(filteredResourceCrns.contains(RESOURCE_CRN3));
    }

    @Test
    public void testIncompatibleSetFiltering() {
        when(commonPermissionCheckingUtils.proceed(any(), any(), anyLong())).thenReturn(Set.of(RESOURCE_CRN1, RESOURCE_CRN2, RESOURCE_CRN3));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> underTest.checkPermissions(getAnnotation(), USER_CRN, null, null, 0L));
        assertEquals("Items of your response list or set should implement ResourceCrnAwareApiModel interface", exception.getMessage());
    }

    @Test
    public void testAuthorizationFilterableCollectionFiltering() {
        when(commonPermissionCheckingUtils.proceed(any(), any(), anyLong())).thenReturn(getAuthorizationFilterableResponse());

        Object result = underTest.checkPermissions(getAnnotation(), USER_CRN, null, null, 0L);

        assertTrue(result instanceof AuthorizationFilterableResponseCollection);
        Collection<ResourceCrnAwareApiModel> responses = ((AuthorizationFilterableResponseCollection) result).getResponses();
        assertEquals(2, responses.size());
        Set<String> filteredResourceCrns = responses.stream()
                .map(ResourceCrnAwareApiModel::getResourceCrn)
                .collect(Collectors.toSet());
        assertTrue(filteredResourceCrns.contains(RESOURCE_CRN1));
        assertFalse(filteredResourceCrns.contains(RESOURCE_CRN3));
    }

    @Test
    public void testIncompatibleTypeFiltering() {
        when(commonPermissionCheckingUtils.proceed(any(), any(), anyLong())).thenReturn("");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> underTest.checkPermissions(getAnnotation(), USER_CRN, null, null, 0L));
        assertEquals("Response of list API should be List, Set or an instance of AuthorizationFilterableResponseCollection interface",
                exception.getMessage());
    }

    private AuthorizationFilterableResponseCollection<ResourceCrnAwareApiModel> getAuthorizationFilterableResponse() {
        return new AuthorizationFilterableResponseCollection<>() {

            private Set<ResourceCrnAwareApiModel> result = Set.of(() -> RESOURCE_CRN1, () -> RESOURCE_CRN2, () -> RESOURCE_CRN3);

            @Override
            public Collection<ResourceCrnAwareApiModel> getResponses() {
                return result;
            }

            @Override
            public void setResponses(Collection<ResourceCrnAwareApiModel> filtered) {
                result = new HashSet<>(filtered);
            }
        };
    }

    private FilterListBasedOnPermissions getAnnotation() {
        return new FilterListBasedOnPermissions() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return FilterListBasedOnPermissions.class;
            }

            @Override
            public AuthorizationResourceAction action() {
                return AuthorizationResourceAction.DESCRIBE_CREDENTIAL;
            }
        };
    }
}
