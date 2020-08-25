package com.bigyaa.datagramsocket

import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

internal object Quote {
    @JvmStatic
    fun main(args: Array<String>) {
        try {

            val person = AddressBookProtos.Person.newBuilder()
                .setId(1)
                .setLatitude("85")
                .setLongitude("25")
                .build()

            val addressbook = AddressBookProtos.AddressBook.newBuilder()
                .addAllPeople(listOf(person))
                .build()

            val bytes = addressbook.toByteArray()

//            PersonProto person =
            val udpSocket = DatagramSocket(2222)
            val serverAddr = InetAddress.getByName("68.183.91.197")
//            val buf = "The String to Send".toByteArray()
            val packet = DatagramPacket(bytes, bytes.size, serverAddr, 2222)
            udpSocket.send(packet)
            println("Connected and sent")
        } catch (ex: IOException) {
            println("Client error: " + ex.message)
            ex.printStackTrace()
        }
    }
}
