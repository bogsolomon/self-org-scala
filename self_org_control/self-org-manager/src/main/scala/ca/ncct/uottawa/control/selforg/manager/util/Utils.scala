package ca.ncct.uottawa.control.selforg.manager.util

import java.net.{NetworkInterface, InetAddress}
import java.util

/**
  * Created by Bogdan on 7/22/2016.
  */
object Utils {
  def findEth0Address: String = {
    val interfaces = NetworkInterface.getNetworkInterfaces
    while (interfaces.hasMoreElements) {
      val element = interfaces.nextElement
      if (element.getDisplayName.equalsIgnoreCase("eth0")) {
        val addresses: util.Enumeration[InetAddress] = element.getInetAddresses
        while (addresses.hasMoreElements) {
          val address = addresses.nextElement
          if (!address.getHostAddress.contains(":")) {
            return address.getHostAddress
          }
        }
      }
    }
    ""
  }
}
