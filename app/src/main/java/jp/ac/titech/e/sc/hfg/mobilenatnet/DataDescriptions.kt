// # OptiTrack NatNet direct depacketization library for Java/Kotlin

import java.lang.RuntimeException
import java.security.MessageDigest
import java.util.*
import kotlin.collections.ArrayList
import kotlin.random.Random

private val kSkip = arrayListOf(0, 0, 1)
private val kFail = arrayListOf(0, 1, 0)
private val kPass = arrayListOf(1, 0, 0)

private fun sha1Digest(target: String): String {
    val hashBytes = MessageDigest.getInstance("SHA-1").digest(target.toByteArray())
    val hexChars = "0123456789abcdef"
    val result = StringBuilder(hashBytes.size * 2)
    hashBytes.forEach {
        val i = it.toInt()
        result.append(hexChars[i shr 4 and 0x0f])
        result.append(hexChars[i and 0x0f])
    }
    return result.toString()
}

private fun getTabStr(tabStr: String, level: Int): String {
    var outTabStr = ""
    val loopRange = 0 until level
    for (i in loopRange) {
        outTabStr += tabStr
    }
    return outTabStr
}

private fun addLists(totals: ArrayList<Int>, totalsTmp: ArrayList<Int>): ArrayList<Int> {
    totals[0] += totalsTmp[0]
    totals[1] += totalsTmp[1]
    totals[2] += totalsTmp[2]
    return totals
}

private fun testHash(testName: String, testHashStr: String, testObject: Any): Boolean {
    try {
        val method = testObject.javaClass.getMethod("getAsString", String::class.java, Int::class.java)
        val outStr = method.invoke(testObject, "  ", 0)
        outStr as String
        val outHashStr = sha1Digest(outStr)
        var retValue = true
        if (testHashStr == outHashStr) {
            println("[PASS]:$testName")
        } else {
            println("[FAIL]:%s test_hash_str != out_hash_str".format(testName))
            println("test_hash_str=%s".format(testHashStr))
            println("out_hash_str=%s".format(outHashStr))
            println("out_str =\n%s".format(outStr))
            retValue = false
        }
        return retValue
    } catch (e: Exception) {
        throw RuntimeException(e)
    }
}

private fun testHash2(testName: String, testHashStr: String, testObject: Any?, runTest: Boolean): ArrayList<Int> {
    var retValue = kFail
    var outStr = "FAIL"
    var outStr2 = ""
    val indentString = "       "
    if (!runTest) {
        retValue = kSkip
        outStr = "SKIP"
    } else if (testObject == null) {
        outStr = "FAIL"
        retValue = kFail
        outStr2 = "${indentString}ERROR: testObject was None"
    } else {
        if (testObject != null) {
            try {
                val method = testObject.javaClass.getMethod("getAsString", String::class.java, Int::class.java)
                val objOutStr = method.invoke(testObject, "  ", 0)
                objOutStr as String
                val objOutHashStr = sha1Digest(objOutStr)
                if (testHashStr == objOutHashStr) {
                    outStr = "PASS"
                    retValue = kPass
                } else {
                    outStr2 += "%s%s test_hash_str != out_hash_str\n".format(indentString, testName)
                    outStr2 += "%stest_hash_str=%s\n".format(indentString, testHashStr)
                    outStr2 += "%sobj_out_hash_str=%s\n".format(indentString, objOutHashStr)
                    outStr2 += "%sobj_out_str =\n%s".format(indentString, objOutStr)
                    retValue = kFail
                }
            } catch (e: Exception) {
                throw e
            }
        }

    }
    println("[%s]:%s".format(outStr, testName))

    if (outStr2.isNotEmpty()) {
        println(outStr2)
    }
    return retValue
}

private fun getAsString(inputStr: Any): String {
    return if (inputStr is String) {
        inputStr
    } else {
        inputStr.toString()
    }
}

