package com.playlisto.controller;

import com.playlisto.model.FolderData;
import com.playlisto.model.Provider;
import com.playlisto.model.ProviderMetadata;
import com.playlisto.model.Source;
import com.playlisto.model.SourceMetadata;
import com.playlisto.service.DriveService;
import com.playlisto.service.SourceProviderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class PlaylistoController {

    private final DriveService driveService;
    private final SourceProviderService sourceProviderService;

    public PlaylistoController(DriveService driveService, SourceProviderService sourceProviderService) {
        this.driveService = driveService;
        this.sourceProviderService = sourceProviderService;
    }

    @PostMapping("/load-folder")
    public Map<String, Object> loadFolder() {
        FolderData data = driveService.loadFolder();
        sourceProviderService.getSources();

        return Map.of(
                "name", data.getFolders().get(0).getName(),
                "files", data.getFolders().get(0).getFiles()
        );
    }

    @GetMapping("/stream/{fileId}")
    public ResponseEntity<StreamingResponseBody> stream(
            @PathVariable String fileId,
            @RequestParam String filename,
            @RequestHeader(value = "Range", required = false) String range
    ) {
        return driveService.streamFile(fileId, filename, range);
    }

    @GetMapping("/sources")
    public Map<String, Object> listSources() {
        List<Source> sources = sourceProviderService.getSources();
        Source activeSource = sourceProviderService.getActiveSource();
        String activeSourceId = activeSource != null ? activeSource.getId() : null;

        return Map.of(
                "sources", sources,
                "active_source_id", activeSourceId
        );
    }

    @GetMapping("/sources-metadata")
    public List<SourceMetadata> listSourcesMetadata() {
        return sourceProviderService.getSourceMetadata();
    }

    @PostMapping("/sources")
    public Map<String, String> createSource(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String url = payload.get("url");
        String providerId = payload.get("provider_id");

        Source source = sourceProviderService.createSource(name, url, providerId);
        return Map.of("status", "created", "id", source.getId());
    }

    @PostMapping("/sources/activate/{sourceId}")
    public Map<String, String> activateSource(@PathVariable String sourceId) {
        sourceProviderService.activateSource(sourceId);
        return Map.of("status", "activated");
    }

    @DeleteMapping("/sources/{sourceId}")
    public Map<String, String> deleteSource(@PathVariable String sourceId) {
        sourceProviderService.deleteSource(sourceId);
        return Map.of("status", "deleted");
    }

    @GetMapping("/providers")
    public List<ProviderMetadata> listProviders() {
        return sourceProviderService.getProviderMetadata();
    }

    @PostMapping("/providers")
    public Provider createProvider(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String apiKey = payload.get("api_key");
        return sourceProviderService.createProvider(name, apiKey);
    }

    @PostMapping("/providers/activate/{providerId}")
    public Map<String, String> activateProvider(@PathVariable String providerId) {
        sourceProviderService.activateProvider(providerId);
        return Map.of("status", "activated");
    }
}
