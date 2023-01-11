package com.sequenceiq.cloudbreak.quartz;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Optional;

import javax.annotation.Nullable;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;

public abstract class MdcQuartzJob extends QuartzJobBean {

    private static final Logger LOGGER = getLogger(MdcQuartzJob.class);

    @Override
    protected final void executeInternal(JobExecutionContext context) throws JobExecutionException {
        fillMdcContext(context);
        try {
            executeTracedJob(context);
        } finally {
            MDCBuilder.cleanupMdc();
        }
    }

    protected void fillMdcContext(JobExecutionContext context) {
        Optional<MdcContextInfoProvider> mdcContextConfigProvider = getMdcContextConfigProvider();
        if (mdcContextConfigProvider == null) {
            LOGGER.debug("getMdcContextConfigProvider does not implemented, try the old one");
            Optional<Object> mdcContextObject = getMdcContextObject();
            if (mdcContextObject == null) {
                throw new IllegalArgumentException("Please implement one of them: getMdcContextObject() or getMdcContextConfigProvider()");
            } else {
                MDCBuilder.buildMdcContext(mdcContextObject.orElse(null));
            }
        } else {
            MDCBuilder.buildMdcContextFromInfoProvider(mdcContextConfigProvider.orElse(null));
        }
        String requestId = MDCBuilder.getOrGenerateRequestId();
        context.put(LoggerContextKey.REQUEST_ID.toString(), requestId);
    }

    /**
     * @deprecated use {@link MdcQuartzJob#getMdcContextConfigProvider()} instead for type-safety
     */
    @Nullable
    @Deprecated
    protected Optional<Object> getMdcContextObject() {
        return null;
    }

    @Nullable
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return null;
    }

    protected abstract void executeTracedJob(JobExecutionContext context) throws JobExecutionException;
}
