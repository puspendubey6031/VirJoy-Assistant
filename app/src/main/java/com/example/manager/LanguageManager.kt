package com.example.manager

import com.example.model.SupportedLanguage
import java.util.Locale

object LanguageManager {

    /**
     * Detects the language of a given spoken input text.
     * Primary signal: Unicode script range analysis + vocabulary heuristics.
     */
    fun detectLanguage(text: String): SupportedLanguage {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return SupportedLanguage.ENGLISH

        var bengaliCount = 0
        var devanagariCount = 0
        var gujaratiCount = 0
        var gurmukhiCount = 0
        var odiaCount = 0
        var tamilCount = 0
        var teluguCount = 0
        var kannadaCount = 0
        var malayalamCount = 0
        var arabicCount = 0
        var latinCount = 0

        for (ch in trimmed) {
            val code = ch.code
            when (code) {
                in 0x0980..0x09FF -> bengaliCount++
                in 0x0900..0x097F -> devanagariCount++
                in 0x0A80..0x0AFF -> gujaratiCount++
                in 0x0A00..0x0A7F -> gurmukhiCount++
                in 0x0B00..0x0B7F -> odiaCount++
                in 0x0B80..0x0BFF -> tamilCount++
                in 0x0C00..0x0C7F -> teluguCount++
                in 0x0C80..0x0CFF -> kannadaCount++
                in 0x0D00..0x0D7F -> malayalamCount++
                in 0x0600..0x06FF -> arabicCount++
                in 'a'.code..'z'.code, in 'A'.code..'Z'.code -> latinCount++
            }
        }

        // Check Assamese-specific characters in Bengali/Assamese block: ৰ (0x09F0) and ৱ (0x09F1)
        val hasAssameseSpecificChars = trimmed.contains('\u09F0') || trimmed.contains('\u09F1')
        if (hasAssameseSpecificChars) {
            return SupportedLanguage.ASSAMESE
        }

        // Distinct script mapping
        if (bengaliCount > 0 && bengaliCount >= devanagariCount && bengaliCount >= latinCount) {
            // Check for Assamese verb endings (লৈ, কৰক, কৰা, নম্বৰ)
            if (trimmed.contains("লৈ") || trimmed.contains("কৰক") || trimmed.contains("কৰা") || trimmed.contains("নম্বৰ")) {
                return SupportedLanguage.ASSAMESE
            }
            return SupportedLanguage.BENGALI
        }

        if (devanagariCount > 0 && devanagariCount >= latinCount) {
            // Check if it has distinctly Hindi verbs or postpositions
            val hasHindiMarkers = trimmed.contains("को") || trimmed.contains("करो") || trimmed.contains("करें") ||
                    trimmed.contains("कीजिए") || trimmed.contains("लगाओ") || trimmed.contains("मिलाओ")

            if (!hasHindiMarkers) {
                // Check for Marathi-specific markers (करा, लावा, यांना, संपर्क, or Marathi suffix ला)
                if (trimmed.contains("करा") || trimmed.contains("लावा") ||
                    trimmed.contains("यांना") || trimmed.contains("संपर्क") || Regex("\\S+ला\\b").containsMatchIn(trimmed)) {
                    return SupportedLanguage.MARATHI
                }
            }
            return SupportedLanguage.HINDI
        }

        if (gujaratiCount > 0) return SupportedLanguage.GUJARATI
        if (gurmukhiCount > 0) return SupportedLanguage.PUNJABI
        if (odiaCount > 0) return SupportedLanguage.ODIA
        if (tamilCount > 0) return SupportedLanguage.TAMIL
        if (teluguCount > 0) return SupportedLanguage.TELUGU
        if (kannadaCount > 0) return SupportedLanguage.KANNADA
        if (malayalamCount > 0) return SupportedLanguage.MALAYALAM
        if (arabicCount > 0) return SupportedLanguage.URDU

        // For Latin text, detect romanized Indian phrases (Hinglish / Benglish etc.)
        val lower = trimmed.lowercase(Locale.ROOT)
        if (lower.contains("koro") || lower.contains("koro na") || lower.contains("daao") || lower.contains("phone koro")) {
            return SupportedLanguage.BENGALI
        }
        if (lower.contains("karo") || lower.contains("lagao") || lower.contains("milao") || lower.contains("call karo") || lower.contains("phone lagao")) {
            return SupportedLanguage.HINDI
        }

        return SupportedLanguage.ENGLISH
    }

