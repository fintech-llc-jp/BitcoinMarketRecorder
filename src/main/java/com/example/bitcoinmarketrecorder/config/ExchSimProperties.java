package com.example.bitcoinmarketrecorder.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "exch-sim")
public class ExchSimProperties {

  private Api api = new Api();
  private boolean enabled = false;
  private Map<String, Map<String, String>> symbolMapping;

  public Api getApi() {
    return api;
  }

  public void setApi(Api api) {
    this.api = api;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Map<String, Map<String, String>> getSymbolMapping() {
    return symbolMapping;
  }

  public void setSymbolMapping(Map<String, Map<String, String>> symbolMapping) {
    this.symbolMapping = symbolMapping;
  }

  public String mapSymbol(String exchange, String symbol) {
    if (symbolMapping == null || symbolMapping.get(exchange) == null) {
      return null;
    }
    return symbolMapping.get(exchange).get(symbol);
  }

  public static class Api {
    private String baseUrl = "http://localhost:8080";
    private String username = "marketmaker1";
    private String password = "mmpass123";

    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }
  }
}
