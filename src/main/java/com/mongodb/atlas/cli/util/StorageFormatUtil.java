package com.mongodb.atlas.cli.util;

import java.text.DecimalFormat;


public class StorageFormatUtil {

    public static String formatSize(long size) {
        String hrSize = "";
        double m = size / 1024.0;
        double g = size / 1048576.0;
        double t = size / 1073741824.0;

        final DecimalFormat dec = new DecimalFormat("0.00");

        if (t > 1) {
            hrSize = dec.format(t).concat("T");
        } else if (g > 1) {
            hrSize = dec.format(g).concat("G");
        } else if (m > 1) {
            hrSize = dec.format(m).concat("M");
        } else {
            hrSize = dec.format(size).concat("K");
        }
        return hrSize;
    }
}
