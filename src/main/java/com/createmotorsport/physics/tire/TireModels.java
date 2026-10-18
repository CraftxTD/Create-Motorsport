package com.createmotorsport.physics.tire;

import com.createmotorsport.physics.Tire;
import com.createmotorsport.physics.spec.TireSpec;

public final class TireModels {

    private TireModels() {
    }

    public static Tire of(TireSpec.Model model) {
        return switch (model) {
            case ARCADE -> ArcadeTire.INSTANCE;
            case PACEJKA -> PacejkaTire.INSTANCE;
            case FIALA -> FialaTire.INSTANCE;
            case TMEASY -> TMeasyTire.INSTANCE;
        };
    }
}
