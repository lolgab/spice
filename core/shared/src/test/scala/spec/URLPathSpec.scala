package spec

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import spice.net._

class URLPathSpec extends AnyWordSpec with Matchers {
  "Path" should {
    "interpolate a simple path" in {
      val path = path"/one/two/three"
      path.parts should be(List(
        URLPathPart.Literal("one"),
        URLPathPart.Literal("two"),
        URLPathPart.Literal("three")
      ))
      path.toString should be("/one/two/three")
    }
    "interpolate with up-level and same-level" in {
      val path = path"/one/two/../three/./four"
      path.toString should be("/one/three/four")
    }
  }
}