    /**
     * Formats response when a single contact match is found and the call is initiated.
     */
    fun getCallingMessage(contactName: String, lang: SupportedLanguage): String {
        return when (lang) {
            SupportedLanguage.BENGALI -> "${contactName}-কে কল করা হচ্ছে..."
            SupportedLanguage.HINDI -> "$contactName को कॉल किया जा रहा है..."
            SupportedLanguage.ASSAMESE -> "$contactName-লৈ কল কৰা হৈছে..."
            SupportedLanguage.GUJARATI -> "$contactName-ને કોલ કરી રહ્યા છીએ..."
            SupportedLanguage.KANNADA -> "$contactName ಅವರಿಗೆ ಕರೆ ಮಾಡಲಾಗುತ್ತಿದೆ..."
            SupportedLanguage.MALAYALAM -> "$contactName-ലേക്ക് വിളിക്കുന്നു..."
            SupportedLanguage.MARATHI -> "$contactName यांना कॉल करत आहे..."
            SupportedLanguage.ODIA -> "$contactName-ଙ୍କୁ କଲ୍ କରାଯାଉଛି..."
            SupportedLanguage.PUNJABI -> "$contactName ਨੂੰ ਕਾਲ ਕੀਤਾ ਜਾ ਰਿਹਾ ਹੈ..."
            SupportedLanguage.TAMIL -> "$contactName-க்கு அழைக்கப்படுகிறது..."
            SupportedLanguage.TELUGU -> "$contactName కి కాల్ చేస్తోంది..."
            SupportedLanguage.URDU -> "$contactName کو کال کی جا رہی ہے..."
            SupportedLanguage.ENGLISH -> "Calling $contactName..."
        }
    }

    /**
     * Formats response when no matching contact is found.
     */
    fun getNoMatchMessage(contactName: String, lang: SupportedLanguage): String {
        return when (lang) {
            SupportedLanguage.BENGALI -> "আপনার ফোনের Contacts-এ $contactName নামে কোনো নম্বর পাওয়া যায়নি।"
            SupportedLanguage.HINDI -> "आपके फोन के Contacts में $contactName नाम का कोई नंबर नहीं मिला।"
            SupportedLanguage.ASSAMESE -> "আপোনাৰ ফোনৰ Contacts-ত $contactName নামৰ কোনো নম্বৰ পোৱা নগ'ল।"
            SupportedLanguage.GUJARATI -> "તમારા ફોનના Contacts માં $contactName નામનો કોઈ નંબર મળ્યો નથી."
            SupportedLanguage.KANNADA -> "ನಿಮ್ಮ ಫೋನ್‌ನ Contacts ನಲ್ಲಿ $contactName ಹೆಸರಿನ ಯಾವುದೇ ಸಂಖ್ಯೆ ಕಂಡುಬಂದಿಲ್ಲ."
            SupportedLanguage.MALAYALAM -> "നിങ്ങളുടെ ഫോണിന്റെ Contacts-ൽ $contactName എന്ന പേരിൽ നമ്പറൊന്നും കണ്ടെത്തിയില്ല."
            SupportedLanguage.MARATHI -> "तुमच्या फोनच्या Contacts मध्ये $contactName नावाचा कोणताही नंबर सापडला नाही."
            SupportedLanguage.ODIA -> "ଆପଣଙ୍କ ଫୋନର Contacts ରେ $contactName ନାମରେ କୌଣସି ନମ୍ବର ମିଳିଲା ନାହିଁ।"
            SupportedLanguage.PUNJABI -> "ਤੁਹਾਡੇ ਫ਼ੋਨ ਦੇ Contacts ਵਿੱਚ $contactName ਨਾਮ ਦਾ ਕੋਈ ਨੰਬਰ ਨਹੀਂ ਮਿਲਿਆ।"
            SupportedLanguage.TAMIL -> "உங்கள் போன் Contacts-ல் $contactName என்ற பெயரில் எந்த எண்ணும் கிடைக்கவில்லை."
            SupportedLanguage.TELUGU -> "మీ ఫోన్ Contacts లో $contactName పేరుతో నంబర్ ఏదీ కనుగొనబడలేదు."
            SupportedLanguage.URDU -> "آپ کے فون کے Contacts میں $contactName نام کا کوئی نمبر نہیں ملا۔"
            SupportedLanguage.ENGLISH -> "I couldn't find $contactName in your contacts."
        }
    }

