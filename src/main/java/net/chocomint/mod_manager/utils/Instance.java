package net.chocomint.mod_manager.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

public record Instance(String name, String version, List<Mod> mods) {
	public JsonObject toJson() {
		JsonObject obj = new JsonObject();
		obj.addProperty("name", name);
		obj.addProperty("version", version);

		JsonArray modList = new JsonArray();
		mods.forEach(mod -> modList.add(mod.toJson()));
		obj.add("mod_list", modList);

		return obj;
	}

	public record Mod(String modName, String slug, ModrinthUtils.Version modVersion) {
		public JsonObject toJson() {
			JsonObject obj = new JsonObject();
			obj.addProperty("mod_name", modName);
			obj.addProperty("slug", slug);
			obj.addProperty("url", modVersion.downloadLink().toExternalForm());
			obj.addProperty("version_name", modVersion.name());

			JsonArray apiList = new JsonArray();
			modVersion.apis().forEach(api -> apiList.add(api.name()));
			obj.add("apis", apiList);

			return obj;
		}
	}
}
