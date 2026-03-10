import java.io.PrintWriter
import java.net.Socket
import java.net.ConnectException
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class XPressionClient(
    private val host: String,
    private val port: Int = 8877,
    private val retryDelayMs: Long = 3000,
    private val maxRetries: Int = 10
) : AutoCloseable {

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    init {
        connect()
    }

    // --------------------------------------------------------
    // Connection management
    // --------------------------------------------------------

    private fun connect() {
        var attempt = 0
        while (attempt < maxRetries) {
            attempt++
            log("Attempting to connect to XPression at $host:$port (attempt $attempt/$maxRetries)...")
            try {
                socket = Socket(host, port)
                writer = PrintWriter(socket!!.getOutputStream(), true)
                log("✓ Connected to XPression at $host:$port")
                return
            } catch (e: ConnectException) {
                log("✗ Connection failed: ${e.message}")
                if (attempt < maxRetries) {
                    log("Retrying in ${retryDelayMs}ms...")
                    Thread.sleep(retryDelayMs)
                } else {
                    log("✗ Max retries ($maxRetries) reached. Giving up.")
                    throw e
                }
            } catch (e: Exception) {
                log("✗ Unexpected error during connect: ${e.javaClass.simpleName}: ${e.message}")
                throw e
            }
        }
    }

    fun reconnect() {
        log("Reconnect requested. Closing existing connection...")
        safeClose()
        connect()
    }

    private fun safeClose() {
        try {
            writer?.close()
            log("Writer closed.")
        } catch (e: Exception) {
            log("Warning: error closing writer: ${e.message}")
        }
        try {
            socket?.close()
            log("Socket closed.")
        } catch (e: Exception) {
            log("Warning: error closing socket: ${e.message}")
        }
        writer = null
        socket = null
    }

    // --------------------------------------------------------
    // Send
    // --------------------------------------------------------

    private fun send(command: String) {
        val w = writer
        val s = socket

        if (w == null || s == null || s.isClosed) {
            log("✗ Cannot send '$command' — not connected. Attempting reconnect...")
            reconnect()
        }

        try {
            log("→ Sending: '$command'")
            writer!!.print("$command\r\n")
            writer!!.flush()

            if (writer!!.checkError()) {
                log("✗ PrintWriter reported an error after sending '$command'")
            } else {
                log("✓ Sent OK: '$command'")
            }
        } catch (e: Exception) {
            log("✗ Send failed for '$command': ${e.javaClass.simpleName}: ${e.message}")
            throw e
        }
    }

    // --------------------------------------------------------
    // Logging
    // --------------------------------------------------------

    private fun log(message: String) {
        val time = LocalTime.now().format(timeFormat)
        println("[$time] [XPressionClient] $message")
    }

    // --------------------------------------------------------
    // Sequencer
    // --------------------------------------------------------

    fun seqi(takeId: Int, layer: Int) {
        log("seqi() called with takeId=$takeId, layer=$layer")
        send("SEQI ${takeId.pad()}:$layer")
    }

    fun seqo(takeId: Int) {
        log("seqo() called with takeId=$takeId")
        send("SEQO ${takeId.pad()}")
    }

    fun next() {
        log("next() called")
        send("NEXT")
    }

    fun read() {
        log("read() called")
        send("READ")
    }

    fun up() {
        log("up() called")
        send("UP")
    }

    fun down() {
        log("down() called")
        send("DOWN")
    }

    fun focus(takeId: Int) {
        log("focus() called with takeId=$takeId")
        send("FOCUS ${takeId.pad()}")
    }

    fun upNext(takeId: Int) {
        log("upNext() called with takeId=$takeId")
        send("UPNEXT ${takeId.pad()}")
    }

    // --------------------------------------------------------
    // Take / Cue
    // --------------------------------------------------------

    fun take(takeId: Int, buffer: Int, layer: Int) {
        log("take() called with takeId=$takeId, buffer=$buffer, layer=$layer")
        send("TAKE $takeId:$buffer:$layer")
    }

    fun cue(takeId: Int, buffer: Int, layer: Int) {
        log("cue() called with takeId=$takeId, buffer=$buffer, layer=$layer")
        send("CUE $takeId:$buffer:$layer")
    }

    fun swap(buffer: Int? = null) {
        log("swap() called with buffer=${buffer ?: "none (global swap)"}")
        send(if (buffer != null) "SWAP $buffer" else "SWAP")
    }

    fun uncue(takeId: Int) {
        log("uncue() called with takeId=$takeId")
        send("UNCUE ${takeId.pad()}")
    }

    fun uncueAll() {
        log("uncueAll() called")
        send("UNCUEALL")
    }

    // --------------------------------------------------------
    // Framebuffer / Layer
    // --------------------------------------------------------

    fun clearFramebuffer(buffer: Int) {
        log("clearFramebuffer() called with buffer=$buffer")
        send("CLFB ${buffer.pad()}")
    }

    fun clearLayer(buffer: Int, layer: Int) {
        log("clearLayer() called with buffer=$buffer, layer=$layer")
        send("CLFB ${buffer.pad()}:$layer")
    }

    fun clearAll() {
        log("clearAll() called")
        send("CLRA")
    }

    fun layerOff(buffer: Int, layer: Int) {
        log("layerOff() called with buffer=$buffer, layer=$layer")
        send("LAYEROFF ${buffer.pad()}:$layer")
    }

    fun resume(buffer: Int) {
        log("resume() called with buffer=$buffer")
        send("RESUME ${buffer.pad()}")
    }

    fun resumeLayer(buffer: Int, layer: Int) {
        log("resumeLayer() called with buffer=$buffer, layer=$layer")
        send("RESUME ${buffer.pad()}:$layer")
    }

    // --------------------------------------------------------
    // GPI
    // --------------------------------------------------------

    fun gpi(input: Int) {
        log("gpi() called with input=$input")
        send("GPI $input")
    }

    // --------------------------------------------------------
    // Helpers
    // --------------------------------------------------------

    private fun Int.pad() = toString().padStart(4, '0')

    override fun close() {
        log("close() called. Shutting down connection to $host:$port...")
        safeClose()
        log("✓ Connection closed.")
    }
}