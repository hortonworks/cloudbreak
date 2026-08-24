package com.sequenceiq.cloudbreak.common.runtime.overlay;

/**
 * Shared constants for the runtime base+overlay model.
 *
 * <p>The base version is a single policy value shared by every overlay tree (duties, cluster templates and
 * blueprints): it is the frozen Cloudera Runtime whose full templates live on disk, on top of which newer
 * versions ship as sparse overlays. Keeping it in one place means promoting a new base is a single edit
 * rather than one per loader.</p>
 */
public final class RuntimeOverlayConstants {

    /**
     * The frozen base runtime version whose full templates live on disk; newer runtimes are overlays on top of it.
     */
    public static final String BASE_VERSION = "7.3.3";

    /**
     * Placeholder token authored into an overlay's version-carrying fields (a {@code name}, {@code description},
     * {@code blueprintName} or {@code cdhVersion}) in place of a concrete version number. The loader replaces every
     * occurrence with the actual target version when it materializes the overlay.
     *
     * <p>It exists so a developer adding a brand-new template under {@code runtime-overlays/<version>/} names it with
     * the version they are actually adding it for, rather than being forced to hand-author the base version and rely
     * on a prefix swap - which reads as if the file belonged to the base. The frozen base files themselves are not
     * touched: they keep their literal base version and the historical prefix-swap injection still applies to them.</p>
     */
    public static final String RUNTIME_VERSION_PLACEHOLDER = "__RUNTIME_VERSION__";

    private RuntimeOverlayConstants() {
    }
}
