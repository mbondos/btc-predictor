package tk.mbondos;

import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

import java.text.DecimalFormat;


class HoveredThresholdNode extends StackPane {

    /**
     * Overwrites default node to show value on mouseover.
     *
     * @param value      Value printed inside the node.
     * @param colorValue Id of one of javafx default colors. Range 0-7 inclusive.
     */
    HoveredThresholdNode(Number value, int colorValue) {
        setPrefSize(15, 15);

        final Label label = createDataThresholdLabel(value, colorValue);

        setOnMouseEntered(mouseEvent -> {
            getChildren().setAll(label);
            setCursor(Cursor.NONE);
            toFront();
        });
        setOnMouseExited(mouseEvent -> {
            getChildren().clear();
            setCursor(Cursor.CROSSHAIR);
        });
    }

    private Label createDataThresholdLabel(Number value, int colorValue) {
        if (colorValue < 0 || colorValue > 7) {
            throw new IllegalArgumentException("Field colorValue must be in range 0-7.");
        }
        DecimalFormat df = new DecimalFormat("####0.00");
        final Label label = new Label(df.format(value) + "");
        label.getStyleClass().addAll("default-color" + colorValue, "chart-line-symbol", "chart-series-line");
        label.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
        return label;
    }
}