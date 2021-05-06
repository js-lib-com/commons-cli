package com.jslib.commons.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConfigTest
{
  private Properties globalProperties = new Properties();
  @Mock
  private Properties projectProperties;

  private Config config;

  @Before
  public void beforeTest() throws IOException
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
}
