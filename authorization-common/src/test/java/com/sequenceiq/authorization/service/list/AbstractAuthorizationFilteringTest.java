package com.sequenceiq.authorization.service.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;

@ExtendWith(MockitoExtension.class)
public class AbstractAuthorizationFilteringTest {

    private static final String ACCOUNT_ID = "ACCOUNT_ID";

    private static final Crn USER_CRN = CrnTestUtil.getUserCrnBuilder()
            .setAccountId(ACCOUNT_ID)
            .setResource("RESOURCE_ID")
            .build();

    private static final String ENVIRONMENT_CRN = dataHubCrn("env-1");

    private static final String DATAHUB_CRN = environmentCrn("datahub-1");

    private static final AuthorizationResourceAction ACTION = AuthorizationResourceAction.DESCRIBE_DATAHUB;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private GrpcUmsClient grpcUmsClient;

    private ResourceFilteringService resourceFilteringService;

    @BeforeEach
    public void setUp() {
        resourceFilteringService = new ResourceFilteringService();
        ReflectionTestUtils.setField(resourceFilteringService, "umsClient", grpcUmsClient);
    }

    @Test
    public void whenNotEntitledThenReturnAllElememts() {
        disableListFiltering();

        String datahubCrn2 = dataHubCrn("datahub-2");
        String datahubCrn3 = dataHubCrn("datahub-3");
        LongFiltering underTest = longFiltering(
                new ResourceWithId(1L, DATAHUB_CRN, ENVIRONMENT_CRN),
                new ResourceWithId(2L, datahubCrn2, ENVIRONMENT_CRN),
                new ResourceWithId(3L, datahubCrn3, ENVIRONMENT_CRN));

        List<Long> result = underTest.filterResources(USER_CRN, ACTION, Map.of());

        assertEquals(List.of(1L, 2L, 3L), result);
        verifyNoInteractions(grpcUmsClient);
    }

    @Test
    public void testWithEmptyResourceList() {
        enableListFiltering();

        LongFiltering underTest = longFiltering();

        List<Long> result = underTest.filterResources(USER_CRN, ACTION, Map.of());

        assertEquals(List.of(), result);
        verifyNoInteractions(grpcUmsClient);
    }

    @Test
    public void testHasRight() {
        enableListFiltering();

        LongFiltering underTest = longFiltering(new ResourceWithId(1L, ENVIRONMENT_CRN));

        when(grpcUmsClient.hasRightsOnResources(
                eq(USER_CRN.toString()),
                eq(List.of(ENVIRONMENT_CRN)),
                eq(ACTION.getRight()))).thenReturn(List.of(true));

        List<Long> result = underTest.filterResources(USER_CRN, ACTION, Map.of());

        assertEquals(List.of(1L), result);
    }

    @Test
    public void testHasNoRight() {
        enableListFiltering();

        LongFiltering underTest = longFiltering(new ResourceWithId(1L, ENVIRONMENT_CRN));

        when(grpcUmsClient.hasRightsOnResources(
                eq(USER_CRN.toString()),
                eq(List.of(ENVIRONMENT_CRN)),
                eq(ACTION.getRight()))).thenReturn(List.of(false));

        List<Long> result = underTest.filterResources(USER_CRN, ACTION, Map.of());

        assertEquals(List.of(), result);
    }

    @Test
    public void testHasRightOnParent() {
        enableListFiltering();

        LongFiltering underTest = longFiltering(new ResourceWithId(1L, DATAHUB_CRN, ENVIRONMENT_CRN));

        when(grpcUmsClient.hasRightsOnResources(
                eq(USER_CRN.toString()),
                eq(List.of(ENVIRONMENT_CRN, DATAHUB_CRN)),
                eq(ACTION.getRight()))).thenReturn(List.of(true, false));

        List<Long> result = underTest.filterResources(USER_CRN, ACTION, Map.of());

        assertEquals(List.of(1L), result);
    }

    @Test
    public void testHasRightOnResourceWithParant() {
        enableListFiltering();

        LongFiltering underTest = longFiltering(new ResourceWithId(1L, DATAHUB_CRN, ENVIRONMENT_CRN));

        when(grpcUmsClient.hasRightsOnResources(
                eq(USER_CRN.toString()),
                eq(List.of(ENVIRONMENT_CRN, DATAHUB_CRN)),
                eq(ACTION.getRight()))).thenReturn(List.of(false, true));

        List<Long> result = underTest.filterResources(USER_CRN, ACTION, Map.of());

        assertEquals(List.of(1L), result);
    }

