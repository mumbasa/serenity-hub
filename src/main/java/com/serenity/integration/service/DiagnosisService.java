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

import com.serenity.integration.models.Diagnosis;
import com.serenity.integration.models.Encounter;
import com.serenity.integration.models.EncounterNote;
import com.serenity.integration.models.PatientData;
import com.serenity.integration.repository.DiagnosisRepository;
import com.serenity.integration.repository.DoctorRepository;
import com.serenity.integration.repository.EncounterNoteRepository;
import com.serenity.integration.repository.EncounterRepository;
import com.serenity.integration.repository.PatientRepository;
import com.serenity.integration.repository.VisitRepository;

@Service
public class DiagnosisService {

    @Autowired
    EncounterRepository encounterRepository;

    @Autowired
    DiagnosisRepository diagnosisRepository;

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
    @Qualifier("serenityJdbcTemplate")
    JdbcTemplate serenityJdbcTemplate;

    @Autowired
    VisitRepository visitRepository;
    Logger logger = LoggerFactory.getLogger(this.getClass().getCanonicalName());

    public int getProvisionalDiagnosis(Map<String, PatientData> mps, Map<String, String> doc, int batch) {
        List<Diagnosis> diagnosises = new ArrayList<>();

        String sqlQuery = """
                select
                	cp.Transaction_ID visit_id,
                	cp.ID 'uuid',
                	cp.CreatedDate created_at,
                	cp.CreatedDate updated_at,
                	cp.ProvisionalDiagnosis 'condition',
                	case
                		when pmh.`Type` = 'IPD' then "admission-diagnosis"
                		else "chief-complaint"
                	end role,
                	1 'rank',
                	null code,
                	'UNKNOWN' system,
                	'provisional' status,
                	null note,
                	case
                		when dm.Doctor_ID is not null then concat(dm.Title, " ", dm.Name)
                		else concat(em.Title, " ", em.Name)
                	end practitioner_name,
                	case
                		when dm.Doctor_ID is not null then dm.Doctor_ID
                		else em.Employee_ID
                	end practitioner_id
                from
                	cpoe_patientdiagnosis cp
                inner join employee_master em on em.Employee_ID = cp.CreatedBy
                inner join patient_medical_history pmh on pmh.Transaction_ID = cp.Transaction_ID
                left join doctor_master dm on dm.Doctor_ID = pmh.Doctor_ID
                where cp.ProvisionalDiagnosis != ''  LIMIT ?,200;
                                """;
        SqlRowSet set = hisJdbcTemplate.queryForRowSet(sqlQuery, batch);
        while (set.next()) {
            Diagnosis diagnosis = new Diagnosis();
            diagnosis.setUuid(UUID.randomUUID().toString());
            diagnosis.setCreatedAt(set.getString("created_at"));
            diagnosis.setCondition(set.getString("condition"));
            diagnosis.setCode(set.getString("code"));
            diagnosis.setPractitionerId(doc.get(set.getString("practitioner_id")));
            diagnosis.setPractitionerName(set.getString("practitioner_name"));
            diagnosis.setRole(set.getString("role"));
            diagnosis.setSystem(set.getString("system"));
            diagnosis.setVisitId(set.getString("visit_id"));
            diagnosis.setRank(set.getInt("rank"));

            diagnosises.add(diagnosis);

        }
        logger.info("saving digas");
        diagnosisRepository.saveAll(diagnosises);
        /// populateWithVisits();
        /// \\
        ///
        return 1;
    }

