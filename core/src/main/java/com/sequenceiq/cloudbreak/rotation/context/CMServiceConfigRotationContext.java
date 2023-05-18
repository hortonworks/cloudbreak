package com.sequenceiq.cloudbreak.rotation.context;

import com.google.common.collect.Table;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;

public class CMServiceConfigRotationContext extends RotationContext {

    private final Table<String, String, String> cmServiceConfigTable;

    public CMServiceConfigRotationContext(String resourceCrn, Table<String, String, String> cmServiceConfigTable) {
        super(resourceCrn);
        this.cmServiceConfigTable = cmServiceConfigTable;
    }

    public Table<String, String, String> getCmServiceConfigTable() {
        return cmServiceConfigTable;
    }
}