private fun getDataSubPacketType(newData: Any?): String {
    var outString = ""
    val dataType = newData?.javaClass
    when (newData) {
        is MarkerSetDescription -> {
            outString = "Type: 0 Markerset\n"
        }

        is RigidBodyDescription -> {
            outString = "Type: 1 Rigid Body\n"
        }

        is SkeletonDescription -> {
            outString = "Type: 2 Skeleton\n"
        }

        is ForcePlateDescription -> {
            outString = "Type: 3 Force Plate\n"
        }

        is DeviceDescription -> {
            outString = "Type: 4 Device\n"
        }

        is CameraDescription -> {
            outString = "Type: 5 Camera\n"
        }

        null -> {
            outString = "Type: None\n"
        }

        else -> {
            outString = "Type: Unknown %s\n".format(dataType.toString())
        }
    }
    return outString
}

// cMarkerSetDescription
class MarkerSetDescription {
    var markerSetName = "Not Set"
    val markerNamesList = arrayListOf<String>()

    fun setName(newName: String) {
        markerSetName = newName
    }

    fun getNumMarkers(): Int {
        return markerNamesList.size
    }

    fun addMarkerName(markerName: String): Int {
        markerNamesList.add(markerName)
        return getNumMarkers()
    }

    fun getAsString(tabStr: String = "  ", level: Int = 0): String {
        val outTabStr = getTabStr(tabStr, level)
        val outTabStr2 = getTabStr(tabStr, level + 1)
        val outTabStr3 = getTabStr(tabStr, level + 2)
        var outString = ""
        outString += "%sMarker Set Name: %s\n".format(outTabStr, markerSetName)
        val numMarkers = markerNamesList.size
        outString += "%sMarker Count   : %d\n".format(outTabStr2, numMarkers)
        for (i in 0 until numMarkers) {
            outString += "%s%3d Marker Name: %s\n".format(outTabStr3, i, markerNamesList[i])
        }
        return outString
    }
}

class RBMarker(
    val markerName: String = "",
    val activeLabel: Int = 0,
    val pos: ArrayList<Double> = arrayListOf(0.0, 0.0, 0.0)
) {
    fun getAsString(tabStr: String = "  ", level: Int = 0): String {
        val outTabStr = getTabStr(tabStr, level)
        var outString = ""
        outString += "%sMarker Label: %s Position: [%f %f %f] %s\n".format(
            outTabStr,
            activeLabel,
            pos[0],
            pos[1],
            pos[2],
            markerName
        )
        return outString
    }
}

class RigidBodyDescription(
    var szName: String = "",
    newId: Int = 0,
    private var parentId: Int = 0,
    var pos: ArrayList<Double> = arrayListOf(0.0, 0.0, 0.0)
) {
    var idNum = newId
    val rbMarkerList = arrayListOf<RBMarker>()

    fun setName(newName: String) {
        szName = newName
    }

    fun setId(newId: Int) {
        idNum = newId
    }

    fun setParentId(newParentId: Int) {
        parentId = newParentId
    }

    fun setPos(pX: Double, pY: Double, pZ: Double) {
        pos = arrayListOf(pX, pY, pZ)
    }

    fun getNumMarkers(): Int {
        return rbMarkerList.size
    }

    fun addRbMarker(newRbMaker: RBMarker): Int {
        rbMarkerList.add(newRbMaker)
        return getNumMarkers()
    }

    fun getAsString(tabStr: String = "  ", level: Int = 0): String {
        val outTabStr = getTabStr(tabStr, level)
        val outTabStr2 = getTabStr(tabStr, level + 1)
        var outString = ""
        outString += "%sRigid Body Name   : %s\n".format(outTabStr, szName)
        outString += "%sID                : %d\n".format(outTabStr, idNum)
        outString += "%sParent ID         : %d\n".format(outTabStr, parentId)
        outString += "%sPosition          : [%3.2f, %3.2f, %3.2f]\n".format(outTabStr, pos[0], pos[1], pos[2])
        val numMarkers = rbMarkerList.size
        outString += "%sNumber of Markers : %d\n".format(outTabStr, numMarkers)
        // loop over markers
        for (i in 0 until numMarkers) {
            outString += "%s%d %s".format(outTabStr2, i, rbMarkerList[i].getAsString(tabStr, 0))
        }
        return outString
    }
}

