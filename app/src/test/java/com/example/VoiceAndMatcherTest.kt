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
}
