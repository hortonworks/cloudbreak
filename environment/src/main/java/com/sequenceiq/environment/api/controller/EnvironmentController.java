package com.sequenceiq.environment.api.controller;

import org.springframework.stereotype.Controller;

import com.sequenceiq.environment.api.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.model.WelcomeResponse;

@Controller
public class EnvironmentController implements EnvironmentEndpoint {

    @Override
    public WelcomeResponse welcome() {
        return new WelcomeResponse("Welcome to the Environment service");
    }

}
