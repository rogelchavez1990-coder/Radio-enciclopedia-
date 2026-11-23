package cu.radioenciclopedia.app

import org.junit.Assert.*
import org.junit.Test

class LinkLauncherTest {
    @Test fun youtubeOpensDirectlyWhenTheApplicationHandlesIt() {
        val attempts = ArrayList<LinkRequest>()
        assertTrue(LinkLauncher.open(Links.VIDEOS) { attempts.add(it); true })
        assertEquals(1, attempts.size)
        assertEquals("com.google.android.youtube", attempts.single().packageName)
        assertEquals(Links.VIDEOS.url, attempts.single().uri)
    }

    @Test fun unavailableApplicationsFallBackToTheOriginalWebsite() {
        for (link in Links.ALL) {
            val attempts = ArrayList<LinkRequest>()
            assertTrue(LinkLauncher.open(link) { attempts.add(it); it.packageName == null && it.uri == link.url })
            assertEquals(LinkRequest(link.url), attempts.last())
        }
    }

    @Test fun nativeProfileAndLiteFallbackStayOnTheOfficialInstagramAccount() {
        val attempts = ArrayList<LinkRequest>()
        assertTrue(LinkLauncher.open(Links.INSTAGRAM) {
            attempts.add(it)
            it.packageName == "com.instagram.lite" && it.uri.startsWith("https:")
        })
        assertEquals("instagram://user?username=radio_enciclopedia", attempts.first().uri)
        assertTrue(attempts.none { it.packageName == null })
        assertEquals(Links.INSTAGRAM.url, attempts.last().uri)
    }

    @Test fun aMissingHandlerIsReportedInsteadOfAssumingALaunchSucceeded() {
        assertFalse(LinkLauncher.open(Links.TELEGRAM) { false })
    }

    @Test fun facebookStaysInTheInstalledApplicationAndUsesTheCorrectPage() {
        val attempts = ArrayList<LinkRequest>()
        assertTrue(LinkLauncher.open(Links.FACEBOOK) { attempts.add(it); true })
        assertEquals(listOf(LinkRequest(Links.FACEBOOK.url, "com.facebook.katana")), attempts)
    }

    @Test fun facebookLiteIsTriedBeforeAnyBrowser() {
        val attempts = ArrayList<LinkRequest>()
        assertTrue(LinkLauncher.open(Links.FACEBOOK) {
            attempts.add(it)
            it.packageName == "com.facebook.lite" && it.uri == Links.FACEBOOK.url
        })
        assertTrue(attempts.all { it.packageName != null })
        assertEquals("com.facebook.lite", attempts.last().packageName)
    }

    @Test fun telegramAlternativeClientGetsTheChannelBeforeTheWebFallback() {
        val attempts = ArrayList<LinkRequest>()
        assertTrue(LinkLauncher.open(Links.TELEGRAM) {
            attempts.add(it)
            it.packageName == null && it.uri.startsWith("tg:")
        })
        assertEquals("tg://resolve?domain=radio_enciclopedia7_cu", attempts.first().uri)
        assertEquals("org.telegram.messenger", attempts.first().packageName)
        assertEquals("tg://resolve?domain=radio_enciclopedia7_cu", attempts.last().uri)
        assertTrue(attempts.none { it.packageName == null && it.uri.startsWith("https:") })
    }
}