class SkeletonDescription(private var name: String = "", newId: Int = 0) {
    var idNum = newId
    val rigidBodyDescriptionList = arrayListOf<RigidBodyDescription>()

    fun setName(newName: String) {
        name = newName
    }

    fun setId(newId: Int) {
        idNum = newId
    }

    fun addRigidBodyDescription(rigidBodyDescription: RigidBodyDescription): Int {
        rigidBodyDescriptionList.add(rigidBodyDescription)
        return rigidBodyDescriptionList.size
    }

    fun getAsString(tabStr: String = "  ", level: Int = 0): String {
        val outTabStr = getTabStr(tabStr, level)
        val outTabStr2 = getTabStr(tabStr, level + 1)
        var outString = ""
        outString += "%sName                    : %s\n".format(outTabStr, name)
        outString += "%sID                      : %d\n".format(outTabStr, idNum)
        val numBones = rigidBodyDescriptionList.size
        outString += "%sRigid Body (Bone) Count : %d\n".format(outTabStr, numBones)
        for (i in 0 until numBones) {
            outString += "%sRigid Body (Bone) %d\n".format(outTabStr2, i)
            outString += rigidBodyDescriptionList[i].getAsString(tabStr, level + 2)
        }
        return outString
    }
}

class ForcePlateDescription(newId: Int = 0, private var serialNumber: String = "") {
    var idNum = newId
    var width = 0.0
    var length = 0.0
    var position = arrayListOf(0.0, 0.0, 0.0)
    private var calMatrix = ArrayList(Collections.nCopies(12, ArrayList(Collections.nCopies(12, 0.0))))

    //    val calMatrix= [[0.0 for col in 0 until 12] for row in 0 until 12]
    private var corners = ArrayList(Collections.nCopies(4, ArrayList(Collections.nCopies(3, 0.0))))

    //    val corners = [[0.0 for col in 0 until 3] for row in 0 until 4]
    private var plateType = 0
    private var channelDataType = 0
    val channelList = arrayListOf<String>()

    fun setId(newId: Int) {
        idNum = newId
    }

    fun setSerialNumber(newSerialNumber: String) {
        serialNumber = newSerialNumber
    }

    fun setDimensions(newWidth: Double, newLength: Double) {
        width = newWidth
        length = newLength
    }

    fun setOrigin(pX: Double, pY: Double, pZ: Double) {
        position = arrayListOf(pX, pY, pZ)
    }

    fun setCalMatrix(newCalMatrix: ArrayList<ArrayList<Double>>) {
        calMatrix = newCalMatrix
    }

    fun setCorners(newCorners: ArrayList<ArrayList<Double>>) {
        corners = newCorners
    }

    fun setPlateType(newPlateType: Int) {
        plateType = newPlateType
    }

    fun setChannelDataType(newChannelDataType: Int) {
        channelDataType = newChannelDataType
    }

    fun addChannelName(channelName: String): Int {
        channelList.add(channelName)
        return channelList.size
    }

    /** Get force plate calibration matrix as string */
    fun getCalMatrixAsString(tabStr: String = "", level: Int = 0): String {
        val outTabStr = getTabStr(tabStr, level)
        val outTabStr2 = getTabStr(tabStr, level + 1)
        var outString = ""
        outString += "%sCal Matrix:\n".format(outTabStr)
        for (i in 0 until 12) {
            outString += "%s%2d %3.3e %3.3e %3.3e %3.3e %3.3e %3.3e %3.3e %3.3e %3.3e %3.3e %3.3e %3.3e\n".format(
                outTabStr2, i,
                calMatrix[i][0], calMatrix[i][1],
                calMatrix[i][2], calMatrix[i][3],
                calMatrix[i][4], calMatrix[i][5],
                calMatrix[i][6], calMatrix[i][7],
                calMatrix[i][8], calMatrix[i][9],
                calMatrix[i][10], calMatrix[i][11]
            )
        }
        return outString
    }

