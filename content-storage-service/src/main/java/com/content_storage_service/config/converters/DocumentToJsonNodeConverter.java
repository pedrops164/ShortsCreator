package com.content_storage_service.config.converters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter // Marks this as a converter used when reading from the DB
public class DocumentToJsonNodeConverter implements Converter<Document, JsonNode> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public JsonNode convert(Document source) {
        try {
            // Convert the BSON Document into a JSON string, then parse it into a JsonNode
            return objectMapper.readTree(source.toJson());
        } catch (Exception e) {
            // Handle exception appropriately, maybe return null or throw a runtime exception
            e.printStackTrace();
            return null;
        }
    }
}