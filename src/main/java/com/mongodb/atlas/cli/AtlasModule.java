package com.mongodb.atlas.cli;

import com.mongodb.atlas.cli.config.ConfigModule;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;


public class AtlasModule extends CliModule {

    public AtlasModule(final String[] pArgs) {
        super(pArgs);
    }

    @Override
    protected void _execute(final CommandLine pCommandLine) throws IOException, ParseException, URISyntaxException {
        validate(Arrays.asList("config", "clusters"));

        final String firstArg = getArgs()[0];
        final String[] subArgs = Arrays.copyOfRange(getArgs(), 1, getArgs().length);
        if (firstArg.equals("config")) {
            new ConfigModule(subArgs).execute();
        } else if (firstArg.equals("clusters")) {
            new ClustersModule(subArgs).execute();
        }
    }
}