    /** Get force plate corner positions as a string */
    fun getCornersAsString(tabStr: String = "", level: Int = 0): String {
        // Corners 4x3 floats
        val outTabStr = getTabStr(tabStr, level)
        val outTabStr2 = getTabStr(tabStr, level + 1)
        var outString = ""
        outString += "%sCorners:\n".format(outTabStr)
        for (i in 0 until 4) {
            outString += "%s%2d %3.3e %3.3e %3.3e\n".format(
                outTabStr2,
                i,
                corners[i][0],
                corners[i][1],
                corners[i][2]
            )
        }
        return outString
    }

    /** Get force plate description as a class */
    fun getAsString(tabStr: String = "  ", level: Int = 0): String {
        val outTabStr = getTabStr(tabStr, level)
        var outString = ""
        outString += "%sID                      : %d\n".format(outTabStr, idNum)
        outString += "%sSerial Number           : %s\n".format(outTabStr, serialNumber)
        outString += "%sWidth                   : %3.2f\n".format(outTabStr, width)
        outString += "%sLength                  : %3.2f\n".format(outTabStr, length)
        outString += "%sOrigin                  : %3.2f, %3.2f, %3.2f\n".format(
            outTabStr,
            position[0],
            position[1],
            position[2]
        )
        outString += getCalMatrixAsString(tabStr, level)
        outString += getCornersAsString(tabStr, level)

        outString += "%sPlate Type                : %d\n".format(outTabStr, plateType)
        outString += "%sChannel Data Type         : %d\n".format(outTabStr, channelDataType)
        val numChannels = channelList.size
        outString += "%sNumber of Channels        : %d\n".format(outTabStr, numChannels)
        // Channel Names list of NoC strings
        val outTabStr2 = getTabStr(tabStr, level + 1)
        for (channelNum in 0 until numChannels) {
            outString += "%sChannel Name %d: %s\n".format(outTabStr2, channelNum, channelList[channelNum])
        }
        return outString
    }
}

/** Device Description class */
class DeviceDescription(
    newId: Int,
    private var name: String,
    val serialNumber: String,
    val deviceType: Int,
    val channelDataType: Int
) {
    var idNum = newId
    val channelList = arrayListOf<String>()

    /** Set the device id */
    fun setId(newId: Int) {
        idNum = newId
    }

    /** Set the Device name */
    fun setName(newName: String) {
        name = newName
    }

    /** Add channel name to channelList */
    fun addChannelName(channelName: String): Int {
        channelList.add(channelName)
        return channelList.size
    }

    /** Get Device Description as string */
    fun getAsString(tabStr: String = "  ", level: Int = 0): String {
        val outTabStr = getTabStr(tabStr, level)
        val outTabStr2 = getTabStr(tabStr, level + 1)
        var outString = ""
        outString += "%sID                 : %5d\n".format(outTabStr, idNum)
        outString += "%sName               : %s\n".format(outTabStr, name)
        outString += "%sSerial Number      : %s\n".format(outTabStr, serialNumber)
        outString += "%sDevice Type        : %d\n".format(outTabStr, deviceType)
        outString += "%sChannel Data Type  : %d\n".format(outTabStr, channelDataType)
        val numChannels = channelList.size
        outString += "%sNumber of Channels : %d\n".format(outTabStr, numChannels)
        for (i in 0 until numChannels) {
            outString += "%sChannel %2d Name : %s\n".format(outTabStr2, i, channelList[i])
        }
        return outString
    }
}

/** Camera Description class */
class CameraDescription(val name: String, positionVec3: ArrayList<Double>, orientationQuat: ArrayList<Double>) {
    val position = positionVec3
    val orientation = orientationQuat

