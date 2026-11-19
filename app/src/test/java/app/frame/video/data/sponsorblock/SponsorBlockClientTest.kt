package app.frame.video.data.sponsorblock

import org.junit.Assert.assertEquals
import org.junit.Test

class SponsorBlockClientTest {
    @Test
    fun parsesOnlySelectedSkipSegmentsAndSortsByStart() {
        val json = """
            [
              {"actionType":"skip","category":"intro","segment":[12.5,20.0]},
              {"actionType":"skip","category":"sponsor","segment":[3.0,10.25]},
              {"actionType":"mute","category":"sponsor","segment":[21.0,24.0]},
              {"actionType":"skip","category":"selfpromo","segment":[30.0,35.0]},
              {"actionType":"skip","category":"sponsor","segment":[8.0,7.0]}
            ]
        """.trimIndent()

        val result = SponsorBlockClient.parseSegments(json, setOf("intro", "sponsor"))

        assertEquals(
            listOf(
                SponsorSegment(3_000, 10_250, "sponsor"),
                SponsorSegment(12_500, 20_000, "intro")
            ),
            result
        )
    }

    @Test
    fun emptySelectionReturnsNoSegments() {
        assertEquals(emptyList<SponsorSegment>(), SponsorBlockClient.parseSegments("[]", emptySet()))
    }
}
