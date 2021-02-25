package com.sequenceiq.cloudbreak.converter.v4.node.status;

import java.util.List;
import java.util.stream.Collectors;

import com.sequenceiq.common.api.node.status.response.DatabusDetails;
import com.sequenceiq.common.api.node.status.response.EventDetails;
import com.sequenceiq.common.api.node.status.response.HealthStatus;
import com.sequenceiq.common.api.node.status.response.MeteringDetails;
import com.sequenceiq.common.api.node.status.response.NetworkDetails;
import com.sequenceiq.common.api.node.status.response.NodeStatusResponse;
import com.sequenceiq.common.api.node.status.response.NodeReport;
import com.sequenceiq.common.api.node.status.response.ServiceDetails;
import com.sequenceiq.node.health.client.model.HealthReport;
import com.sequenceiq.node.health.client.model.NodeHealth;
import com.sequenceiq.node.health.client.model.ServicesDetails;

public class HealthReportToNodeStatusResponseConverter {

    public NodeStatusResponse convert(HealthReport report) {
        NodeStatusResponse response = new NodeStatusResponse();
        List<NodeReport> nodes = report.getNodes()
                .stream()
                .map(this::nodeHealthToNodeMeteringReport)
                .collect(Collectors.toList());
        response.setNodes(nodes);
        response.setTimestamp(response.getTimestamp());
        return response;
    }

    private NodeReport nodeHealthToNodeMeteringReport(NodeHealth nodeHealth) {
        NodeReport report = new NodeReport();
        report.setHost(nodeHealth.getHost());
        report.setMeteringDetails(convertMeteringDetails(nodeHealth.getMeteringDetails()));
        report.setNetworkDetails(convertNetworkDetails(nodeHealth.getNetworkDetails()));
        report.setServiceDetails(convertServiceDetails(nodeHealth.getServicesDetails()));
        report.setResponseStatus(nodeHealth.getResponseStatus());
        return report;
    }

    private ServiceDetails convertServiceDetails(ServicesDetails servicesDetails) {
        ServiceDetails result = new ServiceDetails();
        result.setInfraServices(servicesDetails.getInfraServices());
        result.setFreeipaServices(servicesDetails.getFreeipaServices());
        return result;
    }

    private NetworkDetails convertNetworkDetails(com.sequenceiq.node.health.client.model.NetworkDetails networkDetails) {
        NetworkDetails result = new NetworkDetails();
        result.setCcmAccessible(convertHealthStatus(networkDetails.getCcmAccessible()));
        result.setClouderaComAccessible(convertHealthStatus(networkDetails.getClouderaComAccessible()));
        result.setDatabusAccessible(convertHealthStatus(networkDetails.getDatabusAccessible()));
        result.setCcmEnabled(networkDetails.getCcmEnabled());
        result.setHost(networkDetails.getHost());
        result.setDnsResolvers(networkDetails.getDnsResolvers());
        return result;
    }

    private HealthStatus convertHealthStatus(com.sequenceiq.node.health.client.model.HealthStatus healthStatus) {
        HealthStatus result;
        switch (healthStatus) {
            case OK:
                result = HealthStatus.OK;
                break;
            case NOK:
                result = HealthStatus.NOK;
                break;
            default:
                result = HealthStatus.UKNOWN;
                break;
        }
        return result;
    }

    private MeteringDetails convertMeteringDetails(com.sequenceiq.node.health.client.model.MeteringDetails meteringDetails) {
        MeteringDetails result = new MeteringDetails();
        result.setDatabusDetails(convertDatabusDetails(meteringDetails.getDatabusDetails()));
        result.setEventDetails(convertEventDetails(meteringDetails.getEventDetails()));
        return result;
    }

    private EventDetails convertEventDetails(com.sequenceiq.node.health.client.model.EventDetails eventDetails) {
        EventDetails result = new EventDetails();
        result.setMeteredResourceCrn(eventDetails.getMeteredResourceCrn());
        result.setServiceType(eventDetails.getServiceType());
        result.setMeteredResourceName(eventDetails.getMeteredResourceName());
        result.setServiceType(eventDetails.getServiceType());
        return result;
    }

    private DatabusDetails convertDatabusDetails(com.sequenceiq.node.health.client.model.DatabusDetails databusDetails) {
        DatabusDetails result = new DatabusDetails();
        result.setEndpoint(databusDetails.getEndpoint());
        result.setProxyUrl(databusDetails.getProxyUrl());
        result.setStream(databusDetails.getStream());
        return result;
    }
}
