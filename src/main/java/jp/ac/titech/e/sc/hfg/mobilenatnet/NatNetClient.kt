// # OptiTrack NatNet direct depacketization library for Java/Kotlin

import java.io.IOException
import java.net.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.IllegalBlockingModeException
import java.util.*
import kotlin.math.min

fun trace(vararg args: Any) {
    //uncomment the one you want to use
    println(Array(args.size) { args[it].toString() }.joinToString(separator = ""))
//    ;
}

//Used for Data Description functions
fun traceDd(vararg args: Any) {
    //uncomment the one you want to use
    println(Array(args.size){args[it].toString()}.joinToString(separator = ""))
//    ;
}

//Used for MoCap Frame Data functions
fun traceMf(vararg args: Any) {
    //uncomment the one you want to use
//    println(Array(args.size) { args[it].toString() }.joinToString(separator = ""))
    ;
}

@ExperimentalUnsignedTypes
fun bytesToInt(data: ByteArray, byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN, signed: Boolean = false): Int {
    var result = 0
    if (signed) {
        if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
            for (i in data.indices) {
                result += data[i].toInt() shl 8 * i
            }
        } else {
            for (i in data.indices) {
                result += data[i].toInt() shl 8 * (data.size - i - 1)
            }
        }
    } else {
        val UData = data.asUByteArray()
        if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
            for (i in UData.indices) {
                result += UData[i].toInt() shl 8 * i
            }
        } else {
            for (i in UData.indices) {
                result += UData[i].toInt() shl 8 * (data.size - i - 1)
            }
        }
    }
    return result
}

@ExperimentalUnsignedTypes
fun intToBytes(
    data: Int,
    length: Int,
    byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN,
    signed: Boolean = false
): ByteArray {
    if (signed) {
        return if (length == 2) {
            ByteBuffer.allocate(length).order(byteOrder).putShort(data.toShort()).array()
        } else {
            ByteBuffer.allocate(length).order(byteOrder).putInt(data).array()
        }
    } else {
        var dividend = data
        val bytes = UByteArray(length)
        for (i in 0 until length) {
            val rem = dividend % 256
            when (byteOrder) {
                ByteOrder.LITTLE_ENDIAN -> bytes[i] = rem.toUByte()
                ByteOrder.BIG_ENDIAN -> bytes[length - i - 1] = rem.toUByte()
            }
            dividend /= 256
        }
        return bytes.asByteArray()
    }
}

fun bytesPartition(data: ByteArray, sep: String): Triple<String, String, String> {
    val list = data.decodeToString().split(sep)
    val sb = StringBuilder()
    list.slice(1 until list.size).forEach { i -> sb.append(i) }
    return Triple(list[0], sep, sb.toString())
}

@ExperimentalUnsignedTypes
fun getMessageId(data: ByteArray): Int {
    return bytesToInt(data.sliceArray(0 until 2), ByteOrder.LITTLE_ENDIAN)
}

// Create structs for reading various object types to speed up parsing.
//Vector2 = struct.Struct( '<ff' )
class Vector2 {
    companion object {
        private const val size = 2
        fun pack(data: ArrayList<Double>): ByteArray {
            val buffer = ByteBuffer.allocate(size * 4).order(ByteOrder.LITTLE_ENDIAN)
            data.forEach { i -> buffer.putFloat(i.toFloat()) }
            return buffer.array()
        }

        fun unpack(bytes: ByteArray): ArrayList<Double> {
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val data = ArrayList(MutableList(size) { 0.0 })
            for (i in data.indices) data[i] = buffer.getFloat().toDouble()
            return data
        }
    }
}

//Vector3 = struct.Struct( '<fff' )
class Vector3 {
    companion object {
        private const val size = 3
        fun pack(data: ArrayList<Double>): ByteArray {
            val buffer = ByteBuffer.allocate(size * 4).order(ByteOrder.LITTLE_ENDIAN)
            data.forEach { i -> buffer.putFloat(i.toFloat()) }
            return buffer.array()
        }

        fun unpack(bytes: ByteArray): ArrayList<Double> {
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val data = ArrayList(MutableList(size) { 0.0 })
            for (i in data.indices) data[i] = buffer.getFloat().toDouble()
            return data
        }
    }
}

//Quaternion = struct.Struct( '<ffff' )
class Quaternion {
    companion object {
        private const val size = 4
        fun pack(data: ArrayList<Double>): ByteArray {
            val buffer = ByteBuffer.allocate(size * 4).order(ByteOrder.LITTLE_ENDIAN)
            data.forEach { i -> buffer.putFloat(i.toFloat()) }
            return buffer.array()
        }

        fun unpack(bytes: ByteArray): ArrayList<Double> {
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val data = ArrayList(MutableList(size) { 0.0 })
            for (i in data.indices) data[i] = buffer.getFloat().toDouble()
            return data
        }
    }
}

//FloatValue = struct.Struct( '<f' )
class FloatValue {
    companion object {
        fun pack(data: Double): ByteArray {
            val buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
            buffer.putFloat(data.toFloat())
            return buffer.array()
        }

        fun unpack(bytes: ByteArray): Double {
            return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat().toDouble()
        }
    }
}

//DoubleValue = struct.Struct( '<d' )
class DoubleValue {
    companion object {
        fun pack(data: Double): ByteArray {
            val buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
            buffer.putDouble(data)
            return buffer.array()
        }

        fun unpack(bytes: ByteArray): Double {
            return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getDouble()
        }
    }
}

//NNIntValue = struct.Struct( '<I') //todo finish here later
class NNIntValue {
    companion object {
//        fun pack(data: UInt): ByteArray {
////            val buffer = ByteBuffer.allocate(8)
////            buffer.putInt(data.toByte()&0xffffffffL)
////            return buffer.array()
//        }
//        fun unpack(bytes: ByteArray): UInt {
////            return ByteBuffer.wrap(bytes).getInt()
//        }
    }
}

//FPCalMatrixRow = struct.Struct( '<ffffffffffff' )
class FPCalMatrixRow {
    companion object {
        private const val size = 12
        fun pack(data: ArrayList<Double>): ByteArray {
            val buffer = ByteBuffer.allocate(size * 4).order(ByteOrder.LITTLE_ENDIAN)
            data.forEach { i -> buffer.putFloat(i.toFloat()) }
            return buffer.array()
        }

        fun unpack(bytes: ByteArray): ArrayList<Double> {
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val data = ArrayList(MutableList(size) { 0.0 })
            for (i in data.indices) data[i] = buffer.getFloat().toDouble()
            return data
        }
    }
}

//FPCorners = struct.Struct( '<ffffffffffff')
class FPCorners() {
    companion object {
        private const val size = 12
        fun pack(data: ArrayList<Double>): ByteArray {
            val buffer = ByteBuffer.allocate(size * 4).order(ByteOrder.LITTLE_ENDIAN)
            data.forEach { i -> buffer.putFloat(i.toFloat()) }
            return buffer.array()
        }

        fun unpack(bytes: ByteArray): ArrayList<Double> {
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val data = ArrayList(MutableList(size) { 0.0 })
            for (i in data.indices) data[i] = buffer.getFloat().toDouble()
            return data
        }
    }
}

@ExperimentalUnsignedTypes
class NatNetClient {
    // printLevel = 0 off
    // printLevel = 1 on
    // printLevel = >1 on / print every nth mocap frame
    var printLevel = 20
        set(value) {
            if (value >= 0) {
                field = value
            }
        }

    // Change this value to the IP address of the NatNet server.
    var serverIpAddress = "127.0.0.1"

    // Change this value to the IP address of your local network interface
    var localIpAddress = "127.0.0.1"

    // This should match the multicast address listed in Motive's streaming settings.
    var multicastAddress = "239.255.42.99"

    // NatNet Command channel
    val commandPort = 1510

    // NatNet Data channel
    val dataPort = 1511

    var useMulticast = true
        set(value) {
            if (!isLocked) {
                field = value
            }
        }

    // Set this to a callback method of your choice to receive per-rigid-body data at each frame.
    lateinit var rigidBodyListener: (newId: Int, pos: ArrayList<Double>, rot: ArrayList<Double>) -> Unit
    lateinit var newFrameListener: (dataDict: MutableMap<String, Any>) -> Unit
    lateinit var dataDescriptionsListener: (dataDescs: DataDescriptions) -> Unit

    // Set Application Name
    private var applicationName = "Not Set"

