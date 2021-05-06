package com.jslib.commons.cli;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Version {
	private static final Pattern PATTERN = Pattern.compile(".*(\\d+)\\.(\\d+)\\.(\\d+)?-([a-z0-9])?.*", Pattern.CASE_INSENSITIVE);

	private final int major;
	private final int minor;
	private final int patch;
	private final String stage;

	public Version(String fileName) throws InvalidVersionException {
		Matcher matcher = PATTERN.matcher(fileName);
		if (!matcher.find()) {
			throw new InvalidVersionException(fileName);
		}
		this.major = number(matcher.group(1));
		this.minor = number(matcher.group(2));
		this.patch = number(matcher.group(3));
		this.stage = matcher.group(4);
	}

	private static int number(String value) {
		return value != null ? Integer.parseInt(value) : 0;
	}

	public int getMajor() {
		return major;
	}

	public int getMinor() {
		return minor;
	}

	public int getPatch() {
		return patch;
	}

	public String getStage() {
		return stage;
	}

	public boolean isAfter(Version other) {
		if (this.major > other.major) {
			return true;
		}
		if (this.minor > other.minor) {
			return true;
		}
		if (this.patch > other.patch) {
			return true;
		}
		return false;
	}

	public static class InvalidVersionException extends IOException {
		private static final long serialVersionUID = 2932642854326014446L;

		public InvalidVersionException(String fileName) {
			super(fileName);
		}
	}
}
