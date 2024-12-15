package com.serenity.integration.configuration;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;

@Configuration
@EnableJpaRepositories(
    basePackages = "com.serenity.integration.serenity-models",
    entityManagerFactoryRef = "entityManagerFactory",
    transactionManagerRef = "transactionManager"
)
public class SerenityDataSource {
    @Bean
    @ConfigurationProperties("spring.datasource.sere")
    public DataSourceProperties serenityDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name="SerenityDataSource")
    @ConfigurationProperties("spring.datasource.sere")
    public DataSource dataSource() {
        return serenityDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("SerenityDataSource") DataSource serenityDataSource) {
        return builder
                .dataSource(serenityDataSource)
                .packages("serenity-models")
                .persistenceUnit("primary")
                .properties(getPrimaryJpaProperties())
                .build();
    }

    private java.util.Map<String, Object> getPrimaryJpaProperties() {
        java.util.Map<String, Object> props = new java.util.HashMap<>();
        props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        props.put("hibernate.hbm2ddl.auto", "update");
        props.put("hibernate.jdbc.lob.non_contextual_creation", true);
        return props;
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean(name = "serenityJdbcTemplate")
    public JdbcTemplate serenityJdbcTemplate(){
        return new JdbcTemplate(dataSource());
    }
}
