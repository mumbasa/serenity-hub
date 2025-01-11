package com.serenity.integration.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.repository.CrudRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

import com.serenity.integration.models.Doctors;
import com.serenity.integration.models.Encounter;
import com.serenity.integration.models.EncounterNote;
import com.serenity.integration.models.MedicalRequest;
import com.serenity.integration.models.PatientData;
import com.serenity.integration.models.Visits;
import com.serenity.integration.repository.DoctorRepository;
import com.serenity.integration.repository.EncounterNoteRepository;
import com.serenity.integration.repository.EncounterRepository;
import com.serenity.integration.repository.MedicalRequestRepository;
import com.serenity.integration.repository.PatientRepository;
import com.serenity.integration.repository.VisitRepository;

@Service
public class MedicalRequestService {
    @Autowired
    @Qualifier(value = "hisJdbcTemplate")
    JdbcTemplate hisJdbcTemplate;

    @Autowired
    EncounterNoteRepository encounterNoteRepository;
    static Logger logger = LoggerFactory.getLogger("Medical Request Service");

    @Autowired
    VisitRepository visitRepository;

    @Autowired
    PatientRepository patientRepository;

    @Autowired
    DoctorRepository doctorRepository;

    @Autowired
    EncounterRepository encounterRepository;

    @Autowired
    MedicalRequestRepository medicalRequestRepository;

