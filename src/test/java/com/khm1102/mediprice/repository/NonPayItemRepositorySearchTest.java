package com.khm1102.mediprice.repository;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class NonPayItemRepositorySearchTest {

    @Test
    void naturalLanguageSearchQueryUsesActiveItemsAndTrigramColumns() throws IOException {
        String src = Files.readString(Path.of(
                "src/main/java/com/khm1102/mediprice/repository/NonPayItemRepository.java"),
                StandardCharsets.UTF_8);

        assertThat(src).contains("searchNaturalLanguageMatches");
        assertThat(src).contains("n.npay_kor_nm");
        assertThat(src).contains("n.npay_mdiv_cd_nm");
        assertThat(src).contains("n.npay_sdiv_cd_nm");
        assertThat(src).contains("n.adt_end_dd = '99991231'");
        assertThat(src).contains("similarity(");
        assertThat(src).contains("ILIKE CONCAT('%', :keyword, '%')");
        assertThat(src).contains("LIMIT :limit");
    }
}
