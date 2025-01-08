package com.serenity.integration.service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import com.serenity.integration.repository.EncounterNoteRepository;
import com.serenity.integration.repository.EncounterRepository;

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

    Logger LOGGER = LoggerFactory.getLogger(this.getClass().getCanonicalName());

    public void encounter(int start, int end) {
   
        List<Encounter> notes = new ArrayList<>();
        String sqlQuery = "SELECT " +
                "    pmh.Transaction_ID AS \"uuid\", " +
                "    DATE_FORMAT(pmh.DateOfVisit, '%Y-%m-%dT%H:%i:%sZ') AS \"created_at\", " +
                "    DATE_FORMAT(pmh.Updatedate, '%Y-%m-%dT%H:%i:%sZ') AS \"updated_at\", " +
                "    CASE " +
                "        WHEN pmh.Type = 'IPD' AND pmh.Admission_Type = 'Emergency' THEN 'emergency' " +
                "        WHEN pmh.Type = 'IPD' AND pmh.Admission_Type <> 'Emergency' THEN 'inpatient-encounter' " +
                "        ELSE 'ambulatory' " +
                "    END AS encounter_class, " +
                "    'finished' AS status, " +
                "    '' AS display, " +
                "    CASE " +
                "        WHEN pmh.Type = 'IPD' AND pmh.Admission_Type = 'Emergency' THEN 'stat' " +
                "        WHEN pmh.Type = 'IPD' AND pmh.Admission_Type <> 'Emergency' THEN 'ASAP' " +
                "        ELSE 'routine' " +
                "    END AS priority, " +
                "    DATE_FORMAT(CAST(CONCAT(CAST(app.Date AS DATE), ' ', CAST(app.Time AS TIME)) AS DATETIME), '%Y-%m-%dT%H:%i:%sZ') AS planned_start, "
                +
                "    DATE_FORMAT(CAST(CONCAT(CAST(app.Date AS DATE), ' ', CAST(app.EndTime AS TIME)) AS DATETIME), '%Y-%m-%dT%H:%i:%sZ') AS planned_end, "
                +
                "    DATE_FORMAT(CAST(CONCAT(CAST(pmh.DateOfVisit AS DATE), ' ', CAST(pmh.Time AS TIME)) AS DATETIME), '%Y-%m-%dT%H:%i:%sZ') AS started_at, "
                +
                "    NULL AS ended_at, " +
                "    pmh.Transaction_ID AS external_id, " +
                "    app.App_ID AS appointment_id, " +
                "    NULL AS location_id, " +
                "    'Nyaho Medical Centre' AS location_name, " +
                "    NULL AS service_type_id, " +
                "    NULL AS service_type_name, " +
                "    NULL AS service_provider_id, " +
                "    pmh.Patient_ID AS patient_mr_number, " +
                "    pmh.Patient_ID AS patient_id, " +
                "    CONCAT(pm.PfirstName, ' ', pm.PLastName) AS patient_full_name, " +
                "    pm.Mobile AS patient_mobile, " +
                "    pm.DOB AS patient_birth_date, " +
                "    pm.Gender AS patient_gender, " +
                "    'departed' AS patient_status, " +
                "    pmh.Doctor_ID AS created_by_id, " +
                "    CONCAT(dm.Title, ' ', dm.Name) AS created_by_name, " +
                "    pmh.Transaction_ID AS user_friendly_id, " +
                "    CONCAT(dm.Title, ' ', dm.Name) AS assigned_to_name, " +
                "    pmh.Doctor_ID AS assigned_to_id, " +
                "    'Nyaho Medical Centre' AS location_name " +
                "FROM " +
                "    patient_medical_history pmh " +
                "INNER JOIN " +
                "    patient_master pm ON pm.Patient_ID = pmh.Patient_ID " +
                "INNER JOIN " +
                "    doctor_master dm ON pmh.Doctor_ID = dm.Doctor_ID " +
                "INNER JOIN " +
                "    f_ledgertransaction lt ON lt.`Transaction_ID` = pmh.`Transaction_ID` " +
                "INNER JOIN " +
                "    appointment app ON app.ledgertnxNo = lt.LedgerTransactionNo LIMIT " + start + "," + end;

        SqlRowSet set = hisJdbcTemplate.queryForRowSet(sqlQuery);
        while (set.next()) {
            Encounter note = new Encounter();
            note.setUuid(set.getString(1));
            note.setCreatedAt((set.getString(2)));
            note.setUpdatedAt(null);
            note.setEncounterClass(set.getString(4));
            note.setStatus(set.getString(5));
            note.setDisplay(null);
            note.setPriority(set.getString(7));
            note.setPlannedStart((set.getString(8)));
            note.setPlannedEnd((set.getString(9)));
            note.setStartedAt((set.getString(10)));
            note.setEndedAt(null);
            note.setExternalSystem("his");
            note.setExternalId(set.getString(12));
            note.setAppointmentId(set.getString(13));
            note.setLocationId(null);
            note.setLocationName(set.getString(15));
            note.setPatientMrNumber(set.getString(19));
            note.setPatientId(set.getString(20));
            note.setPatientFullName(set.getString(21));
            note.setPatientMobile(set.getString(22));
            note.setPatientBirthDate(set.getString(23));
            note.setPatientGender(set.getString(24));
            note.setPatientStatus(set.getString(25));
            note.setCreatedById(set.getString(26));
            note.setCreatedByName(set.getString(27));
            note.setUserFriendlyId(set.getString(28));
            note.setAssignedToName(set.getString(29));
            note.setAssignedToId(set.getString(30));

            notes.add(note);
        }
        int rounds = Math.round(notes.size() / 1000);
        for (int i = 0; i < rounds; i++) {
            LOGGER.info("adding round presenting illness " + rounds);
            try {
                encounterRepository.saveAllAndFlush(notes.subList(i * 1000, (i * 1000) + 1000));
            } catch (Exception e) {

            }

        }

    }

    public void setEncounterNotes() {
        int rounds = 1700000 / 1000;
        for (int i = 0; i <= 1; i++) {
            System.err.println("round note "+ i);
            if(i==0){
            List<EncounterNote> notes = encounterNoteRepository.findOffset(i*rounds, 10000);
            notes.stream().forEach(e-> {
                String id = e.getUuid()+":"+UUID.randomUUID().toString();
                e.setUuid(id);


            });
            for(EncounterNote note : notes){
                try{
                saveEncounter(note);
                try{
                    insertNote(note);

                }catch(Exception e ){

                    e.printStackTrace();
                }
                }catch(Exception e){
System.err.println("error adding encounter");
                }
            }
            //insertData(notes);
            break;
            }

        }

    }

    public void insertData(List<EncounterNote> notes) {
        String sql = "INSERT INTO public.encounter_notes\n" + //
                "(created_at, pk, encounter_id, patient_id," +
                " visit_id, \"uuid\", note, is_formatted, note_type," +
                "is_edited, is_recalled, practitioner_id, encounter_date, practitioner_name," +
                "practitioner_role_type, encounter_type, patient_mr_number)\n" + //
                "VALUES(to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS'), nextval('encounter_notes_pk_seq'::regclass), uuid(?), (select uuid from patients p where external_id =?), uuid(?), uuid(?), ?, false, ?, false, false, uuid(?), to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS'), ?, ?, ?, ?)";

        serenityJdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, notes.get(i).getEncounterDate().replaceAll("T|Z", " ").strip());
                ps.setString(2,notes.get(i).getUuid().split(":")[1]);
                ps.setString(3, notes.get(i).getPatientMrNumber());
                ps.setString(4, notes.get(0).getUpdatedAt());
                ps.setString(5, UUID.randomUUID().toString());
                ps.setString(6, notes.get(i).getNote());
                ps.setString(7, notes.get(i).getNoteType());
                ps.setString(8, notes.get(i).getPractitionerRoleType().equalsIgnoreCase("unknown")?null:notes.get(i).getPractitionerRoleType());
                ps.setString(9, notes.get(i).getEncounterDate().replaceAll("T|Z", " ").strip());
                ps.setString(10, notes.get(i).getPractitionerName());
                ps.setString(11, "Doctor");
                ps.setString(12, notes.get(i).getEncounterType());
                ps.setString(13, notes.get(i).getPatientMrNumber());

            }

            @Override
            public int getBatchSize() {
                return notes.size();
            }

        });

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
                ps.setString(2,note.getUuid().split(":")[1]);
                ps.setString(3, note.getPatientMrNumber());
                ps.setString(4, note.getUpdatedAt());
                ps.setString(5, UUID.randomUUID().toString());
                ps.setString(6, note.getNote());
                ps.setString(7, note.getNoteType());
                ps.setString(8, note.getPractitionerRoleType().equalsIgnoreCase("unknown")?null:note.getPractitionerRoleType());
                ps.setString(9, note.getEncounterDate().replaceAll("T|Z", " ").strip());
                ps.setString(10, note.getPractitionerName());
                ps.setString(11, "Doctor");
                ps.setString(12, note.getEncounterType());
                ps.setString(13, note.getPatientMrNumber());

            }

         

        });

    }


    public void saveEncounters(List<EncounterNote> notes){
        String sql="INSERT INTO public.encounters " + //
                        "(created_at,  id,  uuid, encounter_class, status, "+
                        "display,  external_id, external_system,  service_provider_id, patient_mr_number,"+ 
                        "patient_id, patient_full_name, patient_mobile, patient_birth_date, patient_gender,"+ 
                        "encounter_type, practitioner_name, practitioner_id, service_provider_name,  visit_id,"+
                        "has_prescriptions,has_service_requests)" + //
                        "VALUES(to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS'),  nextval('encounters_id_seq'::regclass),  uuid(?),?,?,"+
                        "'',?, ?,uuid(?),(select mr_number from patients p where external_id =?),(select uuid from patients p where external_id =?), (select full_name from patients p where external_id =?),"+
                        "(select mobile from patients p where external_id =?),(select birth_date from patients p where external_id =?),(select gender from patients p where external_id =?),?,?,uuid(?),?, uuid(?),?,?)";
        



        serenityJdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                // TODO Auto-generated method stub

                ps.setString(1, notes.get(i).getEncounterDate().replaceAll("T|Z", " ").strip());
                ps.setString(2, notes.get(i).getUuid().split(":")[1]);
                ps.setString(3, "ambulatory");
                ps.setString(4, "finished");
                ps.setString(5, notes.get(i).getUuid().split(":")[0]);
                ps.setString(6, "his");
                ps.setString(7, "161380e9-22d3-4627-a97f-0f918ce3e4a9");
                ps.setString(8, notes.get(i).getPatientMrNumber());
                ps.setString(9, notes.get(i).getPatientMrNumber());
                ps.setString(10, notes.get(i).getPatientMrNumber());
                ps.setString(11, notes.get(i).getPatientMrNumber());
                ps.setString(12,notes.get(i).getPatientMrNumber());
                ps.setString(13, notes.get(i).getPatientMrNumber());

                ps.setString(14, notes.get(i).getEncounterType());
                ps.setString(15,notes.get(i).getPractitionerName());
                ps.setString(16, notes.get(i).getPractitionerRoleType().equalsIgnoreCase("unknown")?null:notes.get(i).getPractitionerRoleType());

                ps.setString(17,"Nyaho Service Provider");
                ps.setString(18,notes.get(i).getUpdatedAt());
                ps.setBoolean(19,false);
                ps.setBoolean(20,false);
            }

            @Override
            public int getBatchSize() {
                // TODO Auto-generated method 
                return notes.size();
            }
            
        });
    }


    public void saveEncounter(EncounterNote note){
        String sql="INSERT INTO public.encounters " + //
                        "(created_at,  id,  uuid, encounter_class, status, "+
                        "display,  external_id, external_system,  service_provider_id, patient_mr_number,"+ 
                        "patient_id, patient_full_name, patient_mobile, patient_birth_date, patient_gender,"+ 
                        "encounter_type, practitioner_name, practitioner_id, service_provider_name,  visit_id,"+
                        "has_prescriptions,has_service_requests)" + //
                        "VALUES(to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS'),  nextval('encounters_id_seq'::regclass),  uuid(?),?,?,"+
                        "'',?, ?,uuid(?),(select mr_number from patients p where external_id =?),(select uuid from patients p where external_id =?), (select full_name from patients p where external_id =?),"+
                        "(select mobile from patients p where external_id =?),(select birth_date from patients p where external_id =?),(select gender from patients p where external_id =?),?,?,uuid(?),?, uuid(?),?,?)";
        
serenityJdbcTemplate.update(sql, new PreparedStatementSetter() {

    @Override
    public void setValues(PreparedStatement ps) throws SQLException {
        // TODO Auto-generated method stub
        ps.setString(1, note.getEncounterDate().replaceAll("T|Z", " ").strip());
        ps.setString(2, note.getUuid().split(":")[1]);
        ps.setString(3, "ambulatory");
        ps.setString(4, "finished");
        ps.setString(5, note.getUuid().split(":")[0]);
        ps.setString(6, "his");
        ps.setString(7, "161380e9-22d3-4627-a97f-0f918ce3e4a9");
        ps.setString(8, note.getPatientMrNumber());
        ps.setString(9, note.getPatientMrNumber());
        ps.setString(10, note.getPatientMrNumber());
        ps.setString(11, note.getPatientMrNumber());
        ps.setString(12,note.getPatientMrNumber());
        ps.setString(13, note.getPatientMrNumber());

        ps.setString(14, note.getEncounterType());
        ps.setString(15,note.getPractitionerName());
        ps.setString(16, note.getPractitionerRoleType().equalsIgnoreCase("unknown")?null:note.getPractitionerRoleType());

        ps.setString(17,"Nyaho Service Provider");
        ps.setString(18,note.getUpdatedAt());
        ps.setBoolean(19,false);
        ps.setBoolean(20,false);    }
    
});



    }
}
