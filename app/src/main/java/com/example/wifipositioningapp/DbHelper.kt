package com.example.wifipositioningapp

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.widget.Toast
import kotlin.properties.Delegates

class DbHelper(private val context: Context): SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        const val DATABASE_NAME = "fingerprintdb"
        const val DATABASE_VERSION = 1

        const val REFERENCE_NAME = "ref_points"
        const val REFERENCE_ID = "rpid"
        const val REFERENCE_X = "xcoord"
        const val REFERENCE_Y = "ycoord"

        const val SCAN_NAME = "scans"
        const val SCAN_ID = "id"
        const val SCAN_RP_ID = "rpid"
        const val SCAN_ADDRESS = "address"
        const val SCAN_LEVEL = "level"

        const val CREATE_REFERENCES = "CREATE TABLE $REFERENCE_NAME ($REFERENCE_ID INTEGER PRIMARY KEY AUTOINCREMENT, $REFERENCE_X INTEGER NOT NULL, $REFERENCE_Y INTEGER NOT NULL)"
        const val CREATE_SCANS = "CREATE TABLE $SCAN_NAME ($SCAN_ID INTEGER PRIMARY KEY AUTOINCREMENT, $SCAN_RP_ID INTEGER, $SCAN_ADDRESS TEXT NOT NULL, $SCAN_LEVEL INTEGER NOT NULL);"

        const val DELETE_REFERENCES: String = "DROP TABLE IF EXISTS $REFERENCE_NAME"
        const val DELETE_SCANS: String = "DROP TABLE IF EXISTS $SCAN_NAME"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_REFERENCES.toString())
        db.execSQL(CREATE_SCANS.toString())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(DELETE_REFERENCES.toString())
        db.execSQL(DELETE_SCANS.toString())
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    fun deleteAll(db: SQLiteDatabase): Int {
        return db.delete(REFERENCE_NAME, null, null) + db.delete(SCAN_NAME, null, null)
    }

    /**
     Returns the id of the record with x = [xVal] and y = [yVal] or -1 if does not exist
     */
    fun getReferenceId(db: SQLiteDatabase, xVal: Int, yVal: Int): Int {
        // Construct query
        val projection = arrayOf(REFERENCE_ID)
        val selection = "$REFERENCE_X = ? AND $REFERENCE_Y = ?"
        val selectionArgs = arrayOf(xVal.toString(), yVal.toString())

        val cursor = db.query(
            /* table = */ REFERENCE_NAME,
            /* columns = */ projection,
            /* selection = */ selection,
            /* selectionArgs = */ selectionArgs,
            /* groupBy = */ null,
            /* having = */ null,
            /* orderBy = */ null
        )
        // Get column index of primary key or -1 if column does not exist
        var id: Int = cursor.getColumnIndex(REFERENCE_ID)
        if (cursor.moveToNext() && id != -1) {
            // If any rows are actually returned reuse id variable to get the index of the row
            id = cursor.getString(id).toInt()
        }
        cursor.close()

        return id
    }

    private fun checkScanExists(db: SQLiteDatabase, rpid: Int, address: String): Int {
        val cursor = db.query(
            /* table = */ SCAN_NAME,
            /* columns = */ arrayOf(SCAN_ID),
            /* selection = */ "$SCAN_RP_ID = ? AND $SCAN_ADDRESS = ?",
            /* selectionArgs = */ arrayOf(rpid.toString(), address),
            /* groupBy = */ null,
            /* having = */ null,
            /* orderBy = */ null
        )
        // Get column index of primary key or -1 if column does not exist
        var id: Int = cursor.getColumnIndex(SCAN_ID)
        if (cursor.moveToNext() && id != -1) {
            // If any rows are actually returned reuse id variable to get the index of the row
            id = cursor.getString(id).toInt()
        }
        cursor.close()

        return id
    }

    fun getAllReferencePoints(db: SQLiteDatabase): HashMap<Int, Pair<Int, Int>> {
        val values = HashMap<Int, Pair<Int, Int>>()

        val cursor = db.query(
            /* table = */ REFERENCE_NAME,
            /* columns = */ arrayOf(REFERENCE_ID, REFERENCE_X, REFERENCE_Y),
            /* selection = */ null,
            /* selectionArgs = */ null,
            /* groupBy = */ null,
            /* having = */ null,
            /* orderBy = */ null
        )

        val idIndex = cursor.getColumnIndex(REFERENCE_ID)
        val xIndex = cursor.getColumnIndex(REFERENCE_X)
        val yIndex = cursor.getColumnIndex(REFERENCE_Y)

        while (cursor.moveToNext()) {
            values[cursor.getInt(idIndex)] = Pair(cursor.getInt(xIndex), cursor.getInt(yIndex))
        }

        cursor.close()
        return values
    }

    fun getAllScanResults(db: SQLiteDatabase): HashMap<Int, HashMap<String, Int>> {
        // 1. Get all RPs
        // 2. For each RP, get all the scans for that RP
        // Intended output is a map of maps: {1 -> {AC:4A -> -45}, 2 -> {BA:22 -> -23}}

        val values: HashMap<Int, HashMap<String, Int>> = HashMap()

        val refCursor = db.query(
            /* table = */ REFERENCE_NAME,
            /* columns = */ arrayOf(REFERENCE_ID),
            /* selection = */ null,
            /* selectionArgs = */ null,
            /* groupBy = */ null,
            /* having = */ null,
            /* orderBy = */ null
        )

        val index = refCursor.getColumnIndex(REFERENCE_ID)

        while (refCursor.moveToNext()) {
            val refPoint = refCursor.getInt(index)

            val scanCursor = db.query(
                /* table = */ SCAN_NAME,
                /* columns = */ arrayOf(SCAN_ADDRESS, SCAN_LEVEL),
                /* selection = */ "$SCAN_RP_ID = ?",
                /* selectionArgs = */ arrayOf(refPoint.toString()),
                /* groupBy = */ null,
                /* having = */ null,
                /* orderBy = */ null
            )

            val temp = HashMap<String, Int>()
            val addressIndex = scanCursor.getColumnIndex(SCAN_ADDRESS)
            val levelIndex = scanCursor.getColumnIndex(SCAN_LEVEL)

            while (scanCursor.moveToNext()) {
                temp[scanCursor.getString(addressIndex)] = scanCursor.getInt(levelIndex)
            }
            scanCursor.close()

            values[refPoint] = temp
        }

        refCursor.close()

        return values
    }

    fun addNewReference(db: SQLiteDatabase, xVal: Int, yVal: Int): Int {
        val id = getReferenceId(db, xVal, yVal)

        // If id > 0 then the record already exists
        if (id > 0) {
            return id
        }

        // Else insert data to db and return new record id
        val values = ContentValues().apply {
            put(REFERENCE_X, xVal)
            put(REFERENCE_Y, yVal)
        }

        return db.insert(
            /* table = */ REFERENCE_NAME,
            /* nullColumnHack = */null,
            /* values = */values
        ).toInt()
    }

    private fun addNewScan(db: SQLiteDatabase, rpid: Int, address: String, level: Int): Int {
        val values = ContentValues().apply {
            put(SCAN_RP_ID, rpid)
            put(SCAN_ADDRESS, address)
            put(SCAN_LEVEL, level)
        }

        return db.insert(
            /* table = */ SCAN_NAME,
            /* nullColumnHack = */ null,
            /* values = */ values
        ).toInt()
    }

    private fun updateScan(db: SQLiteDatabase, scan_id: Int, new_level: Int) {
        val values = ContentValues().apply {
            put(SCAN_LEVEL, new_level)
        }

        db.update(
            /* table = */ SCAN_NAME,
            /* values = */ values,
            /* whereClause = */ "$SCAN_ID = ?",
            /* whereArgs = */ arrayOf(scan_id.toString())
        )
    }

    fun updateOrAddNewScan(db: SQLiteDatabase, x: Int, y: Int, address: String, level: Int): Int {
        val rpid = getReferenceId(db, x, y)
        if (rpid < 1) {
            // Only happens if reference point does not exist yet
            // !!Should only return -1!!
            return rpid
        }

        // ID of scan in database, -1 if not exists
        val scan_id = checkScanExists(db, rpid, address)
        return if (scan_id < 1) {
            // Scan does not exist.
            addNewScan(db, rpid, address, level)
        } else {
            // Scan exists; update.
            updateScan(db, scan_id, level)
            return scan_id
        }
    }
}