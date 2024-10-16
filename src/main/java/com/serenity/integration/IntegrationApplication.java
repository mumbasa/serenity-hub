package com.serenity.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.serenity.integration.service.NoteService;
import com.serenity.integration.service.PatientService;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
@EnableJpaRepositories(basePackages="com.serenity.integration.repository")
public class IntegrationApplication {

	@Autowired
	PatientService service;

	@Autowired
	NoteService noteService;

	Logger logger = LoggerFactory.getLogger(getClass());
	public static void main(String[] args) {
		SpringApplication.run(IntegrationApplication.class, args);
	}

@PostConstruct
	public void coke(){
		//String[] ac = {"NMC/OG/15/024341","NMC/OG/15/024341"};
		//List<String> ad = Arrays.asList(ac);
		//noteService.getHisNote();
		logger.info("Starting import");
	noteService.getProgressNote();
	noteService.getChiefNote();

		logger.info("finishing import");

	}
	
}