    @Test
    public void testHasNoRightOnResourceWithParent() {
        enableListFiltering();

        LongFiltering underTest = longFiltering(new ResourceWithId(1L, DATAHUB_CRN, ENVIRONMENT_CRN));

        when(grpcUmsClient.hasRightsOnResources(
                eq(USER_CRN.toString()),
                eq(List.of(ENVIRONMENT_CRN, DATAHUB_CRN)),
                eq(ACTION.getRight()))).thenReturn(List.of(false, false));

        List<Long> result = underTest.filterResources(USER_CRN, ACTION, Map.of());

        assertEquals(List.of(), result);
    }

    @Test
    public void testHasRightOnResourcesParent() {
        enableListFiltering();

        String datahubCrn2 = dataHubCrn("datahub-2");
        String datahubCrn3 = dataHubCrn("datahub-3");
        LongFiltering underTest = longFiltering(
                new ResourceWithId(1L, DATAHUB_CRN, ENVIRONMENT_CRN),
                new ResourceWithId(2L, datahubCrn2, ENVIRONMENT_CRN),
                new ResourceWithId(3L, datahubCrn3, ENVIRONMENT_CRN));

        when(grpcUmsClient.hasRightsOnResources(
                eq(USER_CRN.toString()),
                eq(List.of(ENVIRONMENT_CRN, DATAHUB_CRN, datahubCrn2, datahubCrn3)),
                eq(ACTION.getRight()))).thenReturn(List.of(true, false, false, false));

        List<Long> result = underTest.filterResources(USER_CRN, ACTION, Map.of());

        assertEquals(List.of(1L, 2L, 3L), result);
    }

    @Test
    public void testHasRightOnTwoResourceButNotOnItsParent() {
        enableListFiltering();

        String datahubCrn2 = dataHubCrn("datahub-2");
        String datahubCrn3 = dataHubCrn("datahub-3");
        LongFiltering underTest = longFiltering(
                new ResourceWithId(1L, DATAHUB_CRN, ENVIRONMENT_CRN),
                new ResourceWithId(2L, datahubCrn2, ENVIRONMENT_CRN),
                new ResourceWithId(3L, datahubCrn3, ENVIRONMENT_CRN));

        when(grpcUmsClient.hasRightsOnResources(
                eq(USER_CRN.toString()),
                eq(List.of(ENVIRONMENT_CRN, DATAHUB_CRN, datahubCrn2, datahubCrn3)),
                eq(ACTION.getRight()))).thenReturn(List.of(false, true, false, true));

        List<Long> result = underTest.filterResources(USER_CRN, ACTION, Map.of());

        assertEquals(List.of(1L, 3L), result);
    }

    @Test
    public void testHasNoRightOnAnyResources() {
        enableListFiltering();

        String datahubCrn2 = dataHubCrn("datahub-2");
        String datahubCrn3 = dataHubCrn("datahub-3");
        LongFiltering underTest = longFiltering(
                new ResourceWithId(1L, DATAHUB_CRN, ENVIRONMENT_CRN),
                new ResourceWithId(2L, datahubCrn2, ENVIRONMENT_CRN),
                new ResourceWithId(3L, datahubCrn3, ENVIRONMENT_CRN));

        when(grpcUmsClient.hasRightsOnResources(
                eq(USER_CRN.toString()),
                eq(List.of(ENVIRONMENT_CRN, DATAHUB_CRN, datahubCrn2, datahubCrn3)),
                eq(ACTION.getRight()))).thenReturn(List.of(false, false, false, false));

        List<Long> result = underTest.filterResources(USER_CRN, ACTION, Map.of());

        assertEquals(List.of(), result);
    }