    /**
     * Formats disambiguation prompt when multiple matches are found.
     */
    fun getMultipleMatchesMessage(lang: SupportedLanguage): String {
        return when (lang) {
            SupportedLanguage.BENGALI -> "একাধিক পরিচিতি পাওয়া গেছে। অনুগ্রহ করে একটি বেছে নিন:"
            SupportedLanguage.HINDI -> "एक से अधिक संपर्क मिले। कृपया एक चुनें:"
            SupportedLanguage.ASSAMESE -> "একাধিক যোগাযোগ পোৱা গৈছে। অনুগ্ৰহ কৰি এটা বাছক:"
            SupportedLanguage.GUJARATI -> "એક કરતાં વધુ સંપર્કો મળ્યા. કૃપા કરીને એક પસંદ કરો:"
            SupportedLanguage.KANNADA -> "ಬಹು ಸಂಪರ್ಕಗಳು ಕಂಡುಬಂದಿವೆ. ದಯವಿಟ್ಟು ಒಂದನ್ನು ಆಯ್ಕೆಮಾಡಿ:"
            SupportedLanguage.MALAYALAM -> "ഒന്നിലധികം കോൺടാക്റ്റുകൾ കണ്ടെത്തി. ദയവായി ഒന്ന് തിരഞ്ഞെടുക്കുക:"
            SupportedLanguage.MARATHI -> "एकापेक्षा जास्त संपर्क सापडले. कृपया एक निवडा:"
            SupportedLanguage.ODIA -> "ଏକାଧିକ ସମ୍ପର୍କ ମିଳିଲା। ଦୟାକରି ଗୋଟିଏ ବାଛନ୍ତୁ:"
            SupportedLanguage.PUNJABI -> "ਇੱਕ ਤੋਂ ਵੱਧ ਸੰਪਰਕ ਮਿਲੇ। ਕਿਰਪਾ ਕਰਕੇ ਇੱਕ ਚੁਣੋ:"
            SupportedLanguage.TAMIL -> "பல தொடர்புகள் கிடைத்துள்ளன. ஒன்றைத் தேர்ந்தெடுக்கவும்:"
            SupportedLanguage.TELUGU -> "బహుళ పరిచయాలు కనుగొనబడ్డాయి. దయచేసి ఒకదాన్ని ఎంచుకోండి:"
            SupportedLanguage.URDU -> "ایک سے زیادہ رابطے ملے۔ براہ کرم ایک منتخب کریں:"
            SupportedLanguage.ENGLISH -> "Multiple contacts found. Please choose:"
        }
    }

    /**
     * Formats response when a contact has no phone number.
     */
    fun getNoPhoneNumberMessage(contactName: String, lang: SupportedLanguage): String {
        return when (lang) {
            SupportedLanguage.BENGALI -> "$contactName-এর কোনো ফোন নম্বর উপলব্ধ নেই।"
            SupportedLanguage.HINDI -> "$contactName का कोई फोन नंबर उपलब्ध नहीं है।"
            SupportedLanguage.ASSAMESE -> "$contactName-ৰ কোনো ফোন নম্বৰ উপলব্ধ নাই।"
            SupportedLanguage.GUJARATI -> "$contactName માટે કોઈ ફોન નંબર ઉપલબ્ધ નથી."
            SupportedLanguage.KANNADA -> "$contactName ಗೆ ಯಾವುದೇ ಫೋನ್ ಸಂಖ್ಯೆ ಲಭ್ಯವಿಲ್ಲ."
            SupportedLanguage.MALAYALAM -> "$contactName-ന് ഫോൺ നമ്പർ ലഭ്യമല്ല."
            SupportedLanguage.MARATHI -> "$contactName साठी कोणताही फोन नंबर उपलब्ध नाही."
            SupportedLanguage.ODIA -> "$contactName ପାଇଁ କୌଣସି ଫୋନ୍ ନମ୍ବର ଉପଲବ୍ଧ ନାହିଁ।"
            SupportedLanguage.PUNJABI -> "$contactName ਲਈ ਕੋਈ ਫ਼ੋਨ ਨੰਬਰ ਉਪਲਬਧ ਨਹੀਂ ਹੈ।"
            SupportedLanguage.TAMIL -> "$contactName-க்கு தொலைபேசி எண் எதுவும் இல்லை."
            SupportedLanguage.TELUGU -> "$contactName కి ఫోన్ నంబర్ అందుబాటులో లేదు."
            SupportedLanguage.URDU -> "$contactName کے لیے کوئی فون نمبر دستیاب نہیں ہے۔"
            SupportedLanguage.ENGLISH -> "No phone number available for $contactName."
        }
    }

