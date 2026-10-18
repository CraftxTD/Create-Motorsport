package com.createmotorsport.telemetry;


public record DriverInputs(float throttle, float brake, float steer, String keys) {

    public static final DriverInputs NONE = new DriverInputs(0.0F, 0.0F, 0.0F, "-");
}
