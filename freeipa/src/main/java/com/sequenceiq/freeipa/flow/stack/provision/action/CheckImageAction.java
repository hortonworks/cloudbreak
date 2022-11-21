package com.sequenceiq.freeipa.flow.stack.provision.action;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.setup.CheckImageResult;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.provision.PrepareImageResultToStackEventConverter;
import com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent;

@Component("CheckImageAction")
public class CheckImageAction extends AbstractStackProvisionAction<StackEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckImageAction.class);

    private static final int REPEAT_TIME = 5000;

    private static final int FAULT_TOLERANCE = 5;

    private static final String IMAGE_COPY_FAULT_NUM = "IMAGE_COPY_FAULT_NUM";

    @Inject
    private StackProvisionService stackCreationService;

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
        return StackProvisionEvent.IMAGE_COPY_FINISHED_EVENT;
    }

    @Override
    protected void initPayloadConverterMap(List<PayloadConverter<StackEvent>> payloadConverters) {
        payloadConverters.add(new PrepareImageResultToStackEventConverter());
    }

    private void repeat(StackContext context) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    sendEvent(context, new StackEvent(getRepeatEvent().event(),
                            context.getStack().getId()));
                } catch (Exception e) {
                    LOGGER.error("{}", e.getMessage(), e);
                }
            }
        }, REPEAT_TIME);
    }

    protected FlowEvent getRepeatEvent() {
        return StackProvisionEvent.IMAGE_COPY_CHECK_EVENT;
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
