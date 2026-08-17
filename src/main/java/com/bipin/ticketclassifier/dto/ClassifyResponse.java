package com.bipin.ticketclassifier.dto;

// The JSON returned by POST /classify
public record ClassifyResponse(String category, String priority, String suggestedReply) {
}
