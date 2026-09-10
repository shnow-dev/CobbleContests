package com.raspix.neoforge.cobble_contests.client;

import com.raspix.neoforge.cobble_contests.menus.screens.PlayerConditionCardScreen;
import com.raspix.neoforge.cobble_contests.menus.screens.ContestBoothScreen;
import com.raspix.neoforge.cobble_contests.network.CBContestState;
import com.raspix.neoforge.cobble_contests.network.CBWalletScreenParty;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client-only packet effects, kept out of the payload's common data class. */
public final class ClientPayloadHandler {
    private ClientPayloadHandler() {}

    public static void handleWalletParty(CBWalletScreenParty data, IPayloadContext context) {
        if (Minecraft.getInstance().screen instanceof PlayerConditionCardScreen screen
                && context.player().getUUID().equals(data.getId())) {
            screen.setCVs(data.getTag());
            screen.setRibbons(data.getTag());
        }
    }

    public static void handleContestState(CBContestState data, IPayloadContext context) {
        if (Minecraft.getInstance().screen instanceof ContestBoothScreen screen
                && context.player().getUUID().equals(data.playerId())) {
            screen.applyContestState(data);
        }
    }
}
