package spice.http

import java.nio.ByteBuffer

trait BinaryData

case class ByteBufferData(bb: ByteBuffer) extends BinaryData