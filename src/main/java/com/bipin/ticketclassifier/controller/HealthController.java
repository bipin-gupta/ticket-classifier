package com.bipin.ticketclassifier.controller;

import com.bipin.ticketclassifier.dto.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Exposes GET /health, used later as a Kubernetes liveness/readiness probe.
@RestController
public class HealthController {

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("UP");
    }
}
