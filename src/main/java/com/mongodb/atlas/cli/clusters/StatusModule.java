package com.mongodb.atlas.cli.clusters;

import com.mongodb.atlas.cli.CliModule;
import com.mongodb.atlas.cli.Identity;
import org.apache.commons.cli.*;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.IntStream;


public class StatusModule extends CliModule {

    private static Logger logger = LoggerFactory.getLogger(StatusModule.class);

    private static final int LOOKBACK_MINUTES = 20;

    public StatusModule(final String[] pArgs) {
        super(pArgs);
    }

    @Override
    protected Options buildOptions() {
        final Options options = super.buildOptions();

        // refresh
        options.addOption(Option.builder("r")
            .argName("Refresh")
            .longOpt("refresh")
            .hasArg(true)
            .optionalArg(true)
            .desc("Refresh intervsal in seconds.")
            .build());

        return options;
    }

    @Override
    protected void _execute(final CommandLine pCommandLine) throws IOException, ParseException, URISyntaxException {
        final Identity identity = getIdentity(pCommandLine);

        if (pCommandLine.hasOption("r")) {
            while (true) {
                final String refreshOptionValue = pCommandLine.getOptionValue("r");
                final long sleepMs;
                if (refreshOptionValue != null) {
                    try {
                        sleepMs = Long.parseLong(refreshOptionValue) * 1000;
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(String.format(
                            "Invalid refresh uration specified: %s",
                            refreshOptionValue));
                    }
                } else {
                    // default to 5 seconds
                    sleepMs = 5000;
                }

                System.out.print("\033[H\033[2J");
                System.out.flush();
                System.out.print(getStatus(
                    identity));

                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    getLogger().error("Thread interrupted", e);
                    break;
                }
            }
        } else {
            System.out.println(getStatus(identity));
        }
    }

    private String getStatus(final Identity pCredentials)
        throws IOException, URISyntaxException
    {
        final List<List<String>> rows = new ArrayList<>();

        rows.add(Arrays.asList(
            "ReplicaSet",
            "Host",
            "Conn",
            "Data",
            "Disk Used",
            "Disk Total",
            "Disk Utl"
        ));

        final JSONObject processResults = getProcesses(pCredentials);
        final JSONArray processes;
        if (!processResults.has("results")) {
            return toTextTable(rows);
        } else {
            processes = processResults.getJSONArray("results");
        }

        String previousReplicaSet = null;
        for (int i = 0; i < processes.length(); i++) {
            final JSONObject process = processes.getJSONObject(i);
            final String pid = process.getString("id");

            final JSONObject processDetails =
                getProcessDetails(pCredentials, pid);

            final String replicaSetName = process.getString("replicaSetName");

            final Map<String, Object> measurementsMap =
                mapMetrics(processDetails.getJSONArray("measurements"));

            final String name = pid.indexOf(".") > 0 ? pid.substring(0, pid.indexOf(".")) : pid;
            final Integer connections = (Integer) measurementsMap.get("CONNECTIONS");
            final Long dbDataStorage =
                Long.parseLong((String) measurementsMap.getOrDefault("DB_DATA_SIZE_TOTAL", 0).toString());

            final JSONArray availableDisks =
                getAvailableDisks(pCredentials, pid).getJSONArray("results");
            long diskSpaceUsed = 0;
            long diskSpaceFree = 0;

            for (int j = 0; j < availableDisks.length(); j++) {
                final JSONObject diskDetails = getDiskDetails(
                    pCredentials,
                    pid,
                    availableDisks.getJSONObject(j).getString("partitionName"));
                final Map<String, Object> diskMeasurementsMap =
                    mapMetrics(diskDetails.getJSONArray("measurements"));
                diskSpaceUsed += new Long(diskMeasurementsMap.getOrDefault("DISK_PARTITION_SPACE_USED", 0).toString());
                diskSpaceFree += new Long(diskMeasurementsMap.getOrDefault("DISK_PARTITION_SPACE_FREE", 0).toString());
            }

            if (previousReplicaSet != null && !previousReplicaSet.equals(replicaSetName)) {
                rows.add(Arrays.asList("", "", "", "", "", "", ""));
            }

            previousReplicaSet = replicaSetName;

            final long totalDiskSpace = diskSpaceUsed + diskSpaceFree;

            rows.add(Arrays.asList(
                replicaSetName,
                name,
                connections != null ? connections.toString() : "",
                formatSize(dbDataStorage != null ? new Long(dbDataStorage) : 0l),
                formatSize(diskSpaceUsed),
                formatSize(totalDiskSpace),
                (totalDiskSpace > 0 ? (Math.round(((double) diskSpaceUsed / (double) totalDiskSpace) * 100)) : 0) + "%"
            ));
        }

        return toTextTable(rows);
    }

    private String toTextTable(final List<List<String>> pRows) {
        final Map<Integer, Integer> columnSizeMap = new HashMap<>();
        pRows.forEach(r -> {
            IntStream.range(0, r.size()).forEach(i -> {
                final int currentMaxSize = columnSizeMap.getOrDefault(i, 0);
                final int colSize = r.get(i).length();
                if (colSize > currentMaxSize) {
                    columnSizeMap.put(i, colSize);
                }
            });
        });

        final StringBuilder lines = new StringBuilder();
        pRows.forEach(r -> {
            IntStream.range(0, r.size()).forEach(i -> {
                lines.append(String.format("%1$-" + (columnSizeMap.get(i) + 2) + "s", r.get(i)) +
                    (i < r.size() - 1 ? "\t" : ""));
            });
            lines.append("\n");
        });
        return lines.toString();
    }

    private Map<String, Object> mapMetrics(final JSONArray pMeasurements) {
        final Map<String, Object> measurementsMap = new HashMap<>();

        for (int i = 0; i < pMeasurements.length(); i++) {
            final JSONObject m = pMeasurements.getJSONObject(i);
            final JSONArray dataPoints = m.has("dataPoints") ? m.getJSONArray("dataPoints") : null;

            if (dataPoints == null || dataPoints.length() == 0) {
                continue;
            }

            for (int j = dataPoints.length() - 1; j >= 0; j--) {
                final JSONObject dataPoint = dataPoints.getJSONObject(j);
                if (!dataPoint.isNull("value")) {
                    measurementsMap.put(m.getString("name"), dataPoint.get("value"));
                    break;
                }
            }
        }

        return measurementsMap;
    }

    private JSONObject getProcessDetails(
        final Identity pCredentials,
        final String pHostPort) throws IOException, URISyntaxException
    {

        final URIBuilder uriBuilder = new URIBuilder(String.format(
            "%s/%s/processes/%s/measurements",
            BASE_URL,
            pCredentials.getGroupId(),
            pHostPort));

        uriBuilder.addParameter("granularity", "PT1M")
            .addParameter("start", toIsoDate(getStart()))
            .addParameter("end", toIsoDate(getEnd()))
            .addParameter("m", "CONNECTIONS")
            .addParameter("m", "DB_STORAGE_TOTAL")
            .addParameter("m", "DB_DATA_SIZE_TOTAL")
            .addParameter("m", "PROCESS_NORMALIZED_CPU_USER")
            .addParameter("m", "PROCESS_NORMALIZED_CPU_KERNEL")
            .addParameter("m", "CURSORS_TOTAL_OPEN");

        return getResults(pCredentials, uriBuilder.build());
    }

    private Date getStart() {
        final Calendar start = Calendar.getInstance();
        start.add(Calendar.MINUTE, -1 * LOOKBACK_MINUTES);

        return start.getTime();
    }

    private Date getEnd() {
        return new Date();
    }

    private JSONObject getAvailableDisks(
        final Identity pCredentials,
        final String pHostPort) throws IOException, URISyntaxException
    {
        URIBuilder uriBuilder = new URIBuilder(String.format(
            "%s/%s/processes/%s/disks",
            BASE_URL,
            pCredentials.getGroupId(),
            pHostPort));

        return getResults(pCredentials, uriBuilder.build());
    }

    private JSONObject getDiskDetails(
        final Identity pCredentials,
        final String pHostPort,
        final String pDiskId) throws IOException, URISyntaxException
    {
        final URIBuilder uriBuilder = new URIBuilder(String.format(
            "%s/%s/processes/%s/disks/%s/measurements",
            BASE_URL,
            pCredentials.getGroupId(),
            pHostPort,
            pDiskId));

        uriBuilder.addParameter("granularity", "PT1M")
            .addParameter("start", toIsoDate(getStart()))
            .addParameter("end", toIsoDate(getEnd()))
            .addParameter("m", "DISK_PARTITION_SPACE_FREE")
            .addParameter("m", "DISK_PARTITION_SPACE_USED");

        return getResults(pCredentials, uriBuilder.build());
    }

    private String toIsoDate(final Date pDate) {
        return pDate.toInstant().toString();
    }

    private JSONObject getProcesses(final Identity pCredentials) throws
        IOException, URISyntaxException
    {
        return getResults(pCredentials, new URI(String.format(
            "%s/%s/processes",
            BASE_URL,
            pCredentials.getGroupId())));
    }

    public String formatSize(final long sizeBytes) {
        final double k = Math.floor(sizeBytes / 1000d);
        final double m = Math.floor(sizeBytes / (1000d * 1000d));
        final double g = Math.floor(sizeBytes / (1000d * 1000d * 1000d));
        final double t = Math.floor(sizeBytes / (1000d * 1000d * 1000d * 1000d));

        final DecimalFormat dec = new DecimalFormat("0");

        if (t >= 1) {
            return dec.format(t).concat("T");
        } else if (g >= 1) {
            return dec.format(g).concat("G");
        } else if (m >= 1) {
            return dec.format(m).concat("M");
        } else if (k >= 1) {
            return dec.format(k).concat("K");
        } else {
            return dec.format(sizeBytes).concat("B");
        }
    }
}
