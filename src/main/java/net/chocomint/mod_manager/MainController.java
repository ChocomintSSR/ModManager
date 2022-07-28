package net.chocomint.mod_manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import net.chocomint.mod_manager.exceptions.InvalidFilter;
import net.chocomint.mod_manager.exceptions.NoFilterVersion;
import net.chocomint.mod_manager.utils.DataSaver;
import net.chocomint.mod_manager.utils.Instance;
import net.chocomint.mod_manager.utils.ModrinthUtils;
import net.chocomint.mod_manager.utils.Utils;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;

import static net.chocomint.mod_manager.utils.ModrinthUtils.ModInformation;
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
	public ImageView logo;
	public TextField GameVersion;

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {

		// Fetching Mods
		Task<List<ModInformation>> task = new Task<>() {
			@Override
			protected List<ModInformation> call() throws IOException {

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

				File config = new File("./config.json");
				if (!config.exists()) config.createNewFile();

				updateMessage("Reading config");
				updateProgress(0, 1);
				FileReader reader = new FileReader(config);
				List<ModInformation> list = new ArrayList<>();
				JsonElement element = JsonParser.parseReader(reader);
				if (!element.isJsonNull()) {
					JsonObject obj = element.getAsJsonObject();
					if (obj.get("mod_path") != null) {
						DataSaver.MOD_PATH = Paths.get(obj.get("mod_path").getAsString());
						ModPath.setText(DataSaver.MOD_PATH.toString());
					}
					if (obj.get("instance_path") != null) {
						DataSaver.MOD_PATH = Paths.get(obj.get("instance_path").getAsString());
						ModPath.setText(DataSaver.MOD_PATH.toString());
					}

					JsonArray modList = obj.getAsJsonArray("mod_list");
					if (modList != null) {
						int size = modList.size();
						for (int i = 0; i < size; i++) {
							updateMessage("Fetching " + modList.get(i).getAsString() + "... (" + (i + 1) + "/" + size + ")");
							list.add(ModrinthUtils.information(modList.get(i).getAsString()));
							updateProgress(i + 1, size);
						}
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
			List<ModInformation> list = task.getValue();
			list.forEach(this::addMod);
		});
		task.setOnCancelled(event -> {
			Alert noInternet = new Alert(Alert.AlertType.ERROR, "No Internet Access!");
			noInternet.setHeaderText("");
			noInternet.showAndWait().ifPresent(buttonType -> Platform.exit());
		});
		Thread thread = new Thread(task);
		thread.setDaemon(true);
		thread.start();

		// Init Mod Version Table
		name       .setCellValueFactory(new PropertyValueFactory<>("name"       ));
		api        .setCellValueFactory(new PropertyValueFactory<>("api"        ));
		gameVersion.setCellValueFactory(new PropertyValueFactory<>("gameVersion"));
		release    .setCellValueFactory(new PropertyValueFactory<>("release"    ));

		name       .prefWidthProperty().bind(versions.widthProperty().subtract(18).multiply(0.46));
		api        .prefWidthProperty().bind(versions.widthProperty().subtract(18).multiply(0.18));
		gameVersion.prefWidthProperty().bind(versions.widthProperty().subtract(18).multiply(0.18));
		release    .prefWidthProperty().bind(versions.widthProperty().subtract(18).multiply(0.18));

		// Init Buttons
		saveInstance.disableProperty().bind(InstanceName.textProperty().isEmpty());

		filter.textProperty().addListener((observableValue, oldValue, newValue) -> changeVersionTable(newValue));

		// Init ContextMenu
		ContextMenu menu = new ContextMenu();
		MenuItem add = new MenuItem("Add to instance...");
		add.setOnAction(actionEvent -> {
			ChoiceDialog<String> chooseInstance = new ChoiceDialog<>();
			InstancesList.getItems().forEach(chooseInstance.getItems()::add);
			chooseInstance.setHeaderText("");
			chooseInstance.setContentText("What instance: ");
			chooseInstance.show();
		});
		menu.getItems().add(add);
		versions.setContextMenu(menu);

		// Init Logo
		logo.setImage(new Image(Objects.requireNonNull(Main.class.getResourceAsStream("icon.png"))));
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
		dialog.setTitle("Add a modVersion from Modrinth");
		dialog.setHeaderText("");
		dialog.setContentText("Modrinth slug: ");
		Optional<String> opt = dialog.showAndWait();
		if (opt.isPresent()) {
			Task<ModInformation> task = new Task<>() {
				@Override
				protected ModInformation call() throws Exception {
					// this method is invoked on the background thread

					updateMessage("Fetching " + opt.get() + "..."); // update coalesced on FX thread
					ModInformation info = ModrinthUtils.information(opt.get());
					updateMessage("Finished!"); // update coalesced on FX thread
					return info;
				}
			};
			status.textProperty().bind(task.messageProperty());

			task.setOnSucceeded(event -> {
				ModInformation info = task.getValue();
				if (DataSaver.MOD_LIST.contains(info.modName())) {
					status.textProperty().unbind();
					status.setText(info.modName() + " has already in the list!");
				}
				else addMod(info);
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
				DataSaver.MOD_LIST.remove(selectedMod);
				DataSaver.MODS.remove(selectedMod);
				ModList.getItems().remove(index);
			}
		}
	}

	public void onSelectMod(MouseEvent mouseEvent) {
		changeVersionTable(filter.getText());
	}

	public void onMoreInfo(ActionEvent actionEvent) throws IOException {
		ModInformation info = DataSaver.MODS.get(ModList.getSelectionModel().getSelectedItem());
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
		GameVersion.setDisable(false);
	}

	public void onSaveInstance(ActionEvent actionEvent) {
		InstancesList.getItems().add(InstanceName.getText());
		InstanceName.setText("");
		InstanceName.setDisable(true);
		GameVersion.setText("");
		GameVersion.setDisable(true);
	}

	public void onEditInstance(ActionEvent actionEvent) {
	}

	public void onInstanceSelected(MouseEvent mouseEvent) {
		editInstance.setDisable(InstancesList.getSelectionModel().getSelectedItem() == null);
	}

	private void changeVersionTable(String filterString) {
		versions.getItems().clear();
		ModInformation info = DataSaver.MODS.get(ModList.getSelectionModel().getSelectedItem());
		if (info != null) {
			try {
				versions.setPlaceholder(new Label("No content in table"));
				for (Version v : info.allVersion().filterVersions(filterString))
					versions.getItems().add(VersionTable.fromVersion(v));
			} catch (NoFilterVersion e) {
				versions.setPlaceholder(new Label("No Filter Versions!"));
			} catch (InvalidFilter e) {
				versions.setPlaceholder(new Label("Invalid Filter!"));
			}
		}
	}

	private void addMod(ModInformation info) {
		String name = info.modName();
		DataSaver.MOD_LIST.add(name);
		DataSaver.MODS.put(name, info);
		ModList.getItems().add(name);
	}

	public void onChooseVersion(MouseEvent mouseEvent) {
		Version ver = versions.getSelectionModel().getSelectedItem().version;
		ModInformation mod = DataSaver.MODS.get(ModList.getSelectionModel().getSelectedItem());

		System.out.println(new Instance.Mod(mod.modName(), mod.slug(), ver).toJson());
	}
}