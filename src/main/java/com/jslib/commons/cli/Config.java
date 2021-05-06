package com.jslib.commons.cli;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import js.converter.Converter;
import js.converter.ConverterRegistry;
import js.lang.BugError;
import js.util.Strings;

public class Config
{
  private static final String PROJECT_PROPERTIES_FILE = ".project.properties";

  private final Converter converter;

  private final Properties globalProperties;
  private final Properties projectProperties;
  private final File projectPropertiesFile;

  public Config(Properties globalProperties, Properties projectProperties) throws IOException
  {
    this.converter = ConverterRegistry.getConverter();
    this.globalProperties = globalProperties;
    this.projectProperties = projectProperties;

    File workingDir = Paths.get("").toAbsolutePath().toFile();
    this.projectPropertiesFile = new File(workingDir, PROJECT_PROPERTIES_FILE);
    if(this.projectPropertiesFile.exists()) {
      try (Reader reader = new FileReader(projectPropertiesFile)) {
        projectProperties.load(reader);
      }
    }

    // all CLI applications have a bin directory and a properties file there
    Path homeDir = Paths.get(Home.getPath());
    Path binDir = homeDir.resolve("bin");
    if(Files.exists(binDir)) {
      Path propertiesFile = Files.walk(binDir).filter(path -> path.getFileName().toString().endsWith(".properties")).findFirst().get();
      if(Files.exists(propertiesFile)) {
        try (Reader reader = Files.newBufferedReader(propertiesFile)) {
          globalProperties.load(reader);
        }
      }
    }
  }

  public SortedMap<String, String> getProperties(boolean includeGlobal) throws IOException
  {
    SortedMap<String, String> properties = new TreeMap<>();
    for(Map.Entry<Object, Object> entry : projectProperties.entrySet()) {
      properties.put(entry.getKey().toString(), entry.getValue().toString());
    }
    if(includeGlobal) {
      for(Map.Entry<Object, Object> entry : globalProperties.entrySet()) {
        properties.put(entry.getKey().toString(), entry.getValue().toString());
      }
    }
    return properties;
  }

  public Properties getGlobalProperties() throws IOException
  {
    return globalProperties;
  }

  public void updateGlobalProperties(Properties properties) throws IOException
  {
    properties.forEach((key, value) -> globalProperties.merge(key, value, (oldValue, newValue) -> newValue));
  }

  public void put(String key, Object value) throws IOException
  {
    if(!projectPropertiesFile.exists()) {
      throw new BugError("Attempt to alter proporties outside project.");
    }
    projectProperties.put(key, converter.asString(value));
    try (Writer writer = new FileWriter(projectPropertiesFile)) {
      projectProperties.store(writer, "project properties");
    }
  }

  public void remove(String key) throws IOException
  {
    if(!projectPropertiesFile.exists()) {
      throw new BugError("Attempt to alter proporties outside project.");
    }
    projectProperties.remove(key);
    try (Writer writer = new FileWriter(projectPropertiesFile)) {
      projectProperties.store(writer, "project properties");
    }
  }

  public <T> T get(String key, Class<T> type, String... defaultValue) throws IOException
  {
    Object value = projectProperties.get(key);
    if(value == null) {
      value = globalProperties.get(key);
      if(value == null) {
        if(defaultValue.length == 1) {
          value = defaultValue[0];
        }
        else {
          return null;
        }
      }
    }
    return converter.asObject(Strings.injectProperties(value.toString()), type);
  }

  public <T> T getex(String key, Class<T> type, String... defaultValue) throws IOException
  {
    T value = get(key, type, defaultValue);
    if(value == null) {
      throw new BugError("Property not found |%s|.", key);
    }
    return value;
  }

  public String get(String key, String... defaultValue) throws IOException
  {
    return get(key, String.class, defaultValue);
  }

  public String getex(String key, String... defaultValue) throws IOException
  {
    return get(key, String.class, defaultValue);
  }

  public boolean has(String key) throws IOException
  {
    Object value = projectProperties.get(key);
    if(value == null) {
      value = globalProperties.get(key);
      if(value == null) {
        return false;
      }
    }
    return true;
  }
}