    // NatNet stream version server is capable of. This will be updated during initialization only.
    private var natNetStreamVersionServer = arrayListOf(0, 0, 0, 0)

    // NatNet stream version. This will be updated to the actual version the server is using during runtime.
    private val natNetRequestedVersion = arrayListOf(0, 0, 0, 0)

    // server stream version. This will be updated to the actual version the server is using during initialization.
    private val serverVersion = arrayListOf(0, 0, 0, 0)

    // Lock values once run is called
    private var isLocked = false

    // Server has the ability to change bitstream version
    private var canChangeBitstreamVersion = false

    lateinit var commandThread: Thread
    lateinit var dataThread: Thread
    lateinit var commandSocket: DatagramSocket
    lateinit var dataSocket: DatagramSocket

    var stopThreads = false

    // Client/server message ids
    val NAT_CONNECT = 0
    val NAT_SERVERINFO = 1
    val NAT_REQUEST = 2
    val NAT_RESPONSE = 3
    val NAT_REQUEST_MODELDEF = 4
    val NAT_MODELDEF = 5
    val NAT_REQUEST_FRAMEOFDATA = 6
    val NAT_FRAMEOFDATA = 7
    val NAT_MESSAGESTRING = 8
    val NAT_DISCONNECT = 9
    val NAT_KEEPALIVE = 10
    val NAT_UNRECOGNIZED_REQUEST = 100
    val NAT_UNDEFINED = 999999.9999

    fun setClientAddress(newLocalIpAddress: String) {
        if (!isLocked) {
            localIpAddress = newLocalIpAddress
        }
    }

    fun getClientAddress(): String {
        return localIpAddress
    }

    fun setServerAddress(newServerIpAddress: String) {
        if (!isLocked) {
            serverIpAddress = newServerIpAddress
        }
    }

    fun getServerAddress(): String {
        return serverIpAddress
    }

//    fun setUseMulticast( newUseMulticast : Boolean) {
//        if (! isLocked){
//            useMulticast = newUseMulticast
//        }
//    }

    fun canChangeBitstreamVersion(): Boolean {
        return canChangeBitstreamVersion
    }

    /** checks to see if stream version can change, then changes it with position reset */
    fun setNatNetVersion(major: Int, minor: Int): Int {
        var returnCode = -1
        if (canChangeBitstreamVersion && (major != natNetRequestedVersion[0]) && (minor != natNetRequestedVersion[1])) {
            val szCommand = "Bitstream,%1d.%1d".format(major, minor)
            returnCode = sendCommand(szCommand)
            if (returnCode >= 0) {
                natNetRequestedVersion[0] = major
                natNetRequestedVersion[1] = minor
                natNetRequestedVersion[2] = 0
                natNetRequestedVersion[3] = 0
                println("changing bitstream MAIN")
                // get original output state
//                printResults = getPrintResults()
                //turn off output
//                setPrintResults(false)
                // force frame send and play reset
                sendCommand("TimelinePlay")
                Thread.sleep(100)
                val tmpCommands = arrayListOf(
                    "TimelinePlay",
                    "TimelineStop",
                    "SetPlaybackCurrentFrame,0",
                    "TimelineStop"
                )
                sendCommands(tmpCommands, false)
                Thread.sleep(2000)
                //reset to original output state
                //setPrintResults(printResults)
            }
        }
        return returnCode
    }


    fun getMajor(): Int {
        return natNetRequestedVersion[0]
    }

    fun getMinor(): Int {
        return natNetRequestedVersion[1]
    }

//    fun setPrintLevel(newPrintLevel: Int = 0): Int {
//        if (newPrintLevel >= 0) {
//            printLevel = newPrintLevel
//        }
//        return printLevel
//    }
//
//    fun getPrintLevel(): Int {
//        return printLevel
//    }

    fun connected(): Boolean {
        var retValue = true
        // check sockets
        if (!::commandSocket.isInitialized) {
            retValue = false
        } else if (!::dataSocket.isInitialized) {
            retValue = false
            // check versions
        } else if (getApplicationName() == "Not Set") {
            retValue = false
        } else if ((serverVersion[0] == 0) && (serverVersion[1] == 0) && (serverVersion[2] == 0) && (serverVersion[3] == 0)) {
            retValue = false
        }
        return retValue
    }

    // Create a command socket to attach to the NatNet stream
    private fun createCommandSocket(): DatagramSocket {
        if (useMulticast) {
            // Multicast case
            val result = MulticastSocket(0)
            // allow multiple clients on same machine to use multicast group address/port
//            result.setOption(StandardSocketOptions.SO_REUSEADDR, true)
            try {
//                val result = MulticastSocket(0)
//                result.bind(InetSocketAddress(InetAddress.getByName(null), 0))
                result.reuseAddress = true
//                result.bind(InetSocketAddress(InetAddress.getByName(""), 0))
            } catch (e: IOException) {
                println("ERROR: command socket IOException occurred:\n%s".format(e.message))
                println("Check Motive/Server mode requested mode agreement.  You requested Multicast ")
            } catch (e: IllegalArgumentException) {
                println("ERROR: command socket IllegalArgumentException occurred")
            } catch (e: SecurityException) {
                println("ERROR: command socket SecurityException occurred")
            } catch (e: UnknownHostException) {
                println("ERROR: command socket UnknownHostException occurred.")
            }
            // set to broadcast mode
            result.broadcast = true
//            result.setOption(StandardSocketOptions.SO_BROADCAST, true)
            // set timeout to allow for keep alive messages
            result.soTimeout = 2000
            return result
        } else {
            // Unicast case
            val result = DatagramSocket(null)
            try {
                result.bind(InetSocketAddress(localIpAddress, 0))
            } catch (e: SocketException) {
                println("ERROR: command socket IOException occurred:\n%s".format(e.message))
                println("Check Motive/Server mode requested mode agreement.  You requested Unicast ")
            } catch (e: IllegalArgumentException) {
                println("ERROR: command socket IllegalArgumentException occurred")
            } catch (e: SecurityException) {
                println("ERROR: command socket SecurityException occurred")
            }
            // set timeout to allow for keep alive messages
//            result.settimeout(2.0)
            result.soTimeout = 2000
//            result.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            result.reuseAddress = true
//            result.setOption(StandardSocketOptions.SO_REUSEADDR, true)
            return result
        }
    }

    // Create a data socket to attach to the NatNet stream
    private fun createDataSocket(port: Int): DatagramSocket {
        if (useMulticast) {
            // Multicast case
            val result = MulticastSocket(port)
            result.reuseAddress = true
            try {
                val mcastaddr = InetSocketAddress(multicastAddress, port)
                val netIf = NetworkInterface.getByName(localIpAddress)
                result.joinGroup(mcastaddr, netIf)
            } catch (e: SocketException) {
                println("ERROR: data socket SocketException occurred:\n%s".format(e.message))
                println("  Check Motive/Server mode requested mode agreement.  You requested Multicast ")
            } catch (e: SecurityException) {
                println("ERROR: data socket SecurityException occurred")
            } catch (e: IllegalArgumentException) {
                println("ERROR: data socket IllegalArgumentException occurred")
            } catch (e: UnknownHostException) {
                println("ERROR: data socket UnknownHostException occurred")
            }
            return result
        } else {
            // Unicast case
            val result = DatagramSocket(null)
            result.reuseAddress = true
            try {
                result.bind(InetSocketAddress(InetAddress.getByName(""), 0)) //todo check getByName
            } catch (e: SocketException) {
                println("ERROR: data socket error occurred:\n%s".format(e.message))
                println("Check Motive/Server mode requested mode agreement.  You requested Unicast ")
            } catch (e: SecurityException) {
                println("ERROR: data socket SecurityException occurred")
            } catch (e: IllegalArgumentException) {
                println("ERROR: data socket IllegalArgumentException occurred")
            } catch (e: UnknownHostException) {
                println("ERROR: data socket UnknownHostException occurred")
            }
            if (multicastAddress != "255.255.255.255") {
//                result.setsockopt(socket.IPPROTO_IP, socket.IP_ADD_MEMBERSHIP, socket.inet_aton(self.multicast_address) + socket.inet_aton(self.local_ip_address)) //todo need to join multicast group?
            }
            return result
        }
    }

