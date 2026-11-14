package com.henribdev.nasa.challenge.astrasoil.domain.models;

import java.util.List;

public record ReportTemplate(String title, List<String> headers, List<List<String>> rows) {}
