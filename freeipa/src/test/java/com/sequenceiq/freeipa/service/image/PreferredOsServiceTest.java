package com.sequenceiq.freeipa.service.image;

import static com.sequenceiq.common.model.OsType.CENTOS7;
import static com.sequenceiq.common.model.OsType.RHEL8;
import static com.sequenceiq.common.model.OsType.RHEL9;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.common.model.OsType;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PreferredOsServiceTest {

    private static final String ACCOUNT_ID = "cloudera";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:" + UUID.randomUUID();

    private static final String INTERNAL_CRN = "crn:cdp:iam:us-west-1:altus:user:__internal__actor__";

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private PreferredOsService victim;

    @ParameterizedTest(name = "testGetDefaultOs: rhel9Enabled={0} defaultOs={1} resultOs={2}")
    @MethodSource("testGetDefaultOsData")
    void testGetDefaultOs(boolean rhel9Enabled, OsType defaultOs, OsType resultOs) {
        ReflectionTestUtils.setField(victim, PreferredOsService.class, "defaultOs", defaultOs.getOs(), null);
        when(entitlementService.isEntitledToUseOS(ACCOUNT_ID, OsType.getLatestOsType())).thenReturn(rhel9Enabled);

        String actual = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> victim.getDefaultOs());

        assertEquals(resultOs.getOs(), actual);
    }

    static Stream<Arguments> testGetDefaultOsData() {
        return Stream.of(
                Arguments.of(true, CENTOS7, RHEL9),
                Arguments.of(true, RHEL8, RHEL9),
                Arguments.of(true, RHEL9, RHEL9),
                Arguments.of(false, CENTOS7, CENTOS7),
                Arguments.of(false, RHEL8, RHEL8),
                Arguments.of(false, RHEL9, RHEL9)
        );
    }

    @ParameterizedTest(name = "testGetDefaultOs: rhel9Enabled={0} defaultOs={1} resultOs={2}")
    @MethodSource("testGetDefaultOsDataInternalUser")
    void testGetDefaultOsInternal(boolean rhel9Enabled, OsType defaultOs, OsType resultOs) {
        ReflectionTestUtils.setField(victim, PreferredOsService.class, "defaultOs", defaultOs.getOs(), null);
        when(entitlementService.isEntitledToUseOS(ACCOUNT_ID, OsType.getLatestOsType())).thenReturn(rhel9Enabled);

        String actual = ThreadBasedUserCrnProvider.doAs(INTERNAL_CRN, () -> victim.getDefaultOs());

        assertEquals(resultOs.getOs(), actual);

    }

    static Stream<Arguments> testGetDefaultOsDataInternalUser() {
        return Stream.of(
                Arguments.of(true, CENTOS7, CENTOS7),
                Arguments.of(true, RHEL8, RHEL8),
                Arguments.of(true, RHEL9, RHEL9),
                Arguments.of(false, CENTOS7, CENTOS7),
                Arguments.of(false, RHEL8, RHEL8),
                Arguments.of(false, RHEL9, RHEL9)
        );
    }

    @ParameterizedTest(name = "getPreferredOs: rhel9Enabled={0} defaultOs={1} requestedOs={2} resultOs={3}")
    @MethodSource("getPreferredOsData")
    void testGetPreferredOs(boolean rhel9Enabled, OsType defaultOs, OsType requestedOs, OsType resultOs) {
        ReflectionTestUtils.setField(victim, PreferredOsService.class, "defaultOs", defaultOs.getOs(), null);
        when(entitlementService.isEntitledToUseOS(ACCOUNT_ID, OsType.getLatestOsType())).thenReturn(rhel9Enabled);

        String actual = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> victim.getPreferredOs(requestedOs == null ? null : requestedOs.getOs()));

        assertEquals(resultOs.getOs(), actual);
    }

    static Stream<Arguments> getPreferredOsData() {
        return Stream.of(
                Arguments.of(true, RHEL8, CENTOS7, RHEL9),
                Arguments.of(true, RHEL8, RHEL8, RHEL9),
                Arguments.of(true, RHEL8, RHEL9, RHEL9),
                Arguments.of(true, RHEL8, null, RHEL9),

                Arguments.of(false, RHEL8, CENTOS7, CENTOS7),
                Arguments.of(false, RHEL8, RHEL8, RHEL8),
                Arguments.of(false, RHEL8, RHEL9, RHEL8),
                Arguments.of(false, RHEL8, null, RHEL8)
        );
    }

    @ParameterizedTest(name = "getPreferredOs: rhel9Enabled={0} defaultOs={1} requestedOs={2} resultOs={3}")
    @MethodSource("getPreferredOsDataInternal")
    void testGetPreferredOsInternal(boolean rhel9Enabled, OsType defaultOs, OsType requestedOs, OsType resultOs) {
        ReflectionTestUtils.setField(victim, PreferredOsService.class, "defaultOs", defaultOs.getOs(), null);
        when(entitlementService.isEntitledToUseOS(ACCOUNT_ID, OsType.getLatestOsType())).thenReturn(rhel9Enabled);

        String actual = ThreadBasedUserCrnProvider.doAs(INTERNAL_CRN,
                () -> victim.getPreferredOs(requestedOs == null ? null : requestedOs.getOs()));

        assertEquals(resultOs.getOs(), actual);
    }

    static Stream<Arguments> getPreferredOsDataInternal() {
        return Stream.of(
                Arguments.of(true, RHEL8, CENTOS7, CENTOS7),
                Arguments.of(true, RHEL8, RHEL8, RHEL8),
                Arguments.of(true, RHEL8, RHEL9, RHEL8),
                Arguments.of(true, RHEL8, null, RHEL8),

                Arguments.of(false, RHEL8, CENTOS7, CENTOS7),
                Arguments.of(false, RHEL8, RHEL8, RHEL8),
                Arguments.of(false, RHEL8, RHEL9, RHEL8),
                Arguments.of(false, RHEL8, null, RHEL8)
        );
    }
}