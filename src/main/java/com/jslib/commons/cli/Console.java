package com.jslib.commons.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Console implements IConsole
{
  @Override
  public void print(String format, Object... args)
  {
    System.out.printf(format, args);
    System.out.println();
  }

  @Override
  public void print(char c)
  {
    System.out.print(c);
  }

  @Override
  public void print(Object object)
  {
    System.out.println(object.toString());
  }

  @Override
  public void info(String format, Object... args)
  {
    System.out.printf(format, args);
    System.out.println();
  }

  @Override
  public void info(Object object)
  {
    System.out.println(object.toString());
  }

  @Override
  public void warning(String format, Object... args)
  {
    System.out.printf(format, args);
    System.out.println();
  }

  @Override
  public void warning(Object object)
  {
    System.out.println(object.toString());
  }

  @Override
  public void error(String format, Object... args)
  {
    System.out.printf(format, args);
    System.out.println();
  }

  @Override
  public void error(Object object)
  {
    System.out.println(object.toString());
  }

  private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

  @Override
  public String input(String message, String... defaultValue) throws IOException
  {
    System.out.print("- ");
    System.out.print(message);
    System.out.print(": ");
    if(defaultValue.length == 1) {
      System.out.printf("[%s]: ", defaultValue[0]);
    }
    String value = reader.readLine();
    return value.isEmpty() ? defaultValue.length == 1 ? defaultValue[0] : value : value;
  }

  @Override
  public boolean confirm(String message, String positiveAnswer) throws IOException
  {
    System.out.print(message);
    System.out.print(": ");
    String answer = reader.readLine();
    return answer.equalsIgnoreCase(positiveAnswer);
  }

  @Override
  public void crlf()
  {
    System.out.println();
  }

  @Override
  public void stackTrace(Throwable t)
  {
    t.printStackTrace(System.out);
  }
}
