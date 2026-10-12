package com.sequenceiq.cloudbreak.common.runtime.overlay;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.spring.YamlPropertySourceFactory;
import com.sequenceiq.cloudbreak.util.VersionComparator;

/**
 * Guards the runtime base+overlay model for a template tree: every Cloudera Runtime newer than the configured base
 * ({@code cb.runtimes.base} in {@code service-common-config.yml}) must ship as a sparse overlay under
 * {@code runtime-overlays/<version>/}, never as a full on-disk directory under {@code <baseSubtree>/<version>/}.
 *
 * <p>Without this guard a developer could copy a whole template tree for a new version instead of authoring an
 * overlay, and {@link RuntimeOverlayResolver} would <em>silently</em> skip that version (its
 * {@code hasOnDiskDirectory} filter) rather than fail - so the version would quietly not be served as an overlay.
 * The frozen base and every version at or below it keep their full on-disk directories and are not touched here;
 * a whole file that genuinely must supersede a base template is authored as a {@code .replace} overlay.</p>
 *
 * <p>Each module that owns a base tree calls this from a small test of its own, so the scan runs against that
 * module's classpath.</p>
 */
public final class RuntimeOverlayEnforcementTestUtil {

    private static final String SERVICE_COMMON_CONFIG = "service-common-config.yml";

    private static final String BASE_PROPERTY = "cb.runtimes.base";

    private static final PathMatchingResourcePatternResolver RESOLVER = new PathMatchingResourcePatternResolver();

    private static final VersionComparator VERSION_COMPARATOR = new VersionComparator();

    private RuntimeOverlayEnforcementTestUtil() {
    }

    public static void testIfNoFullOnDiskDirectoryAboveBase(String baseSubtree, String baseFileSuffix) throws IOException {
        testIfNoFullOnDiskDirectoryAboveBase(configuredBaseVersion(), baseSubtree, baseFileSuffix);
    }

    static void testIfNoFullOnDiskDirectoryAboveBase(String baseVersion, String baseSubtree, String baseFileSuffix) throws IOException {
        Set<String> fullOnDiskVersionsAboveBase = onDiskVersionsAboveBase(baseVersion, baseSubtree, baseFileSuffix);

        assertTrue(fullOnDiskVersionsAboveBase.isEmpty(),
                () -> "Found full on-disk " + baseSubtree + " director(ies) for runtime version(s) newer than the base "
                        + baseVersion + ": " + fullOnDiskVersionsAboveBase
                        + ". A runtime newer than the base must ship as a sparse overlay under runtime-overlays/<version>/ "
                        + "(a .patch.json, .tombstone or whole-file addition, or a .replace" + baseFileSuffix
                        + " for a wholesale rewrite), not as a full copy of the tree - the overlay resolver silently "
                        + "skips any version it finds on disk, so a full copy would never be served.");
    }

    static String configuredBaseVersion() throws IOException {
        PropertySource<?> config = new YamlPropertySourceFactory()
                .createPropertySource(SERVICE_COMMON_CONFIG, new EncodedResource(new ClassPathResource(SERVICE_COMMON_CONFIG)));
        String baseVersion = StringUtils.trimToEmpty((String) config.getProperty(BASE_PROPERTY));
        assertTrue(StringUtils.isNotEmpty(baseVersion), () -> BASE_PROPERTY + " is not set in " + SERVICE_COMMON_CONFIG);
        return baseVersion;
    }

    private static Set<String> onDiskVersionsAboveBase(String baseVersion, String baseSubtree, String baseFileSuffix) throws IOException {
        Resource[] resources = RESOLVER.getResources("classpath*:" + baseSubtree + "/**/*" + baseFileSuffix);
        String marker = "/" + baseSubtree + "/";
        Set<String> versionsAboveBase = new TreeSet<>();
        for (Resource resource : resources) {
            String version = versionSegment(resource.getURL().getPath(), marker);
            if (version != null && newerThanBase(version, baseVersion)) {
                versionsAboveBase.add(version);
            }
        }
        return versionsAboveBase;
    }

    private static String versionSegment(String resourcePath, String marker) {
        int index = resourcePath.indexOf(marker);
        if (index < 0) {
            return null;
        }
        String tail = resourcePath.substring(index + marker.length());
        int slash = tail.indexOf('/');
        return slash < 0 ? tail : tail.substring(0, slash);
    }

    private static boolean newerThanBase(String version, String baseVersion) {
        // The glob is root-anchored at the base subtree, so the first segment is always a version directory; the
        // digit check is a defensive guard against any stray non-version path rather than an expected case.
        if (!version.matches("\\d.*")) {
            return false;
        }
        Versioned candidate = () -> version;
        Versioned base = () -> baseVersion;
        return VERSION_COMPARATOR.compare(candidate, base) > 0;
    }
}
