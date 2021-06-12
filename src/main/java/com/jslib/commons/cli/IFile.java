package com.jslib.commons.cli;

import java.net.URI;
import java.time.LocalDateTime;

public interface IFile
{

  URI getURI();

  String getName();

  LocalDateTime getModificationTime();

  long getSize();

  boolean isAfter(IFile other);

}