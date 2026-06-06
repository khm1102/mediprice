package com.khm1102.mediprice.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HIRA 외부 호출 설정 회귀 방지.
 * 실제 data.go.kr 호출 없이 보안 기본값과 timeout 설정만 정적으로 검증한다.
 */
class HiraClientStaticCheckTest {

    @Test
    void hiraBaseUrlDefaultsUseHttps() throws Exception {
        String yml = Files.readString(Path.of("src/main/resources/application.yml"), StandardCharsets.UTF_8);
        String nonPayClient = Files.readString(
                Path.of("src/main/java/com/khm1102/mediprice/client/HiraNonPayClient.java"), StandardCharsets.UTF_8);
        String detailClient = Files.readString(
                Path.of("src/main/java/com/khm1102/mediprice/client/HiraDetailClient.java"), StandardCharsets.UTF_8);

        assertThat(yml).contains("HIRA_DETAIL_BASE_URL:https://apis.data.go.kr");
        assertThat(yml).contains("HIRA_NONPAY_BASE_URL:https://apis.data.go.kr");
        assertThat(nonPayClient).contains("hira.nonpay-base-url:https://apis.data.go.kr");
        assertThat(detailClient).contains("hira.detail-base-url:https://apis.data.go.kr");
        assertThat(yml).doesNotContain("HIRA_DETAIL_BASE_URL:http://apis.data.go.kr");
        assertThat(yml).doesNotContain("HIRA_NONPAY_BASE_URL:http://apis.data.go.kr");
    }

    @Test
    void hiraClientsUseDirectHttpClientWithRequestTimeouts() throws Exception {
        String httpClient = Files.readString(
                Path.of("src/main/java/com/khm1102/mediprice/client/HiraApiHttpClient.java"),
                StandardCharsets.UTF_8);
        String hospitalClient = Files.readString(
                Path.of("src/main/java/com/khm1102/mediprice/client/HiraHospitalClient.java"),
                StandardCharsets.UTF_8);
        String nonPayClient = Files.readString(
                Path.of("src/main/java/com/khm1102/mediprice/client/HiraNonPayClient.java"),
                StandardCharsets.UTF_8);
        String detailClient = Files.readString(
                Path.of("src/main/java/com/khm1102/mediprice/client/HiraDetailClient.java"),
                StandardCharsets.UTF_8);

        assertThat(httpClient).contains("HttpClient.newBuilder()");
        assertThat(httpClient).contains(".connectTimeout(CONNECT_TIMEOUT)");
        assertThat(httpClient).contains(".timeout(REQUEST_TIMEOUT)");
        assertThat(httpClient).contains("sendAsync");
        assertThat(httpClient).contains("future.get(REQUEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)");
        assertThat(httpClient).contains("future.cancel(true)");
        assertThat(httpClient).contains("Duration.ofSeconds(10)");
        assertThat(httpClient).contains("Duration.ofSeconds(30)");
        assertThat(httpClient).contains("EncodingMode.URI_COMPONENT");
        assertThat(httpClient).contains("encodeQueryPlus");
        assertThat(httpClient).contains("\"%2B\"");
        assertThat(hospitalClient).contains("new HiraApiHttpClient(baseUrl)");
        assertThat(nonPayClient).contains("new HiraApiHttpClient(baseUrl)");
        assertThat(detailClient).contains("new HiraApiHttpClient(baseUrl)");
        assertThat(hospitalClient + nonPayClient + detailClient)
                .doesNotContain("SimpleClientHttpRequestFactory")
                .doesNotContain("RestClient.builder()");
    }

    @Test
    void hospitalBasisOperationNameMatchesActualPath() throws Exception {
        try (Stream<Path> paths = Files.walk(Path.of("src/main/java/com/khm1102/mediprice"))) {
            String source = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(HiraClientStaticCheckTest::readUnchecked)
                    .collect(Collectors.joining("\n"));

            assertThat(source).doesNotContain("getHospBasisList1");
        }
    }

    private static String readUnchecked(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
