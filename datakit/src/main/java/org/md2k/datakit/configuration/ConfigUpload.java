package org.md2k.datakit.configuration;

import org.md2k.datakitapi.source.datasource.DataSource;

import java.util.ArrayList;

/**
 * Created by monowar on 5/6/16.
 */
public class ConfigUpload {
    public boolean enabled;
    public String url;
    public long interval;
    public String network_high_frequency;
    public String network_low_frequency;

    public ArrayList<DataSource> restricted_datasource = new ArrayList<>();
}
