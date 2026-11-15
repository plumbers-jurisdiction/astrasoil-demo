package com.henribdev.nasa.challenge.astrasoil.domain.models.rotation;

// Entries the farmer can pick in the crop history besides the crops themselves, with their exact labels.
// The front-end gets these labels from GET /v1/plants and sends them back in the history, so the rules can recognise them.
public enum HistoryOption {

    // A confident fact: the field was left empty after the harvest
    BARE_SOIL("Bare soil"),

    // Something not on the list was planted. For the analysis it's an unknown slot, like an empty one:
    // we have no information about it (no plant family or roles)
    OTHER("Other (not listed)"),

    // The farmer doesn't know what was in the field. For the analysis it's an unknown slot, like an empty one
    NOT_SURE("Not sure");

    private final String label;

    HistoryOption(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
