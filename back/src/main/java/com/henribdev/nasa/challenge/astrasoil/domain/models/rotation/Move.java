package com.henribdev.nasa.challenge.astrasoil.domain.models.rotation;

import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;

// A small change to a rotation, the way an advisor talks ("keep your corn and soybean, but add wheat in winter").
// - FILL_GAP: sow crop as a cover after afterCrop's harvest, every year afterCrop comes around (a habit, one move)
// - SWAP_YEAR: grow crop instead of the main crop of the cycle's year at position
// - INSERT_YEAR: add a year of crop to the cycle, at position
public record Move(Kind kind, Plant crop, Plant afterCrop, int position) {

    public enum Kind {
        FILL_GAP, SWAP_YEAR, INSERT_YEAR
    }

    public static Move fillGap(Plant cover, Plant afterCrop) {
        return new Move(Kind.FILL_GAP, cover, afterCrop, -1);
    }

    public static Move swapYear(int position, Plant crop) {
        return new Move(Kind.SWAP_YEAR, crop, null, position);
    }

    public static Move insertYear(int position, Plant crop) {
        return new Move(Kind.INSERT_YEAR, crop, null, position);
    }

    // The move puts its crop in a main-crop slot (rather than an after-harvest one)
    public boolean isMainCrop() {
        return kind != Kind.FILL_GAP;
    }
}
