// !API_VERSION: 1.0

@SinceKotlin("1.0")
fun ok() {}

@SinceKotlin("1.1")
fun f() {}

@SinceKotlin("1.1")
var p = Unit

@SinceKotlin("1.1")
open class Foo

class Bar @SinceKotlin("1.1") constructor()

@SinceKotlin("1.1")
annotation class Anno1(val s: String)

annotation class Anno2 @SinceKotlin("1.1") constructor()

// ------------------------

fun t0() = ok()

fun t1() = <!UNRESOLVED_REFERENCE!>f<!>()
fun t2() = <!UNRESOLVED_REFERENCE!>p<!>
fun t3() { <!UNRESOLVED_REFERENCE!>p<!> = Unit }
fun t4(): <!API_NOT_AVAILABLE!>Foo<!> = <!UNRESOLVED_REFERENCE!>Foo<!>()
fun t5() = object : <!UNRESOLVED_REFERENCE, DEBUG_INFO_UNRESOLVED_WITH_TARGET!>Foo<!>() {}
fun t6(): Bar? = <!UNRESOLVED_REFERENCE!>Bar<!>()

@<!UNRESOLVED_REFERENCE, DEBUG_INFO_UNRESOLVED_WITH_TARGET!>Anno1<!>("")
@<!UNRESOLVED_REFERENCE, DEBUG_INFO_UNRESOLVED_WITH_TARGET!>Anno2<!>
fun t7() = Unit
