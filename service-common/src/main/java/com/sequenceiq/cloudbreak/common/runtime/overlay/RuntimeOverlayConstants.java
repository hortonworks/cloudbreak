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

    private RuntimeOverlayConstants() {
    }
}
