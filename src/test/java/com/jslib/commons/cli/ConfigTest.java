package com.jslib.commons.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Properties;

import javax.xml.xpath.XPathExpressionException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.xml.sax.SAXException;

@RunWith(MockitoJUnitRunner.class)
public class ConfigTest
{
  private Properties globalProperties = new Properties();
  @Mock
  private Properties projectProperties;

  private Config config;

  @Before
  public void beforeTest() throws IOException, XPathExpressionException, SAXException
  {
    Home.setPath("D:\\wood-1.0");
    config = new Config(globalProperties, projectProperties);
  }

  @Test
  public void get_GivenValueHasSystemProperty_ThenInjectionIt() throws IOException
  {
    // given
    System.setProperty("WOOD_HOME", "D:\\wood-1.0");
    globalProperties.put("repository.dir", "${WOOD_HOME}\repository");

    // when
    String property = config.get("repository.dir");

    // then
    assertThat(property, equalTo("D:\\wood-1.0\repository"));
  }

  @Test
  public void updateGlobalProperties_GivenMissingProperty_ThenAddIt() throws IOException
  {
    // given
    Properties properties = new Properties();
    properties.put("user.name", "Iulian Rotaru");

    // when
    config.updateGlobalProperties(properties);

    // then
    assertThat(globalProperties.get("user.name"), equalTo("Iulian Rotaru"));
  }

  @Test
  public void updateGlobalProperties_GivenPropertyExists_ThenUpdateIt() throws IOException
  {
    // given
    globalProperties.put("user.name", "Iulian Rotaru");

    Properties properties = new Properties();
    properties.put("user.name", "Iulian Rotaru Sr.");

    // when
    config.updateGlobalProperties(properties);

    // then
    assertThat(globalProperties.get("user.name"), equalTo("Iulian Rotaru Sr."));
  }

  @Test
  public void updateGlobalProperties_GivenNewPropertiesEmpty_ThenPreserveGlobal() throws IOException
  {
    // given
    globalProperties.put("user.name", "Iulian Rotaru");

    Properties properties = new Properties();

    // when
    config.updateGlobalProperties(properties);

    // then
    assertThat(globalProperties.get("user.name"), equalTo("Iulian Rotaru"));
  }

  @Test
  public void GivenProjectDescriptor_WhenInjectDescriptorProperties_ThenInsertProperties() throws XPathExpressionException, IOException, SAXException
  {
    // given
    projectProperties = new Properties();
    config = new Config(globalProperties, projectProperties);

    String descriptor = "" + //
        "<project>" + //
        "   <name>fables</name>" + //
        "   <build-dir>target/site</build-dir>" + //
        "   <exclude-dirs>src</exclude-dirs>" + //
        "   <display> </display>" + //
        "   <description></description>" + //
        "   <head>" + //
        "       <script src=''></script>" + //
        "       <script src=''/>" + //
        "   </head>" + //
        "</project>";
    Reader descriptorReader = new StringReader(descriptor);

    // when
    config.injectDescriptorProperties(descriptorReader);

    // then
    assertThat(projectProperties.size(), equalTo(3));
    assertThat(projectProperties.get("project.name"), equalTo("fables"));
    assertThat(projectProperties.get("build.dir"), equalTo("target/site"));
    assertThat(projectProperties.get("exclude.dirs"), equalTo("src"));
  }
}
