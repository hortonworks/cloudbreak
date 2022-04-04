package com.sequenceiq.freeipa.client.model;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sequenceiq.freeipa.client.deserializer.ListFlatteningDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SudoCommand {

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private String sudocmd;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private String description;

    public String getSudocmd() {
        return sudocmd;
    }

    public void setSudocmd(String sudocmd) {
        this.sudocmd = sudocmd;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SudoCommand.class.getSimpleName() + "[", "]")
                .add("sudocmd='" + sudocmd + "'")
                .add("description='" + description + "'")
                .toString();
    }
}
