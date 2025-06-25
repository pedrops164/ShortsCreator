package com.content_storage_service.config.converters;

import com.fasterxml.jackson.databind.JsonNode;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter // Marks this as a converter used when writing to the DB
public class JsonNodeToDocumentConverter implements Converter<JsonNode, Document> {

    @Override
    public Document convert(JsonNode source) {
        // Parse the JsonNode's string representation into a BSON Document
        return Document.parse(source.toString());
    }
}