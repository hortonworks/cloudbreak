package com.sequenceiq.periscope.model.yarn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.periscope.model.yarn.YarnScalingServiceV1Request.HostGroupInstanceType;

@JsonIgnoreProperties(ignoreUnknown = true)
public class YarnScalingServiceV1Response {

    private static final String YARN_RESPONSE_DECOMMISSION_CANDIDATES_KEY = "candidates";

    private String apiVersion;

    private String consideredResourceTypes;

    @JsonIgnore
    private Map<String, List<HostGroupInstanceType>> nodeInstanceTypeList;

    private NewNodeManagerCandidates newNMCandidates;

    private Map<String, List<DecommissionCandidate>> decommissionCandidates;

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getConsideredResourceTypes() {
        return consideredResourceTypes;
    }

    public void setConsideredResourceTypes(String consideredResourceTypes) {
        this.consideredResourceTypes = consideredResourceTypes;
    }

    public NewNodeManagerCandidates getNewNMCandidates() {
        return newNMCandidates;
    }

    public void setNewNMCandidates(NewNodeManagerCandidates newNMCandidates) {
        this.newNMCandidates = newNMCandidates;
    }

    public Map<String, List<DecommissionCandidate>> getDecommissionCandidates() {
        return decommissionCandidates == null ? Map.of() : decommissionCandidates;
    }

    public void setDecommissionCandidates(Map<String, List<DecommissionCandidate>> decommissionCandidates) {
        this.decommissionCandidates = decommissionCandidates;
    }

    public Map<String, List<HostGroupInstanceType>> getNodeInstanceTypeList() {
        return nodeInstanceTypeList == null ? Map.of() : nodeInstanceTypeList;
    }

    public void setNodeInstanceTypeList(Map<String, List<HostGroupInstanceType>> nodeInstanceTypeList) {
        this.nodeInstanceTypeList = nodeInstanceTypeList;
    }

    public Optional<NewNodeManagerCandidates> getScaleUpCandidates() {
        return Optional.ofNullable(getNewNMCandidates());
    }

    public Optional<List<DecommissionCandidate>> getScaleDownCandidates() {
        return Optional.ofNullable(getDecommissionCandidates().get(YARN_RESPONSE_DECOMMISSION_CANDIDATES_KEY));
    }

    @Override
    public String toString() {
        return "YarnScalingServiceV1Response{" +
                "apiVersion='" + apiVersion + '\'' +
                ", newNMCandidates=" + newNMCandidates +
                ", decommissionCandidates=" + decommissionCandidates +
                '}';
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DecommissionCandidate {

        private Integer amCount;

        private Integer runningAppCount;

        private Integer decommissionTimeout;

        private String nodeId;

        private String nodeState;

        public Integer getAmCount() {
            return amCount;
        }

        public void setAmCount(Integer amCount) {
            this.amCount = amCount;
        }

        public Integer getRunningAppCount() {
            return runningAppCount;
        }

        public void setRunningAppCount(Integer runningAppCount) {
            this.runningAppCount = runningAppCount;
        }

        public Integer getDecommissionTimeout() {
            return decommissionTimeout;
        }

        public void setDecommissionTimeout(Integer decommissionTimeout) {
            this.decommissionTimeout = decommissionTimeout;
        }

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        public String getNodeState() {
            return nodeState;
        }

        public void setNodeState(String nodeState) {
            this.nodeState = nodeState;
        }

        @Override
        public String toString() {
            return "DecommissionCandidate{" +
                    "amCount=" + amCount +
                    ", runningAppCount=" + runningAppCount +
                    ", decommissionTimeout=" + decommissionTimeout +
                    ", nodeId='" + nodeId + '\'' +
                    ", nodeState='" + nodeState + '\'' +
                    '}';
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NewNodeManagerCandidates {

        private List<Candidate> candidates = new ArrayList<>(1);

        public List<Candidate> getCandidates() {
            return candidates;
        }

        public void setCandidates(List<Candidate> candidates) {
            this.candidates = candidates;
        }

        @Override
        public String toString() {
            return "NewNodeManagerCandidates{" +
                    "Candidates=" + candidates +
                    '}';
        }

        public static class Candidate {
            private String modelName;

            private Integer count;

            public String getModelName() {
                return modelName;
            }

            public void setModelName(String modelName) {
                this.modelName = modelName;
            }

            public Integer getCount() {
                return count;
            }

            public void setCount(Integer count) {
                this.count = count;
            }

            @Override
            public String toString() {
                return "Candidate{" +
                        "modelName='" + modelName + '\'' +
                        ", count=" + count +
                        '}';
            }
        }
    }
}

