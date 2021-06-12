package com.jslib.commons.cli;

import java.io.IOException;
import java.io.Reader;
import java.util.Properties;
import java.util.SortedMap;

import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

public interface IConfig
{

  void injectDescriptorProperties(Reader descriptorReader) throws IOException, SAXException, XPathExpressionException;

  SortedMap<String, String> getProperties(boolean includeGlobal) throws IOException;

  Properties getGlobalProperties() throws IOException;

  void updateGlobalProperties(Properties properties) throws IOException;

  void put(String key, Object value) throws IOException;

  <T> T get(String key, Class<T> type, String... defaultValue) throws IOException;

  <T> T getex(String key, Class<T> type, String... defaultValue) throws IOException;

  String get(String key, String... defaultValue) throws IOException;

  String getex(String key, String... defaultValue) throws IOException;

  boolean has(String key) throws IOException;

  void remove(String key) throws IOException;

}