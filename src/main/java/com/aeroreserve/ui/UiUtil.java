package com.aeroreserve.ui;

import javafx.geometry.Insets;
import javafx.print.PrinterJob;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Window;
import javafx.util.Callback;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Small shared formatting helpers for controllers. */
public final class UiUtil {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private UiUtil() {
    }

    public static String money(double value) {
        return "$" + String.format("%,.2f", value);
    }

    public static String dateTime(LocalDateTime value) {
        return value == null ? "—" : DATE_TIME.format(value);
    }

    public static String date(LocalDate value) {
        return value == null ? "—" : DATE.format(value);
    }

    public static Label pill(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().addAll("pill", styleClass);
        return label;
    }

    public static boolean printText(String text, Window owner) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null || !job.showPrintDialog(owner)) {
            return false;
        }
        VBox page = new VBox(2);
        page.setPadding(new Insets(24));
        for (String line : text.split("\\R")) {
            Text row = new Text(line.isEmpty() ? " " : line);
            row.setFont(Font.font("Monospaced", 10));
            page.getChildren().add(row);
        }
        boolean printed = job.printPage(page);
        if (printed) {
            job.endJob();
        }
        return printed;
    }

    @FunctionalInterface
    public interface StyleResolver {
        String styleFor(String value);
    }

    public static <T> Callback<TableColumn<T, String>, TableCell<T, String>> pillCell(StyleResolver resolver) {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(pill(item, resolver.styleFor(item)));
                }
            }
        };
    }
}