    /**
     * Formats response when call execution fails.
     */
    fun getCallFailedMessage(contactName: String, errorReason: String, lang: SupportedLanguage): String {
        return when (lang) {
            SupportedLanguage.BENGALI -> "কল করা সম্ভব হয়নি: $errorReason"
            SupportedLanguage.HINDI -> "कॉल नहीं किया जा सका: $errorReason"
            SupportedLanguage.ASSAMESE -> "কল কৰিব পৰা নগ'ল: $errorReason"
            SupportedLanguage.GUJARATI -> "કોલ કરી શકાયો નથી: $errorReason"
            SupportedLanguage.KANNADA -> "ಕರೆ ಮಾಡಲು ಸಾಧ್ಯವಾಗಲಿಲ್ಲ: $errorReason"
            SupportedLanguage.MALAYALAM -> "വിളിക്കാൻ കഴിഞ്ഞില്ല: $errorReason"
            SupportedLanguage.MARATHI -> "कॉल करता आला नाही: $errorReason"
            SupportedLanguage.ODIA -> "କଲ୍ କରିବା ସମ୍ଭବ ହେଲା ନାହିଁ: $errorReason"
            SupportedLanguage.PUNJABI -> "ਕਾਲ ਨਹੀਂ ਕੀਤੀ ਜਾ ਸਕੀ: $errorReason"
            SupportedLanguage.TAMIL -> "அழைக்க முடியவில்லை: $errorReason"
            SupportedLanguage.TELUGU -> "కాల్ చేయడం సాధ్యం కాలేదు: $errorReason"
            SupportedLanguage.URDU -> "کال نہیں کی جا سکی: $errorReason"
            SupportedLanguage.ENGLISH -> "Could not make call: $errorReason"
        }
    }

    /**
     * Formats response when no matching contact is found (by query string).
     */
    fun getNoContactFoundMessage(query: String, lang: SupportedLanguage): String {
        return getNoMatchMessage(query, lang)
    }

