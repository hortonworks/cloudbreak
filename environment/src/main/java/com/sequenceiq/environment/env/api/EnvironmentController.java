package com.sequenceiq.environment.env.api;

import org.springframework.stereotype.Controller;

import com.sequenceiq.environment.api.WelcomeResponse;

@Controller
public class EnvironmentController implements EnvironmentEndpoint {

    @Override
    public WelcomeResponse welcome() {
        return new WelcomeResponse("Welcome to the Environment service");
    }

}
