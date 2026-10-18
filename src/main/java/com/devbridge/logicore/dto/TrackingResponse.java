package com.devbridge.logicore.dto;

import com.devbridge.logicore.enums.ShipmentMode;
import com.devbridge.logicore.enums.ShipmentStatus;

import java.util.ArrayList;
import java.util.List;

public class TrackingResponse {

    private String trackingNumber;
    private ShipmentStatus currentStatus;
    private String origin;
    private String destination;
    private ShipmentMode mode;
    private List<TrackingTimelineResponse> timeline = new ArrayList<TrackingTimelineResponse>();

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public ShipmentStatus getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(ShipmentStatus currentStatus) {
        this.currentStatus = currentStatus;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public ShipmentMode getMode() {
        return mode;
    }

    public void setMode(ShipmentMode mode) {
        this.mode = mode;
    }

    public List<TrackingTimelineResponse> getTimeline() {
        return timeline;
    }

    public void setTimeline(List<TrackingTimelineResponse> timeline) {
        this.timeline = timeline;
    }
}
