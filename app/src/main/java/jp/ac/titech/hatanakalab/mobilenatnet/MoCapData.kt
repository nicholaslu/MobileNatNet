// # OptiTrack NatNet direct depacketization library for Java/Kotlin

import java.lang.RuntimeException
import java.security.MessageDigest
import kotlin.math.min
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

// getTabStr
// generate a string that takes the nesting level into account
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

private fun getAsString(inputStr: Any?): String {
    val typeInputStr = inputStr?.javaClass.toString()
    return when (inputStr) {
        is String -> {
            inputStr
        }

        null -> {
            ""
        }

        is ByteArray -> {
            inputStr.decodeToString()
        }

        else -> {
            println("type_input_str = %s NOT HANDLED".format(typeInputStr))
            inputStr.toString()
        }
    }
}

// MoCap Frame Classes
class FramePrefixData(val frameNumber: Int) {
    fun getAsString(tabStr: String = "  ", level: Int = 0): String {
        val outTabStr = getTabStr(tabStr, level)
        return "%sFrame #: %3d\n".format(outTabStr, frameNumber)
    }
}

class MarkerData {
    private var modelName = ""
    val markerPosList = arrayListOf<ArrayList<Double>>()

    fun setModelName(name: String) {
        this.modelName = name
    }

    fun addPos(pos: ArrayList<Double>): Int {
        markerPosList.add(pos)
        return markerPosList.size
    }

    fun getNumPoints(): Int {
        return markerPosList.size
    }

    fun getAsString(tabStr: String = "  ", level: Int = 0): String {
        val outTabStr = getTabStr(tabStr, level)
        val outTabStr2 = getTabStr(tabStr, level + 1)
        var outStr = ""
        if (modelName != "") {
            outStr += "%sModel Name : %s\n".format(outTabStr, modelName)
        }
        val markerCount = markerPosList.size
        outStr += "%sMarker Count :%3d\n".format(outTabStr, markerCount)
        for (i in 0 until markerCount) {
            val pos = markerPosList[i]
            outStr += "%sMarker %3d pos : [%3.2f,%3.2f,%3.2f]\n".format(outTabStr2, i, pos[0], pos[1], pos[2])
        }
        return outStr
    }
}

class MarkerSetData {
    val markerDataList = arrayListOf<MarkerData>()
    val unlabeledMarkers = MarkerData()

    init {
        unlabeledMarkers.setModelName("")
    }

    fun addMarkerData(markerData: MarkerData): Int {
        markerDataList.add(markerData)
        return markerDataList.size
    }

    fun addUnlabeledMarker(pos: ArrayList<Double>) {
        unlabeledMarkers.addPos(pos)
    }

    fun getMarkerSetCount(): Int {
        return markerDataList.size
    }

    fun getUnlabeledMarkerCount(): Int {
        return unlabeledMarkers.getNumPoints()
    }

    fun getAsString(tabStr: String = "  ", level: Int = 0): String {
        val outTabStr = getTabStr(tabStr, level)
        var outStr = ""

        // Labeled markers count
        val markerDataCount = markerDataList.size
        outStr += "%sMarker Set Count:%3d\n".format(outTabStr, markerDataCount)
        for (markerData in markerDataList) {
            outStr += markerData.getAsString(tabStr, level + 1)
        }

        // Unlabeled markers count (4 bytes)
        val unlabeledMarkersCount = unlabeledMarkers.getNumPoints()
        outStr += "%sUnlabeled Markers Count:%3d\n".format(outTabStr, unlabeledMarkersCount)
        outStr += unlabeledMarkers.getAsString(tabStr, level + 1)
        return outStr
    }
}

class RigidBodyMarker {
    var pos = arrayListOf<Double>(0.0, 0.0, 0.0)
    var idNum = 0
    var size = 0
    var error = 0.0

