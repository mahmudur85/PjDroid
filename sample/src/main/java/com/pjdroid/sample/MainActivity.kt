package com.pjdroid.sample

import android.app.AlertDialog
import android.content.*
import android.content.pm.ApplicationInfo
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.Process
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AppCompatActivity
import com.pjdroid.sample.CallActivity
import org.pjsip.pjsua2.*
import java.util.*

class MainActivity : AppCompatActivity(), Handler.Callback, MyAppObserver {
    private var buddyListView: ListView? = null
    private var buddyListAdapter: SimpleAdapter? = null
    private var buddyListSelectedIdx = -1
    var buddyList: ArrayList<MutableMap<String, String?>>? = null
    private var lastRegStatus = ""
    private val handler = Handler(this)

    object MSG_TYPE {
        const val INCOMING_CALL = 1
        const val CALL_STATE = 2
        const val REG_STATE = 3
        const val BUDDY_STATE = 4
        const val CALL_MEDIA_STATE = 5
        const val CHANGE_NETWORK = 6
    }

    inner class MyBroadcastReceiver : BroadcastReceiver() {
        private var conn_name = ""
        override fun onReceive(context: Context, intent: Intent) {
            if (isNetworkChange(context)) notifyChangeNetwork()
        }

        private fun isNetworkChange(context: Context): Boolean {
            var network_changed = false
            val connectivity_mgr = context.getSystemService(
                CONNECTIVITY_SERVICE
            ) as ConnectivityManager
            val net_info = connectivity_mgr.activeNetworkInfo
            if (net_info != null && net_info.isConnectedOrConnecting &&
                !conn_name.equals("", ignoreCase = true)
            ) {
                val new_con = net_info.extraInfo
                if (new_con != null && !new_con.equals(
                        conn_name,
                        ignoreCase = true
                    )
                ) network_changed = true
                conn_name = new_con ?: ""
            } else {
                if (conn_name.equals("", ignoreCase = true)) conn_name = net_info!!.extraInfo
            }
            return network_changed
        }
    }

    private fun putData(uri: String, status: String?): HashMap<String, String?> {
        val item = HashMap<String, String?>()
        item["uri"] = uri
        item["status"] = status
        return item
    }

    private fun showCallActivity() {
        val intent = Intent(this, CallActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (app == null) {
            app = MyApp()
            // Wait for GDB to init, for native debugging only
            if (false &&
                applicationInfo.flags and
                ApplicationInfo.FLAG_DEBUGGABLE != 0
            ) {
                try {
                    Thread.sleep(5000)
                } catch (e: InterruptedException) {
                }
            }
            app!!.init(this, filesDir.absolutePath)
        }
        if (app!!.accList.size == 0) {
            accCfg = AccountConfig()
            accCfg!!.idUri = "sip:localhost"
            accCfg!!.natConfig.iceEnabled = true
            accCfg!!.videoConfig.autoTransmitOutgoing = true
            accCfg!!.videoConfig.autoShowIncoming = true
            account = app!!.addAcc(accCfg!!)
        } else {
            account = app!!.accList[0]
            accCfg = account!!.cfg
        }
        buddyList = ArrayList()
        for (i in account!!.buddyList.indices) {
            buddyList!!.add(
                putData(
                    account!!.buddyList[i].cfg.uri,
                    account!!.buddyList[i].statusText
                )
            )
        }
        val from = arrayOf("uri", "status")
        val to = intArrayOf(android.R.id.text1, android.R.id.text2)
        buddyListAdapter = SimpleAdapter(
            this, buddyList,
            android.R.layout.simple_list_item_2,
            from, to
        )
        buddyListView = findViewById<View>(R.id.listViewBuddy) as ListView
        buddyListView!!.adapter = buddyListAdapter
        buddyListView!!.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            view.isSelected = true
            buddyListSelectedIdx = position
        }
        if (receiver == null) {
            receiver = MyBroadcastReceiver()
            intentFilter = IntentFilter(
                ConnectivityManager.CONNECTIVITY_ACTION
            )
            registerReceiver(receiver, intentFilter)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar
        // if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_acc_config -> dlgAccountSetting()
            R.id.action_quit -> {
                val m = Message.obtain(handler, 0)
                m.sendToTarget()
            }
            else -> {
            }
        }
        return true
    }

