package jp.ac.titech.e.sc.hfg.mobilenatnet

import DataDescriptions
import NatNetClient
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import com.google.android.material.snackbar.Snackbar
import jp.ac.titech.e.sc.hfg.mobilenatnet.R
import jp.ac.titech.e.sc.hfg.mobilenatnet.databinding.FragmentMonitorBinding
import kotlin.math.asin
import kotlin.math.atan2


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class MonitorFragment : Fragment() {

    private var _binding: FragmentMonitorBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var natNetSetting: MainActivity.NatNetSetting? = null
    private val rigidBodyText = RigidBodyText()
    private var rigidBodyNameMap = mutableMapOf<Int, String>()
    private var streamingClient: NatNetClient? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        natNetSetting = savedInstanceState?.getParcelable(SETTING_KEY)
        // Use the Kotlin extension in the fragment-ktx artifact
        setFragmentResultListener("requestKey") { _, bundle ->
            natNetSetting = bundle.getParcelable("bundleKey")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(SETTING_KEY, natNetSetting)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentMonitorBinding.inflate(inflater, container, false)
        rigidBodyText.bindTextView(binding.textMonitor)
        return binding.root

    }

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonStart.setOnClickListener {
            val setting = natNetSetting
            if (setting == null) {
                Snackbar.make(view, R.string.no_setting, Snackbar.LENGTH_LONG)
                    .setAnchorView(R.id.fab).show()
                return@setOnClickListener
            }
            if (streamingClient != null) {
                Snackbar.make(view, R.string.already_connected, Snackbar.LENGTH_LONG)
                    .setAnchorView(R.id.fab).show()
                return@setOnClickListener
            }
            val client = NatNetClient()
            streamingClient = client
            if (setting.useMulticast) {
                acquireMulticastLock()
            }
            Thread {
                client.localIpAddress = setting.clientAddress
                client.serverIpAddress = setting.serverAddress
                client.useMulticast = setting.useMulticast
                client.multicastAddress = setting.multicastAddress
                client.rigidBodyListener = { id: Int, pos: ArrayList<Double>, rot: ArrayList<Double> ->
                        rigidBodyText.getRigidBody(id, pos, rot)
                }
                client.dataDescriptionsListener = { dataDescs: DataDescriptions ->
                    dataDescs.rigidBodyList.forEach { rigidBodyNameMap[it.idNum] = it.szName }
                    rigidBodyText.rigidBodyNameMap = rigidBodyNameMap
                }
                if (!client.run()) {
                    Handler(Looper.getMainLooper()).post {
                        streamingClient = null
                        releaseMulticastLock()
                        _binding?.let {
                            Snackbar.make(it.root, R.string.connect_failed, Snackbar.LENGTH_LONG)
                                .setAnchorView(R.id.fab).show()
                        }
                    }
                }
            }.start()
            Snackbar.make(
                view,
                "Connect with IP setting ${setting.clientAddress}, ${setting.serverAddress}, ${setting.multicastAddress}, use multicast: ${setting.useMulticast}",
                Snackbar.LENGTH_LONG
            ).setAnchorView(R.id.fab).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rigidBodyText.unbindTextView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        val client = streamingClient
        streamingClient = null
        if (client != null) {
            Thread {
                try {
                    client.shutdown()
                } catch (e: Exception) {
                    Log.w("NatNet", "Error while shutting down NatNet client", e)
                }
            }.start()
        }
        releaseMulticastLock()
    }

    private fun acquireMulticastLock() {
        if (multicastLock == null) {
            val wifiManager =
                requireContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            multicastLock = wifiManager.createMulticastLock("MobileNatNet").apply {
                setReferenceCounted(false)
                acquire()
            }
        }
    }

    private fun releaseMulticastLock() {
        multicastLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        multicastLock = null
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun printConfiguration(natNetClient: NatNetClient) {
        println("Connection Configuration:")
        println("  Client:          %s".format(natNetClient.localIpAddress))
        println("  Server:          %s".format(natNetClient.serverIpAddress))
        println("  Command Port:    %d".format(natNetClient.commandPort))
        println("  Data Port:       %d".format(natNetClient.dataPort))

        if (natNetClient.useMulticast) {
            println("  Using Multicast")
            println("  Multicast Group: %s".format(natNetClient.multicastAddress))
        } else {
            println("  Using Unicast")
        }

//        NatNet Server Info
        val applicationName = natNetClient.getApplicationName()
        val natNetRequestedVersion = natNetClient.getNatNetRequestedVersion()
        val natNetVersionServer = natNetClient.getNatNetVersionServer()
        val serverVersion = natNetClient.getServerVersion()

        println("  NatNet Server Info")
        println("    Application Name %s".format(applicationName))
        println(
            "    NatNetVersion  %d %d %d %d".format(
                natNetVersionServer[0],
                natNetVersionServer[1],
                natNetVersionServer[2],
                natNetVersionServer[3]
            )
        )
        println(
            "    ServerVersion  %d %d %d %d".format(
                serverVersion[0],
                serverVersion[1],
                serverVersion[2],
                serverVersion[3]
            )
        )
        println("  NatNet Bitstream Requested")
        println(
            "    NatNetVersion  %d %d %d %d".format(
                natNetRequestedVersion[0], natNetRequestedVersion[1],
                natNetRequestedVersion[2], natNetRequestedVersion[3]
            )
        )
    }

    private data class RigidBodyData(
        var id: Int,
        var name: String?,
        var pos: ArrayList<Double>,
        var rot: ArrayList<Double>,
    )

    class RigidBodyText {
        private var rigidBodyMap = mutableMapOf<Int, RigidBodyData>()
        var rigidBodyNameMap = mutableMapOf<Int, String>()
        private var text: TextView? = null
        private val mainHandler = Handler(Looper.getMainLooper())

        fun bindTextView(textView: TextView) {
            text = textView
        }

        fun unbindTextView() {
            text = null
        }

        fun getRigidBody(newId: Int, pos: ArrayList<Double>, rot: ArrayList<Double>) {
            if (newId in rigidBodyMap.keys) {
                rigidBodyMap[newId]?.pos = pos
                rigidBodyMap[newId]?.rot = rot
                Log.d("NatNet", "Update map")
            } else if (newId !in rigidBodyMap.keys) {
                val rigidBodyData = RigidBodyData(newId, getNameFromId(newId), pos, rot)
                rigidBodyMap[newId] = rigidBodyData
                Log.d("NatNet", "Create map, newId: $newId, dict: ${rigidBodyMap.keys}")
            }
            showAsText()
        }
        private fun getNameFromId(id: Int): String {
            return rigidBodyNameMap[id] ?: "Unknown"
        }

        // NatNet quaternion [qx, qy, qz, qw] -> intrinsic ZYX Euler angles [roll, pitch, yaw] in degrees
        private fun quaternionToEulerDegrees(q: ArrayList<Double>): DoubleArray {
            val (qx, qy, qz) = q
            val qw = q[3]
            val roll = atan2(2 * (qw * qx + qy * qz), 1 - 2 * (qx * qx + qy * qy))
            val pitch = asin((2 * (qw * qy - qz * qx)).coerceIn(-1.0, 1.0))
            val yaw = atan2(2 * (qw * qz + qx * qy), 1 - 2 * (qy * qy + qz * qz))
            return doubleArrayOf(
                Math.toDegrees(roll), Math.toDegrees(pitch), Math.toDegrees(yaw)
            )
        }

        private fun showAsText() {
            var outStr = ""
            for (i in rigidBodyMap.keys) {
                val data = rigidBodyMap[i]
                data?.name = getNameFromId(i)
                outStr += data?.let {
                    val euler = quaternionToEulerDegrees(it.rot)
                    "%s, id: %2d, position: %2.2f, %2.2f, %2.2f, rotation: %3.1f°, %3.1f°, %3.1f°\n".format(
                        it.name, it.id,
                        it.pos[0], it.pos[1], it.pos[2],
                        euler[0], euler[1], euler[2]
                    )
                }
            }
            Log.d("NatNet", outStr.length.toString())
            mainHandler.post { text?.text = outStr }
        }
    }

    companion object {
        private const val SETTING_KEY = "natNetSetting"
    }
}
