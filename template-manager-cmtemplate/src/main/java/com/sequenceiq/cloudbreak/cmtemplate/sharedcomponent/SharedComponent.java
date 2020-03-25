package com.sequenceiq.cloudbreak.cmtemplate.sharedcomponent;

public class SharedComponent {

    private String componentName;

    private String serviceType;

    public SharedComponent() {

    }

    public SharedComponent(String serviceType, String componentName) {
        this.serviceType = serviceType;
        this.componentName = componentName;
    }

    public String getComponentName() {
        return componentName;
    }

    public String getServiceType() {
        return serviceType;
    }

    @Override
    public String toString() {
        return "SharedComponent{" +
                "componentName='" + componentName + '\'' +
                ", serviceType='" + serviceType + '\'' +
                '}';
    }
}
