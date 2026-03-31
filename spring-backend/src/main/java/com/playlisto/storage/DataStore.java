package com.playlisto.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.playlisto.model.FolderData;
import com.playlisto.model.Provider;
import com.playlisto.model.Source;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataStore {
    private static final Path DATA_DIR = Paths.get("data");
    private static final Path FOLDERS_FILE = DATA_DIR.resolve("folders.json");
    private static final Path PROVIDERS_FILE = DATA_DIR.resolve("providers.json");
    private static final Path SOURCES_FILE = DATA_DIR.resolve("sources.json");

    private final ObjectMapper objectMapper;

    public DataStore() {
        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public synchronized FolderData loadFolderData() {
        try {
            if (Files.notExists(FOLDERS_FILE)) {
                return new FolderData();
            }
            return objectMapper.readValue(FOLDERS_FILE.toFile(), FolderData.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load folder data", e);
        }
    }

    public synchronized void saveFolderData(FolderData data) {
        try {
            Files.createDirectories(DATA_DIR);
            objectMapper.writeValue(FOLDERS_FILE.toFile(), data);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save folder data", e);
        }
    }

    public synchronized List<Provider> loadProviders() {
        try {
            if (Files.notExists(PROVIDERS_FILE)) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(PROVIDERS_FILE.toFile(), new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to load providers", e);
        }
    }

    public synchronized void saveProviders(List<Provider> providers) {
        try {
            Files.createDirectories(DATA_DIR);
            objectMapper.writeValue(PROVIDERS_FILE.toFile(), providers);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save providers", e);
        }
    }

    public synchronized List<Source> loadSources() {
        try {
            if (Files.notExists(SOURCES_FILE)) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(SOURCES_FILE.toFile(), new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to load sources", e);
        }
    }

    public synchronized void saveSources(List<Source> sources) {
        try {
            Files.createDirectories(DATA_DIR);
            objectMapper.writeValue(SOURCES_FILE.toFile(), sources);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save sources", e);
        }
    }
}
