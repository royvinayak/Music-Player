package com.playlisto.service;

import com.playlisto.model.DriveFile;
import com.playlisto.model.FolderData;
import com.playlisto.model.Source;
import com.playlisto.model.Provider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DriveService {
    private static final Pattern FOLDER_ID_PATTERN = Pattern.compile("(?:folders/|open\\?id=)([a-zA-Z0-9_-]+)");
    private static final Pattern FOLDER_ID_PATTERN_ALT = Pattern.compile("/d/([a-zA-Z0-9_-]+)/");
    private final SourceProviderService sourceProviderService;
    private final ObjectMapper objectMapper;

    public DriveService(SourceProviderService sourceProviderService) {
        this.sourceProviderService = sourceProviderService;
        this.objectMapper = new ObjectMapper();
    }

    public FolderData loadFolder() {
        Source activeSource = sourceProviderService.getActiveSource();
        if (activeSource == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active source configured");
        }

        Provider provider = sourceProviderService.getActiveProvider();
        if (provider == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active provider configured");
        }

        String apiKey = provider.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Active provider has no API key");
        }

        String folderUrl = activeSource.getUrl();
        if (folderUrl == null || folderUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Active source URL missing");
        }

        String folderId = extractFolderId(folderUrl);
        List<DriveFile> files = fetchFolderRecursive(folderId, apiKey);

        FolderData data = new FolderData();
        data.setLastFolder("active_source");

        FolderData.FolderEntry entry = new FolderData.FolderEntry();
        entry.setName("Active Source");
        entry.setFiles(files);

        data.getFolders().add(entry);

        return data;
    }

    private String extractFolderId(String url) {
        Matcher matcher = FOLDER_ID_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }

        matcher = FOLDER_ID_PATTERN_ALT.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Google Drive folder URL");
    }

    private List<DriveFile> fetchFolderRecursive(String folderId, String apiKey) {
        return fetchFolderRecursive(folderId, apiKey, null);
    }

    private List<DriveFile> fetchFolderRecursive(String folderId, String apiKey, String pageToken) {
        List<DriveFile> result = new ArrayList<>();
        String query = String.format("'%s' in parents and trashed=false", folderId);

        StringBuilder endpoint = new StringBuilder("https://www.googleapis.com/drive/v3/files");
        endpoint.append("?key=").append(apiKey);
        endpoint.append("&supportsAllDrives=true");
        endpoint.append("&includeItemsFromAllDrives=true");
        endpoint.append("&fields=nextPageToken,files(id,name,mimeType)");
        endpoint.append("&pageSize=1000");
        endpoint.append("&q=").append(urlEncode(query));
        if (pageToken != null && !pageToken.isBlank()) {
            endpoint.append("&pageToken=").append(urlEncode(pageToken));
        }

        System.out.println("Fetching Drive folder with endpoint: " + endpoint); 
        try {
            String raw = fetch(endpoint.toString(), null);
            JsonNode root = objectMapper.readTree(raw);
            if (root.has("files")) {
                for (JsonNode item : root.get("files")) {
                    if (item.has("mimeType") && item.get("mimeType").asText().equals("application/vnd.google-apps.folder")) {
                        String subId = item.get("id").asText();
                        result.addAll(fetchFolderRecursive(subId, apiKey));
                    } else if (item.has("name") && isAudioFile(item.get("name").asText())) {
                        result.add(new DriveFile(item.get("id").asText(), item.get("name").asText()));
                    }
                }
            }

            if (root.has("nextPageToken")) {
                String nextPageToken = root.get("nextPageToken").asText();
                if (!nextPageToken.isBlank()) {
                    result.addAll(fetchFolderRecursive(folderId, apiKey, nextPageToken));
                }
            }

            return result;

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Drive folder fetch failed", e);
        }
    }

    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    private boolean isAudioFile(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".mp3") || lower.endsWith(".flac") || lower.endsWith(".wav") || lower.endsWith(".m4a") || lower.endsWith(".ogg");
    }

    private String fetch(String address, String range) throws IOException {
        URL url = new URL(address);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(30_000);
        connection.setReadTimeout(120_000);

        if (range != null) {
            connection.setRequestProperty("Range", range);
        }

        int status = connection.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK && status != HttpURLConnection.HTTP_PARTIAL) {
            String errorBody;
            try (InputStream err = connection.getErrorStream()) {
                errorBody = err != null ? new String(err.readAllBytes(), StandardCharsets.UTF_8) : "";
            }
            String message = String.format("Drive request failed: %d %s. %s", status, connection.getResponseMessage(), errorBody);
            System.out.println("Drive API ERROR: " + message);
            throw new IOException(message);
        }

        try (InputStream is = connection.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            connection.disconnect();
        }
    }

    public ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody> streamFile(String fileId, String filename, String rangeHeader) {
        String driveUrl = "https://drive.google.com/uc?export=download&id=" + fileId;
        MediaType mediaType = guessMediaType(filename);

        try {
            URL url = new URL(driveUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(true);
            if (rangeHeader != null && !rangeHeader.isBlank()) {
                conn.setRequestProperty("Range", rangeHeader);
            }

            int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK && status != HttpURLConnection.HTTP_PARTIAL) {
                throw new ResponseStatusException(HttpStatus.valueOf(status), "Drive download failed");
            }

            HttpHeaders headers = new HttpHeaders();
            if (conn.getHeaderField("Content-Length") != null) {
                headers.set(HttpHeaders.CONTENT_LENGTH, conn.getHeaderField("Content-Length"));
            }
            if (conn.getHeaderField("Content-Range") != null) {
                headers.set("Content-Range", conn.getHeaderField("Content-Range"));
            }
            if (conn.getHeaderField("Accept-Ranges") != null) {
                headers.set("Accept-Ranges", conn.getHeaderField("Accept-Ranges"));
            }
            headers.setContentType(mediaType);

            InputStream stream = conn.getInputStream();

            org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody body = outputStream -> {
                try (InputStream in = new BufferedInputStream(stream); OutputStream out = outputStream) {
                    byte[] buffer = new byte[8192];
                    int n;
                    while ((n = in.read(buffer)) != -1) {
                        out.write(buffer, 0, n);
                    }
                    out.flush();
                } finally {
                    conn.disconnect();
                }
            };

            return ResponseEntity.status(status).headers(headers).body(body);

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Drive streaming failed", e);
        }
    }

    private MediaType guessMediaType(String filename) {
        String lower = filename == null ? "" : filename.toLowerCase();
        if (lower.endsWith(".mp3")) {
            return MediaType.valueOf("audio/mpeg");
        }
        if (lower.endsWith(".flac")) {
            return MediaType.valueOf("audio/flac");
        }
        if (lower.endsWith(".wav")) {
            return MediaType.valueOf("audio/wav");
        }
        if (lower.endsWith(".m4a")) {
            return MediaType.valueOf("audio/mp4");
        }
        if (lower.endsWith(".ogg")) {
            return MediaType.valueOf("audio/ogg");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
