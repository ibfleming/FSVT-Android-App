package me.ian.fsvt.csv

import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import me.ian.fsvt.graph.MyObjects
import me.ian.fsvt.graph.UnitType
import timber.log.Timber
import java.io.BufferedWriter
import java.io.File
import java.nio.file.Files.newBufferedWriter
import java.nio.file.Paths
import java.util.Calendar

class CSVProcessing {
    companion object {

        /*******************************************
         * Properties
         *******************************************/

        private lateinit var saveFolder : File
        private var fileWriter : BufferedWriter? = null

        private val cal : Calendar = Calendar.getInstance()
        private val defaultFileName = "Test" + generateTag()

        /*******************************************
         * Extension Functions
         *******************************************/

        private fun generateTag(): String {
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

        fun createDirectory(): Boolean {
            val externalStorageState = Environment.getExternalStorageState()
            if (externalStorageState == Environment.MEDIA_MOUNTED) {
                Timber.d("External storage is available")

                val saveDir = Environment.getExternalStoragePublicDirectory("Documents")
                if (!saveDir.exists() && !saveDir.mkdirs()) {
                    Timber.e("Failed to create parent directory")
                    return false
                }

                saveFolder = File(saveDir, "StreamData")

                return if (saveFolder.exists()) {
                    Timber.v("'StreamData' exists in the device's file system")
                    true
                } else {
                    if (saveFolder.mkdir()) {
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

        @RequiresApi(Build.VERSION_CODES.O)
        fun createFile() {
            val file: File = if (MyObjects.fileName != null) {
                val name = generateCustomName()
                Timber.v("Using user-defined file name.")
                Timber.d("File name: $name")
                File("$saveFolder/${name}.csv")
            } else {
                Timber.w("No file name specified by the user - generating DEFAULT name.")
                Timber.d("File name: $defaultFileName")
                File("$saveFolder/$defaultFileName.csv")
            }

            if (file.exists()) {
                Timber.w("File already exists.")
            } else {
                Timber.v("Creating and opening the file for writes.")
                Timber.d("Full path: $file")
                fileWriter = newBufferedWriter((Paths.get(file.toString())))
                fileWriter?.close()
            }
        }

        fun writeToCSV() {
            /** Get Test Header **/
            if( MyObjects.testCount > 1 ) {
                fileWriter?.newLine()
            }
            fileWriter?.write("Test #${MyObjects.testCount}")
            fileWriter?.newLine()

            /** Get Velocity and Distance **/
            if( MyObjects.unitType == UnitType.METERS) {
                fileWriter?.write("Velocity:,${MyObjects.velocity} m/s")
                fileWriter?.newLine()
                fileWriter?.write("Distance:,${MyObjects.distance} m")
                fileWriter?.newLine()
            }
            else {
                fileWriter?.write("Velocity:,${MyObjects.velocity} ft/s")
                fileWriter?.newLine()
                fileWriter?.write("Distance:,${MyObjects.distance} ft")
                fileWriter?.newLine()
            }

            /** Generate Value Headers  **/
            fileWriter?.write("Time (s),Probe 1 (ppm), Probe 2 (ppm)")
            fileWriter?.newLine()

            /** Adding data values to CSV file **/
            val dataOne = MyObjects.graphOneFragment.data()
            val dataTwo = MyObjects.graphTwoFragment.data()

            if( (dataOne != null && dataOne.dataSetCount > 0) &&
                (dataTwo != null && dataTwo.dataSetCount > 0 ) )
            {
                val setOne = dataOne.getDataSetByIndex(0)
                val setTwo = dataTwo.getDataSetByIndex(0)

                if( (setOne != null && setOne.entryCount > 0) &&
                    (setTwo != null && setTwo.entryCount > 0) )
                {
                    val maxEntryCount = maxOf(setOne.entryCount, setTwo.entryCount)

                    for(i in 1 until maxEntryCount) {
                        val avgX = calcAvg(i, setOne, setTwo)
                        val y1 = setOne.getEntryForIndex(i).y
                        val y2 = setTwo.getEntryForIndex(i).y

                        Timber.d("[CSV] -> ($avgX, $y1, $y2)")
                        fileWriter?.write("$avgX,$y1,$y2")
                        fileWriter?.newLine()
                    }

                    Timber.v("Closing file '${MyObjects.fileName}'. Finished writing data.")
                    fileWriter?.flush()
                }
            }
        }

        fun closeFile() {
            fileWriter?.close()
        }

        private fun calcAvg(index: Int, setOne: ILineDataSet, setTwo: ILineDataSet): Float {
            val x1 = setOne.getEntryForIndex(index).x
            val x2 = setTwo.getEntryForIndex(index).x

            val formatAvgX = String.format("%.2f", (x1 + x2) / 2)
            return formatAvgX.toFloat()
        }
    }
}