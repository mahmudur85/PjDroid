package com.pjdroid.sample

import org.pjsip.pjsua2.*
import java.io.File
import java.util.*

/* Interface to separate UI & engine a bit better */
interface MyAppObserver {
    fun notifyRegState(code: Int, reason: String?, expiration: Long)
    fun notifyIncomingCall(call: MyCall?)
    fun notifyCallState(call: MyCall?)
    fun notifyCallMediaState(call: MyCall?)
    fun notifyBuddyState(buddy: MyBuddy?)
    fun notifyChangeNetwork()
}

internal class MyLogWriter : LogWriter() {
    override fun write(entry: LogEntry) {
        println(entry.msg)
    }
}

class MyCall(acc: MyAccount?, call_id: Int) : Call(acc, call_id) {
    var vidWin: VideoWindow? = null
    var vidPrev: VideoPreview? = null
    override fun onCallState(prm: OnCallStateParam) {
        try {
            val ci = info
            if (ci.state ==
                pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED
            ) {
                MyApp.ep?.utilLogWrite(3, "MyCall", dump(true, ""))
            }
        } catch (e: Exception) {
        }

        // Should not delete this call instance (self) in this context,
        // so the observer should manage this call instance deletion
        // out of this callback context.
        MyApp.observer?.notifyCallState(this)
    }

    override fun onCallMediaState(prm: OnCallMediaStateParam) {
        val ci: CallInfo
        ci = try {
            info
        } catch (e: Exception) {
            return
        }
        val cmiv = ci.media
        for (i in cmiv.indices) {
            val cmi = cmiv[i]
            if (cmi.type == pjmedia_type.PJMEDIA_TYPE_AUDIO &&
                (cmi.status ==
                        pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE ||
                        cmi.status ==
                        pjsua_call_media_status.PJSUA_CALL_MEDIA_REMOTE_HOLD)
            ) {
                // unfortunately, on Java too, the returned Media cannot be
                // downcasted to AudioMedia
                val m = getMedia(i.toLong())
                val am = AudioMedia.typecastFromMedia(m)

                // connect ports
                try {
                    MyApp.ep?.audDevManager()?.captureDevMedia?.startTransmit(am)
                    am.startTransmit(MyApp.ep?.audDevManager()?.playbackDevMedia)
                } catch (e: Exception) {
                    continue
                }
            } else if (cmi.type == pjmedia_type.PJMEDIA_TYPE_VIDEO && cmi.status ==
                pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE && cmi.videoIncomingWindowId != pjsua2.INVALID_ID
            ) {
                vidWin = VideoWindow(cmi.videoIncomingWindowId)
                vidPrev = VideoPreview(cmi.videoCapDev)
            }
        }
        MyApp.observer?.notifyCallMediaState(this)
    }
}

class MyAccount(var cfg: AccountConfig) : Account() {
    @JvmField
    var buddyList = ArrayList<MyBuddy>()
    fun addBuddy(bud_cfg: BuddyConfig): MyBuddy? {
        /* Create Buddy */
        var bud: MyBuddy? = MyBuddy(bud_cfg)
        try {
            bud?.create(this, bud_cfg)
        } catch (e: Exception) {
            bud?.delete()
            bud = null
        }
        if (bud != null) {
            buddyList.add(bud)
            if (bud_cfg.subscribe) try {
                bud.subscribePresence(true)
            } catch (e: Exception) {
            }
        }
        return bud
    }

    fun delBuddy(buddy: MyBuddy) {
        buddyList.remove(buddy)
        buddy.delete()
    }

    fun delBuddy(index: Int) {
        val bud = buddyList[index]
        buddyList.removeAt(index)
        bud.delete()
    }

    override fun onRegState(prm: OnRegStateParam) {
        MyApp.observer?.notifyRegState(
            prm.code, prm.reason,
            prm.expiration
        )
    }

    override fun onIncomingCall(prm: OnIncomingCallParam) {
        println("======== Incoming call ======== ")
        val call = MyCall(this, prm.callId)
        MyApp.observer?.notifyIncomingCall(call)
    }

    override fun onInstantMessage(prm: OnInstantMessageParam) {
        println("======== Incoming pager ======== ")
        println("From     : " + prm.fromUri)
        println("To       : " + prm.toUri)
        println("Contact  : " + prm.contactUri)
        println("Mimetype : " + prm.contentType)
        println("Body     : " + prm.msgBody)
    }
}

