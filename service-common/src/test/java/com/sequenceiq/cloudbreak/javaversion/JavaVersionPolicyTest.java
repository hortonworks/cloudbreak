package com.sequenceiq.cloudbreak.javaversion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.conf.ServiceCommonConfig;

class JavaVersionPolicyTest {

    private static final String OLDEST_SOURCE_RUNTIME = "7.1.0";

    private static final JavaVersionPolicy SHIPPED_POLICY = loadShippedPolicy();

    @Test
    void shippedConfigurationIsLoadedAndValid() {
        assertThat(SHIPPED_POLICY.getVersions()).hasSize(4);
        assertThat(SHIPPED_POLICY.getDefaults()).hasSize(2);
        assertThat(SHIPPED_POLICY.getAutoUpgrades()).hasSize(2);
        assertThat(SHIPPED_POLICY.getVersions().stream().map(SupportedJavaVersion::jdk)).containsExactly(8, 11, 17, 21);
        assertThat(SHIPPED_POLICY.getAutoUpgrades().stream().map(JavaVersionAutoUpgrade::whenJdk)).containsOnly(11);
        assertThat(SHIPPED_POLICY.getSwitchRestrictions()).hasSize(2);
        assertThat(SHIPPED_POLICY.getSwitchRestrictions()).anySatisfy(restriction -> {
            assertThat(restriction.jdk()).isEqualTo(11);
            assertThat(restriction.until()).isNull();
        });
        assertThat(SHIPPED_POLICY.getSwitchRestrictions()).anySatisfy(restriction -> {
            assertThat(restriction.jdk()).isEqualTo(17);
            assertThat(restriction.until()).isEqualTo("7.3.1.500");
            assertThat(restriction.reason()).contains("CB-30923");
        });
    }

