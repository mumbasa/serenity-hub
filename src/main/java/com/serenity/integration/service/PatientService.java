package com.serenity.integration.service;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;
import com.serenity.integration.models.EncounterNote;
import com.serenity.integration.models.PatientData;
import com.serenity.integration.repository.PatientRepository;

@Service
public class PatientService {

    @Autowired
    PatientRepository patientRepository;

    @Autowired
    @Qualifier(value = "hisJdbcTemplate")
    JdbcTemplate hisJdbcTemplate;

    @Autowired
    @Qualifier(value = "vectorJdbcTemplate")
    JdbcTemplate vectorJdbcTemplate;


    Logger LOGGER = LoggerFactory.getLogger(this.getClass().getCanonicalName());

    @Value("${serenity.token}")
    private String serenityToken;

    static final String DIGITS = "0123456789";
    static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    static final SecureRandom RANDOM = new SecureRandom();

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
                Gson j = new Gson();
                String k = j.toJson(data);
                System.err.println(k);
                migrate(k);

            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        for (int i = 0; i < 10; i++) {
            // patientRepository.saveAllAndFlush(patients.subList((i * 1000), (i * 1000) +
            // 1000));

        }

    }

    public PatientData migrate(String stock) {
        String url = "https://stag.api.cloud.serenity.health/v2/emr/patients";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.add("x-api-key", "efomrddi");
        // Add token if needed
        HttpEntity<String> httpEntity = new HttpEntity<>(stock, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<PatientData> response = restTemplate.exchange(url, HttpMethod.POST, httpEntity,
                PatientData.class);
        // setting the stock with the data in serenity
        System.err.println(stock);
        System.err.println(response.getBody());

        return response.getBody();
    }

    public void getHisNote() {
        List<PatientData> fallouts = new ArrayList<>();

        String sql = "SELECT * FROM patient_master";
        SqlRowSet record = hisJdbcTemplate.queryForRowSet(sql);
        while (record.next()) {
            PatientData pd = new PatientData();

            pd.setExternalId(record.getString("patient_id"));
            pd.setLastName(record.getString("plastname"));
            pd.setFirstName(record.getString("pfirstname"));
            pd.setMobile(record.getString("mobile").isEmpty() ? "" :record.getString("mobile").replaceAll("-", ""));
            pd.getMobile().replaceAll("\u0000", "");
            pd.setEmail(record.getString("email"));
            pd.setBirthDate(record.getString("dob"));
            // pd.setId(String.valueOf(record.getLong(1)));
            String str = record.getString("dateenrolled");
            if (str != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
                LocalDateTime dateTime = LocalDateTime.parse(str, formatter);
                pd.setCreatedAt(dateTime.toString());
                pd.setMrNumber(generateMRNumber("NMC", dateTime));
            }
            // nationalId(record.getString("countryid");
            pd.setGender(record.getString("gender").toUpperCase());
            pd.setExternalSystem("his");
            pd.setNationalMobileNumber(record.getString("phone"));
            pd.setFullName(record.getString("pname"));
            pd.setTitle(record.getString("title"));
            pd.setOccupation(record.getString("occupation"));
            pd.setEmployer(record.getString("employer"));
            pd.setBloodType(record.getString("bloodgroup"));
            pd.setMaritalStatus(record.getString("maritalstatus"));
            pd.setNationality(record.getString("country"));
            pd.setPassportNumber(record.getString("passport_no"));
            pd.setBirthTime(record.getString("timeofbirth"));
            pd.setReligiousAffiliation(record.getString("religiousaffiliation"));
            pd.setManagingOrganizationId("161380e9-22d3-4627-a97f-0f918ce3e4a9");
            fallouts.add(pd);
        }
        int cycle = 0;
        int rounds = (fallouts.size() / 1000);
        for (int i = 0; i <= rounds; i++) {
            LOGGER.info("adding round " + i);
        
                 if (cycle < rounds) {
                //     if(i==310 | i==1205){

             patientRepository.saveAll(fallouts.subList(i * 1000, (i * 1000) + 1000));    
            }
                else {
                  patientRepository.saveAll(fallouts.subList(cycle * 1000, fallouts.size()));

             }
             
             cycle++;
            } 
         
        }

    

    public static String generateMRNumber(String prefix, LocalDateTime createdAt) {
        // Format the date for a more precise timestamp (e.g., YYMMDD)
        String dateSuffix = createdAt.format(DateTimeFormatter.ofPattern("yy"));

        // Generate a short UUID (for uniqueness)
        String uniqueId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Format the MR number with prefix, date, random suffix, and unique ID
        return String.format("%s-%s-%s", prefix.toUpperCase(), dateSuffix, uniqueId);

    }

    private static String removeNullValues(String obj) {
        JSONObject jsonObject = new JSONObject(obj);
        Iterator<String> keys = jsonObject.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            Object value = jsonObject.get(key);

            // Remove the key if the value is null or an empty string
            if (value == null || (value instanceof String && ((String) value).isEmpty())) {
                keys.remove(); // Removes the current key
            }

            
        }
        return jsonObject.toString();
    }

    public void setupSerenity() {
        List<PatientData> data = patientRepository.findTop5();
        data.stream().forEach(e -> {
            e.setGender(e.getGender().toUpperCase());
          /*   if(!e.getMobile().isEmpty()){
                e.setMobile("233"+e.getMobile());
            } */
          //  e.setNationality(StringUtils.capitalize(e.getNationality().toLowerCase()));

        });
        System.err.println(data.size() + " patients");


        
        for (PatientData g : data) {
            Gson j = new Gson();
            String k = removeNullValues(j.toJson(g));
          
            try{
            migrate(k);
            System.err.println("Correct");
            }catch (Exception e ){
                System.err.println(k);
                e.printStackTrace();
            }
        } 

    }


