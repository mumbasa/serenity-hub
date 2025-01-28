package com.serenity.integration.service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

import com.serenity.integration.models.Encounter;
import com.serenity.integration.models.EncounterNote;
import com.serenity.integration.models.PatientData;
import com.serenity.integration.repository.DoctorRepository;
import com.serenity.integration.repository.EncounterNoteRepository;
import com.serenity.integration.repository.EncounterRepository;
import com.serenity.integration.repository.PatientRepository;
import com.serenity.integration.repository.VisitRepository;

@Service
public class EncounterService {

    @Autowired
    EncounterRepository encounterRepository;

    @Autowired
    EncounterNoteRepository encounterNoteRepository;

    @Autowired
    @Qualifier("serenityJdbcTemplate")
    JdbcTemplate serenityJdbcTemplate;

    @Autowired
    @Qualifier(value = "hisJdbcTemplate")
    JdbcTemplate hisJdbcTemplate;

    @Autowired
    @Qualifier(value = "legJdbcTemplate")
    JdbcTemplate legJdbcTemplate;

    @Autowired
    @Qualifier(value = "vectorJdbcTemplate")
    JdbcTemplate vectorJdbcTemplate;

    @Autowired
    PatientRepository patientRepository;

    @Autowired
    DoctorRepository doctorRepository;

    @Autowired
    VisitRepository visitRepository;
    Logger logger = LoggerFactory.getLogger(this.getClass().getCanonicalName());

    public void generateOPDEncounter() {
        int rows = visitRepository.countByEncounterClass("ambulatory");
        logger.info(rows + " number of rows");
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> futures = executorService.invokeAll(submitTask2(100, 1000));
            for (Future<Integer> future : futures) {
                System.out.println("future.get = " + future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        executorService.shutdown();
        System.err.println("patiend count is " + rows);

    }

    public Set<Callable<Integer>> submitTask2(int batchSize, long rows) {

        Set<Callable<Integer>> callables = new HashSet<>();
        int totalSize = (int) rows;
        int batches = (totalSize + batchSize - 1) / batchSize; // Ceiling division

        for (int i = 0; i < batches; i++) {
            final int batchNumber = i; // For use in lambda

            callables.add(() -> {
                int startIndex = batchNumber * batchSize;
                int endIndex = Math.min(startIndex + batchSize, totalSize);
                logger.debug("Processing batch {}/{}, indices [{}]",
                        batchNumber + 1, batches, startIndex);
                List<Encounter> notes = encounterRepository.getfirst100k(startIndex);

                try {
                    saveEncounters(notes);
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                    logger.info("error adding note");

                }

                return 1;
            });
        }

        return callables;
    }


    public Set<Callable<Integer>> submitLegacyTask2(int batchSize, long rows) {

        Set<Callable<Integer>> callables = new HashSet<>();
        int totalSize = (int) rows;
        int batches = (totalSize + batchSize - 1) / batchSize; // Ceiling division

        for (int i = 0; i < batches; i++) {
            final int batchNumber = i; // For use in lambda

            callables.add(() -> {
                int startIndex = batchNumber * batchSize;
                int endIndex = Math.min(startIndex + batchSize, totalSize);
                logger.debug("Processing batch {}/{}, indices [{}]",
                        batchNumber + 1, batches, startIndex);
                List<Encounter> notes = encounterRepository.getfirstOPD100k(startIndex);

                try {
                    saveEncounters(notes);
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                    logger.info("error adding note");

                }

                return 1;
            });
        }

        return callables;
    }

    public void insertNote(EncounterNote note) {
        System.err.println("inserting note");
        String sql = "INSERT INTO public.encounter_notes\n" + //
                "(created_at, pk, encounter_id, patient_id," +
                " visit_id, \"uuid\", note, is_formatted, note_type," +
                "is_edited, is_recalled, practitioner_id, encounter_date, practitioner_name," +
                "practitioner_role_type, encounter_type, patient_mr_number)\n" + //
                "VALUES(to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS'), nextval('encounter_notes_pk_seq'::regclass), uuid(?), (select uuid from patients p where external_id =?), uuid(?), uuid(?), ?, false, ?, false, false, uuid(?), to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS'), ?, ?, ?, ?)";

        serenityJdbcTemplate.update(sql, new PreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps) throws SQLException {
                ps.setString(1, note.getEncounterDate().replaceAll("T|Z", " ").strip());
                ps.setString(2, note.getUuid().split(":")[1]);
                ps.setString(3, note.getPatientMrNumber());
                ps.setString(4, note.getUpdatedAt());
                ps.setString(5, UUID.randomUUID().toString());
                ps.setString(6, note.getNote());
                ps.setString(7, note.getNoteType());
                ps.setString(8, note.getPractitionerRoleType().equalsIgnoreCase("unknown") ? null
                        : note.getPractitionerRoleType());
                ps.setString(9, note.getEncounterDate().replaceAll("T|Z", " ").strip());
                ps.setString(10, note.getPractitionerName());
                ps.setString(11, "Doctor");
                ps.setString(12, note.getEncounterType());
                ps.setString(13, note.getPatientMrNumber());

            }

        });

    }

