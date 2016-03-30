package com.sequenceiq.cloudbreak.core.flow2.stack.provision.action;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.cloud.event.setup.CheckImageResult;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowStackEvent;
import com.sequenceiq.cloudbreak.core.flow2.PayloadConverter;
import com.sequenceiq.cloudbreak.core.flow2.stack.SelectableFlowStackEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.PrepareImageResultToFlowStackEventConverter;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent;

import reactor.fn.Consumer;
import reactor.fn.timer.Timer;

@Component("CheckImageAction")
public class CheckImageAction extends AbstractStackCreationAction<FlowStackEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckImageAction.class);
    private static final int REPEAT_TIME = 5000;
    private static final int FAULT_TOLERANCE = 5;
    private static final String IMAGE_COPY_FAULT_NUM = "IMAGE_COPY_FAULT_NUM";

    @Inject
    private StackCreationService stackCreationService;
    @Inject
    private Timer timer;

    public CheckImageAction() {
        super(FlowStackEvent.class);
    }

    @Override
    protected void doExecute(final StackContext context, FlowStackEvent payload, Map<Object, Object> variables) {
        CheckImageResult checkImageResult = stackCreationService.checkImage(context);
        switch (checkImageResult.getImageStatus()) {
            case IN_PROGRESS:
                repeat(context);
                break;
            case CREATE_FINISHED:
                sendEvent(context);
                break;
            case CREATE_FAILED:
                LOGGER.error("Error during image status check: {}", payload);
                int faultNum = getFaultNum(variables) + 1;
                if (faultNum == FAULT_TOLERANCE) {
                    removeFaultNum(variables);
                    sendEvent(context.getFlowId(), StackCreationEvent.IMAGE_COPY_FAILED_EVENT.stringRepresentation(), payload);
                } else {
                    setFaultNum(variables, faultNum);
                    repeat(context);
                }
                break;
            default:
                // TODO error handling
                LOGGER.error("Unknown imagestatus: {}", checkImageResult.getImageStatus());
                break;
        }
    }

    @Override
    protected Selectable createRequest(StackContext context) {
        return new SelectableFlowStackEvent(context.getStack().getId(), StackCreationEvent.IMAGE_COPY_FINISHED_EVENT.stringRepresentation());
    }

    @Override
    protected void initPayloadConverterMap(List<PayloadConverter<FlowStackEvent>> payloadConverters) {
        payloadConverters.add(new PrepareImageResultToFlowStackEventConverter());
    }

    @Override
    protected Long getStackId(FlowStackEvent payload) {
        return payload.getStackId();
    }

    private void repeat(final StackContext context) {
        timer.submit(new Consumer<Long>() {
            @Override
            public void accept(Long aLong) {
                sendEvent(context.getFlowId(), StackCreationEvent.IMAGE_COPY_CHECK_EVENT.stringRepresentation(),
                        new FlowStackEvent(context.getStack().getId()));
            }
        }, REPEAT_TIME, TimeUnit.MILLISECONDS);
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
