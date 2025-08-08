package com.keycloak_event_listener.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;

public class ConfigLoader {
    private static final String CONFIG_PATH_ENV = "KEYCLOAK_LISTENER_CONFIG";
    private static final String DEFAULT_CONFIG_PATH = "/etc/keycloak-providers/config.yml";

    public static AppProperties load() throws IOException {
        String configPath = System.getenv(CONFIG_PATH_ENV);
        if (configPath == null || configPath.isEmpty()) {
            configPath = DEFAULT_CONFIG_PATH;
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        // We read the 'app' root key from the YAML
        return mapper.readValue(new File(configPath), AppPropertiesWrapper.class).getApp();
    }
    
    // Helper wrapper class to match the 'app' root key in YAML
    private static class AppPropertiesWrapper {
        private AppProperties app;
        public AppProperties getApp() { return app; }
        public void setApp(AppProperties app) { this.app = app; }
    }
}