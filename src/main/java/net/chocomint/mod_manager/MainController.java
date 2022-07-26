package net.chocomint.mod_manager;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import net.chocomint.mod_manager.exceptions.InvalidFilter;
import net.chocomint.mod_manager.exceptions.NoFilterVersion;
import net.chocomint.mod_manager.utils.DataSaver;
import net.chocomint.mod_manager.utils.ModrinthUtils;
import net.chocomint.mod_manager.utils.Utils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static net.chocomint.mod_manager.utils.ModrinthUtils.Information;
import static net.chocomint.mod_manager.utils.ModrinthUtils.Version;

public class MainController implements Initializable {
	public AnchorPane anchorPane;
	public Button ModPathChooser;
	public Button InstancesPathChooser;
	public Label ModPath;
	public Label InstancesPath;
	public Label status;
	public ListView<String> ModList;
	public TableView<VersionTable> versions;
	public TableColumn<VersionTable, String> name;
	public TableColumn<VersionTable, String> api;
	public TableColumn<VersionTable, String> gameVersion;
	public TableColumn<VersionTable, String> release;
	public Button moreInfo;
	public ListView<String> InstancesList;
	public Button editInstance;
	public TextField InstanceName;
	public Button saveInstance;
	public Button cancel;
	public Button addModButton;
	public ProgressBar progressBar;
	public Label progress;
	public TextField filter;
	public Label tableStatus;

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {

		Task<List<Information>> task = new Task<>() {
			@Override
			protected List<Information> call() throws IOException {

				addModButton.setDisable(true);
				updateMessage("Connecting to the Internet...");
				if (!Utils.isConnected()) {
					updateMessage("No Internet Accessible!");
					this.cancel();
					DataSaver.INTERNET_ACCESSIBLE = false;
					return null;
				}

				addModButton.setDisable(false);
				DataSaver.INTERNET_ACCESSIBLE = true;
				DataSaver.FETCH_SUCCESS = false;

				File config = new File("./config.txt");
				if (!config.exists()) config.createNewFile();
				Scanner scanner = new Scanner(config);
				List<String> lines = new ArrayList<>();
				updateMessage("Reading config");
				while (scanner.hasNextLine()) {
					String data = scanner.nextLine();
					lines.add(data);
				}

				if (lines.size() > 0 && !lines.get(0).isEmpty()) {
					DataSaver.MOD_PATH = Paths.get(lines.get(0));
					ModPath.setText(DataSaver.MOD_PATH.toString());
				}
				if (lines.size() > 1 && !lines.get(1).isEmpty()) {
					DataSaver.INSTANCES_PATH = Paths.get(lines.get(1));
					InstancesPath.setText(DataSaver.INSTANCES_PATH.toString());
				}

				List<Information> list = new ArrayList<>();
				if (lines.size() > 2) {
					updateProgress(0, 1);
					for (int i = 2; i < lines.size(); i++) {
						updateMessage("Fetching " + lines.get(i) + " (" + (i - 1) + "/" + (lines.size() - 2) + ")");
						list.add(ModrinthUtils.information(lines.get(i)));
						updateProgress(i - 1, lines.size() - 2);
					}
				}
				updateMessage("Success!");
				DataSaver.FETCH_SUCCESS = true;

				return list;
			}
		};
		Utils.bindProgress(progressBar, progress, task);
		status.textProperty().bind(task.messageProperty());

		task.setOnSucceeded(event -> {
			List<Information> list = task.getValue();
			list.forEach(info -> {
				DataSaver.MODS.put(info.modName(), info);
				ModList.getItems().add(info.modName());
			});
		});
		task.setOnCancelled(event -> {
			Alert noInternet = new Alert(Alert.AlertType.ERROR, "No Internet Access!");
			noInternet.setHeaderText("");
			noInternet.showAndWait().ifPresent(buttonType -> Platform.exit());
		});
		Thread thread = new Thread(task);
		thread.setDaemon(true);
		thread.start();

		name       .setCellValueFactory(new PropertyValueFactory<>("name"       ));
		api        .setCellValueFactory(new PropertyValueFactory<>("api"        ));
		gameVersion.setCellValueFactory(new PropertyValueFactory<>("gameVersion"));
		release    .setCellValueFactory(new PropertyValueFactory<>("release"    ));

		name       .prefWidthProperty().bind(versions.widthProperty().subtract(18).multiply(0.46));
		api        .prefWidthProperty().bind(versions.widthProperty().subtract(18).multiply(0.18));
		gameVersion.prefWidthProperty().bind(versions.widthProperty().subtract(18).multiply(0.18));
		release    .prefWidthProperty().bind(versions.widthProperty().subtract(18).multiply(0.18));

		saveInstance.disableProperty().bind(InstanceName.textProperty().isEmpty());

		filter.textProperty().addListener((observableValue, oldValue, newValue) -> changeVersionTable(newValue));
	}

