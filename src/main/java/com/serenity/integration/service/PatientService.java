package com.serenity.integration.service;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestTemplate;

import com.serenity.integration.models.PatientData;
import com.serenity.integration.repository.PatientRepository;

@Service
public class PatientService {

    @Autowired
    PatientRepository patientRepository;

    Logger LOGGER = LoggerFactory.getLogger(this.getClass().getCanonicalName());
   
    @Value("${serenity.token}")
    private String serenityToken;

    public void loadPatients() {
        List<PatientData> patients = new ArrayList<>();
        String[] firstElements = { "location", "hospcode", "id", "patient_id", "title", "pfirstname", "plastname",
                "pname", "house_no", "street_name", "locality", "city", "pincode", "phone", "mobile", "email", "dob",
                "age", "relation", "relationname", "timeofbirth", "placeofbirth", "identificationmark", "bloodgroup",
                "emergencyphone", "gender", "maritalstatus", "dateenrolled", "feespaid", "hospitalofenroll_id",
                "extractdate", "active", "username", "password", "cardpaid", "membership", "membershipdate", "state",
                "country", "passport_no", "passport_issuedate", "patient_category", "remark", "staffid", "isdob",
                "staffdependantid", "lastupdatedby", "updatedate", "ipaddress", "religiousaffiliation",
                "languagespoken", "occupation", "employer", "emergencynotify", "emergencyrelationship",
                "emergencyaddress", "emergencyphoneno", "residentialaddress", "registerby", "weight", "spousename",
                "patienttype", "ethnicity", "card_id", "panel_id", "emergencyemailid", "housetelephoneno",
                "officetelephoneno", "placeofwork", "countryid", "asondate", "religion", "patientcarepreference" };

        try (Reader in = new FileReader(ResourceUtils.getFile("classpath:real.csv"))) {
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                    .setHeader(firstElements)
                    .build();

            Iterable<CSVRecord> records = csvFormat.parse(in);
            for (CSVRecord record : records) {
                PatientData data = new PatientData(record);
                patients.add(data);
             
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

       for(int i=0;i<patients.size() /1000;i++){
           patientRepository.saveAllAndFlush(patients.subList((i*1000),(i*1000)+1000 ));

        }

    }


        public PatientData migrate(PatientData stock) {
        LOGGER.info("Searching for "+stock.getFullName());
        String url = "https://stag.api.cloud.serenity.health/v2/emr/patients";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "Bearer "+serenityToken); // Add token if needed
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<PatientData> response = restTemplate.exchange(url, HttpMethod.POST, httpEntity, PatientData.class);
//setting the stock with the data in serenity
System.err.println(stock);
         System.err.println(response.getBody());
       
        return response.getBody();
    }




}