    // Unpack a rigid body object from a data packet
    private fun unpackRigidBody(data: ByteArray, major: Int, minor: Int, rbNum: Int): Pair<Int, RigidBody> {
        var offset = 0

        // ID (4 bytes)
        var newId = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
        offset += 4

        traceMf("RB: %3d ID: %3d".format(rbNum, newId))

        // Position and orientation
        var pos = Vector3.unpack(data.sliceArray(offset until offset + 12))
        offset += 12
        traceMf("\tPosition    : [%3.2f, %3.2f, %3.2f]".format(pos[0], pos[1], pos[2]))

        val rot = Quaternion.unpack(data.sliceArray(offset until offset + 16))
        offset += 16
        traceMf("\tOrientation : [%3.2f, %3.2f, %3.2f, %3.2f]".format(rot[0], rot[1], rot[2], rot[3]))

        val rigidBody = RigidBody(newId, pos, rot)

        // Send information to any listener.
        if (::rigidBodyListener.isInitialized) {
            rigidBodyListener.invoke(newId, pos, rot)
        }

        // RB Marker Data ( Before version 3.0.  After Version 3.0 Marker data is in description )
        if (major < 3 && major != 0) {
            // Marker count (4 bytes)
            val markerCount = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
            offset += 4
            val markerCountRange = 0 until markerCount
            traceMf("\tMarker Count:", markerCount)

            val rbMarkerList = arrayListOf<RigidBodyMarker>()
            for (i in markerCountRange) {
                rbMarkerList.add(RigidBodyMarker())
            }

            // Marker positions
            for (i in markerCountRange) {
                pos = Vector3.unpack(data.sliceArray(offset until offset + 12))
                offset += 12
                traceMf("\tMarker", i, ":", pos[0], ",", pos[1], ",", pos[2])
                rbMarkerList[i].pos = pos
            }

            if (major >= 2) {
                // Marker ID's
                for (i in markerCountRange) {
                    newId = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
                    offset += 4
                    traceMf("\tMarker ID", i, ":", newId)
                    rbMarkerList[i].idNum = newId //todo id changed to idNum
                }

                // Marker sizes
                for (i in markerCountRange) {
                    val size = FloatValue.unpack(data.sliceArray(offset until offset + 4))
                    offset += 4
                    traceMf("\tMarker Size", i, ":", size)
                    rbMarkerList[i].size = size.toInt() //todo toInt() added
                }
            }

            for (i in markerCountRange) {
                rigidBody.addRigidBodyMarker(rbMarkerList[i])
            }
        }

        if (major >= 2) {
            val markerError = FloatValue.unpack(data.sliceArray(offset until offset + 4))
            offset += 4
            traceMf("\tMarker Error: %3.2f".format(markerError))
            rigidBody.error = markerError
        }

        // Version 2.6 and later
        if (((major == 2) && (minor >= 6)) || major > 2) {
            val param = ByteBuffer.wrap(data.sliceArray(offset until offset + 2)).getShort().toInt()
            val trackingValid = (param and 0x01) != 0
            offset += 2
            var isValidStr = "false"
            if (trackingValid) {
                isValidStr = "true"
            }
            traceMf("\tTracking Valid: %s".format(isValidStr))
            rigidBody.trackingValid = trackingValid
        }
        return Pair(offset, rigidBody)
    }

    // Unpack a skeleton object from a data packet
    private fun unpackSkeleton(data: ByteArray, major: Int, minor: Int): Pair<Int, Skeleton> {
        var offset = 0
        val newId = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
        offset += 4
        traceMf("ID:", newId)
        val skeleton = Skeleton(newId)

        val rigidBodyCount = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
        offset += 4
        traceMf("Rigid Body Count : %3d".format(rigidBodyCount))
        for (rbNum in 0 until rigidBodyCount) {
            val (offsetTmp, rigidBody) = unpackRigidBody(data.sliceArray(offset until data.size), major, minor, rbNum)
            skeleton.addRigidBody(rigidBody)
            offset += offsetTmp
        }
        return Pair(offset, skeleton)
    }

    //Unpack Mocap Data Functions
    private fun unpackFramePrefixData(data: ByteArray): Pair<Int, FramePrefixData> {
        var offset = 0
        // Frame number (4 bytes)
        val frameNumber = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
        offset += 4
        traceMf("Frame #:", frameNumber)
        val framePrefixData = FramePrefixData(frameNumber)
        return Pair(offset, framePrefixData)
    }

    private fun unpackMarkerSetData(
        data: ByteArray,
        packetSize: Int,
        major: Int,
        minor: Int
    ): Pair<Int, MarkerSetData> {
        val markerSetData = MarkerSetData()
        var offset = 0
        // Marker set count (4 bytes)
        val markerSetCount = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
        offset += 4
        traceMf("Marker Set Count:", markerSetCount)

        for (i in 0 until markerSetCount) {
            val markerData = MarkerData()
            // Model name
            val (modelName, separator, remainder) = bytesPartition(data.sliceArray(offset until data.size), "\u0000")
//            modelName, separator, remainder = bytes(data.sliceArray(offset until )).partition(b'\0')
            offset += modelName.length + 1
            traceMf("Model Name      : ", modelName)
            markerData.setModelName(modelName)
            // Marker count (4 bytes)
            val markerCount = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
            offset += 4
            traceMf("Marker Count    : ", markerCount)

            for (j in 0 until markerCount) {
                val pos = Vector3.unpack(data.sliceArray(offset until offset + 12))
                offset += 12
                traceMf("\tMarker %3d : [%3.2f,%3.2f,%3.2f]".format(j, pos[0], pos[1], pos[2]))
                markerData.addPos(pos)
                markerSetData.addMarkerData(markerData)
            }
        }

        // Unlabeled markers count (4 bytes)
        val unlabeledMarkersCount = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
        offset += 4
        traceMf("Unlabeled Markers Count:", unlabeledMarkersCount)

        for (i in 0 until unlabeledMarkersCount) {
            val pos = Vector3.unpack(data.sliceArray(offset until offset + 12))
            offset += 12
            traceMf("\tMarker %3d : [%3.2f,%3.2f,%3.2f]".format(i, pos[0], pos[1], pos[2]))
            markerSetData.addUnlabeledMarker(pos)
        }
        return Pair(offset, markerSetData)
    }

    private fun unpackRigidBodyData(
        data: ByteArray,
        packetSize: Int,
        major: Int,
        minor: Int
    ): Pair<Int, RigidBodyData> {
        val rigidBodyData = RigidBodyData()
        var offset = 0
        // Rigid body count (4 bytes)
        val rigidBodyCount = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
        offset += 4
        traceMf("Rigid Body Count:", rigidBodyCount)

        for (i in 0 until rigidBodyCount) {
            val (offsetTmp, rigidBody) = unpackRigidBody(data.sliceArray(offset until data.size), major, minor, i)
            offset += offsetTmp
            rigidBodyData.addRigidBody(rigidBody)
        }

        return Pair(offset, rigidBodyData)
    }


    private fun unpackSkeletonData(data: ByteArray, packetSize: Int, major: Int, minor: Int): Pair<Int, SkeletonData> {
        val skeletonData = SkeletonData()

        var offset = 0
        // Version 2.1 and later
        val skeletonCount: Int
        if ((major == 2 && minor > 0) || major > 2) {
            skeletonCount = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
            offset += 4
            traceMf("Skeleton Count:", skeletonCount)
            for (i in 0 until skeletonCount) {
                val (relOffset, skeleton) = unpackSkeleton(data.sliceArray(offset until data.size), major, minor)
                offset += relOffset
                skeletonData.addSkeleton(skeleton)
            }
        }
        return Pair(offset, skeletonData)
    }

    private fun decodeMarkerId(newId: Int): Pair<Int, Int> {
        var modelId = 0
        var markerId = 0
        modelId = newId shr 16
        markerId = newId and 0x0000ffff
        return Pair(modelId, markerId)
    }

