package com.sequenceiq.cloudbreak.common.runtime.overlay;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.cloudbreak.common.json.patch.JsonPatchApplier;

/**
 * Version-agnostic materializer for the runtime base+overlay model. Reconstructs the full template set for a
 * target runtime version from a frozen <em>base</em> version, instead of shipping a full copy of every file
 * per version.
 *
 * <p>For every base file (unless tombstoned): apply the version's RFC 6902 patch (if any), then inject the
 * target version into the caller-supplied set of version-carrying fields. Injection is deliberately
 * field-targeted — it rewrites the pointed-at leaf only when it carries the base version as a leading
 * {@code "<baseVersion> "} prefix (for example {@code "7.3.3 - Data Engineering"}) or is exactly the bare
 * base version (for example a {@code cdhVersion} of {@code "7.3.3"}), never a blind string replacement that
 * could corrupt unrelated values such as parcel URLs or embedded component versions.</p>
 *
 * <p>The engine carries no knowledge of a particular template tree (duties, cluster templates, blueprints):
 * the caller passes the relative-path-keyed base map, the patch/tombstone set, and the JSON Pointers whose
 * value carries the version prefix (for example {@code /cluster/blueprintName} for duties, or {@code /name}
 * plus {@code /distroXTemplate/cluster/blueprintName} for cluster templates).</p>
 */
public final class RuntimeOverlayMaterializer {

    private RuntimeOverlayMaterializer() {
    }

    /**
     * Reconstructs the full template set for {@code targetVersion} from the {@code baseVersion} templates.
     *
     * @param baseVersion              the frozen base runtime version (for example {@code 7.3.3})
     * @param targetVersion            the runtime version being materialized (for example {@code 7.3.4})
     * @param baseTemplates            base templates keyed by version-relative path (for example {@code aws/light_duty.json})
     * @param patches                  RFC 6902 patch documents keyed by the same relative path; only genuinely changed files appear
     * @param tombstones               relative paths present in the base but dropped for this version
     * @param versionInjectionPointers JSON Pointers whose leaf value carries the {@code "<version> - "} prefix to rewrite
     * @return materialized templates keyed by relative path
     */
    public static Map<String, JsonNode> materialize(
            String baseVersion,
            String targetVersion,
            Map<String, JsonNode> baseTemplates,
            Map<String, JsonNode> patches,
            Set<String> tombstones,
            List<String> versionInjectionPointers) {

        Map<String, JsonNode> materialized = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> entry : baseTemplates.entrySet()) {
            String relativePath = entry.getKey();
            if (tombstones.contains(relativePath)) {
                continue;
            }
            JsonNode template = entry.getValue().deepCopy();
            JsonNode patch = patches.get(relativePath);
            if (patch != null) {
                template = JsonPatchApplier.apply(template, patch);
            }
            injectRuntimeVersion(template, baseVersion, targetVersion, versionInjectionPointers);
            materialized.put(relativePath, template);
        }
        return materialized;
    }

    private static void injectRuntimeVersion(JsonNode template, String baseVersion, String targetVersion, List<String> pointers) {
        String basePrefix = baseVersion + " ";
        for (String pointer : pointers) {
            JsonNode node = template.at(pointer);
            if (!node.isTextual()) {
                continue;
            }
            String value = node.asText();
            // Rewrite either a "<baseVersion> ..." prefix (e.g. a name/description) or a leaf that is exactly the
            // bare base version (e.g. cdhVersion "7.3.3"). Anything else - parcel URLs, component versions that merely
            // contain the base version - is left untouched.
            if (!value.startsWith(basePrefix) && !value.equals(baseVersion)) {
                continue;
            }
            int lastSlash = pointer.lastIndexOf('/');
            if (lastSlash < 0) {
                continue;
            }
            String parentPointer = pointer.substring(0, lastSlash);
            String leaf = decode(pointer.substring(lastSlash + 1));
            JsonNode parent = parentPointer.isEmpty() ? template : template.at(parentPointer);
            if (parent instanceof ObjectNode objectParent) {
                objectParent.put(leaf, targetVersion + value.substring(baseVersion.length()));
            }
        }
    }

    private static String decode(String token) {
        return token.replace("~1", "/").replace("~0", "~");
    }
}