    fun getAsString(tabStr: String = "  ", level: Int = 0): String {
        val outTabStr = getTabStr(tabStr, level)
        var outStr = ""

        outStr += "%sPosition: [%3.2f %3.2f %3.2f]\n".format(outTabStr, pos[0], pos[1], pos[2])
        outStr += "%sID      : %3d\n".format(outTabStr, idNum)
        outStr += "%sSize    : %3d\n".format(outTabStr, size)
        return outStr
    }
}

class RigidBody(val idNum: Int, val pos: ArrayList<Double>, val rot: ArrayList<Double>) {
    var rbMarkerList = arrayListOf<RigidBodyMarker>()
    var trackingValid = false
    var error = 0.0

    fun addRigidBodyMarker(rigidBodyMarker: RigidBodyMarker): Int {
        rbMarkerList.add(rigidBodyMarker)
        return rbMarkerList.size
    }

    fun getAsString(tabStr: String = "  ", level: Int = 0): String {
        val outTabStr = getTabStr(tabStr, level)
        val outTabStr2 = getTabStr(tabStr, level + 1)

        var outStr = ""

        // header
        outStr += "%sID            : %3d\n".format(outTabStr, idNum)
        // Position and orientation
        outStr += "%sPosition      : [%3.2f, %3.2f, %3.2f]\n".format(outTabStr, pos[0], pos[1], pos[2])
        outStr += "%sOrientation   : [%3.2f, %3.2f, %3.2f, %3.2f]\n".format(outTabStr, rot[0], rot[1], rot[2], rot[3])

        val markerCount = rbMarkerList.size
        val markerCountRange = 0 until markerCount

        // Marker Data
        if (markerCount > 0) {
            outStr += "%sMarker Count: %3d\n".format(outTabStr, markerCount)
            for (i in markerCountRange) {
                outStr += "%sMarker %3d\n".format(outTabStr2, i)
                val rbMarker = rbMarkerList[i]
                outStr += rbMarker.getAsString(tabStr, level + 2)
            }
        }
        outStr += "%sMarker Error  : %3.2f\n".format(outTabStr, error)

        // Valid Tracking
        var tfString = "False"
        if (trackingValid) {
            tfString = "True"
        }
        outStr += "%sTracking Valid: %s\n".format(outTabStr, tfString)

        return outStr
    }
}

class RigidBodyData {
    var rigidBodyList = arrayListOf<RigidBody>()

    fun addRigidBody(rigidBody: RigidBody): Int {
        rigidBodyList.add(rigidBody)
        return rigidBodyList.size
    }

    fun getRigidBodyCount(): Int {
        return rigidBodyList.size
    }

    fun getAsString(tabStr: String = "  ", level: Int = 0): String {
        val outTabStr = getTabStr(tabStr, level)
        var outStr = ""
        val rigidBodyCount = rigidBodyList.size
        outStr += "%sRigid Body Count: %3d\n".format(outTabStr, rigidBodyCount)
        for (rigidBody in rigidBodyList) {
            outStr += rigidBody.getAsString(tabStr, level + 1)
        }
        return outStr
    }
}

class Skeleton(val idNum: Int = 0) {
    val rigidBodyList = arrayListOf<RigidBody>()

    fun addRigidBody(rigidBody: RigidBody): Int {
        rigidBodyList.add(rigidBody)
        return rigidBodyList.size
    }

    fun getAsString(tabStr: String = "  ", level: Int = 0): String {
        val outTabStr = getTabStr(tabStr, level)
        val outTabStr2 = getTabStr(tabStr, level + 1)
        var outStr = ""
        outStr += "%sID: %3d\n".format(outTabStr, idNum)
        val rigidBodyCount = rigidBodyList.size
        outStr += "%sRigid Body Count: %3d\n".format(outTabStr, rigidBodyCount)
        for (rbNum in 0 until rigidBodyCount) {
            outStr += "%sRigid Body %3d\n".format(outTabStr2, rbNum)
            outStr += rigidBodyList[rbNum].getAsString(tabStr, level + 2)
        }
        return outStr
    }
}

