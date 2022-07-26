package net.chocomint.mod_manager.utils;

import net.chocomint.mod_manager.exceptions.InvalidFilter;
import net.chocomint.mod_manager.exceptions.NoFilterVersion;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ModrinthUtils {
	public static Information information(String slug) throws IOException {
		Document document = Jsoup.connect("https://modrinth.com/mod/" + slug).get();
		String title = document.getElementsByClass("title").get(0).text();
		String id = document.getElementsByClass("value lowercase").get(0).text();

		return new Information(title, slug, id, allVersions(slug));
	}

	public static VersionList allVersions(String slug) throws IOException {
		Document document = Jsoup.connect("https://modrinth.com/mod/" + slug + "/versions").get();
		Element tbody = document.selectFirst("tbody");
		assert tbody != null;
		VersionList versions = new VersionList();
		for (Element tr : tbody.select("tr")) {
			URL url = new URL(tr.child(0).child(0).attr("href"));
			String name = tr.child(1).child(0).child(0).child(0).text();

			List<API> apis = new ArrayList<>();
			for (String api : tr.child(2).child(0).text().split(",")) {
				apis.add(API.fromString(api.replaceAll(" ", "")));
			}

			String gameVersion = tr.child(2).child(1).text();
			Channel channel = Channel.fromString(tr.child(1).child(0).child(1).child(0).text());
			versions.add(new Version(url, name, apis, gameVersion, channel));
		}

		return versions;
	}

	public static class VersionList extends ArrayList<Version> {
		public VersionList filterVersions(String s) throws NoFilterVersion, InvalidFilter {
			VersionList list = new VersionList();
			String regex;
			if (s.isEmpty()) return this;
			else if (s.matches("[1-9]+\\.[1-9]+")) {
				regex = s.replaceAll("\\.", "\\.") + "(\\.[1-9]+)?";
			} else if (s.matches("[1-9]+\\.[1-9]+(\\.[1-9]*)?")) {
				regex = s.replaceAll("\\.", "\\.");
			} else throw new InvalidFilter();
			this.stream().filter(version -> version.gameVersion.matches(regex)).forEach(list::add);
			if (list.size() == 0) throw new NoFilterVersion();
			return list;
		}

		public void print() {
			this.forEach(Version::print);
		}
	}

	public record Information(String modName, String slug, String projectID, VersionList allVersion) {
		public void print() {
			System.out.println("Mod Name  : " + modName + " [" + slug + "]");
			System.out.println("Project ID: " + projectID + "\n");
			allVersion.forEach(Version::print);
		}
	}

	public record Version(URL downloadLink, String name, List<API> apis, String gameVersion, Channel channel) {
		public void print() {
			System.out.println(name + " [" + channel + "]");
			System.out.println("  Accept : " + apis.toString().replaceAll("\\[", "").replaceAll("]", ""));
			System.out.println("  Version: " + gameVersion + "\n");
		}
	}

	public enum API {
		FABRIC, FORGE, RISUGAMI, QUILT, RIFT;

		public static API fromString(String value) {
			if (value == null || value.length() < 1)
				return null;

			for (API t : values()) {
				if (t.name().equalsIgnoreCase(value))
					return t;
			}

			return null;
		}

		@Override
		public String toString() {
			return Utils.firstUpper(this.name());
		}
	}

	public enum Channel {
		RELEASE, ALPHA, BETA;

		public static Channel fromString(String value) {
			if (value == null || value.length() < 1)
				return null;

			for (Channel t : values()) {
				if (t.name().equalsIgnoreCase(value))
					return t;
			}

			return null;
		}

		@Override
		public String toString() {
			return Utils.firstUpper(this.name());
		}
	}
}
