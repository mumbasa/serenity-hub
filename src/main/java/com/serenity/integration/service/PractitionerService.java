package com.serenity.integration.service;

import java.util.ArrayList;
import java.util.List;

import org.aspectj.internal.lang.annotation.ajcITD;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

import com.serenity.integration.models.Doctors;
import com.serenity.integration.repository.DoctorRepository;
@Service
public class PractitionerService {

   @Autowired
    @Qualifier(value = "hisJdbcTemplate")
    JdbcTemplate hisJdbcTemplate;

    @Autowired
    DoctorRepository doctorRepository;

    public void saveHisPractioner() {
        List<Doctors> doctors = new ArrayList<>();
        String query = "SELECT   em.employee_id,   de.doctor_id,   em.locality,   em.house_no,   em.city,   em.mobile,   em.dob,   em.title,   em.street_name,   em.email,   dm.name FROM   employee_master em   JOIN doctor_employee de ON de.Employee_id = em.Employee_ID   JOIN doctor_master dm ON dm.doctor_id = de.doctor_id";
        SqlRowSet set = hisJdbcTemplate.queryForRowSet(query);
        while (set.next()) {
            Doctors d = new Doctors();
            d.setEmpId(set.getString(2));
            d.setTitle(set.getString("title"));
            d.setMobile(set.getString("mobile"));
            d.setHomeAddress(set.getString("house_no")+" "+(set.getString("locality"))+" "+(set.getString("city")));
            d.setDateOfBirth(set.getString("dob"));
            d.setEmail(set.getString("email"));
            d.setHisId(set.getString(3));
            d.setFirstName(set.getString("name"));
            d.setPostalAddress(set.getString("street_name"));
            doctors.add(d);
        }

        doctorRepository.saveAll(doctors);
    }
}
