package com.autoservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class HotelOccupancyDto {

    private Long hotelId;
    private String hotelName;
    private LocalDate fromDate;
    private LocalDate toDate;
    private long totalRoomNights;
    private long occupiedRoomNights;
    private BigDecimal occupancyPercent;

    public HotelOccupancyDto(Long hotelId,
                             String hotelName,
                             LocalDate fromDate,
                             LocalDate toDate,
                             long totalRoomNights,
                             long occupiedRoomNights,
                             BigDecimal occupancyPercent) {
        this.hotelId = hotelId;
        this.hotelName = hotelName;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.totalRoomNights = totalRoomNights;
        this.occupiedRoomNights = occupiedRoomNights;
        this.occupancyPercent = occupancyPercent;
    }

    public Long getHotelId() {
        return hotelId;
    }

    public String getHotelName() {
        return hotelName;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public long getTotalRoomNights() {
        return totalRoomNights;
    }

    public long getOccupiedRoomNights() {
        return occupiedRoomNights;
    }

    public BigDecimal getOccupancyPercent() {
        return occupancyPercent;
    }
}
