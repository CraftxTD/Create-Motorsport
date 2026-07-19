package com.createmotorsport.physics;

import net.minecraft.util.Mth;

// slip-based tire friction
// based on Rapier's DynamicRayCastVehicleController (which came from Bullet's btRaycastVehicle)

public final class TireModel {

    // thanks Dr. Pacejka for magic tire numbers; stiffness/shape/curvature
    private static final double PACEJKA_B = 10.0;
    private static final double PACEJKA_C = 1.9;
    private static final double PACEJKA_E = 0.85;


    // boost grip, F1 cars can have really high friction coefficients, apparently
    public static final double TIRE_GRIP = 1.5;

    private TireModel() {
    }

    // normalize forze response to slip ratio, odd function, |f| <= 1
    public static double slipCurve(double slip) {
        double bx = PACEJKA_B * slip;
        return Math.sin(PACEJKA_C * Math.atan(bx * (1.0 - PACEJKA_E) + PACEJKA_E * Math.atan(bx)));
    }


    // better tire design load modeling
    private static final double LF_MAX = 1.15; // grip multiplier as load -> 0 (very light)
    private static final double LF_MIN = 0.80; // grip multiplier as load -> large (very heavy)
    private static final double LF_K = -0.6;   // how fast it falls with load

    public static double loadSensitivity(double load, double designLoad) {
        if (designLoad <= 0.0) {
            return 1.0;
        }
        double x = load / designLoad;
        return LF_MIN + (LF_MAX - LF_MIN) * Math.exp(LF_K * x);
    }



    /** Longitudinal Force (N)
     * @param normalForce -> current load on the tire from suspension (N)
     * @param surfaceMu -> friction coefficient of the block under the tire, from Sable
     * @param wheelSpeed -> omega * radius, contact patch speed (m/s)
     * @param groundSpeed -> longitudinal velocity of the hub over the ground (m/s)
     */
    public static double longitudinalForce(double normalForce, double surfaceMu, double wheelSpeed, double groundSpeed) {
        double slipRatio = (wheelSpeed - groundSpeed) / Math.max(Math.abs(groundSpeed), 2.0);
        return normalForce * surfaceMu * TIRE_GRIP * slipCurve(slipRatio);
    }

    // stop moving when still, based on how offroad seems to do it
    private static final double ROLL_RESIST_COEF = 0.015;
    private static final double ROLL_RESIST_SMOOTH = 0.5;

    public static double rollingResistance(double normalForce, double groundSpeed) {
        return ROLL_RESIST_COEF * normalForce * Mth.clamp(groundSpeed / ROLL_RESIST_SMOOTH, -1.0, 1.0);
    }

    /** Clamps a pair of impulses from each side to the friction ellipse
    // maxImpulse = N * mu * dt, same thing that Rapier does in DynamicRayCastVehicleController for the sliding check
    // Longitudinal is weighted 0.5, Bullet does this fwd_factor that is for braking/driving to feel smoother I think
    // return is scaled factoer in (0,1] to apply to both impulses; 1 is inside the ellipse
    */
    public static double frictionEllipseScale(double forwardImpulse, double sideImpulse, double maxImpulse) {
        double x = forwardImpulse * 0.5;
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
