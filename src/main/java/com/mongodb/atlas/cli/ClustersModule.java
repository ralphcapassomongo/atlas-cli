package com.mongodb.atlas.cli;

import com.mongodb.atlas.cli.clusters.ListModule;
import com.mongodb.atlas.cli.clusters.StatusModule;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;


public class ClustersModule extends CliModule {

    public ClustersModule(final String[] pArgs) {
        super(pArgs);
    }

    @Override
    public void _execute(final CommandLine pCommandLine) throws IOException, ParseException, URISyntaxException {
        validate(Arrays.asList("list", "status"));

        final String firstArg = getArgs()[0];

        final CliModule cliModule;
        final String[] aubArgs = Arrays.copyOfRange(getArgs(), 1, getArgs().length);
        if (firstArg.equals("status")) {
            cliModule = new StatusModule(aubArgs);
        } else {
            cliModule = new ListModule(aubArgs);
        }

        cliModule.execute();
    }
}
