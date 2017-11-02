package com.mongodb.atlas.cli;

import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


public class AtlasCliApp {

    private static final Logger _logger = LoggerFactory.getLogger(AtlasCliApp.class);

    public static void main(String[] args) {
        try {
            new AtlasModule(args).execute();
        } catch (IOException | ParseException e) {
            System.err.println(String.format("Error handling your request: %s", e.getMessage()));
            //_logger.error("Exception thrown while processing the request", e);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        } catch (Exception e) {
            System.err.println(String.format("An unexpected error occurred: %s", e.getMessage()));
            //_logger.error("Exception thrown while processing the request", e);
        }
    }

}
