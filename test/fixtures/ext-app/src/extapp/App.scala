package extapp

import extlib.Base
import extlib.Flag
import extlib.Pair

class App extends Base:
  def total(pair: Pair, flag: Boolean): Int =
    if flag then pair.left + pair.right else pair.left

  def both(left: Flag, right: Flag): Flag = left && right
