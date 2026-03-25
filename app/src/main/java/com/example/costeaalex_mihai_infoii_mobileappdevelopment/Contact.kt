package com.example.costeaalex_mihai_infoii_mobileappdevelopment

data class Contact(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val phone: String
)