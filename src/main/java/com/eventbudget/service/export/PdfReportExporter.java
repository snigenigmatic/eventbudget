package com.eventbudget.service.export;

import com.eventbudget.model.domain.Budget;
import com.eventbudget.model.domain.BudgetCategory;
import com.eventbudget.model.domain.ExportFormat;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * ADAPTER PATTERN (Structural) — Concrete adapter that adapts a {@link Budget}
 * domain aggregate into a minimal PDF 1.4 byte stream.
 *
 * <p>The PDF specification is a different "interface" than our Java domain
 * model: it requires a header, object catalog, page tree, font dictionary,
 * content stream, cross-reference table and trailer. This class hides that
 * complexity behind the common {@link ReportExporter} interface so callers
 * treat PDF and CSV export identically.
 */
@Component
public class PdfReportExporter implements ReportExporter {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public ExportFormat getFormat() {
        return ExportFormat.PDF;
    }

    @Override
    public byte[] export(Budget budget) {
        List<String> lines = new ArrayList<>();
        lines.add("EVENT BUDGET REPORT");
        lines.add("===========================================");
        lines.add("Budget ID   : " + budget.getBudgetId());
        lines.add("Event       : " + sanitizeText(budget.getEvent().getName()));
        lines.add("Status      : " + budget.getStatus().name());
        lines.add("Total Amount: INR " + budget.getTotalAmount());
        lines.add("Created At  : " + formatTimestamp(budget.getCreatedAt()));
        lines.add("Closed At   : " + formatTimestamp(budget.getClosedAt()));
        lines.add("");
        lines.add("CATEGORY BREAKDOWN");
        lines.add("===========================================");
        lines.add(String.format("%-20s %10s %10s %10s %8s%%",
                "Category", "Allocated", "Committed", "Available", "Used"));
        lines.add("-------------------------------------------");
        for (BudgetCategory category : budget.getCategories()) {
            lines.add(String.format("%-20s %10s %10s %10s %8s%%",
                    sanitizeText(category.getExpenseCategory().getName()),
                    category.getAllocatedAmount(),
                    category.getCommittedAmount(),
                    category.getAvailableBalance(),
                    category.getUtilizationPercent()));
        }

        StringBuilder streamBuilder = new StringBuilder();
        int y = 750;
        for (String line : lines) {
            String pdfSafe = line
                    .replace("\\", "\\\\")
                    .replace("(", "\\(")
                    .replace(")", "\\)");
            streamBuilder.append("BT /F1 10 Tf 40 ").append(y).append(" Td (")
                    .append(pdfSafe)
                    .append(") Tj ET\n");
            y -= 16;
        }

        String stream = streamBuilder.toString();
        List<String> objects = List.of(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792]"
                        + " /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>",
                "<< /Length " + stream.getBytes(StandardCharsets.UTF_8).length + " >>\nstream\n"
                        + stream
                        + "endstream");

        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int index = 0; index < objects.size(); index++) {
            offsets.add(pdf.toString().getBytes(StandardCharsets.UTF_8).length);
            pdf.append(index + 1)
                    .append(" 0 obj\n")
                    .append(objects.get(index))
                    .append("\nendobj\n");
        }

        int xrefStart = pdf.toString().getBytes(StandardCharsets.UTF_8).length;
        pdf.append("xref\n0 ").append(objects.size() + 1).append('\n');
        pdf.append("0000000000 65535 f \n");
        for (Integer offset : offsets) {
            pdf.append(String.format(Locale.ROOT, "%010d 00000 n \n", offset));
        }
        pdf.append("trailer << /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\n");
        pdf.append("startxref\n").append(xrefStart).append('\n');
        pdf.append("%%EOF");
        return pdf.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String sanitizeText(String value) {
        return value.replace("\n", " ").replace("\r", " ");
    }

    private String formatTimestamp(LocalDateTime timestamp) {
        return timestamp == null ? "" : timestamp.format(DATE_TIME_FORMATTER);
    }
}
