package net.chocomint.mod_manager;

import net.chocomint.mod_manager.utils.ModrinthUtils;

public class VersionTable {
	public final ModrinthUtils.Version version;
	private final String name;
	private final String api;
	private final String gameVersion;
	private final String release;

	public VersionTable(ModrinthUtils.Version v, String name, String api, String versionCode, String release) {
		this.version = v;
		this.name = name;
		this.api = api;
		this.gameVersion = versionCode;
		this.release = release;
	}

	public static VersionTable fromVersion(ModrinthUtils.Version version) {
		return new VersionTable(version, version.name(), version.apis().toString().replaceAll("\\[", "").replaceAll("]", ""), version.gameVersion(), version.channel().toString());
	}

	public String getName() {
		return name;
	}

	public String getApi() {
		return api;
	}

	public String getGameVersion() {
		return gameVersion;
	}

	public String getRelease() {
		return release;
	}
}
