package com.bipin.ticketclassifier.controller;

import com.bipin.ticketclassifier.dto.ClassifyRequest;
import com.bipin.ticketclassifier.dto.ClassifyResponse;
import com.bipin.ticketclassifier.service.ClassificationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// Exposes POST /classify. All classification logic lives in
// ClassificationService, so this controller stays unchanged even if that
// logic later switches from keyword rules to an AI model.
@RestController
public class ClassifyController {

    private final ClassificationService classificationService;

    public ClassifyController(ClassificationService classificationService) {
        this.classificationService = classificationService;
    }

    @PostMapping("/classify")
    public ClassifyResponse classify(@RequestBody ClassifyRequest request) {
        return classificationService.classify(request.text());
    }
}
