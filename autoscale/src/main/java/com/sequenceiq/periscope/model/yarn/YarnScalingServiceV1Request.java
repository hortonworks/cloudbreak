package com.sequenceiq.periscope.model.yarn;

import java.util.List;

public class YarnScalingServiceV1Request {

    private List<HostGroupInstanceType> instanceTypes;

    public List<HostGroupInstanceType> getInstanceTypes() {
        return instanceTypes;
    }

    public void setInstanceTypes(List<HostGroupInstanceType> instanceTypes) {
        this.instanceTypes = instanceTypes;
    }

    public static class HostGroupInstanceType {
        private String modelName;

        private Capacity capacity;

        public HostGroupInstanceType() {
        }

        public HostGroupInstanceType(String modelName, Integer memMB, Integer vcore) {
            this.modelName = modelName;
            this.capacity = new Capacity(memMB, vcore);
        }

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }

        public Capacity getCapacity() {
            return capacity;
        }

        public void setCapacity(Capacity capacity) {
            this.capacity = capacity;
        }

        public static class Capacity {
            private Integer memMB;

            private Integer vcore;

            Capacity() {
            }

            Capacity(Integer memMB, Integer vcore) {
                this.memMB = memMB;
                this.vcore = vcore;
            }

            public Integer getMemMB() {
                return memMB;
            }

            public void setMemMB(Integer memMB) {
                this.memMB = memMB;
            }

            public Integer getVcore() {
                return vcore;
            }

            public void setVcore(Integer vcore) {
                this.vcore = vcore;
            }
        }
    }
}

