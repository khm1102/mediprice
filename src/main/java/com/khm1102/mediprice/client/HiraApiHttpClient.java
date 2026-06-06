package com.khm1102.mediprice.client;

import com.khm1102.mediprice.client.hira.common.HiraResponse;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;
import tools.jackson.core.type.TypeReference;
import tools.jackson.dataformat.xml.XmlMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.UnaryOperator;

/**
 * HIRA 공공 API 전용 HTTP client.
 * <p>
 * {@link HttpRequest#timeout(Duration)}으로 요청 전체 timeout을 강제한다. 외부 API가 응답 헤더 단계에서
 * 오래 멈춰도 배치 producer 스레드가 무기한 묶이지 않게 하는 방어선이다.
 */
final class HiraApiHttpClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final DefaultUriBuilderFactory uriFactory;
    private final URI trustedBaseUri;

    HiraApiHttpClient(String baseUrl) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.uriFactory = new DefaultUriBuilderFactory(baseUrl);
        this.uriFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.URI_COMPONENT);
        this.trustedBaseUri = URI.create(baseUrl);
    }

    byte[] get(UnaryOperator<UriBuilder> uriBuilder) throws IOException, InterruptedException {
        URI uri = encodeQueryPlus(uriBuilder.apply(uriFactory.builder()).build());
        validateRequestUri(uri);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
        CompletableFuture<HttpResponse<byte[]>> future =
                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());
        HttpResponse<byte[]> response;
        try {
            response = future.get(REQUEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new IOException("HIRA request timeout (" + REQUEST_TIMEOUT.toSeconds() + "s)", e);
        } catch (ExecutionException e) {
            future.cancel(true);
            Throwable cause = e.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("HIRA request failed: " + cause.getMessage(), cause);
        }
        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("HIRA HTTP " + statusCode);
        }
        return response.body();
    }

    <T> HiraResponse<T> getXml(
            UnaryOperator<UriBuilder> uriBuilder,
            TypeReference<HiraResponse<T>> typeRef,
            XmlMapper xmlMapper) throws IOException, InterruptedException {
        return xmlMapper.readValue(get(uriBuilder), typeRef);
    }

    /**
     * scheme/host/port가 생성자에서 받은 base URL과 일치하는지 강제한다.
     * {@link DefaultUriBuilderFactory}의 builder는 caller가 host/scheme을 갈아치울 수 있어
     * 외부 입력이 흘러들어올 때 SSRF 방어선이 필요하다.
     */
    private void validateRequestUri(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException("Request URI must not be null");
        }
        String scheme = uri.getScheme();
        String trustedScheme = trustedBaseUri.getScheme();
        if (scheme == null || trustedScheme == null || !trustedScheme.equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("Unexpected URI scheme: " + uri);
        }
        String host = uri.getHost();
        String trustedHost = trustedBaseUri.getHost();
        if (host == null || trustedHost == null || !trustedHost.equalsIgnoreCase(host)) {
            throw new IllegalArgumentException("Unexpected URI host: " + uri);
        }
        if (normalizedPort(uri) != normalizedPort(trustedBaseUri)) {
            throw new IllegalArgumentException("Unexpected URI port: " + uri);
        }
    }

    private static int normalizedPort(URI uri) {
        int port = uri.getPort();
        if (port != -1) {
            return port;
        }
        String scheme = uri.getScheme();
        if ("https".equalsIgnoreCase(scheme)) {
            return 443;
        }
        if ("http".equalsIgnoreCase(scheme)) {
            return 80;
        }
        return -1;
    }

    private static URI encodeQueryPlus(URI uri) {
        String rawQuery = uri.getRawQuery();
        if (rawQuery == null || !rawQuery.contains("+")) {
            return uri;
        }
        String ascii = uri.toASCIIString();
        int queryStart = ascii.indexOf('?');
        if (queryStart < 0) {
            return uri;
        }
        int fragmentStart = ascii.indexOf('#', queryStart);
        String prefix = ascii.substring(0, queryStart + 1);
        String query = fragmentStart < 0 ? ascii.substring(queryStart + 1) : ascii.substring(queryStart + 1, fragmentStart);
        String suffix = fragmentStart < 0 ? "" : ascii.substring(fragmentStart);
        return URI.create(prefix + query.replace("+", "%2B") + suffix);
    }
}
