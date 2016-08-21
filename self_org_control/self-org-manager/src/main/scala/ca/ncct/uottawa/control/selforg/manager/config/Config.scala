package ca.ncct.uottawa.control.selforg.manager.config

/**
  * Created by Bogdan on 2/14/2016.
  */
case class Config(val networkName:String, val cloudName: String, startPort: Int, seedNode: String) {

}

object Config {
  def fromXML(node: scala.xml.NodeSeq): Config =
    new Config(
      networkName = (node \ "@network").text,
      cloudName = (node \ "@cloudName").text,
      startPort = (node \ "@startPort").text.toInt,
      seedNode = (node \ "@cluster-seed").text
    )
}
