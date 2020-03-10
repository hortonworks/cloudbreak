package com.sequenceiq.periscope.model;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.google.common.annotations.VisibleForTesting;

public class NameOrCrn {

    private static final String NAME_MUST_BE_PROVIDED_EXCEPTION_MESSAGE = "Name must be provided.";

    private static final String CRN_MUST_BE_PROVIDED_EXCEPTION_MESSAGE = "Crn must be provided.";

    @VisibleForTesting
    final String name;

    @VisibleForTesting
    final String crn;

    private NameOrCrn(String name, String crn) {
        this.name = name;
        this.crn = crn;
    }

    public static NameOrCrn ofName(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException(NAME_MUST_BE_PROVIDED_EXCEPTION_MESSAGE);
        }
        return new NameOrCrn(name, null);
    }

    public static NameOrCrn ofCrn(String crn) {
        if (StringUtils.isEmpty(crn)) {
            throw new IllegalArgumentException(CRN_MUST_BE_PROVIDED_EXCEPTION_MESSAGE);
        }
        return new NameOrCrn(null, crn);
    }

    public String getName() {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Request to get name when crn was provided on " + this);
        }
        return name;
    }

    public String getCrn() {
        if (StringUtils.isEmpty(crn)) {
            throw new IllegalArgumentException("Request to get crn when name was provided on " + this);
        }
        return crn;
    }

    public boolean hasName() {
        return isNotEmpty(name);
    }

    public boolean hasCrn() {
        return isNotEmpty(crn);
    }

    @Override
    public String toString() {
        StringBuilder toString = new StringBuilder();
        toString.append("[NameOrCrn");
        if (isNotEmpty(name)) {
            toString.append(" of name: '");
            toString.append(name);
        } else {
            toString.append(" of crn: '");
            toString.append(crn);
        }
        toString.append("']");
        return toString.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NameOrCrn nameOrCrn = (NameOrCrn) o;
        return Objects.equals(name, nameOrCrn.name) &&
                Objects.equals(crn, nameOrCrn.crn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, crn);
    }
}