class MyBuddy(var cfg: BuddyConfig) : Buddy() {
    val statusText: String?
        get() {
            val bi: BuddyInfo
            bi = try {
                info
            } catch (e: Exception) {
                return "?"
            }
            var status: String? = ""
            if (bi.subState == pjsip_evsub_state.PJSIP_EVSUB_STATE_ACTIVE) {
                if (bi.presStatus.status ==
                    pjsua_buddy_status.PJSUA_BUDDY_STATUS_ONLINE
                ) {
                    status = bi.presStatus.statusText
                    if (status == null || status.length == 0) {
                        status = "Online"
                    }
                } else if (bi.presStatus.status ==
                    pjsua_buddy_status.PJSUA_BUDDY_STATUS_OFFLINE
                ) {
                    status = "Offline"
                } else {
                    status = "Unknown"
                }
            }
            return status
        }

    override fun onBuddyState() {
        MyApp.observer?.notifyBuddyState(this)
    }
}

internal class MyAccountConfig {
    var accCfg = AccountConfig()
    var buddyCfgs = ArrayList<BuddyConfig>()
    fun readObject(node: ContainerNode) {
        try {
            val acc_node = node.readContainer("Account")
            accCfg.readObject(acc_node)
            val buddies_node = acc_node.readArray("buddies")
            buddyCfgs.clear()
            while (buddies_node.hasUnread()) {
                val bud_cfg = BuddyConfig()
                bud_cfg.readObject(buddies_node)
                buddyCfgs.add(bud_cfg)
            }
        } catch (e: Exception) {
        }
    }

    fun writeObject(node: ContainerNode) {
        try {
            val acc_node = node.writeNewContainer("Account")
            accCfg.writeObject(acc_node)
            val buddies_node = acc_node.writeNewArray("buddies")
            for (j in buddyCfgs.indices) {
                buddyCfgs[j].writeObject(buddies_node)
            }
        } catch (e: Exception) {
        }
    }
}

class MyApp {

    companion object {
        var ep: Endpoint? = Endpoint()
        var observer: MyAppObserver? = null
    }

    @JvmField
    var accList = ArrayList<MyAccount?>()
    private val accCfgs = ArrayList<MyAccountConfig>()
    private val epConfig = EpConfig()
    private val sipTpConfig = TransportConfig()
    private var appDir: String? = null

    /* Maintain reference to log writer to avoid premature cleanup by GC */
    private var logWriter: MyLogWriter? = null
    private val configName = "pjsua2.json"
    private val SIP_PORT = 6000
    private val LOG_LEVEL = 4
    @JvmOverloads
    fun init(
        obs: MyAppObserver?, app_dir: String?,
        own_worker_thread: Boolean = false
    ) {
        observer = obs
        appDir = app_dir

        /* Create endpoint */try {
            ep?.libCreate()
        } catch (e: Exception) {
            return
        }


        /* Load config */
        val configPath = "$appDir/$configName"
        val f = File(configPath)
        if (f.exists()) {
            loadConfig(configPath)
        } else {
            /* Set 'default' values */
            sipTpConfig.port = SIP_PORT.toLong()
        }

        /* Override log level setting */epConfig.logConfig.level = LOG_LEVEL.toLong()
        epConfig.logConfig.consoleLevel = LOG_LEVEL.toLong()

        /* Set log config. */
        val log_cfg = epConfig.logConfig
        logWriter = MyLogWriter()
        log_cfg.writer = logWriter
        log_cfg.decor = log_cfg.decor and
                (pj_log_decoration.PJ_LOG_HAS_CR or
                        pj_log_decoration.PJ_LOG_HAS_NEWLINE).inv().toLong()

        /* Write log to file (just uncomment whenever needed) */
        //String log_path = android.os.Environment.getExternalStorageDirectory().toString();
        //log_cfg.setFilename(log_path + "/pjsip.log");

        /* Set ua config. */
        val ua_cfg = epConfig.uaConfig
        ua_cfg.userAgent = "Pjsua2 Android " + ep?.libVersion()?.full

        /* STUN server. */
        //StringVector stun_servers = new StringVector();
        //stun_servers.add("stun.pjsip.org");
        //ua_cfg.setStunServer(stun_servers);

        /* No worker thread */if (own_worker_thread) {
            ua_cfg.threadCnt = 0
            ua_cfg.mainThreadOnly = true
        }

        /* Init endpoint */try {
            ep?.libInit(epConfig)
        } catch (e: Exception) {
            return
        }

        /* Create transports. */try {
            ep?.transportCreate(
                pjsip_transport_type_e.PJSIP_TRANSPORT_UDP,
                sipTpConfig
            )
        } catch (e: Exception) {
            println(e)
        }
        try {
            ep?.transportCreate(
                pjsip_transport_type_e.PJSIP_TRANSPORT_TCP,
                sipTpConfig
            )
        } catch (e: Exception) {
            println(e)
        }
        try {
            sipTpConfig.port = (SIP_PORT + 1).toLong()
            ep?.transportCreate(
                pjsip_transport_type_e.PJSIP_TRANSPORT_TLS,
                sipTpConfig
            )
        } catch (e: Exception) {
            println(e)
        }

        /* Set SIP port back to default for JSON saved config */sipTpConfig.port = SIP_PORT.toLong()

        /* Create accounts. */for (i in accCfgs.indices) {
            val my_cfg = accCfgs[i]

            /* Customize account config */my_cfg.accCfg.natConfig.iceEnabled = true
            my_cfg.accCfg.videoConfig.autoTransmitOutgoing = true
            my_cfg.accCfg.videoConfig.autoShowIncoming = true
            val acc = addAcc(my_cfg.accCfg) ?: continue

            /* Add Buddies */for (j in my_cfg.buddyCfgs.indices) {
                val bud_cfg = my_cfg.buddyCfgs[j]
                acc.addBuddy(bud_cfg)
            }
        }

        /* Start. */try {
            ep?.libStart()
        } catch (e: Exception) {
            return
        }
    }

