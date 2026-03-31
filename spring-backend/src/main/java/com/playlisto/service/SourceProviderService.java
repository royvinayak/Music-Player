package com.playlisto.service;

import com.playlisto.model.Provider;
import com.playlisto.model.ProviderMetadata;
import com.playlisto.model.Source;
import com.playlisto.model.SourceMetadata;
import com.playlisto.storage.DataStore;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SourceProviderService {
    private final DataStore dataStore;
    private String activeProviderId;
    private String activeSourceId;

    public SourceProviderService(DataStore dataStore) {
        this.dataStore = dataStore;
        this.activeProviderId = null;
        this.activeSourceId = null;
        initializeActiveIds();
    }

    private void initializeActiveIds() {
        List<Provider> providers = dataStore.loadProviders();
        if (!providers.isEmpty()) {
            activeProviderId = providers.get(0).getId();
        }

        List<Source> sources = dataStore.loadSources();
        if (!sources.isEmpty()) {
            activeSourceId = sources.get(0).getId();
        }
    }

    public List<ProviderMetadata> getProviderMetadata() {
        List<Provider> providers = dataStore.loadProviders();
        List<ProviderMetadata> metadata = new ArrayList<>();

        for (Provider p : providers) {
            metadata.add(new ProviderMetadata(
                    p.getId(),
                    p.getName(),
                    p.getType(),
                    StringUtils.equals(p.getId(), activeProviderId)
            ));
        }

        return metadata;
    }

    public Provider createProvider(String name, String apiKey) {
        if (StringUtils.isBlank(name) || StringUtils.isBlank(apiKey)) {
            throw new IllegalArgumentException("Name and API key required");
        }

        List<Provider> providers = dataStore.loadProviders();

        Provider provider = new Provider(UUID.randomUUID().toString(), name, apiKey, "google_drive");
        providers.add(provider);

        if (activeProviderId == null) {
            activeProviderId = provider.getId();
        }

        dataStore.saveProviders(providers);
        return provider;
    }

    public void activateProvider(String providerId) {
        List<Provider> providers = dataStore.loadProviders();

        Optional<Provider> providerOptional = providers.stream()
                .filter(provider -> provider.getId().equals(providerId))
                .findFirst();

        if (providerOptional.isEmpty()) {
            throw new IllegalArgumentException("Provider not found");
        }

        activeProviderId = providerId;
        dataStore.saveProviders(providers);
    }

    public Provider getActiveProvider() {
        if (StringUtils.isBlank(activeProviderId)) {
            return null;
        }
        return dataStore.loadProviders().stream()
                .filter(provider -> provider.getId().equals(activeProviderId))
                .findFirst()
                .orElse(null);
    }

    public List<Source> getSources() {
        return dataStore.loadSources();
    }

    public Source createSource(String name, String url, String providerId) {
        if (StringUtils.isBlank(name) || StringUtils.isBlank(url) || StringUtils.isBlank(providerId)) {
            throw new IllegalArgumentException("Invalid input");
        }

        List<Source> sources = dataStore.loadSources();

        Source source = new Source(UUID.randomUUID().toString(), name, url, providerId);
        sources.add(source);

        if (activeSourceId == null) {
            activeSourceId = source.getId();
        }

        dataStore.saveSources(sources);
        return source;
    }

    public void activateSource(String sourceId) {
        List<Source> sources = dataStore.loadSources();

        Optional<Source> sourceOptional = sources.stream()
                .filter(source -> source.getId().equals(sourceId))
                .findFirst();

        if (sourceOptional.isEmpty()) {
            throw new IllegalArgumentException("Source not found");
        }

        activeSourceId = sourceId;
        dataStore.saveSources(sources);
    }

    public void deleteSource(String sourceId) {
        List<Source> sources = dataStore.loadSources();
        sources.removeIf(v -> v.getId().equals(sourceId));
        if (StringUtils.equals(activeSourceId, sourceId)) {
            activeSourceId = sources.isEmpty() ? null : sources.get(0).getId();
        }
        dataStore.saveSources(sources);
    }

    public Source getActiveSource() {
        List<Source> sources = dataStore.loadSources();
        
        // If no sources exist, return null
        if (sources.isEmpty()) {
            return null;
        }
        
        // If activeSourceId is set, try to find it
        if (!StringUtils.isBlank(activeSourceId)) {
            Optional<Source> found = sources.stream()
                    .filter(source -> source.getId().equals(activeSourceId))
                    .findFirst();
            if (found.isPresent()) {
                return found.get();
            }
        }
        
        // If activeSourceId is not found or not set, default to first source
        activeSourceId = sources.get(0).getId();
        return sources.get(0);
    }

    public List<SourceMetadata> getSourceMetadata() {
        List<Source> sources = dataStore.loadSources();
        List<SourceMetadata> metadata = new ArrayList<>();
        List<Provider> providers = dataStore.loadProviders();

        // Ensure active source is set
        Source activeSource = getActiveSource();
        String activeId = activeSource != null ? activeSource.getId() : null;

        for (Source source : sources) {
            // Get the provider for this source to retrieve the API key
            Provider provider = providers.stream()
                    .filter(p -> p.getId().equals(source.getProviderId()))
                    .findFirst()
                    .orElse(null);

            String apiKey = provider != null ? provider.getApiKey() : "";
            boolean isActive = StringUtils.equals(source.getId(), activeId);

            metadata.add(new SourceMetadata(
                    source.getId(),
                    source.getName(),
                    source.getUrl(),
                    apiKey,
                    isActive
            ));
        }

        return metadata;
    }
}
