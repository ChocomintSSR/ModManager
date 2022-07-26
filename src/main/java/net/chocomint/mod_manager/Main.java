package net.chocomint.mod_manager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import net.chocomint.mod_manager.utils.DataSaver;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Objects;

public class Main extends Application {
	@Override
	public void start(Stage stage) throws IOException {
		FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("main.fxml"));
		Scene scene = new Scene(fxmlLoader.load(), 800, 550);
		stage.getIcons().add(new Image(Objects.requireNonNull(Main.class.getResourceAsStream("icon.png"))));
		stage.setTitle("Mod Manager");
		stage.setScene(scene);
		stage.setOnCloseRequest(windowEvent -> {
			if (!DataSaver.FETCH_SUCCESS) {
				Alert alert = new Alert(Alert.AlertType.WARNING, "Fetch doesn't finished.\nChanges before finishing fetching won't be saved.\nDo you want to close?", ButtonType.YES, ButtonType.CANCEL);
				alert.setHeaderText("");
				alert.showAndWait().filter(buttonType -> buttonType != ButtonType.YES).ifPresent(buttonType -> windowEvent.consume());
			}
		});
		stage.show();
	}

	@Override
	public void stop() throws Exception {
		if (DataSaver.INTERNET_ACCESSIBLE && DataSaver.FETCH_SUCCESS) DataSaver.onStop();
	}

	public static void main(String[] args) {
		launch();
	}
}