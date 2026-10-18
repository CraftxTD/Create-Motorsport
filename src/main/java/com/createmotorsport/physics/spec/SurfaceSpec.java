package com.createmotorsport.physics.spec;

public record SurfaceSpec(Category category, double peakMu, double slipAtPeakScale, double slideGripScale,
                          double rollingResistanceScale) {

    public enum Category {
        ROAD, DIRT, GRAVEL, SAND
    }

    public static final SurfaceSpec DRY_DEFAULT = new SurfaceSpec(Category.ROAD, 1.00, 1.0, 1.00, 1.0);
    public static final SurfaceSpec WET_DEFAULT = new SurfaceSpec(Category.ROAD, 0.65, 1.1, 0.95, 1.2);

    public static final SurfaceSpec DRY_DIRT = new SurfaceSpec(Category.DIRT, 0.60, 1.8, 1.10, 2.0);
    public static final SurfaceSpec WET_DIRT = new SurfaceSpec(Category.DIRT, 0.45, 2.0, 1.10, 3.0);

    public static final SurfaceSpec DRY_GRAVEL = new SurfaceSpec(Category.GRAVEL, 0.55, 2.2, 1.10, 3.0);
    public static final SurfaceSpec WET_GRAVEL = new SurfaceSpec(Category.GRAVEL, 0.50, 2.2, 1.10, 3.5);

    public static final SurfaceSpec DRY_SAND = new SurfaceSpec(Category.SAND, 0.45, 2.8, 1.15, 6.0);
    public static final SurfaceSpec WET_SAND = new SurfaceSpec(Category.SAND, 0.55, 2.4, 1.15, 4.0);

    public SurfaceSpec scaledBy(SurfaceResponse r) {
        return r == SurfaceResponse.NEUTRAL ? this : new SurfaceSpec(category,
                peakMu * r.peakMuScale(),
                slipAtPeakScale * r.slipAtPeakScale(),
                slideGripScale * r.slideGripScale(),
                rollingResistanceScale * r.rollingResistanceScale());
    }
}