    override fun handleMessage(m: Message): Boolean {
        if (m.what == 0) {
            app!!.deinit()
            finish()
            Runtime.getRuntime().gc()
            Process.killProcess(Process.myPid())
        } else if (m.what == MSG_TYPE.CALL_STATE) {
            val ci = m.obj as CallInfo
            if (currentCall == null || ci == null || ci.id != currentCall!!.id) {
                println("Call state event received, but call info is invalid")
                return true
            }

            /* Forward the call info to CallActivity */if (CallActivity.handler_ != null) {
                val m2 = Message.obtain(CallActivity.handler_, MSG_TYPE.CALL_STATE, ci)
                m2.sendToTarget()
            }
            if (ci.state == pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED) {
                currentCall!!.delete()
                currentCall = null
            }
        } else if (m.what == MSG_TYPE.CALL_MEDIA_STATE) {

            /* Forward the message to CallActivity */
            if (CallActivity.handler_ != null) {
                val m2 = Message.obtain(
                    CallActivity.handler_,
                    MSG_TYPE.CALL_MEDIA_STATE,
                    null
                )
                m2.sendToTarget()
            }
        } else if (m.what == MSG_TYPE.BUDDY_STATE) {
            val buddy = m.obj as MyBuddy
            val idx = account!!.buddyList.indexOf(buddy)

            /* Update buddy status text, if buddy is valid and
             * the buddy lists in account and UI are sync-ed.
             */if (idx >= 0 && account!!.buddyList.size == buddyList!!.size) {
                buddyList!![idx]["status"] = buddy.statusText
                buddyListAdapter!!.notifyDataSetChanged()
                // TODO: selection color/mark is gone after this,
                //       dont know how to return it back.
                //buddyListView.setSelection(buddyListSelectedIdx);
                //buddyListView.performItemClick(buddyListView,
                //				     buddyListSelectedIdx,
                //				     buddyListView.
                //		    getItemIdAtPosition(buddyListSelectedIdx));

                /* Return back Call activity */notifyCallState(currentCall)
            }
        } else if (m.what == MSG_TYPE.REG_STATE) {
            val msg_str = m.obj as String
            lastRegStatus = msg_str
        } else if (m.what == MSG_TYPE.INCOMING_CALL) {

            /* Incoming call */
            val call = m.obj as MyCall
            val prm = CallOpParam()

            /* Only one call at anytime */if (currentCall != null) {
                /*
		prm.setStatusCode(pjsip_status_code.PJSIP_SC_BUSY_HERE);
		try {
		call.hangup(prm);
		} catch (Exception e) {}
		*/
                // TODO: set status code
                call.delete()
                return true
            }

            /* Answer with ringing */prm.statusCode = pjsip_status_code.PJSIP_SC_RINGING
            try {
                call.answer(prm)
            } catch (e: Exception) {
            }
            currentCall = call
            showCallActivity()
        } else if (m.what == MSG_TYPE.CHANGE_NETWORK) {
            app!!.handleNetworkChange()
        } else {

            /* Message not handled */
            return false
        }
        return true
    }