    private fun unpackLabeledMarkerData(
        data: ByteArray,
        packetSize: Int,
        major: Int,
        minor: Int
    ): Pair<Int, LabeledMarkerData> {
        val labeledMarkerData = LabeledMarkerData()
        var offset = 0
        // Labeled markers (Version 2.3 and later)
        val labeledMarkerCount: Int
        if ((major == 2 && minor > 3) || major > 2) {
            labeledMarkerCount = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
            offset += 4
            traceMf("Labeled Marker Count:", labeledMarkerCount)
            for (i in 0 until labeledMarkerCount) {
                val tmpId = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
                offset += 4
                val (modelId, markerId) = decodeMarkerId(tmpId)
                val pos = Vector3.unpack(data.sliceArray(offset until offset + 12))
                offset += 12
                val size = FloatValue.unpack(data.sliceArray(offset until offset + 4))
                offset += 4
                traceMf("ID     : [MarkerID: %3d] [ModelID: %3d]".format(markerId, modelId))
                traceMf("  pos  : [%3.2f, %3.2f, %3.2f]".format(pos[0], pos[1], pos[2]))
                traceMf("  size : [%3.2f]".format(size))

                // Version 2.6 and later
                var param = 0
                if ((major == 2 && minor >= 6) || major > 2) {
                    param = ByteBuffer.wrap(data.sliceArray(offset until offset + 2)).getShort().toInt()
                    offset += 2
//                    val occluded = ( param and 0x01 ) != 0
//                    val pointCloudSolved = ( param and 0x02 ) != 0
//                    val modelSolved = ( param and 0x04 ) != 0
                }

                // Version 3.0 and later
                var residual = 0.0
                if (major >= 3) {
                    residual = FloatValue.unpack(data.sliceArray(offset until offset + 4))
                    offset += 4
                    traceMf("  err  : [%3.2f]".format(residual))
                }

                val labeledMarker = LabeledMarker(tmpId, pos, size, param, residual)
                labeledMarkerData.addLabeledMarker(labeledMarker)
            }
        }

        return Pair(offset, labeledMarkerData)
    }


    private fun unpackForcePlateData(
        data: ByteArray,
        packetSize: Int,
        major: Int,
        minor: Int
    ): Pair<Int, ForcePlateData> {
        val forcePlateData = ForcePlateData()
        val nFramesShowMax = 4
        var offset = 0
        // Force Plate data (version 2.9 and later)
        val forcePlateCount: Int
        if ((major == 2 && minor >= 9) || major > 2) {
            forcePlateCount = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
            offset += 4
            traceMf("Force Plate Count:", forcePlateCount)
            for (i in 0 until forcePlateCount) {
                // ID
                val forcePlateId = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
                offset += 4
                val forcePlate = ForcePlate(forcePlateId)

                // Channel Count
                val forcePlateChannelCount =
                    bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
                offset += 4

                traceMf(
                    "\tForce Plate %3d ID: %3d Num Channels: %3d".format(
                        i,
                        forcePlateId,
                        forcePlateChannelCount
                    )
                )

                // Channel Data
                for (j in 0 until forcePlateChannelCount) {
                    val fpChannelData = ForcePlateChannelData()
                    val forcePlateChannelFrameCount =
                        bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
                    offset += 4
                    var outString = "\tChannel %3d: ".format(j)
                    outString += "  %3d Frames - Frame Data: ".format(forcePlateChannelFrameCount)

                    // Force plate frames
                    val nFramesShow = min(forcePlateChannelFrameCount, nFramesShowMax)
                    for (k in 0 until forcePlateChannelFrameCount) {
                        val forcePlateChannelVal = FloatValue.unpack(data.sliceArray(offset until offset + 4))
                        offset += 4
                        fpChannelData.addFrameEntry(forcePlateChannelVal)

                        if (k < nFramesShow) {
                            outString += "%3.2f ".format(forcePlateChannelVal)
                        }
                    }
                    if (nFramesShow < forcePlateChannelFrameCount) {
                        outString += " showing %3d of %3d frames".format(nFramesShow, forcePlateChannelFrameCount)
                    }
                    traceMf("%s".format(outString))
                    forcePlate.addChannelData(fpChannelData)
                    forcePlateData.addForcePlate(forcePlate)
                }
            }
        }
        return Pair(offset, forcePlateData)
    }

    private fun unpackDeviceData(data: ByteArray, packetSize: Int, major: Int, minor: Int): Pair<Int, DeviceData> {
        val deviceData = DeviceData()
        val nFramesShowMax = 4
        var offset = 0
        // Device data (version 2.11 and later)
        val deviceCount: Int
        if ((major == 2 && minor >= 11) || (major > 2)) {
            deviceCount = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
            offset += 4
            traceMf("Device Count:", deviceCount)
            for (i in 0 until deviceCount) {
                // ID
                val deviceId = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
                offset += 4
                val device = Device(deviceId)
                // Channel Count
                val deviceChannelCount = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
                offset += 4

                traceMf("\tDevice %3d      ID: %3d Num Channels: %3d".format(i, deviceId, deviceChannelCount))

                // Channel Data
                for (j in 0 until deviceChannelCount) {
                    val deviceChannelData = DeviceChannelData()
                    val deviceChannelFrameCount =
                        bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
                    offset += 4
                    var outString = "\tChannel %3d ".format(j)
                    outString += "  %3d Frames - Frame Data: ".format(deviceChannelFrameCount)

                    // Device Frame Data
                    val nFramesShow = min(deviceChannelFrameCount, nFramesShowMax)
                    for (k in 0 until deviceChannelFrameCount) {
//                        val deviceChannelVal = bytesToInt( data.sliceArray(offset until offset+4), ByteOrder.LITTLE_ENDIAN ) //todo choose what
                        val deviceChannelVal = FloatValue.unpack(data.sliceArray(offset until offset + 4))
                        offset += 4
                        if (k < nFramesShow) {
                            outString += "%3.2f ".format(deviceChannelVal)
                        }

                        deviceChannelData.addFrameEntry(deviceChannelVal)
                        if (nFramesShow < deviceChannelFrameCount) {
                            outString += " showing %3d of %3d frames".format(nFramesShow, deviceChannelFrameCount)
                        }
                        traceMf("%s".format(outString))
                        device.addChannelData(deviceChannelData)
                    }
                    deviceData.addDevice(device)
                }
            }
        }
        return Pair(offset, deviceData)
    }

    private fun unpackFrameSuffixData(
        data: ByteArray,
        packetSize: Int,
        major: Int,
        minor: Int
    ): Pair<Int, FrameSuffixData> {
        val frameSuffixData = FrameSuffixData()
        var offset = 0

        // Timecode
        val timecode = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
        offset += 4
        frameSuffixData.timecode = timecode

        val timecodeSub = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
        offset += 4
        frameSuffixData.timecodeSub = timecodeSub

        // Timestamp (increased to double precision in 2.7 and later)
        if ((major == 2 && minor >= 7) or (major > 2)) {
            val timestamp = DoubleValue.unpack(data.sliceArray(offset until offset + 8))
            offset += 8
        } else {
            val timestamp = FloatValue.unpack(data.sliceArray(offset until offset + 4))
            offset += 4
            traceMf("Timestamp : %3.2f".format(timestamp))
            frameSuffixData.timestamp = timestamp
        }

        // Hires Timestamp (Version 3.0 and later)
        if (major >= 3) {
            val stampCameraMidExposure = bytesToInt(data.sliceArray(offset until offset + 8), ByteOrder.LITTLE_ENDIAN)
            traceMf("Mid-exposure timestamp         : %3d".format(stampCameraMidExposure))
            offset += 8
            frameSuffixData.stampCameraMidExposure = stampCameraMidExposure.toLong()

            val stampDataReceived = bytesToInt(data.sliceArray(offset until offset + 8), ByteOrder.LITTLE_ENDIAN)
            offset += 8
            frameSuffixData.stampDataReceived = stampDataReceived
            traceMf("Camera data received timestamp : %3d".format(stampDataReceived))

            val stampTransmit = bytesToInt(data.sliceArray(offset until offset + 8), ByteOrder.LITTLE_ENDIAN)
            offset += 8
            traceMf("Transmit timestamp             : %3d".format(stampTransmit))
            frameSuffixData.stampTransmit = stampTransmit.toLong()
        }


        // Frame parameters
        val param = ByteBuffer.wrap(data.sliceArray(offset until offset + 2)).getShort().toInt()
        val isRecording = (param and 0x01) != 0
        val trackedModelsChanged = (param and 0x02) != 0
        offset += 2
        frameSuffixData.param = param
        frameSuffixData.isRecording = isRecording
        frameSuffixData.trackedModelsChanged = trackedModelsChanged

        return Pair(offset, frameSuffixData)
    }


