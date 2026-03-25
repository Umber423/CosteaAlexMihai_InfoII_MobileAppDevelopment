package com.example.costeaalex_mihai_infoii_mobileappdevelopment

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ContactsManager {
    private const val PREFS_NAME = "contacts_prefs"
    private const val KEY_CONTACTS = "contacts"

    fun getContacts(context: Context): List<Contact> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_CONTACTS, "[]") ?: "[]"
        val array = JSONArray(json)
        val contacts = mutableListOf<Contact>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            contacts.add(
                Contact(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    phone = obj.getString("phone")
                )
            )
        }
        return contacts
    }

    fun saveContacts(context: Context, contacts: List<Contact>) {
        val array = JSONArray()
        contacts.forEach { contact ->
            val obj = JSONObject()
            obj.put("id", contact.id)
            obj.put("name", contact.name)
            obj.put("phone", contact.phone)
            array.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CONTACTS, array.toString())
            .apply()
    }

    fun addContact(context: Context, contact: Contact) {
        val contacts = getContacts(context).toMutableList()
        contacts.add(contact)
        saveContacts(context, contacts)
    }

    fun removeContact(context: Context, id: String) {
        val contacts = getContacts(context).filter { it.id != id }
        saveContacts(context, contacts)
    }

    fun getPrimaryContact(context: Context): Contact? {
        return getContacts(context).firstOrNull()
    }
}