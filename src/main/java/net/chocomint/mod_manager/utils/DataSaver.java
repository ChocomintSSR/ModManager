package net.chocomint.mod_manager.utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class DataSaver {
	public static Map<String, ModrinthUtils.Information> MODS = new HashMap<>();
	public static Path MOD_PATH;
	public static Path INSTANCES_PATH;
	public static boolean FETCH_SUCCESS;
	public static boolean INTERNET_ACCESSIBLE;

	public static void onStop() throws IOException {
		File config = new File("./config.txt");
		if (!config.exists()) config.createNewFile();

		PrintWriter writer = new PrintWriter(config);
		writer.println(MOD_PATH != null ? MOD_PATH : "");
		writer.println(INSTANCES_PATH != null ? INSTANCES_PATH : "");
		MODS.forEach((s, information) -> writer.println(information.slug()));
		writer.close();
	}
}
