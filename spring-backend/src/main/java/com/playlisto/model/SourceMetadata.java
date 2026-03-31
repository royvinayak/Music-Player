package com.playlisto.model;

public class SourceMetadata {
    private String id;
    private String name;
    private String url;
    private String apiKey;
    private boolean active;

    public SourceMetadata(String id, String name, String url, String apiKey, boolean active) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.apiKey = apiKey;
        this.active = active;
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