    // Unpack data from a motion capture frame message
    private fun unpackMocapData(data: ByteArray, packetSize: Int, major: Int, minor: Int): Pair<Int, MoCapData> {
        val mocapData = MoCapData()
        traceMf("MoCap Frame Begin\n-----------------")
        var offset = 0

        //Frame Prefix Data
        val (relOffset0, framePrefixData) = unpackFramePrefixData(data.sliceArray(offset until data.size))
        offset += relOffset0
        mocapData.prefixData = framePrefixData
        val frameNumber = framePrefixData.frameNumber

        //Marker Set Data
        val (relOffset1, markerSetData) = unpackMarkerSetData(
            data.sliceArray(offset until data.size),
            (packetSize - offset),
            major,
            minor
        )
        offset += relOffset1
        mocapData.markerSetData = markerSetData
        val markerSetCount = markerSetData.getMarkerSetCount()
        val unlabeledMarkersCount = markerSetData.getUnlabeledMarkerCount()

        // Rigid Body Data
        val (relOffset2, rigidBodyData) = unpackRigidBodyData(
            data.sliceArray(offset until data.size),
            (packetSize - offset),
            major,
            minor
        )
        offset += relOffset2
        mocapData.rigidBodyData = rigidBodyData
        val rigidBodyCount = rigidBodyData.getRigidBodyCount()

        // Skeleton Data
        val (relOffset3, skeletonData) = unpackSkeletonData(
            data.sliceArray(offset until data.size),
            (packetSize - offset),
            major,
            minor
        )
        offset += relOffset3
        mocapData.skeletonData = skeletonData
        val skeletonCount = skeletonData.getSkeletonCount()

        // Labeled Marker Data
        val (relOffset4, labeledMarkerData) = unpackLabeledMarkerData(
            data.sliceArray(offset until data.size),
            (packetSize - offset),
            major,
            minor
        )
        offset += relOffset4
        mocapData.labeledMarkerData = labeledMarkerData
        val labeledMarkerCount = labeledMarkerData.getLabeledMarkerCount()

        // Force Plate Data
        val (relOffset5, forcePlateData) = unpackForcePlateData(
            data.sliceArray(offset until data.size),
            (packetSize - offset),
            major,
            minor
        )
        offset += relOffset5
        mocapData.forcePlateData = forcePlateData

        // Device Data
        val (relOffset6, deviceData) = unpackDeviceData(
            data.sliceArray(offset until data.size),
            (packetSize - offset),
            major,
            minor
        )
        offset += relOffset6
        mocapData.deviceData = deviceData

        // Frame Suffix Data
        //relOffset, timecode, timecodeSub, timestamp, isRecording, trackedModelsChanged = \
        val (relOffset7, frameSuffixData) = unpackFrameSuffixData(
            data.sliceArray(offset until data.size),
            (packetSize - offset),
            major,
            minor
        )
        offset += relOffset7
        mocapData.suffixData = frameSuffixData

        val timecode = frameSuffixData.timecode
        val timecodeSub = frameSuffixData.timecodeSub
        val timestamp = frameSuffixData.timestamp
        val isRecording = frameSuffixData.isRecording
        val trackedModelsChanged = frameSuffixData.trackedModelsChanged
        // Send information to any listener.
        if (::newFrameListener.isInitialized) {
            val dataDict = mutableMapOf<String, Any>()
            dataDict["frameNumber"] = frameNumber
            dataDict["markerSetCount"] = markerSetCount
            dataDict["unlabeledMarkersCount"] = unlabeledMarkersCount
            dataDict["rigidBodyCount"] = rigidBodyCount
            dataDict["skeletonCount"] = skeletonCount
            dataDict["labeledMarkerCount"] = labeledMarkerCount
            dataDict["timecode"] = timecode
            dataDict["timecodeSub"] = timecodeSub
            dataDict["timestamp"] = timestamp
            dataDict["isRecording"] = isRecording
            dataDict["trackedModelsChanged"] = trackedModelsChanged
            newFrameListener.invoke(dataDict)
        }
        traceMf("MoCap Frame End\n-----------------")
        return Pair(offset, mocapData)
    }

    // Unpack a marker set description packet
    private fun unpackMarkerSetDescription(data: ByteArray, major: Int, minor: Int): Pair<Int, MarkerSetDescription> {
        val msDesc = MarkerSetDescription()

        var offset = 0

        val (name, _, _) = bytesPartition(data.sliceArray(offset until data.size), "\u0000")
        offset += name.length + 1
        traceDd("Marker Set Name: %s".format(name))
        msDesc.setName(name.toString())

        val markerCount = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
        offset += 4
        traceDd("Marker Count : %3d".format(markerCount))
        for (i in 0 until markerCount) {
            val (name, _, _) = bytesPartition(data.sliceArray(offset until data.size), "\u0000")
            offset += name.length + 1
            traceDd("\t%2d Marker Name: %s".format(i, name))
            msDesc.addMarkerName(name.toString())
        }

        return Pair(offset, msDesc)
    }

    // Unpack a rigid body description packet
    private fun unpackRigidBodyDescription(data: ByteArray, major: Int, minor: Int): Pair<Int, RigidBodyDescription> {
        val rbDesc = RigidBodyDescription()
        var offset = 0

        // Version 2.0 or higher
        if ((major >= 2) || (major == 0)) {
            val (name, _, _) = bytesPartition(data.sliceArray(offset until data.size), "\u0000")
            offset += name.length + 1
            rbDesc.setName(name)
            traceDd("\tRigid Body Name   : ", name)
        }

        // ID
        val newId = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
        offset += 4
        rbDesc.setId(newId)
        traceDd("\tID                : ", newId)

        //Parent ID
        val parentId = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
        offset += 4
        rbDesc.setParentId(parentId)
        traceDd("\tParent ID         : ", parentId)

        // Position Offsets
        val pos = Vector3.unpack(data.sliceArray(offset until offset + 12))
        offset += 12
        rbDesc.setPos(pos[0], pos[1], pos[2])

        traceDd("\tPosition          : [%3.2f, %3.2f, %3.2f]".format(pos[0], pos[1], pos[2]))

        // Version 3.0 and higher, rigid body marker information contained in description
        if ((major >= 3) or (major == 0)) {
            // Marker Count
            val markerCount = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
            offset += 4
            traceDd("\tNumber of Markers : ", markerCount)

            val markerCountRange = 0 until markerCount
            var offset1 = offset
            var offset2 = offset1 + (12 * markerCount)
            var offset3 = offset2 + (4 * markerCount)
            // Marker Offsets X,Y,Z
            var markerName = ""
            for (marker in markerCountRange) {
                // Offset
                val markerOffset = Vector3.unpack(data.sliceArray(offset1 until offset1 + 12))
                offset1 += 12

                // Active Label
                val activeLabel = bytesToInt(data.sliceArray(offset2 until offset2 + 4), ByteOrder.LITTLE_ENDIAN)
                offset2 += 4

                //Marker Name
                if ((major >= 4) || (major == 0)) {
                    // markername
                    val (markerNameTmp, _, _) = bytesPartition(
                        data.sliceArray(offset3 until data.size),
                        "\u0000"
                    )
                    markerName = markerNameTmp
                    offset3 += markerName.length + 1
                }

                val rbMarker = RBMarker(markerName, activeLabel, markerOffset)
                rbDesc.addRbMarker(rbMarker)
                traceDd(
                    "\t%3d Marker Label: %s Position: [%3.2f %3.2f %3.2f] %s".format(
                        marker,
                        activeLabel,
                        markerOffset[0],
                        markerOffset[1],
                        markerOffset[2],
                        markerName
                    )
                )
            }
            offset = offset3
        }

        traceDd("\tunpackRigidBodyDescription processed bytes: ", offset)
        return Pair(offset, rbDesc)
    }

    // Unpack a skeleton description packet
    private fun unpackSkeletonDescription(data: ByteArray, major: Int, minor: Int): Pair<Int, SkeletonDescription> {
        val skeletonDesc = SkeletonDescription()
        var offset = 0

        //Name
        val (name, _, _) = bytesPartition(data.sliceArray(offset until data.size), "\u0000")
        offset += name.length + 1
        skeletonDesc.setName(name)
        traceDd("Name : %s".format(name))

        //ID
        val newId = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
        offset += 4
        skeletonDesc.setId(newId)
        traceDd("ID : %3d".format(newId))

        // # of RigidBodies
        val rigidBodyCount = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
        offset += 4
        traceDd("Rigid Body (Bone) Count : %3d".format(rigidBodyCount))

        // Loop over all Rigid Bodies
        for (i in 0 until rigidBodyCount) {
            traceDd("Rigid Body (Bone) ", i)
            val (offsetTmp, rbDescTmp) = unpackRigidBodyDescription(
                data.sliceArray(offset until data.size),
                major,
                minor
            )
            offset += offsetTmp
            skeletonDesc.addRigidBodyDescription(rbDescTmp)
        }
        return Pair(offset, skeletonDesc)
    }

