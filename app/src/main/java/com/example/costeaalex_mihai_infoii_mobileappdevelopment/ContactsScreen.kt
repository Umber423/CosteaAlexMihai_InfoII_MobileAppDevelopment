package com.example.costeaalex_mihai_infoii_mobileappdevelopment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ContactsScreen(modifier: Modifier = Modifier, onBack: () -> Unit = {}) { // ← added onBack
    val context = LocalContext.current

    var contacts by remember { mutableStateOf(ContactsManager.getContacts(context)) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
    ) {
        FilledTonalButton( // ← back button at the top
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text("← Back")
        }

        Text(
            "Emergency Contacts", fontSize = 22.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone number") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        FilledTonalButton(
            onClick = {
                if (name.isNotBlank() && phone.isNotBlank()) {
                    ContactsManager.addContact(context, Contact(name = name, phone = phone))
                    contacts = ContactsManager.getContacts(context)
                    name = ""
                    phone = ""
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("Add Contact")
        }

        Text(
            "Saved Contacts",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
        )

        if (contacts.isEmpty()) {
            Text("No contacts saved yet.", color = Color.Gray)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(contacts, key = { it.id }) { contact ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(contact.name, fontWeight = FontWeight.Bold)
                            Text(contact.phone, color = Color.Gray, fontSize = 13.sp)
                        }
                        IconButton(onClick = {
                            ContactsManager.removeContact(context, contact.id)
                            contacts = ContactsManager.getContacts(context)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }
}