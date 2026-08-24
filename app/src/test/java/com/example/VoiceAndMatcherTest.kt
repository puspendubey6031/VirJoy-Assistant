package com.example

import com.example.manager.BengaliHindiEnglishMatcher
import com.example.manager.LanguageManager
import com.example.manager.ParsedVoiceCommand
import com.example.manager.VoiceCommandParser
import com.example.model.Contact
import com.example.model.SupportedLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceAndMatcherTest {

    // ==========================================
    // 1. Language Detection & Response Tests
    // ==========================================

    @Test
    fun testLanguageDetection_Bengali() {
        val lang1 = LanguageManager.detectLanguage("রাহুলকে ফোন করো")
        assertEquals(SupportedLanguage.BENGALI, lang1)

        val lang2 = LanguageManager.detectLanguage("বিনোদকে কল দাও")
        assertEquals(SupportedLanguage.BENGALI, lang2)

        val msg = LanguageManager.getNoMatchMessage("রাহুল", SupportedLanguage.BENGALI)
        assertEquals("আপনার ফোনের Contacts-এ রাহুল নামে কোনো নম্বর পাওয়া যায়নি।", msg)

        val callMsg = LanguageManager.getCallingMessage("রাহুল", SupportedLanguage.BENGALI)
        assertEquals("রাহুল-কে কল করা হচ্ছে...", callMsg)
    }

    @Test
    fun testLanguageDetection_Hindi() {
        val lang1 = LanguageManager.detectLanguage("राहुल को फोन करो")
        assertEquals(SupportedLanguage.HINDI, lang1)

        val lang2 = LanguageManager.detectLanguage("विनोद को कॉल लगाओ")
        assertEquals(SupportedLanguage.HINDI, lang2)

        val msg = LanguageManager.getNoMatchMessage("राहुल", SupportedLanguage.HINDI)
        assertEquals("आपके फोन के Contacts में राहुल नाम का कोई नंबर नहीं मिला।", msg)

        val callMsg = LanguageManager.getCallingMessage("राहुल", SupportedLanguage.HINDI)
        assertEquals("राहुल को कॉल किया जा रहा है...", callMsg)
    }

    @Test
    fun testLanguageDetection_English() {
        val lang1 = LanguageManager.detectLanguage("Call Rahul")
        assertEquals(SupportedLanguage.ENGLISH, lang1)

        val lang2 = LanguageManager.detectLanguage("Please phone to Vinod")
        assertEquals(SupportedLanguage.ENGLISH, lang2)

        val msg = LanguageManager.getNoMatchMessage("Rahul", SupportedLanguage.ENGLISH)
        assertEquals("I couldn't find Rahul in your contacts.", msg)

        val callMsg = LanguageManager.getCallingMessage("Rahul", SupportedLanguage.ENGLISH)
        assertEquals("Calling Rahul...", callMsg)
    }

    @Test
    fun testLanguageDetection_OtherIndianLanguages() {
        assertEquals(SupportedLanguage.TAMIL, LanguageManager.detectLanguage("ராகுலுக்கு போன் செய்"))
        assertEquals(SupportedLanguage.TELUGU, LanguageManager.detectLanguage("రాహుల్ కి కాల్ చేయి"))
        assertEquals(SupportedLanguage.KANNADA, LanguageManager.detectLanguage("ರಾಹುಲ್ ಅವರಿಗೆ ಕರೆ ಮಾಡಿ"))
        assertEquals(SupportedLanguage.MALAYALAM, LanguageManager.detectLanguage("രാഹുലിനെ വിളിക്കൂ"))
        assertEquals(SupportedLanguage.GUJARATI, LanguageManager.detectLanguage("રાહુલને કોલ કરો"))
        assertEquals(SupportedLanguage.PUNJABI, LanguageManager.detectLanguage("ਰਾਹੁਲ ਨੂੰ ਕਾਲ ਕਰੋ"))
        assertEquals(SupportedLanguage.ODIA, LanguageManager.detectLanguage("ରାହୁଲଙ୍କୁ କଲ୍ କରନ୍ତୁ"))
        assertEquals(SupportedLanguage.URDU, LanguageManager.detectLanguage("راہل کو کال کرو"))
    }

    // ==========================================
    // 2. Voice Command Parser Tests
    // ==========================================

    @Test
    fun testVoiceCommandParser_English() {
        val cmd1 = VoiceCommandParser.parse("Call Vinod")
        assertTrue(cmd1 is ParsedVoiceCommand.CallContact)
        assertEquals("Vinod", (cmd1 as ParsedVoiceCommand.CallContact).targetName)
        assertEquals(SupportedLanguage.ENGLISH, cmd1.detectedLanguage)

        val cmd2 = VoiceCommandParser.parse("Please make a call to Ram")
        assertTrue(cmd2 is ParsedVoiceCommand.CallContact)
        assertEquals("Ram", (cmd2 as ParsedVoiceCommand.CallContact).targetName)
        assertEquals(SupportedLanguage.ENGLISH, cmd2.detectedLanguage)
    }

    @Test
    fun testVoiceCommandParser_Bengali() {
        val cmd1 = VoiceCommandParser.parse("বিনোদকে কল করো")
        assertTrue(cmd1 is ParsedVoiceCommand.CallContact)
        assertEquals("বিনোদ", (cmd1 as ParsedVoiceCommand.CallContact).targetName)
        assertEquals(SupportedLanguage.BENGALI, cmd1.detectedLanguage)

        val cmd2 = VoiceCommandParser.parse("রামকে ফোন করো")
        assertTrue(cmd2 is ParsedVoiceCommand.CallContact)
        assertEquals("রাম", (cmd2 as ParsedVoiceCommand.CallContact).targetName)
        assertEquals(SupportedLanguage.BENGALI, cmd2.detectedLanguage)

        val cmd3 = VoiceCommandParser.parse("রাহুলকে কল দাও")
        assertTrue(cmd3 is ParsedVoiceCommand.CallContact)
        assertEquals("রাহুল", (cmd3 as ParsedVoiceCommand.CallContact).targetName)
        assertEquals(SupportedLanguage.BENGALI, cmd3.detectedLanguage)

        val cmd4 = VoiceCommandParser.parse("রাহুলকে ফোন দাও")
        assertTrue(cmd4 is ParsedVoiceCommand.CallContact)
        assertEquals("রাহুল", (cmd4 as ParsedVoiceCommand.CallContact).targetName)
        assertEquals(SupportedLanguage.BENGALI, cmd4.detectedLanguage)
    }

    @Test
    fun testVoiceCommandParser_Hindi() {
        val cmd1 = VoiceCommandParser.parse("विनोद को कॉल करो")
        assertTrue(cmd1 is ParsedVoiceCommand.CallContact)
        assertEquals("विनोद", (cmd1 as ParsedVoiceCommand.CallContact).targetName)
        assertEquals(SupportedLanguage.HINDI, cmd1.detectedLanguage)

        val cmd2 = VoiceCommandParser.parse("राम को फोन लगाओ")
        assertTrue(cmd2 is ParsedVoiceCommand.CallContact)
        assertEquals("राम", (cmd2 as ParsedVoiceCommand.CallContact).targetName)
        assertEquals(SupportedLanguage.HINDI, cmd2.detectedLanguage)

        val cmd3 = VoiceCommandParser.parse("राहुल को फोन मिलाओ")
        assertTrue(cmd3 is ParsedVoiceCommand.CallContact)
        assertEquals("राहुल", (cmd3 as ParsedVoiceCommand.CallContact).targetName)
        assertEquals(SupportedLanguage.HINDI, cmd3.detectedLanguage)
    }

    @Test
    fun testVoiceCommandParser_Tamil() {
        val cmd = VoiceCommandParser.parse("ராகுலுக்கு போன் செய்")
        assertTrue(cmd is ParsedVoiceCommand.CallContact)
        assertEquals("ராகுல்", (cmd as ParsedVoiceCommand.CallContact).targetName)
        assertEquals(SupportedLanguage.TAMIL, cmd.detectedLanguage)
    }

    // ==========================================
    // 3. Cross-Script Contact Matching Tests
    // ==========================================

    @Test
    fun testBengaliCrossScriptMatching() {
        // রাহুল ↔ Rahul
        val scoreRahul = BengaliHindiEnglishMatcher.computeMatchScore("রাহুল", "Rahul")
        assertTrue("Expected score for রাহুল vs Rahul >= 0.8, got $scoreRahul", scoreRahul >= 0.80)

        // বিনোদ ↔ Binod ↔ Vinod
        val scoreVinod = BengaliHindiEnglishMatcher.computeMatchScore("বিনোদ", "Vinod")
        assertTrue("Expected score for বিনোদ vs Vinod >= 0.8, got $scoreVinod", scoreVinod >= 0.80)
        val scoreBinod = BengaliHindiEnglishMatcher.computeMatchScore("বিনোদ", "Binod")
        assertTrue("Expected score for বিনোদ vs Binod >= 0.8, got $scoreBinod", scoreBinod >= 0.80)

        // রাম ↔ Ram
        val scoreRam = BengaliHindiEnglishMatcher.computeMatchScore("রাম", "Ram")
        assertTrue("Expected score for রাম vs Ram >= 0.8, got $scoreRam", scoreRam >= 0.80)

        // সুমন ↔ Suman
        val scoreSuman = BengaliHindiEnglishMatcher.computeMatchScore("সুমন", "Suman")
        assertTrue("Expected score for সুমন vs Suman >= 0.8, got $scoreSuman", scoreSuman >= 0.80)

        // অমিত ↔ Amit
        val scoreAmit = BengaliHindiEnglishMatcher.computeMatchScore("অমিত", "Amit")
        assertTrue("Expected score for অমিত vs Amit >= 0.8, got $scoreAmit", scoreAmit >= 0.80)
    }

    @Test
    fun testHindiCrossScriptMatching() {
        // राहुल ↔ Rahul
        val scoreRahul = BengaliHindiEnglishMatcher.computeMatchScore("राहुल", "Rahul")
        assertTrue("Expected score for राहुल vs Rahul >= 0.8, got $scoreRahul", scoreRahul >= 0.80)

        // विनोद ↔ Vinod ↔ Binod
        val scoreVinod = BengaliHindiEnglishMatcher.computeMatchScore("विनोद", "Vinod")
        assertTrue("Expected score for विनोद vs Vinod >= 0.8, got $scoreVinod", scoreVinod >= 0.80)

        // राम ↔ Ram
        val scoreRam = BengaliHindiEnglishMatcher.computeMatchScore("राम", "Ram")
        assertTrue("Expected score for राम vs Ram >= 0.8, got $scoreRam", scoreRam >= 0.80)

        // सुमन ↔ Suman
        val scoreSuman = BengaliHindiEnglishMatcher.computeMatchScore("सुमन", "Suman")
        assertTrue("Expected score for सुमन vs Suman >= 0.8, got $scoreSuman", scoreSuman >= 0.80)

        // अमित ↔ Amit
        val scoreAmit = BengaliHindiEnglishMatcher.computeMatchScore("अमित", "Amit")
        assertTrue("Expected score for अमित vs Amit >= 0.8, got $scoreAmit", scoreAmit >= 0.80)
    }

    @Test
    fun testTamilCrossScriptMatching() {
        // ராகுல் ↔ Rahul
        val scoreRahul = BengaliHindiEnglishMatcher.computeMatchScore("ராகுல்", "Rahul")
        assertTrue("Expected score for ராகுல் vs Rahul >= 0.75, got $scoreRahul", scoreRahul >= 0.75)
    }

    @Test
    fun testTeluguCrossScriptMatching() {
        // రాహుల్ ↔ Rahul
        val scoreRahul = BengaliHindiEnglishMatcher.computeMatchScore("రాహుల్", "Rahul")
        assertTrue("Expected score for రాహుల్ vs Rahul >= 0.8, got $scoreRahul", scoreRahul >= 0.80)
    }

    @Test
    fun testUnrelatedContactsDoNotMatch() {
        val score = BengaliHindiEnglishMatcher.computeMatchScore("রাহুল", "Suresh")
        assertTrue("Expected low score for Rahul vs Suresh, got $score", score < 0.50)
    }

    // ==========================================
    // 4. 12 Indian Languages Model & Support Tests
    // ==========================================

    @Test
    fun testAll12IndianLanguagesDefined() {
        val languages = SupportedLanguage.ALL_12_INDIAN_LANGUAGES
        assertEquals(12, languages.size)

        val expectedCodes = listOf(
            "bn-IN", // 1. Bengali
            "hi-IN", // 2. Hindi
            "en-IN", // 3. English (India)
            "te-IN", // 4. Telugu
            "mr-IN", // 5. Marathi
            "ta-IN", // 6. Tamil
            "gu-IN", // 7. Gujarati
            "kn-IN", // 8. Kannada
            "ml-IN", // 9. Malayalam
            "pa-IN", // 10. Punjabi
            "or-IN", // 11. Odia
            "as-IN"  // 12. Assamese
        )

        assertEquals(expectedCodes, languages.map { it.code })

        // Check native names
        assertEquals("বাংলা", SupportedLanguage.BENGALI.nativeName)
        assertEquals("हिंदी", SupportedLanguage.HINDI.nativeName)
        assertEquals("English", SupportedLanguage.ENGLISH.nativeName)
        assertEquals("తెలుగు", SupportedLanguage.TELUGU.nativeName)
        assertEquals("मराठी", SupportedLanguage.MARATHI.nativeName)
        assertEquals("தமிழ்", SupportedLanguage.TAMIL.nativeName)
        assertEquals("ગુજરાતી", SupportedLanguage.GUJARATI.nativeName)
        assertEquals("ಕನ್ನಡ", SupportedLanguage.KANNADA.nativeName)
        assertEquals("മലയാളം", SupportedLanguage.MALAYALAM.nativeName)
        assertEquals("ਪੰਜਾਬੀ", SupportedLanguage.PUNJABI.nativeName)
        assertEquals("ଓଡ଼ିଆ", SupportedLanguage.ODIA.nativeName)
        assertEquals("অসমীয়া", SupportedLanguage.ASSAMESE.nativeName)
    }

    @Test
    fun testLanguageManagerMessagesForAll12Languages() {
        for (lang in SupportedLanguage.ALL_12_INDIAN_LANGUAGES) {
            val callingMsg = LanguageManager.getCallingMessage("Rahul", lang)
            assertTrue("Calling message for ${lang.code} should not be empty", callingMsg.isNotEmpty())

            val noMatchMsg = LanguageManager.getNoMatchMessage("Rahul", lang)
            assertTrue("No match message for ${lang.code} should not be empty", noMatchMsg.isNotEmpty())

            val noPhoneMsg = LanguageManager.getNoPhoneNumberMessage("Rahul", lang)
            assertTrue("No phone message for ${lang.code} should not be empty", noPhoneMsg.isNotEmpty())

            val multiMatchMsg = LanguageManager.getMultipleMatchesMessage(lang)
            assertTrue("Multi match message for ${lang.code} should not be empty", multiMatchMsg.isNotEmpty())
        }
    }

    @Test
    fun testVoiceCommandParserWithPreferredLanguage() {
        // When Latin text without specific script is provided, preferredLanguage is respected
        val cmd = VoiceCommandParser.parse("Call Rahul", SupportedLanguage.BENGALI)
        assertTrue(cmd is ParsedVoiceCommand.CallContact)
        assertEquals("Rahul", (cmd as ParsedVoiceCommand.CallContact).targetName)
        assertEquals(SupportedLanguage.BENGALI, cmd.detectedLanguage)
    }

    // ==========================================
    // 5. Wake Name & Hands-Free Activation Tests
    // ==========================================

    @Test
    fun testWakeNameDetection_BengaliPhrases() {
        // 1. "রাম, বাবুকে কল করো"
        val r1 = com.example.manager.WakeNameDetector.checkWakeName("রাম, বাবুকে কল করো", "রাম")
        assertTrue("Expected wake detected for 'রাম, বাবুকে কল করো'", r1.isWakeWordDetected)
        assertEquals("বাবুকে কল করো", r1.remainingCommand)

        // 2. "রাম বাবুকে ফোন লাগাও"
        val r2 = com.example.manager.WakeNameDetector.checkWakeName("রাম বাবুকে ফোন লাগাও", "রাম")
        assertTrue("Expected wake detected for 'রাম বাবুকে ফোন লাগাও'", r2.isWakeWordDetected)
        assertEquals("বাবুকে ফোন লাগাও", r2.remainingCommand)

        // 3. "রাম, বাড়িতে ফোন করো"
        val r3 = com.example.manager.WakeNameDetector.checkWakeName("রাম, বাড়িতে ফোন করো", "রাম")
        assertTrue("Expected wake detected for 'রাম, বাড়িতে ফোন করো'", r3.isWakeWordDetected)
        assertEquals("বাড়িতে ফোন করো", r3.remainingCommand)

        // 4. Standalone wake name "রাম"
        val r4 = com.example.manager.WakeNameDetector.checkWakeName("রাম", "রাম")
        assertTrue("Expected wake detected for 'রাম'", r4.isWakeWordDetected)
        assertEquals("", r4.remainingCommand)

        // 5. English wake name "Ram" matching Bengali spoken "রাম, বাবুকে কল করো"
        val r5 = com.example.manager.WakeNameDetector.checkWakeName("রাম, বাবুকে কল করো", "Ram")
        assertTrue("Expected cross-script wake detected for configured 'Ram'", r5.isWakeWordDetected)
        assertEquals("বাবুকে কল করো", r5.remainingCommand)

        // 6. Bengali wake name "স্যাম" (Sam)
        val r6 = com.example.manager.WakeNameDetector.checkWakeName("স্যাম, রাহুলকে ফোন করো", "স্যাম")
        assertTrue("Expected wake detected for 'স্যাম, রাহুলকে ফোন করো'", r6.isWakeWordDetected)
        assertEquals("রাহুলকে ফোন করো", r6.remainingCommand)

        // 7. Bengali wake name "যদু" (Yadu)
        val r7 = com.example.manager.WakeNameDetector.checkWakeName("যদু বাবাকে কল কর", "যদু")
        assertTrue("Expected wake detected for 'যদু বাবাকে কল কর'", r7.isWakeWordDetected)
        assertEquals("বাবাকে কল কর", r7.remainingCommand)

        // 8. Bengali wake name "মধু" (Madhu)
        val r8 = com.example.manager.WakeNameDetector.checkWakeName("মধু, মাকে ফোন দাও", "মধু")
        assertTrue("Expected wake detected for 'মধু, মাকে ফোন দাও'", r8.isWakeWordDetected)
        assertEquals("মাকে ফোন দাও", r8.remainingCommand)
    }

    @Test
    fun testWakeNameDetection_HindiPhrases() {
        // "राम, राहुल को कॉल करो"
        val r1 = com.example.manager.WakeNameDetector.checkWakeName("राम, राहुल को कॉल करो", "राम")
        assertTrue("Expected wake detected for 'राम, राहुल को कॉल करो'", r1.isWakeWordDetected)
        assertEquals("राहुल को कॉल करो", r1.remainingCommand)

        // "हे राम विनोद को फोन लगाओ"
        val r2 = com.example.manager.WakeNameDetector.checkWakeName("हे राम विनोद को फोन लगाओ", "Ram")
        assertTrue("Expected wake detected for 'हे राम विनोद को फोन लगाओ'", r2.isWakeWordDetected)
        assertEquals("विनोद को फोन लगाओ", r2.remainingCommand)
    }

    @Test
    fun testWakeNameDetection_EnglishPhrases() {
        // "Hey Ram, call Vinod"
        val r1 = com.example.manager.WakeNameDetector.checkWakeName("Hey Ram, call Vinod", "Ram")
        assertTrue(r1.isWakeWordDetected)
        assertEquals("call Vinod", r1.remainingCommand)

        // "VirJoy, please call mom"
        val r2 = com.example.manager.WakeNameDetector.checkWakeName("VirJoy, please call mom", "VirJoy")
        assertTrue(r2.isWakeWordDetected)
        assertEquals("please call mom", r2.remainingCommand)

        // "Sam call Dad"
        val r3 = com.example.manager.WakeNameDetector.checkWakeName("Sam call Dad", "Sam")
        assertTrue(r3.isWakeWordDetected)
        assertEquals("call Dad", r3.remainingCommand)
    }

    @Test
    fun testWakeNameDetection_NormalConversationIgnored() {
        // Normal conversation phrases that do NOT contain the configured wake name must NOT trigger activation
        val nonTriggers = listOf(
            "আজকে আবহাওয়া কেমন?",
            "বাবুকে কল করো",
            "কালকে বাড়ি যাব",
            "आज का मौसम कैसा है",
            "How are you doing today?",
            "What is the time right now?"
        )

        for (phrase in nonTriggers) {
            val result = com.example.manager.WakeNameDetector.checkWakeName(phrase, "রাম")
            org.junit.Assert.assertFalse(
                "Phrase '$phrase' should NOT activate when wake name is 'রাম'",
                result.isWakeWordDetected
            )
        }
    }

    @Test
    fun testWakeAcknowledgementAndPromptsForAll12Languages() {
        for (lang in SupportedLanguage.ALL_12_INDIAN_LANGUAGES) {
            val ack = LanguageManager.getWakeAcknowledgementMessage(lang)
            assertTrue("Acknowledgement message for ${lang.code} should not be empty", ack.isNotEmpty())

            val idlePrompt = LanguageManager.getWakeIdlePrompt("রাম", lang)
            assertTrue("Idle prompt for ${lang.code} should not be empty", idlePrompt.isNotEmpty())
            assertTrue("Idle prompt for ${lang.code} should contain wake name", idlePrompt.contains("রাম"))

            val commandPrompt = LanguageManager.getListeningForCommandPrompt(lang)
            assertTrue("Command prompt for ${lang.code} should not be empty", commandPrompt.isNotEmpty())
        }
    }

    // ==========================================
    // 6. Voice-Only Disambiguation Tests
    // ==========================================

    @Test
    fun testDisambiguationPromptGeneration() {
        val options = listOf(
            com.example.model.PhoneNumberOption(
                number = "+919876543210",
                label = "Mobile",
                lastFourDigits = "3210",
                optionIndex = 1,
                contactName = "Rahul"
            ),
            com.example.model.PhoneNumberOption(
                number = "+919876543211",
                label = "Home",
                lastFourDigits = "3211",
                optionIndex = 2,
                contactName = "Rahul"
            )
        )

        val bengaliPrompt = LanguageManager.formatMultiNumberDisambiguationPrompt("Rahul", options, SupportedLanguage.BENGALI)
        assertTrue("Bengali prompt should contain contact name", bengaliPrompt.contains("Rahul"))
        assertTrue("Bengali prompt should contain option 1", bengaliPrompt.contains("এক নম্বর"))
        assertTrue("Bengali prompt should contain option 2", bengaliPrompt.contains("দুই নম্বর"))

        val englishPrompt = LanguageManager.formatMultiNumberDisambiguationPrompt("Rahul", options, SupportedLanguage.ENGLISH)
        assertTrue("English prompt should contain contact name", englishPrompt.contains("Rahul"))
        assertTrue("English prompt should contain 'Option 1'", englishPrompt.contains("Option 1"))
    }

    @Test
    fun testDisambiguationResolver_ResolvingBySpokenOrdinalOrIndex() {
        val options = listOf(
            com.example.model.PhoneNumberOption(
                number = "+919876543210",
                label = "Mobile",
                lastFourDigits = "3210",
                optionIndex = 1,
                contactName = "Rahul"
            ),
            com.example.model.PhoneNumberOption(
                number = "+919876543211",
                label = "Home",
                lastFourDigits = "3211",
                optionIndex = 2,
                contactName = "Rahul"
            )
        )

        // 1. Spoken "১" / "প্রথমটি" in Bengali
        val match1 = com.example.manager.DisambiguationResolver.resolveOption("১ নম্বর", options, SupportedLanguage.BENGALI)
        assertEquals(1, match1?.optionIndex)

        val match2 = com.example.manager.DisambiguationResolver.resolveOption("দ্বিতীয়টি", options, SupportedLanguage.BENGALI)
        assertEquals(2, match2?.optionIndex)

        // 2. Spoken "first" / "2" in English
        val match3 = com.example.manager.DisambiguationResolver.resolveOption("call the first one", options, SupportedLanguage.ENGLISH)
        assertEquals(1, match3?.optionIndex)

        val match4 = com.example.manager.DisambiguationResolver.resolveOption("option 2", options, SupportedLanguage.ENGLISH)
        assertEquals(2, match4?.optionIndex)

        // 3. Spoken label "home" / "mobile"
        val match5 = com.example.manager.DisambiguationResolver.resolveOption("mobile", options, SupportedLanguage.ENGLISH)
        assertEquals(1, match5?.optionIndex)

        val match6 = com.example.manager.DisambiguationResolver.resolveOption("home", options, SupportedLanguage.ENGLISH)
        assertEquals(2, match6?.optionIndex)

        // 4. Spoken last 4 digits "3211"
        val match7 = com.example.manager.DisambiguationResolver.resolveOption("3211 নম্বরে লাগাও", options, SupportedLanguage.BENGALI)
        assertEquals(2, match7?.optionIndex)
    }
}
