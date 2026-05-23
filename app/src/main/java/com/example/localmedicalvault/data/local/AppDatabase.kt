package com.example.localmedicalvault.data.local

import android.content.Context
import androidx.room.*

class DbConverters { @TypeConverter fun fromCategory(c: DocumentCategory)=c.name; @TypeConverter fun toCategory(v:String)=DocumentCategory.valueOf(v) }

@Database(entities = [PatientEntity::class, MedicalDocumentEntity::class, VisitEntity::class, VisitDocumentCrossRef::class], version = 1)
@TypeConverters(DbConverters::class)
abstract class AppDatabase: RoomDatabase() {
 abstract fun patientDao(): PatientDao; abstract fun documentDao(): DocumentDao; abstract fun visitDao(): VisitDao; abstract fun visitDocDao(): VisitDocDao
 companion object { @Volatile private var INSTANCE: AppDatabase? = null; fun get(context: Context) = INSTANCE ?: synchronized(this){ INSTANCE ?: Room.databaseBuilder(context, AppDatabase::class.java, "medical_vault.db").build().also { INSTANCE = it } } }
}