    @Test
    void theLoadedPolicyCannotBeModified() {
        assertThatThrownBy(() -> SHIPPED_POLICY.getVersions().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> SHIPPED_POLICY.getDefaults().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> SHIPPED_POLICY.getAutoUpgrades().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> SHIPPED_POLICY.getSwitchRestrictions().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest(name = "[{index}] runtime {0} supports {1}")
    @MethodSource("supportedVersions")
    void supportedVersionsPerRuntime(String runtimeVersion, List<Integer> expectedSupported) {
        assertThat(SHIPPED_POLICY.supportedVersions(runtimeVersion)).containsExactlyElementsOf(expectedSupported);
    }

    static Stream<Arguments> supportedVersions() {
        return Stream.of(
                Arguments.of("7.1.0", List.of(8, 11)),
                Arguments.of("7.2.9", List.of(8, 11)),
                Arguments.of("7.2.10", List.of(8, 11)),
                Arguments.of("7.2.18", List.of(8, 11)),
                Arguments.of("7.3.0", List.of(8, 11)),
                Arguments.of("7.3.1", List.of(8, 11, 17)),
                Arguments.of("7.3.1.500", List.of(8, 11, 17)),
                Arguments.of("7.3.2", List.of(17)),
                Arguments.of("7.3.2.30000", List.of(17)),
                Arguments.of("7.3.3", List.of(17, 21)),
                Arguments.of("7.3.3.0", List.of(17, 21)),
                Arguments.of("7.3.4", List.of(17, 21)),
                Arguments.of("7.3.9.100", List.of(17, 21)),
                Arguments.of("7.4.0", List.of(17, 21))
        );
    }

    @ParameterizedTest(name = "[{index}] runtime {0} allows switching to {1}")
    @MethodSource("switchableVersions")
    void switchableVersionsPerRuntime(String runtimeVersion, List<Integer> expectedSwitchable) {
        assertThat(SHIPPED_POLICY.switchableVersions(runtimeVersion)).containsExactlyElementsOf(expectedSwitchable);
    }

    static Stream<Arguments> switchableVersions() {
        return Stream.of(
                Arguments.of("7.1.0", List.of(8)),
                Arguments.of("7.2.18", List.of(8)),
                Arguments.of("7.3.1", List.of(8)),
                Arguments.of("7.3.1.100", List.of(8)),
                Arguments.of("7.3.1.499", List.of(8)),
                Arguments.of("7.3.1.500", List.of(8, 17)),
                Arguments.of("7.3.1.600", List.of(8, 17)),
                Arguments.of("7.3.2", List.of(17)),
                Arguments.of("7.3.3", List.of(17, 21)),
                Arguments.of("7.4.0", List.of(17, 21))
        );
    }

    @ParameterizedTest(name = "[{index}] a new cluster on {0} may be created with Java 17")
    @CsvSource({"7.3.1", "7.3.1.100", "7.3.1.499", "7.3.1.500", "7.3.2", "7.3.3"})
    void java17MayBeCreatedFrom731(String runtimeVersion) {
        assertThatCode(() -> SHIPPED_POLICY.validateSupported(runtimeVersion, 17)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "[{index}] a running cluster on {0} may not be switched to Java 17")
    @CsvSource({"7.3.1", "7.3.1.100", "7.3.1.499"})
    void java17MayNotBeSwitchedToBefore7315OO(String runtimeVersion) {
        assertThatThrownBy(() -> SHIPPED_POLICY.validateSwitchable(runtimeVersion, 17))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Java version 17 cannot be set for runtime " + runtimeVersion)
                .hasMessageContaining("CB-30923")
                .hasMessageContaining("Available Java versions are: [8]");
    }

    @Test
    void aRejectionWithoutARestrictionCarriesNoReason() {
        assertThatThrownBy(() -> SHIPPED_POLICY.validateSwitchable("7.3.1", 21))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Java version 21 cannot be set for runtime 7.3.1. Available Java versions are: [8].");
    }

    @Test
    void aRejectionForAnUnsupportedVersionCarriesNoRestrictionReason() {
        assertThatThrownBy(() -> SHIPPED_POLICY.validateSwitchable("7.2.18.600", 17))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Java version 17 cannot be set for runtime 7.2.18.600. Available Java versions are: [8].");
    }

    @ParameterizedTest(name = "[{index}] a running cluster on {0} may be switched to Java 17")
    @CsvSource({"7.3.1.500", "7.3.1.600", "7.3.2", "7.3.3", "7.4.0"})
    void java17MayBeSwitchedToFrom7315OO(String runtimeVersion) {
        assertThatCode(() -> SHIPPED_POLICY.validateSwitchable(runtimeVersion, 17)).doesNotThrowAnyException();
    }

    @Test
    void startupFailsWhenARestrictionFallsOutsideItsVersionRange() {
        JavaVersionPolicy policy = policyWithJava17(List.of(new JavaVersionSwitchRestriction(17, "7.3.4", "out of range")));

        assertThatThrownBy(policy::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("has 'until' outside the runtime range of");
    }

    @Test
    void startupFailsWhenARestrictionHasNoMatchingVersion() {
        JavaVersionPolicy policy = policyWithJava17(List.of(new JavaVersionSwitchRestriction(21, "7.3.4", "no such version")));

        assertThatThrownBy(policy::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("has no matching java.versions entry");
    }

    @Test
    void startupFailsWhenARestrictionHasNoReason() {
        JavaVersionPolicy policy = policyWithJava17(List.of(new JavaVersionSwitchRestriction(17, "7.3.1.500", "  ")));

        assertThatThrownBy(policy::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must declare 'reason' so the restriction stays explainable");
    }

    @Test
    void startupFailsWhenAJavaVersionIsRestrictedTwice() {
        JavaVersionPolicy policy = policyWithJava17(List.of(
                new JavaVersionSwitchRestriction(17, "7.3.1.500", "first"),
                new JavaVersionSwitchRestriction(17, "7.3.1.600", "second")));

        assertThatThrownBy(policy::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("more than one entry for the same Java version");
    }

    private static JavaVersionPolicy policyWithJava17(List<JavaVersionSwitchRestriction> switchRestrictions) {
        return new JavaVersionPolicy(List.of(new SupportedJavaVersion(17, "7.3.1", "7.3.3")), List.of(new DefaultJavaVersion(17, "7.3.1")),
                List.of(), switchRestrictions);
    }

    @ParameterizedTest(name = "[{index}] runtime {0} defaults to Java {1}")
    @CsvSource({
            "7.1.0, 8",
            "7.2.10, 8",
            "7.2.18, 8",
            "7.2.19, 8",
            "7.3.0, 8",
            "7.3.1, 17",
            "7.3.1.500, 17",
            "7.3.2, 17",
            "7.3.2.30000, 17",
            "7.3.3, 17",
            "7.3.3.0, 17",
            "7.3.4, 17"
    })
    void defaultVersionPerRuntime(String runtimeVersion, int expectedDefault) {
        assertThat(SHIPPED_POLICY.defaultVersion(runtimeVersion)).isEqualTo(expectedDefault);
    }

    @Test
    void theRequestedJavaVersionWinsOverTheDefault() {
        assertThat(SHIPPED_POLICY.requestedOrDefaultVersion(21, "7.3.3.0")).isEqualTo(21);
    }

    @Test
    void theDefaultIsUsedWhenNoJavaVersionIsRequested() {
        assertThat(SHIPPED_POLICY.requestedOrDefaultVersion(null, "7.3.3.0")).isEqualTo(17);
        assertThat(SHIPPED_POLICY.requestedOrDefaultVersion(null, "7.2.18")).isEqualTo(8);
    }

    @Test
    void defaultVersionFailsBelowTheOldestConfiguredRuntime() {
        assertThatThrownBy(() -> SHIPPED_POLICY.defaultVersion("7.0.99"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("is not supported in CDP");
    }

    @ParameterizedTest(name = "[{index}] Java 21 is supported on {0}")
    @CsvSource({"7.3.3", "7.3.3.0", "7.3.3.100", "7.3.4", "7.4.0"})
    void java21IsSupportedFrom733(String runtimeVersion) {
        assertThatCode(() -> SHIPPED_POLICY.validateSupported(runtimeVersion, 21)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "[{index}] Java 21 is rejected on {0}")
    @CsvSource({"7.2.18", "7.3.1", "7.3.1.600", "7.3.2", "7.3.2.30000"})
    void java21IsRejectedBefore733(String runtimeVersion) {
        assertThatThrownBy(() -> SHIPPED_POLICY.validateSupported(runtimeVersion, 21))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Java version 21 is not supported for runtime " + runtimeVersion);
    }

    @ParameterizedTest(name = "[{index}] a new cluster on {0} may be created with Java 11")
    @CsvSource({"7.1.0", "7.2.10", "7.2.18", "7.3.0", "7.3.1", "7.3.1.499", "7.3.1.600"})
    void java11MayBeRequestedUpTo731(String runtimeVersion) {
        assertThatCode(() -> SHIPPED_POLICY.validateSupported(runtimeVersion, 11)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "[{index}] Java 11 is not supported on {0}")
    @CsvSource({"7.0.2", "7.3.2", "7.3.3", "7.3.4"})
    void java11IsNotSupportedFrom732(String runtimeVersion) {
        assertThat(SHIPPED_POLICY.supportedVersions(runtimeVersion)).doesNotContain(11);
        assertThatThrownBy(() -> SHIPPED_POLICY.validateSupported(runtimeVersion, 11))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Java version 11 is not supported for runtime " + runtimeVersion);
    }

    @ParameterizedTest(name = "[{index}] a running cluster on {0} may never be switched to Java 11")
    @CsvSource({"7.2.18", "7.3.1", "7.3.1.600"})
    void java11IsNeverSwitchable(String runtimeVersion) {
        assertThat(SHIPPED_POLICY.switchableVersions(runtimeVersion)).doesNotContain(11);
        assertThatThrownBy(() -> SHIPPED_POLICY.validateSwitchable(runtimeVersion, 11))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Java 11 is kept only for existing clusters");
    }

    @Test
    void java11IsNotListedAmongTheSwitchableVersions() {
        assertThat(SHIPPED_POLICY.allSwitchableVersions()).containsExactly(8, 17, 21);
    }

    @Test
    void aRestrictionWithoutUntilBlocksEveryRuntime() {
        JavaVersionSwitchRestriction restriction = new JavaVersionSwitchRestriction(11, null, "legacy");

        assertThat(restriction.blocks("7.1.0")).isTrue();
        assertThat(restriction.blocks("9.9.9")).isTrue();
        assertThat(restriction).hasToString("Java 11 never switchable (legacy)");
    }

    @Test
    void unknownRuntimeIsRejectedInsteadOfFallingBackToAPermissiveList() {
        assertThatThrownBy(() -> SHIPPED_POLICY.validateSupported("", 8))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("the runtime version is unknown");
        assertThatThrownBy(() -> SHIPPED_POLICY.validateSupported(null, 8))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("the runtime version is unknown");
    }

    @ParameterizedTest(name = "[{index}] Java {1} on runtime {0} -> {2}")
    @MethodSource("autoUpgradeTargets")
    void autoUpgradeTargetPerRuntimeAndCurrentVersion(String runtimeVersion, Integer currentJavaVersion, OptionalInt expectedTarget) {
        assertThat(SHIPPED_POLICY.autoUpgradeTarget(OLDEST_SOURCE_RUNTIME, runtimeVersion, currentJavaVersion)).isEqualTo(expectedTarget);
    }

    static Stream<Arguments> autoUpgradeTargets() {
        return Stream.of(
                Arguments.of("7.2.18", 8, OptionalInt.empty()),
                Arguments.of("7.3.1", 8, OptionalInt.empty()),
                Arguments.of("7.3.1.500", 8, OptionalInt.empty()),
                Arguments.of("7.3.2", 8, OptionalInt.of(17)),
                Arguments.of("7.3.3.0", 8, OptionalInt.of(17)),
                Arguments.of("7.2.18", 11, OptionalInt.empty()),
                Arguments.of("7.3.0", 11, OptionalInt.empty()),
                Arguments.of("7.3.1", 11, OptionalInt.of(8)),
                Arguments.of("7.3.1.400", 11, OptionalInt.of(8)),
                Arguments.of("7.3.1.500", 11, OptionalInt.of(17)),
                Arguments.of("7.3.2", 11, OptionalInt.of(17)),
                Arguments.of("7.3.3.0", 11, OptionalInt.of(17)),
                Arguments.of("7.4.0", 17, OptionalInt.empty()),
                Arguments.of("7.3.2", 17, OptionalInt.empty()),
                Arguments.of("7.3.3.0", 17, OptionalInt.empty()),
                Arguments.of("7.3.4", 17, OptionalInt.empty()),
                Arguments.of("7.3.2", 21, OptionalInt.empty()),
                Arguments.of("7.3.4", 21, OptionalInt.empty()),
                Arguments.of("7.4.0", 21, OptionalInt.empty()),
                Arguments.of("7.3.3.0", null, OptionalInt.empty()),
                Arguments.of("", 8, OptionalInt.empty())
        );
    }

    @Test
    void java11ClusterOnAnOlderRuntimeIsNotSilentlyDowngraded() {
        assertThat(SHIPPED_POLICY.autoUpgradeTarget(OLDEST_SOURCE_RUNTIME, "7.2.9", 11)).isEqualTo(OptionalInt.empty());
    }

    @Test
    void startupFailsWhenADefaultIsNotSupported() {
        JavaVersionPolicy policy = new JavaVersionPolicy(List.of(new SupportedJavaVersion(17, "7.3.1", "7.3.3")),
                List.of(new DefaultJavaVersion(21, "7.3.1")), List.of(), List.of());

        assertThatThrownBy(policy::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("java.defaults gives Java 21 on runtime 7.3.1, which is not supported there");
    }

    @Test
    void startupFailsWhenTheNewestRuntimeHasNoSupportedDefault() {
        JavaVersionPolicy policy = new JavaVersionPolicy(List.of(
                new SupportedJavaVersion(17, "7.3.1", "7.4.0"),
                new SupportedJavaVersion(21, "7.4.0", null)),
                List.of(new DefaultJavaVersion(17, "7.3.1")), List.of(), List.of());

        assertThatThrownBy(policy::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("java.defaults gives Java 17 on runtime 7.4.0, which is not supported there");
    }

    @Test
    void startupFailsWhenARangeIsInverted() {
        JavaVersionPolicy policy = new JavaVersionPolicy(List.of(new SupportedJavaVersion(17, "7.3.3", "7.3.1")),
                List.of(new DefaultJavaVersion(17, "7.3.3")), List.of(), List.of());

        assertThatThrownBy(policy::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("'from' greater than or equal to 'until'");
    }

    @Test
    void startupFailsWhenAJavaVersionIsDeclaredTwice() {
        JavaVersionPolicy policy = new JavaVersionPolicy(List.of(
                new SupportedJavaVersion(17, "7.3.1", "7.3.3"),
                new SupportedJavaVersion(17, "7.3.3", null)),
                List.of(new DefaultJavaVersion(17, "7.3.1")), List.of(), List.of());

        assertThatThrownBy(policy::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("duplicated Java versions");
    }

    @Test
    void startupFailsWhenAnAutoUpgradeTargetIsNotSwitchable() {
        JavaVersionPolicy policy = new JavaVersionPolicy(List.of(new SupportedJavaVersion(17, "7.3.1", null)),
                List.of(new DefaultJavaVersion(17, "7.3.1")), List.of(new JavaVersionAutoUpgrade("7.3.1", null, 11, 8)), List.of());

        assertThatThrownBy(policy::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("targets Java 8 which is not switchable on runtime 7.3.1");
    }

    @Test
    void startupFailsWhenADefaultFallsIntoAGapBetweenVersions() {
        JavaVersionPolicy policy = new JavaVersionPolicy(
                List.of(new SupportedJavaVersion(8, "7.1.0", "7.2.19"), new SupportedJavaVersion(17, "7.3.1", null)),
                List.of(new DefaultJavaVersion(8, "7.1.0"), new DefaultJavaVersion(17, "7.3.1")),
                List.of(), List.of());

        assertThatThrownBy(policy::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("java.defaults gives Java 8 on runtime 7.2.19, which is not supported there");
    }

    @Test
    void startupFailsWhenAnAutoUpgradeTargetEndsInsideTheRuleRange() {
        JavaVersionPolicy policy = policyWhereTheJava17RuleOutlivesJava17();

        assertThatThrownBy(policy::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("targets Java 17 which is not switchable on runtime 7.4.0");
    }

    @Test
    void anAutoUpgradeRuleIsIgnoredWhereItsTargetIsNotSwitchable() {
        JavaVersionPolicy policy = policyWhereTheJava17RuleOutlivesJava17();

        assertThat(policy.autoUpgradeTarget(OLDEST_SOURCE_RUNTIME, "7.3.2", 11)).isEqualTo(OptionalInt.of(17));
        assertThat(policy.autoUpgradeTarget(OLDEST_SOURCE_RUNTIME, "7.4.0", 11)).isEqualTo(OptionalInt.of(21));
    }

    @Test
    void startupAcceptsRuntimesBeyondTheLastClosedVersionWithoutADefault() {
        JavaVersionPolicy policy = new JavaVersionPolicy(
                List.of(new SupportedJavaVersion(17, "7.3.1", "7.4.0")),
                List.of(new DefaultJavaVersion(17, "7.3.1")),
                List.of(), List.of());

        assertThatCode(policy::validateConfiguration).doesNotThrowAnyException();
    }

    private static JavaVersionPolicy policyWhereTheJava17RuleOutlivesJava17() {
        return new JavaVersionPolicy(
                List.of(new SupportedJavaVersion(17, "7.3.1", "7.4.0"), new SupportedJavaVersion(21, "7.3.3", null)),
                List.of(new DefaultJavaVersion(17, "7.3.1"), new DefaultJavaVersion(21, "7.4.0")),
                List.of(new JavaVersionAutoUpgrade("7.3.1.500", null, 11, 17)),
                List.of());
    }

    @ParameterizedTest(name = "[{index}] Java {2} upgraded from {0} to {1} -> {3}")
    @MethodSource("crossingRuleCases")
    void aRuleForAStillSupportedVersionOnlyAppliesWhenTheUpgradeCrossesIt(String sourceRuntimeVersion, String runtimeVersion, int currentJavaVersion,
            OptionalInt expectedTarget) {
        assertThat(policyWithJava17To21RuleFrom733().autoUpgradeTarget(sourceRuntimeVersion, runtimeVersion, currentJavaVersion))
                .isEqualTo(expectedTarget);
    }

    static Stream<Arguments> crossingRuleCases() {
        return Stream.of(
                Arguments.of("7.3.2", "7.3.3", 17, OptionalInt.of(21)),
                Arguments.of("7.3.2", "7.3.4", 17, OptionalInt.of(21)),
                Arguments.of("7.3.2.30000", "7.3.3.0", 17, OptionalInt.of(21)),
                Arguments.of("7.3.3", "7.3.4", 17, OptionalInt.empty()),
                Arguments.of("7.3.3.0", "7.3.3.100", 17, OptionalInt.empty()),
                Arguments.of("7.3.1", "7.3.2", 17, OptionalInt.empty()),
                Arguments.of("7.3.2", "7.3.4", 21, OptionalInt.empty()),
                Arguments.of("7.3.1", "7.3.2", 8, OptionalInt.of(17))
        );
    }

    @Test
    void aRuleForAStillSupportedVersionIsNotAppliedWhenTheCurrentRuntimeIsUnknown() {
        assertThat(policyWithJava17To21RuleFrom733().autoUpgradeTarget("", "7.3.4", 17)).isEqualTo(OptionalInt.empty());
        assertThat(policyWithJava17To21RuleFrom733().autoUpgradeTarget(null, "7.3.4", 17)).isEqualTo(OptionalInt.empty());
    }

    @ParameterizedTest(name = "[{index}] Java 11 upgraded from {0} to {1} -> {2}")
    @MethodSource("java11Cases")
    void rulesForAnUnsupportedVersionApplyOnEveryUpgradeIntoTheirRange(String sourceRuntimeVersion, String runtimeVersion, OptionalInt expectedTarget) {
        assertThat(SHIPPED_POLICY.autoUpgradeTarget(sourceRuntimeVersion, runtimeVersion, 11)).isEqualTo(expectedTarget);
    }

    static Stream<Arguments> java11Cases() {
        return Stream.of(
                Arguments.of("7.3.1.100", "7.3.1.200", OptionalInt.of(8)),
                Arguments.of("7.3.1.600", "7.3.1.700", OptionalInt.of(17)),
                Arguments.of("", "7.3.2", OptionalInt.of(17))
        );
    }

    @Test
    void startupFailsWhenARuleMovesAVersionToItself() {
        JavaVersionPolicy policy = new JavaVersionPolicy(List.of(new SupportedJavaVersion(17, "7.3.1", null)),
                List.of(new DefaultJavaVersion(17, "7.3.1")), List.of(new JavaVersionAutoUpgrade("7.3.3", null, 17, 17)), List.of());

        assertThatThrownBy(policy::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("moves Java 17 to itself");
    }

    @Test
    void startupFailsWhenTwoRulesForTheSameVersionOverlap() {
        JavaVersionPolicy policy = new JavaVersionPolicy(
                List.of(new SupportedJavaVersion(17, "7.3.1", null), new SupportedJavaVersion(21, "7.3.3", null)),
                List.of(new DefaultJavaVersion(17, "7.3.1")),
                List.of(new JavaVersionAutoUpgrade("7.3.3", null, 17, 21), new JavaVersionAutoUpgrade("7.4.0", null, 17, 21)),
                List.of());

        assertThatThrownBy(policy::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("overlap, so it is ambiguous which one applies");
    }

    @Test
    void startupAcceptsRulesForTheSameVersionThatOnlyTouch() {
        assertThatCode(() -> SHIPPED_POLICY.validateConfiguration()).doesNotThrowAnyException();
        assertThatCode(() -> policyWithJava17To21RuleFrom733().validateConfiguration()).doesNotThrowAnyException();
    }

    @Test
    void theShippedAutoUpgradeRulesBindTheirTargetRange() {
        assertThat(SHIPPED_POLICY.getAutoUpgrades()).containsExactly(
                new JavaVersionAutoUpgrade("7.3.1", "7.3.1.500", 11, 8),
                new JavaVersionAutoUpgrade("7.3.1.500", null, 11, 17));
    }

    @Test
    void startupNamesTheTargetFieldsWhenAnAutoUpgradeRangeIsInverted() {
        JavaVersionPolicy policy = new JavaVersionPolicy(List.of(new SupportedJavaVersion(17, "7.3.1", null)),
                List.of(new DefaultJavaVersion(17, "7.3.1")), List.of(new JavaVersionAutoUpgrade("7.3.3", "7.3.1", 11, 17)), List.of());

        assertThatThrownBy(policy::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("has 'targetFrom' greater than or equal to 'targetUntil'");
    }

    @Test
    void aRestrictionStillUsingTheOldFromKeyFailsStartup() {
        new ApplicationContextRunner()
                .withUserConfiguration(ServiceCommonConfig.class)
                .withPropertyValues("java.switch-restrictions[0].jdk=17", "java.switch-restrictions[0].from=7.3.1.500",
                        "java.switch-restrictions[0].reason=old key")
                .run(context -> assertThat(context).getFailure().rootCause()
                        .hasMessageContaining("targets Java 17 which is not switchable on runtime 7.3.1.500"));
    }

    @Test
    void aVersionEntryStillUsingTheOldVersionKeyFailsStartupInsteadOfBecomingJava0() {
        new ApplicationContextRunner()
                .withUserConfiguration(ServiceCommonConfig.class)
                .withPropertyValues("java.versions[0].version=17", "java.versions[0].from=7.3.1")
                .run(context -> assertThat(context).getFailure().rootCause()
                        .hasMessageContaining("java.versions entry Java 0 [7.3.1, *) must declare 'jdk'"));
    }

    @Test
    void anAutoUpgradeRuleStillUsingTheOldKeysFailsStartup() {
        new ApplicationContextRunner()
                .withUserConfiguration(ServiceCommonConfig.class)
                .withPropertyValues("java.auto-upgrades[0].targetFrom=7.3.1.500", "java.auto-upgrades[0].whenCurrent=11",
                        "java.auto-upgrades[0].setTo=17")
                .run(context -> assertThat(context).getFailure().rootCause()
                        .hasMessageContaining("must declare 'whenJdk'"));
    }

    private static JavaVersionPolicy policyWithJava17To21RuleFrom733() {
        return new JavaVersionPolicy(
                List.of(new SupportedJavaVersion(8, "7.1.0", "7.3.1.X"), new SupportedJavaVersion(17, "7.3.1", null),
                        new SupportedJavaVersion(21, "7.3.3", null)),
                List.of(new DefaultJavaVersion(8, "7.1.0"), new DefaultJavaVersion(17, "7.3.1")),
                List.of(new JavaVersionAutoUpgrade("7.3.3", null, 17, 21)),
                List.of());
    }

    private static JavaVersionPolicy loadShippedPolicy() {
        JavaVersionPolicy[] loaded = new JavaVersionPolicy[1];
        new ApplicationContextRunner()
                .withUserConfiguration(ServiceCommonConfig.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    loaded[0] = context.getBean(JavaVersionPolicy.class);
                });
        return loaded[0];
    }
}
