package com.serenity.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.serenity.integration.cron.NoteServiceCron;
import com.serenity.integration.cron.VisitsCron;
import com.serenity.integration.service.EncounterService;
import com.serenity.integration.service.PatientService;
import com.serenity.integration.service.SetupService;
import com.serenity.integration.service.WardSetupService;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
@EnableJpaRepositories(basePackages="com.serenity.integration.repository")
public class IntegrationApplication {

	@Autowired
	PatientService service;

	@Autowired
	NoteServiceCron noteService;

	@Autowired
	VisitsCron vCron;

	@Autowired
	SetupService setupService;


	Logger logger = LoggerFactory.getLogger(getClass());
	public static void main(String[] args) {
		SpringApplication.run(IntegrationApplication.class, args);
	}

@PostConstruct
	public void coke(){
		//String[] ac = {"NMC/OG/15/024341","NMC/OG/15/024341"};
		//List<String> ad = Arrays.asList(ac);
		//service.loadPatients();
		logger.info("Starting import");
		//setupService.getServicePrices(1000);
//service.setupSerenity();
//setupService.getServicePrice("161380e9-22d3-4627-a97f-0f918ce3e4a9");
//setupService.setPricing("161380e9-22d3-4627-a97f-0f918ce3e4a9", "161380e9-22d3-4627-a97f-0f918ce3e4a9");
//setupService.healthServiceSetup("161380e9-22d3-4627-a97f-0f918ce3e4a9","RR.csv");;
//System.err.println(setupService.getWards("161380e9-22d3-4627-a97f-0f918ce3e4a9").size());
//	service.getHisNote();
//setupService.setWard("161380e9-22d3-4627-a97f-0f918ce3e4a9");
//.searchCountries();;
//noteService.getProgressNote();
//noteService.getCarePlan();
//noteService.getPresentingIllness();
//noteService.getChiefNote();
vCron.setupVisits();
	logger.info("finishing import");

	}
	
}