    /** Get Camera Description as a string */
    fun getAsString(tabStr: String = "..", level: Int = 0): String {
        val outTabStr = getTabStr(tabStr, level)
        var outString = ""
        outString += "%sName        : %s\n".format(outTabStr, name)
        outString += "%sPosition    : [%3.2f, %3.2f, %3.2f]\n".format(outTabStr, position[0], position[1], position[2])
        outString += "%sOrientation : [%3.2f, %3.2f, %3.2f, %3.2f]\n".format(
            outTabStr,
            orientation[0],
            orientation[1],
            orientation[2],
            orientation[3]
        )
        return outString
    }
}

// cDataDescriptions
// Full data descriptions
/** Data Descriptions class */
class DataDescriptions(var orderNum: Int = 0) {
    val dataOrderDict = mutableMapOf<String, Pair<String, Int>>()
    val markerSetList = arrayListOf<MarkerSetDescription>()
    val rigidBodyList = arrayListOf<RigidBodyDescription>()
    val skeletonList = arrayListOf<SkeletonDescription>()
    val forcePlateList = arrayListOf<ForcePlateDescription>()
    val deviceList = arrayListOf<DeviceDescription>()
    val cameraList = arrayListOf<CameraDescription>()

    /** Generate the name for the order list based on the current length of the list */
    fun generateOrderName(): String {
        // should be a one up counter instead of based on length of dataOrderDict
        val orderName = "data_%03d".format(orderNum)
        orderNum += 1
        return orderName
    }

    // Add Marker Set
    /** Add a marker set */
    fun addMarkerSet(newMarkerSet: MarkerSetDescription) {
        val orderName = generateOrderName()

        // generate order entry
        val pos = markerSetList.size
        dataOrderDict[orderName] = Pair("marker_set_list", pos)
        markerSetList.add(newMarkerSet)
    }

    // Add Rigid Body
    /** Add a rigid body */
    fun addRigidBody(newRigidBody: RigidBodyDescription) {
        val orderName = generateOrderName()

        // generate order entry
        val pos = rigidBodyList.size
        dataOrderDict[orderName] = Pair("rigid_body_list", pos)
        rigidBodyList.add(newRigidBody)
    }

    // Add a skeleton
    /** Add a skeleton */
    fun addSkeleton(newSkeleton: SkeletonDescription) {
        val orderName = generateOrderName()

        // generate order entry
        val pos = skeletonList.size
        dataOrderDict[orderName] = Pair("skeleton_list", pos)
        skeletonList.add(newSkeleton)
    }


    // Add a force plate
    /** Add a force plate */
    fun addForcePlate(newForcePlate: ForcePlateDescription) {
        val orderName = generateOrderName()

        // generate order entry
        val pos = forcePlateList.size
        dataOrderDict[orderName] = Pair("force_plate_list", pos)
        forcePlateList.add(newForcePlate)
    }

    /** addDevice - Add a device */
    fun addDevice(newDevice: DeviceDescription) {
        val orderName = generateOrderName()

        // generate order entry
        val pos = deviceList.size
        dataOrderDict[orderName] = Pair("device_list", pos)
        deviceList.add(newDevice)
    }

    /** Add a new camera */
    fun addCamera(newCamera: CameraDescription) {
        val orderName = generateOrderName()

        // generate order entry
        val pos = cameraList.size
        dataOrderDict[orderName] = Pair("camera_list", pos)
        cameraList.add(newCamera)
    }

    /** Add data based on data type */
    fun addData(newData: Any?) {
        var dataType = newData?.javaClass
        when (newData) {
            is MarkerSetDescription -> {
                addMarkerSet(newData)
            }

            is RigidBodyDescription -> {
                addRigidBody(newData)
            }

            is SkeletonDescription -> {
                addSkeleton(newData)
            }

            is ForcePlateDescription -> {
                addForcePlate(newData)
            }

            is DeviceDescription -> {
                addDevice(newData)
            }

            is CameraDescription -> {
                addCamera(newData)
            }

            null -> {
                dataType = Unit.javaClass
            }

            else -> {
                println("ERROR: Type %s unknown".format(dataType.toString()))
            }
        }
    }

