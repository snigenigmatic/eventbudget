package com.eventbudget.service.export;

import com.eventbudget.model.domain.Budget;
import com.eventbudget.model.domain.ExportFormat;

/**
 * ADAPTER PATTERN (Structural) — Target interface.
 *
 * <p>Each concrete implementation adapts the internal domain object
 * {@link Budget} to an external representation (byte stream) in a specific
 * output format ({@link ExportFormat}). The rest of the application
 * (e.g. {@code FinanceService}) depends only on this target interface and is
 * therefore decoupled from every concrete output format.
 *
 * <p>Adding a new output format (XLSX, JSON, HTML, …) only requires adding a
 * new {@code @Component} implementing {@code ReportExporter} — no existing
 * code has to change (this also satisfies the Open/Closed Principle).
 *
 * <p>Concrete adapters:
 * <ul>
 *   <li>{@link CsvReportExporter} — adapts {@link Budget} → CSV bytes</li>
 *   <li>{@link PdfReportExporter} — adapts {@link Budget} → PDF bytes</li>
 * </ul>
 */
public interface ReportExporter {

    /**
     * @return the export format this adapter produces.
     */
    ExportFormat getFormat();

    /**
     * Adapt the given {@link Budget} domain object to a byte stream in the
     * adapter's native format.
     *
     * @param budget the budget to export (never {@code null})
     * @return the serialised report bytes
     */
    byte[] export(Budget budget);
}
