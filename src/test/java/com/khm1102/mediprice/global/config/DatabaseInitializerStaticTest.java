package com.khm1102.mediprice.global.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseInitializerStaticTest {

    @Test
    void initializerCreatesPgTrgmAndNonPayItemSearchIndex() throws IOException {
        String src = Files.readString(Path.of(
                "src/main/java/com/khm1102/mediprice/global/config/DatabaseInitializer.java"),
                StandardCharsets.UTF_8);

        assertThat(src).contains("CREATE EXTENSION IF NOT EXISTS pg_trgm");
        assertThat(src).contains("idx_nonpayitem_search_trgm");
        assertThat(src).contains("gin_trgm_ops");
        assertThat(src).contains("WHERE adt_end_dd = '99991231'");
    }
}
