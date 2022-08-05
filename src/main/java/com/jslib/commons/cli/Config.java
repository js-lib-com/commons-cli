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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

import com.jslib.api.dom.Document;
import com.jslib.api.dom.DocumentBuilder;
import com.jslib.api.dom.Element;
import com.jslib.converter.Converter;
import com.jslib.converter.ConverterRegistry;
import com.jslib.lang.BugError;
import com.jslib.util.Classes;
import com.jslib.util.Strings;

public class Config implements IConfig
{
  private static final String PROJECT_DESCRIPTOR_FILE = "project.xml";
  private static final String PROJECT_PROPERTIES_FILE = ".project.properties";

  private final Converter converter;

  private final Properties globalProperties;
  private final Properties projectProperties;
  private final File projectPropertiesFile;

  public Config(Properties globalProperties, Properties projectProperties) throws IOException, SAXException, XPathExpressionException
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

    File projectDescriptorFile = new File(workingDir, PROJECT_DESCRIPTOR_FILE);
    if(projectDescriptorFile.exists()) {
      injectDescriptorProperties(new FileReader(projectDescriptorFile));
    }

    // all CLI applications have a bin directory and a properties file there
    Path propertiesFile = null;
    if(Home.getPath() == null) {
      // TODO: hack
      propertiesFile = Paths.get("C://Users/DEV/wood.properties");
    }
    else {
      Path homeDir = Paths.get(Home.getPath());
      Path binDir = homeDir.resolve("bin");
      if(Files.exists(binDir)) {
        // findFirst returns and Optional the throws exception on get if there is no properties file found
        propertiesFile = Files.walk(binDir).filter(path -> path.getFileName().toString().endsWith(".properties")).findFirst().get();
      }
    }
    if(propertiesFile != null) {
      try (Reader reader = Files.newBufferedReader(propertiesFile)) {
        globalProperties.load(reader);
      }
    }
  }

  @Override
  public void injectDescriptorProperties(Reader descriptorReader) throws IOException, SAXException, XPathExpressionException
  {
    DocumentBuilder builder = Classes.loadService(DocumentBuilder.class);
    Document doc = builder.loadXML(descriptorReader);
    for(Element element : doc.findByXPath("//*[normalize-space(text())]")) {
      String key = key(element);
      if(!projectProperties.containsKey(key)) {
        projectProperties.put(key, element.getText());
      }
    }
  }

  private static String key(Element element)
  {
    List<String> parts = Strings.split(element.getTag(), '-');
    if(parts.size() == 1) {
      parts.add(0, "project");
    }
    return Strings.join(parts, '.');
  }

  @Override
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

  @Override
  public Properties getGlobalProperties() throws IOException
  {
    return globalProperties;
  }

  @Override
  public void updateGlobalProperties(Properties properties) throws IOException
  {
    properties.forEach((key, value) -> globalProperties.merge(key, value, (oldValue, newValue) -> newValue));
  }

  @Override
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

  @Override
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

  @Override
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

  @Override
  public <T> T getex(String key, Class<T> type, String... defaultValue) throws IOException
  {
    T value = get(key, type, defaultValue);
    if(value == null) {
      throw new BugError("Property not found |%s|.", key);
    }
    return value;
  }

  @Override
  public String get(String key, String... defaultValue) throws IOException
  {
    return get(key, String.class, defaultValue);
  }

  @Override
  public String getex(String key, String... defaultValue) throws IOException
  {
    return getex(key, String.class, defaultValue);
  }

  @Override
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
