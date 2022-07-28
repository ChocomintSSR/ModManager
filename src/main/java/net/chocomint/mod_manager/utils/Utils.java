package net.chocomint.mod_manager.utils;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringExpression;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import net.chocomint.mod_manager.exceptions.NotJsonFile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Utils {
	public static String firstUpper(String s) {
		return s.substring(0, 1).toUpperCase() + s.toLowerCase().substring(1, s.length());
	}

	public static boolean isConnected() {
		try {
			return isHostAvailable("google.com") || isHostAvailable("8.8.8.8");
		} catch (IOException e) {
			return false;
		}
	}

	private static boolean isHostAvailable(String hostName) throws IOException
	{
		try (Socket socket = new Socket()) {
			int port = 443; // https
			InetSocketAddress socketAddress = new InetSocketAddress(hostName, port);
			socket.connect(socketAddress, 2000);

			return true;
		} catch (UnknownHostException unknownHost) {
			return false;
		}
	}

	public static void bindProgress(ProgressBar progressBar, Label progress, Task<?> task) {
		progressBar.progressProperty().bind(task.progressProperty());
		progress.textProperty().bind(Utils.getTaskProgressStatus(task));
	}

	public static StringExpression getTaskProgressStatus(Task<?> task) {
		return Bindings.format("%.0f%%", task.progressProperty().multiply(100));
	}

	public static String toPrettyString(JsonObject json) {
		return new GsonBuilder().setPrettyPrinting().create().toJson(json);
	}

	public static void writeToFile(File file, JsonObject json) throws NotJsonFile, IOException {
		if (file.getName().matches(".*\\.json")) {
			FileWriter writer = new FileWriter(file);
			writer.write(toPrettyString(json));
			writer.close();
		} else throw new NotJsonFile();
	}

	public static OS getOS() {
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("win")) return OS.WINDOWS;
		if (os.contains("mac")) return OS.MAC;
		if (os.contains("nix") || os.contains("nux") || os.contains("aix")) return OS.LINUX;
		else return OS.OTHER;
	}

	public static Path getMinecraftPath() {
		return switch (Utils.getOS()) {
			case WINDOWS -> Paths.get(System.getenv("APPDATA"), ".minecraft");
			case LINUX -> Paths.get(System.getenv("HOME"), ".minecraft");
			case MAC -> Paths.get(System.getenv("HOME"), "Library", "Application Support", "minecraft");
			case OTHER -> null;
		};
	}

	public static void download(String url, String path, String fileName) throws IOException {
		URL u = new URL(url);
		try (InputStream in = u.openStream()) {
			Files.copy(in, Paths.get(path).resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	public enum OS {
		WINDOWS, LINUX, MAC, OTHER
	}
}
