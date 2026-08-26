package com.example.manager

import com.example.model.Contact
import com.example.model.PhoneNumberOption
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
        val lower = trimmed.lowercase(Locale.ROOT)

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

        if (lower.contains("koro") || lower.contains("koro na") || lower.contains("daao") || lower.contains("phone koro")) {
            return SupportedLanguage.BENGALI
        }
        if (lower.contains("karo") || lower.contains("lagao") || lower.contains("milao") || lower.contains("call karo") || lower.contains("phone lagao")) {
            return SupportedLanguage.HINDI
        }

        return SupportedLanguage.ENGLISH
    }

    /**
     * Checks whether a recognized voice utterance is an immediate stop or cancellation command.
     */
    fun isCancelOrStopPhrase(text: String): Boolean {
        val trimmed = text.trim().lowercase(Locale.ROOT)
        if (trimmed.isEmpty()) return false

        val cancelTokens = listOf(
            // English & Romanized Indian expressions
            "stop", "cancel", "shut up", "hush", "quit", "halt", "reset", "abort", "nevermind", "never mind", "silence", "be quiet", "close", "end call", "hang up", "wait",
            "thamo", "tham", "thamo na", "ruko", "roko", "ruk jao", "band koro", "band karo", "chup", "chup karo", "chup koro", "chup raho", "bas", "khatam", "rahne do", "rehe dao",
            // Bengali & common variants
            "থামো", "থাম", "থামো না", "বন্ধ করো", "বন্ধ কর", "চুপ করো", "চুপ কর", "চুপ থাকো", "স্টপ করো", "স্টপ কর", "স্টপ", "রিসেট", "বাদ দাও", "ব্যাস", "থেমে যাও", "বাতিল", "বাতিল করো", "থাক", "রুকো", "রোকো", "রুক যাও", "ক্যান্সেল", "ক্যানসেল",
            // Hindi & common variants
            "रुको", "रुक जाओ", "रुक", "रोक", "रोको", "बंद करो", "बंद कर", "बंद", "चुप करो", "चुप रहो", "चुप", "स्टॉप", "कैंसिल", "कैनसिल", "बस", "रद्द करो", "रहने दो", "रहने दे", "खत्म", "मत करो",
            // Assamese
            "থামক", "বন্ধ কৰক", "চুপ থাকক", "বাতিল কৰক", "ষ্টপ", "থাকক", "ৰোকক",
            // Gujarati
            "રોકો", "બંધ કરો", "શાંત રહો", "રદ કરો", "સ્ટોપ", "બસ",
            // Marathi
            "थांबा", "बंद करा", "शांत राहा", "रद्द करा", "स्टॉप", "बस",
            // Punjabi
            "ਰੋਕੋ", "ਬੰਦ ਕਰੋ", "ਚੁੱਪ ਕਰੋ", "ਰੱਦ ਕਰੋ", "ਸਟਾਪ", "ਬੱਸ",
            // Odia
            "ରୁହନ୍ତୁ", "ବନ୍ଦ କରନ୍ତୁ", "ଚୁପ୍ ରୁହନ୍ତୁ", "ବାତିଲ କରନ୍ତୁ", "ଷ୍ଟପ୍",
            // Tamil
            "நிறுத்து", "நிறுத்துங்கள்", "மூடு", "அமைதியாக இரு", "ரத்து செய்", "ஸ்டாப்",
            // Telugu
            "ఆపు", "ఆపండి", "మూయి", "రద్దు చేయి", "స్టಾప్",
            // Kannada
            "ನಿಲ್ಲಿಸಿ", "ನಿಲ್ಲಿಸು", "ಮುಚ್ಚು", "ರದ್ದುಮಾಡು", "ಸ್ಟಾಪ್",
            // Malayalam
            "നിർത്തൂ", "നിർത്തുക", "മിണ്ടാതിരിക്കൂ", "റദ്ദാക്കുക", "സ്റ്റോപ്പ്",
            // Urdu
            "روکو", "بند کرو", "خاموش", "منسوخ کریں", "سٹاپ"
        )

        for (token in cancelTokens) {
            if (trimmed == token || trimmed.contains(token)) {
                return true
            }
        }
        return false
    }

    /**
     * Spoken message when an ongoing operation or voice flow is cancelled.
     */
    fun getCancelledMessage(lang: SupportedLanguage): String {
        return when (lang) {
            SupportedLanguage.BENGALI -> "বাতিল করা হয়েছে।"
            SupportedLanguage.HINDI -> "रद्द कर दिया गया।"
            SupportedLanguage.ASSAMESE -> "বাতিল কৰা হৈছে।"
            SupportedLanguage.GUJARATI -> "રદ કરવામાં આવ્યું છે."
            SupportedLanguage.KANNADA -> "ರದ್ದುಮಾಡಲಾಗಿದೆ."
            SupportedLanguage.MALAYALAM -> "റദ്ദാക്കി."
            SupportedLanguage.MARATHI -> "रद्द केले आहे."
            SupportedLanguage.ODIA -> "ବାତିଲ କରାଗଲା।"
            SupportedLanguage.PUNJABI -> "ਰੱਦ ਕਰ ਦਿੱਤਾ ਗਿਆ।"
            SupportedLanguage.TAMIL -> "ரத்து செய்யப்பட்டது."
            SupportedLanguage.TELUGU -> "రద్దు చేయబడింది."
            SupportedLanguage.URDU -> "منسوخ کر دیا گیا۔"
            SupportedLanguage.ENGLISH -> "Cancelled."
        }
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
     * Formats response when multiple matching contacts are found.
     */
    fun getMultipleMatchesMessage(lang: SupportedLanguage): String {
        return when (lang) {
            SupportedLanguage.BENGALI -> "একাধিক মিল পাওয়া গেছে। অনুগ্রহ করে একটি নির্বাচন করুন।"
            SupportedLanguage.HINDI -> "कई मिलान मिले हैं। कृपया एक चुनें।"
            SupportedLanguage.ASSAMESE -> "একাধিক মিল পোৱা গৈছে। অনুগ্ৰহ কৰি এটা বাছনি কৰক।"
            SupportedLanguage.GUJARATI -> "બહુવિધ મેળ મળ્યા. કૃપા કરીને એક પસંદ કરો."
            SupportedLanguage.KANNADA -> "ಬಹು ಹೊಂದಾಣಿಕೆಗಳು ಕಂಡುಬಂದಿವೆ. ದಯವಿಟ್ಟು ಒಂದನ್ನು ಆಯ್ಕೆಮಾಡಿ."
            SupportedLanguage.MALAYALAM -> "ഒന്നിലധികം പൊരുത്തങ്ങൾ കണ്ടെത്തി. ഒരെണ്ണം തിരഞ്ഞെടുക്കുക."
            SupportedLanguage.MARATHI -> "अनेक जुळण्या सापडल्या. कृपया एक निवडा."
            SupportedLanguage.ODIA -> "ଏକାଧିକ ମେଳ ମିଳିଲା। ଦୟାକରି ଗୋଟିଏ ବାଛନ୍ତୁ।"
            SupportedLanguage.PUNJABI -> "ਕਈ ਮੇਲ ਮਿਲੇ ਹਨ। ਕਿਰਪਾ ਕਰਕੇ ਇੱਕ ਚੁਣੋ।"
            SupportedLanguage.TAMIL -> "பல பொருத்தங்கள் கிடைத்துள்ளன. ஒன்றைத் தேர்ந்தெடுக்கவும்."
            SupportedLanguage.TELUGU -> "బహుళ సరిపోలిକలు దొరికాయి. దయచేసి ఒకదాన్ని ఎంచుకోండి."
            SupportedLanguage.URDU -> "ایک سے زیادہ مماثلتیں ملیں۔ براہ کرم ایک کا انتخاب کریں۔"
            SupportedLanguage.ENGLISH -> "Multiple matches found. Please select one."
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
            SupportedLanguage.MARATHI -> "कॉल करणे शक्य झाले नाही: $errorReason"
            SupportedLanguage.ODIA -> "କଲ୍ କରିବା ସମ୍ଭବ ହେଲା ନାହିଁ: $errorReason"
            SupportedLanguage.PUNJABI -> "ਕਾਲ ਨਹੀਂ ਕੀਤੀ ਜਾ ਸਕੀ: $errorReason"
            SupportedLanguage.TAMIL -> "அழைக்க முடியவில்லை: $errorReason"
            SupportedLanguage.TELUGU -> "కాల్ చేయడం సాధ్యం కాలేదు: $errorReason"
            SupportedLanguage.URDU -> "کال نہیں کی جا سکی: $errorReason"
            SupportedLanguage.ENGLISH -> "Could not make call: $errorReason"
        }
    }

    private fun getOrdinalWord(idx: Int, lang: SupportedLanguage): String {
        return when (lang) {
            SupportedLanguage.BENGALI -> when (idx) { 1 -> "এক"; 2 -> "দুই"; 3 -> "তিন"; else -> "$idx" }
            SupportedLanguage.HINDI -> when (idx) { 1 -> "एक"; 2 -> "दो"; 3 -> "तीन"; else -> "$idx" }
            SupportedLanguage.ASSAMESE -> when (idx) { 1 -> "এক"; 2 -> "দুই"; 3 -> "তিনি"; else -> "$idx" }
            SupportedLanguage.GUJARATI -> when (idx) { 1 -> "એક"; 2 -> "બે"; 3 -> "ત્રણ"; else -> "$idx" }
            SupportedLanguage.KANNADA -> when (idx) { 1 -> "ಒಂದು"; 2 -> "ಎರಡು"; 3 -> "ಮೂರು"; else -> "$idx" }
            SupportedLanguage.MALAYALAM -> when (idx) { 1 -> "ഒന്ന്"; 2 -> "രണ്ട്"; 3 -> "മൂന്ന്"; else -> "$idx" }
            SupportedLanguage.MARATHI -> when (idx) { 1 -> "एक"; 2 -> "दोन"; 3 -> "तीन"; else -> "$idx" }
            SupportedLanguage.ODIA -> when (idx) { 1 -> "ଏକ"; 2 -> "ଦୁଇ"; 3 -> "ତିନି"; else -> "$idx" }
            SupportedLanguage.PUNJABI -> when (idx) { 1 -> "ਇੱਕ"; 2 -> "ਦੋ"; 3 -> "ਤਿੰਨ"; else -> "$idx" }
            SupportedLanguage.TAMIL -> when (idx) { 1 -> "ஒன்று"; 2 -> "இரண்டு"; 3 -> "மூன்று"; else -> "$idx" }
            SupportedLanguage.TELUGU -> when (idx) { 1 -> "ఒకటి"; 2 -> "రెండు"; 3 -> "మూడు"; else -> "$idx" }
            SupportedLanguage.URDU -> when (idx) { 1 -> "ایک"; 2 -> "دو"; 3 -> "تین"; else -> "$idx" }
            SupportedLanguage.ENGLISH -> when (idx) { 1 -> "One"; 2 -> "Two"; 3 -> "Three"; else -> "$idx" }
        }
    }

    /**
     * Formats spoken disambiguation prompt when a SINGLE contact has multiple unique phone numbers.
     * Limits options to at most 3.
     * Uses Contact phone-number LABELs (Mobile, Office, Home) rather than reading raw numbers.
     */
    fun formatMultiNumberDisambiguationPrompt(
        contactName: String,
        options: List<PhoneNumberOption>,
        lang: SupportedLanguage
    ): String {
        val limitedOptions = options.take(3)
        val count = limitedOptions.size
        val countWord = when (lang) {
            SupportedLanguage.BENGALI -> when (count) { 2 -> "দুটি"; 3 -> "তিনটি"; else -> "$count টি" }
            SupportedLanguage.HINDI -> when (count) { 2 -> "दो"; 3 -> "तीन"; else -> "$count" }
            SupportedLanguage.ASSAMESE -> when (count) { 2 -> "দুটা"; 3 -> "তিনিটা"; else -> "$count টা" }
            SupportedLanguage.MARATHI -> when (count) { 2 -> "दोन"; 3 -> "तीन"; else -> "$count" }
            SupportedLanguage.GUJARATI -> when (count) { 2 -> "બે"; 3 -> "ત્રણ"; else -> "$count" }
            SupportedLanguage.PUNJABI -> when (count) { 2 -> "ਦੋ"; 3 -> "ਤਿੰਨ"; else -> "$count" }
            SupportedLanguage.ODIA -> when (count) { 2 -> "ଦୁଇଟି"; 3 -> "ତିନୋଟି"; else -> "$count ଟି" }
            SupportedLanguage.TAMIL -> when (count) { 2 -> "இரண்டு"; 3 -> "மூன்று"; else -> "$count" }
            SupportedLanguage.TELUGU -> when (count) { 2 -> "రెండు"; 3 -> "మూడు"; else -> "$count" }
            SupportedLanguage.KANNADA -> when (count) { 2 -> "ಎರಡು"; 3 -> "ಮೂರು"; else -> "$count" }
            SupportedLanguage.MALAYALAM -> when (count) { 2 -> "രണ്ട്"; 3 -> "മൂന്ന്"; else -> "$count" }
            SupportedLanguage.URDU -> when (count) { 2 -> "دو"; 3 -> "تین"; else -> "$count" }
            SupportedLanguage.ENGLISH -> "$count"
        }

        val header = when (lang) {
            SupportedLanguage.BENGALI -> "$contactName-এর $countWord নম্বর আছে।"
            SupportedLanguage.HINDI -> "$contactName के $countWord नंबर हैं।"
            SupportedLanguage.ASSAMESE -> "$contactName-ৰ $countWord নম্বৰ আছে।"
            SupportedLanguage.GUJARATI -> "$contactName ના $countWord નંબર છે."
            SupportedLanguage.KANNADA -> "$contactName ಗೆ $countWord ಸಂಖ್ಯೆಗಳಿವೆ."
            SupportedLanguage.MALAYALAM -> "$contactName-ന് $countWord നമ്പറുകൾ ഉണ്ട്."
            SupportedLanguage.MARATHI -> "$contactName चे $countWord नंबर आहेत."
            SupportedLanguage.ODIA -> "$contactName ଙ୍କର $countWord ନମ୍ବର ଅଛି।"
            SupportedLanguage.PUNJABI -> "$contactName ਦੇ $countWord ਨੰਬਰ ਹਨ।"
            SupportedLanguage.TAMIL -> "$contactName-க்கு $countWord எண்கள் உள்ளன."
            SupportedLanguage.TELUGU -> "$contactName కి $countWord నంబర్లు ఉన్నాయి."
            SupportedLanguage.URDU -> "$contactName کے $countWord نمبر ہیں۔"
            SupportedLanguage.ENGLISH -> "$contactName has $countWord numbers."
        }

        val hasDuplicateLabels = limitedOptions.map { it.label.lowercase(Locale.ROOT) }.distinct().size < limitedOptions.size

        val optionLines = limitedOptions.mapIndexed { idx, opt ->
            val ordinal = getOrdinalWord(idx + 1, lang)
            val indicDigits = DisambiguationResolver.toIndicDigits(opt.lastFourDigits, lang)

            if (!hasDuplicateLabels) {
                when (lang) {
                    SupportedLanguage.URDU -> "$ordinal — ${opt.label}۔"
                    SupportedLanguage.ENGLISH, SupportedLanguage.GUJARATI, SupportedLanguage.KANNADA,
                    SupportedLanguage.MALAYALAM, SupportedLanguage.MARATHI, SupportedLanguage.TAMIL,
                    SupportedLanguage.TELUGU -> "$ordinal — ${opt.label}."
                    else -> "$ordinal — ${opt.label}।"
                }
            } else {
                when (lang) {
                    SupportedLanguage.BENGALI -> "$ordinal — ${opt.label}, শেষ চার সংখ্যা $indicDigits।"
                    SupportedLanguage.HINDI -> "$ordinal — ${opt.label}, अंतिम चार अंक $indicDigits।"
                    SupportedLanguage.ASSAMESE -> "$ordinal — ${opt.label}, শেষ চাৰিটা সংখ্যা $indicDigits।"
                    SupportedLanguage.GUJARATI -> "$ordinal — ${opt.label}, છેલ્લા ચાર અંક $indicDigits."
                    SupportedLanguage.KANNADA -> "$ordinal — ${opt.label}, ಕೊನೆಯ ನಾಲ್ಕು ಅಂಕಿಗಳು $indicDigits."
                    SupportedLanguage.MALAYALAM -> "$ordinal — ${opt.label}, അവസാന നാല് അക്കങ്ങൾ $indicDigits."
                    SupportedLanguage.MARATHI -> "$ordinal — ${opt.label}, शेवटचे चार अंक $indicDigits."
                    SupportedLanguage.ODIA -> "$ordinal — ${opt.label}, ଶେଷ ଚାରିଟି ଅଙ୍କ $indicDigits।"
                    SupportedLanguage.PUNJABI -> "$ordinal — ${opt.label}, ਆਖਰੀ ਚਾਰ ਅੰਕ $indicDigits।"
                    SupportedLanguage.TAMIL -> "$ordinal — ${opt.label}, கடைசி நான்கு இலக்கங்கள் $indicDigits."
                    SupportedLanguage.TELUGU -> "$ordinal — ${opt.label}, చివరి నాలుగు అంకెలు $indicDigits."
                    SupportedLanguage.URDU -> "$ordinal — ${opt.label}, آخری چار ہندسے $indicDigits۔"
                    SupportedLanguage.ENGLISH -> "$ordinal — ${opt.label}, ending in ${opt.lastFourDigits}."
                }
            }
        }.joinToString("\n")

        val question = when (lang) {
            SupportedLanguage.BENGALI -> "কোনটিতে কল করব?"
            SupportedLanguage.HINDI -> "किस पर कॉल करूँ?"
            SupportedLanguage.ASSAMESE -> "কোনটোত কল কৰিম?"
            SupportedLanguage.GUJARATI -> "કયા નંબર પર કોલ કરું?"
            SupportedLanguage.KANNADA -> "ಯಾವ ಸಂಖ್ಯೆಗೆ ಕರೆ ಮಾಡಲಿ?"
            SupportedLanguage.MALAYALAM -> "ഏതിലേക്ക് വിളിക്കണം?"
            SupportedLanguage.MARATHI -> "कोणत्या नंबरवर कॉल करू?"
            SupportedLanguage.ODIA -> "କେଉଁଥିରେ କଲ୍ କରିବି?"
            SupportedLanguage.PUNJABI -> "ਕਿਸ ਉੱਤੇ ਕਾਲ ਕਰਾਂ?"
            SupportedLanguage.TAMIL -> "எதற்கு அழைக்க வேண்டும்?"
            SupportedLanguage.TELUGU -> "దేనికి కాల్ చేయాలి?"
            SupportedLanguage.URDU -> "کس پر کال کروں؟"
            SupportedLanguage.ENGLISH -> "Which one should I call?"
        }

        return "$header\n$optionLines\n$question"
    }

    /**
     * Formats spoken disambiguation prompt when MULTIPLE DIFFERENT CONTACTS match the query.
     * Enforces a strict limit of 3 options.
     * Speaks ONLY the actual contact names — does NOT enumerate unrelated phone numbers.
     */
    fun formatMultiContactDisambiguationPrompt(
        options: List<PhoneNumberOption>,
        lang: SupportedLanguage
    ): String = formatMultiContactDisambiguationPrompt("", options, lang)

    fun formatMultiContactDisambiguationPrompt(
        targetName: String = "",
        options: List<PhoneNumberOption>,
        lang: SupportedLanguage
    ): String {
        val limitedOptions = options.take(3)
        val count = limitedOptions.size
        val cleanTarget = BengaliHindiEnglishMatcher.cleanContactQuery(targetName).ifBlank { targetName.trim() }

        val header = when (lang) {
            SupportedLanguage.BENGALI -> {
                val cWord = when (count) { 2 -> "দু"; 3 -> "তিন"; else -> "$count" }
                if (cleanTarget.isNotBlank()) "$cleanTarget নামে ${cWord}জনকে পেয়েছি।" else "একাধিক মিল পেয়েছি।"
            }
            SupportedLanguage.HINDI -> {
                val cWord = when (count) { 2 -> "दो"; 3 -> "तीन"; else -> "$count" }
                if (cleanTarget.isNotBlank()) "$cleanTarget नाम के $cWord लोग मिले हैं।" else "कई संपर्क मिले हैं।"
            }
            SupportedLanguage.ASSAMESE -> {
                val cWord = when (count) { 2 -> "দু"; 3 -> "তিনি"; else -> "$count" }
                if (cleanTarget.isNotBlank()) "$cleanTarget নামত ${cWord}জন পোৱা গৈছে।" else "একাধিক মিল পাইছোঁ।"
            }
            SupportedLanguage.GUJARATI -> {
                val cWord = when (count) { 2 -> "બે"; 3 -> "ત્રણ"; else -> "$count" }
                if (cleanTarget.isNotBlank()) "$cleanTarget નામથી $cWord સંપર્ક મળ્યા છે." else "ઘણા સંપર્ક મળ્યા છે."
            }
            SupportedLanguage.KANNADA -> {
                val cWord = when (count) { 2 -> "ಎರಡು"; 3 -> "ಮೂರು"; else -> "$count" }
                if (cleanTarget.isNotBlank()) "$cleanTarget ಹೆಸರಿನಲ್ಲಿ $cWord ಸಂಪರ್ಕಗಳು ಸಿಕ್ಕಿವೆ." else "ಬಹು ಹೊಂದಾಣಿಕೆಗಳು ಸಿಕ್ಕಿವೆ."
            }
            SupportedLanguage.MALAYALAM -> {
                val cWord = when (count) { 2 -> "രണ്ട്"; 3 -> "മൂന്ന്"; else -> "$count" }
                if (cleanTarget.isNotBlank()) "$cleanTarget പേരിൽ $cWord കോൺടാക്റ്റുകൾ കണ്ടെത്തി." else "ഒന്നിലധികം പൊരുത്തങ്ങൾ കണ്ടെത്തി."
            }
            SupportedLanguage.MARATHI -> {
                val cWord = when (count) { 2 -> "दोन"; 3 -> "तीन"; else -> "$count" }
                if (cleanTarget.isNotBlank()) "$cleanTarget नावाचे $cWord संपर्क सापडले आहेत." else "अनेक संपर्क सापडले आहेत."
            }
            SupportedLanguage.ODIA -> {
                val cWord = when (count) { 2 -> "ଦୁଇ"; 3 -> "ତିନି"; else -> "$count" }
                if (cleanTarget.isNotBlank()) "$cleanTarget ନାମରେ $cWord ଜଣ ମିଳିଲେ।" else "ଏକାଧିକ ସମ୍ପର୍କ ମିଳିଲା।"
            }
            SupportedLanguage.PUNJABI -> {
                val cWord = when (count) { 2 -> "ਦੋ"; 3 -> "ਤਿੰਨ"; else -> "$count" }
                if (cleanTarget.isNotBlank()) "$cleanTarget ਨਾਮ ਦੇ $cWord ਸੰਪਰਕ ਮਿਲੇ ਹਨ।" else "ਕਈ ਸੰਪਰਕ ਮਿਲੇ ਹਨ।"
            }
            SupportedLanguage.TAMIL -> {
                val cWord = when (count) { 2 -> "இரண்டு"; 3 -> "மூன்று"; else -> "$count" }
                if (cleanTarget.isNotBlank()) "$cleanTarget பெயரில் $cWord தொடர்புகள் கிடைத்துள்ளன." else "பல தொடர்புகள் கிடைத்துள்ளன."
            }
            SupportedLanguage.TELUGU -> {
                val cWord = when (count) { 2 -> "రెండు"; 3 -> "మూడు"; else -> "$count" }
                if (cleanTarget.isNotBlank()) "$cleanTarget పేరుతో $cWord పరిచయాలు దొరికాయి." else "బహుళ పరిచయాలు దొరికాయి."
            }
            SupportedLanguage.URDU -> {
                val cWord = when (count) { 2 -> "دو"; 3 -> "تین"; else -> "$count" }
                if (cleanTarget.isNotBlank()) "$cleanTarget نام کے $cWord رابطے ملے۔" else "متعدد رابطے ملے۔"
            }
            SupportedLanguage.ENGLISH -> {
                if (cleanTarget.isNotBlank()) "I found $count contacts for $cleanTarget." else "I found $count matching contacts."
            }
        }

        val optionLines = limitedOptions.mapIndexed { idx, opt ->
            val ordinal = getOrdinalWord(idx + 1, lang)
            when (lang) {
                SupportedLanguage.URDU -> "$ordinal — ${opt.contactName}۔"
                SupportedLanguage.ENGLISH, SupportedLanguage.GUJARATI, SupportedLanguage.KANNADA,
                SupportedLanguage.MALAYALAM, SupportedLanguage.MARATHI, SupportedLanguage.TAMIL,
                SupportedLanguage.TELUGU -> "$ordinal — ${opt.contactName}."
                else -> "$ordinal — ${opt.contactName}।"
            }
        }.joinToString("\n")

        val question = when (lang) {
            SupportedLanguage.BENGALI -> "কাকে কল করব?"
            SupportedLanguage.HINDI -> "किसे कॉल करूँ?"
            SupportedLanguage.ASSAMESE -> "কাক কল কৰিম?"
            SupportedLanguage.GUJARATI -> "કોને કોલ કરું?"
            SupportedLanguage.KANNADA -> "ಯಾರಿಗೆ ಕರೆ ಮಾಡಲಿ?"
            SupportedLanguage.MALAYALAM -> "ആരെയാണ് വിളിക്കേണ്ടത്?"
            SupportedLanguage.MARATHI -> "कोणाला कॉल करू?"
            SupportedLanguage.ODIA -> "କାହାକୁ କଲ୍ କରିବି?"
            SupportedLanguage.PUNJABI -> "ਕਿਸਨੂੰ ਕਾਲ ਕਰਾਂ?"
            SupportedLanguage.TAMIL -> "யாருக்கு அழைக்க வேண்டும்?"
            SupportedLanguage.TELUGU -> "ఎవరికి కాల్ చేయాలి?"
            SupportedLanguage.URDU -> "کس کو کال کروں؟"
            SupportedLanguage.ENGLISH -> "Who would you like to call?"
        }

        return "$header\n$optionLines\n$question"
    }

    /**
     * Active command listening prompt after wake name trigger.
     */
    fun getListeningForCommandPrompt(lang: SupportedLanguage): String {
        return when (lang) {
            SupportedLanguage.BENGALI -> "শুনছি... বলুন কাকে কল করব?"
            SupportedLanguage.HINDI -> "सुन रहा हूँ... बोलिए किसको कॉल करना है?"
            SupportedLanguage.ASSAMESE -> "শুনি আছোঁ... কাক কল কৰিব লাগিব?"
            SupportedLanguage.GUJARATI -> "સાંભળી રહ્યો છું... બોલો કોને કોલ કરવો છે?"
            SupportedLanguage.KANNADA -> "ಕೇಳಿಸಿಕೊಳ್ಳುತ್ತಿದ್ದೇನೆ... ಯಾರಿಗೆ ಕರೆ ಮಾಡಬೇಕು?"
            SupportedLanguage.MALAYALAM -> "കേൾക്കുന്നു... ആരെയാണ് വിളിക്കേണ്ടത്?"
            SupportedLanguage.MARATHI -> "ऐकत आहे... कोणाला कॉल करायचा आहे?"
            SupportedLanguage.ODIA -> "ଶୁଣୁଛି... କାହାକୁ କଲ୍ କରିବାକୁ ହେବ?"
            SupportedLanguage.PUNJABI -> "ਸੁਣ ਰਿਹਾ ਹਾਂ... ਦੱਸੋ ਕਿਸਨੂੰ ਕਾਲ ਕਰਨੀ ਹੈ?"
            SupportedLanguage.TAMIL -> "கேட்கிறேன்... யாருக்கு அழைக்க வேண்டும்?"
            SupportedLanguage.TELUGU -> "వింటున్నాను... ఎవరికి కాల్ చేయాలి?"
            SupportedLanguage.URDU -> "سن رہا ہوں... کس کو کال کرنی ہے؟"
            SupportedLanguage.ENGLISH -> "Listening for command..."
        }
    }

    /**
     * Idle prompt indicating the user should speak the wake name.
     */
    fun getWakeIdlePrompt(wakeName: String, lang: SupportedLanguage): String {
        return when (lang) {
            SupportedLanguage.BENGALI -> "জাগ্রত করতে বলুন \"$wakeName\""
            SupportedLanguage.HINDI -> "जगाने के लिए कहें \"$wakeName\""
            SupportedLanguage.ASSAMESE -> "জাগ্ৰত কৰিবলৈ কওক \"$wakeName\""
            SupportedLanguage.GUJARATI -> "જગાડવા માટે કહો \"$wakeName\""
            SupportedLanguage.KANNADA -> "ಎಚ್ಚರಿಸಲು ಹೇಳಿ \"$wakeName\""
            SupportedLanguage.MALAYALAM -> "ഉണർത്താൻ പറയുക \"$wakeName\""
            SupportedLanguage.MARATHI -> "जागे करण्यासाठी म्हणा \"$wakeName\""
            SupportedLanguage.ODIA -> "ଜାଗ୍ରତ କରିବାକୁ କୁହନ୍ତୁ \"$wakeName\""
            SupportedLanguage.PUNJABI -> "ਜਗਾਉਣ ਲਈ ਕਹੋ \"$wakeName\""
            SupportedLanguage.TAMIL -> "எழுப்ப சொல்லுங்கள் \"$wakeName\""
            SupportedLanguage.TELUGU -> "మేల్కొలపడానికి చెప్పండి \"$wakeName\""
            SupportedLanguage.URDU -> "جگانے کے لیے کہیں \"$wakeName\""
            SupportedLanguage.ENGLISH -> "Say \"$wakeName\" to wake up"
        }
    }

    /**
     * Spoken acknowledgment when wake name is detected.
     */
    fun getWakeAcknowledgementMessage(lang: SupportedLanguage): String {
        return when (lang) {
            SupportedLanguage.BENGALI -> "হ্যাঁ, বলুন?"
            SupportedLanguage.HINDI -> "हाँ, बोलिए?"
            SupportedLanguage.ASSAMESE -> "হয়, কওক?"
            SupportedLanguage.GUJARATI -> "હા, બોલો?"
            SupportedLanguage.KANNADA -> "ಹೌದು, ಹೇಳಿ?"
            SupportedLanguage.MALAYALAM -> "അതെ, പറയൂ?"
            SupportedLanguage.MARATHI -> "हो, बोला?"
            SupportedLanguage.ODIA -> "ହଁ, କୁହନ୍ତু?"
            SupportedLanguage.PUNJABI -> "ਹਾਂ ਜੀ, ਦੱਸੋ?"
            SupportedLanguage.TAMIL -> "ஆம், சொல்லுங்கள்?"
            SupportedLanguage.TELUGU -> "అవును, చెప్పండి?"
            SupportedLanguage.URDU -> "جی فرمائیں؟"
            SupportedLanguage.ENGLISH -> "Yes, how can I help?"
        }
    }
}
