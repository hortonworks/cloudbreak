package com.sequenceiq.freeipa.service.freeipa.user.model;

import java.util.Objects;

/*
 * This class encapsulates the metadata stored in the 'title' attribute of users and machine users sync'd
 * to the FreeIPA. It contains the same information as JsonUserMetadata, but in an easily accessible format.
 */
public class UserMetadata {

    private final String crn;

    private final long workloadCredentialsVersion;

    public UserMetadata(String crn, long workloadCredentialsVersion) {
        this.crn = crn;
        this.workloadCredentialsVersion = workloadCredentialsVersion;
    }

    public String getCrn() {
        return crn;
    }

    public long getWorkloadCredentialsVersion() {
        return workloadCredentialsVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UserMetadata that = (UserMetadata) o;
        return workloadCredentialsVersion == that.workloadCredentialsVersion && Objects.equals(crn, that.crn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(crn, workloadCredentialsVersion);
    }

    @Override
    public String toString() {
        return "UserMetadata{"
                + "crn='" + crn + '\''
                + ", workloadCredendtialsVersion='" + workloadCredentialsVersion + '\''
                + '}';
    }
}
