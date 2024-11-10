package com.serenity.integration.setup;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class RoomResponse {
private List<Room> results;
private String message;
}
