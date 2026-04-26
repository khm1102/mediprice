package com.khm1102.mediprice.config;

import com.zaxxer.hikari.HikariDataSource;
import org.postgresql.Driver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;

/**
 * JPA / Hibernate 설정.
 * <ul>
 *   <li>{@code hibernate.dialect}는 명시하지 않음 — Hibernate 7+가 JDBC 메타데이터로 자동 감지.
 *       Hibernate Spatial이 PostGIS 함수를 자동 등록.</li>
 *   <li>{@code ddl-auto}는 환경변수로 제어. <b>운영 환경에서는 반드시 {@code validate}</b> 사용
 *       (자동 스키마 변경은 데이터 손실 위험).</li>
 *   <li>{@code show-sql}/{@code format-sql} 기본값은 false — 운영 로그에 PII가 섞이는 것을 방지.</li>
 *   <li>{@code hibernate.jdbc.time_zone=UTC} — OffsetDateTime 저장/조회를 UTC로 정규화하여 컨테이너 TZ 차이 영향 차단.</li>
 *   <li>HikariCP pool 사이즈/leak detection은 환경변수로 외부화 — Tomcat thread 수와 균형 유지.</li>
 * </ul>
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.khm1102.mediprice.repository")
public class JpaConfig {

    @Value("${db.url}")
    private String dbUrl;

    @Value("${db.username}")
    private String dbUsername;

    @Value("${db.password}")
    private String dbPassword;

    @Value("${db.pool-size}")
    private int poolSize;

    @Value("${db.min-idle}")
    private int minIdle;

    @Value("${db.max-lifetime}")
    private long maxLifetime;

    @Value("${db.leak-detection}")
    private long leakDetectionMs;

    @Value("${jpa.ddl-auto}")
    private String ddlAuto;

    @Value("${jpa.show-sql}")
    private boolean showSql;

    @Value("${jpa.format-sql}")
    private boolean formatSql;

    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        // ServiceLoader 자동 감지가 Tomcat WAR ClassLoader에서 불안정해 명시 등록
        dataSource.setDriverClassName(Driver.class.getName());
        dataSource.setJdbcUrl(dbUrl);
        dataSource.setUsername(dbUsername);
        dataSource.setPassword(dbPassword);
        dataSource.setMaximumPoolSize(poolSize);
        dataSource.setMinimumIdle(minIdle);
        dataSource.setMaxLifetime(maxLifetime);
        dataSource.setPoolName("MediPriceHikariPool");
        if (leakDetectionMs > 0) {
            dataSource.setLeakDetectionThreshold(leakDetectionMs);
        }
        return dataSource;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan("com.khm1102.mediprice.entity");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setShowSql(showSql);
        emf.setJpaVendorAdapter(vendorAdapter);

        Properties jpaProperties = new Properties();
        jpaProperties.setProperty("hibernate.hbm2ddl.auto", ddlAuto);
        // dialect는 Hibernate 7+에서 JDBC 메타데이터로 자동 감지 (명시 시 deprecation warning)
        jpaProperties.setProperty("hibernate.format_sql", String.valueOf(formatSql));
        jpaProperties.setProperty("hibernate.jdbc.time_zone", "UTC");
        emf.setJpaProperties(jpaProperties);

        return emf;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager txManager = new JpaTransactionManager();
        txManager.setEntityManagerFactory(entityManagerFactory);
        return txManager;
    }
}
