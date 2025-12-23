package com.bolsadeideas.springboot.app.models.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Service
public class GoogleDriveApiService {

    private final GoogleOAuthService oauthService;
    private final RestTemplate restTemplate = new RestTemplate();

    public GoogleDriveApiService(GoogleOAuthService oauthService) {
        this.oauthService = oauthService;
    }

    public byte[] downloadFileBytes(String userId, String fileId) {
        return downloadWithAutoRefresh(userId, fileId, true);
    }

    private byte[] downloadWithAutoRefresh(String userId, String fileId, boolean retryOn401) {
        String accessToken = oauthService.getValidAccessTokenOrRefresh(userId);

        URI uri = UriComponentsBuilder
                .fromHttpUrl("https://www.googleapis.com/drive/v3/files/{fileId}")
                .queryParam("alt", "media")
                .queryParam("supportsAllDrives", "true")
                .build(fileId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        try {
            ResponseEntity<byte[]> resp = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
            return resp.getBody();

        } catch (HttpClientErrorException.Unauthorized ex) {
            if (!retryOn401) throw new RuntimeException("401_TOKEN_EXPIRED");
            // fuerza refresh y reintenta 1 vez
            oauthService.getValidAccessTokenOrRefresh(userId);
            return downloadWithAutoRefresh(userId, fileId, false);

        } catch (HttpClientErrorException.Forbidden ex) {
            throw new RuntimeException("403_ACCESS_DENIED");

        } catch (HttpClientErrorException.NotFound ex) {
            throw new RuntimeException("404_NOT_FOUND");
        }
    }
    public Map<String, Object> getFileMetadata(String userId, String fileId) {
        String accessToken = oauthService.getValidAccessTokenOrRefresh(userId);

        URI uri = UriComponentsBuilder
                .fromHttpUrl("https://www.googleapis.com/drive/v3/files/{fileId}")
                .queryParam("fields", "id,name,mimeType,size")
                .queryParam("supportsAllDrives", "true")
                .build(fileId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<Map> resp = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map body = resp.getBody();
        //noinspection unchecked
        return body != null ? (Map<String, Object>) body : Map.of();
    }

    public List<Map<String, Object>> listFolderChildren(String userId, String folderId) {
        String accessToken = oauthService.getValidAccessTokenOrRefresh(userId);

        // q = '<folderId>' in parents and trashed=false
        String q = "'" + folderId + "' in parents and trashed=false";

        URI uri = UriComponentsBuilder
                .fromHttpUrl("https://www.googleapis.com/drive/v3/files")
                .queryParam("q", q)
                .queryParam("fields", "files(id,name,mimeType,size)")
                .queryParam("supportsAllDrives", "true")
                .queryParam("includeItemsFromAllDrives", "true")
                .build(true)
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<Map> resp = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map body = resp.getBody();
        if (body == null) return List.of();

        Object files = body.get("files");
        if (files instanceof List) {
            //noinspection unchecked
            return (List<Map<String, Object>>) files;
        }
        return List.of();
    }
}