    public int encountersData(int size) {
        List<Encounter> encounters = new ArrayList<>();
        String sql = "select * from encounter e join visits v on date(e.created_at)=date(v.createdat) and e.patient_id=v.patientid and  e.created_at !='0000-00-00' and assigned_to_id is not null OFFSET ? LIMIT 1000";
        SqlRowSet set = vectorJdbcTemplate.queryForRowSet(sql, size);
        while (set.next()) {
            Encounter encounter = new Encounter();
            encounter.setCreatedAt(set.getString(5));
            encounter.setAssignedToId(set.getString(2));
            encounter.setAssignedToName(set.getString("assigned_to_name"));
            encounter.setDisplay(set.getString(8));
            encounter.setEncounterClass(set.getString("encounterclass"));
            encounter.setExternalId(set.getString(11));
            encounter.setExternalSystem(set.getString(12));
            encounter.setLocationId(set.getString(13));
            encounter.setLocationName(set.getString(14));
            encounter.setPatientBirthDate(set.getString("patient_birth_date"));
            encounter.setPatientFullName(set.getString("patient_full_name"));
            encounter.setPatientGender(set.getString("patient_gender"));
            encounter.setPatientMobile(set.getString("patient_mobile"));
            encounter.setPatientId(set.getString("patient_id"));
            encounter.setPatientMrNumber(set.getString("patient_mr_number"));
            encounter.setServiceProviderId("161380e9-22d3-4627-a97f-0f918ce3e4a9");
            encounter.setServiceProviderName("Nyaho Medical Center");
            encounter.setStatus("finished");
            encounter.setVisitId(set.getString(53));
            encounters.add(encounter);
        }

        saveEncounters(encounters);

        return size;

    }

    public void saveEncounters(List<Encounter> notes) {
        String sql = "INSERT INTO public.encounters " + //
                "(created_at,  id,  uuid, encounter_class, status, " +
                "display,  external_id, external_system,  service_provider_id, patient_mr_number," +
                "patient_id, patient_full_name, patient_mobile, patient_birth_date, patient_gender," +
                "encounter_type, practitioner_name, practitioner_id, service_provider_name,  visit_id," +
                "has_prescriptions,has_service_requests)" + //
                "VALUES(to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS'),  ?,  uuid(?),?,?,"
                +
                "'',?, ?,uuid(?),?,uuid(?), ?," +
                "?,to_date(?, 'YYYY-MM-DD'),?,?,?,uuid(?),?, uuid(?),?,?)";

        serenityJdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                try {
                    ps.setString(1, notes.get(i).getStartedAt().replaceAll("T|Z", " ").strip());
                } catch (Exception e) {
                    ps.setString(1, notes.get(i).getCreatedAt() + " 14:55:37");

                }
                ps.setLong(2, notes.get(i).getId());
                ps.setString(3, notes.get(i).getUuid());
                ps.setString(4, "ambulatory");
                ps.setString(5, "finished");
                ps.setString(6, notes.get(i).getExternalId() + "-" + notes.get(i).getUuid());
                ps.setString(7, notes.get(i).getExternalSystem());
                ps.setString(8, "161380e9-22d3-4627-a97f-0f918ce3e4a9");
                ps.setString(9, notes.get(i).getPatientMrNumber());
                ps.setString(10, notes.get(i).getPatientId());
                ps.setString(11, notes.get(i).getPatientFullName());
                ps.setString(12, notes.get(i).getPatientMobile() == null ? "" : notes.get(i).getPatientMobile());
                ps.setString(13, notes.get(i).getPatientBirthDate());
                ps.setString(14, notes.get(i).getPatientGender());

                ps.setString(15, notes.get(i).getEncounterClass());
                ps.setString(16, notes.get(i).getAssignedToName());
                ps.setString(17, notes.get(i).getAssignedToId());

                ps.setString(18, "Nyaho Medical Centre");
                ps.setString(19, notes.get(i).getVisitId());
                ps.setBoolean(20, false);
                ps.setBoolean(21, false);
            }