    fun addAcc(cfg: AccountConfig): MyAccount? {
        var acc: MyAccount? = MyAccount(cfg)
        try {
            acc?.create(cfg)
        } catch (e: Exception) {
            acc = null
            return null
        }
        accList.add(acc)
        return acc
    }

    fun delAcc(acc: MyAccount?) {
        accList.remove(acc)
    }

    private fun loadConfig(filename: String) {
        val json = JsonDocument()
        try {
            /* Load file */
            json.loadFile(filename)
            val root = json.rootContainer

            /* Read endpoint config */epConfig.readObject(root)

            /* Read transport config */
            val tp_node = root.readContainer("SipTransport")
            sipTpConfig.readObject(tp_node)

            /* Read account configs */accCfgs.clear()
            val accs_node = root.readArray("accounts")
            while (accs_node.hasUnread()) {
                val acc_cfg = MyAccountConfig()
                acc_cfg.readObject(accs_node)
                accCfgs.add(acc_cfg)
            }
        } catch (e: Exception) {
            println(e)
        }

        /* Force delete json now, as I found that Java somehow destroys it
         * after lib has been destroyed and from non-registered thread.
         */json.delete()
    }

    private fun buildAccConfigs() {
        /* Sync accCfgs from accList */
        accCfgs.clear()
        for (i in accList.indices) {
            val acc = accList[i]
            val my_acc_cfg = MyAccountConfig()
            acc?.let {
                my_acc_cfg.accCfg = it.cfg
            }
            my_acc_cfg.buddyCfgs.clear()
            acc?.let {
                for (j in it.buddyList.indices) {
                    val bud = it.buddyList[j]
                    my_acc_cfg.buddyCfgs.add(bud.cfg)
                }
            }

            accCfgs.add(my_acc_cfg)
        }
    }

    private fun saveConfig(filename: String) {
        val json = JsonDocument()
        try {
            /* Write endpoint config */
            json.writeObject(epConfig)

            /* Write transport config */
            val tp_node = json.writeNewContainer("SipTransport")
            sipTpConfig.writeObject(tp_node)

            /* Write account configs */buildAccConfigs()
            val accs_node = json.writeNewArray("accounts")
            for (i in accCfgs.indices) {
                accCfgs[i].writeObject(accs_node)
            }

            /* Save file */json.saveFile(filename)
        } catch (e: Exception) {
        }

        /* Force delete json now, as I found that Java somehow destroys it
         * after lib has been destroyed and from non-registered thread.
         */json.delete()
    }

    fun handleNetworkChange() {
        try {
            println("Network change detected")
            val changeParam = IpChangeParam()
            ep?.handleIpChange(changeParam)
        } catch (e: Exception) {
            println(e)
        }
    }

    fun deinit() {
        val configPath = "$appDir/$configName"
        saveConfig(configPath)

        /* Try force GC to avoid late destroy of PJ objects as they should be
         * deleted before lib is destroyed.
         */Runtime.getRuntime().gc()

        /* Shutdown pjsua. Note that Endpoint destructor will also invoke
         * libDestroy(), so this will be a test of double libDestroy().
         */try {
            ep?.libDestroy()
        } catch (e: Exception) {
        }

        /* Force delete Endpoint here, to avoid deletion from a non-
         * registered thread (by GC?).
         */ep?.delete()
        ep = null
    }
}