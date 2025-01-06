package com.serenity.integration.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.serenity.integration.models.Doctors;

public interface DoctorRepository extends JpaRepository<Doctors,Long>{
public Optional<Doctors> findByExternalId(String id);
public List<Doctors> NationalMobileNumber(String mobile);
}