class SkeletonData {
    val skeletonList = arrayListOf<Skeleton>()

    fun addSkeleton(newSkeleton: Skeleton) {
        skeletonList.add(newSkeleton)
    }


    fun getSkeletonCount(): Int {
        return skeletonList.size
    }


    fun getAsString(tabStr: String = "  ", level: Int = 0): String {
        val outTabStr = getTabStr(tabStr, level)
        val outTabStr2 = getTabStr(tabStr, level + 1)
        var outStr = ""
        val skeletonCount = skeletonList.size
        outStr += "%sSkeleton Count: %3d\n".format(outTabStr, skeletonCount)
        for (skeletonNum in 0 until skeletonCount) {
            outStr += "%sSkeleton %3d\n".format(outTabStr2, skeletonNum)
            outStr += skeletonList[skeletonNum].getAsString(tabStr, level + 2)
        }
        return outStr
    }
}

class LabeledMarker(
    val idNum: Int,
    val pos: ArrayList<Double>,
    val size: Double = 0.0,
    val param: Int = 0,
    val residual: Double = 0.0
) {
    //todo size is <class 'tuple'>"?

    private fun decodeMarkerId(): Pair<Int, Int> {
        val modelId = idNum shr 16
        val markerId = idNum and 0x0000ffff
        return Pair(modelId, markerId)
    }

    private fun decodeParam(): Triple<Boolean, Boolean, Boolean> {
        val occluded = (param and 0x01) != 0
        val pointCloudSolved = (param and 0x02) != 0
        val modelSolved = (param and 0x04) != 0
        return Triple(occluded, pointCloudSolved, modelSolved)
    }

    fun getAsString(tabStr: String, level: Int): String {
        val outTabStr = getTabStr(tabStr, level)
        val (modelId, markerId) = decodeMarkerId()
        var outStr = ""
        outStr += "%sID                 : [MarkerID: %3d] [ModelID: %3d]\n".format(outTabStr, markerId, modelId)
        outStr += "%spos                : [%3.2f, %3.2f, %3.2f]\n".format(outTabStr, pos[0], pos[1], pos[2])
        outStr += "%ssize               : [%3.2f]\n".format(outTabStr, size)

        val (occluded, pointCloudSolved, modelSolved) = decodeParam()
        outStr += "%soccluded           : [%3d]\n".format(outTabStr, occluded.compareTo(false))
        outStr += "%spoint_cloud_solved : [%3d]\n".format(outTabStr, pointCloudSolved.compareTo(false))
        outStr += "%smodel_solved       : [%3d]\n".format(outTabStr, modelSolved.compareTo(false))
        outStr += "%serr                : [%3.2f]\n".format(outTabStr, residual)

        return outStr
    }
}

class LabeledMarkerData {
    val labeledMarkerList = arrayListOf<LabeledMarker>()

    fun addLabeledMarker(labeledMarker: LabeledMarker): Int {
        labeledMarkerList.add(labeledMarker)
        return labeledMarkerList.size
    }

    fun getLabeledMarkerCount(): Int {
        return labeledMarkerList.size
    }

    fun getAsString(tabStr: String = "  ", level: Int = 0): String {
        val outTabStr = getTabStr(tabStr, level)
        val outTabStr2 = getTabStr(tabStr, level + 1)
        var outStr = ""

        val labeledMarkerCount = labeledMarkerList.size
        outStr += "%sLabeled Marker Count:%3d\n".format(outTabStr, labeledMarkerCount)
        for (i in 0 until labeledMarkerCount) {
            outStr += "%sLabeled Marker %3d\n".format(outTabStr2, i)
            val labeledMarker = labeledMarkerList[i]
            outStr += labeledMarker.getAsString(tabStr, level + 2)
        }
        return outStr
    }
}

class ForcePlateChannelData {
    val frameList = arrayListOf<Double>()


