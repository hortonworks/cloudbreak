package com.sequenceiq.cloudbreak.template.views;

import java.util.Objects;

public class DatalakeView {

    private boolean razEnabled;

    private String crn;

    public DatalakeView(boolean razEnabled, String crn) {
        this.razEnabled = razEnabled;
        this.crn = crn;
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
}
