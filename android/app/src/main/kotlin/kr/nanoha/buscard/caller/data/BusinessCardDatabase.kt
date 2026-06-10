package kr.nanoha.buscard.caller.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class BusinessCardDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE cards (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL DEFAULT '',
                company TEXT NOT NULL DEFAULT '',
                department TEXT NOT NULL DEFAULT '',
                position TEXT NOT NULL DEFAULT '',
                email TEXT NOT NULL DEFAULT '',
                mobile TEXT NOT NULL DEFAULT '',
                phone TEXT NOT NULL DEFAULT '',
                address TEXT NOT NULL DEFAULT '',
                website TEXT NOT NULL DEFAULT '',
                tags TEXT NOT NULL DEFAULT '',
                memo_preview TEXT NOT NULL DEFAULT '',
                overlay_image_kind TEXT NOT NULL DEFAULT 'generated',
                updated_at TEXT NOT NULL DEFAULT '',
                deleted INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE phone_numbers (
                normalized_number TEXT NOT NULL,
                card_id TEXT NOT NULL,
                kind TEXT NOT NULL,
                display_number TEXT NOT NULL DEFAULT '',
                updated_at TEXT NOT NULL DEFAULT '',
                PRIMARY KEY(normalized_number, card_id, kind)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_phone_numbers_number ON phone_numbers(normalized_number)")
        db.execSQL("CREATE INDEX idx_phone_numbers_card ON phone_numbers(card_id)")
        db.execSQL(
            """
            CREATE TABLE overlay_images (
                card_id TEXT NOT NULL,
                image_kind TEXT NOT NULL,
                image_hash TEXT NOT NULL DEFAULT '',
                local_path TEXT NOT NULL DEFAULT '',
                mime TEXT NOT NULL DEFAULT '',
                updated_at TEXT NOT NULL DEFAULT '',
                PRIMARY KEY(card_id, image_kind)
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS overlay_images")
        db.execSQL("DROP TABLE IF EXISTS phone_numbers")
        db.execSQL("DROP TABLE IF EXISTS cards")
        onCreate(db)
    }

    fun replaceCard(card: SyncCard) {
        writableDatabase.beginTransaction()
        try {
            writableDatabase.delete("phone_numbers", "card_id = ?", arrayOf(card.id))
            val values = ContentValues().apply {
                put("id", card.id)
                put("updated_at", card.updatedAt)
                put("deleted", if (card.deleted) 1 else 0)
                if (!card.deleted) {
                    put("name", card.name)
                    put("company", card.company)
                    put("department", card.department)
                    put("position", card.position)
                    put("email", card.email)
                    put("mobile", card.mobile)
                    put("phone", card.phone)
                    put("address", card.address)
                    put("website", card.website)
                    put("tags", card.tags)
                    put("memo_preview", card.memoPreview)
                    put("overlay_image_kind", card.overlayImageKind.ifBlank { "generated" })
                }
            }
            writableDatabase.insertWithOnConflict("cards", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            if (!card.deleted) {
                card.phoneNumbers.forEach { phone ->
                    phone.aliases.forEach { alias ->
                        val phoneValues = ContentValues().apply {
                            put("normalized_number", alias)
                            put("card_id", card.id)
                            put("kind", phone.kind)
                            put("display_number", phone.displayNumber)
                            put("updated_at", card.updatedAt)
                        }
                        writableDatabase.insertWithOnConflict("phone_numbers", null, phoneValues, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                }
            }
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
    }

    fun overlayImageHash(cardId: String, kind: String): String {
        readableDatabase.rawQuery(
            "SELECT image_hash FROM overlay_images WHERE card_id = ? AND image_kind = ? LIMIT 1",
            arrayOf(cardId, kind),
        ).use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0).orEmpty()
        }
        return ""
    }

    fun upsertOverlayImage(cardId: String, kind: String, imageHash: String, path: String, mime: String, updatedAt: String) {
        val values = ContentValues().apply {
            put("card_id", cardId)
            put("image_kind", kind)
            put("image_hash", imageHash)
            put("local_path", path)
            put("mime", mime)
            put("updated_at", updatedAt)
        }
        writableDatabase.insertWithOnConflict("overlay_images", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteOverlayImage(cardId: String, kind: String) {
        writableDatabase.delete("overlay_images", "card_id = ? AND image_kind = ?", arrayOf(cardId, kind))
    }

    fun findByPhoneAliases(aliases: Set<String>): CachedBusinessCard? {
        if (aliases.isEmpty()) return null
        val placeholders = aliases.joinToString(",") { "?" }
        val sql = """
            SELECT c.*, i.local_path AS image_path
            FROM phone_numbers p
            JOIN cards c ON c.id = p.card_id
            LEFT JOIN overlay_images i ON i.card_id = c.id AND i.image_kind = c.overlay_image_kind
            WHERE p.normalized_number IN ($placeholders) AND c.deleted = 0
            ORDER BY c.updated_at DESC
            LIMIT 1
        """.trimIndent()
        readableDatabase.rawQuery(sql, aliases.toTypedArray()).use { cursor ->
            if (cursor.moveToFirst()) return cursor.toCachedCard()
        }
        return null
    }

    fun getCard(cardId: String): CachedBusinessCard? {
        readableDatabase.rawQuery(
            """
            SELECT c.*, i.local_path AS image_path
            FROM cards c
            LEFT JOIN overlay_images i ON i.card_id = c.id AND i.image_kind = c.overlay_image_kind
            WHERE c.id = ? AND c.deleted = 0
            LIMIT 1
            """.trimIndent(),
            arrayOf(cardId),
        ).use { cursor ->
            if (cursor.moveToFirst()) return cursor.toCachedCard()
        }
        return null
    }

    fun firstCard(): CachedBusinessCard? {
        readableDatabase.rawQuery(
            """
            SELECT c.*, i.local_path AS image_path
            FROM cards c
            LEFT JOIN overlay_images i ON i.card_id = c.id AND i.image_kind = c.overlay_image_kind
            WHERE c.deleted = 0
            ORDER BY c.updated_at DESC
            LIMIT 1
            """.trimIndent(),
            emptyArray(),
        ).use { cursor ->
            if (cursor.moveToFirst()) return cursor.toCachedCard()
        }
        return null
    }

    fun activeCardCount(): Int {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM cards WHERE deleted = 0", emptyArray()).use { cursor ->
            if (cursor.moveToFirst()) return cursor.getInt(0)
        }
        return 0
    }

    private fun Cursor.text(name: String): String = getString(getColumnIndexOrThrow(name)).orEmpty()

    private fun Cursor.toCachedCard(): CachedBusinessCard {
        return CachedBusinessCard(
            id = text("id"),
            name = text("name"),
            company = text("company"),
            department = text("department"),
            position = text("position"),
            email = text("email"),
            mobile = text("mobile"),
            phone = text("phone"),
            address = text("address"),
            website = text("website"),
            tags = text("tags"),
            memoPreview = text("memo_preview"),
            overlayImageKind = text("overlay_image_kind").ifBlank { "generated" },
            imagePath = runCatching { text("image_path") }.getOrNull()?.ifBlank { null },
        )
    }

    companion object {
        private const val DB_NAME = "business-card-caller.db"
        private const val DB_VERSION = 1
    }
}
