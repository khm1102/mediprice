package com.khm1102.mediprice.global.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 부팅 시 DB 초기화 — JPA ddl-auto가 다루지 못하는 영역 담당.
 * <ul>
 *   <li>PostGIS extension 활성화 (멱등 — IF NOT EXISTS)</li>
 *   <li>Hospital.location GIST 인덱스 (ddl-auto가 GEOGRAPHY 인덱스 생성 못 함)</li>
 *   <li>{@code procedures.sql}의 {@code search_nearby_hospitals} 함수 등록 (CREATE OR REPLACE)</li>
 * </ul>
 * 모든 DDL은 멱등이라 재실행 안전.
 */
@Slf4j
@Component
@DependsOn("entityManagerFactory")
public class DatabaseInitializer {

    private static final String PROCEDURES_PATH = "sql/procedures.sql";
    private static final String POSTGIS_EXTENSION_DDL =
            "CREATE EXTENSION IF NOT EXISTS postgis";
    /** Hospital 엔티티에 location을 매핑하지 않고 native UPDATE로만 채움 — JPA ddl-auto가 만들지 못하는 컬럼 추가. */
    private static final String LOCATION_COLUMN_DDL =
            "ALTER TABLE Hospital ADD COLUMN IF NOT EXISTS location GEOGRAPHY(POINT, 4326)";
    private static final String GIST_INDEX_DDL =
            "CREATE INDEX IF NOT EXISTS idx_hospital_location ON Hospital USING GIST(location)";

    private final DataSource dataSource;

    public DatabaseInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void initialize() {
        try (Connection conn = dataSource.getConnection()) {
            execute(conn, POSTGIS_EXTENSION_DDL);
            execute(conn, LOCATION_COLUMN_DDL);
            execute(conn, GIST_INDEX_DDL);
            executeProceduresScript(conn);
            log.info("DatabaseInitializer 완료");
        } catch (SQLException e) {
            throw new IllegalStateException("DatabaseInitializer 실행 실패", e);
        }
    }

    private void execute(Connection conn, String ddl) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(ddl);
        } catch (SQLException e) {
            // Hospital 테이블이 아직 생성되기 전(첫 부팅) 시 실패 가능 — JPA가 테이블 만든 다음 부팅에 정상화
            log.warn("DDL 실행 실패 (멱등 재시도 가능): {}\nDDL: {}", e.getMessage(), ddl);
        }
    }

    /**
     * procedures.sql 통째로 실행. {@code ScriptUtils.executeSqlScript}는 PL/pgSQL의
     * {@code $$ ... $$} 본문 안의 {@code ;}도 statement 구분자로 잘못 해석하므로 사용하지 않는다.
     */
    private void executeProceduresScript(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            String sql = StreamUtils.copyToString(
                    new ClassPathResource(PROCEDURES_PATH).getInputStream(),
                    StandardCharsets.UTF_8);
            stmt.execute(sql);
        } catch (IOException | SQLException e) {
            throw new IllegalStateException(PROCEDURES_PATH + " 실행 실패", e);
        }
    }
}
