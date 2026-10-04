package com.steelstorm.compat.neo.neoforge.client.event;

import com.steelstorm.compat.neo.bus.api.Event;

public class CalculateDetachedCameraDistanceEvent extends Event {
    private double distance;
    public CalculateDetachedCameraDistanceEvent(double distance) { this.distance = distance; }
    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }
}
