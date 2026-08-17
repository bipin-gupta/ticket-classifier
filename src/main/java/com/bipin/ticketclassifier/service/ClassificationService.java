package com.bipin.ticketclassifier.service;

import com.bipin.ticketclassifier.dto.ClassifyResponse;

// Defines how ticket text gets classified. The controller only depends on
// this interface, so the rule-based logic below can later be swapped for a
// call to an AI model (a new class implementing this interface) without
// changing the controller at all.
public interface ClassificationService {

    ClassifyResponse classify(String text);
}
