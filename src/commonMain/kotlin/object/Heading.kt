package `object`

import com.soywiz.korma.geom.Angle

enum class Heading(val angle: Angle) {
    East(Angle.fromDegrees(0)),
    SouthEast(Angle.fromDegrees(45)),
    South(Angle.fromDegrees(90)),
    SouthWest(Angle.fromDegrees(135)),
    West(Angle.fromDegrees(180)),
    NorthWest(Angle.fromDegrees(225)),
    North(Angle.fromDegrees(270)),
    NorthEast(Angle.fromDegrees(315)),
}