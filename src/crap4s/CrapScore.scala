package crap4s

object CrapScore:
  /** CRAP = cc² × (1 − cov)³ + cc. */
  def of(cc: Int, cov: Double): Double =
    val ccD = cc.toDouble
    ccD * ccD * math.pow(1.0 - cov, 3) + ccD
