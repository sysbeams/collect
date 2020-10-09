package org.odk.collect.android.widgets.interfaces;

import android.content.Context;
import android.os.Bundle;

import org.javarosa.core.model.FormIndex;
import org.odk.collect.android.geo.MapConfigurator;
import org.odk.collect.android.utilities.PermissionUtils;
import org.odk.collect.android.widgets.utilities.WaitingForDataRegistry;

public interface GeoWidget {

    void onButtonClicked(Context context, FormIndex index, PermissionUtils permissionUtils, MapConfigurator mapConfigurator,
                         WaitingForDataRegistry waitingForDataRegistry, Class activityClass, Bundle bundle, int requestCode);
}
