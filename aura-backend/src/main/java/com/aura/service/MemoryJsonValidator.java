package com.aura.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class MemoryJsonValidator {

    private static final Set<String> TOP_LEVEL_KEYS = Set.of(
            "user_prefs", "project_context", "decisions", "open_questions", "facts"
    );
    private static final Set<String> USER_PREF_KEYS = Set.of("language", "tone", "format", "other");
    private static final Set<String> PROJECT_CONTEXT_KEYS = Set.of("app_name", "stack", "current_goal", "notes");
    private static final Set<String> DECISION_KEYS = Set.of("date", "decision");

    private final ObjectMapper objectMapper;

    public boolean isValid(String json) {
        if (json == null || json.isBlank()) {
            return false;
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            return validateRoot(root);
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean validateRoot(JsonNode root) {
        if (!root.isObject()) {
            return false;
        }
        Iterator<String> fields = root.fieldNames();
        while (fields.hasNext()) {
            String key = fields.next();
            if (!TOP_LEVEL_KEYS.contains(key)) {
                return false;
            }
        }
        if (root.has("user_prefs") && !validateUserPrefs(root.get("user_prefs"))) {
            return false;
        }
        if (root.has("project_context") && !validateProjectContext(root.get("project_context"))) {
            return false;
        }
        if (root.has("decisions") && !validateDecisions(root.get("decisions"))) {
            return false;
        }
        if (root.has("open_questions") && !validateStringArray(root.get("open_questions"))) {
            return false;
        }
        if (root.has("facts") && !validateStringArray(root.get("facts"))) {
            return false;
        }
        return true;
    }

    private boolean validateUserPrefs(JsonNode node) {
        if (node == null || node.isNull()) {
            return true;
        }
        if (!node.isObject()) {
            return false;
        }
        Iterator<String> fields = node.fieldNames();
        while (fields.hasNext()) {
            String key = fields.next();
            if (!USER_PREF_KEYS.contains(key)) {
                return false;
            }
        }
        return validateOptionalString(node.get("language"))
                && validateOptionalString(node.get("tone"))
                && validateOptionalString(node.get("format"))
                && validateOptionalStringArray(node.get("other"));
    }

    private boolean validateProjectContext(JsonNode node) {
        if (node == null || node.isNull()) {
            return true;
        }
        if (!node.isObject()) {
            return false;
        }
        Iterator<String> fields = node.fieldNames();
        while (fields.hasNext()) {
            String key = fields.next();
            if (!PROJECT_CONTEXT_KEYS.contains(key)) {
                return false;
            }
        }
        return validateOptionalString(node.get("app_name"))
                && validateOptionalString(node.get("current_goal"))
                && validateOptionalStringArray(node.get("stack"))
                && validateOptionalStringArray(node.get("notes"));
    }

    private boolean validateDecisions(JsonNode node) {
        if (node == null || node.isNull()) {
            return true;
        }
        if (!node.isArray()) {
            return false;
        }
        for (JsonNode item : node) {
            if (!item.isObject()) {
                return false;
            }
            Iterator<String> fields = item.fieldNames();
            while (fields.hasNext()) {
                String key = fields.next();
                if (!DECISION_KEYS.contains(key)) {
                    return false;
                }
            }
            JsonNode decisionNode = item.get("decision");
            if (decisionNode == null || !decisionNode.isTextual()) {
                return false;
            }
            if (!validateOptionalString(item.get("date"))) {
                return false;
            }
        }
        return true;
    }

    private boolean validateStringArray(JsonNode node) {
        if (node == null || node.isNull()) {
            return true;
        }
        if (!node.isArray()) {
            return false;
        }
        for (JsonNode item : node) {
            if (!item.isTextual()) {
                return false;
            }
        }
        return true;
    }

    private boolean validateOptionalString(JsonNode node) {
        return node == null || node.isNull() || node.isTextual();
    }

    private boolean validateOptionalStringArray(JsonNode node) {
        return node == null || node.isNull() || validateStringArray(node);
    }
}
