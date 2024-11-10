package com.serenity.integration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.serenity.integration.models.ServiceData;
@Repository
public interface ServiceDataRepo extends JpaRepository<ServiceData,Long>{

}
