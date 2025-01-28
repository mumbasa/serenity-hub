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
	PractitionerService practitionerService;

	@Autowired
	VisitService visitService;

	@Autowired
	VisitMigration visitMigration;

	@Autowired
	PatientMigrationService v ;

	@Autowired
	SetupService setupService;
	@Autowired
	PatientMigrationService patientMigrationService;
	@Autowired
	EncounterService encounterService;
	@Autowired
	NoteWrangling noteWrangling;

	@Autowired
	PatientMigrationService migrationService;

	@Autowired
	PatientService patientService;

	@Autowired
	MedicalRequestService medicalRequestService;

	Logger logger = LoggerFactory.getLogger(getClass());

	public static void main(String[] args) {
		SpringApplication.run(IntegrationApplication.class, args);
	}

	@PostConstruct
	public void coke() {
		logger.info("Starting import");
		//patientService.loadPatients();;
	//	patientMigrationService.getPatientsThreads();;
		//practitionerService.getLegacyPractitioner();
		//practitionerService.migrateDoctors();
	//	visitService.getHisThreads();
	///	visitService.getIPDVISITSThreads();
		//visitService.getlegacyThreads();
		//visitMigration.getVisitThreads();
		//noteService.truncate();
		//noteService.getChiefNotes();
		//noteService.cleanData();

		//noteService.getCarePlan();
		//noteService.cleanData();

	//	noteService.getPresentingIllness();
	//	noteService.cleanData();

	//	 noteService.getProgressNote();
	//	 noteService.cleanData();

	//	 noteService.cleanData();
	//	encounterService.encounterOPDthread();
	//noteService.noteThread();getLegacyEncounters
	//medicalRequestService.medicalRequestOPD2();
	//medicalRequestService.medicalRequestOPD2();
	//encounterService.encounterLegacythread();
	noteService.getLegacyEncounters();
	logger.info("finishi.ng import");

	}

}
