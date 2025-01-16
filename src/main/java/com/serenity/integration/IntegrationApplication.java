package com.serenity.integration;

import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.serenity.integration.cron.NoteServiceCron;
import com.serenity.integration.cron.VisitsCron;
import com.serenity.integration.repository.EncounterRepository;
import com.serenity.integration.repository.VisitRepository;
import com.serenity.integration.service.AdmissionService;
import com.serenity.integration.service.EncounterService;
import com.serenity.integration.service.MedicalRequestService;
import com.serenity.integration.service.NoteService;
import com.serenity.integration.service.NoteWrangling;
import com.serenity.integration.service.PatientMigrationService;
import com.serenity.integration.service.PatientService;
import com.serenity.integration.service.PractitionerService;
import com.serenity.integration.service.SetupService;
import com.serenity.integration.service.VisitMigration;
import com.serenity.integration.service.VisitService;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.serenity.integration.repository")
public class IntegrationApplication {

	@Autowired
	PatientService service;

	@Autowired
	NoteService noteService;

	@Autowired
	MedicalRequestService medicalRequestService;

	@Autowired
	VisitMigration visitService;

	@Autowired
	VisitRepository v ;

	@Autowired
	SetupService setupService;
	@Autowired
	EncounterRepository encounterRepository;
	@Autowired
	EncounterService encounterService;
	@Autowired
	NoteWrangling noteWrangling;

	@Autowired
	PatientMigrationService migrationService;

	@Autowired
	PatientService patientService;

	Logger logger = LoggerFactory.getLogger(getClass());

	public static void main(String[] args) {
		SpringApplication.run(IntegrationApplication.class, args);
	}

	@PostConstruct
	public void coke() {
				logger.info("Starting import");
		
		try {
			//medicalRequestService.IPDDataThread();
			//medicalRequestService.IPDThread();
			//visitService.getPatientsThreads();
			System.err.println(v.getfirst1());
		//	encounterService.getEncounterThreads();

//System.err.println(encounterRepository.findEcounterByPatientDateDoctor("NMC/OG/15/018824", "2015-06-01T19:16:02Z", "LSHHI153"));
} catch (Exception e) {
			e.printStackTrace();
		}
		
		// practitionerService.savePracttioner();
		// visitMigration.getPatientsThreads();
		logger.info("finishing import");

	}

}
