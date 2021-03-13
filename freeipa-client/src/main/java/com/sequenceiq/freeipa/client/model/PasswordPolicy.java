package com.sequenceiq.freeipa.client.model;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sequenceiq.freeipa.client.deserializer.ListFlatteningDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PasswordPolicy {

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private Integer krbpwdminlength;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private Integer krbpwdmindiffchars;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private Integer krbmaxpwdlife;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private Integer krbpwdmaxfailure;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private Integer krbpwdfailurecountinterval;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private Integer krbpwdlockoutduration;

    public Integer getKrbpwdminlength() {
        return krbpwdminlength;
    }

    public void setKrbpwdminlength(Integer krbpwdminlength) {
        this.krbpwdminlength = krbpwdminlength;
    }

    public Integer getKrbpwdmindiffchars() {
        return krbpwdmindiffchars;
    }

    public void setKrbpwdmindiffchars(Integer krbpwdmindiffchars) {
        this.krbpwdmindiffchars = krbpwdmindiffchars;
    }

    public Integer getKrbmaxpwdlife() {
        return krbmaxpwdlife;
    }

    public void setKrbmaxpwdlife(Integer krbmaxpwdlife) {
        this.krbmaxpwdlife = krbmaxpwdlife;
    }

    public Integer getKrbpwdmaxfailure() {
        return krbpwdmaxfailure;
    }

    public void setKrbpwdmaxfailure(Integer krbpwdmaxfailure) {
        this.krbpwdmaxfailure = krbpwdmaxfailure;
    }

    public Integer getKrbpwdfailurecountinterval() {
        return krbpwdfailurecountinterval;
    }

    public void setKrbpwdfailurecountinterval(Integer krbpwdfailurecountinterval) {
        this.krbpwdfailurecountinterval = krbpwdfailurecountinterval;
    }

    public Integer getKrbpwdlockoutduration() {
        return krbpwdlockoutduration;
    }

    public void setKrbpwdlockoutduration(Integer krbpwdlockoutduration) {
        this.krbpwdlockoutduration = krbpwdlockoutduration;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PasswordPolicy.class.getSimpleName() + "[", "]")
                .add("krbpwdminlength=" + krbpwdminlength)
                .add("krbpwdmindiffchars=" + krbpwdmindiffchars)
                .add("krbmaxpwdlife=" + krbmaxpwdlife)
                .add("krbpwdmaxfailure=" + krbpwdmaxfailure)
                .add("krbpwdfailurecountinterval=" + krbpwdfailurecountinterval)
                .add("krbpwdlockoutduration=" + krbpwdlockoutduration)
                .toString();
    }
}