    fun addFrameEntry(frameEntry: Double): Int {
        frameList.add(frameEntry)
        return frameList.size
    }

    fun getAsString(tabStr: String = "  ", level: Int = 0, channelNum: Int = -1): String {
        val fcMax = 4
        val outTabStr = getTabStr(tabStr, level)

        var outStr = ""
        val frameCount = frameList.size
        val fcShow = min(frameCount, fcMax)
        outStr += "%s".format(outTabStr)
        if (channelNum >= 0) {
            outStr += "Channel %3d: ".format(channelNum)
        }
        outStr += "%3d Frames - Frame Data: ".format(frameCount)
        for (i in 0 until fcShow) {
            outStr += "%3.2f ".format(frameList[i])
        }
        if (fcShow < frameCount) {
            outStr += " - Showing %3d of %3d frames".format(fcShow, frameCount)
        }
        outStr += "\n"
        return outStr
    }
}

class ForcePlate(val idNum: Int = 0) {
    val channelDataList = arrayListOf<ForcePlateChannelData>()

    fun addChannelData(channelData: ForcePlateChannelData): Int {
        channelDataList.add(channelData)
        return channelDataList.size
    }

    fun getAsString(tabStr: String, level: Int): String {
        val outTabStr = getTabStr(tabStr, level)
        var outStr = ""

        outStr += "%sID           : %3d".format(outTabStr, idNum)
        val numChannels = channelDataList.size
        outStr += "%sChannel Count: %3d\n".format(outTabStr, numChannels)
        for (i in 0 until numChannels) {
            outStr += channelDataList[i].getAsString(tabStr, level + 1, i)
        }
        return outStr
    }
}

class ForcePlateData {
    val forcePlateList = arrayListOf<ForcePlate>()

    fun addForcePlate(forcePlate: ForcePlate): Int {
        forcePlateList.add(forcePlate)
        return forcePlateList.size
    }

    fun getForcePlateCount(): Int {
        return forcePlateList.size
    }

    fun getAsString(tabStr: String = "  ", level: Int = 0): String {
        val outTabStr = getTabStr(tabStr, level)
        val outTabStr2 = getTabStr(tabStr, level + 1)
        var outStr = ""

        val forcePlateCount = forcePlateList.size
        outStr += "%sForce Plate Count: %3d\n".format(outTabStr, forcePlateCount)
        for (i in 0 until forcePlateCount) {
            outStr += "%sForce Plate %3d\n".format(outTabStr2, i)
            outStr += forcePlateList[i].getAsString(tabStr, level + 2)
        }
        return outStr
    }

}

class DeviceChannelData {
    val frameList = arrayListOf<Double>()

    fun addFrameEntry(frameEntry: Double): Int {
        frameList.add(frameEntry)
        return frameList.size
    }

    fun getAsString(tabStr: String, level: Int, channelNum: Int = -1): String { //changed default param
        val fcMax = 4
        val outTabStr = getTabStr(tabStr, level)

        var outStr = ""
        val frameCount = frameList.size
        val fcShow = min(frameCount, fcMax)
        outStr += "%s".format(outTabStr)
        if (channelNum >= 0) {
            outStr += "Channel %3d: ".format(channelNum)
        }
        outStr += "%3d Frames - Frame Data: ".format(frameCount)
        for (i in 0 until fcShow) {
            outStr += "%3.2f ".format(frameList[i])
        }
        if (fcShow < frameCount) {
            outStr += " - Showing %3d of %3d frames".format(fcShow, frameCount)
        }
        outStr += "\n"
        return outStr
    }
}

class Device(val idNum: Int) {
    val channelDataList = arrayListOf<DeviceChannelData>()

    fun addChannelData(channelData: DeviceChannelData) {
        channelDataList.add(channelData)
    }

