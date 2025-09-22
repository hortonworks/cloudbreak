package com.sequenceiq.authorization.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.ws.rs.ForbiddenException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.defaults.CrnsByCategory;
import com.sequenceiq.authorization.service.defaults.DefaultResourceChecker;
import com.sequenceiq.authorization.service.model.AuthorizationRule;

@ExtendWith(MockitoExtension.class)
public class DefaultResourceAuthorizationProviderTest {

    private static final AuthorizationResourceType RESOURCE_TYPE = AuthorizationResourceType.IMAGE_CATALOG;

    private static final String RESOURCE_CRN_1 = "resourceCrn1";

    private static final String RESOURCE_CRN_2 = "resourceCrn2";

    private static final String RESOURCE_CRN_3 = "resourceCrn3";

    private static final String RESOURCE_CRN_4 = "resourceCrn4";

    private static final AuthorizationResourceAction ACTION = AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG;

    @Mock
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Spy
    private Map<AuthorizationResourceType, DefaultResourceChecker> defaultResourceCheckerMap = new EnumMap<>(AuthorizationResourceType.class);

    @InjectMocks
    private DefaultResourceAuthorizationProvider underTest;

    @Mock
    private DefaultResourceChecker defaultResourceChecker;

    @Mock
    private Supplier<Optional<AuthorizationRule>> externalSupplier;

    @Mock
    private Function<Collection<String>, Optional<AuthorizationRule>> externalFunction;

    @BeforeEach
    public void setUp() {
        defaultResourceCheckerMap.put(RESOURCE_TYPE, defaultResourceChecker);
    }

    @Test
    public void testNotDefault() {
        when(defaultResourceChecker.isDefault(eq(RESOURCE_CRN_1))).thenReturn(false);
        Optional<AuthorizationRule> expected = Optional.of(mock(AuthorizationRule.class));
        when(externalSupplier.get()).thenReturn(expected);

        Optional<AuthorizationRule> authorization = underTest.authorizeDefaultOrElseCompute(RESOURCE_CRN_1, ACTION, externalSupplier);

        verify(externalSupplier).get();
        assertEquals(expected, authorization);
    }

    @Test
    public void testDefaultNotAllowed() {
        when(defaultResourceChecker.isDefault(eq(RESOURCE_CRN_1))).thenReturn(true);
        doThrow(new ForbiddenException("Bad")).when(commonPermissionCheckingUtils)
                .throwAccessDeniedIfActionNotAllowed(any(), anyCollection(), eq(Optional.of(defaultResourceChecker)));

        ForbiddenException accessDeniedException =
                assertThrows(ForbiddenException.class, () -> underTest.authorizeDefaultOrElseCompute(RESOURCE_CRN_1, ACTION, externalSupplier));

        verifyNoInteractions(externalSupplier);
        assertEquals("Bad", accessDeniedException.getMessage());
    }

    @Test
    public void testDefaultAllowed() {
        when(defaultResourceChecker.isDefault(eq(RESOURCE_CRN_1))).thenReturn(true);
        doNothing().when(commonPermissionCheckingUtils)
                .throwAccessDeniedIfActionNotAllowed(any(), anyCollection(), eq(Optional.of(defaultResourceChecker)));

        Optional<AuthorizationRule> authorization = underTest.authorizeDefaultOrElseCompute(RESOURCE_CRN_1, ACTION, externalSupplier);

        assertEquals(Optional.empty(), authorization);
    }

    @Test
    public void testNoDefaultChecker() {
        Optional<AuthorizationRule> expected = Optional.of(mock(AuthorizationRule.class));
        when(externalSupplier.get()).thenReturn(expected);

        Optional<AuthorizationRule> authorization = underTest.authorizeDefaultOrElseCompute(RESOURCE_CRN_1, AuthorizationResourceAction.EDIT_CREDENTIAL,
                externalSupplier);

        verify(externalSupplier).get();
        assertEquals(expected, authorization);
    }

    @Test
    public void testMultipleWithDefaultsAndNotDefaults() {
        Optional<AuthorizationRule> expected = Optional.of(mock(AuthorizationRule.class));
        List<String> resourceCrns = List.of(RESOURCE_CRN_1, RESOURCE_CRN_2, RESOURCE_CRN_3, RESOURCE_CRN_4);
        List<String> defaultResourceCrns = List.of(RESOURCE_CRN_1, RESOURCE_CRN_3);
        List<String> notDefaultResourceCrns = List.of(RESOURCE_CRN_2, RESOURCE_CRN_4);
        when(defaultResourceChecker.getDefaultResourceCrns(resourceCrns))
                .thenReturn(CrnsByCategory.newBuilder()
                        .defaultResourceCrns(defaultResourceCrns)
                        .notDefaultResourceCrns(notDefaultResourceCrns)
                        .build());
        when(externalFunction.apply(eq(notDefaultResourceCrns))).thenReturn(expected);

        Optional<AuthorizationRule> authorization = underTest.authorizeDefaultOrElseCompute(resourceCrns, ACTION, externalFunction);

        assertEquals(expected, authorization);
        verify(commonPermissionCheckingUtils).throwAccessDeniedIfActionNotAllowed(ACTION, defaultResourceCrns, Optional.of(defaultResourceChecker));
        verify(externalFunction).apply(notDefaultResourceCrns);
    }

    @Test
    public void testWhenDefaultsFail() {
        List<String> resourceCrns = List.of(RESOURCE_CRN_1, RESOURCE_CRN_2);
        when(defaultResourceChecker.getDefaultResourceCrns(resourceCrns))
                .thenReturn(CrnsByCategory.newBuilder()
                        .defaultResourceCrns(List.of(RESOURCE_CRN_1))
                        .notDefaultResourceCrns(List.of(RESOURCE_CRN_2))
                        .build());
        doThrow(new ForbiddenException("Bad")).when(commonPermissionCheckingUtils)
                .throwAccessDeniedIfActionNotAllowed(any(), anyCollection(), eq(Optional.of(defaultResourceChecker)));

        ForbiddenException accessDeniedException =
                assertThrows(ForbiddenException.class, () -> underTest.authorizeDefaultOrElseCompute(resourceCrns, ACTION, externalFunction));

        verifyNoInteractions(externalFunction);
        assertEquals("Bad", accessDeniedException.getMessage());
    }

    @Test
    public void testWhenEmpty() {
        when(defaultResourceChecker.getDefaultResourceCrns(List.of()))
                .thenReturn(CrnsByCategory.newBuilder()
                        .build());

        Optional<AuthorizationRule> authorization = underTest.authorizeDefaultOrElseCompute(List.of(), ACTION, externalFunction);

        verifyNoInteractions(commonPermissionCheckingUtils);
        verifyNoInteractions(externalFunction);
        assertEquals(Optional.empty(), authorization);
    }

    @Test
    public void testWhenNoDefaultsChecker() {
        Optional<AuthorizationRule> expected = Optional.of(mock(AuthorizationRule.class));
        when(externalFunction.apply(anyCollection())).thenReturn(expected);

        Optional<AuthorizationRule> authorization = underTest.authorizeDefaultOrElseCompute(List.of(RESOURCE_CRN_1),
                AuthorizationResourceAction.EDIT_CREDENTIAL, externalFunction);

        verifyNoInteractions(commonPermissionCheckingUtils);
        verify(externalFunction).apply(List.of(RESOURCE_CRN_1));
        assertEquals(expected, authorization);
    }
}