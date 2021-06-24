package com.sequenceiq.authorization.utils;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.getUserCrn;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.CommonPermissionCheckingUtils;

@ExtendWith(MockitoExtension.class)
public class EventAuthorizationUtilsTest {

    @Mock
    private CommonPermissionCheckingUtils mockCommonPermissionCheckingUtils;

    private EventAuthorizationUtils underTest;

    @BeforeEach
    public void setUp() {
        underTest = new EventAuthorizationUtils(mockCommonPermissionCheckingUtils);
    }

    @Test
    public void testWhenEmptySetPassedThenNoPermissionCheckCallShouldHappen() {
        underTest.checkPermissionBasedOnResourceTypeAndCrn(emptySet());

        verify(mockCommonPermissionCheckingUtils, never()).checkPermissionForUserOnResource(any(), any(), any());
    }

    @ParameterizedTest
    @MethodSource("testInputForCheckTesting")
    public void testWhenProperSetOfDtosHasPassedThenProperCallShouldHappen(AuthzActionTypePair testPair) {
        EventAuthorizationDto testDto = new EventAuthorizationDto(testPair.getResourceType() + "someResourceCrn", testPair.getResourceType().name(), "FLOW");

        underTest.checkPermissionBasedOnResourceTypeAndCrn(Set.of(testDto));
        verify(mockCommonPermissionCheckingUtils, times(1))
                .checkPermissionForUserOnResource(testPair.getResourceAction(), getUserCrn(), testDto.getResourceCrn());
        verifyNoMoreInteractions(mockCommonPermissionCheckingUtils);
    }

    @ParameterizedTest
    @MethodSource("testInputForNoActionCheckTesting")
    public void testWhenActionRequiresHasNoActionThenNoCheckHappens(AuthzActionTypePair testPair) {
        EventAuthorizationDto testDto = new EventAuthorizationDto(testPair.getResourceType() + "someResourceCrn", testPair.getResourceType().name(), "FLOW");

        underTest.checkPermissionBasedOnResourceTypeAndCrn(Set.of(testDto));

        verifyNoInteractions(mockCommonPermissionCheckingUtils);
    }

    @Test
    public void testAllTheResourceTypesHasNoIssues() {
        List<EventAuthorizationDto> dtos = Arrays.asList(AuthorizationResourceType.values()).stream()
                .map(type -> new EventAuthorizationDto("someCrn", type.name().toLowerCase(), "EVENT"))
                .collect(Collectors.toList());

        List<String> issues = new LinkedList<>();

        for (EventAuthorizationDto dto : dtos) {
            try {
                underTest.checkPermissionBasedOnResourceTypeAndCrn(Set.of(dto));
            } catch (IllegalStateException ise) {
                issues.add(String.format("[resourceType: %s, message: %s]", dto.getResourceType(), ise.getMessage()));
            }
        }
        assertTrue(issues.isEmpty(), String.join("The following resource types has issues: {%s}", String.join(", ", issues)));
    }

    public static Set<AuthzActionTypePair> testInputForNoActionCheckTesting() {
        return GetAuthzActionTypeProvider.getPairs()
                .stream()
                .filter(AuthzActionTypePair::hasNoAction)
                .collect(Collectors.toSet());
    }

    public static Set<AuthzActionTypePair> testInputForCheckTesting() {
        return GetAuthzActionTypeProvider.getPairs()
                .stream()
                .filter(AuthzActionTypePair::hasAction)
                .collect(Collectors.toSet());
    }

}