package com.raspix.neoforge.cobble_contests;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-side options for CobbleContests. */
public final class Config {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.DoubleValue MENU_MUSIC_VOLUME;
    private static final ModConfigSpec.DoubleValue CHALLENGE_MUSIC_VOLUME;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("CobbleContests client audio settings").push("music");
        MENU_MUSIC_VOLUME = builder
                .comment("Volume of the looping music in contest menus (0.0 to 1.0).")
                .defineInRange("menuVolume", 0.62D, 0.0D, 1.0D);
        CHALLENGE_MUSIC_VOLUME = builder
                .comment("Volume of the looping music during contest challenges (0.0 to 1.0).")
                .defineInRange("challengeVolume", 0.68D, 0.0D, 1.0D);
        builder.pop();
        SPEC = builder.build();
    }

    private Config() {
    }

    public static float menuMusicVolume() {
        return MENU_MUSIC_VOLUME.get().floatValue();
    }

    public static float challengeMusicVolume() {
        return CHALLENGE_MUSIC_VOLUME.get().floatValue();
    }
}
