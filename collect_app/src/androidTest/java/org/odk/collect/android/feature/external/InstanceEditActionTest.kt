package org.odk.collect.android.feature.external

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.BaseColumns
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.odk.collect.android.R
import org.odk.collect.android.external.InstancesContract
import org.odk.collect.android.support.CollectTestRule
import org.odk.collect.android.support.TestRuleChain
import org.odk.collect.android.support.pages.FormEntryPage
import org.odk.collect.android.support.pages.MainMenuPage
import org.odk.collect.android.support.pages.OkDialog

@RunWith(AndroidJUnit4::class)
class InstanceEditActionTest {

    private val rule = CollectTestRule()

    @get:Rule
    val chain: RuleChain = TestRuleChain.chain()
        .around(rule)

    @Test
    fun opensInstance() {
        rule.startAtMainMenu()
            .copyForm("one-question.xml")
            .startBlankForm("One Question")
            .swipeToEndScreen()
            .clickSaveAndExit()

        val instanceId = getFirstInstanceIdFromContentProvider("DEMO")
        val uri = InstancesContract.getUri("DEMO", instanceId)

        val intent = Intent(Intent.ACTION_EDIT).also { it.data = uri }
        rule.launch(intent, FormEntryPage("One Question"))
    }

    @Test
    fun whenInstanceIsNotCurrentProject_showsWarningAndExits() {
        rule.startAtMainMenu()
            .copyAndSyncForm("one-question.xml")
            .startBlankForm("One Question")
            .swipeToEndScreen()
            .clickSaveAndExit()
            .addAndSwitchToProject("https://example.com")

        val instanceId = getFirstInstanceIdFromContentProvider("DEMO")
        val uri = InstancesContract.getUri("DEMO", instanceId)

        val intent = Intent(Intent.ACTION_EDIT).also { it.data = uri }
        rule.launch(intent, OkDialog())
            .assertText(R.string.wrong_project_selected_for_form)
            .clickOK(MainMenuPage())
    }

    @Test
    fun whenUriDoesNotHaveProjectId_andCurrentProjectIsFirstOne_opensForm() {
        rule.startAtMainMenu()
            .copyAndSyncForm("one-question.xml")
            .startBlankForm("One Question")
            .swipeToEndScreen()
            .clickSaveAndExit()
            .addAndSwitchToProject("https://example.com")
            .openProjectSettings()
            .selectProject("Demo project")

        val instanceId = getFirstInstanceIdFromContentProvider("DEMO")
        val uri = InstancesContract.getUri("DEMO", instanceId)
        val uriWithoutProjectId = Uri.Builder()
            .scheme(uri.scheme)
            .authority(uri.authority)
            .path(uri.path)
            .query(null)
            .build()

        val intent = Intent(Intent.ACTION_EDIT).also { it.data = uriWithoutProjectId }
        rule.launch(intent, FormEntryPage("One Question"))
    }

    @Test
    fun whenUriDoesNotHaveProjectId_andCurrentProjectIsNotFirstOne_showsWarningAndExits() {
        rule.startAtMainMenu()
            .copyAndSyncForm("one-question.xml")
            .startBlankForm("One Question")
            .swipeToEndScreen()
            .clickSaveAndExit()
            .addAndSwitchToProject("https://example.com")

        val instanceId = getFirstInstanceIdFromContentProvider("DEMO")
        val uri = InstancesContract.getUri("DEMO", instanceId)
        val uriWithoutProjectId = Uri.Builder()
            .scheme(uri.scheme)
            .authority(uri.authority)
            .path(uri.path)
            .query(null)
            .build()

        val intent = Intent(Intent.ACTION_EDIT).also { it.data = uriWithoutProjectId }
        rule.launch(intent, OkDialog())
            .assertText(R.string.wrong_project_selected_for_form)
            .clickOK(MainMenuPage())
    }

    private fun getFirstInstanceIdFromContentProvider(projectId: String): Long {
        val contentResolver = ApplicationProvider.getApplicationContext<Application>().contentResolver
        val uri = InstancesContract.getUri(projectId)
        return contentResolver.query(uri, null, null, null, null, null).use {
            if (it != null) {
                it.moveToFirst()
                it.getLong(it.getColumnIndex(BaseColumns._ID))
            } else {
                throw RuntimeException("Null cursor!")
            }
        }
    }
}
