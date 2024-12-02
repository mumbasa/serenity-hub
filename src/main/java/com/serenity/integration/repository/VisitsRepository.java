package com.serenity.integration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.serenity.integration.models.Visit;
@Repository
public interface VisitsRepository extends JpaRepository<Visit,Long>{

}
