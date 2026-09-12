package com.raspix.neoforge.cobble_contests.client.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** A relative, looping music track whose volume can be cross-faded in real time. */
public final class LoopingContestMusic extends AbstractTickableSoundInstance {
    private static final float FADE_PER_TICK = 0.045F;

    private float targetVolume;

    public LoopingContestMusic(SoundEvent sound, float initialVolume) {
        super(sound, SoundSource.MUSIC, RandomSource.create());
        looping = true;
        delay = 0;
        relative = true;
        volume = Mth.clamp(initialVolume, 0.0F, 1.0F);
        targetVolume = volume;
    }

    public void fadeTo(float targetVolume) {
        this.targetVolume = Mth.clamp(targetVolume, 0.0F, 1.0F);
    }

    public void stopImmediately() {
        stop();
    }

    @Override
    public void tick() {
        if (volume < targetVolume) {
            volume = Math.min(targetVolume, volume + FADE_PER_TICK);
        } else if (volume > targetVolume) {
            volume = Math.max(targetVolume, volume - FADE_PER_TICK);
        }
    }
}
