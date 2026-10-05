package com.systemdesign.flashsale.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/flashsale_db");
        config.setUsername("engine_user");
        config.setPassword("engine_secret_password");

        // The core distributed systems protections:
        config.setMaximumPoolSize(20);
        config.setConnectionTimeout(3000); // Fail fast: wait max 3 seconds for a connection
        config.setMaxLifetime(1800000); // 30 minutes before forcing a connection refresh

        return new HikariDataSource(config);
    }

    @Bean
    public TransactionTemplate transactionTemplate(DataSource dataSource) {
        return new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }
}