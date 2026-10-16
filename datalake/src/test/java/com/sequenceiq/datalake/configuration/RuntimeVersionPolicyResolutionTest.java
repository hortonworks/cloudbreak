package com.sequenceiq.datalake.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.spring.YamlPropertySourceFactory;

/**
 * Guards the precedence ladder the shared runtime-version policy relies on:
 * <b>environment variable &gt; module {@code application.yml} &gt; the canonical {@code cb.runtimes} block in
 * {@code service-common-config.yml}</b> loaded by {@code ServiceCommonConfig}'s {@code @PropertySource}.
 *
 * <p>The canonical file is the single place a new runtime is listed, but the {@code datalake.runtimes.*} keys stay
 * in {@code application.yml} as placeholder chains onto it, because deployed regions override them through
 * {@code DATALAKE_RUNTIMES_*} environment variables (gov narrows the supported list; stage and prod blank the
 * default to get the image-catalog fallback). Break the chaining or the ordering and those overrides are silently
 * ignored - which is exactly the regression this test exists to catch, so it asserts resolution against the real
 * two files rather than fixtures.</p>
 *
 * <p>TODO remove this class together with the {@code datalake.runtimes.*} keys once deployments stop setting
 * {@code DATALAKE_RUNTIMES_*} and read {@code CB_RUNTIMES_*} only; see the TODO on each test for what goes and
 * what moves.</p>
 */
class RuntimeVersionPolicyResolutionTest {

    // TODO remove with the datalake.runtimes.* keys: the chaining it checks no longer exists without them.
    @Test
    void datalakeKeysChainOntoTheCanonicalPolicySoAVersionIsListedOnlyOnce() throws IOException {
        PropertySourcesPropertyResolver resolver = resolver();

        assertThat(resolver.getProperty("cb.runtimes.supported")).isNotBlank();
        assertThat(resolver.getProperty("datalake.runtimes.supported")).isEqualTo(resolver.getProperty("cb.runtimes.supported"));
        assertThat(resolver.getProperty("datalake.runtimes.default")).isEqualTo(resolver.getProperty("cb.runtimes.default"));
    }

    // TODO move to service-common next to RuntimeVersionsTest when the rest of this class is removed; it checks
    // service-common-config.yml only, not datalake.
    @Test
    void theCanonicalPolicyPinsLatestExplicitlyBecauseItIsNeverDerived() throws IOException {
        // A blank latest means "fall back to the default image catalog" (stage and prod rely on that), so the
        // shipped policy has to name the version rather than leave it to be derived from the supported list.
        assertThat(resolver().getProperty("cb.runtimes.latest")).isNotBlank();
    }

    // TODO remove with the datalake.runtimes.* keys: DATALAKE_RUNTIMES_* overrides go away with them.
    @Test
    void anEnvironmentVariableStillOverridesTheChainedDatalakeKeys() throws IOException {
        String canonicalSupported = resolver().getProperty("cb.runtimes.supported");
        PropertySourcesPropertyResolver resolver = resolver(ImmutableMap.of(
                "datalake.runtimes.supported", "7.2.18,7.3.2",
                "datalake.runtimes.default", ""));

        assertThat(resolver.getProperty("datalake.runtimes.supported")).isEqualTo("7.2.18,7.3.2");
        assertThat(resolver.getProperty("datalake.runtimes.default")).isEmpty();
        assertThat(resolver.getProperty("cb.runtimes.supported")).isEqualTo(canonicalSupported);
    }

    // TODO remove with the datalake.runtimes.* keys: without application.yml in the chain this only checks that an
    // environment variable beats a @PropertySource, which Spring guarantees.
    @Test
    void anEnvironmentVariableStillOverridesTheCanonicalPolicyDirectly() throws IOException {
        // CB_RUNTIMES_LATEST="" is how stage and prod ask for the image-catalog fallback; it has to survive the
        // canonical file shipping a concrete value.
        PropertySourcesPropertyResolver resolver = resolver(ImmutableMap.of("cb.runtimes.latest", ""));

        assertThat(resolver.getProperty("cb.runtimes.latest")).isEmpty();
    }

    private PropertySourcesPropertyResolver resolver() throws IOException {
        return resolver(ImmutableMap.of());
    }

    private PropertySourcesPropertyResolver resolver(ImmutableMap<String, Object> environmentOverrides) throws IOException {
        MutablePropertySources propertySources = new MutablePropertySources();
        // Lowest precedence first, so every addFirst below layers on top of it.
        propertySources.addFirst(yaml("service-common-config.yml"));
        propertySources.addFirst(yaml("application.yml"));
        propertySources.addFirst(new MapPropertySource("environment", environmentOverrides));
        return new PropertySourcesPropertyResolver(propertySources);
    }

    private PropertySource<?> yaml(String classpathFile) throws IOException {
        return new YamlPropertySourceFactory().createPropertySource(classpathFile, new EncodedResource(new ClassPathResource(classpathFile)));
    }
}
