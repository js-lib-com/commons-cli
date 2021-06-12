package com.jslib.commons.cli;

import java.io.IOException;

public interface IConsole
{

  void print(String format, Object... args);

  void print(char c);

  void print(Object object);

  void info(String format, Object... args);

  void info(Object object);

  void warning(String format, Object... args);

  void warning(Object object);

  void error(String format, Object... args);

  void error(Object object);

  String input(String message, String... defaultValue) throws IOException;

  boolean confirm(String message, String positiveAnswer) throws IOException;

  void crlf();

  void stackTrace(Throwable t);

}