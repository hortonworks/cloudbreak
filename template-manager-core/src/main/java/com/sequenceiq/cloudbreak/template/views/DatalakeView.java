package com.sequenceiq.cloudbreak.template.views;

import java.util.Objects;

public class DatalakeView {

    private boolean razEnabled;

    public DatalakeView(boolean razEnabled) {
        this.razEnabled = razEnabled;
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
        return razEnabled == that.razEnabled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(razEnabled);
    }

    public boolean isRazEnabled() {
        return razEnabled;
    }
}
