package org.aion.api.serialization;

import com.fasterxml.jackson.databind.JsonNode;

public class MethodDescriptor {
    private final String name;
    private final JsonNode request;
    private final JsonNode response;
    private final JsonNode error;
    private final String description;

    public MethodDescriptor(String name,
                            JsonNode request,
                            JsonNode response,
                            JsonNode error,
                            String description) {
        this.request = request;
        this.response = response;
        this.error = error;
        this.description = description;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public JsonNode getRequest() {
        return request;
    }

    public JsonNode getResponse() {
        return response;
    }

    public JsonNode getError() {
        return error;
    }

    public String getDescription() {
        return description;
    }
}
