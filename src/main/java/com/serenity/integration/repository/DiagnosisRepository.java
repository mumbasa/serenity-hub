package com.serenity.integration.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.serenity.integration.models.Diagnosis;

public interface DiagnosisRepository extends JpaRepository<Diagnosis,Long>{

}
