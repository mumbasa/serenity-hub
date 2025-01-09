package com.serenity.integration.service;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

import com.google.gson.Gson;
import com.serenity.integration.models.Healthcare;
import com.serenity.integration.models.HealthcareService;
import com.serenity.integration.models.HealthcareServiceResponse;
import com.serenity.integration.models.User;
import com.serenity.integration.models.V1Response;
import com.serenity.integration.repository.ReportRepo;
import com.serenity.integration.repository.ServiceDataRepo;
import com.serenity.integration.setup.Bed;
import com.serenity.integration.setup.BedDTO;
import com.serenity.integration.setup.HealthcareResponse;
import com.serenity.integration.setup.Location;
import com.serenity.integration.setup.Room;
import com.serenity.integration.setup.RoomDTO;
import com.serenity.integration.setup.BedResponse;
import com.serenity.integration.setup.RoomResponse;
import com.serenity.integration.setup.Ward;
import com.serenity.integration.setup.WardResponse;

@Service
public class WardSetupService {
    @Value("${access.token}")
    String token;

    @Autowired
    ServiceDataRepo serviceDataRepo;

    @Autowired
    ReportRepo reportRepo;
    Logger LOGGER = LoggerFactory.getLogger("Ward Setup");

    public Ward addWard(String orgId, String data, String tokens) {
        // LOGGER.info("Searching for "+stock.getFullName());
        String url = "https://staging.nyaho.serenity.health/v1/providers/" + orgId
                + "/administration/healthcareservices";
        System.err.println(url);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.add("Authorization", "Bearer " + tokens);
        headers.add("PROVIDER-PORTAL-ID", "j&4P8F<6+dF7/HASJ^hI92/6a&jdJOj*O\"[pHsh}t{o\"&7]\"}1~wg&SI%--,h{/");
        HttpEntity<String> httpEntity = new HttpEntity<>(data, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<WardResponse> response = restTemplate.exchange(url, HttpMethod.POST, httpEntity,
                WardResponse.class);

        System.err.println(response.getBody());

        return response.getBody().getData();
    }

    public List<Room> addRoom(String orgId, List<RoomDTO> rooms, String tokens) {
        // LOGGER.info("Searching for "+stock.getFullName());
        String url = "https://staging.nyaho.serenity.health/v1/providers/" + orgId
                + "/rooms";
       
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.add("Authorization", "Bearer " + tokens);
        headers.add("PROVIDER-PORTAL-ID", "j&4P8F<6+dF7/HASJ^hI92/6a&jdJOj*O\"[pHsh}t{o\"&7]\"}1~wg&SI%--,h{/");
        HttpEntity<List<RoomDTO>> httpEntity = new HttpEntity<>(rooms, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<RoomResponse> response = restTemplate.exchange(url, HttpMethod.POST, httpEntity,
        RoomResponse.class);

        System.err.println(response.getBody());

        return response.getBody().getResults();
    }


    public List<Bed> addBed(String orgId, String data, String tokens) {
        // LOGGER.info("Searching for "+stock.getFullName());
        String url = "https://staging.nyaho.serenity.health/v1/providers/" + orgId
                + "/beds";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.add("Authorization", "Bearer " + tokens);
        headers.add("PROVIDER-PORTAL-ID", "j&4P8F<6+dF7/HASJ^hI92/6a&jdJOj*O\"[pHsh}t{o\"&7]\"}1~wg&SI%--,h{/");
        HttpEntity<String> httpEntity = new HttpEntity<>(data, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<BedResponse> response = restTemplate.exchange(url, HttpMethod.POST, httpEntity,
        BedResponse.class);

        System.err.println(response.getBody());

        return response.getBody().getResults();
    }






    public Map<String, String> getWards(String orgId) {
        List<HealthcareService> cares = new ArrayList<>();
        // LOGGER.info("Searching for "+stock.getFullName());
        String url = "https://staging.nyaho.serenity.health/v1/providers/" + orgId
                + "/administration/healthcareservices";
        System.err.println(url);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.add("Authorization", "Bearer " + getToken().getAccess());
        headers.add("PROVIDER-PORTAL-ID", "j&4P8F<6+dF7/HASJ^hI92/6a&jdJOj*O\"[pHsh}t{o\"&7]\"}1~wg&SI%--,h{/");
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<HealthcareResponse> response = restTemplate.exchange(url, HttpMethod.GET, httpEntity,
                HealthcareResponse.class);

        for (HealthcareService c : response.getBody().getData()) {
            try {
                if (!c.getWardType().isBlank()) {
                    cares.add(c);

                }
            } catch (Exception e) {

            }
        }
        return cares.stream().collect(Collectors.toMap(e -> e.getHealthcareServiceName(), e -> e.getUuid()));
    }



    public Map<String, String> getRoom(String orgId) {
        // LOGGER.info("Searching for "+stock.getFullName());
        String url = "https://staging.nyaho.serenity.health/v1/providers/" + orgId
                + "/rooms?page_size=100";
        System.err.println(url);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.add("Authorization", "Bearer " + getToken().getAccess());
        headers.add("PROVIDER-PORTAL-ID", "j&4P8F<6+dF7/HASJ^hI92/6a&jdJOj*O\"[pHsh}t{o\"&7]\"}1~wg&SI%--,h{/");
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<RoomResponse> response = restTemplate.exchange(url, HttpMethod.GET, httpEntity,
        RoomResponse.class);
       // System.err.println(response.getBody());
        return response.getBody().getResults().stream().collect(Collectors.toMap(e -> e.getName(), e -> e.getUuid()));
    }


    public V1Response getToken() {
        User user = new User();
        user.setEmail("chris@clearspacelabs.com");
        user.setPassword("charlehaschanged");
        Gson g = new Gson();
        String data = g.toJson(user);
        System.err.println(data);
        String url = "https://staging.nyaho.serenity.health/v1/providers/auth/login";
        System.err.println(url);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        headers.add("Authorization", "Bearer " + token);
        headers.add("PROVIDER-PORTAL-ID", "j&4P8F<6+dF7/HASJ^hI92/6a&jdJOj*O\"[pHsh}t{o\"&7]\"}1~wg&SI%--,h{/");
        HttpEntity<String> httpEntity = new HttpEntity<>(data, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<V1Response> response = restTemplate.exchange(url, HttpMethod.POST, httpEntity,
                V1Response.class);
        // System.err.println(response.getBody().getData());
        return (response.getBody());
    }

    public void setWard(String orgId) {

        Map<Ward, List<Room>> wardRooms = new HashMap();
        Map<Room, List<Bed>> roomBed = new HashMap<>();
        Map<String, String> wardPrices = new HashMap();
        String[] firstElements = { "Bed name", "Ward name", "Ward type", "Room name", "Room Names",
                "Ward names", "Ward name", "Price tier name", "Price", "Currency", "Billing Frequency" };

        try (Reader in = new FileReader(ResourceUtils.getFile("classpath:wardbeds.csv"))) {
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                    .setHeader(firstElements)
                    .setDelimiter("\t")
                    .build();

            Iterable<CSVRecord> records = csvFormat.parse(in);
            for (CSVRecord record : records) {
                wardPrices.putIfAbsent(record.get(6).strip(), record.get(8));

                LOGGER.info("New Record");
                Ward ward = new Ward();
                ward.setWardName(record.get(1).strip());
                ward.setWardType(record.get(2).strip());
                ward.setDescription("General hospitalizations at your favorite hospital");
                ward.setMaxCapacity(100);
                ward.setServiceClass("HOSPITALIZATION");
                ward.setSubscriptionFrequency("Daily");
                ward.setSection("General");
                ward.setPrice(wardPrices.get(ward.getWardName()));
                Location location = new Location();
                location.setId("u101-location-None");
                List<Location> locations = new ArrayList<>();
                locations.add(location);
                ward.setLocations(locations);
                List<String> specialties = new ArrayList<>();
                specialties.add("general medicine practice");
                ward.setSpecialties(specialties);
                Room room = new Room();
                room.setName(record.get(3));
                room.setWardName(ward.getWardName());

                Bed bed = new Bed();
                bed.setName(record.get(0));
                bed.setRoomName(room.getName());

                if (roomBed.containsKey(room)) {
                    roomBed.get(room).add(bed);
                } else {
                    List<Bed> beds = new ArrayList<>();
                    beds.add(bed);
                    roomBed.put(room, beds);
                }

                if (wardRooms.containsKey(ward)) {
                    wardRooms.get(ward).add(room);
                } else {
                    List<Room> rooms = new ArrayList<>();
                    rooms.add(room);
                    wardRooms.put(ward, rooms);
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

        }

    
        Gson jGson = new Gson();
     
        for (Ward wd : wardRooms.keySet()) {
            String d = jGson.toJson(new HealthcareService(wd));
        //    System.err.println(d);
            String tokes = getToken().getAccess();

            try {
            //+    addWard(orgId, d, tokes);
            } catch (Exception e) {
                System.err.println("erroe");
            }

        }
     

        Map<String,String> wardData = getWards(orgId);
        List<RoomDTO> roomDto = new ArrayList<>();
        Map<String,String> roomData = getRoom(orgId);

        for(Room room : roomBed.keySet()){
            room.setWardUuid(wardData.get(room.getWardName()));
            if(room.getWardUuid()!=null & !roomData.keySet().contains(room.getName())){
            roomDto.add(new RoomDTO(room));
            }
            
        }

        String roomPayload = jGson.toJson(roomDto);
     //   System.err.println(roomPayload);
       // addRoom(orgId, roomDto.subList(5, 8),getToken().getAccess());


      //  System.err.println(roomData.keySet());
       roomData = getRoom(orgId);

        List<Bed> beds = new ArrayList<>();
        List<BedDTO> bedDTOs = new ArrayList<>();

        for(Room m : roomBed.keySet()){
            beds.addAll(roomBed.get(m));
        }

       
        for(Bed bed : beds){
            bed.setRoomUuid(roomData.get(bed.getRoomName()));
            System.err.println(bed.getRoomName() +"\t"+bed.getRoomUuid());
            if(bed.getRoomUuid() !=null | !bed.getRoomUuid().isBlank()){
            BedDTO bdd = new BedDTO(bed);
            
            bedDTOs.add(bdd);
            }

        } 

        String bedsPayload = jGson.toJson(bedDTOs);
        System.err.println(bedsPayload);
        System.err.println(roomData.keySet());
       addBed(orgId, bedsPayload,getToken().getAccess());


    }
}
