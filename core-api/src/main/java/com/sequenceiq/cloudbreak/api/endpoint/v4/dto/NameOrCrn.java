package com.sequenceiq.cloudbreak.api.endpoint.v4.dto;

import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import org.apache.commons.lang3.StringUtils;

import com.google.common.annotations.VisibleForTesting;

public class NameOrCrn {

    private static final String NULL_DTO_EXCEPTION_MESSAGE = "Name or crn should not be null.";

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

    public static class NameOrCrnReader {

        private final NameOrCrn nameOrCrn;

        private NameOrCrnReader(NameOrCrn nameOrCrn) {
            this.nameOrCrn = nameOrCrn;
        }

        public static NameOrCrnReader create(NameOrCrn nameOrCrn) {
            throwIfNull(nameOrCrn, () -> new IllegalArgumentException(NULL_DTO_EXCEPTION_MESSAGE));
            return new NameOrCrnReader(nameOrCrn);
        }

        public boolean hasName() {
            return isNotEmpty(nameOrCrn.name);
        }

        public boolean hasCrn() {
            return isNotEmpty(nameOrCrn.crn);
        }

        public String getName() {
            return nameOrCrn.name;
        }

        public String getCrn() {
            return nameOrCrn.crn;
        }
    }
}
