package com.sequenceiq.freeipa.service.freeipa.user.model;

import java.util.Objects;

public class FmsUser {

    private String name;

    private String firstName;

    private String lastName;

    private WorkloadCredential creds;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public WorkloadCredential getCreds() {
        return creds;
    }

    public void setCreds(WorkloadCredential creds) {
        this.creds = creds;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FmsUser other = (FmsUser) o;
        return Objects.equals(this.name, other.name)
                && Objects.equals(this.firstName, other.firstName)
                && Objects.equals(this.lastName, other.lastName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, firstName, lastName);
    }

    @Override
    public String toString() {
        return "FmsUser{"
                + "name='" + name + '\''
                + ", firstName='" + firstName + '\''
                + ", lastName='" + lastName + '\''
                + '}';
    }
}
