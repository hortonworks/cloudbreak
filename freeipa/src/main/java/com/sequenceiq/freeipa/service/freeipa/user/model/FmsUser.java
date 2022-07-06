package com.sequenceiq.freeipa.service.freeipa.user.model;

import java.util.Objects;

public class FmsUser {

    public enum State {
        ENABLED,
        DISABLED
    }

    private String name;

    private String firstName;

    private String lastName;

    private State state;

    private String crn;

    public String getName() {
        return name;
    }

    public FmsUser withName(String name) {
        this.name = name;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public FmsUser withFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public FmsUser withLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public State getState() {
        return state;
    }

    public FmsUser withState(State state) {
        this.state = state;
        return this;
    }

    public String getCrn() {
        return crn;
    }

    public FmsUser withCrn(String crn) {
        this.crn = crn;
        return this;
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
                && Objects.equals(this.lastName, other.lastName)
                && Objects.equals(this.state, other.state)
                && Objects.equals(this.crn, other.crn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, firstName, lastName, state, crn);
    }

    @Override
    public String toString() {
        return "FmsUser{"
                + "name='" + name + '\''
                + ", firstName='" + firstName + '\''
                + ", lastName='" + lastName + '\''
                + ", state=" + state
                + ", crn='" + crn + '\''
                + '}';
    }
}
