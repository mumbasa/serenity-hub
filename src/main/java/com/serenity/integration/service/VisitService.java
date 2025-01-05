package com.serenity.integration.service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

import com.serenity.integration.models.PatientData;
import com.serenity.integration.models.Visits;
import com.serenity.integration.repository.DoctorRepository;
import com.serenity.integration.repository.PatientRepository;
import com.serenity.integration.repository.VisitRepository;

@Service
public class VisitService {
    @Autowired
    @Qualifier("serenityJdbcTemplate")
    JdbcTemplate serenityJdbcTemplate;

    @Autowired
    VisitRepository visitRepository;

    @Autowired
    @Qualifier(value = "hisJdbcTemplate")
    JdbcTemplate hisJdbcTemplate;

    @Autowired
    PatientRepository patientRepository;

    @Autowired
    DoctorRepository doctorRepository;

    public void loadVisits(int size) {
        List<Visits> visits = new ArrayList<>();
        String sql = "select pmh.Transaction_ID as \"uuid\",\n" + //
                "  pmh.DateOfVisit created_at,\n" + //
                "  case\n" + //
                "    when pmh.Type = 'IPD'\n" + //
                "    and pmh.Admission_Type = 'Emergency' then 'emergency'\n" + //
                "    when pmh.Type = 'IPD'\n" + //
                "    and pmh.Admission_Type <> 'Emergency' then 'inpatient-encounter'\n" + //
                "    else 'ambulatory'\n" + //
                "  end as encounter_class,\n" + //
                "  'finished' as status,\n" + //
                "  case\n" + //
                "    when pmh.Type = 'IPD'\n" + //
                "    and pmh.Admission_Type = 'Emergency' then 'stat'\n" + //
                "    when pmh.Type = 'IPD'\n" + //
                "    and pmh.Admission_Type <> 'Emergency' then 'ASAP'\n" + //
                "    else 'routine'\n" + //
                "  end as priority,\n" + //
                "  cast(\n" + //
                "    CONCAT(\n" + //
                "      cast(app.Date as date),\n" + //
                "      ' ',\n" + //
                "      cast(app.Time as time)\n" + //
                "    ) as datetime\n" + //
                "  ) planned_start,\n" + //
                "  cast(\n" + //
                "    CONCAT(\n" + //
                "      cast(app.Date as date),\n" + //
                "      ' ',\n" + //
                "      cast(app.EndTime as time)\n" + //
                "    ) as datetime\n" + //
                "  ) planned_end,\n" + //
                "  cast(\n" + //
                "    CONCAT(\n" + //
                "      cast(pmh.DateOfVisit as date),\n" + //
                "      ' ',\n" + //
                "      cast(pmh.Time as time)\n" + //
                "    ) as datetime\n" + //
                "  ) started_at,\n" + //
                "  pmh.Transaction_ID external_id,\n" + //
                "  app.App_ID as appointment_id,\n" + //
                "  null as location_id,\n" + //
                "  'Nyaho Medical Centre' as location_name,\n" + //
                "  pmh.Patient_ID patient_mr_number,\n" + //
                "  pmh.Patient_ID patient_id,\n" + //
                "  concat(pm.PfirstName, ' ', pm.PLastName) patient_full_name,\n" + //
                "  pm.Mobile patient_mobile,\n" + //
                "  pm.DOB patient_birth_date,\n" + //
                "  pm.Gender patient_gender,\n" + //
                "  \"departed\" as patient_status,\n" + //
                "  pmh.Doctor_ID created_by_id,\n" + //
                "  CONCAT(dm.Title, ' ', dm.Name) created_by_name,\n" + //
                "  pmh.Transaction_ID user_friendly_id,\n" + //
                "  CONCAT(dm.Title, ' ', dm.Name) assigned_to_name,\n" + //
                "  pmh.Doctor_ID assigned_to_id,\n" + //
                "  'Nyaho Medical Centre' as location_name\n" + //
                "from patient_medical_history pmh\n" + //
                "  inner join patient_master pm on pm.Patient_ID = pmh.Patient_ID\n" + //
                "  inner join doctor_master dm on pmh.Doctor_ID = dm.Doctor_ID\n" + //
                "  inner join f_ledgertransaction lt on lt.`Transaction_ID` = pmh.`Transaction_ID`\n" + //
                "  inner join appointment app on app.ledgertnxNo = lt.LedgerTransactionNo LIMIT 204000,680000";
        SqlRowSet set = hisJdbcTemplate.queryForRowSet(sql);
        while (set.next()) {
            Visits visit = new Visits();
            visit.setUuid(UUID.randomUUID());
            visit.setCreatedAt(set.getString(2));
            visit.setEncounterClass(set.getString(3));
            visit.setStatus(set.getString(4));
            visit.setPriority(set.getString(5));
            visit.setStartedAt(set.getString(8));
            visit.setEndedAt(set.getString(7));
            visit.setExternalSystem("his");
            visit.setExternalId(set.getString(1));
            visit.setServiceProviderId("161380e9-22d3-4627-a97f-0f918ce3e4a9");
            visit.setServiceProviderName("Nyaho Medical Centre");
            visit.setHisNumber(set.getString(13));
            visit.setGender(set.getString(18));
            visit.setPatientMobile(set.getString(16));
            visit.setPatientDob(set.getString(17));
            visit.setAssignedToName(set.getString(21));
            visit.setAssignedToId(set.getString(20));
            visit.setPatientName(set.getString(15));
            visit.setPatientStatus(set.getString(19));
            visits.add(visit);
        }

        int rounds = visits.size() / size;

        for (int i = 0; i <= rounds; i++) {
            if (i < rounds) {
                System.err.println("Round submission " + i);
                List<Visits> ds = visits.subList(i * size, (i * size) + size);
                try {
                    visitRepository.saveAll(ds);
                } catch (Exception e) {

                    e.printStackTrace();
                }
            } else {
                System.err.println("Finishing Round submission " + i);

                List<Visits> ds = visits.subList((i * size), visits.size());
                visitRepository.saveAll(ds);

            }
            ;
        }

    }

