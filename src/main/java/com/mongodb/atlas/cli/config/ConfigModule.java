package com.mongodb.atlas.cli.config;

import com.mongodb.atlas.cli.CliModule;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Properties;


public class ConfigModule extends CliModule {

    public ConfigModule(final String[] pArgs) {
        super(pArgs);
    }

    @Override
    protected void _execute(final CommandLine pCommandLine) throws IOException, ParseException, URISyntaxException {
        final Properties properties = new Properties();
        properties.setProperty("atlas.username", getValueFromSystemInput("Enter Username"));
        properties.setProperty("atlas.apiKey", getValueFromSystemInput("Enter Api Key"));
        properties.setProperty("atlas.groupId", getValueFromSystemInput("Enter Group ID"));
        try (FileWriter fw = new FileWriter(getConfigFile())) {
            properties.store(fw, null);
        }

        System.out.println("Configuration saved successfully.");
    }

    private String getValueFromSystemInput(final String prompt) {
        System.out.print(String.format("%s:", prompt));
        try {
            final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            final String value = br.readLine();
            if (value == null || value.trim().equals("")) {
                return getValueFromSystemInput(prompt);
            }
            return value.trim();
        } catch (IOException e) {
            return getValueFromSystemInput(prompt);
        }
    }
}
