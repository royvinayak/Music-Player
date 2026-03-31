package com.playlisto.model;

public class Source {
    private String id;
    private String name;
    private String url;
    private String providerId;

    public Source() {}

    public Source(String id, String name, String url, String providerId) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.providerId = providerId;
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

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
}
