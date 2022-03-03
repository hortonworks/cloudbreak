package com.sequenceiq.authorization.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.model.AllMatch;
import com.sequenceiq.authorization.service.model.AnyMatch;
import com.sequenceiq.authorization.service.model.AuthorizationRule;
import com.sequenceiq.authorization.service.model.HasRight;
import com.sequenceiq.authorization.service.model.HasRightOnAll;
import com.sequenceiq.authorization.service.model.HasRightOnAny;

@ExtendWith(MockitoExtension.class)
public class EnvironmentBasedAuthorizationProviderTest {

    private static final AuthorizationResourceAction ACTION = AuthorizationResourceAction.DESCRIBE_DATALAKE;

    private static final String RESOURCE_CRN_1 = "crn:cdp:datalake:us-west-1:tenant:datalake:resourceCrn1";

    private static final String RESOURCE_CRN_2 = "crn:cdp:datalake:us-west-1:tenant:datalake:resourceCrn2";

    private static final String RESOURCE_CRN_3 = "crn:cdp:datalake:us-west-1:tenant:datalake:resourceCrn3";

    private static final String RESOURCE_CRN_WITHOUT_ENV_1 = "crn:cdp:datalake:us-west-1:tenant:datalake:resourceCrnWithoutEnv1";

    private static final String RESOURCE_CRN_WITHOUT_ENV_2 = "crn:cdp:datalake:us-west-1:tenant:datalake:resourceCrnWithoutEnv2";

    private static final String ENV_CRN_1 = "crn:cdp:environments:us-west-1:tenant:environment:envCrn1";

    private static final String ENV_CRN_2 = "crn:cdp:environments:us-west-1:tenant:environment:envCrn2";

    @Mock
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @InjectMocks
    private EnvironmentBasedAuthorizationProvider underTest;

    @Mock
    private AuthorizationEnvironmentCrnProvider environmentCrnProvider;

    @Mock
    private AuthorizationEnvironmentCrnListProvider environmentCrnListProvider;

    @BeforeEach
    public void setup() throws IllegalAccessException {
        FieldUtils.writeField(underTest, "environmentCrnProviderMap",
                Map.of(ACTION.getAuthorizationResourceType(), environmentCrnProvider), true);
        FieldUtils.writeField(underTest, "environmentCrnListProviderMap",
                Map.of(ACTION.getAuthorizationResourceType(), environmentCrnListProvider), true);
    }

    @Test
    public void testWithEnvironment() {
        when(environmentCrnProvider.getEnvironmentCrnByResourceCrn(eq(RESOURCE_CRN_1))).thenReturn(Optional.of(ENV_CRN_1));

        Optional<AuthorizationRule> authorization = underTest.getAuthorizations(RESOURCE_CRN_1, ACTION);
        assertEquals(Optional.of(new HasRightOnAny(ACTION, List.of(ENV_CRN_1, RESOURCE_CRN_1))), authorization);
    }

    @Test
    public void testWithoutEnvironment() {
        when(environmentCrnProvider.getEnvironmentCrnByResourceCrn(eq(RESOURCE_CRN_1))).thenReturn(Optional.empty());

        Optional<AuthorizationRule> authorization = underTest.getAuthorizations(RESOURCE_CRN_1, ACTION);
        assertEquals(Optional.of(new HasRight(ACTION, RESOURCE_CRN_1)), authorization);
    }

    @Test
    public void testWithoutCrnProvider() throws IllegalAccessException {
        FieldUtils.writeField(underTest, "environmentCrnProviderMap", Map.of(), true);
        assertThrows(AccessDeniedException.class, () -> underTest.getAuthorizations(RESOURCE_CRN_1, ACTION),
                String.format("Action %s is not supported over resource %s, thus access is denied!", ACTION.getRight(), RESOURCE_CRN_1));
    }