            @Override
            public int getBatchSize() {
                // TODO Auto-generated method
                return notes.size();
            }

        });
    }

    public void saveEncounter(Encounter notes) {
        String sql = "INSERT INTO public.encounters " + //
                "(created_at,  id,  uuid, encounter_class, status, " +
                "display,  external_id, external_system,  service_provider_id, patient_mr_number," +
                "patient_id, patient_full_name, patient_mobile, patient_birth_date, patient_gender," +
                "encounter_type, practitioner_name, practitioner_id, service_provider_name,  visit_id," +
                "has_prescriptions,has_service_requests)" + //
                "VALUES(to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS'),  nextval('encounters_id_seq'::regclass),  uuid(?),?,?,"
                +
                "'',?, ?,uuid(?),?,uuid(?), ?," +
                "(SELECT mobile from patients where mr_number=?),to_date(?, 'YYYY-MM-DD'),?,?,?,uuid(?),?, uuid(?),?,?)";

        serenityJdbcTemplate.update(sql, new PreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps) throws SQLException {
                // TODO Auto-generated method stub
                ps.setString(1, notes.getCreatedAt().replaceAll("T|Z", " ").strip());
                ps.setString(2, notes.getUuid());
                ps.setString(3, "ambulatory");
                ps.setString(4, "finished");
                ps.setString(5, UUID.randomUUID().toString());
                ps.setString(6, "his");
                ps.setString(7, "161380e9-22d3-4627-a97f-0f918ce3e4a9");
                ps.setString(8, notes.getPatientMrNumber());
                ps.setString(9, notes.getPatientId());
                ps.setString(10, notes.getPatientFullName());
                ps.setString(11, notes.getPatientMrNumber());
                ps.setString(12, notes.getPatientBirthDate());
                ps.setString(13, notes.getPatientGender());

                ps.setString(14, notes.getEncounterClass());
                ps.setString(15, notes.getAssignedToName());
                ps.setString(16, notes.getAssignedToId());

                ps.setString(17, "Nyaho Medical Centre");
                ps.setString(18, notes.getVisitId());
                ps.setBoolean(19, false);
                ps.setBoolean(20, false);

            }

        });

    }

    public void saveEncounter() {
        String sql = "INSERT INTO public.encounters " + //
                "(created_at,  id,  uuid, encounter_class, status, " +
                "display,  external_id, external_system,  service_provider_id, patient_mr_number," +
                "patient_id, patient_full_name, patient_mobile, patient_birth_date, patient_gender," +
                "encounter_type, practitioner_name, practitioner_id, service_provider_name,  visit_id," +
                "has_prescriptions,has_service_requests)" + //
                "VALUES(to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS'),  nextval('encounters_id_seq'::regclass),  uuid(?),?,?,"
                +
                "'',?, ?,uuid(?),?,uuid(?), ?," +
                "(SELECT mobile from patients where mr_number=?),to_date(?, 'YYYY-MM-DD'),?,?,?,uuid(?),?, uuid(?),?,?)";

        serenityJdbcTemplate.update(sql, new PreparedStatementSetter() {
            Encounter notes = encounterRepository.getfirst100k().get(0);

            @Override
            public void setValues(PreparedStatement ps) throws SQLException {
                // TODO Auto-generated method stub
                ps.setString(1, notes.getCreatedAt().replaceAll("T|Z", " ").strip());
                ps.setString(2, notes.getUuid());
                ps.setString(3, "ambulatory");
                ps.setString(4, "finished");
                ps.setString(5, UUID.randomUUID().toString());
                ps.setString(6, "his");
                ps.setString(7, "161380e9-22d3-4627-a97f-0f918ce3e4a9");
                ps.setString(8, notes.getPatientMrNumber());
                ps.setString(9, notes.getPatientId());
                ps.setString(10, notes.getPatientFullName());
                ps.setString(11, notes.getPatientMrNumber());
                ps.setString(12, notes.getPatientBirthDate());
                ps.setString(13, notes.getPatientGender());

                ps.setString(14, notes.getEncounterClass());
                ps.setString(15, notes.getAssignedToName());
                ps.setString(16, notes.getAssignedToId());

                ps.setString(17, "Nyaho Medical Centre");
                ps.setString(18, notes.getVisitId());
                ps.setBoolean(19, false);
                ps.setBoolean(20, false);

            }

        });

    }

    public void encounterthread() {
        logger.info("kooooooooooooooading");
        int dataSize = 1878637;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            List<Future<Integer>> futures = executorService.invokeAll(submitTask2(1000, dataSize));
            for (Future<Integer> future : futures) {
                System.out.println("future.get = " + future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        executorService.shutdown();
        System.err.println("patiend count is " + dataSize);

    }



    public void encounterLegacythread() {
        logger.info("kooooooooooooooading");
        int dataSize = encounterRepository.getOOPCount();
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            List<Future<Integer>> futures = executorService.invokeAll(submitLegacyTask2(1000, dataSize));
            for (Future<Integer> future : futures) {
                System.out.println("future.get = " + future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        executorService.shutdown();
        System.err.println("patiend count is " + dataSize);

    }

    public void encounterOPDthread() {
        logger.info("kooooooooooooooading");
        long dataSize = encounterRepository.count();
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            List<Future<Integer>> futures = executorService.invokeAll(submitTask2(1000, dataSize));
            for (Future<Integer> future : futures) {
                System.out.println("future.get = " + future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        executorService.shutdown();
        System.err.println("patiend count is " + dataSize);

    }

    public void getLegacyEncounters() {

        Map<String, PatientData> patientDataMap = patientRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e));
        Map<String, String> doctorMap = doctorRepository.findHisPractitioners().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e.getSerenityUUid()));

        List<Encounter> encounters = new ArrayList<>();
        String sql = "select * from encounter e join patient p on p.id=e.patient_id";
        SqlRowSet set = legJdbcTemplate.queryForRowSet(sql);
        while (set.next()) {
            System.err.println(set.getString("mr_number")+"-----------------");
            PatientData patient = patientDataMap.get(set.getString("mr_number"));
            Encounter encounter = new Encounter();
            encounter.setUuid(set.getString(5));
            encounter.setExternalId(set.getString(5));
            encounter.setCreatedAt(set.getString(2));
            encounter.setEncounterClass(set.getString("encounter_class"));
            encounter.setPriority(set.getString("priority"));
            encounter.setPatientId(patient.getUuid());
            encounter.setPatientBirthDate(patient.getBirthDate());
            encounter.setPatientFullName(patient.getFullName());
            encounter.setPatientMobile(patient.getMobile());
            encounter.setPatientMrNumber(patient.getMrNumber());
            encounter.setExternalSystem("opd");
            encounter.setDisplay(set.getString("uuid"));
            encounter.setLocationId(set.getString("primary_location_id"));
            encounter.setVisitId(set.getString("visit_id"));
            encounter.setServiceProviderId("161380e9-22d3-4627-a97f-0f918ce3e4a9");
            encounter.setServiceProviderName("Nyaho Medical Centre");
            encounter.setStatus(set.getString(6));
            encounters.add(encounter);
            logger.info("adding encounter");
        }

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            List<Future<Integer>> futures = executorService.invokeAll(submitNote(encounters, 20000));
            for (Future<Integer> future : futures) {
                System.out.println("future.get = " + future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        executorService.shutdown();
        System.err.println("patiend count is ");
        populateWithVisits();

    }

    public Set<Callable<Integer>> submitNote(List<Encounter> notes, int batchSize) {

        Set<Callable<Integer>> callables = new HashSet<>();
        int totalSize = notes.size();
        int batches = (totalSize + batchSize - 1) / batchSize; // Ceiling division

        for (int i = 0; i < batches; i++) {
            final int batchNumber = i; // For use in lambda

            callables.add(() -> {
                int startIndex = batchNumber * batchSize;
                int endIndex = Math.min(startIndex + batchSize, totalSize);
                logger.debug("Processing batch {}/{}, indices [{}]",
                        batchNumber + 1, batches, startIndex);
                encounterRepository.saveAll(notes.subList(startIndex, endIndex));
                return 1;
            });
        }

        return callables;
    }

    public void populateWithVisits() {
        String sql = """
                        update encounter m
                set assigned_to_id = e.practitionerid ,
                assigned_to_name =e.assignedtoname
                from visits e
                where e.externalid = m.visit_id
                and m.assigned_to_id is null and m.external_system ='opd'

                        """;
        vectorJdbcTemplate.update(sql);
        sql= """
                update encounter m
               set visit_id =e.uuid
                from visits e
                where e.externalid = m.visit_id
                 and m.external_system ='opd'
                """;
                vectorJdbcTemplate.update(sql);

        sql ="""
                update encounter m
                set patient_gender = e.gender          
                from patient_information e
                where e.uuid = m.patient_id and m.external_system ='opd'
                """;
                vectorJdbcTemplate.update(sql);

    }

}
