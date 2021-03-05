/*
 * Copyright 2017 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.odk.collect.android.database;

import android.database.Cursor;
import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.dao.InstancesDao;
import org.odk.collect.android.instances.Instance;
import org.odk.collect.android.provider.InstanceProviderAPI.InstanceColumns;
import org.odk.collect.android.storage.StoragePathProvider;
import org.odk.collect.android.storage.StorageSubdirectory;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class InstancesDaoTest {

    private InstancesDao instancesDao;
    private final StoragePathProvider storagePathProvider = new StoragePathProvider();

    // sample instances
    private Instance hypertensionScreeningInstance;
    private Instance cascadingSelectInstance;
    private Instance biggestNOfSetInstance;
    private Instance widgetsInstance;
    private Instance sampleInstance;
    private Instance biggestNOfSet2Instance;

    @Before
    public void setUp() {
        instancesDao = new InstancesDao();
        fillDatabase();
    }

    @Test
    public void getUnsentInstancesCursorTest() {
        Cursor cursor = instancesDao.getUnsentInstancesCursor();
        List<Instance> instances = instancesDao.getInstancesFromCursor(cursor);

        assertEquals(4, instances.size());
        assertEquals(cascadingSelectInstance, instances.get(0));
        assertEquals(hypertensionScreeningInstance, instances.get(1));
        assertEquals(sampleInstance, instances.get(2));
        assertEquals(biggestNOfSet2Instance, instances.get(3));
    }

    @Test
    public void getSentInstancesCursorTest() {
        Cursor cursor = instancesDao.getSentInstancesCursor();
        List<Instance> instances = instancesDao.getInstancesFromCursor(cursor);

        assertEquals(2, instances.size());
        assertEquals(biggestNOfSetInstance, instances.get(0));
        assertEquals(widgetsInstance, instances.get(1));
    }

    @Test
    public void getSavedInstancesCursorTest() {
        Cursor cursor = instancesDao.getSavedInstancesCursor(InstanceColumns.DISPLAY_NAME + " ASC");
        List<Instance> instances = instancesDao.getInstancesFromCursor(cursor);

        assertEquals(5, instances.size());
        assertEquals(biggestNOfSetInstance, instances.get(0));
        assertEquals(biggestNOfSet2Instance, instances.get(1));
        assertEquals(cascadingSelectInstance, instances.get(2));
        assertEquals(hypertensionScreeningInstance, instances.get(3));
        assertEquals(sampleInstance, instances.get(4));
    }

    @Test
    public void getInstancesCursorForFilePathTest() {
        String instancePath = storagePathProvider.getOdkDirPath(StorageSubdirectory.INSTANCES) + "/Hypertension Screening_2017-02-20_14-03-53/Hypertension Screening_2017-02-20_14-03-53.xml";
        Cursor cursor = instancesDao.getInstancesCursorForFilePath(instancePath);
        List<Instance> instances = instancesDao.getInstancesFromCursor(cursor);

        assertEquals(1, instances.size());
        assertEquals(hypertensionScreeningInstance, instances.get(0));
    }

    @Test
    public void getAllCompletedUndeletedInstancesCursorTest() {
        Cursor cursor = instancesDao.getAllCompletedUndeletedInstancesCursor();
        List<Instance> instances = instancesDao.getInstancesFromCursor(cursor);

        assertEquals(2, instances.size());
        assertEquals(biggestNOfSetInstance, instances.get(0));
        assertEquals(biggestNOfSet2Instance, instances.get(1));
    }

    @Test
    public void getInstancesCursorForIdTest() {
        Cursor cursor = instancesDao.getInstancesCursorForId("2");
        List<Instance> instances = instancesDao.getInstancesFromCursor(cursor);

        assertEquals(1, instances.size());
        assertEquals(cascadingSelectInstance, instances.get(0));
    }

    @Test
    public void updateInstanceTest() {
        String filePath = storagePathProvider.getOdkDirPath(StorageSubdirectory.INSTANCES) + "/Biggest N of Set_2017-02-20_14-24-46/Biggest N of Set_2017-02-20_14-24-46.xml";
        Cursor cursor = instancesDao.getInstancesCursorForFilePath(filePath);
        List<Instance> instances = instancesDao.getInstancesFromCursor(cursor);

        assertEquals(1, instances.size());
        assertEquals(biggestNOfSet2Instance, instances.get(0));

        biggestNOfSetInstance = new Instance.Builder()
                .displayName("Biggest N of Set")
                .instanceFilePath(storagePathProvider.getRelativeInstancePath(filePath))
                .jrFormId("N_Biggest")
                .status(Instance.STATUS_SUBMITTED)
                .lastStatusChangeDate(1487597090653L)
                .build();

        String where = InstanceColumns.INSTANCE_FILE_PATH + "=?";
        String[] whereArgs = {storagePathProvider.getRelativeInstancePath(filePath)};

        assertEquals(instancesDao.updateInstance(instancesDao.getValuesFromInstanceObject(biggestNOfSet2Instance), where, whereArgs), 1);

        cursor = instancesDao.getInstancesCursorForFilePath(filePath);

        instances = instancesDao.getInstancesFromCursor(cursor);

        assertEquals(1, instances.size());
        assertEquals(biggestNOfSet2Instance, instances.get(0));
    }

    @Test public void deletingSentInstance_keepsItsDatabaseRow_butClearsItsGeometryFields() {
        Instance formWithGeopointInstance = new Instance.Builder()
                .jrFormId("fake")
                .displayName("Form with geopoint")
                .instanceFilePath(new StoragePathProvider().getRelativeInstancePath("/my/fake/path"))
                .status(Instance.STATUS_SUBMITTED)
                .lastStatusChangeDate(1487595836793L)
                .geometryType("Point")
                .geometry("{\"type\":\"Point\",\"coordinates\":[127.6, 11.1]}")
                .build();
        Uri result = instancesDao.saveInstance(instancesDao.getValuesFromInstanceObject(formWithGeopointInstance));

        Collect.getInstance().getContentResolver().delete(result, null, null);

        Cursor cursor = instancesDao.getInstancesCursorForFilePath("/my/fake/path");
        formWithGeopointInstance = instancesDao.getInstancesFromCursor(cursor).get(0);

        assertThat(formWithGeopointInstance.getGeometryType(), is(nullValue()));
        assertThat(formWithGeopointInstance.getGeometry(), is(nullValue()));
    }

    private void fillDatabase() {
        String hypertensionScreeningPath = storagePathProvider.getOdkDirPath(StorageSubdirectory.INSTANCES) + "/Hypertension Screening_2017-02-20_14-03-53/Hypertension Screening_2017-02-20_14-03-53.xml";
        hypertensionScreeningInstance = new Instance.Builder()
                .displayName("Hypertension Screening")
                .instanceFilePath(storagePathProvider.getRelativeInstancePath(hypertensionScreeningPath))
                .jrFormId("hypertension")
                .status(Instance.STATUS_INCOMPLETE)
                .lastStatusChangeDate(1487595836793L)
                .build();
        instancesDao.saveInstance(instancesDao.getValuesFromInstanceObject(hypertensionScreeningInstance));

        String cascadingSelectInstancePath = storagePathProvider.getOdkDirPath(StorageSubdirectory.INSTANCES) + "/Cascading Select Form_2017-02-20_14-06-44/Cascading Select Form_2017-02-20_14-06-44.xml";
        cascadingSelectInstance = new Instance.Builder()
                .displayName("Cascading Select Form")
                .instanceFilePath(storagePathProvider.getRelativeInstancePath(cascadingSelectInstancePath))
                .jrFormId("CascadingSelect")
                .status(Instance.STATUS_INCOMPLETE)
                .lastStatusChangeDate(1487596015000L)
                .build();
        instancesDao.saveInstance(instancesDao.getValuesFromInstanceObject(cascadingSelectInstance));

        String biggestN1Path = storagePathProvider.getRelativeInstancePath(storagePathProvider.getOdkDirPath(StorageSubdirectory.INSTANCES) + "/Biggest N of Set_2017-02-20_14-06-51/Biggest N of Set_2017-02-20_14-06-51.xml");
        biggestNOfSetInstance = new Instance.Builder()
                .displayName("Biggest N of Set")
                .instanceFilePath(storagePathProvider.getRelativeInstancePath(biggestN1Path))
                .jrFormId("N_Biggest")
                .status(Instance.STATUS_SUBMITTED)
                .lastStatusChangeDate(1487596015100L)
                .build();
        instancesDao.saveInstance(instancesDao.getValuesFromInstanceObject(biggestNOfSetInstance));

        String widgetPath = storagePathProvider.getOdkDirPath(StorageSubdirectory.INSTANCES) + "/Widgets_2017-02-20_14-06-58/Widgets_2017-02-20_14-06-58.xml";
        widgetsInstance = new Instance.Builder()
                .displayName("Widgets")
                .instanceFilePath(storagePathProvider.getRelativeInstancePath(widgetPath))
                .jrFormId("widgets")
                .status(Instance.STATUS_SUBMITTED)
                .lastStatusChangeDate(1487596020803L)
                .deletedDate(1487596020803L)
                .build();
        instancesDao.saveInstance(instancesDao.getValuesFromInstanceObject(widgetsInstance));

        String samplePath = storagePathProvider.getOdkDirPath(StorageSubdirectory.INSTANCES) + "/sample_2017-02-20_14-07-03/sample_2017-02-20_14-07-03.xml";
        sampleInstance = new Instance.Builder()
                .displayName("sample")
                .instanceFilePath(storagePathProvider.getRelativeInstancePath(samplePath))
                .jrFormId("sample")
                .status(Instance.STATUS_INCOMPLETE)
                .lastStatusChangeDate(1487596026373L)
                .build();
        instancesDao.saveInstance(instancesDao.getValuesFromInstanceObject(sampleInstance));

        String biggestN2Path = storagePathProvider.getOdkDirPath(StorageSubdirectory.INSTANCES) + "/Biggest N of Set_2017-02-20_14-24-46/Biggest N of Set_2017-02-20_14-24-46.xml";
        biggestNOfSet2Instance = new Instance.Builder()
                .displayName("Biggest N of Set")
                .instanceFilePath(storagePathProvider.getRelativeInstancePath(biggestN2Path))
                .jrFormId("N_Biggest")
                .status(Instance.STATUS_COMPLETE)
                .lastStatusChangeDate(1487597090653L)
                .build();
        instancesDao.saveInstance(instancesDao.getValuesFromInstanceObject(biggestNOfSet2Instance));
    }
}