	public void onModPathChooserClicked(MouseEvent mouseEvent) {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		File selectedDirectory = directoryChooser.showDialog(anchorPane.getScene().getWindow());
		if (selectedDirectory != null) {
			ModPath.setText(selectedDirectory.getPath());
			DataSaver.MOD_PATH = selectedDirectory.toPath();
		}
	}

	public void onInstancePathChooserClicked(MouseEvent mouseEvent) {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		File selectedDirectory = directoryChooser.showDialog(anchorPane.getScene().getWindow());
		if (selectedDirectory != null) {
			InstancesPath.setText(selectedDirectory.getPath());
			DataSaver.INSTANCES_PATH = selectedDirectory.toPath();
		}
	}

	public void onAddMod(ActionEvent actionEvent) {
		// run dialog code on FX thread
		TextInputDialog dialog = new TextInputDialog("fabric-api");
		dialog.setTitle("Add a mod from Modrinth");
		dialog.setHeaderText("");
		dialog.setContentText("Modrinth slug: ");
		Optional<String> opt = dialog.showAndWait();
		if (opt.isPresent()) {
			Task<Information> task = new Task<>() {
				@Override
				protected Information call() throws Exception {
					// this method is invoked on the background thread

					updateMessage("Fetching " + opt.get() + "..."); // update coalesced on FX thread
					Information info = ModrinthUtils.information(opt.get());
					updateMessage("Finished!"); // update coalesced on FX thread
					return info;
				}
			};
			status.textProperty().bind(task.messageProperty());

			task.setOnSucceeded(event -> {
				Information info = task.getValue();
				DataSaver.MODS.put(info.modName(), info);
				ModList.getItems().add(info.modName());
			});
			task.setOnFailed(event -> {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Error!");
				alert.setHeaderText("");
				alert.setContentText("Invalid slug!");
				alert.show();
			});

			Thread thread = new Thread(task); // 'Task' is a 'Runnable'
			thread.setDaemon(true);
			thread.start(); // start the background thread
		}
	}

	public void onRemove(ActionEvent actionEvent) {
		int index = ModList.getSelectionModel().getSelectedIndex();
		if (index != -1) {
			String selectedMod = ModList.getSelectionModel().getSelectedItem();
			Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to remove " + selectedMod + "?", ButtonType.YES, ButtonType.NO);
			alert.setTitle("Remove " + selectedMod);
			alert.setHeaderText("");
			Optional<ButtonType> opt = alert.showAndWait();
			if (opt.isPresent() && opt.get() == ButtonType.YES) {
				DataSaver.MODS.remove(selectedMod);
				ModList.getItems().remove(index);
			}
		}
	}

	public void onSelectMod(MouseEvent mouseEvent) {
		changeVersionTable(filter.getText());
	}

	public void onMoreInfo(ActionEvent actionEvent) throws IOException {
		Information info = DataSaver.MODS.get(ModList.getSelectionModel().getSelectedItem());
		if (info != null) {
			Desktop.getDesktop().browse(URI.create("https://modrinth.com/mod/" + info.slug()));
		}
	}

	public void openModsDirectory(MouseEvent mouseEvent) throws IOException {
		if (mouseEvent.getClickCount() == 2) {
			Desktop.getDesktop().open(new File(ModPath.getText()));
		}
	}

	public void openInstancesDirectory(MouseEvent mouseEvent) throws IOException {
		if (mouseEvent.getClickCount() == 2) {
			Desktop.getDesktop().open(new File(InstancesPath.getText()));
		}
	}

	public void onCreateNewInstance(ActionEvent actionEvent) {
		InstanceName.setDisable(false);
	}

	public void onSaveInstance(ActionEvent actionEvent) {
		InstancesList.getItems().add(InstanceName.getText());
		InstanceName.setText("");
		InstanceName.setDisable(true);
	}

	public void onEditInstance(ActionEvent actionEvent) {
	}

	public void onInstanceSelected(MouseEvent mouseEvent) {
		editInstance.setDisable(InstancesList.getSelectionModel().getSelectedItem() == null);
	}

	private void changeVersionTable(String filterString) {
		versions.getItems().clear();
		Information info = DataSaver.MODS.get(ModList.getSelectionModel().getSelectedItem());
		if (info != null) {
			try {
				tableStatus.setVisible(false);
				for (Version v : info.allVersion().filterVersions(filterString))
					versions.getItems().add(VersionTable.fromVersion(v));
			} catch (NoFilterVersion e) {
				tableStatus.setVisible(true);
				tableStatus.setText("No Filter Versions!");
			} catch (InvalidFilter e) {
				tableStatus.setVisible(true);
				tableStatus.setText("Invalid Filter!");
			}
		}
	}
}