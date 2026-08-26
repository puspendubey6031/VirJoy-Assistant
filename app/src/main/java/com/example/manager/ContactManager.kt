package com.example.manager

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import android.util.Log
import com.example.model.Contact
import com.example.model.ContactMatchResult
import com.example.model.PhoneNumberOption

class ContactManager(private val context: Context? = null) {

    companion object {
        private const val TAG = "ContactManager"
        private const val MATCH_THRESHOLD = 0.70

        /**
         * Normalizes a phone number to a canonical digit representation for accurate deduplication.
         * Strips non-digits and removes international prefix (e.g. +91, 91, leading 0) for 10-digit Indian numbers.
         */
        fun normalizePhoneNumber(rawNumber: String): String {
            val digitsOnly = rawNumber.replace(Regex("[^0-9]"), "")
            return when {
                digitsOnly.length == 12 && digitsOnly.startsWith("91") -> digitsOnly.substring(2)
                digitsOnly.length == 11 && digitsOnly.startsWith("0") -> digitsOnly.substring(1)
                digitsOnly.length >= 10 -> digitsOnly.takeLast(10)
                else -> digitsOnly
            }
        }

        /**
         * Masks phone number for safe privacy-compliant logging (e.g. "***3210").
         */
        fun maskPhoneNumber(number: String): String {
            val clean = number.replace(Regex("[^0-9+]"), "")
            return if (clean.length >= 4) {
                "***" + clean.takeLast(4)
            } else {
                "***"
            }
        }
    }

    /**
     * Reads all real contacts from the device's ContactsContract database.
     * Deduplicates identical numbers within each contact.
     */
    fun getAllContacts(): List<Contact> {
        val contactsMap = mutableMapOf<String, Triple<String, MutableList<String>, MutableList<PhoneNumberOption>>>()
        val seenCanonicalPerContact = mutableMapOf<String, MutableSet<String>>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL
        )

        try {
            val cursor: Cursor? = context?.contentResolver?.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val typeIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                val labelIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)

                while (it.moveToNext()) {
                    val id = if (idIndex >= 0) it.getString(idIndex) ?: "" else ""
                    val name = if (nameIndex >= 0) it.getString(nameIndex) ?: "" else ""
                    val number = if (numberIndex >= 0) it.getString(numberIndex) ?: "" else ""
                    val phoneType = if (typeIndex >= 0) it.getInt(typeIndex) else ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    val customLabel = if (labelIndex >= 0) it.getString(labelIndex) else null

                    if (id.isNotEmpty() && name.isNotEmpty() && number.isNotEmpty()) {
                        val cleanNumber = number.replace(Regex("[^0-9+]"), "")
                        val canonical = normalizePhoneNumber(cleanNumber)

                        val seenSet = seenCanonicalPerContact.getOrPut(id) { mutableSetOf() }
                        if (canonical.isNotEmpty() && !seenSet.contains(canonical)) {
                            seenSet.add(canonical)
                            val entry = contactsMap.getOrPut(id) { Triple(name, mutableListOf(), mutableListOf()) }
                            entry.second.add(cleanNumber)

                            val typeLabel = try {
                                ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                                    context.resources,
                                    phoneType,
                                    customLabel
                                ).toString()
                            } catch (e: Exception) {
                                when (phoneType) {
                                    ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Office"
                                    ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
                                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobile"
                                    else -> "Phone"
                                }
                            }

                            val cleanLabel = if (typeLabel.isNotBlank()) typeLabel else "Mobile"
                            val last4 = if (cleanNumber.length >= 4) cleanNumber.takeLast(4) else cleanNumber
                            val option = PhoneNumberOption(
                                number = cleanNumber,
                                label = cleanLabel,
                                lastFourDigits = last4,
                                optionIndex = entry.third.size + 1,
                                contactName = name
                            )
                            entry.third.add(option)
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied while accessing contacts", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error querying contacts", e)
        }

