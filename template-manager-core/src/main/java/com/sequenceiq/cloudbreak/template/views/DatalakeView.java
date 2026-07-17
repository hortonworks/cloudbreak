package com.sequenceiq.cloudbreak.template.views;

import java.util.Map;
import java.util.Objects;

import com.sequenceiq.cloudbreak.sdx.RdcView;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;

public class DatalakeView {

    private final boolean razEnabled;

    private final String razAuthenticationType;

    private final Map<String, String> userMappings;

    private final String crn;

    private final DatabaseType databaseType;

    private final RdcView rdcView;

    public DatalakeView(boolean razEnabled, String razAuthenticationType, Map<String, String> userMappings, String crn, boolean externalDb) {
        this(razEnabled, razAuthenticationType, userMappings, crn, externalDb, null);
    }

    public DatalakeView(boolean razEnabled, String razAuthenticationType, Map<String, String> userMappings, String crn, boolean externalDb, RdcView rdcView) {
        this.razEnabled = razEnabled;
        this.razAuthenticationType = razAuthenticationType;
        this.userMappings = userMappings;
        this.crn = crn;
        this.databaseType = externalDb ? DatabaseType.EXTERNAL_DATABASE : DatabaseType.EMBEDDED_DATABASE;
        this.rdcView = rdcView;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DatalakeView that = (DatalakeView) o;

        return crn.equals(that.crn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(crn);
    }

    public boolean isRazEnabled() {
        return razEnabled;
    }

    public String getRazAuthenticationType() {
        return razAuthenticationType;
    }

    public Map<String, String> getUserMappings() {
        return userMappings;
    }

    public TargetPlatform getTargetPlatform() {
        return TargetPlatform.getByCrn(crn);
    }

    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    public RdcView getRdcView() {
        return rdcView;
    }
}
