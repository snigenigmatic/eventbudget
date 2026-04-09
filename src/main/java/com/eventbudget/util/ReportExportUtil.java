package com.eventbudget.util;

import com.eventbudget.model.domain.Budget;
import com.eventbudget.model.domain.BudgetCategory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class ReportExportUtil {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] exportBudgetCsv(Budget budget) {
        StringBuilder builder = new StringBuilder();
        builder.append("Budget ID,Event,Status,Total Amount,Created At,Closed At").append('\n');
        builder.append(budget.getBudgetId()).append(',')
                .append(escape(budget.getEvent().getName())).append(',')
                .append(budget.getStatus().name()).append(',')
                .append(budget.getTotalAmount()).append(',')
                .append(formatTimestamp(budget.getCreatedAt())).append(',')
                .append(formatTimestamp(budget.getClosedAt())).append('\n');
        builder.append('\n');
        builder.append("Category,Allocated,Committed,Approved,Available,Utilization %").append('\n');
        for (BudgetCategory category : budget.getCategories()) {
            builder.append(escape(category.getExpenseCategory().getName())).append(',')
                    .append(category.getAllocatedAmount()).append(',')
                    .append(category.getCommittedAmount()).append(',')
                    .append(category.getApprovedExpenditure()).append(',')
                    .append(category.getAvailableBalance()).append(',')
                    .append(category.getUtilizationPercent()).append('\n');
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportBudgetPdf(Budget budget) {
        // Build plain lines — no escaping yet
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
        lines.add(String.format("%-20s %10s %10s %10s %8s%%", "Category", "Allocated", "Committed", "Available", "Used"));
        lines.add("-------------------------------------------");
        for (BudgetCategory category : budget.getCategories()) {
            lines.add(String.format("%-20s %10s %10s %10s %8s%%",
                    sanitizeText(category.getExpenseCategory().getName()),
                    category.getAllocatedAmount(),
                    category.getCommittedAmount(),
                    category.getAvailableBalance(),
                    category.getUtilizationPercent()));
        }

        // Escape PDF string specials per line, then write into content stream
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
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
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

    private String escape(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String sanitizeText(String value) {
        return value.replace("\n", " ").replace("\r", " ");
    }

    private String formatTimestamp(java.time.LocalDateTime timestamp) {
        return timestamp == null ? "" : timestamp.format(DATE_TIME_FORMATTER);
    }
}
