package com.createmotorsport.physics.vehicle;

import com.createmotorsport.physics.spec.SurfaceSpec;
import com.createmotorsport.physics.spec.TireSpec;
import com.createmotorsport.physics.terrain.TerrainAssist;

public record CornerInput(
        double radius,
        boolean hit, double rigidRaw, double castLift, TerrainAssist.Probe probe,
        SurfaceSpec surface, double surfaceMu,
        double airborneVel, double hardpointVel,
        double vLon, double vLat, double velNormal, double airspeed,
        double normalY,
        double invNormalMass, double rollResponseMass, double invAxleMass,
        TireSpec tireSpec, float designLoad, double tireRate, double tireDampingRatio, double wheelMass,
        long gameTime) {
}
