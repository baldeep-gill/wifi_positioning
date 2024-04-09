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

    fun getAllScanResults(db: SQLiteDatabase): HashMap<String, Int> {
        TODO("Not implemented yet!")
        // 1. Get all RPs
        // 2. For each RP, get all the scans for that RP
        // {1 -> {AC:4A -> -45}, 2 -> {BA:22 -> -23}}
        /**
        val projection = arrayOf(SCAN_ADDRESS, SCAN_LEVEL)
        val selection = "$SCAN_RP_ID = ?"
        val selectionArgs = arrayOf(rpid.toString())

        val cursor = db.query(
            SCAN_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        val map: HashMap<String, Int> = HashMap()
        val addressIndex = cursor.getColumnIndex(SCAN_ADDRESS)
        val levelIndex = cursor.getColumnIndex(SCAN_LEVEL)

        while(cursor.moveToNext()) {
            map.put(cursor.getString(addressIndex), cursor.getInt(levelIndex))
        }

        cursor.close()
        return map
        */
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