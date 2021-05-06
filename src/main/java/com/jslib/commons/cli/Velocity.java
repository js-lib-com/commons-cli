package com.jslib.commons.cli;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

public class Velocity
{
  private final VelocityEngine velocity;
  private final VelocityContext context;
  private final String templateResource;

  public Velocity(String templateResource)
  {
    this.velocity = new VelocityEngine();
    this.velocity.setProperty(RuntimeConstants.RESOURCE_LOADERS, "classpath");
    this.velocity.setProperty("resource.loader.classpath.class", ClasspathResourceLoader.class.getName());
    this.velocity.init();

    this.context = new VelocityContext();
    this.templateResource = templateResource;
  }

  public void put(String key, Object value)
  {
    context.put(key, value);
  }

  public void writeTo(Writer writer) throws IOException
  {
    try (Writer bufferedWriter = new BufferedWriter(writer)) {
      velocity.getTemplate(templateResource).merge(context, bufferedWriter);
    }
  }
}
