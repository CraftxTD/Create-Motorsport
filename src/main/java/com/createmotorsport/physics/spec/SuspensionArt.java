package com.createmotorsport.physics.spec;

public record SuspensionArt(String model, String texture, String animation) {

    public static final SuspensionArt DEFAULT = new SuspensionArt(
            "createmotorsport:geo/suspension.geo.json",
            "createmotorsport:textures/block/suspension.png",
            "createmotorsport:animations/suspension.animation.json");
}
