/*
 * Copyright (c) 2009, CoreMedia AG, Hamburg. All rights reserved.
 */
package net.jangaroo.extxml;

public final class Log {

  private static ErrorHandler errorHandler;

  private Log() {
    
  }

  public static void setErrorHandler(ErrorHandler handler) {
    errorHandler = handler;
  }

  public static ErrorHandler getErrorHandler() {
    if(errorHandler == null) {
      errorHandler = new StandardOutErrorHandler();
    }
    return errorHandler;
  }
}