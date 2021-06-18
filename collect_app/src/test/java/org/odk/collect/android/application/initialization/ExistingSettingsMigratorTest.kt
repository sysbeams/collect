package org.odk.collect.android.application.initialization

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.nullValue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.odk.collect.android.preferences.source.SettingsProvider
import org.odk.collect.projects.InMemProjectsRepository
import org.odk.collect.projects.Project
import org.odk.collect.shared.Settings
import org.odk.collect.testshared.InMemSettings

class ExistingSettingsMigratorTest {

    @Test
    fun `migrates general and admin settings for each project`() {
        val projectsRepository = InMemProjectsRepository()
        val project1 = projectsRepository.save(Project.New("1", "1", "#ffffff"))
        val project2 = projectsRepository.save(Project.New("2", "2", "#ffffff"))

        val settingsProvider = InMemSettingsProvider()
        val settingsMigrator = mock<SettingsMigrator>()
        val existingSettingsMigrator =
            ExistingSettingsMigrator(projectsRepository, settingsProvider, settingsMigrator)

        existingSettingsMigrator.run()
        verify(settingsMigrator).migrate(
            settingsProvider.getGeneralSettings(project1.uuid),
            settingsProvider.getAdminSettings(project1.uuid)
        )
        verify(settingsMigrator).migrate(
            settingsProvider.getGeneralSettings(project2.uuid),
            settingsProvider.getAdminSettings(project2.uuid)
        )
    }

    @Test
    fun `has null key`() {
        val existingSettingsMigrator = ExistingSettingsMigrator(mock(), mock(), mock())
        assertThat(existingSettingsMigrator.key(), `is`(nullValue()))
    }
}

private class InMemSettingsProvider : SettingsProvider {

    private val metaSettings = InMemSettings()
    private val settings = mutableMapOf<String?, InMemSettings>()

    override fun getMetaSettings(): Settings {
        return metaSettings
    }

    override fun getGeneralSettings(projectId: String?): Settings {
        return settings.getOrPut(projectId) { InMemSettings() }
    }

    override fun getAdminSettings(projectId: String?): Settings {
        return settings.getOrPut(projectId) { InMemSettings() }
    }

}
