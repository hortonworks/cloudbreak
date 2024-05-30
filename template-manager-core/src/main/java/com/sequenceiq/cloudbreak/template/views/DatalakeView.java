package com.sequenceiq.cloudbreak.template.views;

import java.util.Objects;

public class DatalakeView {

    private final boolean razEnabled;

    private final String crn;

    private final DatabaseType databaseType;

    public DatalakeView(boolean razEnabled, String crn, boolean externalDb) {
        this.razEnabled = razEnabled;
        this.crn = crn;
        this.databaseType = externalDb ? DatabaseType.EXTERNAL_DATABASE : DatabaseType.EMBEDDED_DATABASE;
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

    public String getCrn() {
        return crn;
    }

    public DatabaseType getDatabaseType() {
        return databaseType;
    }
}