    fun getAsString(tabStr: String, level: Int, deviceNum: Int): String {
        val outTabStr = getTabStr(tabStr, level)

        var outStr = ""

        val numChannels = channelDataList.size
        outStr += "%sDevice %3d      ID: %3d Num Channels: %3d\n".format(outTabStr, deviceNum, idNum, numChannels)
        for (i in 0 until numChannels) {
            outStr += channelDataList[i].getAsString(tabStr, level + 1, i)
        }

        return outStr
    }
}

class DeviceData {
    val deviceList = arrayListOf<Device>()

    fun addDevice(device: Device): Int {
        deviceList.add(device)
        return deviceList.size
    }

    fun getDeviceCount(): Int {
        return deviceList.size
    }

    fun getAsString(tabStr: String = "  ", level: Int = 0): String {
        val outTabStr = getTabStr(tabStr, level)

        var outStr = ""

        val deviceCount = deviceList.size
        outStr += "%sDevice Count: %3d\n".format(outTabStr, deviceCount)
        for (i in 0 until deviceCount) {
            outStr += deviceList[i].getAsString(tabStr, level + 1, i)
        }
        return outStr
    }
}

class FrameSuffixData {
    var timecode = -1
    var timecodeSub = -1
    var timestamp = -1.0
    var stampCameraMidExposure: Long = -1
    var stampDataReceived = -1
    var stampTransmit: Long = -1
    var param = 0
    var isRecording = false
    var trackedModelsChanged = true

    fun getAsString(tabStr: String = "  ", level: Int = 0): String {
        val outTabStr = getTabStr(tabStr, level)

        var outStr = ""
        if (timestamp != -1.0) {
            outStr += "%sTimestamp : %3.2f\n".format(outTabStr, timestamp)
        }
        if (stampCameraMidExposure != (-1).toLong()) {
            outStr += "%sMid-exposure timestamp : %3d\n".format(outTabStr, stampCameraMidExposure)
        }
        if (stampDataReceived != -1) {
            outStr += "%sCamera data received timestamp : %3d\n".format(outTabStr, stampDataReceived)
        }
        if (stampTransmit != (-1).toLong()) {
            outStr += "%sTransmit timestamp : %3d\n".format(outTabStr, stampTransmit)
        }
        return outStr
    }
}

class MoCapData {
    // Packet Parts
    lateinit var prefixData: FramePrefixData
    lateinit var markerSetData: MarkerSetData
    lateinit var rigidBodyData: RigidBodyData
    lateinit var skeletonData: SkeletonData
    lateinit var labeledMarkerData: LabeledMarkerData
    lateinit var forcePlateData: ForcePlateData
    lateinit var deviceData: DeviceData
    lateinit var suffixData: FrameSuffixData

    fun getAsString(tabStr: String = "  ", level: Int = 0): String {
        val outTabStr = getTabStr(tabStr, level)

        var outStr = ""
        outStr += "%sMoCap Frame Begin\n%s-----------------\n".format(outTabStr, outTabStr)
        outStr += if (::prefixData.isInitialized) {
            prefixData.getAsString()
        } else {
            "%sNo Prefix Data Set\n".format(outTabStr)
        }

        outStr += if (::markerSetData.isInitialized) {
            markerSetData.getAsString(tabStr, level + 1)
        } else {
            "%sNo Marker Set Data Set\n".format(outTabStr)
        }

        outStr += if (::rigidBodyData.isInitialized) {
            rigidBodyData.getAsString(tabStr, level + 1)
        } else {
            "%sNo Rigid Body Data Set\n".format(outTabStr)
        }

        outStr += if (::skeletonData.isInitialized) {
            skeletonData.getAsString(tabStr, level + 1)
        } else {
            "%sNo Skeleton Data Set\n".format(outTabStr)
        }

        outStr += if (::labeledMarkerData.isInitialized) {
            labeledMarkerData.getAsString(tabStr, level + 1)
        } else {
            "%sNo Labeled Marker Data Set\n".format(outTabStr)
        }

        outStr += if (::forcePlateData.isInitialized) {
            forcePlateData.getAsString(tabStr, level + 1)
        } else {
            "%sNo Force Plate Data Set\n".format(outTabStr)
        }

        outStr += if (::deviceData.isInitialized) {
            deviceData.getAsString(tabStr, level + 1)
        } else {
            "%sNo Device Data Set\n".format(outTabStr)
        }

        outStr += if (::suffixData.isInitialized) {
            suffixData.getAsString(tabStr, level + 1)
        } else {
            "%sNo Suffix Data Set\n".format(outTabStr)
        }

        outStr += "%sMoCap Frame End\n%s-----------------\n".format(outTabStr, outTabStr)
        return outStr
    }
}