    public void setITem() {
        int rounds = 640871 / 1000;

        for (int i = 0; i <= rounds; i++) {
            List<Visits> visits = visitRepository.getfirst100k((i * 1000) + 3900, 1000);
            System.err.println(visits.size() + "-----------");

            System.err.println("doing");
            try {
                insertIntoSerenity(visits);
            } catch (Exception e) {
                System.err.println("error in adding");
            }

        }

    }

    public void insertIntoSerenity(List<Visits> visitss) {
        List<Visits> visits = new ArrayList<>();

        System.err.println("Settting variables ");
        visitss.stream().forEach(e -> {
            if (patientRepository.findByExternalId(e.getHisNumber()).isPresent()) {

                e.setPatient(patientRepository.findByExternalId(e.getHisNumber()).get());
                e.setDoctors(doctorRepository.findByEmpId(e.getAssignedToId()));
                visits.add(e);
            }
        });

        String sql = "INSERT INTO public.visits " + //
                "(created_at,  id,  \"uuid\", encounter_class, status," +
                "priority,  started_at, ended_at, external_id, external_system," +
                "service_provider_id, service_provider_name, patient_mr_number, patient_id, patient_full_name," +
                "patient_mobile, patient_birth_date, patient_gender, patient_status, assigned_to_name, assigned_to_id,display)\n"
                + //
                "VALUES(to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS'),nextval('visits_id_seq'::regclass),uuid(?),?  ,?," +
                "?,to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS'),to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS'),?,?," +
                "uuid(?),?,?,(select uuid from patients p where external_id =?),?," +
                "?,TO_DATE(?, 'YYYY/MM/DD'),?,?,?,uuid(?),?)";

        System.err.println("Settting Insert values ");

        serenityJdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                // TODO Auto-generated method stub
                ps.setString(1, visits.get(i).getCreatedAt() + " 08:03:02.226");
                ps.setString(2, visits.get(i).getUuid().toString());
                ps.setString(3, visits.get(i).getEncounterClass());
                ps.setString(4, visits.get(i).getStatus());

                ps.setString(5, visits.get(i).getPriority());
                ps.setString(6, visits.get(i).getStartedAt().replaceAll("T", " "));
                ps.setString(7, visits.get(i).getEndedAt().replaceAll("T", " "));
                ps.setString(8, visits.get(i).getExternalId());

                ps.setString(9, "his");

                ps.setString(10, visits.get(i).getServiceProviderId());
                ps.setString(11, visits.get(i).getServiceProviderName());
                try {
                    ps.setString(12, visits.get(i).getPatient().getMrNumber());
                } catch (Exception e) {
                    System.err.println("cannot find patient");

                }
                ps.setString(13, visits.get(i).getHisNumber());

                ps.setString(14, visits.get(i).getPatientName());
                ps.setString(15, visits.get(i).getPatientMobile());
                ps.setString(16, visits.get(i).getPatientDob());

                ps.setString(17, visits.get(i).getGender());
                ps.setString(18, visits.get(i).getPatientStatus());
                ps.setString(19, visits.get(i).getAssignedToName());
                try {
                    ps.setString(20, visits.get(i).getDoctors().getSerenityId());
                } catch (Exception e) {
                    System.err.println("cannot find doctor");

                }
                ps.setString(21, visits.get(i).getHisNumber());

            }

            @Override
            public int getBatchSize() {
                // TODO Auto-generated method stub
                return visits.size();
            }

        });

    }

}