    public void medicalRequestOPD() {
        Map<String, PatientData> mps = patientRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e));
        Map<String, String> doc = doctorRepository.findHisPractitioners().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e.getSerenityUUid()));
        String query = """
                                        select

                	pm.PatientMedicine_ID uuid,

                	pm.EntryDate created_at,

                	pm.EntryDate updated_at,

                	pm.EntryDate authored_on,

                	IFNULL(im.TypeName, pm.MedicineName) name,

                	"outpatient" category,

                	pm.Medicine_ID code,

                	null date,

                	pm.Remarks notes,

                	null intended_dispenser,

                	"routine" priority,

                	"completed" status,

                	concat(pm.dose, " - ", pm.NoTimesDay, " - ", pm.NoOfDays) dosage_display,

                	null dosage_form,

                	null dosage_route,

                	null dosage_site,

                	null dosage_frequency,

                	null dosage_frequency_unit,

                	null dose,

                	null dose_unit,

                	null dosage_strength,

                	null dosage_period,

                	null course_of_therapy,

                	null quantity_to_dispense,

                	null number_of_refills,

                	null dosage_period_unit,

                	"Nyaho Medical Centre" service_provider_id,

                	null encounter_id,

                	pm.Transaction_ID visit_id,

                	pm.Patient_ID patient_id,

                	pm.Patient_ID mr_number,

                	patient_master.PName patient_full_name,

                	CONCAT(dm.Title, ' ', dm.Name) practitioner_name,

                	pm.DoctorID practitioner_id

                from

                	patient_medicine pm

                join doctor_master dm on dm.Doctor_ID = pm.DoctorID

                join patient_master on patient_master.Patient_ID = pm.Patient_ID

                left join f_itemmaster im on

                	pm.Medicine_ID = im.ItemID

                where

                	pm.IsChange = 0

                	and pm.isReject = 0
                                    """;

        List<Encounter> encounters = new ArrayList<>();
        List<MedicalRequest> requests = new ArrayList<>();
        List<Visits> visits = new ArrayList<>();

        SqlRowSet set = hisJdbcTemplate.queryForRowSet(query);
        while (set.next()) {
            String patientMr = set.getString("patient_id");
            String date = set.getString("created_at");
            String doctor = set.getString("practitioner_id");
            Optional<Encounter> ecounter = encounterRepository.findEcounterByPatientDateDoctor(patientMr, date, doctor);
            if (ecounter.isPresent()) {
                MedicalRequest request = new MedicalRequest();
                request.setUuid(UUID.randomUUID().toString());
                request.setCreatedAt(set.getString("created_at"));
                request.setAuthoredOn(set.getString("authored_on"));
                request.setName(set.getString("name"));
                request.setCategory(set.getString("category"));
                request.setCategory(set.getString(query));
                request.setCode(set.getString("code"));
                request.setNotes(cleanString(set.getString("notes")));
                request.setPriority(set.getString("priority"));
                request.setStatus(set.getString("status"));
                request.setDosageDisplay(set.getString("dosage_display"));
                request.setServiceProviderId("161380e9-22d3-4627-a97f-0f918ce3e4a9");
                request.setServiceProviderName("Nyaho Medical Centre");
                try {
                    request.setPatientId(mps.get(set.getString("patient_id")).getUuid());
                } catch (Exception e) {
                    logger.info("patient not found");
                }
                try {

                    request.setPractitionerId(doc.get(set.getString("practitioner_id")));
                    request.setPractitionerName(set.getString("practitioner_name"));
                } catch (Exception e) {
                    logger.info("doctor not found");
                }

                request.setEncounterId(ecounter.get().getUuid());
                requests.add(request);
            } else {
                MedicalRequest request = new MedicalRequest();
                request.setUuid(UUID.randomUUID().toString());
                request.setCreatedAt(set.getString("created_at"));
                request.setAuthoredOn(set.getString("authored_on"));
                request.setName(set.getString("name"));
                request.setCategory(set.getString("category"));
                request.setCategory(set.getString(query));
                request.setCode(set.getString("code"));
                request.setNotes(set.getString("notes"));
                request.setPriority(set.getString("priority"));
                request.setStatus(set.getString("status"));
                request.setDosageDisplay(set.getString("dosage_display"));
                request.setServiceProviderId("161380e9-22d3-4627-a97f-0f918ce3e4a9");
                request.setServiceProviderName("Nyaho Medical Centre");
                try {
                    request.setPatientId(mps.get(set.getString("patient_id")).getUuid());
                } catch (Exception e) {
                    logger.info("patient not found");
                }
                try {

                    request.setPractitionerId(doc.get(set.getString("practitioner_id")));
                    request.setPractitionerName(set.getString("practitioner_name"));
                } catch (Exception e) {
                    logger.info("doctor not found");
                }

                request.setEncounterId(UUID.randomUUID().toString());
                Encounter encounter = new Encounter(request, mps.get(set.getString("patient_id")), "his");
                Visits visit = new Visits(encounter);
                visits.add(visit);
                encounters.add(encounter);
                requests.add(request);

            }

        }
        saveInBatches(requests, medicalRequestRepository, 2000);
        saveInBatches(encounters, encounterRepository, 2000);
        saveInBatches(visits, visitRepository, 2000);
    }

    public void medicalRequestIPD() {
        Map<String, PatientData> mps = patientRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e));
        Map<String, String> doc = doctorRepository.findHisPractitioners().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e.getSerenityUUid()));
        String query = """
                        Select om.EntryID uuid,

                  om.EntryDate created_at,

                  case

                    when om.UpdateDateTime is null then om.EntryDate

                    else concat(om.UpdateDateTime, ' 00:00:00')

                  end updated_at,

                  om.EntryDate authored_on,

                  om.MedicineName name,

                  "inpatient" category,

                  om.MedicineID code,

                  null date,

                  om.Remark notes,

                  null intended_dispenser,

                  "routine" priority,

                  case

                    when IFNULL(id.ReceiveQty, om.ReqQty) = 0 then "draft"

                    when (

                      IFNULL(id.ReceiveQty, om.ReqQty) - ifnull(

                        (

                          select SUM(Qty)

                          from cpoe_medication_record mr

                          where itemID = om.MedicineID

                            and TransactionID = pmh.Transaction_ID

                            and IndentNo = om.IndentNo

                        ),

                        0

                      )

                    ) = 0 then "completed"

                    when DATE(om.Duration) < DATE(NOW()) then "ended"

                    when ifnull(

                      (

                        select STATUS

                        from cpoe_medication_record mr

                        where itemID = om.MedicineID

                          and TransactionID = pmh.Transaction_ID

                          and IndentNo = om.IndentNo

                        order by id desc

                        limit 1

                      ), 0

                    ) = 0 then "active"

                    when (

                      select STATUS

                      from cpoe_medication_record mr

                      where itemID = om.MedicineID

                        and TransactionID = pmh.Transaction_ID

                        and IndentNo = om.IndentNo

                      order by id desc

                      limit 1

                    ) = 2 then "stopped"

                    else "unknown"

                  end status,

                  concat(om.Dose, " - ", om.Timing, " - ", om.Duration) dosage_display,

                  null dosage_form,

                  null dosage_route,

                  null dosage_site,

                  null dosage_frequency,

                  null dosage_frequency_unit,

                  null dose,

                  null dose_unit,

                  null dosage_strength,

                  null dosage_period,

                  null course_of_therapy,

                  null quantity_to_dispense,

                  null number_of_refills,

                  null dosage_period_unit,

                  null encounter_id,

                  om.TransactionID visit_id,

                  pmh.Patient_ID patient_id,

                  CONCAT(dm.Title, ' ', dm.Name) practitioner_name,

                  pmh.Doctor_ID practitioner_id

                from orderset_medication om

                  inner join patient_medical_history pmh on pmh.Transaction_ID = om.TransactionID

                  inner join doctor_master dm on dm.Doctor_ID = pmh.Doctor_ID

                  inner join patient_master on patient_master.Patient_ID = pmh.Patient_ID

                  left outer join f_indent_detail_patient id on om.IndentNo = id.IndentNo

                  and om.MedicineID = id.ItemId

                  left outer join f_salesdetails sd on sd.IndentNo = id.IndentNo

                  and sd.ItemID = id.ItemId

                  and sd.TrasactionTypeID = '3'

                  where  om.Remark !=''

                order by om.EntryDate desc LIMIT 500
                    """;

        List<Encounter> encounters = new ArrayList<>();
        List<MedicalRequest> requests = new ArrayList<>();
        List<Visits> visits = new ArrayList<>();

        SqlRowSet set = hisJdbcTemplate.queryForRowSet(query);
        while (set.next()) {
            logger.info("fetching");

            String patientMr = set.getString("patient_id");
            String date = set.getString("created_at");
            String doctor = set.getString("practitioner_id");
            Optional<Encounter> ecounter = encounterRepository.findEcounterByPatientDateDoctor(patientMr, date, doctor);
            if (ecounter.isPresent()) {
                MedicalRequest request = new MedicalRequest();
                request.setUuid(UUID.randomUUID().toString());
                request.setCreatedAt(set.getString("created_at"));
                request.setAuthoredOn(set.getString("authored_on"));
                request.setName(set.getString("name"));
                request.setCategory(set.getString("category"));
                request.setCode(set.getString("code"));
                request.setNotes(cleanString(set.getString("notes")));
                request.setPriority(set.getString("priority"));
                request.setStatus(set.getString("status"));
                request.setDosageDisplay(set.getString("dosage_display"));
                request.setServiceProviderId("161380e9-22d3-4627-a97f-0f918ce3e4a9");
                request.setServiceProviderName("Nyaho Medical Centre");
                try {
                    request.setPatientId(mps.get(set.getString("patient_id")).getUuid());
                } catch (Exception e) {
                    logger.info("patient not found");
                }
                try {

                    request.setPractitionerId(doc.get(set.getString("practitioner_id")));
                    request.setPractitionerName(set.getString("practitioner_name"));
                } catch (Exception e) {
                    logger.info("doctor not found");
                }

                request.setEncounterId(ecounter.get().getUuid());
                requests.add(request);
            } else {
                MedicalRequest request = new MedicalRequest();
                request.setUuid(UUID.randomUUID().toString());
                request.setCreatedAt(set.getString("created_at"));
                request.setAuthoredOn(set.getString("authored_on"));
                request.setName(set.getString("name"));
                request.setCategory(set.getString("category"));
                request.setCode(set.getString("code"));
                request.setNotes(set.getString("notes"));
                request.setPriority(set.getString("priority"));
                request.setStatus(set.getString("status"));
                request.setDosageDisplay(set.getString("dosage_display"));
                request.setServiceProviderId("161380e9-22d3-4627-a97f-0f918ce3e4a9");
                request.setServiceProviderName("Nyaho Medical Centre");
                try {
                    request.setPatientId(mps.get(set.getString("patient_id")).getUuid());
                } catch (Exception e) {
                    logger.info("patient not found");
                }
                try {

                    request.setPractitionerId(doc.get(set.getString("practitioner_id")));
                    request.setPractitionerName(set.getString("practitioner_name"));
                } catch (Exception e) {
                    logger.info("doctor not found");
                }

                request.setEncounterId(UUID.randomUUID().toString());
                Encounter encounter = new Encounter(request, mps.get(set.getString("patient_id")), "his");
                Visits visit = new Visits(encounter);
                visits.add(visit);
                encounters.add(encounter);
                requests.add(request);

            }

        }
        saveInBatches(requests, medicalRequestRepository, 100);
     //   saveInBatches(encounters, encounterRepository, 2000);
       // saveInBatches(visits, visitRepository, 2000);
    }

    public static <T> void saveInBatches(Collection<T> items, CrudRepository<T, ?> repository, int batchSize) {
        if (items == null || items.isEmpty()) {
            logger.warn("No items to save");
            return;
        }

        List<T> batch = new ArrayList<>(batchSize);
        AtomicInteger totalProcessed = new AtomicInteger(0);
        AtomicInteger batchNumber = new AtomicInteger(1);

        try {
            for (T item : items) {
                batch.add(item);

                if (batch.size() >= batchSize) {
                    processBatch(batch, repository, batchNumber.get(), totalProcessed, items.size());
                    batch = new ArrayList<>(batchSize);
                    batchNumber.incrementAndGet();
                }
            }

            // Process remaining items
            if (!batch.isEmpty()) {
                processBatch(batch, repository, batchNumber.get(), totalProcessed, items.size());
            }

            logger.info("Batch processing completed. Total items processed: {}", totalProcessed.get());

        } catch (Exception e) {
            logger.error("Error during batch processing at batch {}: {}", batchNumber.get(), e.getMessage());
            throw new RuntimeException("Failed to process batch " + batchNumber.get(), e);
        }
    }

    public static <T> void processBatch(List<T> batch, CrudRepository<T, ?> repository,
            int batchNumber, AtomicInteger totalProcessed, int totalItems) {
        try {
            repository.saveAll(batch);
            totalProcessed.addAndGet(batch.size());

            logger.info("Processed batch {}: {} items. Progress: {}/{} ({}%)",
                    batchNumber,
                    batch.size(),
                    totalProcessed.get(),
                    totalItems,
                    calculateProgress(totalProcessed.get(), totalItems));

        } catch (Exception e) {
            logger.error("Error saving batch {}: {}", batchNumber, e.getMessage());
            throw e;
        }
    }

    private static double calculateProgress(int processed, int total) {
        return Math.round((double) processed / total * 100 * 100.0) / 100.0;
    }

    public String cleanString(String input) {
        if (input == null)
            return null;
        return input.replace("\0", "")
                .replace("\u0000", "");
    }

}