    private fun dlgAccountSetting() {
        val li = LayoutInflater.from(this)
        val view = li.inflate(R.layout.dlg_account_config, null)
        if (lastRegStatus.length != 0) {
            val tvInfo = view.findViewById<View>(R.id.textViewInfo) as TextView
            tvInfo.text = "Last status: $lastRegStatus"
        }
        val adb = AlertDialog.Builder(this)
        adb.setView(view)
        adb.setTitle("Account Settings")
        val etId = view.findViewById<View>(R.id.editTextId) as EditText
        val etReg = view.findViewById<View>(R.id.editTextRegistrar) as EditText
        val etProxy = view.findViewById<View>(R.id.editTextProxy) as EditText
        val etUser = view.findViewById<View>(R.id.editTextUsername) as EditText
        val etPass = view.findViewById<View>(R.id.editTextPassword) as EditText
        etId.setText(accCfg!!.idUri)
        etReg.setText(accCfg!!.regConfig.registrarUri)
        val proxies = accCfg!!.sipConfig.proxies
        if (proxies.size > 0) etProxy.setText(proxies[0]) else etProxy.setText("")
        val creds = accCfg!!.sipConfig.authCreds
        if (creds.size > 0) {
            etUser.setText(creds[0].username)
            etPass.setText(creds[0].data)
        } else {
            etUser.setText("")
            etPass.setText("")
        }
        adb.setCancelable(false)
        adb.setPositiveButton(
            "OK"
        ) { dialog, id ->
            val acc_id = etId.text.toString()
            val registrar = etReg.text.toString()
            val proxy = etProxy.text.toString()
            val username = etUser.text.toString()
            val password = etPass.text.toString()
            accCfg!!.idUri = acc_id
            accCfg!!.regConfig.registrarUri = registrar
            val creds = accCfg!!.sipConfig.authCreds
            creds.clear()
            if (username.length != 0) {
                creds.add(
                    AuthCredInfo(
                        "Digest", "*", username, 0,
                        password
                    )
                )
            }
            val proxies = accCfg!!.sipConfig.proxies
            proxies.clear()
            if (proxy.length != 0) {
                proxies.add(proxy)
            }

            /* Enable ICE */accCfg!!.natConfig.iceEnabled = true

            /* Finally */lastRegStatus = ""
            try {
                account!!.modify(accCfg)
            } catch (e: Exception) {
            }
        }
        adb.setNegativeButton(
            "Cancel"
        ) { dialog, id -> dialog.cancel() }
        val ad = adb.create()
        ad.show()
    }

    fun makeCall(view: View?) {
        if (buddyListSelectedIdx == -1) return

        /* Only one call at anytime */if (currentCall != null) {
            return
        }
        val item =
            buddyListView!!.getItemAtPosition(buddyListSelectedIdx) as HashMap<String, String>
        val buddy_uri = item["uri"]
        val call = MyCall(account, -1)
        val prm = CallOpParam(true)
        try {
            call.makeCall(buddy_uri, prm)
        } catch (e: Exception) {
            call.delete()
            return
        }
        currentCall = call
        showCallActivity()
    }

    private fun dlgAddEditBuddy(initial: BuddyConfig?) {
        val cfg = BuddyConfig()
        val is_add = initial == null
        val li = LayoutInflater.from(this)
        val view = li.inflate(R.layout.dlg_add_buddy, null)
        val adb = AlertDialog.Builder(this)
        adb.setView(view)
        val etUri = view.findViewById<View>(R.id.editTextUri) as EditText
        val cbSubs = view.findViewById<View>(R.id.checkBoxSubscribe) as CheckBox
        if (is_add) {
            adb.setTitle("Add Buddy")
        } else {
            adb.setTitle("Edit Buddy")
            etUri.setText(initial!!.uri)
            cbSubs.isChecked = initial.subscribe
        }
        adb.setCancelable(false)
        adb.setPositiveButton(
            "OK"
        ) { dialog, id ->
            cfg.uri = etUri.text.toString()
            cfg.subscribe = cbSubs.isChecked
            if (is_add) {
                account!!.addBuddy(cfg)
                buddyList!!.add(putData(cfg.uri, ""))
                buddyListAdapter!!.notifyDataSetChanged()
                buddyListSelectedIdx = -1
            } else {
                if (initial!!.uri != cfg.uri) {
                    account!!.delBuddy(buddyListSelectedIdx)
                    account!!.addBuddy(cfg)
                    buddyList!!.removeAt(buddyListSelectedIdx)
                    buddyList!!.add(putData(cfg.uri, ""))
                    buddyListAdapter!!.notifyDataSetChanged()
                    buddyListSelectedIdx = -1
                } else if (initial.subscribe !=
                    cfg.subscribe
                ) {
                    val bud = account!!.buddyList[buddyListSelectedIdx]
                    try {
                        bud.subscribePresence(cfg.subscribe)
                    } catch (e: Exception) {
                    }
                }
            }
        }
        adb.setNegativeButton(
            "Cancel"
        ) { dialog, id -> dialog.cancel() }
        val ad = adb.create()
        ad.show()
    }

