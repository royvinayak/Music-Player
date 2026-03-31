package com.playlisto.model;

public class Provider {
    private String id;
    private String name;
    private String apiKey;
    private String type;

    public Provider() {}

    public Provider(String id, String name, String apiKey, String type) {
        this.id = id;
        this.name = name;
        this.apiKey = apiKey;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
