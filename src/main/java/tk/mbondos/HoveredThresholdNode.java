package tk.mbondos;

import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

import java.text.DecimalFormat;


class HoveredThresholdNode extends StackPane {
    HoveredThresholdNode(Number value) {
        setPrefSize(15, 15);

        final Label label = createDataThresholdLabel(value);

        setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                getChildren().setAll(label);
                setCursor(Cursor.NONE);
                toFront();
            }
        });
        setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                getChildren().clear();
                setCursor(Cursor.CROSSHAIR);
            }
        });
    }

    private Label createDataThresholdLabel(Number value) {
        DecimalFormat df= new DecimalFormat("####0.00");
        final Label label = new Label(df.format(value) + "");
        label.getStyleClass().addAll( "default-color7","chart-line-symbol", "chart-series-line");

      /*  label.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");*/


        label.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
        return label;
    }
}