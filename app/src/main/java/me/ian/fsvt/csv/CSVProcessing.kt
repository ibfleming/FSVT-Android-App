package me.ian.fsvt.csv

import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import me.ian.fsvt.AppGlobals
import me.ian.fsvt.UnitType
import timber.log.Timber
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.Calendar

class CSVProcessing {
    companion object {

        /*******************************************
         * Properties
         *******************************************/

        private val defaultFileName = "Test" + generateTag()

        /*******************************************
         * File Name Functions
         *******************************************/

        private fun generateTag(): String {
            val cal = Calendar.getInstance()
            return ("--${cal.get(Calendar.YEAR)}" +
                    "-${cal.get(Calendar.MONTH) + 1}" +
                    "-${cal.get(Calendar.DAY_OF_MONTH)}" +
                    "--${cal.get(Calendar.HOUR_OF_DAY)}" +
                    "-${cal.get(Calendar.MINUTE)}" +
                    "-${cal.get(Calendar.SECOND)}")
        }

        private fun generateCustomName(): String {
            return AppGlobals.fileName + generateTag()
        }

        /*******************************************
         * Directory and File Creations
         *******************************************/

        fun createDirectory(): Boolean {
            val externalStorageState = Environment.getExternalStorageState()
            if (externalStorageState == Environment.MEDIA_MOUNTED) {
                Timber.d("External storage is available")

                val saveDir = Environment.getExternalStoragePublicDirectory("Documents")
                if (!saveDir.exists() && !saveDir.mkdirs()) {
                    Timber.e("Failed to create parent directory")
                    return false
                }

                AppGlobals.csvDirectory = File(saveDir, "StreamData")

                return if (AppGlobals.csvDirectory!!.exists()) {
                    Timber.v("'StreamData' exists in the device's file system")
                    true
                } else {
                    if (AppGlobals.csvDirectory!!.mkdir()) {
                        Timber.v("Successfully created the folder")
                        true
                    } else {
                        Timber.e("Failed to create the folder")
                        false
                    }
                }
            } else {
                Timber.e("External storage not available")
                return false
            }
        }

        fun createFile(): Boolean {
            AppGlobals.csvFile = if (AppGlobals.fileName != null) {
                val name = generateCustomName()
                Timber.v("Using user-defined file name.")
                Timber.d("File name: '$name'")
                File("${AppGlobals.csvDirectory}/${name}.csv")
            } else {
                Timber.w("No file name specified by the user - generating DEFAULT name.")
                Timber.d("File name: '$defaultFileName'")
                File("${AppGlobals.csvDirectory}/$defaultFileName.csv")
            }

            return if (AppGlobals.csvFile!!.exists()) {
                Timber.w("File already exists.")
                false
            } else {
                Timber.tag("CSVProcessing").d("Full file path: '${AppGlobals.csvFile}'")
                true
            }
        }

        /*******************************************
         * Write to CSV
         *******************************************/

        fun writeToCSV() {

            val bufferRef = AppGlobals.fileBuffer

            if (bufferRef == null) {
                Timber.e("writeToCSV() -> ERROR: Buffer Writer NULL!")
                return
            }

            /** Get Test Header **/
            if (AppGlobals.testCount > 1) bufferRef.newLine()
            bufferRef.write("Test #${AppGlobals.testCount}")
            bufferRef.newLine()

            /** Get Velocity and Distance **/
            if (AppGlobals.unitType == UnitType.METERS) {
                bufferRef.write("Velocity:,${AppGlobals.velocity} m/s")
                bufferRef.newLine()
                bufferRef.write("Distance:,${AppGlobals.distance} m")
                bufferRef.newLine()
            } else {
                bufferRef.write("Velocity:,${AppGlobals.velocity} ft/s")
                bufferRef.newLine()
                bufferRef.write("Distance:,${AppGlobals.distance} ft")
                bufferRef.newLine()
            }

            /** Generate Value Headers  **/
            bufferRef.write("Time (s),Probe 1 (ppm), Probe 2 (ppm)")
            bufferRef.newLine()

            /** Adding data values to CSV file **/
            val dataOne = AppGlobals.graphOneFragment.data()
            val dataTwo = AppGlobals.graphTwoFragment.data()

            if ((dataOne != null && dataOne.dataSetCount > 0) &&
                (dataTwo != null && dataTwo.dataSetCount > 0)
            ) {
                val setOne = dataOne.getDataSetByIndex(0)
                val setTwo = dataTwo.getDataSetByIndex(0)

                if ((setOne != null && setOne.entryCount > 0) &&
                    (setTwo != null && setTwo.entryCount > 0)
                ) {
                    val maxEntryCount = maxOf(setOne.entryCount, setTwo.entryCount)

                    for (i in 1 until maxEntryCount) {
                        val avgX = calcAvg(i, setOne, setTwo)
                        val y1 = setOne.getEntryForIndex(i).y
                        val y2 = setTwo.getEntryForIndex(i).y

                        Timber.d("[CSV] -> ($avgX, $y1, $y2)")
                        bufferRef.write("$avgX,$y1,$y2")
                        bufferRef.newLine()
                    }

                    Timber.v("Finished writing test #${AppGlobals.testCount}")
                    bufferRef.flush()
                }
            }
        }

        private fun calcAvg(index: Int, setOne: ILineDataSet, setTwo: ILineDataSet): Float {
            val x1 = setOne.getEntryForIndex(index).x
            val x2 = setTwo.getEntryForIndex(index).x

            val formatAvgX = String.format("%.2f", (x1 + x2) / 2)
            return formatAvgX.toFloat()
        }

        /*******************************************
         * Helper Functions
         *******************************************/

        @RequiresApi(Build.VERSION_CODES.O)
        fun openBuffer(): Boolean {
            if (AppGlobals.csvFile == null) return false
            AppGlobals.fileBuffer = BufferedWriter(FileWriter(AppGlobals.csvFile, true))
            //MyObjects.fileBuffer = newBufferedWriter(Paths.get(MyObjects.csvFile.toString()))
            return true
        }

        fun closeBuffer(): Boolean {
            if (AppGlobals.fileBuffer == null) return false
            AppGlobals.fileBuffer!!.flush()
            AppGlobals.fileBuffer!!.close()
            AppGlobals.fileBuffer = null
            return true
        }

        /**
         * TODO "MUST BE WORKED ON MORE -> DELETES CURRENT OPEN FILE"
         * Wipes all the tests in the 'StreamData' Directory
         */
        @RequiresApi(Build.VERSION_CODES.O)
        fun wipeTests(): Boolean {
            if (AppGlobals.csvDirectory == null) return false

            Timber.v("Wiping all tests in directory.")
            AppGlobals.csvDirectory!!.listFiles()?.forEach { file ->
                if (file.isFile) {
                    file.delete()
                }
            }
            return true
        }
    }
}