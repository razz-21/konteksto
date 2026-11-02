package com.example.konteksto

import org.junit.Assert.assertNotNull
import org.junit.Test

class LoginScreenTest {
    @Test
    fun testLoginScreenPackageExists() {
        val clazz = Class.forName("com.example.konteksto.ui.login.LoginScreenKt")
        assertNotNull(clazz)
    }
}