public void updateCountiers(){

    final String[] countryNames = {
        "Afghanistan", "Åland Islands", "Albania", "Algeria", "American Samoa", 
        "Andorra", "Angola", "Anguilla", "Antarctica", "Antigua and Barbuda", 
        "Argentina", "Armenia", "Aruba", "Australia", "Austria", "Azerbaijan", 
        "Bahamas", "Bahrain", "Bangladesh", "Barbados", "Belarus", "Belgium", 
        "Belize", "Benin", "Bermuda", "Bhutan", "Bolivia (Plurinational State of)", 
        "Bonaire, Sint Eustatius and Saba", "Bosnia and Herzegovina", "Botswana", 
        "Bouvet Island", "Brazil", "British Indian Ocean Territory", "Brunei Darussalam", 
        "Bulgaria", "Burkina Faso", "Burundi", "Cabo Verde", "Cambodia", "Cameroon", 
        "Canada", "Cayman Islands", "Central African Republic", "Chad", "Chile", 
        "China", "Christmas Island", "Cocos (Keeling) Islands", "Colombia", "Comoros", 
        "Congo (the Democratic Republic of the)", "Congo", "Cook Islands", "Costa Rica", 
        "Croatia", "Cuba", "Curaçao", "Cyprus", "Czechia", "Denmark", "Djibouti", 
        "Dominica", "Dominican Republic", "Ecuador", "Egypt", "El Salvador", 
        "Equatorial Guinea", "Eritrea", "Estonia", "Eswatini", "Ethiopia", "Falkland Islands (Malvinas)", 
        "Faroe Islands", "Fiji", "Finland", "France", "French Guiana", "French Polynesia", 
        "French Southern Territories", "Gabon", "Gambia", "Georgia", "Germany", 
        "Ghana", "Gibraltar", "Greece", "Greenland", "Grenada", "Guadeloupe", 
        "Guam", "Guatemala", "Guernsey", "Guinea", "Guinea-Bissau", "Guyana", 
        "Haiti", "Heard Island and McDonald Islands", "Holy See", "Honduras", 
        "Hong Kong", "Hungary", "Iceland", "India", "Indonesia", "Iran (Islamic Republic of)", 
        "Iraq", "Ireland", "Isle of Man", "Israel", "Italy", "Jamaica", "Japan", 
        "Jersey", "Jordan", "Kazakhstan", "Kenya", "Kiribati", "Korea (the Democratic People's Republic of)", 
        "Korea (the Republic of)", "Kuwait", "Kyrgyzstan", "Lao People's Democratic Republic", 
        "Latvia", "Lebanon", "Lesotho", "Liberia", "Libya", "Liechtenstein", 
        "Lithuania", "Luxembourg", "Madagascar", "Malawi", "Malaysia", "Maldives", 
        "Mali", "Malta", "Marshall Islands", "Martinique", "Mauritania", "Mauritius", 
        "Mayotte", "Mexico", "Micronesia (Federated States of)", "Moldova (the Republic of)", 
        "Monaco", "Mongolia", "Montenegro", "Montserrat", "Morocco", "Mozambique", 
        "Myanmar", "Namibia", "Nauru", "Nepal", "Netherlands", "New Caledonia", 
        "New Zealand", "Nicaragua", "Niger", "Nigeria", "Niue", "Norfolk Island", 
        "North Macedonia", "Northern Mariana Islands", "Norway", "Oman", "Pakistan", 
        "Palau", "Palestine, State of", "Panama", "Papua New Guinea", "Paraguay", 
        "Peru", "Philippines", "Pitcairn", "Poland", "Portugal", "Puerto Rico", 
        "Qatar", "Romania", "Russian Federation", "Rwanda", "Réunion", "Saint Barthélemy", 
        "Saint Helena, Ascension and Tristan da Cunha", "Saint Kitts and Nevis", 
        "Saint Lucia", "Saint Martin (French part)", "Saint Pierre and Miquelon", 
        "Saint Vincent and the Grenadines", "Samoa", "San Marino", "Sao Tome and Principe", 
        "Saudi Arabia", "Senegal", "Serbia", "Seychelles", "Sierra Leone", 
        "Singapore", "Sint Maarten (Dutch part)", "Slovakia", "Slovenia", "Solomon Islands", 
        "Somalia", "South Africa", "South Georgia and the South Sandwich Islands", 
        "South Sudan", "Spain", "Sri Lanka", "Sudan", "Suriname", "Sweden", 
        "Switzerland", "Syrian Arab Republic", "Taiwan (Province of China)", "Tajikistan", 
        "Tanzania, United Republic of", "Thailand", "Timor-Leste", "Togo", "Tokelau", 
        "Tonga", "Trinidad and Tobago", "Tunisia", "Turkey", "Turkmenistan", 
        "Turks and Caicos Islands", "Tuvalu", "Uganda", "Ukraine", "United Arab Emirates", 
        "United Kingdom of Great Britain and Northern Ireland", "United States of America", 
        "Uruguay", "Uzbekistan", "Vanuatu", "Venezuela (Bolivarian Republic of)", 
        "Viet Nam", "Western Sahara", "Yemen", "Zambia", "Zimbabwe"
    };

    String sql ="UPDATE patient_information set nationality=? WHERE nationality=?";
    vectorJdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
        public void setValues(PreparedStatement ps, int i) throws SQLException {
						
						ps.setString(1, countryNames[i]);
						ps.setString(2,countryNames[i].toUpperCase());
					}
					public int getBatchSize() {
						return countryNames.length;
					}
				
    });
  

}


    public void searchCountries(){

        String url = "https://stag.api.cloud.serenity.health/v2/valuesets/countries";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.add("x-api-key", "efomrddi");
        // Add token if needed
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, httpEntity,
                String.class);
        // setting the stock with the data in serenity
        System.err.println(response.getBody());

    }
}