    @Test
    public void testHasRightOnOneParentResource() {
        enableListFiltering();

        String environmentCrn2 = environmentCrn("env-2");
        String datahubCrn2 = dataHubCrn("datahub-2");
        String datahubCrn3 = dataHubCrn("datahub-3");
        LongFiltering underTest = longFiltering(
                new ResourceWithId(1L, DATAHUB_CRN, ENVIRONMENT_CRN),
                new ResourceWithId(2L, datahubCrn2, environmentCrn2),
                new ResourceWithId(3L, datahubCrn3, environmentCrn2));

        when(grpcUmsClient.hasRightsOnResources(
                eq(USER_CRN.toString()),
                eq(List.of(ENVIRONMENT_CRN, DATAHUB_CRN, environmentCrn2, datahubCrn2, datahubCrn3)),
                eq(ACTION.getRight()))).thenReturn(List.of(false, false, true, false, false));

        List<Long> result = underTest.filterResources(USER_CRN, ACTION, Map.of());

        assertEquals(List.of(2L, 3L), result);
    }

    @Test
    public void testHasRightOnAllParentResources() {
        enableListFiltering();

        String environmentCrn2 = environmentCrn("env-2");
        String datahubCrn2 = dataHubCrn("datahub-2");
        String datahubCrn3 = dataHubCrn("datahub-3");
        LongFiltering underTest = longFiltering(
                new ResourceWithId(1L, DATAHUB_CRN, ENVIRONMENT_CRN),
                new ResourceWithId(2L, datahubCrn2, environmentCrn2),
                new ResourceWithId(3L, datahubCrn3, environmentCrn2));

        when(grpcUmsClient.hasRightsOnResources(
                eq(USER_CRN.toString()),
                eq(List.of(ENVIRONMENT_CRN, DATAHUB_CRN, environmentCrn2, datahubCrn2, datahubCrn3)),
                eq(ACTION.getRight()))).thenReturn(List.of(true, false, true, false, false));

        List<Long> result = underTest.filterResources(USER_CRN, ACTION, Map.of());

        assertEquals(List.of(1L, 2L, 3L), result);
    }

    @Test
    public void testHasRightOnOneSubResource() {
        enableListFiltering();

        String environmentCrn2 = environmentCrn("env-2");
        String datahubCrn2 = dataHubCrn("datahub-2");
        String datahubCrn3 = dataHubCrn("datahub-3");
        LongFiltering underTest = longFiltering(
                new ResourceWithId(1L, DATAHUB_CRN, ENVIRONMENT_CRN),
                new ResourceWithId(2L, datahubCrn2, environmentCrn2),
                new ResourceWithId(3L, datahubCrn3, environmentCrn2));

        when(grpcUmsClient.hasRightsOnResources(
                eq(USER_CRN.toString()),
                eq(List.of(ENVIRONMENT_CRN, DATAHUB_CRN, environmentCrn2, datahubCrn2, datahubCrn3)),
                eq(ACTION.getRight()))).thenReturn(List.of(false, false, false, false, true));

        List<Long> result = underTest.filterResources(USER_CRN, ACTION, Map.of());

        assertEquals(List.of(3L), result);
    }

    @Test
    public void testHasRightOnAllSubResources() {
        enableListFiltering();

        String environmentCrn2 = environmentCrn("env-2");
        String datahubCrn2 = dataHubCrn("datahub-2");
        String datahubCrn3 = dataHubCrn("datahub-3");
        LongFiltering underTest = longFiltering(
                new ResourceWithId(1L, DATAHUB_CRN, ENVIRONMENT_CRN),
                new ResourceWithId(2L, datahubCrn2, environmentCrn2),
                new ResourceWithId(3L, datahubCrn3, environmentCrn2));

        when(grpcUmsClient.hasRightsOnResources(
                eq(USER_CRN.toString()),
                eq(List.of(ENVIRONMENT_CRN, DATAHUB_CRN, environmentCrn2, datahubCrn2, datahubCrn3)),
                eq(ACTION.getRight()))).thenReturn(List.of(false, true, false, true, true));

        List<Long> result = underTest.filterResources(USER_CRN, ACTION, Map.of());

        assertEquals(List.of(1L, 2L, 3L), result);
    }

