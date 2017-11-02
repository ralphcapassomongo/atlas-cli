package com.mongodb.atlas.cli.clusters;

import com.mongodb.atlas.cli.CliModule;
import com.mongodb.atlas.cli.Identity;
import org.apache.commons.cli.*;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;


public class CreateModule extends CliModule {

    public CreateModule(final String[] pArgs) {
        super(pArgs);
    }

    @Override
    protected Options buildOptions() {
        final Options options = super.buildOptions();

        // cluster name
        options.addOption(Option.builder("cn")
            .argName("Cluster Name")
            .longOpt("clusterName")
            .hasArg(true)
            .optionalArg(true)
            .desc("Name of a specific cluster to be queried.")
            .required()
            .build());

        options.addOption(Option.builder("p")
            .argName("Cloud Provider")
            .longOpt("provider")
            .hasArg(true)
            .optionalArg(false)
            .desc("Name of a specific cluster to be queried.")
            .required()
            .build());

        options.addOption(Option.builder("instanceSize")
            .argName("Instance Size")
            .longOpt("instanceSize")
            .hasArg(true)
            .optionalArg(false)
            .desc("Desired instance size (M10, M20, etc.).")
            .required()
            .build());

        options.addOption(Option.builder("region")
            .argName("Region")
            .longOpt("regionName")
            .hasArg(true)
            .optionalArg(false)
            .desc("Desired provider-specific region (US_EAST_1, US_WEST_1, etc.).")
            .required()
            .build());

        options.addOption(Option.builder("backup")
            .argName("Backup")
            .longOpt("backupEnabled")
            .hasArg(true)
            .optionalArg(false)
            .desc("Enable backup on the cluster.")
            .required()
            .build());

        return options;
    }

    @Override
    protected void _execute(final CommandLine pCommandLine) throws IOException, ParseException, URISyntaxException {
        final Identity identity = getIdentity(pCommandLine);

        final JSONObject clusterRequestBody = new JSONObject();
        clusterRequestBody.put("name", pCommandLine.getOptionValue("cn"));
        clusterRequestBody.put("backupEnabled", pCommandLine.getOptionValue("backup"));

        final JSONObject providerSettings = new JSONObject();
        providerSettings.put("providerName", pCommandLine.getOptionValue("p"));
        providerSettings.put("instanceSizeName", pCommandLine.getOptionValue("instanceSize"));
        providerSettings.put("regionName", pCommandLine.getOptionValue("region"));

        clusterRequestBody.put("providerSettings", providerSettings);

        outputPostResults(identity, new URI(String.format(
            "%s/%s/clusters%s",
            BASE_URL,
            identity.getGroupId())),
            clusterRequestBody);
    }
}
