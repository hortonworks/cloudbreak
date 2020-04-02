package com.sequenceiq.periscope.model.yarn;

public class HostGroupInstanceType {
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