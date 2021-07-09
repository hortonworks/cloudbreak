package com.sequenceiq.flow.api.model.operation;

import java.util.List;
import java.util.Map;

import com.sequenceiq.flow.api.model.FlowProgressResponse;

public class OperationFlowsView {

    private final OperationType operationType;

    private final Map<String, FlowProgressResponse> flowTypeProgressMap;

    private final List<String> typeOrderList;

    private final boolean inMemory;

    private final Integer progressFromHistory;

    private final String operationId;

    private OperationFlowsView(Builder builder) {
        this.operationType = builder.operationType;
        this.flowTypeProgressMap = builder.flowTypeProgressMap;
        this.typeOrderList = builder.typeOrderList;
        this.inMemory = builder.inMemory;
        this.progressFromHistory = builder.progressFromHistory;
        this.operationId = builder.operationId;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public Map<String, FlowProgressResponse> getFlowTypeProgressMap() {
        return flowTypeProgressMap;
    }

    public List<String> getTypeOrderList() {
        return typeOrderList;
    }

    public Integer getProgressFromHistory() {
        return progressFromHistory;
    }

    public boolean isInMemory() {
        return inMemory;
    }

    public String getOperationId() {
        return operationId;
    }

    @Override
    public String toString() {
        return "OperationFlowsView{" +
                "operationType=" + operationType +
                ", flowTypeProgressMap=" + flowTypeProgressMap +
                ", typeOrderList=" + typeOrderList +
                ", inMemory=" + inMemory +
                ", progressFromHistory=" + progressFromHistory +
                ", operationId='" + operationId + '\'' +
                '}';
    }

    public static class Builder {

        private OperationType operationType;

        private Map<String, FlowProgressResponse> flowTypeProgressMap;

        private List<String> typeOrderList;

        private boolean inMemory;

        private Integer progressFromHistory;

        private String operationId;

        private Builder() {
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public OperationFlowsView build() {
            return new OperationFlowsView(this);
        }

        public Builder withOperationType(OperationType operationType) {
            this.operationType = operationType;
            return this;
        }

        public Builder withFlowTypeProgressMap(Map<String, FlowProgressResponse> flowTypeProgressMap) {
            this.flowTypeProgressMap = flowTypeProgressMap;
            return this;
        }

        public Builder withTypeOrderList(List<String> typeOrderList) {
            this.typeOrderList = typeOrderList;
            return this;
        }

        public Builder withInMemory(boolean inMemory) {
            this.inMemory = inMemory;
            return this;
        }

        public Builder withProgressFromHistory(Integer progressFromHistory) {
            this.progressFromHistory = progressFromHistory;
            return this;
        }

        public Builder withOperationId(String operationId) {
            this.operationId = operationId;
            return this;
        }
    }
}
