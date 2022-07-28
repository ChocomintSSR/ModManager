package net.chocomint.mod_manager.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.chocomint.mod_manager.exceptions.NotJsonFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataSaver {
	public static List<String> MOD_LIST = new ArrayList<>();
	public static Map<String, ModrinthUtils.ModInformation> MODS = new HashMap<>();
	public static Path MOD_PATH;
	public static Path INSTANCES_PATH;
	public static boolean FETCH_SUCCESS;
	public static boolean INTERNET_ACCESSIBLE;

	public static void onStop() throws IOException, NotJsonFile {
		File config = new File("./config.json");
		if (!config.exists()) config.createNewFile();

		JsonObject obj = new JsonObject();
		if (MOD_PATH != null) obj.addProperty("mod_path", MOD_PATH.toString());
		if (INSTANCES_PATH != null) obj.addProperty("instance_path", INSTANCES_PATH.toString());
		JsonArray modList_Json = new JsonArray();
		MOD_LIST.forEach(s -> modList_Json.add(MODS.get(s).slug()));
		obj.add("mod_list", modList_Json);

		Utils.writeToFile(config, obj);
	}
}
