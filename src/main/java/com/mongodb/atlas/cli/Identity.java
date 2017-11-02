package com.mongodb.atlas.cli;

import org.apache.commons.cli.CommandLine;


public class Identity {
    private final String _username;
    private final String _apiKey;
    private final String _groupId;

     public Identity(final String pUsername, final String pApiKey, final String pGroupId) {
         this._username = pUsername;
         this._apiKey = pApiKey;
         this._groupId = pGroupId;
     }

    public String getApiKey() {
        return _apiKey;
    }

    public String getUsername() {
        return _username;
    }

    public String getGroupId() {
        return _groupId;
    }

    public static Identity fromCommandLine(final CommandLine pCommandLine) {
        final String[] userCredentials = pCommandLine.getOptionValue("u").split(":");
        final String username = userCredentials[0];
        final String apiKey = userCredentials[1];

        return new Identity(username, apiKey, pCommandLine.getOptionValue("g"));
    }
}
