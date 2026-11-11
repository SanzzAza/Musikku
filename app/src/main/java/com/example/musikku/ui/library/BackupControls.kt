package com.example.musikku.ui.library

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.musikku.AppModule
import com.example.musikku.data.local.CollectionBackup
import com.example.musikku.data.local.CollectionBackupCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BackupControls() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmImport by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null && !busy) {
            val backup = CollectionBackup(favorites = AppModule.favorites.items.value, playlists = AppModule.playlists.items.value)
            busy = true
            scope.launch {
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        val bytes = CollectionBackupCodec.encode(backup).toByteArray(Charsets.UTF_8)
                        require(bytes.size <= CollectionBackupCodec.MAX_BYTES) { "Koleksi terlalu besar untuk backup (maksimal 5 MB)." }
                        val stream = context.contentResolver.openOutputStream(uri, "wt") ?: error("File tidak bisa dibuka.")
                        stream.use { it.write(bytes) }
                    }
                }
                busy = false
                toast(if (result.isSuccess) "Backup koleksi berhasil disimpan." else "Backup gagal: ${result.exceptionOrNull()?.message ?: "coba lagi"}")
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !busy) {
            busy = true
            scope.launch {
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        val stream = context.contentResolver.openInputStream(uri) ?: error("File tidak bisa dibuka.")
                        val json = stream.use { CollectionBackupCodec.readLimited(it) }
                        CollectionBackupCodec.decode(json)
                    }
                }
                result.onSuccess { backup ->
                    AppModule.favorites.merge(backup.favorites)
                    AppModule.playlists.merge(backup.playlists)
                    toast("Koleksi berhasil digabungkan dari backup.")
                }.onFailure { toast("Impor gagal: ${it.message ?: "file tidak valid"}") }
                busy = false
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        TextButton(enabled = !busy, onClick = { export.launch("Musikku-backup.json") }) { Text("Backup koleksi") }
        TextButton(enabled = !busy, onClick = { confirmImport = true }) { Text("Impor backup") }
    }
    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text("Impor koleksi?") },
            text = { Text("Favorit dan lagu di playlist akan digabungkan dengan koleksi yang ada. Playlist dengan ID yang sama mempertahankan nama dan urutan lokal. File harus berupa backup Musikku.") },
            confirmButton = { TextButton(onClick = { confirmImport = false; importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) { Text("Pilih file") } },
            dismissButton = { TextButton(onClick = { confirmImport = false }) { Text("Batal") } },
        )
    }
}