// test program
fun generatePrefixData(frameNumber: Int = 0): FramePrefixData {
    return FramePrefixData(frameNumber)
}

fun generateLabel(labelBase: String = "label", labelNum: Int = 0): String {
    return "%s_%03d".format(labelBase, labelNum)
}

fun generatePositionSrand(posNum: Int = 0, frameNum: Int = 0): ArrayList<Double> {
    val random = Random(posNum + (frameNum * 1000))
    return arrayListOf(random.nextDouble(0.0, 100.0), random.nextDouble(0.0, 100.0), random.nextDouble(0.0, 100.0))
}

fun generateMarkerData(labelBase: String, labelNum: Int, numPoints: Int = 1): MarkerData {
    var label = generateLabel(labelBase, labelNum)
    if ((labelBase == null) || (labelBase == "")) {
        label = ""
    }
    val markerData = MarkerData()
    markerData.setModelName(label)
    val startNum = labelNum * 10000
    val endNum = startNum + numPoints
    for (pointNum in startNum until endNum) {
        val position = generatePositionSrand(pointNum)
        markerData.addPos(position)
    }
    return markerData
}

fun generateMarkerSetData(frameNum: Int = 0, markerSetNum: Int = 0): MarkerSetData {
    val markerSetData = MarkerSetData()
    //add labeled markers
    markerSetData.addMarkerData(generateMarkerData("marker", 0, 3))
    markerSetData.addMarkerData(generateMarkerData("marker", 1, 6))
    markerSetData.addMarkerData(generateMarkerData("marker", 2, 5))
    //add unlabeled markers
    val numPoints = 5
    val startNum = (frameNum * 100000) + (10000 + markerSetNum)
    val endNum = startNum + numPoints
    for (pointNum in startNum until endNum) {
        val position = generatePositionSrand(pointNum)
        markerSetData.addUnlabeledMarker(position)
    }
    return markerSetData
}

fun generateRigidBodyMarkerSrand(markerNum: Int = 0, frameNum: Int = 0): RigidBodyMarker {
    val rigidBodyMarker = RigidBodyMarker()
    val rbmNum = 11000 + markerNum
    val random = Random(rbmNum)
    rigidBodyMarker.pos = generatePositionSrand(rbmNum, frameNum)
    rigidBodyMarker.idNum = markerNum
    rigidBodyMarker.size = 1
    rigidBodyMarker.error = random.nextDouble()
    return rigidBodyMarker
}

fun generateRigidBody(bodyNum: Int = 0, frameNum: Int = 0): RigidBody {
    val pos = generatePositionSrand(10000 + bodyNum, frameNum)
    val rot = arrayListOf(1.0, 0.0, 0.0, 0.0)
    val rigidBody = RigidBody(bodyNum, pos, rot)
    rigidBody.addRigidBodyMarker(generateRigidBodyMarkerSrand(0, frameNum))
    rigidBody.addRigidBodyMarker(generateRigidBodyMarkerSrand(1, frameNum))
    rigidBody.addRigidBodyMarker(generateRigidBodyMarkerSrand(2))
    return rigidBody
}

