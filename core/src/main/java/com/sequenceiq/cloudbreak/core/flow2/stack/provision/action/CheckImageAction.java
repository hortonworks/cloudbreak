package com.sequenceiq.cloudbreak.core.flow2.stack.provision.action;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.setup.CheckImageResult;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.PrepareImageResultToStackEventConverter;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.service.StackCreationService;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.PayloadConverter;

import reactor.fn.timer.Timer;

@Component("CheckImageAction")
public class CheckImageAction extends AbstractStackCreationAction<StackEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckImageAction.class);

    private static final int REPEAT_TIME = 30000;

    private static final int FAULT_TOLERANCE = 5;

    private static final String IMAGE_COPY_FAULT_NUM = "IMAGE_COPY_FAULT_NUM";

    @Inject
    private StackCreationService stackCreationService;

    @Inject
    private Timer timer;

    public CheckImageAction() {
        super(StackEvent.class);
    }

    @Override
    protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
        CheckImageResult checkImageResult = stackCreationService.checkImage(context);
        switch (checkImageResult.getImageStatus()) {
            case IN_PROGRESS:
                setFaultNum(variables, 0);
                repeat(context);
                break;
            case CREATE_FINISHED:
                sendEvent(context);
                break;
            case CREATE_FAILED:
                LOGGER.info("Error during image status check: {}", payload);
                int faultNum = getFaultNum(variables) + 1;
                if (faultNum == FAULT_TOLERANCE) {
                    removeFaultNum(variables);
                    throw new CloudbreakServiceException("Image copy failed.");
                } else {
                    setFaultNum(variables, faultNum);
                    repeat(context);
                }
                break;
            default:
                LOGGER.error("Unknown image status: {}", checkImageResult.getImageStatus());
                break;
        }
    }

    @Override
    protected Selectable createRequest(StackContext context) {
        return new StackEvent(getFinishedEvent().event(), context.getStack().getId());
    }

    protected FlowEvent getFinishedEvent() {
        return StackCreationEvent.IMAGE_COPY_FINISHED_EVENT;
    }

    @Override
    protected void initPayloadConverterMap(List<PayloadConverter<StackEvent>> payloadConverters) {
        payloadConverters.add(new PrepareImageResultToStackEventConverter());
    }

    private void repeat(StackContext context) {
        timer.submit(aLong -> sendEvent(context, new StackEvent(getRepeatEvent().event(),
                context.getStack().getId())), REPEAT_TIME, TimeUnit.MILLISECONDS);
    }

    protected FlowEvent getRepeatEvent() {
        return StackCreationEvent.IMAGE_COPY_CHECK_EVENT;
    }

    private int getFaultNum(Map<Object, Object> variables) {
        Integer faultNum = (Integer) variables.get(IMAGE_COPY_FAULT_NUM);
        return faultNum == null ? 0 : faultNum;
    }

    private void setFaultNum(Map<Object, Object> variables, int faultNum) {
        variables.put(IMAGE_COPY_FAULT_NUM, faultNum);
    }

    private void removeFaultNum(Map<Object, Object> variables) {
        variables.remove(IMAGE_COPY_FAULT_NUM);
    }
}
