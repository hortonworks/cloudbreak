package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.GetConsoleOutputRequest;
import com.amazonaws.services.ec2.model.GetConsoleOutputResult;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component("awsConsoleOutputCheckerTask")
public class ConsoleOutputCheckerTask extends StackBasedStatusCheckerTask<ConsoleOutputContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleOutputCheckerTask.class);

    @Inject
    private AwsStackUtil awsStackUtil;

    @Override
    public boolean checkStatus(ConsoleOutputContext consoleOutputContext) {
        AmazonEC2Client amazonEC2Client = awsStackUtil.createEC2Client(consoleOutputContext.getStack());
        GetConsoleOutputResult result = amazonEC2Client.getConsoleOutput(new GetConsoleOutputRequest().withInstanceId(consoleOutputContext.getInstanceId()));
        if (result != null && result.getOutput() != null) {
            return true;
        }
        LOGGER.warn("Console output is not yet available.");
        return false;
    }

    @Override
    public void handleTimeout(ConsoleOutputContext consoleOutputContext) {
        LOGGER.error("Operation timed out: Couldn't get console output of gateway instance.");
    }

    @Override
    public String successMessage(ConsoleOutputContext consoleOutputContext) {
        return "Console output of instance successfully retrieved.";
    }
}