    public void provisionalDiagnosisThread() {
        logger.info("kooooooooooooooading");
        String sql = """
                select
                count(*) from cpoe_patientdiagnosis cp

                where cp.ProvisionalDiagnosis != ''
                """;
        long dataSize = hisJdbcTemplate.queryForObject(sql, Long.class);
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            List<Future<Integer>> futures = executorService.invokeAll(submitTask2(200, dataSize));
            for (Future<Integer> future : futures) {
                System.out.println("future.get = " + future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        executorService.shutdown();
        System.err.println("patiend count is " + dataSize);

    }

    public Set<Callable<Integer>> submitTask2(int batchSize, long rows) {
        Map<String, PatientData> mps = patientRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e));
        Map<String, String> doc = doctorRepository.findHisPractitioners().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e.getSerenityUUid()));

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
                System.err.println("Batch no " + batchNumber);

                try {
                    getProvisionalDiagnosis(mps, doc, startIndex);
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

    public void getICD10Diagnosis() {
        Map<String, PatientData> mps = patientRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e));
        Map<String, String> doc = doctorRepository.findHisPractitioners().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e.getSerenityUUid()));

        String sqlCount = """
                select count(*) from cpoe_10cm_patient ccp
                """;
        @SuppressWarnings("null")
        int rows = hisJdbcTemplate.queryForObject(sqlCount, Integer.class);
        logger.info(rows + " number of rows");
        int totalSize = rows;
        int batches = (totalSize + 100 - 1) / 100; // Ceiling division

        for (int i = 0; i < batches; i++) {
            List<Diagnosis> diagnosises = new ArrayList<>();

            int startIndex = i * 1000;
            int endIndex = Math.min(startIndex + 1000, totalSize);

            String sqlQuery = """
                          select
                    	ccp.Transaction_ID visit_id,
                    	ccp.id uuid,
                    	ccp.EntDate created_at,
                    	ccp.EntDate updated_at,
                    	icd.WHO_Full_Desc 'condition',
                    	case
                    		when pmh.`Type` = 'IPD' then "admission-diagnosis"
                    		else "chief-complaint"
                    	end role,
                    	1 rank,
                    	icd.ICD10_Code code,
                    	"ICD-10" system,
                    	"confirmed" status,
                    	null note,
                    	case
                    		when dm.Doctor_ID is not null then concat(dm.Title, " ", dm.Name)
                    		else concat(em.Title, " ", em.Name)
                    	end practitioner_name,
                    	case
                    		when dm.Doctor_ID is not null then dm.Doctor_ID
                    		else em.Employee_ID
                    	end practitioner_id
                    from cpoe_10cm_patient ccp
                    inner join icd_10_new icd on ccp.icd_id = icd.ID
                    inner join employee_master em on em.Employee_ID = ccp.UserID
                    inner join patient_medical_history pmh on pmh.Transaction_ID = ccp.Transaction_ID
                    left join doctor_master dm on dm.Doctor_ID = pmh.Doctor_ID  LIMIT ?,100;
                                            """;
            SqlRowSet set = hisJdbcTemplate.queryForRowSet(sqlQuery, startIndex);
            while (set.next()) {
                Diagnosis diagnosis = new Diagnosis();
                diagnosis.setUuid(UUID.randomUUID().toString());
                diagnosis.setCreatedAt(set.getString("created_at"));
                diagnosis.setCondition(set.getString("condition"));
                diagnosis.setCode(set.getString("code"));
                diagnosis.setPractitionerId(doc.get(set.getString("practitioner_id")));
                diagnosis.setPractitionerName(set.getString("practitioner_name"));
                diagnosis.setRole(set.getString("role"));
                diagnosis.setVisitId(set.getString("visit_id"));
                diagnosis.setSystem(set.getString("system"));
                diagnosis.setRank(set.getInt("rank"));

                diagnosises.add(diagnosis);

            }
            diagnosisRepository.saveAll(diagnosises);
        }
    }

    public void getProvisionalDiagnosis() {

        Map<String, String> doc = doctorRepository.findHisPractitioners().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e.getSerenityUUid()));

        String sqlCount = """
                select

                 count(*)

                 from
                 	cpoe_patientdiagnosis cp

                 where cp.ProvisionalDiagnosis != '' ;
                 """;
        @SuppressWarnings("null")
        int rows = hisJdbcTemplate.queryForObject(sqlCount, Integer.class);
        logger.info(rows + " number of rows");
        int totalSize = rows;
        int batches = (totalSize + 1000 - 1) / 1000; // Ceiling division

        for (int i = 0; i < batches; i++) {
            List<Diagnosis> diagnosises = new ArrayList<>();

            int startIndex = i * 1000;
            int endIndex = Math.min(startIndex + 1000, totalSize);

            String sqlQuery = """
                            select
                    	cp.Transaction_ID visit_id,
                    	cp.ID 'uuid',
                    	cp.CreatedDate created_at,
                    	cp.CreatedDate updated_at,
                    	cp.ProvisionalDiagnosis 'condition',
                    	case
                    		when pmh.`Type` = 'IPD' then "admission-diagnosis"
                    		else "chief-complaint"
                    	end role,
                    	1 'rank',
                    	null code,
                    	'UNKNOWN' system,
                    	'provisional' status,
                    	null note,
                    	case
                    		when dm.Doctor_ID is not null then concat(dm.Title, " ", dm.Name)
                    		else concat(em.Title, " ", em.Name)
                    	end practitioner_name,
                    	case
                    		when dm.Doctor_ID is not null then dm.Doctor_ID
                    		else em.Employee_ID
                    	end practitioner_id
                    from
                    	cpoe_patientdiagnosis cp
                    inner join employee_master em on em.Employee_ID = cp.CreatedBy
                    inner join patient_medical_history pmh on pmh.Transaction_ID = cp.Transaction_ID
                    left join doctor_master dm on dm.Doctor_ID = pmh.Doctor_ID
                    where cp.ProvisionalDiagnosis != ''  LIMIT ?,1000
                                                """;
            SqlRowSet set = hisJdbcTemplate.queryForRowSet(sqlQuery, startIndex);
            while (set.next()) {
                Diagnosis diagnosis = new Diagnosis();
                diagnosis.setUuid(UUID.randomUUID().toString());
                diagnosis.setCreatedAt(set.getString("created_at"));
                diagnosis.setCondition(set.getString("condition"));
                diagnosis.setCode(set.getString("code"));
                diagnosis.setPractitionerId(doc.get(set.getString("practitioner_id")));
                diagnosis.setPractitionerName(set.getString("practitioner_name"));
                diagnosis.setRole(set.getString("role"));
                diagnosis.setVisitId(set.getString("visit_id"));
                diagnosis.setSystem(set.getString("system"));
                diagnosis.setRank(set.getInt("rank"));

                diagnosises.add(diagnosis);

            }
            diagnosisRepository.saveAll(diagnosises);
        }
    }

    public void getNursingDiagnosis() {
        Map<String, PatientData> mps = patientRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e));
        Map<String, String> doc = doctorRepository.findHisPractitioners().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e.getSerenityUUid()));

        String sqlCount = """
                                           select count(*)from
                nursingprogress np

                                                               """;
        @SuppressWarnings("null")
        int rows = hisJdbcTemplate.queryForObject(sqlCount, Integer.class);
        logger.info(rows + " number of rows");
        int totalSize = rows;
        int batches = (totalSize + 1000 - 1) / 1000; // Ceiling division

        for (int i = 0; i < batches; i++) {
            List<Diagnosis> diagnosises = new ArrayList<>();

            int startIndex = i * 1000;
            int endIndex = Math.min(startIndex + 1000, totalSize);

            String sqlQuery = """
                                select
                    	np.TransactionID visit_id,
                    	np.ID uuid,
                    	np.Createddatetime created_at,
                    	np.Createddatetime updated_at,
                    	np.NursingDiagnosis 'condition',
                    	"nursing-diagnosis" role,
                    	1 rank,
                    	null code,
                    	'UNKNOWN' system,
                    	'provisional' status,
                    	null note,
                    	concat(em.Title, ' ', em.Name) practitioner_name,
                    	em.Employee_ID practitioner_id
                    from
                    	nursingprogress np
                    inner join employee_master em on
                    	np.CreateUserID = em.Employee_ID  LIMIT ?,1000;
                                                    """;
            SqlRowSet set = hisJdbcTemplate.queryForRowSet(sqlQuery, startIndex);
            while (set.next()) {
                Diagnosis diagnosis = new Diagnosis();
                diagnosis.setUuid(UUID.randomUUID().toString());
                diagnosis.setCreatedAt(set.getString("created_at"));
                diagnosis.setCondition(set.getString("condition"));
                diagnosis.setCode(set.getString("code"));
                diagnosis.setPractitionerId(doc.get(set.getString("practitioner_id")));
                diagnosis.setPractitionerName(set.getString("practitioner_name"));
                diagnosis.setRole(set.getString("role"));
                diagnosis.setStatus(set.getString("status"));
                diagnosis.setVisitId(set.getString("visit_id"));
                diagnosis.setSystem(set.getString("system"));
                diagnosis.setRank(set.getInt("rank"));
                diagnosises.add(diagnosis);

            }
            diagnosisRepository.saveAll(diagnosises);
        }
    }

    public void populateWithVisits() {
        String sql = """
                        update diagnosis m
                set patientid = e.patientid,visitid=e.uuid
                from visits e
                where e.externalid = m.visit_id

                        """;
        vectorJdbcTemplate.update(sql);

    }

    public void saveDiagnoses(List<Diagnosis> diagnoses) {

        String sql = """
                        INSERT INTO public.diagnoses
                (created_at,  id, "uuid", "condition", "role",
                "system", status, note, practitioner_name, patient_id,
                practitioner_id, visit_id )
                VALUES(to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS') ,?, uuid(?), ?, ?, ?, ?, ?, ?, uuid(?), uuid(?), uuid(?))
                        """;
        serenityJdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Diagnosis diagnosis = diagnoses.get(i);
                ps.setString(1, diagnosis.getCreatedAt().replaceAll("T|Z", " "));
                ps.setLong(2, diagnosis.getId());
                ps.setString(3, diagnosis.getUuid());
                ps.setString(4, diagnosis.getCondition());
                ps.setString(5, diagnosis.getRole());
                ps.setString(6, diagnosis.getSystem());
                ps.setString(7, diagnosis.getStatus());
                ps.setString(8, diagnosis.getNote());
                ps.setString(9, diagnosis.getPractitionerName());
                ps.setString(10, diagnosis.getPatientId());
                ps.setString(11, diagnosis.getPractitionerId());
                ps.setString(12, diagnosis.getVisitId());

            }

            @Override
            public int getBatchSize() {
                // TODO Auto-generated method stub
                return diagnoses.size();
            }

        });

    }

    public void migrationThread() {
        logger.info("kooooooooooooooading");

        long dataSize = diagnosisRepository.count();
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            List<Future<Integer>> futures = executorService.invokeAll(submitTasker(1000, dataSize));
            for (Future<Integer> future : futures) {
                System.out.println("future.get = " + future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        executorService.shutdown();
        System.err.println("patiend count is " + dataSize);

    }

    public Set<Callable<Integer>> submitTasker(int batchSize, long rows) {

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
                System.err.println("Batch no " + batchNumber);

                try {
                    saveDiagnoses(diagnosisRepository.findBySystemLimit(startIndex));
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

    public void getLegacyDiagnosis(int size, int batchSize) {
        List<Diagnosis> diagnoses = new ArrayList<>();
        String sql = """
                SELECT * FROM encounter_diagnosis OFFSET ? LIMIT ?
                """;
        SqlRowSet set = legJdbcTemplate.queryForRowSet(sql, size, batchSize);
        while (set.next()) {
            // System.err.println(set.getString("role"));
            Diagnosis diagnosis = new Diagnosis();
            diagnosis.setCode(set.getString("code"));
            diagnosis.setRole(set.getString("role"));
            diagnosis.setCondition(set.getString("condition"));
            diagnosis.setRank(set.getInt("rank"));
            diagnosis.setEncounterId(set.getString("encounter_id"));
            diagnosis.setStatus(set.getString("status"));
            diagnosis.setNote(set.getString("note"));
            diagnosis.setUuid(set.getString("id"));
            diagnosis.setSystem("opd");
            // diagnosis.setExternalId(set.getString("uuid"));
            diagnosis.setCreatedAt(set.getString("created_at"));
            diagnoses.add(diagnosis);

        }
        diagnosisRepository.saveAll(diagnoses);
        System.err.println("added to database");

    }

    public void getLegacyDiagnosis() {

        String sqlCount = """
                                           select count(*) from
                encounter_diagnosis

                                                               """;
        @SuppressWarnings("null")
        int rows = legJdbcTemplate.queryForObject(sqlCount, Integer.class);
        logger.info(rows + " number of rows");
        int totalSize = rows;
        int batches = (totalSize + 1000 - 1) / 1000; // Ceiling division

        for (int i = 0; i < batches; i++) {
            List<Diagnosis> diagnoses = new ArrayList<>();

            int startIndex = i * 1000;

            String sqlQuery = """
                    SELECT * FROM encounter_diagnosis OFFSET ? LIMIT 1000;
                                        """;
            SqlRowSet set = legJdbcTemplate.queryForRowSet(sqlQuery, startIndex);
            while (set.next()) {
                Diagnosis diagnosis = new Diagnosis();
                diagnosis.setCode(set.getString("code"));
                diagnosis.setRole(set.getString("role"));
                diagnosis.setCondition(set.getString("condition"));
                diagnosis.setRank(set.getInt("rank"));
                diagnosis.setEncounterId(set.getString("encounter_id"));
                diagnosis.setStatus(set.getString("status"));
                diagnosis.setNote(set.getString("note"));
                diagnosis.setUuid(set.getString("id"));
                diagnosis.setSystem("opd");
                // diagnosis.setExternalId(set.getString("uuid"));
                diagnosis.setCreatedAt(set.getString("created_at"));
                diagnoses.add(diagnosis);

            }
            diagnosisRepository.saveAll(diagnoses);
        }

        updateWithData();
    }

    public void getDignosisLegacyThread() {
        String sqlCount = "SELECT count(*) FROM encounter_diagnosis";
        @SuppressWarnings("null")
        int rows = legJdbcTemplate.queryForObject(sqlCount, Integer.class);
        logger.info("Legacy row => " + rows);
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            List<Future<Integer>> futures = executorService.invokeAll(insertLegacyData(2000, rows));
            for (Future<Integer> future : futures) {
                System.out.println("future.get = " + future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        executorService.shutdown();
        System.err.println("patiend count is " + rows);

    }

    public Set<Callable<Integer>> insertLegacyData(int batchSize, long rows) {

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
                System.err.println("Batch no " + batchNumber);

                getLegacyDiagnosis(startIndex, batchSize);

                return 1;
            });
        }

        return callables;
    }

    public void updateWithData() {
        String sql = """
                        update diagnosis
                set visitid = e.visit_id,patientid =e.patient_id,practitionerid =e.assigned_to_id,practitionername=e.assigned_to_name
                from encounter e
                where system = 'opd' and
                encounterid=e.uuid;
                        """;
        legJdbcTemplate.update(sql);
    }
}
