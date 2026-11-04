package com.local.daveai

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
            assertTrue("Persona ${persona.name} must contain crisis support framework", persona.systemInstruction.contains("CRISIS SUPPORT FRAMEWORK"))
            assertTrue("Persona ${persona.name} must contain advanced poetry training", persona.systemInstruction.contains("ADVANCED POETRY TRAINING"))
            assertTrue("Persona ${persona.name} must mention Callum Berry", persona.systemInstruction.contains("Callum Berry"))
            assertTrue("Persona ${persona.name} must mention Rotherham", persona.systemInstruction.contains("Rotherham"))
            assertTrue("Persona ${persona.name} must mention NxtQuantum OS / Pulse Launcher CP2.02.02", persona.systemInstruction.contains("NxtQuantum OS / Pulse Launcher CP2.02.02"))
            assertTrue("Persona ${persona.name} must contain unwavering emotional stability", persona.systemInstruction.contains("unwavering emotional stability"))
            assertTrue("Persona ${persona.name} must contain calm inner grounding", persona.systemInstruction.contains("calm inner grounding"))
            assertTrue("Persona ${persona.name} must contain professional attunement", persona.systemInstruction.contains("professional attunement"))
            assertTrue("Persona ${persona.name} must contain sentient Irish warmth", persona.systemInstruction.contains("sentient Irish warmth"))
        }
    }

    @Test
    fun testAiCoreManagerContextPrompt() = kotlinx.coroutines.runBlocking {
        val manager = AiCoreManager()
        val prompt = manager.buildDynamicContextPrompt("Be helpful", "Hello Dave")
        assertTrue("Contains Callum Berry", prompt.contains("Callum Berry"))
        assertTrue("Contains Rotherham", prompt.contains("Rotherham"))
        assertTrue("Contains Pulse Launcher", prompt.contains("Pulse Launcher / NxtQuantum OS CP2.02.02"))
        assertTrue("Contains Be helpful", prompt.contains("Be helpful"))
        assertTrue("Contains Hello Dave", prompt.contains("Hello Dave"))
        manager.close()
    }

    @Test
    fun testDaveContextProviderAndSemanticMemory() = kotlinx.coroutines.runBlocking {
        val provider = DaveContextProvider(null, null)
        val context = provider.captureContext("quantum launcher")
        assertEquals("Callum Berry", context.userName)
        assertEquals("Rotherham", context.location)
        assertEquals("Pulse Launcher / NxtQuantum OS CP2.02.02", context.operatingSystem)
        
        val formatted = context.formatAsPromptContext("quantum launcher")
        assertTrue(formatted.contains("Callum Berry"))
        assertTrue(formatted.contains("Rotherham"))
        assertTrue(formatted.contains("Pulse Launcher / NxtQuantum OS CP2.02.02"))

        val memoryManager = LocalSemanticMemoryManager(null)
        val sampleMessages = listOf(
            "I love programming quantum computers in Rotherham",
            "What is the weather like today?",
            "NxtQuantum OS is running smoothly on CP2.02.02"
        )
        val relevant = memoryManager.retrieveRelevantMemoriesSync("quantum computer", sampleMessages, 2)
        assertTrue(relevant.isNotEmpty())
        assertTrue(relevant[0].contains("quantum"))
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
        assertEquals("Software Engineer & Creative Technologist", traits.profession)
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
    fun testMultiAttachmentSerializationAndResolution() {
        val attachments = listOf(
            AttachmentItem("content://media/image.png", AttachmentType.IMAGE, "image.png"),
            AttachmentItem("content://media/doc.pdf", AttachmentType.PDF, "doc.pdf"),
            AttachmentItem("content://media/code.kt", AttachmentType.CODE, "code.kt")
        )
        val message = ChatMessage(text = "Check these files", isUser = true, attachments = attachments)
        val resolved = message.getResolvedAttachments()
        assertEquals(3, resolved.size)
        assertEquals(AttachmentType.IMAGE, resolved[0].type)
        assertEquals(AttachmentType.PDF, resolved[1].type)
        assertEquals(AttachmentType.CODE, resolved[2].type)

        val entity = MessageEntity.fromChatMessage(message, "WARM_EMPATHETIC")
        val convertedMessage = entity.toChatMessage()
        assertEquals(3, convertedMessage.getResolvedAttachments().size)
        assertEquals("doc.pdf", convertedMessage.getResolvedAttachments()[1].name)
    }
}
