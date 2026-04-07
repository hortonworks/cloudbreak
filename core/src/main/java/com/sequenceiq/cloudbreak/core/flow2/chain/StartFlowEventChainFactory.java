package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.RESUME_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.RESUME_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.RESUME_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UNSET;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrenew.ClusterCertificateRenewEvent.CLUSTER_CERTIFICATE_REISSUE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.CLUSTER_START_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.start.config.ExternalDatabaseStartEvent.EXTERNAL_DATABASE_COMMENCE_START_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartEvent.STACK_START_EVENT;

import java.security.cert.CertificateException;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value;
import com.sequenceiq.cloudbreak.client.KeyStoreUtil;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartState;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartState;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterUseCaseAware;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class StartFlowEventChainFactory implements FlowEventChainFactory<StackEvent>, ClusterUseCaseAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartFlowEventChainFactory.class);

    private static final long THRESHOLD_TIME_FOR_CERT_RENEWAL_IN_MILLIS = 10L * 24 * 60 * 60 * 1000;

    private static final String CERTIFICATE_ENCODING = "X.509";

    @Inject
    private SecurityConfigService securityConfigService;

    @Override
    public String initEvent() {
        return FlowChainTriggers.FULL_START_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(StackEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new StackEvent(EXTERNAL_DATABASE_COMMENCE_START_EVENT.event(), event.getResourceId(), event.accepted()));
        flowEventChain.add(new StackEvent(STACK_START_EVENT.event(), event.getResourceId()));
        flowEventChain.add(new StackEvent(CLUSTER_START_EVENT.event(), event.getResourceId()));
        addPublicCertRenewalEventIfNeeded(event.getResourceId(), flowEventChain);
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    private void addPublicCertRenewalEventIfNeeded(Long resourceId, Queue<Selectable> flowEventChain) {
        try {
            Optional<SecurityConfig> securityConfig = securityConfigService.findOneByStackId(resourceId);
            if (securityConfig.isPresent()) {
                String publicCert = securityConfig.get().getUserFacingCert();
                if (!(publicCert == null || publicCert.isEmpty())) {
                    long publicCertExpiryDate = KeyStoreUtil.getCertificateExpiryDate(publicCert, CERTIFICATE_ENCODING);
                    long now = System.currentTimeMillis();
                    if (publicCertExpiryDate - now <= THRESHOLD_TIME_FOR_CERT_RENEWAL_IN_MILLIS) {
                        LOGGER.info("Public cert is older than 80 days so trying to update it for stack with ID {}", resourceId);
                        flowEventChain.add(new StackEvent(CLUSTER_CERTIFICATE_REISSUE_EVENT.event(), resourceId));
                    }
                }
            }
        } catch (CertificateException e) {
            LOGGER.error("Could not fetch the expiry date for the public certificate due to {}", e.getMessage(), e);
        } catch (NullPointerException e) {
            LOGGER.error("Null value encountered while trying fetch certificate expiry date", e);
        } catch (Exception e) {
            LOGGER.error("Unexpected error while fetching certificate expiry", e);
        }
    }

    @Override
    public Value getUseCaseForFlowState(Enum flowState) {
        if (StackStartState.INIT_STATE.equals(flowState)) {
            return RESUME_STARTED;
        } else if (ClusterStartState.CLUSTER_START_FINISHED_STATE.equals(flowState)) {
            return RESUME_FINISHED;
        } else if (flowState.toString().endsWith("FAILED_STATE")) {
            return RESUME_FAILED;
        } else {
            return UNSET;
        }
    }
}
