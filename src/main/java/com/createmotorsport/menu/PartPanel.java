package com.createmotorsport.menu;

import net.minecraft.network.chat.Component;

import java.util.List;

public interface PartPanel {

    PartKind partKind();

    List<Component> statusLines();
}
