package com.serenity.integration.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.serenity.integration.models.Doctors;

public interface DoctorRepository extends JpaRepository<Doctors,Long>{
public Doctors findByEmpId(String id);
public Doctors findByMobile(String mobile);
}
