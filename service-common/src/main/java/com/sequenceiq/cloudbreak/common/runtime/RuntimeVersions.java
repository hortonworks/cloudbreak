package com.sequenceiq.cloudbreak.common.runtime;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.util.VersionComparator;

/**
 * The single source of truth for which Cloudera Runtime versions this deployment knows about.
 *
 * <p>Backed by the canonical {@code cb.runtimes} block in {@code service-common-config.yml}, which both core and datalake
 * read, so introducing a new runtime is one edit instead of the same version list maintained per module.
 * {@link #patched()} is always derived - a version ships as an overlay precisely because it is supported and
 * newer than the base - and {@link #advertised()} defaults to the supported list. {@link #latest()} and
 * {@link #defaultVersion()} are never derived: both are promoted deliberately, and a deployment that leaves
 * either blank is asking for the dynamic image-catalog behaviour instead.</p>
 *
 * <p>Every value is read once at startup, matching the {@code @PostConstruct} caches that consume it; changing
 * the policy needs a restart.</p>
 */
@Component
public class RuntimeVersions {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeVersions.class);

    private static final VersionComparator VERSION_COMPARATOR = new VersionComparator();

    @Value("${cb.runtimes.base:}")
    private String base;

    @Value("${cb.runtimes.supported:}")
    private String supported;

    @Value("${cb.runtimes.default:}")
    private String defaultRuntime;

    @Value("${cb.runtimes.advertised:}")
    private String advertised;

    @Value("${cb.runtimes.latest:}")
    private String latest;

    @Value("${cb.runtimes.overlay.enabled:false}")
    private boolean overlayEnabled;

    /**
     * @return the frozen base runtime whose full templates live on disk, as configured by {@code cb.runtimes.base}
     */
    public String base() {
        return StringUtils.trimToEmpty(base);
    }

    /**
     * @return every runtime version this deployment supports, in configured order; empty means "do not filter"
     */
    public Set<String> supported() {
        return parse(supported);
    }

    /**
     * @return the versions to offer in the UI: the configured list, or {@link #supported()} when unset. Empty -
     *         whether because nothing is supported or because the value is explicitly emptied - means "do not filter"
     */
    public Set<String> advertised() {
        return isUnset(advertised) ? supported() : parse(advertised);
    }

    /**
     * @return the newest version to serve blueprints for, or blank to let the caller fall back to the default image
     *         catalog. Deliberately <em>not</em> derived from {@link #supported()}: a blank value is how a deployment
     *         asks for the dynamic image-catalog behaviour, and deriving would silently pin the deployments that rely on it
     */
    public String latest() {
        return isUnset(latest) ? "" : latest.trim();
    }

    /**
     * @return the runtime to offer by default, or blank to let the caller derive it from the default image catalog.
     *         Deliberately not derived: it is promoted only once a prewarmed production image exists
     */
    public String defaultVersion() {
        return isUnset(defaultRuntime) ? "" : defaultRuntime.trim();
    }

    /**
     * @return the supported versions newer than {@link #base()}, i.e. exactly those that ship as sparse overlays
     *         rather than as full on-disk template directories. Never configured directly - a version is an overlay
     *         precisely because it is supported and above the base
     */
    public Set<String> patched() {
        String baseVersion = base();
        return supported().stream()
                .filter(version -> VERSION_COMPARATOR.compare(versioned(version), versioned(baseVersion)) > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * @return whether the runtime base+overlay model is active; when false only full on-disk template directories load
     */
    public boolean overlayEnabled() {
        return overlayEnabled;
    }

    @PostConstruct
    void logResolvedPolicy() {
        LOGGER.info("Resolved runtime version policy: {}", this);
    }

    private static Set<String> parse(String value) {
        if (isUnset(value)) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static boolean isUnset(String value) {
        return StringUtils.isBlank(value);
    }

    private static Versioned versioned(String version) {
        return () -> version;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public void setSupported(String supported) {
        this.supported = supported;
    }

    public void setDefaultRuntime(String defaultRuntime) {
        this.defaultRuntime = defaultRuntime;
    }

    public void setAdvertised(String advertised) {
        this.advertised = advertised;
    }

    public void setLatest(String latest) {
        this.latest = latest;
    }

    public void setOverlayEnabled(boolean overlayEnabled) {
        this.overlayEnabled = overlayEnabled;
    }

    @Override
    public String toString() {
        return "RuntimeVersions{base=" + base() + ", supported=" + supported() + ", default=" + defaultVersion()
                + ", advertised=" + advertised() + ", latest=" + latest() + ", patched=" + patched()
                + ", overlayEnabled=" + overlayEnabled + '}';
    }
}