    private fun unpackForcePlateDescription(
        data: ByteArray,
        major: Int,
        minor: Int
    ): Pair<Int, ForcePlateDescription?> {
        var offset = 0
        if (major >= 3) {
            val fpDesc = ForcePlateDescription()
            // ID
            val newId = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
            offset += 4
            fpDesc.setId(newId)
            traceDd("\tID : ", newId)

            // Serial Number
            val (serialNumber, _, _) = bytesPartition(data.sliceArray(offset until data.size), "\u0000")
            offset += serialNumber.length + 1
            fpDesc.setSerialNumber(serialNumber)
            traceDd("\tSerial Number : ", serialNumber)

            // Dimensions
            val fWidth = FloatValue.unpack(data.sliceArray(offset until offset + 4))
            offset += 4
            traceDd("\tWidth  : %3.2f".format(fWidth))
            val fLength = FloatValue.unpack(data.sliceArray(offset until offset + 4))
            offset += 4
            fpDesc.setDimensions(fWidth, fLength)
            traceDd("\tLength : %3.2f".format(fLength))

            // Origin
            val origin = Vector3.unpack(data.sliceArray(offset until offset + 12))
            offset += 12
            fpDesc.setOrigin(origin[0], origin[1], origin[2])
            traceDd("\tOrigin : %3.2f, %3.2f, %3.2f".format(origin[0], origin[1], origin[2]))

            // Calibration Matrix 12x12 floats
            traceDd("Cal Matrix:")
            val calMatrixTmp = ArrayList(Collections.nCopies(12, ArrayList(Collections.nCopies(12, 0.0))))

            for (i in 0 until 12) {
                val calMatrixRow = FPCalMatrixRow.unpack(data.sliceArray(offset until offset + (12 * 4)))
                traceDd(
                    "\t%3d %3.3e %3.3e %3.3e %3.3e %3.3e %3.3e %3.3e %3.3e %3.3e %3.3e %3.3e %3.3e".format(
                        i,
                        calMatrixRow[0],
                        calMatrixRow[1],
                        calMatrixRow[2],
                        calMatrixRow[3],
                        calMatrixRow[4],
                        calMatrixRow[5],
                        calMatrixRow[6],
                        calMatrixRow[7],
                        calMatrixRow[8],
                        calMatrixRow[9],
                        calMatrixRow[10],
                        calMatrixRow[11]
                    )
                )
                calMatrixTmp[i] = calMatrixRow
                offset += (12 * 4)
            }
            fpDesc.setCalMatrix(calMatrixTmp)
            // Corners 4x3 floats
            val corners = FPCorners.unpack(data.sliceArray(offset until offset + (12 * 4)))
            offset += (12 * 4)
            var o2 = 0
            traceDd("Corners:")
            val cornersTmp = ArrayList(Collections.nCopies(4, ArrayList(Collections.nCopies(3, 0.0))))
            for (i in 0 until 4) {
                traceDd("\t%3d %3.3e %3.3e %3.3e".format(i, corners[o2], corners[o2 + 1], corners[o2 + 2]))
                cornersTmp[i][0] = corners[o2]
                cornersTmp[i][1] = corners[o2 + 1]
                cornersTmp[i][2] = corners[o2 + 2]
                o2 += 3
            }
            fpDesc.setCorners(cornersTmp)

            // Plate Type int
            val plateType = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
            offset += 4
            fpDesc.setPlateType(plateType)
            traceDd("Plate Type : ", plateType)

            // Channel Data Type int
            val channelDataType = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
            offset += 4
            fpDesc.setChannelDataType(channelDataType)
            traceDd("Channel Data Type : ", channelDataType)

            // Number of Channels int
            val numChannels = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
            offset += 4
            traceDd("Number of Channels : ", numChannels)

            // Channel Names list of NoC strings
            for (i in 0 until numChannels) {
                val (channelName, _, _) = bytesPartition(
                    data.sliceArray(offset until data.size),
                    "\u0000"
                )
                offset += channelName.length + 1
                traceDd("\tChannel Name %3d: %s".format(i, channelName))
                fpDesc.addChannelName(channelName)
            }
            traceDd("unpackForcePlate processed ", offset, " bytes")
            return Pair(offset, fpDesc)
        } else {
            traceDd("unpackForcePlate processed ", offset, " bytes")
            return Pair(offset, null)
        }
    }

    private fun unpackDeviceDescription(data: ByteArray, major: Int, minor: Int): Pair<Int, DeviceDescription?> {
        var offset = 0
        if (major >= 3) {
            // newId
            val newId = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
            offset += 4
            traceDd("\tID : ", newId)

            // Name
            val (name, _, _) = bytesPartition(data.sliceArray(offset until data.size), "\u0000")
            offset += name.length + 1
            traceDd("\tName : ", name)

            // Serial Number
            val (serialNumber, _, _) = bytesPartition(data.sliceArray(offset until data.size), "\u0000")
            offset += serialNumber.length + 1
            traceDd("\tSerial Number : ", serialNumber)

            // Device Type int
            val deviceType = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
            offset += 4
            traceDd("Device Type : ", deviceType)

            // Channel Data Type int
            val channelDataType = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
            offset += 4
            traceDd("Channel Data Type : ", channelDataType)

            val deviceDesc = DeviceDescription(newId, name, serialNumber, deviceType, channelDataType)

            // Number of Channels int
            val numChannels = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
            offset += 4
            traceDd("Number of Channels ", numChannels)

            // Channel Names list of NoC strings
            for (i in 0 until numChannels) {
                val (channelName, _, _) = bytesPartition(
                    data.sliceArray(offset until data.size),
                    "\u0000"
                )
                offset += channelName.length + 1
                deviceDesc.addChannelName(channelName)
                traceDd("\tChannel ", i, " Name : ", channelName)
            }
            traceDd("unpackDeviceDescription processed ", offset, " bytes")
            return Pair(offset, deviceDesc)
        } else {
            traceDd("unpackDeviceDescription processed ", offset, " bytes")
            return Pair(offset, null)
        }
    }

    private fun unpackCameraDescription(data: ByteArray, major: Int, minor: Int): Pair<Int, CameraDescription> {
        var offset = 0
        // Name
        val (name, _, _) = bytesPartition(data.sliceArray(offset until data.size), "\u0000")
        offset += name.length + 1
        traceDd("\tName       : %s".format(name))
        // Position
        val position = Vector3.unpack(data.sliceArray(offset until offset + 12))
        offset += 12
        traceDd("\tPosition   : [%3.2f, %3.2f, %3.2f]".format(position[0], position[1], position[2]))

        // Orientation
        val orientation = Quaternion.unpack(data.sliceArray(offset until offset + 16))
        offset += 16
        traceDd(
            "\tOrientation: [%3.2f, %3.2f, %3.2f, %3.2f]".format(
                orientation[0],
                orientation[1],
                orientation[2],
                orientation[3]
            )
        )
        traceDd("unpackCameraDescription processed %3d bytes".format(offset))

        val cameraDesc = CameraDescription(name, position, orientation)
        return Pair(offset, cameraDesc)
    }