fun generateRigidBodyData(frameNum: Int = 0): RigidBodyData {
    val rigidBodyData = RigidBodyData()
    // add rigid bodies
    rigidBodyData.addRigidBody(generateRigidBody(0, frameNum))
    rigidBodyData.addRigidBody(generateRigidBody(1, frameNum))
    rigidBodyData.addRigidBody(generateRigidBody(2, frameNum))
    return rigidBodyData
}

fun generateSkeleton(frameNum: Int = 0, skeletonNum: Int = 0, numRbs: Int = 1): Skeleton {
    val skeleton = Skeleton(skeletonNum)
    // add rigid bodies
    val rbSeedStart = skeletonNum * 165
    val rbSeedEnd = rbSeedStart + numRbs
    for (rbNum in rbSeedStart until rbSeedEnd) {
        skeleton.addRigidBody(generateRigidBody(rbNum, frameNum))
    }
    return skeleton
}

fun generateSkeletonData(frameNum: Int = 0): SkeletonData {
    val skeletonData = SkeletonData()
    skeletonData.addSkeleton(generateSkeleton(frameNum, 0, 2))
    skeletonData.addSkeleton(generateSkeleton(frameNum, 1, 6))
    skeletonData.addSkeleton(generateSkeleton(frameNum, 2, 3))
    return skeletonData
}

fun generateLabeledMarker(frameNum: Int = 0, markerNum: Int = 0): LabeledMarker {
    val pointNum = (frameNum * 2000) + markerNum
    val pos = generatePositionSrand(pointNum)
    val size = 1.0
    var param = 0
    //occluded 0x01
    param += 0x01 * 0
    //pointCloudSolved 0x02
    param += 0x02 * 0
    //modelSolved 0x04
    param += 0x04 * 1
    val residual = 0.01
    return LabeledMarker(markerNum, pos, size, param, residual)
}


fun generateLabeledMarkerData(frameNum: Int = 0): LabeledMarkerData {
    val labeledMarkerData = LabeledMarkerData()
    //add labeled marker
    labeledMarkerData.addLabeledMarker(generateLabeledMarker(frameNum, 0))
    labeledMarkerData.addLabeledMarker(generateLabeledMarker(frameNum, 1))
    labeledMarkerData.addLabeledMarker(generateLabeledMarker(frameNum, 2))

    return labeledMarkerData
}

fun generateFpChannelData(
    frameNum: Int = 0,
    fpNum: Int = 0,
    channelNum: Int = 0,
    numFrames: Int = 1
): ForcePlateChannelData {
    val rseed = (frameNum * 100000) + (fpNum * 10000) + (channelNum * 1000)
    val random = Random(rseed)
    val fpChannelData = ForcePlateChannelData()
    for (i in 0 until numFrames) {
        fpChannelData.addFrameEntry(random.nextDouble(0.0, 100.0))
    }
    return fpChannelData
}


fun generateForcePlate(frameNum: Int = 0, fpNum: Int = 0, numChannels: Int = 1): ForcePlate {
    val forcePlate = ForcePlate(fpNum)
    //add channelData
    for (i in 0 until numChannels) {
        forcePlate.addChannelData(generateFpChannelData(frameNum, fpNum, i, 10))
    }
    return forcePlate
}

fun generateForcePlateData(frameNum: Int = 0): ForcePlateData {
    val forcePlateData = ForcePlateData()
    // add force plates
    forcePlateData.addForcePlate(generateForcePlate(frameNum, 0, 3))
    forcePlateData.addForcePlate(generateForcePlate(frameNum, 1, 4))
    forcePlateData.addForcePlate(generateForcePlate(frameNum, 2, 2))
    return forcePlateData
}

fun generateDeviceChannelData(
    frameNum: Int = 0,
    deviceNum: Int = 0,
    channelNum: Int = 0,
    numFrames: Int = 1
): DeviceChannelData {
    val rseed = (frameNum * 100000) + (deviceNum * 10000) + (channelNum * 1000)
    val random = Random(rseed)
    val deviceChannelData = DeviceChannelData()
    for (i in 0 until (numFrames)) {
        deviceChannelData.addFrameEntry(random.nextDouble(0.0, 100.0))
    }
    return deviceChannelData
}


