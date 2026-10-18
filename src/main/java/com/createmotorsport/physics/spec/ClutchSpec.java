package com.createmotorsport.physics.spec;


public record ClutchSpec(double maxTorque, double lockStiffness, double engageTime,
                         double launchRpm, double launchGrab, double holdGrab,
                         double flareBand, double dumpBand, double idleCreep) {

    public static final ClutchSpec F1_AUTO =
            new ClutchSpec(600.0, 400.0, 0.15, 9000.0, 1.00, 1.30, 2500.0, 4000.0, 0.10);

    public static final ClutchSpec ROAD =
            new ClutchSpec(450.0, 300.0, 0.35, 1800.0, 0.60, 1.10, 900.0, 1500.0, 0.14);

    public static final ClutchSpec TRUCK =
            new ClutchSpec(2250.0, 300.0, 0.35, 1800.0, 0.60, 1.10, 900.0, 1500.0, 0.14);

    public static final ClutchSpec PASSENGER_DCT =
            new ClutchSpec(500.0, 350.0, 0.25, 2400.0, 0.70, 1.20, 900.0, 1450.0, 0.12);

    public static final ClutchSpec MOTORCYCLE =
            new ClutchSpec(600.0, 350.0, 0.20, 7000.0, 0.80, 1.20, 1950.0, 3100.0, 0.10);
}
