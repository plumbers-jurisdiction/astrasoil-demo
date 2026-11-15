package com.henribdev.nasa.challenge.astrasoil.domain.services.text;

import java.time.LocalDate;
import java.time.Month;
import java.time.MonthDay;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateSummary;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateWindows;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.Gap;
import com.henribdev.nasa.challenge.astrasoil.domain.models.fit.FitCheck;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.Card;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.Goal;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rule.Problem;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rule.RainWarning;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilSummary;

// Every plain sentence the cards use, in one place. No jargon and no exact values beyond what a farmer reads easily.
final class CardSentences {

    private CardSentences() {}

    // Card titles

    static String title(Card card) {
        return switch (card.type()) {
            case CURRENT -> "What you do now";
            case SIMPLE -> "A simple rotation that fits here";
            case ANOTHER_GOOD_FIT -> "Another good fit";
            case GOAL -> card.rotation().goals().stream().map(CardSentences::goal).collect(Collectors.joining(" · "));
        };
    }

    private static String goal(Goal goal) {
        return switch (goal) {
            case HEALTHIER_SOIL -> "Healthier soil";
            case SAVE_WATER -> "Save water";
            case FEWER_PESTS -> "Fewer pests";
            case LOWER_COSTS -> "Lower costs";
            case COPE_WITH_WET_SOIL -> "Cope with wet soil";
        };
    }

    // Problems: what the farmer reads under Risks when a card leaves the problem, or under Benefits when it fixes it

    static String problem(Problem problem, ClimateSummary climate, int repeatedYears) {
        return switch (problem.type()) {
            case BARE_AFTER_HARVEST -> problem.soft()
                ? "If you don't already cover your soil after the " + lower(problem.crop()) + " harvest, it sits empty " + gapMonths(climate)
                : "Your field sits empty " + gapMonths(climate) + " after the " + lower(problem.crop()) + " harvest";
            case LESS_RAIN -> "Rain in the growing season is " + percentLess(climate) + "% lower than before "
                + climate.rainChange().windows().recentStartYear();
            case FIELD_DRIES_OUT -> "The field loses more water than it gets in the growing season";
            case LOW_ORGANIC_MATTER -> "The soil here holds little organic matter";
            case SOIL_WASHES_AWAY -> "The soil here washes away easily";
            case WET_SOIL -> "Water sits in this soil after rain";
            case SOIL_DRIES_FAST -> "This soil dries out fast between rains";
            case SAME_CROP_REPEATED -> capital(problem.crop()) + " " + repeatedYears + " years in a row lets pests build up";
            case NO_LEGUME -> problem.soft()
                ? "If you don't already grow a legume, no crop in your rotation adds nitrogen"
                : "No crop in your rotation adds nitrogen";
        };
    }

    // How the crop that fixes a problem helps, after the problem's sentence
    static String howItHelps(Problem problem, Plant crop) {
        String name = lower(crop);
        return switch (problem.type()) {
            case LESS_RAIN, FIELD_DRIES_OUT, SOIL_DRIES_FAST -> name + " copes better with dry spells";
            case WET_SOIL -> name + " copes a little better with wet soil";
            case NO_LEGUME -> name + " adds nitrogen";
            case LOW_ORGANIC_MATTER -> name + " adds organic matter";
            case SOIL_WASHES_AWAY, BARE_AFTER_HARVEST -> name + " covers the soil";
            case SAME_CROP_REPEATED -> name + " breaks the pest cycle";
        };
    }

    // Pair facts

    // A legume feeding the next crop, named by the pair
    static String nitrogen(Plant previous, Plant next) {
        return capital(previous) + " → " + capital(next) + ": " + lower(next) + " uses the nitrogen " + lower(previous) + " left";
    }

    // Main crops from different families in a row, said once for the whole rotation
    static String familyBreak() {
        return "Different crops in a row break the pest cycle";
    }

    static String lost(String benefit) {
        return "Loses this benefit: " + benefit;
    }

    // A crop the card brings in fits the field
    static String fits(Plant crop, boolean soilChecked) {
        return capital(crop) + " fits your area's climate" + (soilChecked ? " and soil" : "");
    }

    // Cover crops: one short line on what the cover does and what it leaves for the next crop

    static String coverReason(ClimateSummary climate, boolean addsOrganicMatter, boolean protectsSoil, Plant feeds) {
        StringBuilder reason = new StringBuilder("Covers the soil")
            .append(climate.calendarWindows().gap() == null ? " after the harvest" : " " + gapMonths(climate));
        if (addsOrganicMatter) {
            reason.append(", adds organic matter");
        }
        if (protectsSoil) {
            reason.append(", keeps it from washing away");
        }
        if (feeds != null) {
            reason.append(" and leaves nitrogen for the ").append(lower(feeds));
        }
        return reason.toString();
    }

    // Notes (risks)

    static String frostInGrowingSeason() {
        return "Frosts reach into the growing season";
    }

    static String summerHeat(List<Month> heatMonths) {
        return "Hot spells" + (heatMonths.isEmpty() ? "" : " in " + months(heatMonths)) + " can hurt crops in flower";
    }

    static String rainWarning(Plant crop, RainWarning warning) {
        return warning == RainWarning.BELOW_MINIMUM
            ? capital(crop) + " needs more rain than falls here: it would need irrigation"
            : capital(crop) + " usually grows with less rain than falls here";
    }

