package com.sequenceiq.cloudbreak.javaversion;

import static com.sequenceiq.cloudbreak.javaversion.RuntimeVersionRange.compareVersions;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

@ConfigurationProperties(prefix = "java")
public class JavaVersionPolicy {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaVersionPolicy.class);

    private static final Comparator<String> VERSION_ORDER = RuntimeVersionRange::compareVersions;

    private final List<SupportedJavaVersion> versions;

    private final List<DefaultJavaVersion> defaults;

    private final List<JavaVersionAutoUpgrade> autoUpgrades;

    private final List<JavaVersionSwitchRestriction> switchRestrictions;

    public JavaVersionPolicy(@DefaultValue List<SupportedJavaVersion> versions, @DefaultValue List<DefaultJavaVersion> defaults,
            @DefaultValue List<JavaVersionAutoUpgrade> autoUpgrades, @DefaultValue List<JavaVersionSwitchRestriction> switchRestrictions) {
        this.versions = List.copyOf(versions);
        this.defaults = List.copyOf(defaults);
        this.autoUpgrades = List.copyOf(autoUpgrades);
        this.switchRestrictions = List.copyOf(switchRestrictions);
    }

    @PostConstruct
    public void validateConfiguration() {
        validateVersions();
        validateDefaults();
        validateSwitchRestrictions();
        validateAutoUpgrades();
        validateEveryBoundaryPoint();
        LOGGER.info("Java version policy loaded. Versions: {}, defaults: {}, switch restrictions: {}, auto upgrades: {}",
                versions, defaults, switchRestrictions, autoUpgrades);
    }

    public List<SupportedJavaVersion> getVersions() {
        return versions;
    }

    public List<DefaultJavaVersion> getDefaults() {
        return defaults;
    }

    public List<JavaVersionAutoUpgrade> getAutoUpgrades() {
        return autoUpgrades;
    }

    public List<JavaVersionSwitchRestriction> getSwitchRestrictions() {
        return switchRestrictions;
    }

    public SortedSet<Integer> supportedVersions(String runtimeVersion) {
        return versions.stream()
                .filter(supportedJavaVersion -> supportedJavaVersion.covers(runtimeVersion))
                .map(SupportedJavaVersion::jdk)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public SortedSet<Integer> switchableVersions(String runtimeVersion) {
        return supportedVersions(runtimeVersion).stream()
                .filter(javaVersion -> blockingRestriction(runtimeVersion, javaVersion).isEmpty())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public SortedSet<Integer> allSwitchableVersions() {
        return versions.stream()
                .map(SupportedJavaVersion::jdk)
                .filter(javaVersion -> switchRestrictions.stream()
                        .noneMatch(restriction -> restriction.jdk() == javaVersion && StringUtils.isBlank(restriction.until())))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private Optional<JavaVersionSwitchRestriction> blockingRestriction(String runtimeVersion, int javaVersion) {
        return switchRestrictions.stream()
                .filter(restriction -> restriction.jdk() == javaVersion && restriction.blocks(runtimeVersion))
                .findFirst();
    }

    public void validateSwitchable(String runtimeVersion, int javaVersion) {
        validateRuntimeVersionIsKnown(runtimeVersion, javaVersion);
        SortedSet<Integer> switchableVersions = switchableVersions(runtimeVersion);
        if (!switchableVersions.contains(javaVersion)) {
            String restrictionReason = supportedVersions(runtimeVersion).contains(javaVersion)
                    ? blockingRestriction(runtimeVersion, javaVersion).map(restriction -> " " + restriction.reason() + ".").orElse("")
                    : "";
            throw new BadRequestException(String.format("Java version %d cannot be set for runtime %s.%s Available Java versions are: %s.",
                    javaVersion, runtimeVersion, restrictionReason, switchableVersions));
        }
    }

    public void validateSupported(String runtimeVersion, int javaVersion) {
        validateRuntimeVersionIsKnown(runtimeVersion, javaVersion);
        SortedSet<Integer> supportedVersions = supportedVersions(runtimeVersion);
        if (!supportedVersions.contains(javaVersion)) {
            throw new BadRequestException(String.format("Java version %d is not supported for runtime %s. Supported Java versions are: %s.",
                    javaVersion, runtimeVersion, supportedVersions));
        }
    }

    private void validateRuntimeVersionIsKnown(String runtimeVersion, int javaVersion) {
        if (StringUtils.isBlank(runtimeVersion)) {
            throw new BadRequestException(String.format("Java version %d cannot be validated because the runtime version is unknown.", javaVersion));
        }
    }

    public int requestedOrDefaultVersion(Integer requestedJavaVersion, String runtimeVersion) {
        return requestedJavaVersion == null ? defaultVersion(runtimeVersion) : requestedJavaVersion;
    }

    public int defaultVersion(String runtimeVersion) {
        return defaults.stream()
                .filter(defaultJavaVersion -> StringUtils.isNotBlank(runtimeVersion)
                        && compareVersions(runtimeVersion, defaultJavaVersion.from()) >= 0)
                .max(Comparator.comparing(DefaultJavaVersion::from, VERSION_ORDER))
                .map(DefaultJavaVersion::jdk)
                .orElseThrow(() -> new BadRequestException(String.format("The runtimeVersion %s is not supported in CDP. "
                        + "Please change the runtime version.", runtimeVersion)));
    }

    public OptionalInt autoUpgradeTarget(String sourceRuntimeVersion, String runtimeVersion, Integer currentJavaVersion) {
        if (currentJavaVersion == null || StringUtils.isBlank(runtimeVersion)) {
            return OptionalInt.empty();
        }
        SortedSet<Integer> switchableVersions = switchableVersions(runtimeVersion);
        if (switchableVersions.contains(currentJavaVersion)) {
            return crossedRuleTarget(sourceRuntimeVersion, runtimeVersion, currentJavaVersion);
        }
        Optional<JavaVersionAutoUpgrade> matchingRule = findAutoUpgradeRule(currentJavaVersion, runtimeVersion);
        if (matchingRule.isPresent()) {
            if (switchableVersions.contains(matchingRule.get().setJdk())) {
                LOGGER.debug("Java {} is not switchable on runtime {}, auto upgrade rule {} applies.", currentJavaVersion, runtimeVersion, matchingRule.get());
                return OptionalInt.of(matchingRule.get().setJdk());
            }
            LOGGER.warn("Auto upgrade rule {} targets Java {} which is not switchable on runtime {}, ignoring the rule. Switchable versions: {}.",
                    matchingRule.get(), matchingRule.get().setJdk(), runtimeVersion, switchableVersions);
        }
        if (switchableVersions.isEmpty() || switchableVersions.first() <= currentJavaVersion) {
            LOGGER.warn("Java {} is not switchable on runtime {} but no auto upgrade rule applies, keeping the current version. Switchable versions: {}.",
                    currentJavaVersion, runtimeVersion, switchableVersions);
            return OptionalInt.empty();
        }
        LOGGER.debug("Java {} is not switchable on runtime {}, falling back to the lowest switchable version {}.",
                currentJavaVersion, runtimeVersion, switchableVersions.first());
        return OptionalInt.of(switchableVersions.first());
    }

    private Optional<JavaVersionAutoUpgrade> findAutoUpgradeRule(int currentJavaVersion, String runtimeVersion) {
        return autoUpgrades.stream()
                .filter(autoUpgrade -> autoUpgrade.whenJdk() == currentJavaVersion && autoUpgrade.covers(runtimeVersion))
                .findFirst();
    }

    private OptionalInt crossedRuleTarget(String sourceRuntimeVersion, String runtimeVersion, int currentJavaVersion) {
        Optional<JavaVersionAutoUpgrade> matchingRule = findAutoUpgradeRule(currentJavaVersion, runtimeVersion);
        if (matchingRule.isEmpty()) {
            return OptionalInt.empty();
        }
        JavaVersionAutoUpgrade rule = matchingRule.get();
        if (StringUtils.isBlank(sourceRuntimeVersion)) {
            LOGGER.warn("Auto upgrade rule {} covers runtime {} but the current runtime is unknown, so it cannot be decided whether the upgrade crosses "
                    + "the rule. Keeping Java {}.", rule, runtimeVersion, currentJavaVersion);
            return OptionalInt.empty();
        }
        if (compareVersions(sourceRuntimeVersion, rule.targetFrom()) >= 0) {
            LOGGER.debug("Upgrade from runtime {} to {} does not cross auto upgrade rule {}, keeping Java {}.",
                    sourceRuntimeVersion, runtimeVersion, rule, currentJavaVersion);
            return OptionalInt.empty();
        }
        if (!switchableVersions(runtimeVersion).contains(rule.setJdk())) {
            LOGGER.warn("Auto upgrade rule {} targets Java {} which is not switchable on runtime {}, ignoring the rule.", rule, rule.setJdk(), runtimeVersion);
            return OptionalInt.empty();
        }
        LOGGER.debug("Upgrade from runtime {} to {} crosses auto upgrade rule {}.", sourceRuntimeVersion, runtimeVersion, rule);
        return OptionalInt.of(rule.setJdk());
    }

    private void validateVersions() {
        if (versions.isEmpty()) {
            throw new IllegalStateException("java.versions must not be empty.");
        }
        versions.forEach(supportedJavaVersion -> {
            validateJdkIsDeclared(supportedJavaVersion.jdk(), "jdk", "java.versions entry " + supportedJavaVersion);
            validateRange(supportedJavaVersion, "java.versions entry " + supportedJavaVersion, "from", "until");
        });
        if (versions.stream().map(SupportedJavaVersion::jdk).distinct().count() != versions.size()) {
            throw new IllegalStateException("java.versions contains duplicated Java versions: " + versions);
        }
    }

    private void validateDefaults() {
        if (defaults.isEmpty()) {
            throw new IllegalStateException("java.defaults must not be empty.");
        }
        defaults.forEach(defaultJavaVersion -> {
            validateJdkIsDeclared(defaultJavaVersion.jdk(), "jdk", "java.defaults entry " + defaultJavaVersion);
            if (StringUtils.isBlank(defaultJavaVersion.from())) {
                throw new IllegalStateException("java.defaults entry " + defaultJavaVersion + " must declare 'from'.");
            }
        });
        if (defaults.stream().map(DefaultJavaVersion::from).distinct().count() != defaults.size()) {
            throw new IllegalStateException("java.defaults contains duplicated 'from' runtime versions.");
        }
    }

    private void validateSwitchRestrictions() {
        switchRestrictions.forEach(restriction -> {
            String description = "java.switch-restrictions entry for Java " + restriction.jdk();
            validateJdkIsDeclared(restriction.jdk(), "jdk", description);
            if (StringUtils.isBlank(restriction.reason())) {
                throw new IllegalStateException(description + " must declare 'reason' so the restriction stays explainable.");
            }
            SupportedJavaVersion restrictedVersion = versions.stream()
                    .filter(supportedJavaVersion -> supportedJavaVersion.jdk() == restriction.jdk())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(description + " has no matching java.versions entry."));
            if (StringUtils.isNotBlank(restriction.until()) && !restrictedVersion.covers(restriction.until())) {
                throw new IllegalStateException(description + " has 'until' outside the runtime range of " + restrictedVersion + ".");
            }
        });
        if (switchRestrictions.stream().map(JavaVersionSwitchRestriction::jdk).distinct().count() != switchRestrictions.size()) {
            throw new IllegalStateException("java.switch-restrictions contains more than one entry for the same Java version: " + switchRestrictions);
        }
    }

    private void validateAutoUpgrades() {
        autoUpgrades.forEach(autoUpgrade -> {
            validateJdkIsDeclared(autoUpgrade.whenJdk(), "whenJdk", "java.auto-upgrades entry " + autoUpgrade);
            validateJdkIsDeclared(autoUpgrade.setJdk(), "setJdk", "java.auto-upgrades entry " + autoUpgrade);
            validateRange(autoUpgrade, "java.auto-upgrades entry " + autoUpgrade, "targetFrom", "targetUntil");
            if (autoUpgrade.whenJdk() == autoUpgrade.setJdk()) {
                throw new IllegalStateException("java.auto-upgrades entry " + autoUpgrade + " moves Java " + autoUpgrade.setJdk() + " to itself.");
            }
        });
        for (JavaVersionAutoUpgrade autoUpgrade : autoUpgrades) {
            autoUpgrades.stream()
                    .filter(otherAutoUpgrade -> otherAutoUpgrade != autoUpgrade && otherAutoUpgrade.whenJdk() == autoUpgrade.whenJdk())
                    .filter(otherAutoUpgrade -> overlap(autoUpgrade, otherAutoUpgrade))
                    .findFirst()
                    .ifPresent(otherAutoUpgrade -> {
                        throw new IllegalStateException("java.auto-upgrades entries " + autoUpgrade + " and " + otherAutoUpgrade
                                + " overlap, so it is ambiguous which one applies.");
                    });
        }
    }

    private boolean overlap(RuntimeVersionRange range, RuntimeVersionRange otherRange) {
        boolean rangeStartsBeforeOtherEnds = StringUtils.isBlank(otherRange.until()) || compareVersions(range.from(), otherRange.until()) < 0;
        boolean otherStartsBeforeRangeEnds = StringUtils.isBlank(range.until()) || compareVersions(otherRange.from(), range.until()) < 0;
        return rangeStartsBeforeOtherEnds && otherStartsBeforeRangeEnds;
    }

    private void validateEveryBoundaryPoint() {
        String oldestDefaultFrom = defaults.stream().map(DefaultJavaVersion::from).min(VERSION_ORDER).orElseThrow();
        for (String runtimeVersion : boundaryPoints()) {
            if (compareVersions(runtimeVersion, oldestDefaultFrom) >= 0 && anyVersionRunsOnOrAfter(runtimeVersion)) {
                validateDefaultIsSupported(runtimeVersion);
            }
            validateAutoUpgradeTargetsAreSwitchable(runtimeVersion);
        }
    }

    private void validateDefaultIsSupported(String runtimeVersion) {
        int defaultVersion = defaultVersion(runtimeVersion);
        SortedSet<Integer> supportedVersions = supportedVersions(runtimeVersion);
        if (!supportedVersions.contains(defaultVersion)) {
            throw new IllegalStateException(String.format("java.defaults gives Java %d on runtime %s, which is not supported there. "
                    + "Supported versions: %s.", defaultVersion, runtimeVersion, supportedVersions));
        }
    }

    private void validateAutoUpgradeTargetsAreSwitchable(String runtimeVersion) {
        SortedSet<Integer> switchableVersions = switchableVersions(runtimeVersion);
        autoUpgrades.stream()
                .filter(autoUpgrade -> autoUpgrade.covers(runtimeVersion) && !switchableVersions.contains(autoUpgrade.setJdk()))
                .findFirst()
                .ifPresent(autoUpgrade -> {
                    throw new IllegalStateException(String.format("java.auto-upgrades entry %s targets Java %d which is not switchable on runtime %s. "
                            + "Switchable versions: %s.", autoUpgrade, autoUpgrade.setJdk(), runtimeVersion, switchableVersions));
                });
    }

    private boolean anyVersionRunsOnOrAfter(String runtimeVersion) {
        return versions.stream().anyMatch(supportedJavaVersion -> StringUtils.isBlank(supportedJavaVersion.until())
                || compareVersions(supportedJavaVersion.until(), runtimeVersion) > 0);
    }

    private SortedSet<String> boundaryPoints() {
        Stream<String> versionBoundaries = versions.stream()
                .flatMap(supportedJavaVersion -> Stream.of(supportedJavaVersion.from(), supportedJavaVersion.until()));
        Stream<String> defaultBoundaries = defaults.stream().map(DefaultJavaVersion::from);
        Stream<String> restrictionBoundaries = switchRestrictions.stream().map(JavaVersionSwitchRestriction::until);
        Stream<String> autoUpgradeBoundaries = autoUpgrades.stream()
                .flatMap(autoUpgrade -> Stream.of(autoUpgrade.targetFrom(), autoUpgrade.targetUntil()));
        return Stream.of(versionBoundaries, defaultBoundaries, restrictionBoundaries, autoUpgradeBoundaries)
                .flatMap(boundaries -> boundaries)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(() -> new TreeSet<>(VERSION_ORDER)));
    }

    private void validateJdkIsDeclared(int jdk, String field, String description) {
        if (jdk <= 0) {
            throw new IllegalStateException(String.format("%s must declare '%s'.", description, field));
        }
    }

    private void validateRange(RuntimeVersionRange range, String description, String fromField, String untilField) {
        if (StringUtils.isBlank(range.from())) {
            throw new IllegalStateException(String.format("%s must declare '%s'.", description, fromField));
        }
        if (StringUtils.isNotBlank(range.until()) && compareVersions(range.from(), range.until()) >= 0) {
            throw new IllegalStateException(String.format("%s has '%s' greater than or equal to '%s'.", description, fromField, untilField));
        }
    }
}
