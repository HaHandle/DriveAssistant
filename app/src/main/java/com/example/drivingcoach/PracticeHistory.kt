package com.example.drivingcoach

import android.content.Context
import android.util.AtomicFile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal class PracticeHistoryStore(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "drivecoach-practice.json"))

    fun load(): List<PracticeRecord> = synchronized(diskLock) { readRecords() }

    private fun readRecords(): List<PracticeRecord> {
        val bytes = try {
            file.readFully()
        } catch (error: FileNotFoundException) {
            if (file.baseFile.exists()) throw error
            return emptyList()
        }
        if (bytes.size > MAX_FILE_BYTES) throw IOException("Practice history is too large")
        val json = JSONObject(bytes.toString(Charsets.UTF_8))
        if (json.getInt("version") != VERSION) throw IOException("Unsupported practice history")
        val records = json.getJSONArray("records")
        return (0 until minOf(records.length(), MAX_RECORDS)).mapNotNull { index ->
            records.optJSONObject(index)?.toPracticeRecord()
        }
    }

    fun append(record: PracticeRecord): List<PracticeRecord> = synchronized(diskLock) {
        val next = prependRecord(readRecords(), record)
        val records = JSONArray()
        next.forEach { records.put(it.toJson()) }
        val data = JSONObject().put("version", VERSION).put("records", records)
            .toString().toByteArray(Charsets.UTF_8)
        val stream = file.startWrite()
        try {
            stream.write(data)
            file.finishWrite(stream)
        } catch (error: IOException) {
            file.failWrite(stream)
            throw error
        }
        next
    }

    fun clear(): List<PracticeRecord> = synchronized(diskLock) {
        file.delete()
        if (file.baseFile.exists()) throw IOException("Could not clear practice history")
        emptyList()
    }

    private fun PracticeRecord.toJson() = JSONObject()
        .put("id", id)
        .put("time", completedAtMillis)
        .put("evidence", evidence.name)
        .put("met", met)
        .put("observed", observed)
        .put("focus", focus.name)

    private fun JSONObject.toPracticeRecord(): PracticeRecord? {
        val id = optString("id")
        val time = optLong("time", -1L)
        val evidence = PracticeEvidence.entries.firstOrNull { it.name == optString("evidence") } ?: return null
        val met = optInt("met", -1)
        val observed = optInt("observed", 0)
        val focus = PracticeFocus.entries.firstOrNull { it.name == optString("focus") } ?: return null
        if (id.length != 36 || time < 0 || observed !in 1..10_000 || met !in 0..observed) return null
        if (evidence.lesson == PracticeLesson.LaneChange && observed > 4) return null
        return PracticeRecord(id, time, evidence, met, observed, focus)
    }

    private companion object {
        val diskLock = Any()
        const val VERSION = 1
        const val MAX_FILE_BYTES = 32_768
    }
}

private const val MAX_RECORDS = 24

private fun prependRecord(records: List<PracticeRecord>, record: PracticeRecord) =
    (listOf(record) + records.filterNot { it.id == record.id }).take(MAX_RECORDS)

internal data class PracticeHistoryState(
    val records: List<PracticeRecord> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null
)

internal class PracticeHistoryController(
    private val store: PracticeHistoryStore?,
    private val scope: CoroutineScope
) {
    private val io = Mutex()
    var state by mutableStateOf(PracticeHistoryState(busy = store != null))
        private set

    suspend fun load() {
        val persistent = store ?: return
        update("저장한 기록을 읽지 못했어요.", persistent::load)
    }

    fun record(record: PracticeRecord) {
        val persistent = store
        if (persistent == null) {
            state = PracticeHistoryState(records = prependRecord(state.records, record))
        } else {
            scope.launch { update("이번 연수 기록을 저장하지 못했어요.") { persistent.append(record) } }
        }
    }

    fun clear() {
        val persistent = store
        if (persistent == null) {
            state = PracticeHistoryState()
        } else {
            scope.launch { update("기록을 지우지 못했어요.", persistent::clear) }
        }
    }

    private suspend fun update(errorMessage: String, operation: () -> List<PracticeRecord>) {
        io.withLock {
            state = state.copy(busy = true, message = null)
            try {
                val records = withContext(Dispatchers.IO) { operation() }
                state = PracticeHistoryState(records = records)
            } catch (error: Exception) {
                when (error) {
                    is IOException, is JSONException, is SecurityException -> {
                        state = state.copy(busy = false, message = errorMessage)
                    }
                    else -> throw error
                }
            }
        }
    }
}

@Composable
internal fun rememberPracticeHistory(store: PracticeHistoryStore?): PracticeHistoryController {
    val scope = rememberCoroutineScope()
    val controller = remember(store, scope) { PracticeHistoryController(store, scope) }
    LaunchedEffect(controller) { controller.load() }
    return controller
}