    @Test
    public void testResourcesWithParentAndWithoutParent() {
        enableListFiltering();

        String datahubCrn2 = dataHubCrn("datahub-2");
        String datahubCrn3 = dataHubCrn("datahub-3");
        String datahubCrn4 = dataHubCrn("datahub-4");
        String datahubCrn5 = dataHubCrn("datahub-5");
        LongFiltering underTest = longFiltering(
                new ResourceWithId(1L, DATAHUB_CRN, ENVIRONMENT_CRN),
                new ResourceWithId(2L, datahubCrn2, ENVIRONMENT_CRN),
                new ResourceWithId(3L, datahubCrn3),
                new ResourceWithId(4L, datahubCrn4),
                new ResourceWithId(5L, datahubCrn5));

        when(grpcUmsClient.hasRightsOnResources(
                eq(USER_CRN.toString()),
                eq(List.of(ENVIRONMENT_CRN, DATAHUB_CRN, datahubCrn2, datahubCrn3, datahubCrn4, datahubCrn5)),
                eq(ACTION.getRight()))).thenReturn(List.of(true, false, false, true, false, true));

        List<Long> result = underTest.filterResources(USER_CRN, ACTION, Map.of());

        assertEquals(List.of(1L, 2L, 3L, 5L), result);
    }

    @Test
    public void testFilteringWithUnorderedAuthorizationResourceList() {
        enableListFiltering();

        String environmentCrn2 = environmentCrn("env-2");
        String datahubCrn2 = dataHubCrn("datahub-2");
        String datahubCrn3 = dataHubCrn("datahub-3");
        String datahubCrn4 = dataHubCrn("datahub-4");
        String datahubCrn5 = dataHubCrn("datahub-5");
        LongFiltering underTest = longFiltering(
                new ResourceWithId(1L, DATAHUB_CRN, ENVIRONMENT_CRN),
                new ResourceWithId(2L, datahubCrn2, environmentCrn2),
                new ResourceWithId(3L, datahubCrn3, ENVIRONMENT_CRN),
                new ResourceWithId(4L, datahubCrn4, environmentCrn2),
                new ResourceWithId(5L, datahubCrn5, ENVIRONMENT_CRN));

        when(grpcUmsClient.hasRightsOnResources(
                eq(USER_CRN.toString()),
                eq(List.of(ENVIRONMENT_CRN, DATAHUB_CRN, datahubCrn3, datahubCrn5, environmentCrn2, datahubCrn2, datahubCrn4)),
                eq(ACTION.getRight()))).thenReturn(List.of(true, false, false, false, false, true, false));

        List<Long> result = underTest.filterResources(USER_CRN, ACTION, Map.of());

        assertEquals(List.of(1L, 2L, 3L, 5L), result);
    }

    private void enableListFiltering() {
        when(entitlementService.listFilteringEnabled(any())).thenReturn(true);
    }

    private void disableListFiltering() {
        when(entitlementService.listFilteringEnabled(any())).thenReturn(false);
    }

    private static String dataHubCrn(String resourceId) {
        return CrnTestUtil.getDatahubCrnBuilder()
                .setAccountId(ACCOUNT_ID)
                .setResource(resourceId)
                .build()
                .toString();
    }

    private static String environmentCrn(String resourceId) {
        return CrnTestUtil.getEnvironmentCrnBuilder()
                .setAccountId(ACCOUNT_ID)
                .setResource(resourceId)
                .build()
                .toString();
    }

    private LongFiltering longFiltering(ResourceWithId... authorizationResources) {
        LongFiltering longFiltering = new LongFiltering(Arrays.asList(authorizationResources));
        ReflectionTestUtils.setField(longFiltering, "entitlementService", entitlementService);
        ReflectionTestUtils.setField(longFiltering, "resourceFilteringService", resourceFilteringService);
        return longFiltering;
    }

    private static class LongFiltering extends AbstractAuthorizationFiltering<List<Long>> {

        private List<ResourceWithId> authorizationResources;

        private List<Long> ids;

        LongFiltering(List<ResourceWithId> authorizationResources) {
            this.authorizationResources = authorizationResources;
            this.ids = authorizationResources
                    .stream()
                    .map(ResourceWithId::getId)
                    .collect(Collectors.toList());
        }

        @Override
        protected List<ResourceWithId> getAllResources(Map<String, Object> args) {
            return authorizationResources;
        }

        @Override
        protected List<Long> filterByIds(List<Long> authorizedResourceIds, Map<String, Object> args) {
            return authorizedResourceIds;
        }

        @Override
        protected List<Long> getAll(Map<String, Object> args) {
            return ids;
        }
    }

}