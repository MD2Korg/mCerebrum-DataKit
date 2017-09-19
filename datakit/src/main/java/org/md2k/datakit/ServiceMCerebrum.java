package org.md2k.datakit;

import android.content.Intent;
import android.os.Bundle;

import org.md2k.datakit.cerebralcortex.ServiceCerebralCortex;
import org.md2k.mcerebrum.commons.app_info.AppInfo;
import org.md2k.mcerebrum.commons.permission.Permission;
import org.md2k.mcerebrum.core.access.AbstractServiceMCerebrum;

public class ServiceMCerebrum extends AbstractServiceMCerebrum {
    public ServiceMCerebrum() {
    }


    @Override
    protected boolean hasClear() {
        return true;
    }

    @Override
    public void initialize(Bundle bundle) {
        Intent intent=new Intent(this, ActivityMain.class);
        intent.putExtra("PERMISSION",true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void launch(Bundle bundle) {
        Intent intent=new Intent(this, ActivityMain.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void startBackground(Bundle bundle) {

        Intent intent=new Intent(this, ServiceDataKit.class);
        startService(intent);
        intent=new Intent(this, ServiceCerebralCortex.class);
        startService(intent);

    }

    @Override
    public void stopBackground(Bundle bundle) {
        Intent intent=new Intent(this, ServiceDataKit.class);
        stopService(intent);
        intent=new Intent(this, ServiceCerebralCortex.class);
        stopService(intent);
    }

    @Override
    public void report(Bundle bundle) {
    }

    @Override
    public void clear(Bundle bundle) {
        Intent intent = new Intent(this, ActivitySettings.class);
        intent.putExtra("delete", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
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
    public void configure(Bundle bundle) {
        Intent intent = new Intent(this, ActivitySettings.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public boolean isEqualDefault() {
        return true;
    }

}
