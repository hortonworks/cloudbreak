package com.sequenceiq.authorization.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.resource.AuthorizationFilterableResponseCollection;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.ResourceCrnAwareApiModel;

@RunWith(MockitoJUnitRunner.class)
public class ListPermissionCheckerTest {

    private static final String RESOURCE_CRN1 = "crn:cdp:datalake:us-west-1:1234:resource:1";

    private static final String RESOURCE_CRN2 = "crn:cdp:datalake:us-west-1:1234:resource:2";

    private static final String RESOURCE_CRN3 = "crn:cdp:datalake:us-west-1:1234:resource:3";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Mock
    private ResourceBasedCrnProvider resourceBasedCrnProvider;

    @Spy
    private List<ResourceBasedCrnProvider> resourceBasedCrnProviders = new ArrayList<>();

    @InjectMocks
    private ListPermissionChecker underTest;

    @Before
    public void setup() {
        resourceBasedCrnProviders.add(resourceBasedCrnProvider);
        when(resourceBasedCrnProvider.getResourceType()).thenReturn(AuthorizationResourceType.CREDENTIAL);
        when(resourceBasedCrnProvider.getResourceCrnsInAccount()).thenReturn(
                Lists.newArrayList(RESOURCE_CRN1, RESOURCE_CRN2, RESOURCE_CRN3));
        Map<String, Boolean> rightCheckResult = Maps.newHashMap();
        rightCheckResult.put(RESOURCE_CRN1, Boolean.TRUE);
        rightCheckResult.put(RESOURCE_CRN2, Boolean.TRUE);
        rightCheckResult.put(RESOURCE_CRN3, Boolean.FALSE);
        when(commonPermissionCheckingUtils.getPermissionsForUserOnResources(any(), any(), any(), anyList())).thenReturn(rightCheckResult);
        underTest.populateResourceBasedCrnProviderMap();
    }

    @Test
    public void testListFiltering() {
        when(commonPermissionCheckingUtils.proceed(any(), any(), anyLong())).thenReturn(
                Lists.newArrayList((ResourceCrnAwareApiModel) () -> RESOURCE_CRN1, () -> RESOURCE_CRN2, () -> RESOURCE_CRN3));

        Object result = underTest.checkPermissions(getAnnotation(), AuthorizationResourceType.CREDENTIAL, null, null, null, 0L);

        assertTrue(result instanceof List);
        assertEquals(2, ((List) result).size());
        assertTrue(((List) result).stream().allMatch(item -> item instanceof ResourceCrnAwareApiModel));
        List<String> filteredResourceCrns = ((List<ResourceCrnAwareApiModel>) result).stream()
                .map(resourceCrnAwareApiModel -> resourceCrnAwareApiModel.getResourceCrn())
                .collect(Collectors.toList());
        assertTrue(filteredResourceCrns.contains(RESOURCE_CRN1));
        assertFalse(filteredResourceCrns.contains(RESOURCE_CRN3));
    }

    @Test
    public void testIncompatibleListFiltering() {
        when(commonPermissionCheckingUtils.proceed(any(), any(), anyLong())).thenReturn(
                Lists.newArrayList(RESOURCE_CRN1, RESOURCE_CRN2, RESOURCE_CRN3));

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Items of your response list or set should implement ResourceCrnAwareApiModel interface");

        underTest.checkPermissions(getAnnotation(), AuthorizationResourceType.CREDENTIAL, null, null, null, 0L);
    }

    @Test
    public void testSetFiltering() {
        when(commonPermissionCheckingUtils.proceed(any(), any(), anyLong())).thenReturn(
                Sets.newHashSet((ResourceCrnAwareApiModel) () -> RESOURCE_CRN1, () -> RESOURCE_CRN2, () -> RESOURCE_CRN3));

        Object result = underTest.checkPermissions(getAnnotation(), AuthorizationResourceType.CREDENTIAL, null, null, null, 0L);

        assertTrue(result instanceof Set);
        assertEquals(2, ((Set) result).size());
        assertTrue(((Set) result).stream().allMatch(item -> item instanceof ResourceCrnAwareApiModel));
        Set<String> filteredResourceCrns = ((Set<ResourceCrnAwareApiModel>) result).stream()
                .map(resourceCrnAwareApiModel -> resourceCrnAwareApiModel.getResourceCrn())
                .collect(Collectors.toSet());
        assertTrue(filteredResourceCrns.contains(RESOURCE_CRN1));
        assertFalse(filteredResourceCrns.contains(RESOURCE_CRN3));
    }

    @Test
    public void testIncompatibleSetFiltering() {
        when(commonPermissionCheckingUtils.proceed(any(), any(), anyLong())).thenReturn(
                Sets.newHashSet(RESOURCE_CRN1, RESOURCE_CRN2, RESOURCE_CRN3));

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Items of your response list or set should implement ResourceCrnAwareApiModel interface");

        underTest.checkPermissions(getAnnotation(), AuthorizationResourceType.CREDENTIAL, null, null, null, 0L);
    }

    @Test
    public void testAuthorizationFilterableCollectionFiltering() {
        when(commonPermissionCheckingUtils.proceed(any(), any(), anyLong())).thenReturn(getAuthorizationFilterableResponse());

        Object result = underTest.checkPermissions(getAnnotation(), AuthorizationResourceType.CREDENTIAL, null, null, null, 0L);

        assertTrue(result instanceof AuthorizationFilterableResponseCollection);
        Collection<ResourceCrnAwareApiModel> responses = ((AuthorizationFilterableResponseCollection) result).getResponses();
        assertEquals(2, responses.size());
        Set<String> filteredResourceCrns = responses.stream()
                .map(resourceCrnAwareApiModel -> resourceCrnAwareApiModel.getResourceCrn())
                .collect(Collectors.toSet());
        assertTrue(filteredResourceCrns.contains(RESOURCE_CRN1));
        assertFalse(filteredResourceCrns.contains(RESOURCE_CRN3));
    }

    @Test
    public void testIncompatibleTypeFiltering() {
        when(commonPermissionCheckingUtils.proceed(any(), any(), anyLong())).thenReturn("");

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Response of list API should be List, Set or an instance of AuthorizationFilterableResponseCollection interface");

        underTest.checkPermissions(getAnnotation(), AuthorizationResourceType.CREDENTIAL, null, null, null, 0L);
    }

    private AuthorizationFilterableResponseCollection<ResourceCrnAwareApiModel> getAuthorizationFilterableResponse() {
        return new AuthorizationFilterableResponseCollection<>() {

            private Set<ResourceCrnAwareApiModel> result = Sets.newHashSet(() -> RESOURCE_CRN1, () -> RESOURCE_CRN2, () -> RESOURCE_CRN3);

            @Override
            public Collection<ResourceCrnAwareApiModel> getResponses() {
                return result;
            }

            @Override
            public void setResponses(Collection<ResourceCrnAwareApiModel> filtered) {
                result.clear();
                result.addAll(filtered);
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
