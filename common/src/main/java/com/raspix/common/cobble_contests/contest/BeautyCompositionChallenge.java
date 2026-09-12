package com.raspix.common.cobble_contests.contest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Deterministic theme-and-decoration challenge for Beauty contests. */
public final class BeautyCompositionChallenge {
    public static final int THEME_COUNT = 4;
    public static final int ELEMENTS_PER_THEME = 3;
    public static final int OPTION_COUNT = 6;
    public static final int PICK_COUNT = 3;

    private final int theme;
    private final int[] options;

    public BeautyCompositionChallenge(long seed) {
        Random random = new Random(seed);
        theme = random.nextInt(THEME_COUNT);

        List<Integer> generated = new ArrayList<>(OPTION_COUNT);
        for (int offset = 0; offset < ELEMENTS_PER_THEME; offset++) {
            generated.add(theme * ELEMENTS_PER_THEME + offset);
        }

        List<Integer> decoys = new ArrayList<>();
        for (int element = 0; element < THEME_COUNT * ELEMENTS_PER_THEME; element++) {
            if (element / ELEMENTS_PER_THEME != theme) {
                decoys.add(element);
            }
        }
        Collections.shuffle(decoys, random);
        generated.addAll(decoys.subList(0, OPTION_COUNT - ELEMENTS_PER_THEME));
        Collections.shuffle(generated, random);
        options = generated.stream().mapToInt(Integer::intValue).toArray();
    }

    public int theme() {
        return theme;
    }

    public int optionAt(int index) {
        if (index < 0 || index >= options.length) {
            throw new IllegalArgumentException("Beauty option index is outside the challenge");
        }
        return options[index];
    }

    public boolean matchesTheme(int index) {
        return optionAt(index) / ELEMENTS_PER_THEME == theme;
    }

    public int pointsForOption(int index) {
        return matchesTheme(index) ? 34 : -10;
    }

    public int packedData() {
        int packed = theme;
        for (int index = 0; index < options.length; index++) {
            packed |= options[index] << (2 + index * 4);
        }
        return packed;
    }

    public static int themeFromPacked(int packed) {
        return packed & 3;
    }

    public static int optionFromPacked(int packed, int index) {
        if (index < 0 || index >= OPTION_COUNT) {
            throw new IllegalArgumentException("Beauty option index is outside the challenge");
        }
        return (packed >> (2 + index * 4)) & 15;
    }
}
