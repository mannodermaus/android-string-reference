package com.likethesalad.placeholder.tasks.actions

import com.google.common.truth.Truth
import com.likethesalad.placeholder.data.resources.ResourcesHandler
import com.likethesalad.placeholder.data.storage.FilesProvider
import com.likethesalad.placeholder.data.storage.IncrementalDataCleaner
import com.likethesalad.placeholder.models.PathIdentity
import com.likethesalad.placeholder.models.StringResourceModel
import com.likethesalad.placeholder.models.StringsGatheredModel
import com.likethesalad.placeholder.models.StringsTemplatesModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import java.io.File

class GatherTemplatesActionTest {

    private lateinit var filesProvider: FilesProvider
    private lateinit var resourcesHandler: ResourcesHandler
    private lateinit var incrementalDataCleaner: IncrementalDataCleaner
    private lateinit var gatherTemplatesAction: GatherTemplatesAction

    @Before
    fun setUp() {
        filesProvider = mockk()
        resourcesHandler = mockk(relaxUnitFun = true)
        incrementalDataCleaner = mockk(relaxUnitFun = true)
        gatherTemplatesAction = GatherTemplatesAction(
            filesProvider, resourcesHandler,
            incrementalDataCleaner
        )
    }

    @Test
    fun check_getStringFiles() {
        // Given:
        val files = listOf<File>(mockk())
        every { filesProvider.getAllGatheredStringsFiles() } returns files

        // When:
        val result = gatherTemplatesAction.getStringFiles()

        // Then:
        verify { filesProvider.getAllGatheredStringsFiles() }
        Truth.assertThat(result).isEqualTo(files)
    }

    @Test
    fun check_getTemplatesFiles() {
        // Given:
        val files = listOf<File>(mockk())
        every { filesProvider.getAllExpectedTemplatesFiles() } returns files

        // When:
        val result = gatherTemplatesAction.getTemplatesFiles()

        // Then:
        verify { filesProvider.getAllExpectedTemplatesFiles() }
        Truth.assertThat(result).isEqualTo(files)
    }

    @Test
    fun check_gatherTemplateStrings_single_file() {
//        // Given:
        val gatheredStringsFile = mockk<File>()
        val gatheredStringsFileEs = mockk<File>()
        val (gatheredStrings, expectedGatheredTemplates) = getRawAndTemplatesPair(
            "client",
            "values", ""
        )
        val (gatheredStringsEs, expectedGatheredTemplatesEs) = getRawAndTemplatesPair(
            "main",
            "values-es", "-es"
        )

        every { resourcesHandler.getGatheredStringsFromFile(gatheredStringsFile) }.returns(gatheredStrings)
        every { resourcesHandler.getGatheredStringsFromFile(gatheredStringsFileEs) }.returns(gatheredStringsEs)
        every { filesProvider.getAllGatheredStringsFiles() } returns listOf(
            gatheredStringsFile,
            gatheredStringsFileEs
        )

        // When:
        gatherTemplatesAction.gatherTemplateStrings()

        // Then:
        verify { incrementalDataCleaner.clearTemplateStrings() }
        verify { resourcesHandler.saveTemplates(expectedGatheredTemplates) }
        verify { resourcesHandler.saveTemplates(expectedGatheredTemplatesEs) }
    }

    private fun getRawAndTemplatesPair(
        variantName: String,
        valuesFolderName: String,
        suffix: String
    ): Pair<StringsGatheredModel, StringsTemplatesModel> {
        val pathIdentity = PathIdentity(variantName, valuesFolderName, suffix)
        val gatheredStrings = StringsGatheredModel(
            pathIdentity,
            listOf(
                StringResourceModel("app_name", "TesT"),
                StringResourceModel("other_string", "Random string"),
                StringResourceModel("template_welcome", "The welcome message for \${app_name}"),
                StringResourceModel(
                    mapOf(
                        "name" to "template_message_non_translatable",
                        "translatable" to "false"
                    ), "Non translatable \${app_name}"
                ),
                StringResourceModel(
                    "template_this_contains_template",
                    "This is the welcome: \${template_welcome}"
                )
            )
        )
        val expectedGatheredTemplates = StringsTemplatesModel(
            pathIdentity,
            listOf(
                StringResourceModel("template_welcome", "The welcome message for \${app_name}"),
                StringResourceModel(
                    mapOf(
                        "name" to "template_message_non_translatable",
                        "translatable" to "false"
                    ), "Non translatable \${app_name}"
                ),
                StringResourceModel(
                    "template_this_contains_template",
                    "This is the welcome: \${template_welcome}"
                )
            ), mapOf(
                "app_name" to "TesT",
                "template_welcome" to "The welcome message for \${app_name}"
            )
        )
        return Pair(gatheredStrings, expectedGatheredTemplates)
    }
}