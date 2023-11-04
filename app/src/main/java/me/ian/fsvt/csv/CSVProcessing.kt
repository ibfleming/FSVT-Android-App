package me.ian.fsvt.csv

import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import me.ian.fsvt.graph.MyObjects
import org.apache.commons.csv.CSVFormat
import timber.log.Timber
import java.io.BufferedWriter
import java.io.File
import java.nio.file.Files.newBufferedWriter
import java.nio.file.Paths
import java.util.Calendar
import kotlin.io.path.Path

class CSVProcessing {
    companion object {

        private lateinit var saveFolder : File
        private lateinit var fileWriter : BufferedWriter
        private var vFeet : Float = 0.0F
        private var vMeters :Float = 0.0F

        private val cal: Calendar = Calendar.getInstance()
        private val defaultFileName = "Test-File" +
                "--${cal.get(Calendar.YEAR)}" +
                "-${cal.get(Calendar.MONTH) + 1}" +
                "-${cal.get(Calendar.DAY_OF_MONTH)}" +
                "--${cal.get(Calendar.HOUR_OF_DAY)}" +
                "-${cal.get(Calendar.MINUTE)}" +
                "-${cal.get(Calendar.SECOND)}"

        fun createDirectory() : Boolean {
            val externalStorageState = Environment.getExternalStorageState()
            if( externalStorageState == Environment.MEDIA_MOUNTED ) {
                Timber.d("External storage is available")

                val saveDir = Environment.getExternalStoragePublicDirectory("Documents")
                if( !saveDir.exists() && !saveDir.mkdirs() ) {
                    Timber.e("Failed to create parent directory")
                    return false
                }

                saveFolder = File(saveDir, "StreamData")

                return if( saveFolder.exists() ) {
                    Timber.v("'StreamData' exists in the device's file system")
                    true
                } else {
                    if( saveFolder.mkdir() ) {
                        Timber.v("Successfully created the folder")
                        true
                    } else {
                        Timber.e("Failed to create the folder")
                        false
                    }
                }
            }
            else {
                Timber.e("External storage not available")
                return false
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun createFile() : Boolean {
            val file = File("$saveFolder/$defaultFileName.csv")

            Timber.v("File name: $file")
            return if( file.exists() ) {
                Timber.e("File already exists!")
                false
            } else {
                Timber.w("Creating the file: $defaultFileName")
                fileWriter = newBufferedWriter((Paths.get(file.toString())))
                fileWriter.flush()
                true
            }
        }

        fun writeTest() {
            fileWriter.write("Test #${MyObjects.testCount + 1}")
            fileWriter.newLine()
            fileWriter.write("Velocity:,$vFeet ft/s,$vMeters m/s")
            fileWriter.newLine()
            fileWriter.write("Time,Probe 1, Probe 2")
            fileWriter.newLine()

            val dataOne = MyObjects.graphOne.data
            if( dataOne != null && dataOne.dataSetCount == 1 ) {
                val dataSet = dataOne.getDataSetByIndex(0)
                for(i in 0 until dataSet.entryCount) {
                    val entry = dataSet.getEntryForIndex(i)
                    fileWriter.write("${entry.x},${entry.y}")
                    fileWriter.newLine()
                }
            }

            fileWriter.flush()
            fileWriter.close()
        }
    }
}

data class DataPoints(val time : Float, val tdm : Float)
data class DataResults(val velocity : Float)