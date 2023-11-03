package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.doAs;
import static com.sequenceiq.common.model.OsType.CENTOS7;
import static com.sequenceiq.common.model.OsType.RHEL8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@ExtendWith(MockitoExtension.class)
class ImageOsServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:hortonworks:user:test@test.com";

    private static final String ACCOUNT_ID = "hortonworks";

    private static final String DEFAULT_OS = CENTOS7.getOs();

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private ImageOsService underTest;

    @BeforeEach
    void setUp() {
        setDefaultOs(DEFAULT_OS);
    }

    @Test
    void isSupportedDefaultOs() {
        boolean result = underTest.isSupported(DEFAULT_OS);

        assertThat(result).isTrue();
    }

    @Test
    void isSupportedRedhat8() {
        boolean result = doAs(USER_CRN, () -> underTest.isSupported(RHEL8.getOs()));

        assertThat(result).isFalse();
        verify(entitlementService).isRhel8ImageSupportEnabled(ACCOUNT_ID);
    }

    @Test
    void isSupportedDefaultRedhat8Os() {
        setDefaultOs(RHEL8.getOs());

        boolean result = underTest.isSupported(DEFAULT_OS);

        assertThat(result).isTrue();
        verifyNoInteractions(entitlementService);
    }

    @Test
    void isSupportedRandomOs() {
        boolean result = underTest.isSupported("random");

        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @MethodSource("preferredOsArguments")
    void preferredOs(String requestedOs, boolean rhel8ImagePreferred, String expectedOs) {
        lenient().when(entitlementService.isRhel8ImagePreferred(ACCOUNT_ID)).thenReturn(rhel8ImagePreferred);

        String result = doAs(USER_CRN, () -> underTest.getPreferredOs(requestedOs));

        assertThat(result).isEqualTo(expectedOs);
    }

    static Stream<Arguments> preferredOsArguments() {
        return Stream.of(
                Arguments.of(null, false, DEFAULT_OS),
                Arguments.of(null, true, RHEL8.getOs()),
                Arguments.of(RHEL8.getOs(), false, RHEL8.getOs()),
                Arguments.of("random", false, "random"),
                Arguments.of("random", true, "random")
        );
    }

    private void setDefaultOs(String os) {
        ReflectionTestUtils.setField(underTest, "defaultOs", os);
    }

}
