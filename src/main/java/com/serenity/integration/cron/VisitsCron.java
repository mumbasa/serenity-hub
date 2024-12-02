package com.serenity.integration.cron;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

import com.serenity.integration.models.Visit;
import com.serenity.integration.repository.VisitsRepository;

@Service
public class VisitsCron {

    @Autowired
    VisitsRepository visitsRepository;

    @Autowired
    JdbcTemplate legJdbcTemplate;

    public void setupVisits(){
        List<Visit> visits = new ArrayList<>();
        String query = 
        "SELECT " +
        "  visit.id AS uuid, " +
        "  visit.created_at AS created_at, " +
        "  visit.modified_at AS updated_at, " +
        "  visit.visit_class AS encounter_class, " +
        "  visit.status AS status, " +
        "  visit.priority AS priority, " +
        "  visit.arrived_at AS started_at, " +
        "  visit.ended_at AS ended_at, " +
        "  visit.appointment_id AS appointment_id, " +
        "  visit.primary_location_id AS location_id, " +
        "  location.name AS location_name, " +
        "  visit.assigned_to_id AS assigned_to_id, " +
        "  visit.service_provider_id AS service_provider_id, " +
        "  patient.birth_date AS patient_birth_date, " +
        "  patient.mr_number AS patient_mr_number, " +
        "  patient.gender AS patient_gender, " +
        "  CONCAT(" +
        "    patient.first_name, ' ', " +
        "    patient.other_names, ' ', " +
        "    patient.last_name" +
        "  ) AS patient_full_name, " +
        "  patient.uuid AS patient_id, " +
        "  provider.name AS service_provider_name, " +
        "  CONCAT(" +
        "    practitioner.first_name, ' ', " +
        "    practitioner.last_name" +
        "  ) AS assigned_to_name " +
        "FROM " +
        "  visit " +
        "LEFT JOIN patient AS patient " +
        "  ON visit.patient_id = patient.id " +
        "LEFT JOIN location AS location " +
        "  ON visit.primary_location_id = location.id " +
        "LEFT JOIN organization AS provider " +
        "  ON visit.service_provider_id = provider.id " +
        "LEFT JOIN practitioner_role AS practitioner " +
        "  ON visit.assigned_to_id = practitioner.id ";

    
    
       SqlRowSet set = legJdbcTemplate.queryForRowSet(query);
        while (set.next()) {
            Visit visit = new Visit();
            visit.setUuid(set.getString(1));
            visit.setCreatedAt(set.getString(2));
            visit.setUpdatedAt(set.getString(3));
            visit.setEncounterClass(set.getString(4));
            visit.setStatus(set.getString(5));
            visit.setPriority(set.getString(6));
            visit.setStartedAt(set.getString(7));
            visit.setEndedAt(set.getString(8));
            visit.setAppointmentId(set.getString(9));
            visit.setServiceProviderId(set.getString(13));
            visit.setServiceTypeId(set.getString(10));
            visit.setPatientMrNumber(set.getString(15));
            visit.setLocationId(set.getString(10));
            visit.setLocationName(set.getString(11));
            visit.setAssignedToId(set.getString(12));
            visit.setPatientFullName(set.getString(17));
            visit.setServiceTypeName(set.getString(19));
            visit.setPatientId(set.getString(18));
            visit.setAssignedToName(set.getString(20));

            visits.add(visit);
        }
    
        int rounds = visits.size()/1000;
        System.err.println("Visits ="+visits.size());
        int cycle =0;
        for(int i=0;i<=rounds;i++){
            if(cycle!=rounds){
                visitsRepository.saveAllAndFlush(visits.subList(i*1000, i+1000));
            }else{
                visitsRepository.saveAllAndFlush(visits.subList(i*1000, visits.size()));
    
            }





        }
    }

}
