package com.hamedvpn.vpngit.service

import android.content.Context
import com.hamedvpn.vpngit.core.CoreConfigManager
import com.hamedvpn.vpngit.core.CoreNativeManager
import com.hamedvpn.vpngit.dto.RealPingEvent
import com.hamedvpn.vpngit.enums.EConfigType
import com.hamedvpn.vpngit.extension.isComplexType
import com.hamedvpn.vpngit.extension.isNotNullEmpty
import com.hamedvpn.vpngit.handler.MmkvManager
import com.hamedvpn.vpngit.handler.SettingsManager
import com.hamedvpn.vpngit.handler.SpeedtestManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class RealPingWorkerService(
    private val context: Context,
    private val guids: List<String>,
    private val onEvent: (RealPingEvent) -> Unit = {}
) {
    private val job = SupervisorJob()
    private val concurrency = SettingsManager.getRealPingConcurrency()
    private val dispatcher = Executors.newFixedThreadPool(concurrency).asCoroutineDispatcher()
    private val scope = CoroutineScope(job + dispatcher + CoroutineName("RealPingBatchWorker"))

    private val runningCount = AtomicInteger(0)
    private val totalCount = AtomicInteger(0)

    fun start() {
        val jobs = guids.map { guid ->
            totalCount.incrementAndGet()
            scope.launch {
                runningCount.incrementAndGet()
                try {
                    val result = startRealPing(guid)
                    onEvent(RealPingEvent.Result(guid, result))
                } catch (_: Throwable) {

                } finally {
                    val count = totalCount.decrementAndGet()
                    val left = runningCount.decrementAndGet()
                    onEvent(RealPingEvent.Progress("$left / $count"))
                }
            }
        }

        scope.launch {
            try {
                joinAll(*jobs.toTypedArray())
                onEvent(RealPingEvent.Finish("0"))
            } catch (_: CancellationException) {
                onEvent(RealPingEvent.Finish("-1"))
            } finally {
                close()
            }
        }
    }

    fun cancel() {
        job.cancel()
    }

    private fun close() {
        try {
            dispatcher.close()
        } catch (_: Throwable) {

        }
    }

    private fun startRealPing(guid: String): Long {
        val retFailure = -1L

        val config = MmkvManager.decodeServerConfig(guid) ?: return retFailure
        if (!config.configType.isComplexType()
            && config.configType != EConfigType.HYSTERIA2
            && config.server.isNotNullEmpty()
            && config.serverPort?.toIntOrNull() != null
        ) {
            val url = config.server.orEmpty()
            val port = config.serverPort.orEmpty().toInt()
            val tcpTime = SpeedtestManager.socketConnectTime(url, port, 1000)
            if (tcpTime <= -1L) {
                return retFailure
            }
        }

        val configResult = CoreConfigManager.getV2rayConfig4Speedtest(context, guid)
        if (!configResult.status) {
            return retFailure
        }
        return CoreNativeManager.measureOutboundDelay(configResult.content, SettingsManager.getDelayTestUrl())
    }
}

