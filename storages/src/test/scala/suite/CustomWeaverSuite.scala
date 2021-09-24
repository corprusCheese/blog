package suite

import weaver.IOSuite
import weaver.scalacheck.Checkers

abstract class CustomWeaverSuite extends IOSuite with Checkers {}
