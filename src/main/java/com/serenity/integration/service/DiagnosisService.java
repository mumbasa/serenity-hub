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
                where cp.ProvisionalDiagnosis != ''  LIMIT ?,1000;
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
        long dataSize = 800000;
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
        List<Diagnosis> diagnosises = new ArrayList<>();
        Map<String, PatientData> mps = patientRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e));
        Map<String, String> doc = doctorRepository.findHisPractitioners().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e.getSerenityUUid()));

        String sqlCount = """
                                                        select
                                        count(*)
                                                                   from cpoe_10cm_patient ccp
                inner join icd_10_new icd on ccp.icd_id = icd.ID
                inner join employee_master em on em.Employee_ID = ccp.UserID
                inner join patient_medical_history pmh on pmh.Transaction_ID = ccp.Transaction_ID
                left join doctor_master dm on dm.Doctor_ID = pmh.Doctor_ID
                                                        """;
        @SuppressWarnings("null")
        int rows = hisJdbcTemplate.queryForObject(sqlCount, Integer.class);
        logger.info(rows + " number of rows");
        int totalSize = rows;
        int batches = (totalSize + 10000 - 1) / 10000; // Ceiling division

        for (int i = 0; i < batches; i++) {
            int startIndex = i * 10000;
            int endIndex = Math.min(startIndex + 10000, totalSize);

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
                    left join doctor_master dm on dm.Doctor_ID = pmh.Doctor_ID  LIMIT ?,10000;
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
        List<Diagnosis> diagnosises = new ArrayList<>();
        Map<String, PatientData> mps = patientRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e));
        Map<String, String> doc = doctorRepository.findHisPractitioners().stream()
                .collect(Collectors.toMap(e -> e.getExternalId(), e -> e.getSerenityUUid()));

        String sqlCount = """
                                                                select
                                                count(*)from
                	nursingprogress np
                inner join employee_master em on
                	np.CreateUserID = em.Employee_ID
                                                                """;
        @SuppressWarnings("null")
        int rows = hisJdbcTemplate.queryForObject(sqlCount, Integer.class);
        logger.info(rows + " number of rows");
        int totalSize = rows;
        int batches = (totalSize + 10000 - 1) / 10000; // Ceiling division

        for (int i = 0; i < batches; i++) {
            int startIndex = i * 10000;
            int endIndex = Math.min(startIndex + 10000, totalSize);

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
                    	np.CreateUserID = em.Employee_ID  LIMIT ?,10000;
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

    public void populateWithVisits() {
        String sql = """
                        update diagnosis m
                set patientid = e.patientid,visitid=e.uuid
                from visits e
                where e.externalid = m.visit_id

                        """;
        vectorJdbcTemplate.update(sql);

    }

}
