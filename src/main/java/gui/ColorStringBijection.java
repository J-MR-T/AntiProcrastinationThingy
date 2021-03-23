package gui;

import javafx.scene.paint.Color;
import javafx.util.StringConverter;

public class ColorStringBijection extends StringConverter<Color> {
    private final String whichObject;
    private final Color staticTextColor;

    public ColorStringBijection(String whichObject) {
        this(whichObject, Color.WHITE);
    }

    public ColorStringBijection() {
        this("fx-background-color");
    }

    public ColorStringBijection(String whichObject, Color staticTextColor) {
        this.whichObject = whichObject;
        this.staticTextColor = staticTextColor;
    }

    @Override
    public String toString(Color object) {
        return "-jfx-button-type: RAISED; -" + this.whichObject + ": #" + object.toString().substring(2) + "; " +
                "-fx-text-fill: #" + staticTextColor.toString().substring(2) + ";";
    }

    @Override
    public Color fromString(String string) {
        return Color.valueOf(string.split(";", 3)[1].split(": ")[1]);
    }
}
