package com.sequenceiq.cloudbreak.controller.json;

public class InviteConfirmationRequest implements UpdateRequest {
    private String password;
    private String firstName;
    private String lastName;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    @Override
    public String toString() {
        return "InviteConfirmationRequest{" +
                "password= ******, firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                '}';
    }
}
