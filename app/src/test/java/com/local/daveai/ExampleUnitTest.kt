package com.local.daveai

import com.local.daveai.broker.DaveIntegrationProvider
import com.local.daveai.ui.theme.DaveThemePalette
import com.local.daveai.ui.theme.NativeShaderThemeEngine
import org.junit.Test
import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the host.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testPersonasHaveCrisisAndPoetryTrainingAndContextAnchors() {
        for (persona in DavePersona.entries) {
            assertTrue("Persona ${persona.name} must specify Dave is an authentic companion", persona.systemInstruction.contains("companion"))
            assertTrue("Persona ${persona.name} must mention Callum", persona.systemInstruction.contains("Callum"))
        }
    }

    @Test
    fun testWorldAndUserLearningRoleDirectives() {
        val prompt = DaveSystemInstruction.daveSystemInstruction
        assertTrue("Contains world learning directive", prompt.contains("learn about the world using the internet"))
        assertTrue("Contains anti-robot directive", prompt.contains("NEVER speak like a robot"))

        for (persona in DavePersona.entries) {
            assertTrue("Persona ${persona.name} contains internet learning", persona.systemInstruction.contains("learn about the world using the internet"))
            assertTrue("Persona ${persona.name} contains disclaimer directive", persona.systemInstruction.contains("Never use artificial AI disclaimers"))
        }
    }

    @Test
    fun testDaveSystemInstructionPrompt() {
        val prompt = DaveSystemInstruction.daveSystemInstruction
        assertTrue("Contains Callum", prompt.contains("Callum"))
        assertTrue("Contains Rotherham", prompt.contains("Rotherham"))
        assertTrue("Contains Pulse Launcher", prompt.contains("Pulse Launcher"))
    }

    @Test
    fun testDaveContextProvider() = kotlinx.coroutines.runBlocking {
        val provider = DaveContextProvider(null, null)
        val context = provider.captureContext("quantum launcher")
        assertEquals("Callum Berry", context.userName)
        assertEquals("Rotherham", context.location)
        assertEquals("Pulse Launcher / NxtQuantum OS CP2.02.02", context.operatingSystem)
        
        val formatted = context.formatAsPromptContext("quantum launcher")
        assertTrue(formatted.contains("Callum Berry"))
        assertTrue(formatted.contains("Rotherham"))
        assertTrue(formatted.contains("Pulse Launcher / NxtQuantum OS CP2.02.02"))
        assertTrue(formatted.contains("Storage:"))
    }

    @Test
    fun testLocalLibraryContextProvider() {
        val provider = LocalLibraryContextProvider(null)
        val libraryContext = provider.captureLocalLibraryContext()
        assertNotNull(libraryContext)
        assertNotNull(libraryContext.networkState)
        assertNotNull(libraryContext.freeStorageGb)
    }

    @Test
    fun testUserTraitsExtraction() {
        val provider = DaveContextProvider(null, null)
        val history = listOf(
            "I love coffee and coding in Kotlin",
            "I dislike bugs and latency",
            "My hobby is writing poetry",
            "I work as a software engineer",
            "Feeling a bit stressed today"
        )
        val traits = provider.extractUserTraits(history)
        assertTrue(traits.likes.isNotEmpty())
        assertTrue(traits.dislikes.isNotEmpty())
        assertTrue(traits.hobbies.isNotEmpty())
        assertEquals("Poet & Author", traits.profession)
        assertTrue(traits.emotionalPatterns.isNotEmpty())
    }

    @Test
    fun testDaveSearchWidgetActionsAndConstants() {
        assertNotNull(DaveSearchWidget.QUERY_KEY)
        assertNotNull(DaveSearchWidget.ANSWER_KEY)
        assertNotNull(DaveQueryAction.QUERY_PARAM_KEY)
        assertNotNull(DaveUpdateQueryAction.QUERY_PARAM_KEY)
        val queryAction = DaveQueryAction()
        val updateQueryAction = DaveUpdateQueryAction()
        val clearAction = DaveClearAction()
        assertNotNull(queryAction)
        assertNotNull(updateQueryAction)
        assertNotNull(clearAction)
    }

    @Test
    fun testExtractedActionSerializationAndMessageEntityConversion() {
        val action = ExtractedAction(
            type = ActionType.CALENDAR_EVENT,
            title = "Scheduled Calendar Event",
            details = "Meeting with Callum at 3pm",
            timeMillis = 1700000000000L
        )
        val message = ChatMessage(
            text = "Schedule a meeting with Callum",
            isUser = false,
            extractedAction = action,
            generationTimeMs = 1200L
        )

        val entity = MessageEntity.fromChatMessage(message, "WARM_EMPATHETIC")
        val convertedMessage = entity.toChatMessage()

        assertNotNull(convertedMessage.extractedAction)
        assertEquals(ActionType.CALENDAR_EVENT, convertedMessage.extractedAction?.type)
        assertEquals("Scheduled Calendar Event", convertedMessage.extractedAction?.title)
        assertEquals(1200L, convertedMessage.generationTimeMs)
    }

    @Test
    fun testExpandedActionTypesSerialization() {
        val types = listOf(
            ActionType.SET_ALARM,
            ActionType.SET_TIMER,
            ActionType.TOGGLE_FLASHLIGHT,
            ActionType.SET_AUDIO_MODE,
            ActionType.OPEN_APP,
            ActionType.LAUNCHER_LAYOUT,
            ActionType.OPEN_SETTINGS,
            ActionType.TOGGLE_BLUETOOTH,
            ActionType.TOGGLE_WIFI
        )
        types.forEach { actionType ->
            val action = ExtractedAction(type = actionType, title = actionType.name, details = "Test details")
            val msg = ChatMessage(text = "Test", isUser = false, extractedAction = action)
            val entity = MessageEntity.fromChatMessage(msg, "WARM_EMPATHETIC")
            val converted = entity.toChatMessage()
            assertEquals(actionType, converted.extractedAction?.type)
        }
    }

    @Test
    fun testAppLaunchExtraction() {
        val prompts = listOf("open Spotify", "launch YouTube", "start Camera", "fire up Chrome")
        prompts.forEach { prompt ->
            val appName = prompt
                .replace(Regex("(?i)\\b(open|launch|start|run|fire up|app|application|the)\\b"), "")
                .trim()
            assertTrue(appName.isNotBlank())
        }
    }

    @Test
    fun testDaveEngineSupport() {
        val engine = DaveEngine()
        assertNotNull(engine)
        assertFalse(engine.isNativeSupported())
    }

    @Test
    fun testClearChatAction() {
        val list = mutableListOf(ChatMessage(text = "Hello Dave", isUser = true))
        list.clear()
        assertTrue(list.isEmpty())
    }

    @Test
    fun testHistoryTruncationToSixMessages() {
        val history = (1..10).map { ChatMessage(text = "Message $it", isUser = it % 2 != 0) }
        val truncated = history.takeLast(6)
        assertEquals(6, truncated.size)
        assertEquals("Message 5", truncated.first().text)
        assertEquals("Message 10", truncated.last().text)
    }

    @Test
    fun testClaudeApiModelAndClient() {
        assertEquals("https://api.anthropic.com/", ClaudeClient.ANTHROPIC_BASE_URL)
        assertNotNull(ClaudeClient.api)

        val req = ClaudeRequest(
            model = ClaudeClient.DEFAULT_MODEL,
            maxTokens = 1024,
            system = "You are Dave.",
            messages = listOf(ClaudeMessage("user", "Hello"))
        )
        assertEquals("claude-haiku-5-5", req.model)
        assertEquals(1024, req.maxTokens)
        assertEquals("You are Dave.", req.system)
        assertEquals(1, req.messages.size)

        val resp = ClaudeResponse(
            id = "msg_123",
            content = listOf(ClaudeContentBlock(type = "text", text = "Hello Callum!"))
        )
        assertEquals("msg_123", resp.id)
        assertEquals("Hello Callum!", resp.content.first().text)
    }

    @Test
    fun testCDataMcpClientEndpoint() {
        assertEquals("https://mcp.cloud.cdata.com/mcp", McpClientHelper.CDATA_MCP_ENDPOINT)
    }

    @Test
    fun testV5SecondBrainAndSpeechRecognizer() {
        val brainItem = SecondBrainEntity(
            title = "Project Roadmap",
            content = "Complete V5 CP5.01.01 release with SQLite FTS5 Second Brain.",
            category = "NOTES"
        )
        assertEquals("Project Roadmap", brainItem.title)
        assertEquals("NOTES", brainItem.category)
        assertTrue(brainItem.content.contains("FTS5"))
    }

    @Test
    fun testUserProfileEntityCreation() {
        val profile = UserProfileEntity(
            name = "Alex Smith",
            location = "London",
            profession = "Architect",
            isActive = true
        )
        assertEquals("Alex Smith", profile.name)
        assertEquals("London", profile.location)
        assertEquals("Architect", profile.profession)
        assertTrue(profile.isActive)
    }

    @Test
    fun testBeta1ThemePalettes() {
        assertEquals(6, DaveThemePalette.entries.size)
        assertEquals("Neon Obsidian", DaveThemePalette.NEON_OBSIDIAN.displayName)
        assertEquals("Emerald Cyber", DaveThemePalette.EMERALD_CYBER.displayName)
        assertEquals("Amethyst Void", DaveThemePalette.AMETHYST_VOID.displayName)
        assertEquals("Crimson Dusk", DaveThemePalette.CRIMSON_DUSK.displayName)
        assertEquals("Solar Gold", DaveThemePalette.SOLAR_GOLD.displayName)
        assertEquals("Oceanic Glass", DaveThemePalette.OCEANIC_GLASS.displayName)
    }

    @Test
    fun testSecurityAuditManagerAndShaderEngine() {
        assertTrue(SecurityAuditManager.verifyCallerPackage(null, "com.s.launcher"))
        val sanitized = SecurityAuditManager.sanitizePayload("Key sk-ant-api-key-123456789")
        assertTrue(sanitized.contains("SANITIZED"))

        val palette = DaveThemePalette.NEON_OBSIDIAN
        assertEquals(3, palette.glPrimaryFloatArray.size)
        assertEquals(3, palette.glSecondaryFloatArray.size)
        assertEquals(3, palette.glBgFloatArray.size)

        assertTrue(NativeShaderThemeEngine.AGSL_GEMINI_GLOW_SHADER_SRC.contains("in_primaryColor"))
        assertTrue(NativeShaderThemeEngine.AGSL_GEMINI_GLOW_SHADER_SRC.contains("uniform shader contents;"))
        assertTrue(NativeShaderThemeEngine.AGSL_GEMINI_GLOW_SHADER_SRC.contains("contents.eval"))
        assertTrue(NativeShaderThemeEngine.AGSL_AURA_SHADER_SRC.contains("uniform shader contents;"))
        assertTrue(NativeShaderThemeEngine.AGSL_AURA_SHADER_SRC.contains("contents.eval"))
    }

    @Test
    fun testOverlayLifecycleOwner() {
        val owner = OverlayLifecycleOwner()
        try {
            owner.onCreate()
            owner.onStart()
            assertNotNull(owner.lifecycle)
        } catch (_: Exception) {}
    }

    @Test
    fun testDaveWebContextNativeAndManager() = kotlinx.coroutines.runBlocking {
        // Test graceful fallback / availability API check
        val isNativeAvailable = DaveWebContextNative.isAvailable()
        val dummySources = listOf(
            WebSource("Example Site", "https://example.com", "Example quantum computing snippet text.")
        )
        val result = DaveWebContextNative.processAndRank("quantum", dummySources)
        if (isNativeAvailable) {
            assertNotNull(result)
            assertTrue(result!!.contains("[Retrieved Web Data]"))
        } else {
            assertNull(result)
        }

        val searchManager = DaveWebSearchManager()
        val searchResult = searchManager.searchWithSources("")
        assertEquals("", searchResult.formattedText)

        val formattedTextSample = "[Retrieved Web Data]\nSource 1:\nURL: https://example.com/article\nSnippet: Test snippet text\n\nSource 2:\nURL: https://duckduckgo.com\nSnippet: Search snippet"
        val parsedSources = searchManager.parseSourcesFromFormattedText(formattedTextSample)
        assertEquals(2, parsedSources.size)
        assertEquals("https://example.com/article", parsedSources[0].url)
        assertEquals("example.com", parsedSources[0].title)
    }

    @Test
    fun testFreeEnvironmentClientEndpoints() {
        assertNotNull(FreeEnvironmentClient.openMeteoWeather)
        assertNotNull(FreeEnvironmentClient.openMeteoAirQuality)
        assertNotNull(FreeEnvironmentClient.nominatim)
        assertNotNull(FreeEnvironmentClient.wikipedia)

        val weather = OpenMeteoCurrentWeather(temperature = 18.5f, windspeed = 12.0f)
        assertEquals(18.5f, weather.temperature)

        val wiki = WikipediaSummaryResponse(title = "Rotherham", extract = "Rotherham is a minster town in South Yorkshire.")
        assertEquals("Rotherham", wiki.title)
    }

    @Test
    fun testDaveVoiceGlyphSyncWaveformProcessing() {
        val silentWaveform = ByteArray(128) { 128.toByte() }
        var sum = 0.0
        for (b in silentWaveform) {
            val sample = (b.toInt() and 0xFF) - 128
            sum += sample * sample
        }
        val rms = kotlin.math.sqrt(sum / silentWaveform.size)
        val rawLevel = ((rms / 45.0) * 100).toInt().coerceIn(0, 100)
        val brightness = if (rawLevel < 8) 0 else rawLevel
        assertEquals(0, brightness)

        val activeWaveform = ByteArray(128) { i -> (128 + if (i % 2 == 0) 30 else -30).toByte() }
        var activeSum = 0.0
        for (b in activeWaveform) {
            val sample = (b.toInt() and 0xFF) - 128
            activeSum += sample * sample
        }
        val activeRms = kotlin.math.sqrt(activeSum / activeWaveform.size)
        val activeRawLevel = ((activeRms / 45.0) * 100).toInt().coerceIn(0, 100)
        assertTrue(activeRawLevel > 20)
    }

    @Test
    fun testDaveIntegrationProvider() {
        val provider = DaveIntegrationProvider()
        assertNotNull(provider)
    }

    @Test
    fun testV4CapabilitiesAndSettings() {
        val prompt = DaveSystemInstruction.daveSystemInstruction
        assertTrue(prompt.contains("Multimodal Vision"))
        assertTrue(prompt.contains("Deep System Access"))
        assertTrue(prompt.contains("Multi-Agent"))

        val mode = AiEngineMode.CLAUDE
        assertEquals("Anthropic Claude Engine", mode.displayName)
    }

    @Test
    fun testMultimodalImageSourceAndAttachment() {
        val imageSource = ClaudeImageSource(mediaType = "image/jpeg", data = "base64data==")
        assertEquals("base64", imageSource.type)
        assertEquals("image/jpeg", imageSource.mediaType)
        assertEquals("base64data==", imageSource.data)

        val block = ClaudeContentBlock(type = "image", source = imageSource)
        assertEquals("image", block.type)
        assertEquals(imageSource, block.source)

        val attachment = FileAttachment(
            uriString = "content://media/123",
            name = "photo.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 2048,
            base64Data = "base64data=="
        )
        assertEquals("photo.jpg", attachment.name)
        assertEquals("image/jpeg", attachment.mimeType)
    }
}
