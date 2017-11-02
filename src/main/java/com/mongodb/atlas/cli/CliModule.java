package com.mongodb.atlas.cli;

import org.apache.commons.cli.*;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.*;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.json.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;


public abstract class CliModule {

    protected static final String HOST = "localhost";
    protected static final int PORT = 8080;
    protected static final String SCHEME = "http";

    private static final int JSON_INDENT = 3;

    protected static final String BASE_URL =
        String.format("%s://%s%s/api/atlas/v1.0/groups", SCHEME, HOST, (PORT != 80 && PORT != 443) ? (":" + PORT) : "");

    private final String[] _args;

    protected String[] getArgs() {
        return _args;
    }

    private final Logger _logger = LoggerFactory.getLogger(this.getClass());

    public CliModule(final String[] pArgs) {
        this._args = pArgs;
    }

    protected CloseableHttpClient createHttpClient(final Identity pCredentials) {
        final HttpHost target = new HttpHost(HOST, PORT, SCHEME);
        final CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
            new AuthScope(target.getHostName(), target.getPort()),
            new UsernamePasswordCredentials(pCredentials.getUsername(), pCredentials.getApiKey()));
        final CloseableHttpClient httpclient = HttpClients.custom()
            .setDefaultCredentialsProvider(credsProvider)
            .build();

        // Create AuthCache instance
        final AuthCache authCache = new BasicAuthCache();
        // Generate DIGEST scheme object, initialize it and add it to the local
        // auth cache
        final DigestScheme digestAuth = new DigestScheme();
        authCache.put(target, digestAuth);

        // Add AuthCache to the execution context
        final HttpClientContext localContext = HttpClientContext.create();
        localContext.setAuthCache(authCache);

        return httpclient;
    }

    private boolean hasIdentity(final CommandLine pCommandLine) {
        return pCommandLine.hasOption("u") && pCommandLine.hasOption("g");
    }

    protected Identity getIdentity(final CommandLine pCommandLine) throws IOException {
        if (hasConfigFile() && !hasIdentity(pCommandLine)) {
            final File configFile = getConfigFile();
            final Properties properties = new Properties();

            try (FileInputStream fis = new FileInputStream(configFile)) {
                properties.load(fis);
            }

            final String username = properties.getProperty("atlas.username");
            final String apiKey = properties.getProperty("atlas.apiKey");
            final String groupId = properties.getProperty("atlas.groupId");

            if (username == null || apiKey == null || groupId == null) {
                throw new IllegalStateException("Invalid config file.  Run atlas config to rebuild.");
            }

            return new Identity(username, apiKey, groupId);
        }
        return Identity.fromCommandLine(pCommandLine);
    }

    private boolean hasConfigFile() {
        return getConfigFile().exists();
    }

    protected File getConfigFile() {
        return new File(System.getProperty("user.home"), ".atlas");
    }

    protected Options buildOptions() {
        final Options options = new Options();

        options.addOption(Option.builder("h")
            .argName("Help")
            .longOpt("help")
            .hasArg(true)
            .optionalArg(!hasConfigFile())
            .desc("Help!")
            .build());

        options.addOption(Option.builder("u")
            .argName("User Identity")
            .longOpt("user-credentials")
            .hasArg(true)
            .optionalArg(!hasConfigFile())
            .desc("username:apiKey")
            .required(!hasConfigFile())
            .build());

        options.addOption(Option.builder("g")
            .argName("Group ID")
            .longOpt("group")
            .hasArg(true)
            .optionalArg(!hasConfigFile())
            .desc("Group ID")
            .required(!hasConfigFile())
            .build());

        return options;
    }

    protected Logger getLogger() {
        return _logger;
    }

    public void execute() throws IOException, ParseException, URISyntaxException {
        final CommandLine commandLine = getCommandLine();
        if (commandLine.hasOption("h")) {
            printHelp();
        }
        _execute(commandLine);
    }

    private void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("atlas", buildOptions());
    }

    protected abstract void _execute(final CommandLine pCommandLine)
        throws IOException, ParseException, URISyntaxException;

    protected JSONObject getResults(final Identity pCredentials, final URI pURI)
        throws IOException
    {
        final HttpGet httpGet = new HttpGet(pURI);
        final CloseableHttpClient httpClient = createHttpClient(pCredentials);
        try {
            final CloseableHttpResponse response = httpClient.execute(httpGet);

            try {
                return new JSONObject(EntityUtils.toString(response.getEntity()));
            } finally {
                response.close();
            }
        } finally {
            httpClient.close();
        }
    }

    protected JSONObject postResults(final Identity pCredentials, final URI pURI, final JSONObject pRequestBody)
        throws IOException
    {
        final HttpPost httpPost = new HttpPost(pURI);

        httpPost.setEntity(new StringEntity(pRequestBody.toString()));
        final CloseableHttpClient httpClient = createHttpClient(pCredentials);
        try {
            final CloseableHttpResponse response = httpClient.execute(httpPost);

            try {
                return new JSONObject(EntityUtils.toString(response.getEntity()));
            } finally {
                response.close();
            }
        } finally {
            httpClient.close();
        }
    }

    protected CommandLine getCommandLine() throws ParseException {
        final CommandLineParser parser = new DefaultParser();
        return parser.parse(buildOptions(), _args);
    }

    protected void outputGetResults(final Identity pIdentity, final URI pURI) throws IOException {
        System.out.println(getResults(pIdentity, pURI).toString(JSON_INDENT));
    }

    protected void outputPostResults(final Identity pIdentity, final URI pURI, final JSONObject pRequestBody)
        throws IOException
    {
        System.out.println(postResults(pIdentity, pURI, pRequestBody).toString(JSON_INDENT));
    }

    protected void validate(final List<String> pValidArgs) {
        final String[] args = getArgs();
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("No valid command specified.");
        }

        if (!pValidArgs.contains(args[0])) {
            throw new IllegalArgumentException(String.format(
                "Uncrecognized command: %s\nValid Arguments: %s",
                args[0],
                toValidArgs(pValidArgs)));
        }

    }

    private String toValidArgs(final List<String> pValidArgs) {
        return pValidArgs.stream()
            .collect(Collectors.joining("\n"));

    }
}
