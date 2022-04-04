package com.sequenceiq.freeipa.client.model;

import java.util.List;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sequenceiq.freeipa.client.deserializer.ListFlatteningDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SudoRule {

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private String cn;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    @JsonProperty("hostcategory")
    private String hostCategory;

    @JsonProperty("memberallowcmd_sudocmd")
    private List<String> allowSudoCommands = List.of();

    @JsonProperty("memberdenycmd_sudocmd")
    private List<String> denySudoCommands = List.of();

    @JsonProperty("memberuser_group")
    private List<String> userGroups = List.of();

    public String getCn() {
        return cn;
    }

    public void setCn(String cn) {
        this.cn = cn;
    }

    public List<String> getAllowSudoCommands() {
        return allowSudoCommands;
    }

    public void setAllowSudoCommands(List<String> allowSudoCommands) {
        this.allowSudoCommands = allowSudoCommands;
    }

    public List<String> getDenySudoCommands() {
        return denySudoCommands;
    }

    public void setDenySudoCommands(List<String> denySudoCommands) {
        this.denySudoCommands = denySudoCommands;
    }

    public List<String> getUserGroups() {
        return userGroups;
    }

    public void setUserGroups(List<String> userGroups) {
        this.userGroups = userGroups;
    }

    public String getHostCategory() {
        return hostCategory;
    }

    public void setHostCategory(String hostCategory) {
        this.hostCategory = hostCategory;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SudoRule.class.getSimpleName() + "[", "]")
                .add("cn='" + cn + "'")
                .add("hostCategory='" + hostCategory + "'")
                .add("allowSudoCommands=" + allowSudoCommands)
                .add("denySudoCommands=" + denySudoCommands)
                .add("userGroups=" + userGroups)
                .toString();
    }
}
