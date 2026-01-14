package com.example.frontendproject;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SessionManagerTest {

    private SessionManager session;

    @Before
    public void setup() {
        Context ctx = ApplicationProvider.getApplicationContext();
        session = new SessionManager(ctx);
        session.logout();
    }

    @Test
    public void saveLogin_withNameEmail_storesValues() {
        session.saveLogin("token123", "Rafat", "rafat@example.com");
        assertEquals("token123", session.getToken());
        assertEquals("Rafat", session.getName());
        assertEquals("rafat@example.com", session.getEmail());
        assertTrue(session.isLoggedIn());
    }

    @Test
    public void saveLogin_withUserId_storesValues() {
        session.saveLogin("token456", "user123");
        assertEquals("token456", session.getToken());
        assertEquals("user123", session.getUserId());
        assertTrue(session.isLoggedIn());
    }

    @Test
    public void saveLogin_withEmailUserId_setsEmailToo() {
        session.saveLogin("token789", "rafat@login.com");
        assertEquals("rafat@login.com", session.getEmail());
    }

    @Test
    public void logout_clearsAllValues() {
        session.saveLogin("token123", "Rafat", "rafat@example.com");
        session.logout();
        assertNull(session.getToken());
        assertNull(session.getName());
        assertNull(session.getEmail());
        assertFalse(session.isLoggedIn());
    }

    @Test
    public void saveAndGetLastLocation_returnsCorrectValues() {
        session.saveLastLocation(42.0, -93.0);
        double[] loc = session.getLastLocation();
        assertNotNull(loc);
        assertEquals(42.0, loc[0], 0.001);
        assertEquals(-93.0, loc[1], 0.001);
    }

    @Test
    public void getLastLocation_returnsNullIfNotSet() {
        assertNull(session.getLastLocation());
    }

    @Test
    public void setEmail_updatesEmail() {
        session.setEmail("new@example.com");
        assertEquals("new@example.com", session.getEmail());
    }
}
