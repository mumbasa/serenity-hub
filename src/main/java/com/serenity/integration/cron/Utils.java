package com.serenity.integration.cron;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.format.annotation.DateTimeFormat;

public class Utils {
public static void main(String args[]){
    String string = "2018-04-10T12313";
   // DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd' 'HH:mm:ss.SSS'Z'", Locale.ENGLISH);
  
   // LocalDateTime date = LocalDateTime.parse(string, formatter);
    System.out.println(string.split("T")[0]);


}
}
