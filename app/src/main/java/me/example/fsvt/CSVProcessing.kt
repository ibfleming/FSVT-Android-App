package me.example.fsvt

import android.os.Environment // used to get the External Storage path
import java.io.File // used to create new files
import java.nio.file.Paths // used to generate a path to a directory
import android.util.Log // used for debugging/output to Logcat
import org.apache.commons.csv.CSVFormat // used in writing to CSV files
import org.apache.commons.csv.CSVPrinter // used to export data to CSV file
import java.nio.file.Files.newBufferedWriter // used in writing to File output
import kotlin.io.path.Path // used with BufferedWriter to write to file
import android.icu.util.Calendar // used to generate default file name

// class for incoming data
    data class Data(val time: Double, val distance: Double, val velocity: Double){}

    /*Example List of data class Data to pass to writeCSV*/
    val data1 = Data(1.234, 5.678, 9.100)
    val data2 = Data(124.124, 114.141, 176.098)
    val data3 = Data(128.124, 111.415, 142.135)
    val dataList = listOf(data1, data2, data3) // list of data objects

class CSVProcessing {
// companion object contains output function
// and name generator
    companion object {
        // use date/time to generate a default name for each file
        fun generateName(): String{
            val defaultName = (Calendar.YEAR.toString()+"-"+Calendar.MONTH.toString()+"-"+
                                Calendar.DAY_OF_MONTH.toString()+"-"+Calendar.HOUR_OF_DAY.toString()+"-"+
                                Calendar.MINUTE.toString())
            return defaultName
        }
        // takes a list of Data objects and a string (name) as parameters
        fun outTest(input: List<Data>, name: String = "") {
            val tag = "TAG" // used to print Log messages (debug)

            val fileNameDefault = generateName() // get default name
            Log.i(tag, fileNameDefault) // print name to Logcat

            //val newDir = File("/storage/emulated/0/StreamVelocityData")
            //val newDir = File("/storage/emulated/0/Documents/StreamData")

            // get the path to external storage on the target device
            val fetchDir = Environment.getExternalStoragePublicDirectory("Documents").toString()
            Log.i(tag, fetchDir)
            val anotherTestDir = File(fetchDir + "/StreamData")

            // check if the directory already exists, else make the dir
            if(anotherTestDir.exists()){
                Log.i(tag, "Fetched Docs Directory")
            }else{
                Log.i(tag,"Creating directory...")
                anotherTestDir.mkdir()
                Log.i(tag, "Directory made")
            }

            Log.i(tag, "Name of file: "+name+" ")

            /*if(newDir.exists()){
                Log.i(tag,"Directory already exists.")
            }else{
                Log.i(tag,"Creating directory...")
                newDir.mkdir()
                Log.i(tag, "Directory made")
            }*/

            // fixed output path
            //val newFile = File("/storage/emulated/0/Documents/StreamData/"+fileNameDefault+name+".csv") // (+"concatenate user input string")
            //val newPath = Path("/storage/emulated/0/Documents/StreamData/"+fileNameDefault+name+".csv").toString() // (+"same here")

            // generate new file
            val newFile = File(anotherTestDir.toString()+"/"+fileNameDefault+name+".csv")
            val newPath = Path(anotherTestDir.toString()+"/"+fileNameDefault+name+".csv").toString()

            val newFileNameString = newFile.toString()
            Log.i(tag, newFileNameString)
            Log.i(tag, newPath)

            if(newFile.exists()){
                Log.i(tag, "File already exists")
            }else{
                Log.i(tag, "Creating new file...")
                // use buff Writer and apache Commons CSV to output data to the new file
                val newWriter = newBufferedWriter((Paths.get(newPath)))
                val csvOut = CSVPrinter(newWriter, CSVFormat.DEFAULT.withHeader("Time", "Distance", "Velocity"))

                for(data in input){
                    //get each member from the data class
                    val outGo = listOf(data.time, data.distance, data.velocity)
                    csvOut.printRecord(outGo)
                }
                csvOut.flush() // finish any CSV processes left
                csvOut.close() // close the file
            }
        }
    }
}


/*                          RECYCLE BIN
//val thingAgain = Environment.DIRECTORY_DOCUMENTS
    //val anotherThing = getExternalStoragePublicDirectory(thingAgain)
    //val newFile = File(anotherThing, "filename.txt") // get filename from user in future
 import java.nio.file.Files
 //forEach(){}
                //csvOut.printRecord(input) // iteration statement for all instances in list
 csvOut.printRecord(data.time)
                    csvOut.printRecord(data.distance)
                    csvOut.printRecord(data.velocity)
//if(name == "placeholder"){}
const val CREATE_FILE = 1 // used with ACTION_CREATE_FILE intent
import android.content.Intent
import android.content.Intent.ACTION_OPEN_DOCUMENT_TREE
import android.net.Uri
import android.os.Environment
import android.os.Environment.getExternalStorageDirectory
import android.os.Environment.getExternalStoragePublicDirectory
import android.os.Environment.getExternalStorageState
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.ui.graphics.Path
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files.isWritable
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isWritable
import java.net.URI
import java.io.BufferedWriter
if(!newDir.exists()){

                Log.i(tag, "Dir was not actually made")
            }

            /*val thingAgain = File(Environment.DIRECTORY_DOCUMENTS)
            if(thingAgain.exists()){
                Log.i(tag, "But we found the document directory!")
            }else{
                Log.i(tag, "We didn't find the document directory :(")
            }*/
            //val a = getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            /*var file = File("content://com.android.providers.media.documents/document/document_root/test.txt")
            file.bufferedWriter().use { out ->
                out.write("Testing")
                //out.write("\n")
                out.write("Testing")
            }*/
val testDir = File("/storage/emulated/0")
            val pathDir = Path("/storage/emulated/0/StreamVelocityData")
private fun createFile(pickerInitialUri: Uri){
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply{
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/txt"
            putExtra(Intent.EXTRA_TITLE, "test.txt")
        }
        //ActivityResultLauncher() =
    }
 fun OutputStream.writeCsv(data: List<Data>) {
            val writer = bufferedWriter()
            writer.write("Time, Distance, Velocity")
            writer.newLine()
            data.forEach {
                writer.write("{$it.time}, {$it.distance}, {$it.velocity}")
                writer.newLine()
            }
            writer.flush()
            FileOutputStream("filename.csv").apply { writeCsv(data) }
        }

        private fun anotherOutTest(){val csv = ByteArrayOutputStream().apply
        { writeCsv(dataList) }.toByteArray().let { String(it) }}*/