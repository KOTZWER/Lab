package com.autoservice.dto;

import java.math.BigDecimal;

public class RoomAvailabilityDto {

    private Long roomId;
    private String roomNumber;
    private String type;
    private Integer capacity;
    private BigDecimal pricePerNight;

    public RoomAvailabilityDto(Long roomId, String roomNumber, String type, Integer capacity, BigDecimal pricePerNight) {
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.type = type;
        this.capacity = capacity;
        this.pricePerNight = pricePerNight;
    }

    public Long getRoomId() {
        return roomId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public String getType() {
        return type;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public BigDecimal getPricePerNight() {
        return pricePerNight;
    }
}
