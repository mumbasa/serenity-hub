package com.serenity.integration.models;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
@Setter
@Getter
public class AvailableTime {

        private boolean isAllDay;
        private String availableStartTime;
        private String availableEndTime;
        private List<String> daysOfWeek;
        
        // Getters and Setters
    
}