    fun addBuddy(view: View?) {
        dlgAddEditBuddy(null)
    }

    fun editBuddy(view: View?) {
        if (buddyListSelectedIdx == -1) return
        val old_cfg = account!!.buddyList[buddyListSelectedIdx].cfg
        dlgAddEditBuddy(old_cfg)
    }

    fun delBuddy(view: View?) {
        if (buddyListSelectedIdx == -1) return
        val item =
            buddyListView!!.getItemAtPosition(buddyListSelectedIdx) as HashMap<String, String?>
        val buddy_uri = item["uri"]
        val ocl = DialogInterface.OnClickListener { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    account!!.delBuddy(buddyListSelectedIdx)
                    buddyList!!.remove(item)
                    buddyListAdapter!!.notifyDataSetChanged()
                    buddyListSelectedIdx = -1
                }
                DialogInterface.BUTTON_NEGATIVE -> {
                }
            }
        }
        val adb = AlertDialog.Builder(this)
        adb.setTitle(buddy_uri)
        adb.setMessage("\nDelete this buddy?\n")
        adb.setPositiveButton("Yes", ocl)
        adb.setNegativeButton("No", ocl)
        adb.show()
    }

    /*
     * === MyAppObserver ===
     *
     * As we cannot do UI from worker thread, the callbacks mostly just send
     * a message to UI/main thread.
     */
    override fun notifyIncomingCall(call: MyCall?) {
        val m = Message.obtain(handler, MSG_TYPE.INCOMING_CALL, call)
        m.sendToTarget()
    }

    override fun notifyRegState(
        code: Int, reason: String?,
        expiration: Long
    ) {
        var msg_str = ""
        msg_str += if (expiration == 0L) "Unregistration" else "Registration"
        msg_str += if (code / 100 == 2) " successful" else " failed: $reason"
        val m = Message.obtain(handler, MSG_TYPE.REG_STATE, msg_str)
        m.sendToTarget()
    }

    override fun notifyCallState(call: MyCall?) {
        if (currentCall == null || call!!.id != currentCall!!.id) return
        var ci: CallInfo? = null
        try {
            ci = call.info
        } catch (e: Exception) {
        }
        if (ci != null) return
        val m = Message.obtain(handler, MSG_TYPE.CALL_STATE, ci)
        m.sendToTarget()
    }

    override fun notifyCallMediaState(call: MyCall?) {
        val m = Message.obtain(handler, MSG_TYPE.CALL_MEDIA_STATE, null)
        m.sendToTarget()
    }

    override fun notifyBuddyState(buddy: MyBuddy?) {
        val m = Message.obtain(handler, MSG_TYPE.BUDDY_STATE, buddy)
        m.sendToTarget()
    }

    override fun notifyChangeNetwork() {
        val m = Message.obtain(handler, MSG_TYPE.CHANGE_NETWORK, null)
        m.sendToTarget()
    } /* === end of MyAppObserver ==== */

    companion object {
        var app: MyApp? = null
        var currentCall: MyCall? = null
        var account: MyAccount? = null
        var accCfg: AccountConfig? = null
        var receiver: MyBroadcastReceiver? = null
        var intentFilter: IntentFilter? = null
    }
}