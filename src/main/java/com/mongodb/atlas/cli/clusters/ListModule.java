package com.mongodb.atlas.cli.clusters;

import com.mongodb.atlas.cli.CliModule;
import com.mongodb.atlas.cli.Identity;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;


public class ListModule extends CliModule {

    public ListModule(final String[] pArgs) {
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
            .build());

        return options;
    }

    @Override
    protected void _execute(final CommandLine pCommandLine) throws IOException, ParseException, URISyntaxException {
        final Identity identity = getIdentity(pCommandLine);
        final String clusterName = pCommandLine.getOptionValue("cn");

        outputGetResults(identity, new URI(String.format(
            "%s/%s/clusters%s?pretty=true",
            BASE_URL,
            identity.getGroupId(),
            clusterName != null ? "/" + clusterName : "")));
    }
}
