import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

/**
 * This program demonstrates how to implement a UDP client program.
 *
 *
 * @author www.codejava.net
 */
object QuoteClient {
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size < 2) {
            println("Syntax: QuoteClient <hostname> <port>")
            return
        }
        val hostname = args[0]
        val port = args[1].toInt()
        try {
            val address = InetAddress.getByName(hostname)
            val socket = DatagramSocket()
            while (true) {
                val request = DatagramPacket(ByteArray(1), 1, address, port)
                socket.send(request)
                val buffer = ByteArray(512)
                val response = DatagramPacket(buffer, buffer.size)
                socket.receive(response)
                val quote = String(buffer, 0, response.length)
                println(quote)
                println()
                Thread.sleep(10000)
            }
        } catch (ex: SocketTimeoutException) {
            println("Timeout error: " + ex.message)
            ex.printStackTrace()
        } catch (ex: IOException) {
            println("Client error: " + ex.message)
            ex.printStackTrace()
        } catch (ex: InterruptedException) {
            ex.printStackTrace()
        }
    }
}