    /** Determine list name and position of the object */
    fun getObjectFromList(listName: String, posNum: Int): Any? {
        return if (listName == "marker_set_list" && posNum < markerSetList.size) {
            markerSetList[posNum]
        } else if (listName == "rigid_body_list" && posNum < rigidBodyList.size) {
            rigidBodyList[posNum]
        } else if (listName == "skeleton_list" && posNum < skeletonList.size) {
            skeletonList[posNum]
        } else if (listName == "force_plate_list" && posNum < forcePlateList.size) {
            forcePlateList[posNum]
        } else if (listName == "device_list" && posNum < deviceList.size) {
            deviceList[posNum]
        } else if (listName == "camera_list" && posNum < cameraList.size) {
            cameraList[posNum]
        } else {
            null
        }
    }

    /** Ensure data comes back as a string */
    fun getAsString(tabStr: String = "  ", level: Int = 0): String {
        val outTabStr = getTabStr(tabStr, level)
        val outTabStr2 = getTabStr(tabStr, level + 1)
        val outTabStr3 = getTabStr(tabStr, level + 2)
        var outString = ""
        val numDataSets = dataOrderDict.size
        outString += "%sNumber of Data Sets: %d\n".format(outTabStr, numDataSets)
        var i = 0
        for (tmp in dataOrderDict.entries) {
            //tmpName,tmpNum=dataOrderDict[dataSet]
            val tmpKey = tmp.key
            val tmpName = tmp.value.first
            val tmpNum = tmp.value.second
            val tmpObject = getObjectFromList(tmpName, tmpNum)
            outString += "%sDataset %3d\n".format(outTabStr2, i)
            val tmpString = getDataSubPacketType(tmpObject)
            if (tmpString != "") {
                outString += "%s%s".format(outTabStr2, tmpString)
            }
            //outString += "%s%s %s %d\n".format(outTabStr2, dataSet, tmpName,tmpNum)
            outString += "%s%s %s %s\n".format(outTabStr2, tmpKey, tmpName, tmpNum)
            outString += if (tmpObject != null) {
                try {
                    val method = tmpObject.javaClass.getMethod("getAsString", String::class.java, Int::class.java)
                    method.invoke(tmpObject, tabStr, level + 2) as String
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
            } else {
                "%s%s %s %s not found\n".format(outTabStr3, tmpKey, tmpName, tmpNum)
            }
            outString += "\n"
            i += 1
        }
        return outString
    }
}
// cDataDescriptions END

/** generateMarkerSetDescription - Testing functions */
fun generateMarkerSetDescription(setNum: Int = 0): MarkerSetDescription {
    val markerSetDescription = MarkerSetDescription()
    markerSetDescription.setName("MarkerSetName%03d".format(setNum))
    markerSetDescription.addMarkerName("MarkerName%03d_0".format(setNum))
    markerSetDescription.addMarkerName("MarkerName%03d_1".format(setNum))
    markerSetDescription.addMarkerName("MarkerName%03d_2".format(setNum))
    markerSetDescription.addMarkerName("MarkerName%03d_3".format(setNum))
    return markerSetDescription
}

/** generateRbMarker - Generate rigid body marker based on marker number */
fun generateRbMarker(markerNum: Int = 0): RBMarker {
    val markerNumMod = markerNum % 4
    val markerName = "RBMarker_%03d".format(markerNum)
    val markerActiveLabel = markerNum + 10000
    var markerPos = arrayListOf(1.0, 4.0, 9.0)
    when (markerNumMod) {
        1 -> markerPos = arrayListOf(1.0, 8.0, 27.0)
        2 -> markerPos = arrayListOf(3.1, 4.1, 5.9)
        3 -> markerPos = arrayListOf(1.0, 3.0, 6.0)
    }

    return RBMarker(markerName, markerActiveLabel, markerPos)
}

/** generateRigidBodyDescription - Generate Rigid Body Description Data */
fun generateRigidBodyDescription(rbdNum: Int = 0): RigidBodyDescription {
    val rbd = RigidBodyDescription()
    rbd.setName("rigidBodyDescription_%03d".format(rbdNum))
    rbd.setId(3141)
    rbd.setParentId(314)
    rbd.setPos(1.0, 4.0, 9.0)
    rbd.addRbMarker(generateRbMarker(0))
    rbd.addRbMarker(generateRbMarker(1))
    rbd.addRbMarker(generateRbMarker(2))

    return rbd
}

/** generateSkeletonDescription -Generate Test SkeletonDescription Data */
fun generateSkeletonDescription(skeletonNum: Int = 0): SkeletonDescription {
    val skelDesc = SkeletonDescription("SkeletonDescription_%03d".format(skeletonNum), skeletonNum)
    //generate some rigid bodies to add
    skelDesc.addRigidBodyDescription(generateRigidBodyDescription(0))
    skelDesc.addRigidBodyDescription(generateRigidBodyDescription(1))
    skelDesc.addRigidBodyDescription(generateRigidBodyDescription(2))
    skelDesc.addRigidBodyDescription(generateRigidBodyDescription(3))
    skelDesc.addRigidBodyDescription(generateRigidBodyDescription(5))
    skelDesc.addRigidBodyDescription(generateRigidBodyDescription(7))
    return skelDesc
}

/** generateForcePlateDescription - Generate Test ForcePlateDescription Data */
fun generateForcePlateDescription(forcePlateNum: Int = 0): ForcePlateDescription {
    val random = Random(forcePlateNum)

    val serialNumber = "S/N_%5d".format(random.nextInt(0, 99999))
    val width = random.nextDouble(0.0, 10.0)
    val length = random.nextDouble(0.0, 10.0)
    val origin =
        arrayListOf<Double>(random.nextDouble(0.0, 100.0), random.nextDouble(0.0, 100.0), random.nextDouble(0.0, 100.0))
    val corners = arrayListOf(
        arrayListOf(0.0, 0.0, 0.0),
        arrayListOf(0.0, 1.0, 0.0),
        arrayListOf(1.0, 1.0, 0.0),
        arrayListOf(1.0, 0.0, 0.0)
    )
    val fpDesc = ForcePlateDescription(forcePlateNum, serialNumber)
    fpDesc.setDimensions(width, length)
    fpDesc.setOrigin(origin[0], origin[1], origin[2])
//    fpDesc.setCalMatrix(calMatrix)
    fpDesc.setCorners(corners)
    for (i in 0 until 3) {
        fpDesc.addChannelName("channel_%03d".format(i))
    }
    return fpDesc
}

/** generateDeviceDescription- Generate Test DeviceDescription Data */
fun generateDeviceDescription(devNum: Int = 0): DeviceDescription {
    val newId = 0
    val name = "Device%03d".format(devNum)
    val serialNumber = "SerialNumber%03d".format(devNum)
    val deviceType = devNum % 4
    val channelDataType = devNum % 5
    val devDesc = DeviceDescription(newId, name, serialNumber, deviceType, channelDataType)
    for (i in 0 until channelDataType + 3) {
        devDesc.addChannelName("channel_name_%02d".format(i))
    }
    return devDesc
}

/** generateCameraDescription - Generate Test CameraDescription data */
fun generateCameraDescription(camNum: Int = 0): CameraDescription {
    val posVec3 = arrayListOf(1.0, 2.0, 3.0)
    val orientationQuat = arrayListOf(1.0, 2.0, 3.0, 4.0)
    return CameraDescription("Camera_%03d".format(camNum), posVec3, orientationQuat)
}


//generateDataDescriptions - Generate Test DataDescriptions
/** Generate data descriptions */
fun generateDataDescriptions(dataDescNum: Int = 0): DataDescriptions {
    val dataDescs = DataDescriptions()

    dataDescs.addData(generateMarkerSetDescription(dataDescNum + 0))
    dataDescs.addData(generateMarkerSetDescription(dataDescNum + 1))

    dataDescs.addData(generateRigidBodyDescription(dataDescNum + 0))
    dataDescs.addData(generateRigidBodyDescription(dataDescNum + 1))

    dataDescs.addSkeleton(generateSkeletonDescription(dataDescNum + 3))
    dataDescs.addSkeleton(generateSkeletonDescription(dataDescNum + 9))
    dataDescs.addSkeleton(generateSkeletonDescription(dataDescNum + 27))

    dataDescs.addForcePlate(generateForcePlateDescription(dataDescNum + 123))
    dataDescs.addForcePlate(generateForcePlateDescription(dataDescNum + 87))
    dataDescs.addForcePlate(generateForcePlateDescription(dataDescNum + 21))

    dataDescs.addDevice(generateDeviceDescription(dataDescNum + 0))
    dataDescs.addDevice(generateDeviceDescription(dataDescNum + 2))
    dataDescs.addDevice(generateDeviceDescription(dataDescNum + 4))

    dataDescs.addCamera(generateCameraDescription(dataDescNum + 0))
    dataDescs.addCamera(generateCameraDescription(dataDescNum + 10))
    dataDescs.addCamera(generateCameraDescription(dataDescNum + 3))
    dataDescs.addCamera(generateCameraDescription(dataDescNum + 7))
    return dataDescs
}


// testAll - Test all the major classes
/** Test all the Data Description classes */
fun testAllDataDescriptions(runTest: Boolean = true): ArrayList<Int> {
    var totals = arrayListOf(0, 0, 0)
    if (runTest) {
        val testCasesNames = arrayListOf<String>(
            "Test Marker Set Description 0", "Test RB Marker 0", "Test Rigid Body Description 0",
            "Test Skeleton Description 0", "Test Force Plate Description 0", "Test Device Description 0",
            "Test Camera Description 0", "Test Data Description 0"
        )
        val testHashStrs = arrayListOf<String>(
            "754fe535286ca84bd054d9aca5e9906ab9384d92",
            "0f2612abf2ce70e479d7b9912f646f12910b3310",
            "7a4e93dcda442c1d9c5dcc5c01a247e4a6c01b66",
            "b4d1a031dd7c323e3d316b5312329881a6a552ca",
            "3bf893c21a6c6da5fa341823942fd86d87e48fc4",
            "39b4fdda402bc73c0b1cd5c7f61599476aa9a926",
            "614602c5d290bda3b288138d5e25516dd1e1e85a",
            "5fc5c86d7f0fe31aede0c7cf6e37a90d6f147672"
        )
        val testListeners = arrayListOf<() -> Any>(
            { generateMarkerSetDescription(0) }, { generateRbMarker(0) },
            { generateRigidBodyDescription(0) }, { generateSkeletonDescription(0) },
            { generateForcePlateDescription(0) }, { generateDeviceDescription(0) },
            { generateCameraDescription(0) }, { generateDataDescriptions(0) })
        val runTests = arrayListOf<Boolean>(true, true, true, true, true, true, true, true)
        val numTests = runTests.size
        for (i in 0 until numTests) {
            val data = testListeners[i].invoke()
            val totalsTmp = testHash2(testCasesNames[i], testHashStrs[i], data, runTests[i])
            totals = addLists(totals, totalsTmp)
        }
    }

    println("--------------------")
    println("[PASS] Count = %3d".format(totals[0]))
    println("[FAIL] Count = %3d".format(totals[1]))
    println("[SKIP] Count = %3d".format(totals[2]))

    return totals
}

fun main() {
    testAllDataDescriptions(true)
}