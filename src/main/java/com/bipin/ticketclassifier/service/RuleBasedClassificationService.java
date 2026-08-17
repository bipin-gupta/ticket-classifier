package com.bipin.ticketclassifier.service;

import com.bipin.ticketclassifier.dto.ClassifyResponse;
import org.springframework.stereotype.Service;

// Simple keyword-based implementation of ClassificationService.
// To switch to an AI model later, create a new @Service class that
// implements ClassificationService and remove the @Service here
// (Spring will then wire in the new implementation automatically).
@Service
public class RuleBasedClassificationService implements ClassificationService {

    private static final String[] BILLING_KEYWORDS =
            {"payment", "refund", "charge", "invoice", "billing"};

    private static final String[] TECHNICAL_KEYWORDS =
            {"error", "crash", "bug", "failed", "broken", "not working"};

    private static final String[] URGENT_KEYWORDS =
            {"urgent", "asap", "immediately", "critical", "down", "twice", "again"};

    @Override
    public ClassifyResponse classify(String text) {
        String lowerText = text == null ? "" : text.toLowerCase();

        String category = detectCategory(lowerText);
        String priority = detectPriority(lowerText, category);
        String suggestedReply = buildReply(category);

        return new ClassifyResponse(category, priority, suggestedReply);
    }

    private String detectCategory(String lowerText) {
        if (containsAny(lowerText, BILLING_KEYWORDS)) {
            return "Billing";
        }
        if (containsAny(lowerText, TECHNICAL_KEYWORDS)) {
            return "Technical";
        }
        return "General";
    }

    private String detectPriority(String lowerText, String category) {
        if (containsAny(lowerText, URGENT_KEYWORDS)) {
            return "High";
        }
        if (!category.equals("General")) {
            return "Medium";
        }
        return "Low";
    }

    private String buildReply(String category) {
        return switch (category) {
            case "Billing" -> "Thanks for reaching out. Our billing team will look into your "
                    + "payment issue and get back to you shortly.";
            case "Technical" -> "Sorry for the trouble. Our technical team has been notified "
                    + "and will investigate the issue.";
            default -> "Thanks for contacting us. Our support team will review your request "
                    + "and respond soon.";
        };
    }

    private boolean containsAny(String text, String[] keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