    @Test
    public void testWithMultipleEnvironments() {
        Map<String, Optional<String>> withEnvs = new LinkedHashMap<>();
        withEnvs.put(RESOURCE_CRN_1, Optional.of(ENV_CRN_1));
        withEnvs.put(RESOURCE_CRN_2, Optional.of(ENV_CRN_1));
        withEnvs.put(RESOURCE_CRN_3, Optional.of(ENV_CRN_2));
        withEnvs.put(RESOURCE_CRN_WITHOUT_ENV_1, Optional.empty());
        withEnvs.put(RESOURCE_CRN_WITHOUT_ENV_2, Optional.empty());
        when(environmentCrnListProvider.getEnvironmentCrnsByResourceCrns(any())).thenReturn(withEnvs);

        Optional<AuthorizationRule> authorization = underTest.getAuthorizations(
                List.of(RESOURCE_CRN_1, RESOURCE_CRN_2, RESOURCE_CRN_3, RESOURCE_CRN_WITHOUT_ENV_1, RESOURCE_CRN_WITHOUT_ENV_2),
                ACTION);

        assertEquals(Optional.of(new AllMatch(List.of(
                new AnyMatch(List.of(new HasRight(ACTION, ENV_CRN_1), new HasRightOnAll(ACTION, List.of(RESOURCE_CRN_1, RESOURCE_CRN_2)))),
                new HasRightOnAny(ACTION, List.of(ENV_CRN_2, RESOURCE_CRN_3)),
                new HasRightOnAll(ACTION, List.of(RESOURCE_CRN_WITHOUT_ENV_1, RESOURCE_CRN_WITHOUT_ENV_2))
        ))), authorization);
    }

    @Test
    public void testWithNoEnvironments() {
        Map<String, Optional<String>> withEnvs = new LinkedHashMap<>();
        withEnvs.put(RESOURCE_CRN_WITHOUT_ENV_1, Optional.empty());
        withEnvs.put(RESOURCE_CRN_WITHOUT_ENV_2, Optional.empty());
        when(environmentCrnListProvider.getEnvironmentCrnsByResourceCrns(any())).thenReturn(withEnvs);

        Optional<AuthorizationRule> authorization = underTest.getAuthorizations(List.of(RESOURCE_CRN_WITHOUT_ENV_1, RESOURCE_CRN_WITHOUT_ENV_2), ACTION);

        assertEquals(Optional.of(new HasRightOnAll(ACTION, List.of(RESOURCE_CRN_WITHOUT_ENV_1, RESOURCE_CRN_WITHOUT_ENV_2))), authorization);
    }

    @Test
    public void testWithOneResourceNoEnvironment() {
        Map<String, Optional<String>> withEnvs = new LinkedHashMap<>();
        withEnvs.put(RESOURCE_CRN_WITHOUT_ENV_1, Optional.empty());
        when(environmentCrnListProvider.getEnvironmentCrnsByResourceCrns(any())).thenReturn(withEnvs);

        Optional<AuthorizationRule> authorization = underTest.getAuthorizations(List.of(RESOURCE_CRN_WITHOUT_ENV_1), ACTION);

        assertEquals(Optional.of(new HasRight(ACTION, RESOURCE_CRN_WITHOUT_ENV_1)), authorization);
    }

    @Test
    public void testWithOneEnvironmentMultipleResources() {
        Map<String, Optional<String>> withEnvs = new LinkedHashMap<>();
        withEnvs.put(RESOURCE_CRN_1, Optional.of(ENV_CRN_1));
        withEnvs.put(RESOURCE_CRN_2, Optional.of(ENV_CRN_1));
        when(environmentCrnListProvider.getEnvironmentCrnsByResourceCrns(any())).thenReturn(withEnvs);

        Optional<AuthorizationRule> authorization = underTest.getAuthorizations(List.of(RESOURCE_CRN_1, RESOURCE_CRN_2), ACTION);

        assertEquals(Optional.of(new AnyMatch(List.of(
                new HasRight(ACTION, ENV_CRN_1),
                new HasRightOnAll(ACTION, List.of(RESOURCE_CRN_1, RESOURCE_CRN_2))
        ))), authorization);
    }

    @Test
    public void testWithDefaultGetEnvironmentsImplementation() {
        when(environmentCrnListProvider.getEnvironmentCrnsByResourceCrns(any())).thenReturn(Map.of());

        List<String> resourceCrns = List.of(RESOURCE_CRN_1, RESOURCE_CRN_2);

        Optional<AuthorizationRule> authorization = underTest.getAuthorizations(resourceCrns, ACTION);

        assertEquals(Optional.of(new HasRightOnAll(ACTION, resourceCrns)), authorization);
    }

    @Test
    public void testWithDefaultGetEnvironmentsImplementationWithOneResource() {
        when(environmentCrnListProvider.getEnvironmentCrnsByResourceCrns(any())).thenReturn(Map.of());

        Optional<AuthorizationRule> authorization = underTest.getAuthorizations(List.of(RESOURCE_CRN_1), ACTION);

        assertEquals(Optional.of(new HasRight(ACTION, RESOURCE_CRN_1)), authorization);
    }
}