    // Unpack a data description packet
    private fun unpackDataDescriptions(
        data: ByteArray,
        packetSize: Int,
        major: Int,
        minor: Int
    ): Pair<Int, DataDescriptions?> {
        val dataDescs = DataDescriptions()
        var offset = 0
        // # of data sets to process
        val datasetCount = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
        offset += 4
        traceDd("Dataset Count : ", datasetCount)
        for (i in 0 until datasetCount) {
            traceDd("Dataset ", i)
            val dataType = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
            offset += 4
//            dataTmp=None
            when (dataType) {
                0 -> {
                    traceDd("Type: 0 Markerset")
                    val (offsetTmp, dataTmp) = unpackMarkerSetDescription(
                        data.sliceArray(offset until data.size),
                        major,
                        minor
                    )
                    offset += offsetTmp
                    dataDescs.addData(dataTmp)
                }

                1 -> {
                    traceDd("Type: 1 Rigid Body")
                    val (offsetTmp, dataTmp) = unpackRigidBodyDescription(
                        data.sliceArray(offset until data.size),
                        major,
                        minor
                    )
                    offset += offsetTmp
                    dataDescs.addData(dataTmp)
                }

                2 -> {
                    traceDd("Type: 2 Skeleton")
                    val (offsetTmp, dataTmp) = unpackSkeletonDescription(
                        data.sliceArray(offset until data.size),
                        major,
                        minor
                    )
                    offset += offsetTmp
                    dataDescs.addData(dataTmp)
                }

                3 -> {
                    traceDd("Type: 3 Force Plate")
                    val (offsetTmp, dataTmp) = unpackForcePlateDescription(
                        data.sliceArray(offset until data.size),
                        major,
                        minor
                    )
                    offset += offsetTmp
                    dataDescs.addData(dataTmp)
                }

                4 -> {
                    traceDd("Type: 4 Device")
                    val (offsetTmp, dataTmp) = unpackDeviceDescription(
                        data.sliceArray(offset until data.size),
                        major,
                        minor
                    )
                    offset += offsetTmp
                    dataDescs.addData(dataTmp)
                }

                5 -> {
                    traceDd("Type: 5 Camera")
                    val (offsetTmp, dataTmp) = unpackCameraDescription(
                        data.sliceArray(offset until data.size),
                        major,
                        minor
                    )
                    offset += offsetTmp
                    dataDescs.addData(dataTmp)
                }

                else -> {
                    println("Type: $dataType UNKNOWN")
                    println("ERROR: Type decode failure")
                    println("\t $(i + 1)  datasets processed of  $datasetCount")
                    println("\t $offset bytes processed of $packetSize")
                    println("\tPACKET DECODE STOPPED")
                    return Pair(offset, null)
                }
            }
            traceDd("\t$i datasets processed of $datasetCount")
            traceDd("\t $offset bytes processed of $packetSize")
        }
        // Send information to any listener.
        if (::dataDescriptionsListener.isInitialized) {
            dataDescriptionsListener.invoke(dataDescs)
        }
        return Pair(offset, dataDescs)
    }

    // unpackServerInfo is for local use of the client
    // and will update the values for the versions/ NatNet capabilities
    // of the server.
    private fun unpackServerInfo(data: ByteArray, packetSize: Int, major: Int, minor: Int): Int {
        var offset = 0
        // Server name
        //szName = data.sliceArray(offset until  offset+256)
        val (applicationNameTmp, _, _) = bytesPartition(
            data.sliceArray(offset until offset + 256),
            "\u0000"
        )
        applicationName = applicationNameTmp
        offset += 256
        // Server Version info
//        serverVersion = struct.unpack( 'BBBB', data.sliceArray(offset until offset+4) )
        val dataTmp = data.sliceArray(offset until offset + 4).asUByteArray()
        offset += 4
        serverVersion[0] = dataTmp[0].toInt()
        serverVersion[1] = dataTmp[1].toInt()
        serverVersion[2] = dataTmp[2].toInt()
        serverVersion[3] = dataTmp[3].toInt()

        // NatNet Version info
//        nnsvs = struct.unpack( 'BBBB', data.sliceArray(offset until offset+4) )
        val nnsvs = data.sliceArray(offset until offset + 4).asUByteArray()
        offset += 4
        natNetStreamVersionServer[0] = nnsvs[0].toInt()
        natNetStreamVersionServer[1] = nnsvs[1].toInt()
        natNetStreamVersionServer[2] = nnsvs[2].toInt()
        natNetStreamVersionServer[3] = nnsvs[3].toInt()
        if ((natNetRequestedVersion[0] == 0) && (natNetRequestedVersion[1] == 0)) {
            natNetRequestedVersion[0] = natNetStreamVersionServer[0]
            natNetRequestedVersion[1] = natNetStreamVersionServer[1]
            natNetRequestedVersion[2] = natNetStreamVersionServer[2]
            natNetRequestedVersion[3] = natNetStreamVersionServer[3]
            // Determine if the bitstream version can be changed
            if ((natNetStreamVersionServer[0] >= 4) && !useMulticast) {
                canChangeBitstreamVersion = true
            }
        }

        traceMf("Sending Application Name: ", applicationName)
        traceMf(
            "NatNetVersion ",
            natNetStreamVersionServer[0],
            " ",
            natNetStreamVersionServer[1],
            " ",
            natNetStreamVersionServer[2],
            " ",
            natNetStreamVersionServer[3]
        )

        traceMf(
            "ServerVersion ", serverVersion[0], " ", serverVersion[1], " ", serverVersion[2], " ", serverVersion[3]
        )
        return offset
    }

    private fun commandThreadFunction(inSocket: DatagramSocket, stop: () -> Boolean, gprintLevel: () -> Int): Int {
        val messageIdDict = mutableMapOf<String, Int>()
        if (!useMulticast) {
            inSocket.soTimeout = 2000
        }
        var data = ByteArray(0)
        // 64k buffer size
        val recvBufferSize = 64 * 1024
        val recvBuffer = ByteArray(recvBufferSize)
        while (!stop()) {
            // Block for input
            try {
                val datagramPacket = DatagramPacket(recvBuffer, recvBufferSize)
                inSocket.receive(datagramPacket)
                data = datagramPacket.data
                val addr = datagramPacket.address
            } catch (e: IOException) {
                if (stop()) {
//                    println("ERROR: command socket access error occurred:\n  %s".format(e.message))
//                    return 1
                    println("shutting down")
                }
            } catch (e: PortUnreachableException) {
                println("ERROR: command socket access PortUnreachableException occurred")
//                return 2
            } catch (e: IllegalBlockingModeException) {
                println("ERROR: command socket access IllegalBlockingModeException occurred")
//                return 3
            } catch (e: SocketTimeoutException) {
                if (useMulticast) {
                    println("ERROR: command socket access timeout occurred. Server not responding")
                    //return 4
                }
            }

            if (data.isNotEmpty()) {
                //peek ahead at messageId
                var messageId = getMessageId(data)
                val tmpStr = "mi_%1d".format(messageId)
                if (tmpStr !in messageIdDict) {
                    messageIdDict[tmpStr] = 0
                }
                messageIdDict[tmpStr] = messageIdDict[tmpStr]!! + 1

                printLevel = gprintLevel()
                if (messageId == NAT_FRAMEOFDATA) {
                    if (printLevel > 0) {
                        printLevel = if ((messageIdDict[tmpStr]?.rem(printLevel)) == 0) {
                            1
                        } else {
                            0
                        }
                    }
                }
                messageId = processMessage(data, printLevel)

                data = ByteArray(0)
            }

            if (!useMulticast) {
                if (!stop()) {
                    sendKeepAlive(inSocket, serverIpAddress, commandPort)
                }
            }
        }
        return 0
    }

    private fun dataThreadFunction(inSocket: DatagramSocket, stop: () -> Boolean, gprintLevel: () -> Int): Int {
        val messageIdDict = mutableMapOf<String, Int>()
        var data = ByteArray(0)
        // 64k buffer size
        val recvBufferSize = 64 * 1024
        val recvBuffer = ByteArray(recvBufferSize)

        while (!stop()) {
            // Block for input
            try {
                val datagramPacket = DatagramPacket(recvBuffer, recvBufferSize)
                inSocket.receive(datagramPacket)
                data = datagramPacket.data
                val addr = datagramPacket.address
            } catch (e: IOException) {
                if (!stop()) {
                    println("ERROR: data socket access error occurred:\n  %s".format(e.message))
                    return 1
                }
            } catch (e: PortUnreachableException) {
                println("ERROR: data socket access PortUnreachableException occurred")
//                return 2
            } catch (e: IllegalBlockingModeException) {
                println("ERROR: data socket access IllegalBlockingModeException occurred")
//                return 3
            } catch (e: SocketTimeoutException) {
//                if (useMulticast) {
                println("ERROR: data socket access timeout occurred. Server not responding")
//                    return 4
//                }
            }
            if (data.isNotEmpty()) {
                //peek ahead at messageId
                var messageId = getMessageId(data)
                val tmpStr = "mi_%1d".format(messageId)
                if (tmpStr !in messageIdDict) {
                    messageIdDict[tmpStr] = 0
                }
                messageIdDict[tmpStr] = messageIdDict[tmpStr]!! + 1

                printLevel = gprintLevel()
                if (messageId == NAT_FRAMEOFDATA) {
                    if (printLevel > 0) {
                        printLevel = if ((messageIdDict[tmpStr]?.rem(printLevel)) == 0) {
                            1
                        } else {
                            0
                        }
                    }
                }
                messageId = processMessage(data, printLevel)

                data = ByteArray(0)
            }
        }
        return 0
    }

