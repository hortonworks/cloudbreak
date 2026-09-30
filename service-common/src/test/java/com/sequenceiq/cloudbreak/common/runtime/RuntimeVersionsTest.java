package com.sequenceiq.cloudbreak.common.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RuntimeVersionsTest {

    private static final String SUPPORTED = "7.2.17,7.2.18,7.3.1,7.3.2,7.3.3";

    private final RuntimeVersions underTest = new RuntimeVersions();

    @Test
    void baseIsBlankWhenNotConfigured() {
        underTest.setBase(null);

        assertThat(underTest.base()).isEmpty();
    }

    @Test
    void baseIsTheConfiguredValueWhenPresent() {
        underTest.setBase("7.3.2");

        assertThat(underTest.base()).isEqualTo("7.3.2");
    }

    @Test
    void supportedIsParsedInConfiguredOrderAndTolerantOfWhitespace() {
        underTest.setSupported(" 7.2.18 , 7.3.2 ,, 7.3.1 ");

        assertThat(underTest.supported()).containsExactly("7.2.18", "7.3.2", "7.3.1");
    }

    @Test
    void supportedIsEmptyWhenUnsetWhichMeansDoNotFilter() {
        underTest.setSupported("");

        assertThat(underTest.supported()).isEmpty();
    }

    @Test
    void patchedIsExactlyTheSupportedVersionsAboveTheBase() {
        underTest.setBase("7.3.1");
        underTest.setSupported(SUPPORTED);

        assertThat(underTest.patched()).containsExactly("7.3.2", "7.3.3");
    }

    @Test
    void patchedIsEmptyWhenTheBaseIsTheNewestSupportedVersion() {
        underTest.setBase("7.3.3");
        underTest.setSupported(SUPPORTED);

        assertThat(underTest.patched()).isEmpty();
    }

    @Test
    void patchedIsNeverConfigurableSoAVersionBelowTheBaseIsNotAnOverlay() {
        underTest.setBase("7.3.3");
        underTest.setSupported("7.2.18,7.3.4");

        assertThat(underTest.patched()).containsExactly("7.3.4");
    }

    @Test
    void patchedComparesVersionsNumericallyNotAsStrings() {
        underTest.setBase("7.2.9");
        underTest.setSupported("7.2.7,7.2.10,7.2.18");

        assertThat(underTest.patched()).containsExactly("7.2.10", "7.2.18");
    }

    @Test
    void latestStaysBlankWhenUnsetBecauseItIsNeverDerivedFromSupported() {
        // Deriving max(supported) here would silently pin the deployments that blank it out on purpose to get the
        // image-catalog fallback.
        underTest.setSupported(SUPPORTED);

        assertThat(underTest.latest()).isEmpty();
    }

    @Test
    void latestIsTheConfiguredValueWhenPresent() {
        underTest.setSupported(SUPPORTED);
        underTest.setLatest("7.3.2");

        assertThat(underTest.latest()).isEqualTo("7.3.2");
    }

    @Test
    void advertisedFallsBackToSupportedWhenNotPinned() {
        underTest.setSupported(SUPPORTED);

        assertThat(underTest.advertised()).containsExactly("7.2.17", "7.2.18", "7.3.1", "7.3.2", "7.3.3");
    }

    @Test
    void anExplicitAdvertisedListBeatsSupported() {
        underTest.setSupported(SUPPORTED);
        underTest.setAdvertised("7.3.2");

        assertThat(underTest.advertised()).containsExactly("7.3.2");
    }

    @Test
    void defaultVersionIsNeverDerivedBecauseItIsPromotedOnlyWhenAPrewarmedImageExists() {
        underTest.setSupported(SUPPORTED);

        assertThat(underTest.defaultVersion()).isEmpty();
    }

    @Test
    void defaultVersionIsTheConfiguredValueWhenPresent() {
        underTest.setDefaultRuntime("7.3.2");

        assertThat(underTest.defaultVersion()).isEqualTo("7.3.2");
    }

    @Test
    void overlayIsDisabledByDefault() {
        assertThat(underTest.overlayEnabled()).isFalse();
    }
}
