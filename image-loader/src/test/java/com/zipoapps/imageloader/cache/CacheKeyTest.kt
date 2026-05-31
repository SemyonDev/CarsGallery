package com.zipoapps.imageloader.cache

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CacheKeyTest {

    // ── fromUrl ──────────────────────────────────────────────────────────────

    @Test
    fun `same URL produces same key`() {
        val url = "https://example.com/image.jpg"
        assertEquals(CacheKey.fromUrl(url), CacheKey.fromUrl(url))
    }

    @Test
    fun `different URLs produce different keys`() {
        val key1 = CacheKey.fromUrl("https://example.com/a.jpg")
        val key2 = CacheKey.fromUrl("https://example.com/b.jpg")
        assertNotEquals(key1, key2)
    }

    @Test
    fun `key is lowercase hex string of length 32`() {
        val key = CacheKey.fromUrl("https://example.com/test.jpg")
        assertTrue("Key should be 32 hex chars", key.matches(Regex("[0-9a-f]{32}")))
    }

    @Test
    fun `key contains only filesystem-safe characters`() {
        val key = CacheKey.fromUrl("https://example.com/path?query=value&other=true#fragment")
        assertTrue("Key must be filesystem-safe", key.matches(Regex("[a-z0-9]+")))
    }

    @Test
    fun `empty string produces a consistent key`() {
        assertEquals(CacheKey.fromUrl(""), CacheKey.fromUrl(""))
    }

    // ── fromRequest ──────────────────────────────────────────────────────────

    @Test
    fun `fromRequest zero dimensions equals fromUrl`() {
        val url = "https://example.com/image.jpg"
        assertEquals(
            "Zero dimensions should produce the same key as fromUrl",
            CacheKey.fromUrl(url),
            CacheKey.fromRequest(url, 0, 0),
        )
    }

    @Test
    fun `fromRequest positive dimensions produces different key than fromUrl`() {
        val url = "https://example.com/image.jpg"
        assertNotEquals(
            "Size-specific key must differ from the no-size key",
            CacheKey.fromUrl(url),
            CacheKey.fromRequest(url, 100, 100),
        )
    }

    @Test
    fun `fromRequest same url different sizes produce different keys`() {
        val url = "https://example.com/image.jpg"
        assertNotEquals(
            CacheKey.fromRequest(url, 100, 100),
            CacheKey.fromRequest(url, 200, 200),
        )
    }

    @Test
    fun `fromRequest same url same size is deterministic`() {
        val url = "https://example.com/image.jpg"
        assertEquals(
            CacheKey.fromRequest(url, 300, 400),
            CacheKey.fromRequest(url, 300, 400),
        )
    }

    @Test
    fun `fromRequest different urls same size produce different keys`() {
        assertNotEquals(
            CacheKey.fromRequest("https://example.com/a.jpg", 100, 100),
            CacheKey.fromRequest("https://example.com/b.jpg", 100, 100),
        )
    }
}
