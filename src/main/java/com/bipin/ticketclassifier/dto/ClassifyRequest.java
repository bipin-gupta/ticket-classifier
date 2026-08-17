package com.bipin.ticketclassifier.dto;

// The JSON body sent to POST /classify, e.g. { "text": "my payment failed twice" }
public record ClassifyRequest(String text) {
}
