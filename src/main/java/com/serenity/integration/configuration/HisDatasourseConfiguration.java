package com.serenity.integration.configuration;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;



@Configuration

public class HisDatasourseConfiguration {


    @Bean
    @ConfigurationProperties("spring.datasource.his")
    public DataSourceProperties hisDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.his")
    public DataSource hisDataSource() {
        return hisDataSourceProperties().initializeDataSourceBuilder().build();
    }

 

   
    @Bean(name = "hisJdbcTemplate")
    public JdbcTemplate hisJdbcTemplate(@Qualifier("hisDataSource") DataSource dataSource){
        return new JdbcTemplate(dataSource);
    }


}
