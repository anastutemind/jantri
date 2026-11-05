package com.anaskhan.jantri.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class Dua(
    val title: String,
    val when_: String,
    val arabic: String,
    val transliteration: String,
    val translation: String,
    val note: String? = null,
)

private val duas = listOf(
    Dua(
        title = "Intention to fast (niyyah)",
        when_ = "At sehri, before the fast begins",
        arabic = "وَبِصَوْمِ غَدٍ نَوَيْتُ مِنْ شَهْرِ رَمَضَانَ",
        transliteration = "Wa bi sawmi ghadin nawaytu min shahri Ramadan",
        translation = "And I intend to keep tomorrow's fast of the month of Ramadan.",
        note = "The intention itself lives in the heart; waking for sehri with the fast in mind is enough. " +
            "This wording is the one printed on most jantri sheets as a reminder, not a hadith text.",
    ),
    Dua(
        title = "At iftar",
        when_ = "When breaking the fast at maghrib",
        arabic = "اللَّهُمَّ إِنِّي لَكَ صُمْتُ وَبِكَ آمَنْتُ وَعَلَيْكَ تَوَكَّلْتُ وَعَلَى رِزْقِكَ أَفْطَرْتُ",
        transliteration = "Allahumma inni laka sumtu wa bika amantu wa 'alayka tawakkaltu wa 'ala rizqika aftartu",
        translation = "O Allah, for You I have fasted, in You I have believed, upon You I have relied, " +
            "and with Your provision I break my fast.",
        note = "The wording most Indian, Pakistani and Arab timetables print at the iftar line.",
    ),
    Dua(
        title = "After breaking the fast",
        when_ = "After eating the first morsel or drinking water",
        arabic = "ذَهَبَ الظَّمَأُ وَابْتَلَّتِ الْعُرُوقُ وَثَبَتَ الْأَجْرُ إِنْ شَاءَ اللَّهُ",
        transliteration = "Dhahaba az-zama'u wabtallatil-'uruqu wa thabatal-ajru in sha' Allah",
        translation = "The thirst has gone, the veins are moistened, and the reward is assured, if Allah wills.",
        note = "Sunan Abi Dawud 2357. This is the supplication reported from the Prophet ﷺ after iftar.",
    ),
)

@Composable
fun DuasScreen() {
    Column(Modifier.fillMaxWidth()) {
        Text(
            "The two supplications a jantri sheet prints, plus the one reported after iftar.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        duas.forEach { dua ->
            SectionCard(title = dua.when_, subtitle = dua.title) {
                Text(
                    dua.arabic,
                    fontSize = 24.sp,
                    lineHeight = 40.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    dua.transliteration,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    dua.translation,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (dua.note != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        dua.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Times are calculated from the sun's position for your coordinates and the angles you pick. " +
                    "Where a mosque's printed sheet differs, match it with the safety margins in Timing.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}
