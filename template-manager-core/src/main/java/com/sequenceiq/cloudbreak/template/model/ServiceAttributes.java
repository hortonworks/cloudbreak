package com.sequenceiq.cloudbreak.template.model;

import java.util.Map;
import java.util.Objects;

/**
 * Stores Attributes which may be associated with a service / component.
 * e.g. 'YARN Node Attributes' which need to be applied on a per node basis.
 */
public class ServiceAttributes {

    private final ServiceComponent serviceComponent;

    private final Map<String, String> attributes;

    /**
     *
     * @param serviceComponent the service component (e.g. YARN, NODEMANAGER)
     * @param attributes list of attributes. interpretation will typically be left up to the component.
     */
    public ServiceAttributes(ServiceComponent serviceComponent, Map<String, String> attributes) {
        this.serviceComponent = serviceComponent;
        this.attributes = attributes;
    }

    public ServiceComponent getServiceComponent() {
        return serviceComponent;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ServiceAttributes that = (ServiceAttributes) o;
        return Objects.equals(serviceComponent, that.serviceComponent) &&
                Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceComponent, attributes);
    }

    @Override
    public String toString() {
        return "ServiceAttributes{" +
                "serviceComponent=" + serviceComponent +
                ", attributes=" + attributes +
                '}';
    }
}