    private fun processMessage(data: ByteArray, printLevel: Int = 0): Int {
        //return message ID
        val major = getMajor()
        val minor = getMinor()

        trace("Begin Packet\n-----------------")
        val showNatNetVersion = false
        if (showNatNetVersion) {
            trace(
                "NatNetVersion ",
                natNetRequestedVersion[0], " ",
                natNetRequestedVersion[1], " ",
                natNetRequestedVersion[2], " ",
                natNetRequestedVersion[3]
            )
        }

        val messageId = getMessageId(data)

        val packetSize = bytesToInt(data.sliceArray(2 until 4), ByteOrder.LITTLE_ENDIAN)

        //skip the 4 bytes for message ID and packetSize
        var offset = 4
        if (messageId == NAT_FRAMEOFDATA) {
            trace("Message ID  : %3d NAT_FRAMEOFDATA".format(messageId))
            trace("Packet Size : ", packetSize)

            val (offsetTmp, mocapData) = unpackMocapData(
                data.sliceArray(offset until data.size),
                packetSize,
                major,
                minor
            )
            offset += offsetTmp
            trace("MoCap Frame: %d\n".format(mocapData.prefixData.frameNumber)) //todo print->trace
            // get a string version of the data for output
            val mocapDataStr = mocapData.getAsString()
            if (printLevel >= 1) {
                println("%s\n".format(mocapDataStr))
            }

        } else if (messageId == NAT_MODELDEF) {
            trace("Message ID  : %3d NAT_MODELDEF".format(messageId))
            trace("Packet Size : %d".format(packetSize))
            val (offsetTmp, dataDescs) = unpackDataDescriptions(
                data.sliceArray(offset until data.size),
                packetSize,
                major,
                minor
            )
            offset += offsetTmp
            println("Data Descriptions:\n")
            // get a string version of the data for output
            val dataDescsStr = dataDescs?.getAsString()
            if (printLevel > 0) {
                println("%s\n".format(dataDescsStr))
            }

        } else if (messageId == NAT_SERVERINFO) {
            trace("Message ID  : %3d NAT_SERVERINFO".format(messageId))
            trace("Packet Size : ", packetSize)
            offset += unpackServerInfo(data.sliceArray(offset until data.size), packetSize, major, minor)

        } else if (messageId == NAT_RESPONSE) {
            trace("Message ID  : %3d NAT_RESPONSE".format(messageId))
            trace("Packet Size : ", packetSize)
            if (packetSize == 4) {
                val commandResponse = bytesToInt(data.sliceArray(offset until offset + 4), ByteOrder.LITTLE_ENDIAN)
                offset += 4
                trace("Command response: %d".format(commandResponse))
            } else {
                val showRemainder = false
                val (message, separator, remainder) = bytesPartition(data.sliceArray(offset until data.size), "\u0000")
                offset += message.length + 1
                if (showRemainder) {
                    trace("Command response:", message, " separator:", separator, " remainder:", remainder)
                } else {
                    trace("Command response:", message)
                }
            }
        } else if (messageId == NAT_UNRECOGNIZED_REQUEST) {
            trace("Message ID  : %3d NAT_UNRECOGNIZED_REQUEST: ".format(messageId))
            trace("Packet Size : ", packetSize)
            trace("Received 'Unrecognized request' from server")
        } else if (messageId == NAT_MESSAGESTRING) {
            trace("Message ID  : %3d NAT_MESSAGESTRING".format(messageId))
            trace("Packet Size : ", packetSize)
            val (message, _, _) = bytesPartition(data.sliceArray(offset until data.size), "\u0000")
            offset += message.length + 1
            trace("Received message from server:", message)
        } else {
            trace("Message ID  : %3d UNKNOWN".format(messageId))
            trace("Packet Size : ", packetSize)
            trace("ERROR: Unrecognized packet type")
        }

        trace("End Packet\n-----------------")
        return messageId
    }

    fun sendRequest(inSocket: DatagramSocket, command: Int, commandStr: String, address: SocketAddress): Int {
        // Compose the message in our known message format
        var commandStr = commandStr
        var packetSize = 0
        when (command) {
            NAT_REQUEST_MODELDEF, NAT_REQUEST_FRAMEOFDATA -> {
                packetSize = 0
                commandStr = ""
            }

            NAT_REQUEST -> {
                packetSize = commandStr.length + 1
            }

            NAT_CONNECT -> {
                commandStr = "Ping"
                packetSize = commandStr.length + 1
            }

            NAT_KEEPALIVE -> {
                packetSize = 0
                commandStr = ""
            }
        }

        var data = intToBytes(command, 2, ByteOrder.LITTLE_ENDIAN)
        data += intToBytes(packetSize, 2, ByteOrder.LITTLE_ENDIAN)

        data += commandStr.toByteArray()
        data += "\u0000".toByteArray()

        val datagramPacket = DatagramPacket(data, data.size, address)
        return try {
            inSocket.send(datagramPacket)
            data.size
        } catch (e: Exception) {
            -1
        }
    }

    fun sendCommand(commandStr: String): Int {
        var nTries = 3
        var retVal = -1
        while (nTries >= 1) {
            nTries -= 1
            retVal =
                sendRequest(commandSocket, NAT_REQUEST, commandStr, InetSocketAddress(serverIpAddress, commandPort))
            if ((retVal != -1)) {
                break
            }
        }
        return retVal

//        return sendRequest(dataSocket, NAT_REQUEST, commandStr, InetSocketAddress(serverIpAddress, commandPort))
    }

    fun sendCommands(tmpCommands: ArrayList<String>, printResults: Boolean = true) {
        for (szCommand in tmpCommands) {
            val returnCode = sendCommand(szCommand)
            if (printResults) {
                println("Command: %s - return_code: %d".format(szCommand, returnCode))
            }
        }
    }

    fun sendKeepAlive(inSocket: DatagramSocket, serverIpAddress: String, serverPort: Int): Int {
        return sendRequest(inSocket, NAT_KEEPALIVE, "", InetSocketAddress(serverIpAddress, serverPort))
    }

//    fun getCommandPort(): Int {
//        return commandPort
//    }

    fun getApplicationName(): String {
        return applicationName
    }

    fun getNatNetRequestedVersion(): ArrayList<Int> {
        return natNetRequestedVersion
    }

    fun getNatNetVersionServer(): ArrayList<Int> {
        return natNetStreamVersionServer
    }

    fun getServerVersion(): ArrayList<Int> {
        return serverVersion
    }

    fun run(): Boolean {
        // Create the data socket
        dataSocket = createDataSocket(dataPort)
        if (!::dataSocket.isInitialized) {
            println("Could not open data channel")
            return false
        }

        // Create the command socket
        commandSocket = createCommandSocket()
        if (!::commandSocket.isInitialized) {
            println("Could not open command channel")
            return false
        }
        isLocked = true

        stopThreads = false
        // Create a separate thread for receiving data packets
        dataThread = Thread { dataThreadFunction(dataSocket, { stopThreads }, { printLevel }) }
        dataThread.start()

        // Create a separate thread for receiving command packets
        commandThread = Thread { commandThreadFunction(commandSocket, { stopThreads }, { printLevel }) }
        commandThread.start()

        // Required for setup
        // Get NatNet and server versions
        sendRequest(commandSocket, NAT_CONNECT, "", InetSocketAddress(serverIpAddress, commandPort))

        //#Example Commands
        //# Get NatNet and server versions
//        sendRequest(commandSocket, NAT_CONNECT, "", InetSocketAddress(serverIpAddress, commandPort) )
        //# Request the model definitions
//        sendRequest(commandSocket, NAT_REQUEST_MODELDEF, "",  InetSocketAddress(serverIpAddress, commandPort) )
        return true
    }

    fun shutdown() {
        println("shutdown called")
        stopThreads = true
        // closing sockets causes blocking recvfrom to throw
        // an exception and break the loop
        commandSocket.close()
        dataSocket.close()
        // attempt to join the threads back.
        commandThread.join()
        dataThread.join()
    }
}



