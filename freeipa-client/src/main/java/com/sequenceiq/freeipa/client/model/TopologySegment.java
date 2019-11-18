package com.sequenceiq.freeipa.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sequenceiq.freeipa.client.deserializer.ListFlatteningDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TopologySegment {
    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private String cn;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    @JsonProperty(value = "iparepltoposegmentleftnode")
    private String leftNode;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    @JsonProperty(value = "iparepltoposegmentrightnode")
    private String rightNode;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    @JsonProperty(value = "iparepltoposegmentdirection")
    private String direction;

    public String getCn() {
        return cn;
    }

    public void setCn(String cn) {
        this.cn = cn;
    }

    public String getLeftNode() {
        return leftNode;
    }

    public void setLeftNode(String leftNode) {
        this.leftNode = leftNode;
    }

    public String getRightNode() {
        return rightNode;
    }

    public void setRightNode(String rightNode) {
        this.rightNode = rightNode;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    @Override
    public String toString() {
        return "TopologySegment{"
                + "cn='" + cn + '\''
                + ",leftNode='" + leftNode + '\''
                + ",rightNode='" + rightNode + '\''
                + ",direction='" + direction + '\''
                + '}';
    }
}