        return contactsMap.map { (id, triple) ->
            Contact(
                id = id,
                name = triple.first,
                phoneNumbers = triple.second,
                labeledPhoneNumbers = triple.third
            )
        }
    }

    /**
     * Searches for contacts matching the given name query using phonetic matching.
     */
    fun searchContacts(name: String): List<Contact> {
        val cleanQuery = BengaliHindiEnglishMatcher.cleanContactQuery(name)
        if (cleanQuery.isBlank()) return emptyList()

        val allContacts: List<Contact> = getAllContacts()
        val scoredList: List<Pair<Contact, Double>> = allContacts.mapNotNull { contact ->
            val score: Double = BengaliHindiEnglishMatcher.computeMatchScore(cleanQuery, contact.name)
            if (score >= MATCH_THRESHOLD) {
                Pair(contact, score)
            } else {
                null
            }
        }

        return scoredList.sortedByDescending { it.second }.map { it.first }
    }

    /**
     * Finds the best contact match for a given name query.
     * Evaluates whether a single contact was matched or multiple distinct contacts were found.
     * Logs structured resolution details for debugging on real devices.
     */
    fun findBestContactMatch(name: String, rawCommand: String = ""): ContactMatchResult {
        return findBestContactMatch(name, getAllContacts(), rawCommand)
    }

    /**
     * Resolves contact matching against a provided list of contacts.
     */
    fun findBestContactMatch(
        name: String,
        allContacts: List<Contact>,
        rawCommand: String = ""
    ): ContactMatchResult {
        val cleanQuery = BengaliHindiEnglishMatcher.cleanContactQuery(name)
        if (cleanQuery.isBlank() || allContacts.isEmpty()) {
            Log.d(TAG, "[ContactResolver] Command='$rawCommand', Query='$cleanQuery', Result=NoMatch (empty query or contacts)")
            return ContactMatchResult.NoMatch
        }

        val scoredList: List<Pair<Contact, Double>> = allContacts.map { contact: Contact ->
            val score: Double = BengaliHindiEnglishMatcher.computeMatchScore(cleanQuery, contact.name)
            Pair(contact, score)
        }.filter { it.second >= MATCH_THRESHOLD }
            .sortedByDescending { it.second }

        if (scoredList.isEmpty()) {
            Log.d(TAG, "[ContactResolver] Command='$rawCommand', Query='$cleanQuery', Result=NoMatch (no matches above $MATCH_THRESHOLD)")
            return ContactMatchResult.NoMatch
        }

        val topScore: Double = scoredList.first().second
        val topCandidate: Contact = scoredList.first().first

        // Determine if top candidate is an unambiguous single winner:
        // 1) Only one contact reached threshold
        // 2) Top candidate is an exact match (score >= 0.98) and clearly better than #2
        // 3) Significant score gap (>= 0.12) over the second candidate
        val isClearSingleWinner = scoredList.size == 1 ||
                (topScore >= 0.98 && (scoredList.size == 1 || scoredList[1].second < 0.98)) ||
                (topScore - (scoredList.getOrNull(1)?.second ?: 0.0) >= 0.12)

        if (isClearSingleWinner) {
            val contact = topCandidate
            val dedupedNumbers = contact.labeledPhoneNumbers
            val maskedNumbers = dedupedNumbers.map { "${it.label}: ***${it.lastFourDigits}" }

            return if (dedupedNumbers.size <= 1) {
                val phone = contact.primaryPhoneNumber
                Log.d(
                    TAG,
                    "[ContactResolver] SINGLE MATCH -> Contact='${contact.name}' (ID=${contact.id}), PhoneCount=${dedupedNumbers.size}, Phone=${maskPhoneNumber(phone)}, Disambiguation=NO"
                )
                ContactMatchResult.SingleMatch(contact, phone)
            } else {
                val limitedOptions = dedupedNumbers.take(3)
                Log.d(
                    TAG,
                    "[ContactResolver] MULTI-NUMBER FOR SINGLE CONTACT -> Contact='${contact.name}' (ID=${contact.id}), UniqueNumbers=${dedupedNumbers.size}, Options=${maskedNumbers.take(3)}, Disambiguation=YES"
                )
                ContactMatchResult.DisambiguationRequired(contact.name, limitedOptions)
            }
        }

        // Multiple distinct contacts with close top scores
        val tiedMatches = scoredList.filter { it.second >= (topScore - 0.05) && it.second >= MATCH_THRESHOLD }
            .map { it.first }
            .distinctBy { it.id }
            .take(3)

        if (tiedMatches.size == 1) {
            val singleContact = tiedMatches.first()
            return if (singleContact.labeledPhoneNumbers.size <= 1) {
                ContactMatchResult.SingleMatch(singleContact, singleContact.primaryPhoneNumber)
            } else {
                ContactMatchResult.DisambiguationRequired(singleContact.name, singleContact.labeledPhoneNumbers.take(3))
            }
        }

        val contactsSummary = tiedMatches.map { c ->
            "${c.name} (ID=${c.id}, PhoneCount=${c.phoneNumbers.size}, Last4=***${c.primaryPhoneNumber.takeLast(4)})"
        }
        Log.d(
            TAG,
            "[ContactResolver] MULTIPLE DISTINCT CONTACTS -> Query='$cleanQuery', MatchesCount=${tiedMatches.size}, Contacts=$contactsSummary, Disambiguation=YES"
        )
        return ContactMatchResult.MultipleMatches(tiedMatches)
    }

    /**
     * Gets all phone numbers associated with a contact.
     */
    fun getPhoneNumbers(contact: Contact): List<String> {
        return contact.phoneNumbers
    }
}
