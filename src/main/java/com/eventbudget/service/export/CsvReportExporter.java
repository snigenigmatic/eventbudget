package com.eventbudget.service.export;

import com.eventbudget.model.domain.Budget;
import com.eventbudget.model.domain.BudgetCategory;
import com.eventbudget.model.domain.ExportFormat;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * ADAPTER PATTERN (Structural) — Concrete adapter that adapts a {@link Budget}
 * domain aggregate into a CSV byte stream.
 *
 * <p>The caller ({@code FinanceService}) programs against the
 * {@link ReportExporter} target interface and is unaware that this adapter
 * produces CSV.
 */
@Component
public class CsvReportExporter implements ReportExporter {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public ExportFormat getFormat() {
        return ExportFormat.CSV;
    }

    @Override
    public byte[] export(Budget budget) {
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

    private String escape(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String formatTimestamp(LocalDateTime timestamp) {
        return timestamp == null ? "" : timestamp.format(DATE_TIME_FORMATTER);
    }
}
