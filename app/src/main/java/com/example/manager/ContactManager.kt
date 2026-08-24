package com.example.manager

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import android.util.Log
import com.example.model.Contact
import com.example.model.ContactMatchResult
import com.example.model.PhoneNumberOption

class ContactManager(private val context: Context) {

    companion object {
        private const val TAG = "ContactManager"
        private const val MATCH_THRESHOLD = 0.70
    }

    /**
     * Reads all real contacts from the device's ContactsContract database.
     */
    fun getAllContacts(): List<Contact> {
        val contactsMap = mutableMapOf<String, Triple<String, MutableList<String>, MutableList<PhoneNumberOption>>>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL
        )

        try {
            val cursor: Cursor? = context.contentResolver.query(
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
                        val entry = contactsMap.getOrPut(id) { Triple(name, mutableListOf(), mutableListOf()) }
                        if (!entry.second.contains(cleanNumber)) {
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
     * Searches for contacts matching the given name query using Bengali, Hindi, and English matching.
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
     * Returns:
     * - SingleMatch if exactly one contact matches and has 1 number
     * - DisambiguationRequired if contact has multiple phone numbers
     * - MultipleMatches if multiple contacts match with close scores
     * - NoMatch if no contact reaches threshold
     */
    fun findBestContactMatch(name: String): ContactMatchResult {
        val cleanQuery = BengaliHindiEnglishMatcher.cleanContactQuery(name)
        if (cleanQuery.isBlank()) return ContactMatchResult.NoMatch

        val allContacts: List<Contact> = getAllContacts()
        if (allContacts.isEmpty()) return ContactMatchResult.NoMatch

        val scoredList: List<Pair<Contact, Double>> = allContacts.map { contact: Contact ->
            val score: Double = BengaliHindiEnglishMatcher.computeMatchScore(cleanQuery, contact.name)
            Pair(contact, score)
        }.filter { it.second >= MATCH_THRESHOLD }
            .sortedByDescending { it.second }

        if (scoredList.isEmpty()) {
            return ContactMatchResult.NoMatch
        }

        val topScore: Double = scoredList.first().second

        // Check if there are exact or near-identical top scores
        val topMatches: List<Pair<Contact, Double>> = scoredList.filter {
            it.second >= (topScore - 0.05) && it.second >= MATCH_THRESHOLD
        }

        return if (topMatches.size == 1) {
            val contact: Contact = topMatches.first().first
            if (contact.labeledPhoneNumbers.size > 1) {
                ContactMatchResult.DisambiguationRequired(contact.name, contact.labeledPhoneNumbers)
            } else {
                val phone: String = contact.primaryPhoneNumber
                ContactMatchResult.SingleMatch(contact, phone)
            }
        } else {
            // Multiple contacts found - also create options for them
            ContactMatchResult.MultipleMatches(topMatches.map { it.first })
        }
    }

    /**
     * Gets all phone numbers associated with a contact.
     */
    fun getPhoneNumbers(contact: Contact): List<String> {
        return contact.phoneNumbers
    }
}