    /**
     * Formats disambiguation prompt when multiple matches are found (with count).
     */
    fun getDisambiguationMessage(query: String, count: Int, lang: SupportedLanguage): String {
        return when (lang) {
            SupportedLanguage.BENGALI -> "\"$query\" নামে $count টি পরিচিতি পাওয়া গেছে। অনুগ্রহ করে একটি বেছে নিন:"
            SupportedLanguage.HINDI -> "\"$query\" नाम के $count संपर्क मिले। कृपया एक चुनें:"
            SupportedLanguage.ASSAMESE -> "\"$query\" নামত $count টা যোগাযোগ পোৱা গৈছে। অনুগ্ৰহ কৰি এটা বাছক:"
            SupportedLanguage.GUJARATI -> "\"$query\" નામથી $count સંપર્કો મળ્યા. કૃપા કરીને એક પસંદ કરો:"
            SupportedLanguage.KANNADA -> "\"$query\" ಹೆಸರಿನಲ್ಲಿ $count ಸಂಪರ್ಕಗಳು ಕಂಡುಬಂದಿವೆ. ದಯವಿಟ್ಟು ಒಂದನ್ನು ಆಯ್ಕೆಮಾಡಿ:"
            SupportedLanguage.MALAYALAM -> "\"$query\" എന്ന പേരിൽ $count കോൺടാക്റ്റുകൾ കണ്ടെത്തി. ദയവായി ഒന്ന് തിരഞ്ഞെടുക്കുക:"
            SupportedLanguage.MARATHI -> "\"$query\" नावाने $count संपर्क सापडले. कृपया एक निवडा:"
            SupportedLanguage.ODIA -> "\"$query\" ନାମରେ $count ଟି ସମ୍ପର୍କ ମିଳିଲା। ଦୟାକରି ଗୋଟିଏ ବାଛନ୍ତୁ:"
            SupportedLanguage.PUNJABI -> "\"$query\" ਨਾਮ ਨਾਲ $count ਸੰਪਰਕ ਮਿਲੇ। ਕਿਰਪਾ ਕਰਕੇ ਇੱਕ ਚੁਣੋ:"
            SupportedLanguage.TAMIL -> "\"$query\" பெயரில் $count தொடர்புகள் கிடைத்துள்ளன. ஒன்றைத் தேர்ந்தெடுக்கவும்:"
            SupportedLanguage.TELUGU -> "\"$query\" పేరుతో $count పరిచయాలు కనుగొనబడ్డాయి. దయచేసి ఒకదాన్ని ఎంచుకోండి:"
            SupportedLanguage.URDU -> "\"$query\" نام کے $count رابطے ملے۔ براہ کرم ایک منتخب کریں:"
            SupportedLanguage.ENGLISH -> "Found $count contacts matching \"$query\". Please choose one:"
        }
    }

    /**
     * Message when recognized speech couldn't be parsed into an action.
     */
    fun getUnrecognizedCommandMessage(rawText: String, lang: SupportedLanguage): String {
        return when (lang) {
            SupportedLanguage.BENGALI -> "বুঝতে পারিনি: \"$rawText\"। উদাহরণ: \"রাহুলকে ফোন করো\"।"
            SupportedLanguage.HINDI -> "समझ नहीं आया: \"$rawText\"। उदाहरण: \"राहुल को कॉल करो\"।"
            SupportedLanguage.ASSAMESE -> "বুজি নাপালোঁ: \"$rawText\"। উদাহৰণ: \"ৰাহুললৈ কল কৰক\"।"
            SupportedLanguage.GUJARATI -> "સમજાયું નહીં: \"$rawText\"। ઉદાહરણ: \"રાહુલને કોલ કરો\"।"
            SupportedLanguage.KANNADA -> "ಅರ್ಥವಾಗಲಿಲ್ಲ: \"$rawText\"। ಉದಾಹರಣೆ: \"ರಾಹುಲ್ ಗೆ ಕಾಲ್ ಮಾಡಿ\"।"
            SupportedLanguage.MALAYALAM -> "മനസ്സിലായില്ല: \"$rawText\"। ഉദാഹരണം: \"രാഹുലിനെ വിളിക്കൂ\"।"
            SupportedLanguage.MARATHI -> "समजले नाही: \"$rawText\"। उदाहरण: \"राहुलला कॉल करा\"।"
            SupportedLanguage.ODIA -> "ବୁଝିପାରିଲି ନାହିଁ: \"$rawText\"। ଉଦାହରଣ: \"ରାହୁଲଙ୍କୁ କଲ୍ କରନ୍ତୁ\"।"
            SupportedLanguage.PUNJABI -> "ਸਮਝ ਨਹੀਂ ਆਇਆ: \"$rawText\"। ਉਦਾਹਰਣ: \"ਰਾਹੁਲ ਨੂੰ ਕਾਲ ਕਰੋ\"।"
            SupportedLanguage.TAMIL -> "புரியவில்லை: \"$rawText\"। உதாரணம்: \"ராகுலுக்கு போன் செய்\"।"
            SupportedLanguage.TELUGU -> "అర్థం కాలేదు: \"$rawText\"। ఉదాహరణ: \"రాహుల్ కి కాల్ చేయి\"।"
            SupportedLanguage.URDU -> "سمجھ نہیں آیا: \"$rawText\"۔ مثال: \"راہل کو کال کرو\"۔"
            SupportedLanguage.ENGLISH -> "I didn't understand: \"$rawText\". Try saying \"Call John\"."
        }
    }
}