fun generateDevice(frameNum: Int = 0, deviceNum: Int = 0): Device {
    val device = Device(deviceNum)
    device.addChannelData(generateDeviceChannelData(frameNum, deviceNum, 1, 4))
    device.addChannelData(generateDeviceChannelData(frameNum, deviceNum, 3, 2))
    device.addChannelData(generateDeviceChannelData(frameNum, deviceNum, 7, 6))
    return device
}

fun generateDeviceData(frameNum: Int = 0): DeviceData {
    val deviceData = DeviceData()
    deviceData.addDevice(generateDevice(frameNum, 0))
    deviceData.addDevice(generateDevice(frameNum, 2))
    return deviceData
}

fun generateSuffixData(frameNum: Int = 0): FrameSuffixData {
    val frameSuffixData = FrameSuffixData()
    frameSuffixData.stampCameraMidExposure = 5844402979291 + frameNum
    frameSuffixData.stampDataReceived = 0
    frameSuffixData.stampTransmit = 5844403268753 + frameNum
    frameSuffixData.timecode = 0
    frameSuffixData.timecodeSub = 0
    frameSuffixData.timestamp = 762.63
    return frameSuffixData
}

fun generateMocapData(frameNum: Int = 0): MoCapData {
    val mocapData = MoCapData()

    mocapData.prefixData = generatePrefixData(frameNum)
    mocapData.markerSetData = generateMarkerSetData(frameNum)
    mocapData.rigidBodyData = generateRigidBodyData(frameNum)
    mocapData.skeletonData = generateSkeletonData(frameNum)
    mocapData.labeledMarkerData = generateLabeledMarkerData(frameNum)
    mocapData.forcePlateData = generateForcePlateData(frameNum)
    mocapData.deviceData = generateDeviceData(frameNum)
    mocapData.suffixData = generateSuffixData(frameNum)

    return mocapData
}

fun testAllMoCapData(runTest: Boolean = true): ArrayList<Int> {
    var totals = arrayListOf(0, 0, 0)
    if (runTest) {
        val testCasesNames = arrayListOf(
            "Test Prefix Data 0", "Test Marker Set Data 0", "Test Rigid Body Data 0",
            "Test Skeleton Data 0", "Test Labeled Marker Data 0", "Test Force Plate Data 0",
            "Test Device Data 0", "Test Suffix Data 0", "Test MoCap Data 0"
        )
        val testHashStrs = arrayListOf(
            "bffba016d02cf2167780df31aee697e1ec746b4c",
            "e39081da4886537ab4f4701cfce29e945a12adba",
            "b208c0b974ef1894e25992c48b8e39bdb01b034a",
            "f13449015a26efb94569fe766afa7fd35e5a24bd",
            "fc595a0bd3ab9ef0fd604109130221f748e09ef6",
            "606fb1dc217d82920c5a77d5ca18d21a81bdb253",
            "03f2c67e4a695bc4389d8bc8d07482278d15fb67",
            "6aa02c434bdb53a418ae1b1f73317dc80a5f887d",
            "4443061197f7702670c9e17bc08d54df6a5b2ce2"
        )
        val testListeners = arrayListOf<() -> Any>(
            { generatePrefixData(0) },
            { generateMarkerSetData(0) },
            { generateRigidBodyData(0) },
            { generateSkeletonData(0) },
            { generateLabeledMarkerData(0) },
            { generateForcePlateData(0) },
            { generateDeviceData(0) },
            { generateSuffixData(0) },
            { generateMocapData(0) })
        val runTests = arrayListOf<Boolean>(true, true, true, true, true, true, true, true, true)
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
    testAllMoCapData(true)
}
