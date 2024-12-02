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
        "    encounter.uuid AS uuid, " +
        "    encounter.created_at AS created_at, " +
        "    encounter.modified_at AS updated_at, " +
        "    encounter.encounter_class AS encounter_class, " +
        "    encounter.status AS status, " +
        "    encounter.type AS type, " +
        "    encounter.start_time AS started_at, " +
        "    encounter.end_time AS ended_at, " +
        "    encounter.appointment_id AS appointment_id, " +
        "    patient.uuid AS patient_id, " +
        "    encounter.service_provider_id AS service_provider_id, " +
        "    encounter.service_type_id AS service_type_id, " +
        "    encounter.slot_id AS slot_id, " +
        "    encounter.visit_id AS visit_id, " +
        "    encounter.primary_location_id AS location_id, " +
        "    Location.name AS location_name, " +
        "    Slot.id AS slot_id, " +
        "    Slot.practitionerid AS practitioner_id, " +
        "    Slot.practitioner_name AS practitioner_name, " +
        "    \"Healthcare Service - Service Type\".name AS service_type_name, " +
        "    \"Serenity Provider Role - Role\".name AS practitioner_role_type " +
        "FROM " +
        "    encounter " +
        "LEFT JOIN slot AS Slot " +
        "    ON encounter.slot_id = Slot.id " +
        "LEFT JOIN patient AS patient " +
        "    ON encounter.patient_id = patient.id " +
        "LEFT JOIN location AS Location " +
        "    ON encounter.primary_location_id = Location.id " +
        "LEFT JOIN ChargeItem AS ChargeItem " +
        "    ON encounter.charge_item_id = ChargeItem.id " +
        "LEFT JOIN healthcare_service AS \"Healthcare Service - Service Type\" " +
        "    ON encounter.service_type_id = \"Healthcare Service - Service Type\".id " +
        "LEFT JOIN practitioner_role AS \"Practitioner Role - Practitionerid\" " +
        "    ON CAST(Slot.practitionerid AS text) = \"Practitioner Role - Practitionerid\".id " +
        "LEFT JOIN serenity_provider_role AS \"Serenity Provider Role - Role\" " +
        "    ON \"Practitioner Role - Practitionerid\".role_id = \"Serenity Provider Role - Role\".id;";
    
    
       SqlRowSet set = legJdbcTemplate.queryForRowSet(query);
        while (set.next()) {
            Visit visit = new Visit();
            visit.setUuid(set.getString(1));
            visit.setCreatedAt(set.getString(2));
            visit.setUpdatedAt(set.getString(3));
            visit.setEncounterClass(set.getString(4));
            visit.setStatus(set.getString(5));
            visit.setStartedAt(set.getString(6));
            visit.setEndedAt(set.getString(7));
            visit.setAppointmentId(set.getString(8));
            visit.setPatientId(set.getString(8));
            visit.setServiceProviderId(set.getString(9));
            visit.setServiceTypeId(set.getString(10));
            visit.setSlotId(set.getString(11));
            visit.setUuid(set.getString(12));
            visit.setLocationId(set.getString(13));
            visit.setLocationName(set.getString(14));
            //visit.setPractitionerId(set.getString(16));
            //visit.setPractitionerName(set.getString(17));
            visit.setServiceTypeName(set.getString(18));
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
