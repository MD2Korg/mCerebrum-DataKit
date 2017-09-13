package org.md2k.datakit;

import android.content.Intent;

import org.md2k.datakit.cerebralcortex.ServiceCerebralCortex;
import org.md2k.mcerebrum.commons.app_info.AppInfo;
import org.md2k.mcerebrum.core.access.AbstractServiceMCerebrum;

public class ServiceMCerebrum extends AbstractServiceMCerebrum {
    public ServiceMCerebrum() {
    }


    @Override
    protected boolean hasClear() {
        return false;
    }

    @Override
    public void initialize() {
        Intent intent=new Intent(this, ActivityMain.class);
        intent.putExtra("PERMISSION",true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void launch() {
        Intent intent=new Intent(this, ActivityMain.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void startBackground() {
        Intent intent=new Intent(this, ActivityMain.class);
        intent.putExtra("RUN",true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void stopBackground() {
        Intent intent=new Intent(this, ServiceDataKit.class);
        stopService(intent);
        intent=new Intent(this, ServiceCerebralCortex.class);
        stopService(intent);
    }

    @Override
    public void report() {
    }

    @Override
    public void clear() {

    }

    @Override
    public boolean hasReport() {
        return true;
    }

    @Override
    public boolean isRunInBackground() {
        return true;
    }

    @Override
    public long getRunningTime() {
        return AppInfo.serviceRunningTime(this, ServiceDataKit.class.getName());
    }

    @Override
    public boolean isRunning() {
        return AppInfo.isServiceRunning(this, ServiceDataKit.class.getName());
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public boolean hasInitialize() {
        return true;
    }

    @Override
    public void configure() {
        Intent intent = new Intent(this, ActivitySettings.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public boolean isEqualDefault() {
        return true;
    }

}
