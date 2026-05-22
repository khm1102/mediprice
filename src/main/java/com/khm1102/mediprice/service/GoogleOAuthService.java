package com.khm1102.mediprice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class GoogleOAuthService {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    @Value("${google.client-id:}")
    private String clientId;

    @Value("${google.client-secret:}")
    private String clientSecret;

    @Value("${google.redirect-uri:http://localhost:8080/auth/oauth2/callback}")
    private String redirectUri;

    private final JsonMapper jsonMapper;
    private final RestClient restClient;

    public GoogleOAuthService(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
        this.restClient = RestClient.create();
    }

    public String buildAuthorizationUrl(String state) {
        return "https://accounts.google.com/o/oauth2/v2/auth" +
               "?client_id=" + encode(clientId) +
               "&redirect_uri=" + encode(redirectUri) +
               "&response_type=code" +
               "&scope=openid+profile+email" +
               "&state=" + encode(state) +
               "&access_type=online";
    }

    public GoogleUserInfo exchangeCodeForUserInfo(String code) {
        // 1. 코드 → 액세스 토큰 교환
        String formBody = "code=" + encode(code) +
                          "&client_id=" + encode(clientId) +
                          "&client_secret=" + encode(clientSecret) +
                          "&redirect_uri=" + encode(redirectUri) +
                          "&grant_type=authorization_code";

        String tokenResponseStr = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formBody)
                .retrieve()
                .body(String.class);

        String accessToken = parseField(tokenResponseStr, "access_token");

        // 2. 액세스 토큰 → 사용자 정보 조회
        String userInfoStr = restClient.get()
                .uri(USERINFO_URL)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(String.class);

        String sub = parseField(userInfoStr, "sub");
        String email = parseField(userInfoStr, "email");
        String name = parseField(userInfoStr, "name");

        return new GoogleUserInfo(sub, email, name);
    }

    private String parseField(String json, String field) {
        try {
            JsonNode node = jsonMapper.readTree(json);
            JsonNode value = node.get(field);
            return value != null ? value.asText() : "";
        } catch (Exception e) {
            throw new RuntimeException("구글 API 응답 파싱 실패: " + e.getMessage(), e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record GoogleUserInfo(String id, String email, String name) {
    }
}
