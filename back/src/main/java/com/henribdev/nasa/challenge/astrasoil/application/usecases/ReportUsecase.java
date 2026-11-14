package com.henribdev.nasa.challenge.astrasoil.application.usecases;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.henribdev.nasa.challenge.astrasoil.application.facades.PdfFacade;
import com.henribdev.nasa.challenge.astrasoil.application.strategies.ReportTemplateStrategy;
import com.henribdev.nasa.challenge.astrasoil.domain.enums.ReportTemplateType;
import com.henribdev.nasa.challenge.astrasoil.domain.exceptions.DomainException;
import com.henribdev.nasa.challenge.astrasoil.domain.models.Report;
import org.springframework.stereotype.Service;

@Service
public class ReportUsecase {

    private final PdfFacade pdfFacade;
    private final Map<ReportTemplateType, ReportTemplateStrategy> templateStrategies;

    public ReportUsecase(PdfFacade pdfFacade, List<ReportTemplateStrategy> strategies) {
        this.pdfFacade = pdfFacade;
        this.templateStrategies = new EnumMap<>(ReportTemplateType.class);
        strategies.forEach(strategy -> templateStrategies.put(strategy.type(), strategy));
    }

    public byte[] exportReport(Report report, ReportTemplateType templateType) {

        ReportTemplateStrategy strategy = templateStrategies.get(templateType);
        if (strategy == null) {
            throw new DomainException("Unsupported report template: " + templateType);
        }

        return pdfFacade.renderTemplate(strategy.build(report));
    }
}
