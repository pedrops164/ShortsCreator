package com.shortscreator.shared.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StreamUtils; // A better way to get string from resource

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.core.report.ProcessingReport;

public class TemplateValidator {

    private final ObjectMapper objectMapper;
    private final Map<String, JsonSchema> schemaCache = new ConcurrentHashMap<>();
    private final JsonSchemaFactory schemaFactory = JsonSchemaFactory.byDefault();

    /**
     * The constructor takes the dependencies it needs. It doesn't know or care
     * that they will be provided by a Spring application.
     */
    public TemplateValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Validates JsonNode templateParams against the specific template's JSON Schema.
     * Emits an error if schema loading fails.
     * This is synchronous and will block until validation is complete.
     */
    public void validate(String templateId, JsonNode templateParams, boolean isFinalValidation) throws ValidationException {
        try {
            String schemaName = templateId + (isFinalValidation ? "_final" : "_draft");
            JsonSchema schema = schemaCache.computeIfAbsent(schemaName, this::loadSchema);

            ProcessingReport report = schema.validate(templateParams);
            if (!report.isSuccess()) {
                // Throw a custom exception with the detailed report.
                throw new ValidationException("JSON schema validation failed: " + report.toString());
            }
        } catch (Exception e) {
            // This catches loading errors or validation errors.
            throw new ValidationException("Cannot validate template " + templateId + ". Reason: " + e.getMessage(), e);
        }
    }

    private JsonSchema loadSchema(String schemaName) {
        String schemaFileName = schemaName + "_schema.json";
        try {
            // Load the resource as a stream directly from this class's context
            InputStream inputStream = TemplateValidator.class.getResourceAsStream("/schemas/" + schemaFileName);

            if (inputStream == null) {
                throw new IOException("Schema file not found on classpath: /schemas/" + schemaFileName);
            }
            String schemaString = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            JsonNode schemaNode = objectMapper.readTree(schemaString);
            return schemaFactory.getJsonSchema(schemaNode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load or parse schema " + schemaFileName + ", reason: " + e.getMessage(), e);
        }
    }
    
    // A simple custom exception class
    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) { super(message); }
        public ValidationException(String message, Throwable cause) { super(message, cause); }
    }
}