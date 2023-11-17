package me.ian.fsvt.csv

import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import me.ian.fsvt.MyObjects
import me.ian.fsvt.UnitType
import timber.log.Timber
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Files.newBufferedWriter
import java.nio.file.Paths
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
            return MyObjects.fileName + generateTag()
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

                MyObjects.csvDirectory = File(saveDir, "StreamData")

                return if (MyObjects.csvDirectory!!.exists()) {
                    Timber.v("'StreamData' exists in the device's file system")
                    true
                } else {
                    if (MyObjects.csvDirectory!!.mkdir()) {
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
            MyObjects.csvFile = if (MyObjects.fileName != null) {
                val name = generateCustomName()
                Timber.v("Using user-defined file name.")
                Timber.d("File name: '$name'")
                File("${MyObjects.csvDirectory}/${name}.csv")
            } else {
                Timber.w("No file name specified by the user - generating DEFAULT name.")
                Timber.d("File name: '$defaultFileName'")
                File("${MyObjects.csvDirectory}/$defaultFileName.csv")
            }

            return if (MyObjects.csvFile!!.exists()) {
                Timber.w("File already exists.")
                false
            } else {
                Timber.tag("CSVProcessing").d("Full file path: '${MyObjects.csvFile}'")
                true
            }
        }

        /*******************************************
         * Write to CSV
         *******************************************/

        fun writeToCSV() {

            val bufferRef = MyObjects.fileBuffer

            if (bufferRef == null) {
                Timber.e("writeToCSV() -> ERROR: Buffer Writer NULL!")
                return
            }

            /** Get Test Header **/
            if (MyObjects.testCount > 1) bufferRef.newLine()
            bufferRef.write("Test #${MyObjects.testCount}")
            bufferRef.newLine()

            /** Get Velocity and Distance **/
            if (MyObjects.unitType == UnitType.METERS) {
                bufferRef.write("Velocity:,${MyObjects.velocity} m/s")
                bufferRef.newLine()
                bufferRef.write("Distance:,${MyObjects.distance} m")
                bufferRef.newLine()
            } else {
                bufferRef.write("Velocity:,${MyObjects.velocity} ft/s")
                bufferRef.newLine()
                bufferRef.write("Distance:,${MyObjects.distance} ft")
                bufferRef.newLine()
            }

            /** Generate Value Headers  **/
            bufferRef.write("Time (s),Probe 1 (ppm), Probe 2 (ppm)")
            bufferRef.newLine()

            /** Adding data values to CSV file **/
            val dataOne = MyObjects.graphOneFragment.data()
            val dataTwo = MyObjects.graphTwoFragment.data()

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

                    Timber.v("Finished writing test #${MyObjects.testCount}")
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
            if (MyObjects.csvFile == null) return false
            MyObjects.fileBuffer = BufferedWriter(FileWriter(MyObjects.csvFile, true))
            //MyObjects.fileBuffer = newBufferedWriter(Paths.get(MyObjects.csvFile.toString()))
            return true
        }

        fun closeBuffer(): Boolean {
            if (MyObjects.fileBuffer == null) return false
            MyObjects.fileBuffer!!.flush()
            MyObjects.fileBuffer!!.close()
            MyObjects.fileBuffer = null
            return true
        }

        /**
         * TODO "MUST BE WORKED ON MORE -> DELETES CURRENT OPEN FILE"
         * Wipes all the tests in the 'StreamData' Directory
         */
        @RequiresApi(Build.VERSION_CODES.O)
        fun wipeTests(): Boolean {
            if (MyObjects.csvDirectory == null) return false

            Timber.v("Wiping all tests in directory.")
            MyObjects.csvDirectory!!.listFiles()?.forEach { file ->
                if (file.isFile) {
                    file.delete()
                }
            }
            return true
        }
    }
}