    static String fitsPoorly(Plant crop, FitCheck check, SoilSummary soil, Double ph, Double phMinimum) {
        String name = lower(crop);
        return switch (check) {
            case COLD -> "Winters here may be too cold for " + name;
            case SEASON_LENGTH -> "The season here may be too short for " + name;
            case SOIL_TEXTURE -> "This soil may be too " + (soil != null && soil.textureGroup() != null
                ? soil.textureGroup().name().toLowerCase(Locale.ROOT) : "heavy or light") + " for " + name;
            case PH -> "This soil may be too " + (ph != null && phMinimum != null && ph < phMinimum ? "acidic" : "alkaline") + " for " + name;
            case WET_SOIL -> "Water sits in this soil, and " + name + " doesn't tolerate wet roots";
        };
    }

    // Needs review

    static String sowCover(MonthDay firstAutumnFrost) {
        String when = firstAutumnFrost == null ? "" : " (around " + day(firstAutumnFrost.atYear(2001).minusDays(14)) + " here)";
        return "When to sow the cover crop: at least 2 weeks before the first autumn frost" + when + ", and when to stop it before planting";
    }

    static String drySeason() {
        return "Our data only finds cold off-seasons. If your field sits bare in the dry season, ask locally about cover crops that suit it";
    }

    static String climateCompared(ClimateWindows windows) {
        return "Climate compares " + windows.recentStartYear() + "–" + windows.recentEndYear() + " with "
            + windows.baselineStartYear() + "–" + windows.baselineEndYear();
    }

    static String irrigation() {
        return "Irrigation options, since the field loses more water than it gets";
    }

    static String plantingDatesForFrost() {
        return "Safe planting and harvest dates around the frosts";
    }

    static String plantingDatesForHeat() {
        return "Shifting planting dates away from the hot spells";
    }

    static String contourPlanting() {
        return "Contour planting on slopes, so the soil doesn't wash away";
    }

    static String drainage() {
        return "Drainage options for the wet soil";
    }

    static String rainWarningReview(Plant crop, RainWarning warning) {
        return (warning == RainWarning.BELOW_MINIMUM ? "Irrigation for " : "Drainage for ") + lower(crop);
    }

    static String keepGrowing(Plant crop) {
        return "Whether to keep growing " + lower(crop) + " here";
    }

    static String notChecked(Plant crop, List<FitCheck> checks) {
        return capital(crop) + ": " + checks.stream().map(CardSentences::checkName).collect(Collectors.joining(", ")) + " not checked (no data)";
    }

    static String noCropData(Plant crop) {
        return capital(crop) + ": not checked, no crop data yet";
    }

    static String noHistory() {
        return "No crop history entered, so the first card is a simple rotation, not yours";
    }

    static String unknownHistory() {
        return "Some years of your history are unknown, so some checks were skipped";
    }

    static String noSoil() {
        return "No soil data here (the soil survey covers the US only): a soil test would help";
    }

    static String climateArea() {
        return "Rain and temperature data cover about 50 km around you";
    }

    static String soilMix(SoilSummary soil) {
        String mix = soil.mix().stream().map(share -> share.sharePercent() + "% " + share.name()).collect(Collectors.joining(", "));
        return "The soil is a mapped area's mix" + (mix.isEmpty() ? "" : ": " + mix) + "; a soil test or Web Soil Survey confirms your field";
    }

    static String marketsAndSeed(Plant crop) {
        return "Where to sell " + lower(crop) + ", at what price, and " + lower(crop) + " seed nearby";
    }

    static String seed(Plant crop) {
        return capital(crop) + " seed nearby";
    }

    static String usdaApproximations() {
        return "Crop values are USDA approximations";
    }

    static String droughtRelative() {
        return "\"Copes with dry spells\" is relative to similar crops in the region";
    }

    // Helpers

    private static String gapMonths(ClimateSummary climate) {
        Gap gap = climate.calendarWindows().gap();
        if (gap == null) {
            return "after the harvest";
        }
        Month first = gap.firstDay().getMonth();
        Month last = gap.lastDay().getMonth();
        return first == last ? "in " + month(first) : "from " + month(first) + " to " + month(last);
    }

    private static long percentLess(ClimateSummary climate) {
        return Math.round((1 - climate.rainChange().ratio()) * 100);
    }

    private static String months(List<Month> months) {
        List<String> names = months.stream().map(CardSentences::month).toList();
        if (names.size() == 1) {
            return names.getFirst();
        }
        return String.join(", ", names.subList(0, names.size() - 1)) + " and " + names.getLast();
    }

    private static String month(Month month) {
        return month.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    private static String day(LocalDate date) {
        return date.getDayOfMonth() + " " + month(date.getMonth());
    }

    private static String checkName(FitCheck check) {
        return switch (check) {
            case COLD -> "cold";
            case SEASON_LENGTH -> "season length";
            case SOIL_TEXTURE -> "soil texture";
            case PH -> "soil pH";
            case WET_SOIL -> "wet soil";
        };
    }

    private static String lower(Plant crop) {
        return crop.name().toLowerCase(Locale.ROOT);
    }

    private static String capital(Plant crop) {
        return crop.name();
    }
}
