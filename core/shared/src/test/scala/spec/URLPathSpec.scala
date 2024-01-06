package spec

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import spice.net._

class URLPathSpec extends AnyWordSpec with Matchers {
  "Path" should {
    "interpolate a simple path" in {
      val path = path"/one/two/three"
      path.parts should be(List(
        URLPathPart.Separator,
        URLPathPart.Literal("one"),
        URLPathPart.Separator,
        URLPathPart.Literal("two"),
        URLPathPart.Separator,
        URLPathPart.Literal("three")
      ))
      path.toString should be("/one/two/three")
    }
    "interpolate with up-level and same-level" in {
      val path = path"/one/two/../three/./four"
      path.toString should be("/one/three/four")
    }
    "verify path argument matching" in {
      val path = path"/one/two/{arg}"
      path"/one/two/three" should be(path)
    }
    "verify partial path argument matching" in {
      val path = path"/one/two/part-{arg}"
      path"/one/two/part-three" should be(path)
    }
  }
}
