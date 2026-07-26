package com.createmotorsport.physics;

import com.createmotorsport.Config;
import net.minecraft.util.Mth;

// slip-based tire friction
// based on Rapier's DynamicRayCastVehicleController (which came from Bullet's btRaycastVehicle)
// added onto by referring to speed dreams a lot and vdrift

public final class TireModel {

    private TireModel() {
    }

    // normalize forze response to slip ratio, odd function, |f| <= 1
    public static double slipCurve(double slip, double b, double c, double e) {
        double bx = b * slip;
        return Math.sin(c * Math.atan(bx * (1.0 - e) + e * Math.atan(bx)));
    }

    // better tire design load modeling; the light/heavy/falloff factors come from the tier
    public static double loadSensitivity(double load, double designLoad, TireSpec spec) {
        if (designLoad <= 0.0) {
            return 1.0;
        }
        double x = load / designLoad;
        return spec.loadFactorHeavy() + (spec.loadFactorLight() - spec.loadFactorHeavy()) * Math.exp(spec.loadFalloff() * x);
    }



    /** Longitudinal Force (N)
     * @param normalForce -> current load on the tire from suspension (N)
     * @param surfaceMu -> friction coefficient of the block under the tire, from Sable
     * @param wheelSpeed -> omega * radius, contact patch speed (m/s)
     * @param groundSpeed -> longitudinal velocity of the hub over the ground (m/s)
     * @param spec -> the tire tier (curve shape + base grip)
     */
    public static double longitudinalForce(double normalForce, double surfaceMu, double wheelSpeed, double groundSpeed, TireSpec spec) {
        double slipRatio = (wheelSpeed - groundSpeed) / Math.max(Math.abs(groundSpeed), 2.0);
        return normalForce * surfaceMu * spec.grip() * slipCurve(slipRatio, spec.pacejkaB(), spec.pacejkaC(), spec.pacejkaE());
    }

    // stop moving when still, based on how offroad seems to do it
    private static final double ROLL_RESIST_SMOOTH = 0.5;

    public static double rollingResistance(double normalForce, double groundSpeed) {
        return Config.ROLLING_RESISTANCE_COEF.getAsDouble() * normalForce
                * Mth.clamp(groundSpeed / ROLL_RESIST_SMOOTH, -1.0, 1.0);
    }

    /** Clamps a pair of impulses from each side to the friction ellipse
    // maxImpulse = N * mu * dt, same thing that Rapier does in DynamicRayCastVehicleController for the sliding check
    // Longitudinal is weighted 0.5, Bullet does this fwd_factor that is for braking/driving to feel smoother I think
    // return is scaled factoer in (0,1] to apply to both impulses; 1 is inside the ellipse
    */
    public static double frictionEllipseScale(double forwardImpulse, double sideImpulse, double maxImpulse) {
        double x = forwardImpulse * Config.FRICTION_ELLIPSE_LONG_WEIGHT.getAsDouble();
        double y = sideImpulse;
        double lenSq = x * x + y * y;
        double maxSq = maxImpulse * maxImpulse;
        if (lenSq <= maxSq || lenSq < 1.0e-12) {
            return 1.0;
        }
        return maxImpulse / Math.sqrt(lenSq);
    }

    /** critically-damped-spring suspension force
     * @param effectiveMass -> 1/inverseNormalMass, only what the individual wheel carries
     * @param naturalFreqHz -> ride frequency; ~1.5 Hz normal road car, ~3.5 Hz race car
     * @param dampingRatio -> 0.2 = boat, 0.7 = sporty, 1.0 = no overshooting
     * @param compression -> rest length minus current spring length (m), positive when compressed
     * @param relVelocity -> vertical velocity of the hardpoint along the suspension axis (m/s)
     *                    positive when extending away from ground
     * @return -> spring force along the contact normal (N), positive only since springs dont pull
     */
    public static double suspensionForce(double effectiveMass, double naturalFreqHz, double dampingRatio,
                                         double compression, double relVelocity) {
        double omega0 = 2.0 * Math.PI * naturalFreqHz;
        double k = effectiveMass * omega0 * omega0;
        // rebound damping intentionally stiffer than compression like real dampers
        double zeta = relVelocity > 0.0 ? dampingRatio * 1.15 : dampingRatio;
        double c = 2.0 * zeta * Math.sqrt(k * effectiveMass);
        double force = k * compression - c * relVelocity;
        return Math.max(0.0, force);
    }


    /** Integrate wheel spin with one Sable substep, return the new angular velocity
     * @param omega       current angular velocity (rad/s)
     * @param radius      tire radius (m)
     * @param inertia     wheel spin inertia (kg m^2)
     * @param driveTorque torque from the drivetrain (Nm)
     * @param brakeTorque maximum braking torque magnitude (Nm)
     * @param tireForce   longitudinal force the tire is currently transmitting (N)
     * @param groundSpeed longitudinal hub speed (m/s)
     * @param dt          substep length (s)
     */

    public static double integrateSpin(double omega, double radius, double inertia, double driveTorque,
                                       double brakeTorque, double tireForce, double groundSpeed, double dt) {
        double reaction = tireForce * radius;
        double brake = -Math.signum(omega) * brakeTorque;
        double newOmega = omega + (driveTorque - reaction + brake) / inertia * dt;


        // dont let brakes reverse the wheel
        if (brakeTorque > 0.0 && Math.signum(newOmega) != Math.signum(omega) && Math.abs(driveTorque) < brakeTorque) {
            newOmega = 0.0;
        }

        // If slip changed sign this step, converge to pure rolling instead of oscillating
        double rollingOmega = groundSpeed / radius;
        double slipBefore = omega - rollingOmega;
        double slipAfter = newOmega - rollingOmega;
        if (slipBefore * slipAfter < 0.0 && Math.abs(driveTorque) < 1.0e-3 && brakeTorque < 1.0e-3) {
            newOmega = rollingOmega;
        }
        return Mth.clamp(newOmega, -400.0, 400.0);
    }
}
