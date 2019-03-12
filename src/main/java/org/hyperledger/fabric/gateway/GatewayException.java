package org.hyperledger.fabric.gateway;

public class GatewayException extends Exception {

  public GatewayException(String message) {
    super(message);
  }

  public GatewayException(Throwable cause) {
    super(cause);
  }

  public GatewayException(String message, Throwable cause) {
    super(message, cause);
  }

  public GatewayException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
